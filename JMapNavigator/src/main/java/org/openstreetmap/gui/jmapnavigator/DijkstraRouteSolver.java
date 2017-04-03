package org.openstreetmap.gui.jmapnavigator;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import org.openstreetmap.gui.jmapviewer.Coordinate;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;


public class DijkstraRouteSolver implements IRouteSolver {

	// General constants
	private static int ROUTE_HEAP_CAPACITY = 1000000;

	private final Random random = new Random(0);

	private List<Long> mapNodeIDsList;
	private final Long2ObjectMap<MapNode> mapNodes = new Long2ObjectOpenHashMap<>();

	private final String roadGraphFile;

	private volatile RoutingState state = RoutingState.NotReady;
	private Long startNodeIndex = null;
	private Long targetNodeIndex = null;
	private volatile boolean needsDispalyRefresh = false;
	public float timeOfRoute = 0.0f; // Route time in seconds

	// Debugging and routing preview
	private List<Coordinate> routingPreviewDots = new LinkedList<>();
	private static final double routingPreviewDotPropability = 0.999;

	// Final route
	private List<Coordinate> calculatedRoute = new LinkedList<>();
	private Long bestCandidateNode;

	// Pathfinding
	private final NodeDistHeap routeDistHeap;
	private long startTime;
	private Random rd;
	private Long2ObjectMap<DiscoveredPathNode> openList = new Long2ObjectOpenHashMap<>();
	private LongSet closedList = new LongOpenHashSet();
	private boolean found = false;
	private DiscoveredPathNode foundNode;



	/**
	 * Constructor, loads grid data
	 */
	public DijkstraRouteSolver(String roadGraphFile) {
		this.roadGraphFile = roadGraphFile;

		try {
			intializeGrids();

			routeDistHeap = new NodeDistHeap(ROUTE_HEAP_CAPACITY);
		}
		catch (Exception e) {
			System.err.println("Error at loadOsmData");
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		startNodeIndex = findNextNode(47.8f, 9.0f);
		targetNodeIndex = findNextNode(49.15f, 9.22f);

		state = RoutingState.Standby;
		needsDispalyRefresh = true;
	}


	/**
	 * Read and initialize grid information
	 *
	 * @throws Exception
	 */
	private void intializeGrids() throws Exception {
		System.out.println("Start loading map graph");
		try (DataInputStream reader = new DataInputStream(
				new BufferedInputStream(new FileInputStream(roadGraphFile)))) {
			int numVertices = reader.readInt();
			for (int iNode = 0; iNode < numVertices; iNode++) {
				long index = reader.readInt();
				float lat = (float) reader.readDouble(); // TODO Double?
				float lon = (float) reader.readDouble();

				int numEdges = reader.readInt();
				long[] edgeTargets = new long[numEdges];
				float[] edgeDists = new float[numEdges];
				for (int iEdge = 0; iEdge < numEdges; iEdge++) {
					edgeTargets[iEdge] = reader.readInt();
					edgeDists[iEdge] = (float) reader.readDouble(); // TODO Double?
				}
				mapNodes.put(index, new MapNode(index, lat, lon, edgeTargets, edgeDists));
			}
		}

		mapNodeIDsList = new ArrayList<>(mapNodes.keySet());
	}



	@Override
	public void startCalculateRoute() {

		if (state != RoutingState.Standby) {
			System.err.println("Routing not available");
			return;
		}

		if (startNodeIndex == null || targetNodeIndex == null) {
			System.err.println("Cannot calculate route: Must select any start and target");
			return;
		}

		this.state = RoutingState.Routing;
		this.startTime = System.currentTimeMillis();
		needsDispalyRefresh = true;

		if (startNodeIndex == null || targetNodeIndex == null) {
			System.err.println("Cannot calculate route: Must select valid start and target");
			return;
		}

		rd = new Random(123);
		routingPreviewDots.clear();

		// Reset buffers and
		routeDistHeap.resetEmpty();
		openList.clear(); // Stores all open nodes
		closedList.clear(); // Stores all closed nodes

		// Add start node
		routeDistHeap.add(startNodeIndex, 0.0f);
		openList.put(startNodeIndex, new DiscoveredPathNode(startNodeIndex, null, 0));

		found = false;
		//		target = (long) targetNodeGridIndex;
		//		visitedCount = 0;
		//		hCalc = 0;
		//		hReuse = 0;
		//		gridChanges = 0;
		//		gridStays = 0;
		//		firstVisits = 0;
		//		againVisits = 0;
		//		fastFollows = 0;

		//		System.out.println("Start routing from " + startLat + "/" + startLon + " to " + targetLat + "/" + targetLon);
		System.out.println("Start routing from " + startNodeIndex + " to " + targetNodeIndex);
		System.out.flush();


		Thread routingThread = new Thread(new Runnable() {

			@Override
			public void run() {
				System.out.println("Start doRouting thread");
				try {
					doRouting();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				state = RoutingState.Standby;
				System.out.println("Finishing doRouting thread");
			}
		});
		routingThread.setName("RoutingThread");
		routingThread.start();
	}


	private void doRouting() {

		long visNodeIndex;
		MapNode visNode;

		// Find route with Dijkstra
		while (!routeDistHeap.isEmpty()) {
			// Remove and get index
			visNodeIndex = routeDistHeap.removeFirst();
			bestCandidateNode = visNodeIndex;
			DiscoveredPathNode visDiscoveredNode = openList.remove(visNodeIndex);
			closedList.add(visNodeIndex);

			// Visit node/neighbors
			if (visNodeIndex == targetNodeIndex) {
				found = true;
				foundNode = visDiscoveredNode;
				break;
			}

			if (rd.nextFloat() > routingPreviewDotPropability) {
				addNewPreviewDot(getNodeCoordinates(visNodeIndex));
			}

			visNode = mapNodes.get(visNodeIndex);
			if (visNode == null) {
				//System.err.println("No node " + visNodeIndex);
				continue;
			}

			for (int i = 0; i < visNode.EdgeTargets.length; i++) {
				long edgeNodeIndex = visNode.EdgeTargets[i];
				if (closedList.contains(edgeNodeIndex)) continue;

				float dist = visDiscoveredNode.Dist + visNode.EdgeDists[i];
				DiscoveredPathNode edgeNode = openList.get(edgeNodeIndex);
				if (edgeNode != null) {
					// Already discovered
					if (routeDistHeap.decreaseKeyIfSmaller(edgeNodeIndex, dist)) {
						edgeNode.Pre = visDiscoveredNode;
						edgeNode.Dist = dist;
					}
				}
				else {
					// Not discovered yet
					routeDistHeap.add(edgeNodeIndex, dist);
					openList.put(edgeNodeIndex, new DiscoveredPathNode(edgeNodeIndex, visDiscoveredNode, dist));
				}
			}
		}


		//		System.out.println("H calc: " + hCalc);
		//		System.out.println("H reuse: " + hReuse);
		//		System.out.println("gridChanges: " + gridChanges);
		//		System.out.println("gridStays: " + gridStays);
		//		System.out.println("firstVisits: " + firstVisits);
		//		System.out.println("againVisits: " + againVisits);
		//		System.out.println("fastFollows: " + fastFollows);
		System.out.println("MaxHeapSize: " + routeDistHeap.getSizeUsageMax());


		// If found reconstruct route
		if (found) {
			// Reconstruct route
			reconstructRoute();
		}
		else {
			System.err.println("No way found");
		}


		// Cleanup
		openList.clear();
		closedList.clear();
		this.state = RoutingState.Standby;
		needsDispalyRefresh = true;
		System.out.println("Finished routing after " + (System.currentTimeMillis() - startTime) + "ms");
	}


	private void reconstructRoute() {

		calculatedRoute.clear();

		if (!found) {
			return;
		}


		//		distOfRoute = 0.0f; // Route distance in metres
		timeOfRoute = foundNode.Dist; // Route time in hours

		DiscoveredPathNode node = foundNode;
		do {
			calculatedRoute.add(getNodeCoordinates(node.Index));
			node = node.Pre;
		} while (node != null);

		//		System.out.println("Route Distance: " + ((int) distOfRoute / 1000.0f) + "km");
		double timeOfRouteHours = timeOfRoute / 3600;
		int timeHours = (int) (timeOfRouteHours);
		int timeMinutes = (int) (60 * (timeOfRouteHours - timeHours));
		int timeSeconds = (int) (3600 * (timeOfRouteHours - timeHours - (double) timeMinutes / 60));
		System.out.println("Route time: " + timeHours + ":" + timeMinutes + ":" + timeSeconds);
	}



	// Start and end for route
	@Override
	public void setStartNode(long nodeGridIndex) {
		startNodeIndex = nodeGridIndex;
		needsDispalyRefresh = true;
	}

	@Override
	public void setTargetNode(long nodeGridIndex) {
		targetNodeIndex = nodeGridIndex;
		needsDispalyRefresh = true;
	}

	@Override
	public Coordinate getStartCoordinate() {
		if (startNodeIndex == null) {
			return null;
		}
		return getNodeCoordinates(startNodeIndex);
	}

	@Override
	public Coordinate getTargetCoordinate() {
		if (targetNodeIndex == null) {
			return null;
		}
		return getNodeCoordinates(targetNodeIndex);
	}

	@Override
	public RoutingState getRoutingState() {
		return state;
	}


	@Override
	public List<Coordinate> getCalculatedRoute() {
		return calculatedRoute;
	}

	@Override
	public boolean getNeedsDispalyRefresh() {
		return needsDispalyRefresh;
	}

	@Override
	public void resetNeedsDispalyRefresh() {
		needsDispalyRefresh = false;
	}


	private synchronized void addNewPreviewDot(Coordinate dot) {
		routingPreviewDots.add(dot);
	}

	@Override
	public synchronized List<Coordinate> getRoutingPreviewDots() {
		return new ArrayList<>(routingPreviewDots);
	}

	@Override
	public Coordinate getBestCandidateCoords() {
		if (bestCandidateNode == null) {
			return null;
		}
		return getNodeCoordinates(bestCandidateNode);
	}



	@Override
	public float getTimeOfRoute() {
		return timeOfRoute;
	}


	/**
	 * Tries to determine coordinates of a node, tries to load grid if necessary
	 *
	 * @return Coordinates of node
	 */
	private Coordinate getNodeCoordinates(long nodeGridIndex) {
		MapNode node = mapNodes.get(nodeGridIndex);
		return new Coordinate(node.Lat, node.Lon);
	}



	/**
	 * Tries to find out index of next point to given coordinate
	 *
	 * @param coord
	 * @return Index of next point
	 */
	@Override
	public Long findNextNode(float lat, float lon) {
		long nextIndex = -1;
		float smallestDist = Float.MAX_VALUE;

		for (Entry<Long, MapNode> node : mapNodes.entrySet()) {
			//			if (!checkNodeWithFilter(grid, iN, filterBitMask, filterBitValue)) {
			//				continue;
			//			}

			float dist = Utils.calcNodeDistPrecise(lat, lon, node.getValue().Lat, node.getValue().Lon);
			if (dist < smallestDist) {
				smallestDist = dist;
				nextIndex = node.getKey();
			}
		}

		if (nextIndex == -1) {
			return null;
		}

		return nextIndex;
	}


	@Override
	public Coordinate getCoordinatesByIndex(long index) {
		return new Coordinate(mapNodes.get(index).Lat, mapNodes.get((long) index).Lon);
	}

	/**
	 * A random node with >1 outgoing edges
	 */
	@Override
	public long getRandomRouteNodeIndex() {
		long pt = mapNodeIDsList.get(random.nextInt(mapNodeIDsList.size()));
		while (mapNodes.get(pt).EdgeDists.length < 2) {
			pt = mapNodeIDsList.get(random.nextInt(mapNodeIDsList.size()));
		}
		return pt;
	}


	@Override
	public List<Long> getMapNodeIDsList() {
		return mapNodeIDsList;
	}


	@Override
	public Long2ObjectMap<MapNode> getMapNodes() {
		return mapNodes;
	}
}

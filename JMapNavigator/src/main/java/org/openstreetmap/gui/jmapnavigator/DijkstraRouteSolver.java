package org.openstreetmap.gui.jmapnavigator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.gui.jmapviewer.Coordinate;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;


public class DijkstraRouteSolver implements IRouteSolver {

	// General constants
	private static final String GRAPH_DATA_DIR = "route_graph";
	private static int ROUTE_HEAP_CAPACITY = 1000000;

	private final Long2ObjectMap<MapNode> mapNodes = new Long2ObjectOpenHashMap<>();
	private final NodeDistHeap routeDistHeap;

	private volatile RoutingState state = RoutingState.NotReady;
	private Long startNodeGridIndex = null;
	private Long targetNodeGridIndex = null;
	private volatile boolean needsDispalyRefresh = false;
	public float timeOfRoute = 0.0f; // Route time in seconds

	// Debugging and routing preview
	private List<Coordinate> routingPreviewDots = new LinkedList<>();
	private static final double routingPreviewDotPropability = 0.999;

	// Final route
	private List<Coordinate> calculatedRoute = new LinkedList<>();


	private Long bestCandidateNode;



	/**
	 * Constructor, loads grid data
	 */
	public DijkstraRouteSolver() {

		try {
			intializeGrids();

			routeDistHeap = new NodeDistHeap(ROUTE_HEAP_CAPACITY);
		}
		catch (Exception e) {
			System.err.println("Error at loadOsmData");
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		startNodeGridIndex = findNextNode(47.8f, 9.0f, (byte) 0, (byte) 0);
		targetNodeGridIndex = findNextNode(49.15f, 9.22f, (byte) 0, (byte) 0);

		state = RoutingState.Standby;
		needsDispalyRefresh = true;
	}


	/**
	 * Read and initialize grid information
	 *
	 * @throws Exception
	 */
	private void intializeGrids() throws Exception {
		System.out.println("Start loading grid index");
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(GRAPH_DATA_DIR + "\\graph.txt")))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) continue;
				String[] lineSplit = line.split("\\|");
				String[] vertexSplit = lineSplit[0].split(";");
				long index = Long.parseLong(vertexSplit[0]);
				float lat = (float) Double.parseDouble(vertexSplit[1]);
				float lon = (float) Double.parseDouble(vertexSplit[2]);

				int numEdges = lineSplit.length - 1;
				long[] edgeTargets = new long[numEdges];
				float[] edgeDists = new float[numEdges];
				for (int i = 0; i < numEdges; i++) {
					String[] edgeSplit = lineSplit[i + 1].split(";");
					edgeTargets[i] = Long.parseLong(edgeSplit[0]);
					edgeDists[i] = (float) Double.parseDouble(edgeSplit[1]);
				}
				mapNodes.put(index, new MapNode(lat, lon, edgeTargets, edgeDists));
			}
		}
	}


	// Start and end for route
	@Override
	public void setStartNode(long nodeGridIndex) {
		startNodeGridIndex = nodeGridIndex;
		needsDispalyRefresh = true;
	}

	@Override
	public void setTargetNode(long nodeGridIndex) {
		targetNodeGridIndex = nodeGridIndex;
		needsDispalyRefresh = true;
	}

	@Override
	public Coordinate getStartCoordinate() {
		if (startNodeGridIndex == null) {
			return null;
		}
		return getNodeCoordinates(startNodeGridIndex);
	}

	@Override
	public Coordinate getTargetCoordinate() {
		if (targetNodeGridIndex == null) {
			return null;
		}
		return getNodeCoordinates(targetNodeGridIndex);
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


	@Override
	public void startCalculateRoute() {
		// TODO Auto-generated method stub

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
	public Long findNextNode(float lat, float lon, byte filterBitMask, byte filterBitValue) {
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
}

package org.openstreetmap.gui.jmapnavigator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.openstreetmap.gui.jmapviewer.Coordinate;

public class QueryGeneration {


	/**
	 * Returns city clusters and their size.
	 */
	public static List<Pair<MapNodeCluster, Integer>> cityClustering(Collection<MapNode> mapNodes, int numSpots,
			double rangeDivisor) {
		// Load cities and their sizes
		List<String> largestCities = new ArrayList<>();
		List<Integer> largestCitiesSizes = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new FileReader("data" + File.separator + "cities_bw_sizes.csv"))) {
			String line;
			while ((line = reader.readLine()) != null && largestCities.size() < numSpots) {
				String[] split = line.split(";");
				largestCities.add(split[0].toLowerCase());
				largestCitiesSizes.add(Integer.parseInt(split[1]));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// Load cities coordinates
		Map<String, Coordinate> citiesCoordinates = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(
				new FileReader("data" + File.separator + "cities_de_coords.csv"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] split = line.split(";");
				citiesCoordinates.put(split[0].toLowerCase(),
						new Coordinate(Double.parseDouble(split[1]), Double.parseDouble(split[2])));
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// Create city clusters
		List<Pair<MapNodeCluster, Integer>> clusters = new ArrayList<>(largestCities.size());
		for (int i = 0; i < largestCities.size(); i++) {
			String cName = largestCities.get(i);
			int cPopulation = largestCitiesSizes.get(i);
			Coordinate cCoords = citiesCoordinates.get(cName);
			if (cCoords == null) {
				System.err.println("No coordinates for " + cName);
				continue;
			}
			double cRange = Math.sqrt((double) cPopulation) / rangeDivisor;
			clusters.add(new Pair<>(
					new MapNodeCluster(Utils.coordinateToVector(citiesCoordinates.get(cName)), cPopulation, cRange),
					cPopulation));
		}

		// Assign nodes to clusters
		for (MapNode node : mapNodes) {
			MapNodeCluster bestCluster = null;
			double bestClusterDist = Double.POSITIVE_INFINITY;
			for (Pair<MapNodeCluster, Integer> clusterPair : clusters) {
				MapNodeCluster cluster = clusterPair.getFirst();
				double dist = Utils.calcVector2Dist(cluster.center, new double[] { node.Lat, node.Lon });
				if (dist <= cluster.range && dist < bestClusterDist) {
					bestCluster = cluster;
					bestClusterDist = dist;
				}
			}
			if (bestCluster != null) bestCluster.nodes.add(node);
		}

		return clusters;
	}


	public static class MapNodeCluster {

		public final double[] center;
		public final int population;
		public final double range;
		public final List<MapNode> nodes;


		public MapNodeCluster(double[] center, int population, double range) {
			super();
			this.center = center;
			this.population = population;
			this.range = range;
			this.nodes = new ArrayList<>();
		}
	}



	//	private static final double MapPointSamplingFactor = 0.005;
	//	private static final int ClustersPerHotspots = 40;
	//
	//	public static List<MapNodeCluster> hotspotClustering(Collection<MapNode> mapNodes,
	//			int numHotspots) {
	//		int k = numHotspots * ClustersPerHotspots;
	//		Random rd = new Random(0);
	//
	//		KMeansPlusPlusClusterer<MapNodeClusterable> clusterer = new KMeansPlusPlusClusterer<>(k);
	//		List<MapNodeClusterable> clusterableNodes = new ArrayList<>(mapNodes.size());
	//		for (MapNode node : mapNodes) {
	//			if (rd.nextDouble() < MapPointSamplingFactor)
	//				clusterableNodes.add(new MapNodeClusterable(node));
	//		}
	//		System.out.println("Start clustering");
	//		List<CentroidCluster<MapNodeClusterable>> clusterResult = clusterer.cluster(clusterableNodes);
	//		System.out.println("Finished clustering");
	//
	//		// Get clusters with lowest variance - our hotspots
	//		List<MapNodeCluster> clusters = clusterResult.stream().map(c -> new MapNodeCluster(c))
	//				.collect(Collectors.toList());
	//		clusters.sort(new Comparator<MapNodeCluster>() {
	//
	//			@Override
	//			public int compare(MapNodeCluster o1, MapNodeCluster o2) {
	//				return Double.compare(o1.variance, o2.variance);
	//				//return Integer.compare(o1.nodes.size(), o2.nodes.size());
	//			}
	//		});
	//
	//		return new ArrayList<MapNodeCluster>(clusters.subList(0, numHotspots));
	//	}
	//
	//	public static class MapNodeClusterable implements Clusterable {
	//
	//		public final MapNode node;
	//		public final double[] nodeCoords;
	//
	//		public MapNodeClusterable(MapNode node) {
	//			this.node = node;
	//			this.nodeCoords = new double[] { node.Lat, node.Lon };
	//		}
	//
	//		@Override
	//		public double[] getPoint() {
	//			return nodeCoords;
	//		}
	//	}

	//	public static class MapNodeCluster {
	//
	//		public final double[] center;
	//		public final List<MapNodeClusterable> nodes;
	//		public final double variance;
	//
	//		public MapNodeCluster(CentroidCluster<MapNodeClusterable> cluster) {
	//			this(cluster.getCenter().getPoint(), cluster.getPoints());
	//		}
	//
	//		public MapNodeCluster(Coordinate center, List<MapNodeClusterable> nodes) {
	//			this(new double[] { center.getLat(), center.getLon() }, nodes);
	//		}
	//
	//		public MapNodeCluster(double[] center, List<MapNodeClusterable> nodes) {
	//			super();
	//			this.center = center;
	//			this.nodes = nodes;
	//
	//			double varianceSum = 0;
	//			for (MapNodeClusterable node : nodes) {
	//				varianceSum = Utils.calcVector2Dist(node.getPoint(), center);
	//			}
	//			variance = varianceSum / nodes.size();
	//		}
	//	}
}

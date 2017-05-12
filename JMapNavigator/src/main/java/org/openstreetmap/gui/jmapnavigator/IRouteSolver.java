package org.openstreetmap.gui.jmapnavigator;

import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;


/**
 * Interface for route solvers finding map paths.
 *
 * @author Jonas Grunert
 *
 */
public interface IRouteSolver {

	public enum RoutingState {
		NotReady, Standby, Routing
	}

	void setStartNode(long nodeGridIndex);

	void setTargetNode(long nodeGridIndex);

	Coordinate getStartCoordinate();

	Coordinate getTargetCoordinate();

	RoutingState getRoutingState();

	List<Coordinate> getRoutingPreviewDots();

	List<Coordinate> getCalculatedRoute();

	boolean getNeedsDispalyRefresh();

	void resetNeedsDispalyRefresh();

	Long findNextNode(float lat, float lon);

	void startCalculateRoute(boolean updateDisplay, int timeout);

	Coordinate getBestCandidateCoords();

	Coordinate getCoordinatesByIndex(long index);

	float getTimeOfRoute();



	List<Long> getMapNodeIDsList();

	Long2ObjectMap<MapNode> getMapNodes();

	long getRandomRouteNodeIndex();


	boolean checkIfPathExisting(long fromNodeIndex, long toNodeIndex, int timeout);
}

package org.openstreetmap.gui.jmapnavigator;

import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;


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

	void startCalculateRoute();

	Coordinate getBestCandidateCoords();

	Coordinate getCoordinatesByIndex(long index);

	float getTimeOfRoute();


	long getRandomRouteNodeIndex();
}

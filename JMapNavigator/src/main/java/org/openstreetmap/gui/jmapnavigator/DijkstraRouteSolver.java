package org.openstreetmap.gui.jmapnavigator;

import java.util.List;

import org.openstreetmap.gui.jmapviewer.Coordinate;


public class DijkstraRouteSolver implements IRouteSolver {

	@Override
	public void setStartNode(long nodeGridIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setTargetNode(long nodeGridIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public Coordinate getStartCoordinate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Coordinate getTargetCoordinate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RoutingState getRoutingState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Coordinate> getRoutingPreviewDots() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Coordinate> getCalculatedRoute() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getNeedsDispalyRefresh() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void resetNeedsDispalyRefresh() {
		// TODO Auto-generated method stub

	}

	@Override
	public Long findNextNode(float lat, float lon, byte filterBitMask, byte filterBitValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void startCalculateRoute() {
		// TODO Auto-generated method stub

	}

	@Override
	public Coordinate getBestCandidateCoords() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getDistOfRoute() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float getTimeOfRoute() {
		// TODO Auto-generated method stub
		return 0;
	}
}

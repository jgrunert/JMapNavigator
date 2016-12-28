package org.openstreetmap.gui.jmapnavigator;


public class MapNode {

	public final float Lat;
	public final float Lon;
	public final long[] EdgeTargets;
	public final float[] EdgeDists;


	public MapNode(float lat, float lon, long[] edgeTargets, float[] edgeDists) {
		super();
		Lat = lat;
		Lon = lon;
		EdgeTargets = edgeTargets;
		EdgeDists = edgeDists;
	}
}

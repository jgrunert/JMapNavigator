package org.openstreetmap.gui.jmapnavigator;


public class MapNode {

	public long Id;
	public final float Lat;
	public final float Lon;
	public final long[] EdgeTargets;
	public final float[] EdgeDists;


	public MapNode(long id, float lat, float lon, long[] edgeTargets, float[] edgeDists) {
		super();
		Id = id;
		Lat = lat;
		Lon = lon;
		EdgeTargets = edgeTargets;
		EdgeDists = edgeDists;
	}
}

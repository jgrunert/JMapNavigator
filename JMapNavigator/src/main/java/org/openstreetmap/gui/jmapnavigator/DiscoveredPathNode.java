package org.openstreetmap.gui.jmapnavigator;


public class DiscoveredPathNode {

	public final long Index;
	public DiscoveredPathNode Pre;
	public float Dist;

	public DiscoveredPathNode(long index, DiscoveredPathNode pre, float dist) {
		super();
		Index = index;
		Pre = pre;
		Dist = dist;
	}
}

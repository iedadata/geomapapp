package org.geomapapp.gis.shape;

import java.awt.geom.Point2D;

public class ESRINull
			implements ESRIShape {
	public ESRINull() {
	}
	public int getType() {
		return 0;
	}
	public void draw(java.awt.Graphics2D g) {
	}
	public NearNeighbor select( NearNeighbor n ) {
		return n;
	}
	public int writeShape( java.io.OutputStream out ) throws java.io.IOException {
		return 0;
	}
	public double[][] inverse( org.geomapapp.geom.MapProjection proj, double[][] bounds) {
		return bounds;
	}
	public boolean canView( java.awt.geom.Rectangle2D r, double wrap) {
		return false;
	}
	public void draw(java.awt.Graphics2D g, java.awt.geom.Rectangle2D r, double wrap) {
	}
}
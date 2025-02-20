package haxby.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.grid.Grid2D;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;

/**
 * This class converts Grid2D objects into arrays that GeoTools can use.
 */
public class GTConverter {
	
	public static class GridInfo {
		float[][] values;
		Rectangle bounds;
		public GridInfo(float[][] valuesIn, Rectangle boundsIn) {
			values = valuesIn;
			bounds = boundsIn;
		}
	}

	public static class Grid2DWrapper {
		public final Grid2D data;
		private double lowest, highest;
		public Grid2DWrapper(Grid2D dataIn, double low, double high) {
			data = dataIn;
			lowest = low;
			highest = high;
		}
		public double getLowest() { return lowest; }
		public double getHighest() { return highest; }
	}
	
	public static GridInfo getArr(Grid2D grid) {
		if(grid instanceof Grid2D.Image) {
			System.out.println("This is an image! No conversion this time!");
			return null;
		}
		Rectangle bounds = grid.getBounds();
		float[][] ret = new float[bounds.width][bounds.height];
		for(int x = 0; x < bounds.width; x++) {
			for(int y = 0; y < bounds.height; y++) {
				ret[x][y] = (float) grid.valueAt(x+bounds.getX(), y+bounds.getY());
			}
		}
		return new GridInfo(ret, bounds);
	}
	
	public static Grid2DWrapper getGrid(GridCoverage2D geotoolsGrid, MapProjection proj) {
		GridGeometry2D geom = geotoolsGrid.getGridGeometry();
		GridEnvelope2D env = geom.getGridRange2D();
		Grid2D.Double grid = new Grid2D.Double(env, proj);
		GridCoordinates2D low = env.getLow(), high = env.getHigh();
		double lowest = Double.MAX_VALUE, highest = -Double.MAX_VALUE;
		//TODO consider multithreading for larger grids
		for(int y = low.y; y < high.y; y++) {
			for(int x = low.x; x < high.x; x++) {
				GridCoordinates2D pt = new GridCoordinates2D(x,y);
				//try {
					double[] vals = geotoolsGrid.evaluate(pt, (double[])null);
					if(!Double.isNaN(vals[0])) {
						//System.out.println("("+x + ", "+y+"): "+vals[0]);
						if(vals[0] < lowest) lowest = vals[0];
						if(vals[0] > highest) highest = vals[0];
						grid.setValue(x, y, vals[0]);
					}
				//}
				//catch(Exception e) {
				//}
			}
		}
		grid.fillNaNs();
		return new Grid2DWrapper(grid, lowest, highest);
	}
}

package haxby.util;

import java.awt.Rectangle;

import org.geomapapp.grid.Grid2D;

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
}

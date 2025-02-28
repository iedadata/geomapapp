package haxby.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.function.Function;

import org.geomapapp.geom.MapProjection;
import org.geomapapp.geom.UTM;
import org.geomapapp.grid.Grid2D;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.Projection;

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
		private double xOffset, yOffset, dx, dy;
		public Grid2DWrapper(Grid2D dataIn, double low, double high, double xOffsetIn, double yOffsetIn, double dxIn, double dyIn) {
			data = dataIn;
			lowest = low;
			highest = high;
			xOffset = xOffsetIn;
			yOffset = yOffsetIn;
			dx = dxIn;
			dy = dyIn;
		}
		public double getLowest() { return lowest; }
		public double getHighest() { return highest; }
		public double getXOffset() { return xOffset; }
		public double getYOffset() { return yOffset; }
		public double getdx() { return dx; }
		public double getdy() { return dy; }
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
	
	public static Grid2DWrapper getGrid(GridCoverage2D geotoolsGrid, MapProjection proj, boolean hasNoData, double noDataVal) {
		GridGeometry2D geom = geotoolsGrid.getGridGeometry();
		GridEnvelope2D env = geom.getGridRange2D();
		Matrix m = ((AffineTransform2D)geom.getGridToCRS2D()).getMatrix();
		double xOffset = m.getElement(0, 2),
				yOffset = m.getElement(1, 2),
				dx = m.getElement(0, 0),
				dy = m.getElement(1, 1);
		Rectangle rect = new Rectangle((int)xOffset, (int)yOffset, env.width, env.height);
		Grid2D.Double grid = new Grid2D.Double(rect, proj);
		GridCoordinates2D low = env.getLow(), high = env.getHigh();
		double lowest = Double.MAX_VALUE, highest = -Double.MAX_VALUE;
		Function <Double, Boolean> isData = (hasNoData)?(x -> x != noDataVal):(x -> !Double.isNaN(x));
		//TODO consider multithreading for larger grids
		for(int y = low.y; y < high.y; y++) {
			for(int x = low.x; x < high.x; x++) {
				GridCoordinates2D pt = new GridCoordinates2D(x,y);
				//try {
					double[] vals = geotoolsGrid.evaluate(pt, (double[])null);
					if(isData.apply(vals[0])) {
						//System.out.println("("+x + ", "+y+"): "+vals[0]);
						if(vals[0] < lowest) lowest = vals[0];
						if(vals[0] > highest) highest = vals[0];
						grid.setValue(x+(int)xOffset, y+(int)yOffset, vals[0]);
					}
					else {
						grid.setValue(x, y, Double.NaN);
					}
				//}
				//catch(Exception e) {
				//}
			}
		}
		return new Grid2DWrapper(grid, lowest, highest, xOffset, yOffset, dx, dy);
	}
	
	public static MapProjection getGmaProj(CoordinateReferenceSystem crs) {
		String epsgPrjStr = String.valueOf(crs.getIdentifiers().toArray()[0]);
		if(epsgPrjStr.startsWith("EPSG:")) {
			String code = epsgPrjStr.substring(5);
			if(code.startsWith("326") || code.startsWith("327")) {
				int whichHemisphere = code.startsWith("326")? MapProjection.NORTH : MapProjection.SOUTH;
				int whichZone = Integer.parseInt(code.substring(3));
				UTM utm = new UTM(whichZone, 2, whichHemisphere);
				return utm;
			}
			else if(code.equals("3857")) {
				//TODO
			}
		}
		System.err.println("Unknown projection: " + epsgPrjStr);
		return null;
	}
}

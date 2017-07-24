package haxby.map;

import haxby.grid.GridImager;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.text.NumberFormat;
import java.util.ListIterator;
import java.util.Vector;

import org.geomapapp.geom.XYZ;
import org.geomapapp.gis.shape.ESRIShapefile;
import org.geomapapp.grid.Grid2DOverlay;

public class MapColorScale implements MapInset, 
				MouseListener,
				MouseMotionListener {
	BufferedImage image;
	int xOffset, yOffset;
	int width, height;
	boolean vertical;
	XMap map;

//	***** GMA 1.6.6: Add left and right bar values
	float leftBar = -7000;
	float rightBar = 3000;
	Grid2DOverlay colorScaleGrid = null;
	String units = "meters";
//	***** GMA 1.6.6	

	public MapColorScale( XMap map ) {
		this.map = map;
		image = new BufferedImage( 20, 100, BufferedImage.TYPE_INT_RGB );
		GridImager imager = new GridImager();
		imager.setGamma(1.5);
		imager.saturate(.35f);

//		***** GMA 1.6.6: Get currently loaded grid, if any, and set ranges according to that grid
//		First try to get an imported grid  **DEPRECATED??** NSS 10/25/16
//		if ( ((MapApp)(map.getApp())).tools.suite.dialog != null && ((MapApp)(map.getApp())).tools.suite.dialog.loaded ) {
//			if ( ((MapApp)(map.getApp())).tools.suite.dialog.getGrid() != null ) {
//				colorScaleGrid = ((MapApp)(map.getApp())).tools.suite.dialog.getGrid();
//				String gridName = ((MapApp)(map.getApp())).tools.suite.dialog.getGrid().toString();
//				Vector shapes = ((MapApp)(map.getApp())).tools.suite.getShapes();
//				ListIterator iter = shapes.listIterator();
//				while (iter.hasNext()) {
//					ESRIShapefile shape = (ESRIShapefile)iter.next();
//					if ( shape.getName().equals(gridName) ) {
//						units = shape.getUnits();
//					}
//				}
//			}
//		}
//		Then try to get a background grid
		if ( ((MapApp)(map.getApp())).tools.gridDialog != null && ((MapApp)(map.getApp())).tools.gridDialog.isDialogVisible() ) {
			colorScaleGrid = ((Grid2DOverlay)(((MapApp)(map.getApp())).tools.gridDialog.getGrid()));
			units = map.getUnits();
		}
//		Then whatever grid might be on the map
		else if ( map.getFocus().getGrid() != null ) {

			colorScaleGrid = map.getFocus();
		}
		if ( colorScaleGrid != null && colorScaleGrid.lut != null ) {
			leftBar = colorScaleGrid.lut.getPalette().getRange()[0];
			rightBar = colorScaleGrid.lut.getPalette().getRange()[1];
		}
//		***** GMA 1.6.6

		for( int y = 0; y < 100; y++ ) {

//			***** GMA 1.6.6: 
			float z = 2950 - 100f*(float)y;
//			***** GMA 1.6.6

			for( int x=0 ; x<20 ; x++ ) {
				float shade = .215f + .03f*(float)x;
//				***** GMA 1.6.6: Set new RGB value from the palette of the currently loaded grid
//								 Set the image color at x,y to the color from the loaded grid, if no loaded grid set it 
//								 to color from background imagery
//				image.setRGB( x, y, imager.getRGB( z, shade ) );

				if ( colorScaleGrid != null && colorScaleGrid.lut != null ) {
					shade = (float)colorScaleGrid.lut.getSun().dot( new XYZ(0.,0.,1.) );
					int rgb = colorScaleGrid.lut.getPalette().getRGB( (float)( (colorScaleGrid.lut.getPalette().getRange()[0]) + ( y * ( ( (colorScaleGrid.lut.getPalette().getRange()[1]) - (colorScaleGrid.lut.getPalette().getRange()[0]) ) / 100 ) ) ), shade );
					Color gridColor = new Color(rgb);
//					y is inverted to correctly vertically orient the color scale
					image.setRGB( x, 99 - y, gridColor.getRGB() );
				}
				else {
					image.setRGB( x, y, imager.getRGB( z, shade ) );
				}
//				***** GMA 1.6.6
			}
		}
		width = 25;
		height = 150;
		vertical = true;
		xOffset = yOffset = 50;
	}

	public void reset() {
		width = 25;
		height = 150;
		vertical = true;
		xOffset = yOffset = 50;
	}
	public void draw( Graphics2D g, int w, int h ) {
		int x = xOffset;
		int y = yOffset;
		if( x+width > w ) x = w - width;
		if( y+height > h ) y = h - height;
		xOffset = x;
		yOffset = y;

		if ( colorScaleGrid != null && colorScaleGrid.toString().toLowerCase().indexOf("gravity") != -1 ) {
			units = "mgals";
		}

		AffineTransform at0 = g.getTransform();
		g.translate( x, y);
		g.setFont( new Font( "Helvetica", Font.PLAIN, 12 ) );
		FontMetrics fm = g.getFontMetrics();
		RenderingHints hints = g.getRenderingHints();
		g.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		g.setStroke( new BasicStroke(1f) );
		g.setColor( Color.black );
		g.setBackground(Color.WHITE);

		Color textC = Color.BLACK;
		Color bgColor = new Color(255, 255, 255, 100);
		Rectangle2D rect = fm.getStringBounds(units, g);
		g.setColor(bgColor);
		g.fillRect((int)rect.getMinX() -4, (int) rect.getY() -4, width + 42, height + 18);
		g.setColor(textC);
		g.drawString( units, 0, -2 );

		AffineTransform at = g.getTransform();
		if( width!=20 || height !=100 ) {
			g.scale( (double)width/20., (double)height/100. );
			g.setStroke( new BasicStroke( 100f/(float)height ) );
		}

		g.drawRenderedImage( image, new AffineTransform() );
		for( int z=0 ; z<=100 ; z+=10 ) {
			g.drawLine(0, z, 20, z);
		}
		g.setTransform( at );
		g.setStroke( new BasicStroke(1f) );
		g.drawRect( 0, 0, width, height );
		y = 10;
		double scale = height/100.;
		x = (int)(24.*(double)width/20.);

//		***** GMA 1.6.6: Adjust color scale labels according to data range palette covers
//		g.drawString( "mx1000", 0, -2 );
//		for( int z=2000 ; z>=-6000 ; z-=2000 ) {
//			int yy = (int) (y*scale);
//			g.drawString( Integer.toString(z/1000), x, yy+5 );
//			y += 20;
//		}
//		g.setTransform( at0 );

		int multiplier = 0;
		if ( Math.abs(leftBar) < 10 || Math.abs(rightBar) < 10 ) {
			multiplier = 1;
		}
		else if ( Math.abs(leftBar) < 100 || Math.abs(rightBar) < 100 ) {
			multiplier = 10;
		}
		else if ( Math.abs(leftBar) < 1000 || Math.abs(rightBar) < 1000 ) {
			multiplier = 100;
		}
		else {
			multiplier = 1000;
		}
//		g.drawString( "mx" +  multiplier, 0, -2 );

//		for( int z = (int)rightBar - ( ( ( (int)rightBar - (int)leftBar ) / 5 ) ) / 2; z >= (int)leftBar; z -= ( ( (int)rightBar - (int)leftBar ) / 5 ) ) {
		float z = rightBar - ( ( ( rightBar - leftBar ) / 5 ) ) / 2;
		NumberFormat fmt = NumberFormat.getInstance();
		if ( Math.abs( rightBar - leftBar ) > 5  ) {
			fmt.setMaximumFractionDigits(0);
		}
		else {
			fmt.setMaximumFractionDigits(1);
			fmt.setMinimumFractionDigits(1);
		}
		while ( z >= leftBar ) {
			int yy = (int) (y*scale);
			// g.drawString( Integer.toString(z/multiplier), x, yy+5 );
			if ( Math.abs( rightBar - leftBar ) > 6000 ) {
				// g.drawString( Integer.toString( ( z - z % 1000) ), x, yy+5 );
				g.drawString( fmt.format( ( (int)z - (int)z % 1000) ).replaceAll( ",", "" ), x, yy+5 );
			}
			else if ( Math.abs( rightBar - leftBar ) <= 6000 && Math.abs( rightBar - leftBar ) > 600 ) {
				// g.drawString( Integer.toString( ( z - z % 100) ), x, yy+5 );
				g.drawString( fmt.format( ( (int)z - (int)z % 100) ).replaceAll( ",", "" ), x, yy+5 );
			}
			else if ( Math.abs( rightBar - leftBar ) <= 600 && Math.abs( rightBar - leftBar ) > 100 ) {
				// g.drawString( Integer.toString( ( z - z % 10) ), x, yy+5 );
				g.drawString( fmt.format( ( (int)z - (int)z % 10) ).replaceAll( ",", "" ), x, yy+5 );
			}
			else {
				// g.drawString( Integer.toString(z), x, yy+5 );
				if ( Math.abs( rightBar - leftBar ) > 5 ) {
					g.drawString( fmt.format( (int)z ).replaceAll( ",", "" ), x, yy+5 );
				}
				else {
					g.drawString( fmt.format(z).replaceAll( ",", ""), x, yy+5 );
				}
			}
			y += 20;
			z -= ( ( rightBar - leftBar ) / 5 );
		}
		g.setTransform( at0 );
	}

	boolean selected = false;
	Point lastPoint = null;
	public void mouseEntered( MouseEvent evt ) {
	}
	public void mouseExited( MouseEvent evt ) {
	}
	public void mousePressed( MouseEvent evt ) {
		selected = false;
		if( evt.isControlDown() ) {
			return;
		}
		selected = false;
		Rectangle r = map.getVisibleRect();
		MapBorder border = map.getMapBorder();
		if( border!=null ) {
			Insets ins = border.getBorderInsets(map);
			r.x += ins.left;
			r.y += ins.top;
		}
		r.x += xOffset;
		r.y += yOffset;
		r.width = width;
		r.height = height;
		if( r.contains( evt.getPoint() ) ) {
			selected = true;
			lastPoint = evt.getPoint();
		}
	}
	public void mouseReleased( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			selected = false;
			return;
		}
		if( selected ) {
			selected = false;
			map.repaint();
		}
	}
	public void mouseClicked( MouseEvent evt ) {
	}
	public void mouseMoved( MouseEvent evt ) {
	}
	public void mouseDragged( MouseEvent evt ) {
		if( evt.isControlDown() ) {
			selected = false;
			return;
		}
		if( selected ) {
			Point p = evt.getPoint();
			xOffset += p.x - lastPoint.x;
			yOffset += p.y - lastPoint.y;
			if(xOffset<0) xOffset=0;
			if(yOffset<0) yOffset=0;
			lastPoint = p;
		}
	}
}

package org.geomapapp.util;

import java.awt.*;
import java.awt.geom.*;
import java.text.*;

import org.geomapapp.db.dsdp.ModBRGTable;

public class ModAxes {
	public final static int LEFT = 1;
	public final static int RIGHT = 2;
	public final static int TOP = 4;
	public final static int BOTTOM = 8;
	int sides;
	XYPoints xy;

//	***** GMA 1.6.8: Add an additional set of points
	int dataIndex2;
	static int data_index_2;
	static int data_index;
	static String data_index_name = null;
//	***** GMA 1.6.8	

	int dataIndex;
	Font font = new Font("Serif", Font.PLAIN, 12 );
	static double[] res = { 2., 2.5, 2. };
	public ModAxes( XYPoints pts, int dataIndex, int sides ) {
		xy = pts;
		this.dataIndex = dataIndex;
		this.data_index = dataIndex;
		data_index_name = xy.getXTitle(dataIndex);
		this.sides = sides;
	}

//	***** GMA 1.6.8: Add an additional set of points

	public void setDataIndex2( int inputDataIndex2 ) {
		this.dataIndex2 = inputDataIndex2;
		this.data_index_2 = inputDataIndex2;
	}
//	***** GMA 1.6.8
	public void setSides( int sides ) {
		this.sides = sides;
	}
	public void setFont( Font font) {
		this.font = font;
	}
	public Insets getInsets() {
		int size = font.getSize();
		size = 5*size/2;
		Insets ins = new Insets(0, 0, 0, 0);
		if( (sides&LEFT) != 0 ) ins.left+=size;
		if( (sides&RIGHT) != 0 ) ins.right+=size;
		if( (sides&TOP) != 0 ) ins.top+=size;
		if( (sides&BOTTOM) != 0 ) ins.bottom+=size;
		return ins;
	}
	public void drawAxes( Graphics2D g,
			Rectangle2D bounds,
			Rectangle rect ) {
		RenderingHints hints = g.getRenderingHints();
	//	g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		AffineTransform at = g.getTransform();
		Insets ins = getInsets();
		g.setFont( new Font("Serif", Font.PLAIN, 12 ) ) ;
		g.setColor( Color.black );
// X Axis
		if( (sides&BOTTOM) != 0 ) {
			g.translate( rect.x+ins.left, rect.y+rect.height-ins.bottom);
			drawAxis( g, false, xy.getXTitle(dataIndex), 
				bounds.getX(), bounds.getX()+bounds.getWidth(), 
				rect.width-ins.left-ins.right, 
				-(rect.height-ins.top-ins.bottom) );
			g.setTransform( at );
		}
		if( (sides&TOP) != 0 ) {
			g.translate( rect.x+ins.left, rect.y+ins.top);
//			System.out.println("dataIndex: " + dataIndex + " dataIndex2: " + dataIndex2);
			drawAxis( g, true, xy.getXTitle(dataIndex2), 
				bounds.getX(), bounds.getX()+bounds.getWidth(), 
				rect.width-ins.left-ins.right, 0 );	
			g.setTransform( at );
		}
// Y Axis
		if( (sides&LEFT ) != 0 ) {
			g.translate( rect.x+ins.left, rect.y+rect.height-ins.bottom);
			g.rotate(Math.toRadians(-90.));
			drawAxis( g, true, xy.getYTitle(dataIndex), 
				bounds.getY()+bounds.getHeight(), bounds.getY(), 
				rect.height-ins.top-ins.bottom, 
				(rect.width-ins.left-ins.right) );
			g.setTransform( at );
		}
		if( (sides&RIGHT ) != 0 ) {
			g.translate( rect.x+rect.width-ins.right, rect.y+rect.height-ins.bottom);
			g.rotate(Math.toRadians(-90.));
			drawAxis( g, false, xy.getYTitle(dataIndex), 
				bounds.getY()+bounds.getHeight(), bounds.getY(), 
				rect.height-ins.top-ins.bottom, 0 );
			g.setTransform( at );
		}
		g.setRenderingHints( hints );
	}

	public double[] getXRange( int inputDataIndex ) {
		return xy.getXRange(inputDataIndex);
	}

	public String getXTitle( int inputDataIndex ) {
		return xy.getXTitle(inputDataIndex);
	}

	public int getDataIndex() {
		return dataIndex;
	}

	public void drawAxis( Graphics2D g, boolean above, String name,
				double x1, double x2, int width, int height ) {
		double resolution = 1.;
		double min, max;
		if( x2>x1 ) {
			min = x1;
			max = x2;
		} else {
			min = x2;
			max = x1;
		}
		int nAnot = width/100;
		if(nAnot<1) nAnot=1;
//	System.out.println( min +"\t"+max +"\t"+ width +"\t"+ nAnot);
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < nAnot) resolution /= 10.;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) > nAnot) resolution *= 10.;
		double power = resolution;
		int kres = 0;
		while( Math.floor(max/resolution)-Math.ceil(min/resolution) < nAnot) {
			kres = (kres+2) %3;
			resolution /= res[kres];
		}
		double val;
		double scale = ((double)width)/(x2-x1);
		if( height != 0 ) {
			double res2 = resolution;
			kres = (kres+2) %3;
			res2 /= res[kres];
			kres = (kres+2) %3;
			res2 /= res[kres];
			g.setStroke(new BasicStroke(1f));
			g.setColor( new Color( 196, 196, 196 ));
			val = res2 * Math.ceil(min/res2);
			while( val<=max ) {
				int x = (int) (scale*(val-x1));
				g.drawLine( x, 0, x, height );
				val += res2;
			}
		}
		val = resolution * Math.ceil(min/resolution);
		NumberFormat nf = NumberFormat.getInstance();

		g.setStroke(new BasicStroke(2f));
		g.setColor( Color.black );
		g.drawLine( 0, 0, width, 0);
		g.setStroke(new BasicStroke(1f));
		int y = 15;
		int x;
		if( above ) y = -5;
		FontMetrics fm = g.getFontMetrics();

//		***** GMA 1.6.6: Draw String every other interval
		boolean drawString = true;
//		***** GMA 1.6.6		

		while( val<=max ) {
			double val2 = Double.NaN;
			if ( !name.equals( xy.getXTitle(dataIndex) ) && !name.equals( xy.getYTitle(dataIndex) ) ) {
//				System.out.println(name);
				val2 = val - xy.getXRange(dataIndex)[0];
				double ratio = ((ModBRGTable)xy).getRatio();
				val2 = val2 / ratio;
				val2 = val2 + xy.getXRange(dataIndex2)[0];
			}

			String s = null;
			if ( !name.equals( xy.getXTitle(dataIndex) ) && !name.equals( xy.getYTitle(dataIndex) ) ) {
				//System.out.println(name);
				s = nf.format(val2);
			}
			else {
				s = nf.format(val);
			}

			x = (int) (scale*(val-x1));
			if( height != 0 ) {
				g.setColor( Color.darkGray );
				g.drawLine( x, 0, x, height);
			}
			x -= fm.stringWidth(s)/2;
			g.setColor( Color.black );

//			***** GMA 1.6.6: Remove comma if present in string
			s = s.replaceAll(",", "");
//			***** GMA 1.6.6

//			***** GMA 1.6.6: Draw String every other interval
//			g.drawString(s, x, y);
			if ( drawString || s.equals("0") ) {
				g.drawString(s, x, y);
				drawString = false;
			}
			else {
				drawString = true;
			}
//			***** GMA 1.6.6

			val += resolution;
		}
		if( name!=null ) {
			x = (width-fm.stringWidth(name)) / 2;
			if(above) y=-18;
			else y=28;
			if ( !name.equals( xy.getXTitle(dataIndex) ) && !name.equals( xy.getYTitle(dataIndex) ) ) {
				g.setColor( Color.red );
			}
			g.drawString(name, x, y);
		}
	}
}
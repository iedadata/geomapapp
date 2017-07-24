package haxby.db.dig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.geomapapp.grid.Grid2DOverlay;

import haxby.db.custom.UnknownDataSet;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;

public class LineSegmentsObject extends AbstractTableModel
				implements DigitizerObject,
					MouseListener,
					MouseMotionListener,
					KeyListener
					{
	XMap map;
	Digitizer dig;
	Vector points;
	boolean visible;
	double wrap;
	Line2D.Double currentSeg;
	String name;
	float lineWidth;
	Color color;
	Color fill;
	Color lastColor, lastFill;
	BasicStroke stroke;
	boolean selected;
	long when;
	int currentPoint;
	double currentOffset = 0.;
	GeneralPath editShape;
	boolean active;
	Vector profile = null;
	int table;
	Grid2DOverlay grid;
	static ImageIcon icon = Digitizer.SEGMENTS(false);
	static ImageIcon invisibleIcon = Digitizer.SEGMENTS(true);
	static Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
	JPopupMenu rightClickMenu;

	public LineSegmentsObject( XMap map, Digitizer dig ) {
		this.map = map;
		wrap = map.getWrap();
		this.dig = dig;
		points = new Vector();
		visible = true;
		currentSeg = null;
		name = null;
		selected = false;
		lineWidth = 1f;
		color = Color.black;
		when = 0L;
		currentPoint = -1;
		editShape = null;
		active = false;
		setColor( dig.options.color );
		setFill( dig.options.fill );
		setStroke( dig.options.stroke );
		lastColor = color;
		lastFill = Color.white;
		table = 0;
		
		rightClickMenu = new JPopupMenu("Popup");
		JMenuItem deleteItem = new JMenuItem("Delete Point");
		deleteItem.setToolTipText("Click to delete");
		deleteItem.addActionListener(new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
	          dig.deletePtsBtn.doClick();
	        }
	      });
		rightClickMenu.add(deleteItem);
	}
	public void setName(String name) {
		this.name = name;
	}
	public String toString() {
		if( name==null) {
			return "Digitized Segment";
		}
		return name;
	}
	public Color getColor() {
		return color;
	}
	public Color getFill() {
		return fill;
	}
	public void setColor( Color c ) {
		if( c==null ) {
			if( color==null ) {
				color = lastColor;
			} else {
				lastColor = color;
				color = c;
			}
			return;
		}
		color = c;
	}
	public void setFill( Color c ) {
		if( c==null ) {
			if( fill==null ) {
				fill = lastFill;
			} else {
				lastFill = fill;
				fill = c;
			}
			return;
		}
		fill = c;
	}
	public BasicStroke getStroke() {
		return stroke;
	}
	public void setStroke( BasicStroke s ) {
		stroke = s;
	}
	public void start() {
		map.addMouseListener( this );
		map.addMouseMotionListener( this );
		map.addKeyListener( this );
		map.setCursor( cursor );
		active = true;
		dig.table.setModel( this );
	}
	public boolean finish() {
		active = false;
		drawSeg();
		map.removeMouseListener( this );
		map.removeMouseMotionListener( this );
		map.removeKeyListener( this );
		map.setCursor( Cursor.getDefaultCursor() );
		if( points.size()<=1 ) return false;
		return true;
	}
	public void setVisible( boolean tf ) {
		visible = tf;
	}
	public boolean isVisible() {
		return visible;
	}
	public boolean select( double x, double y ) {
		if( points.size()<=1 )return false;
		Rectangle2D.Double bounds = (Rectangle2D.Double)computeBounds();
		double r = 2.5/map.getZoom();
		bounds.x -= r;
		bounds.y -= r;
		bounds.width += 2.*r;
		bounds.height += 2.*r;
		if( wrap>0. ) {
			while( bounds.x>x ) x += wrap;
			while( bounds.x+bounds.width<x ) x -= wrap;
		}
		if( !bounds.contains( x, y ) )return false;
		double x1, y1, x2, y2, dx0, dy0, dx, dy, r0, r1, test;;
		double[] xyz = (double[])points.get(0);
		x1 = xyz[0];
		y1 = xyz[1];
		double rMin = 100000.;
		r *= r;
		for( int i=1 ; i<points.size() ; i++) {
			xyz = (double[])points.get(i);
			x2 = xyz[0];
			y2 = xyz[1];
			if(x1==x2 && y1==y2) continue;
			dx0 = x2-x1;
			dy0 = y2-y1;
			dx = x-x1;
			dy = y-y1;
			r0 = dx0*dx0 + dy0*dy0;
			test = dx*dx0 + dy*dy0;
			x1 = x2;
			y1 = y2;
			if(test<0) {
				r1 = dx*dx + dy*dy;
			} else if( test>r0 ) {
				dx -= dx0;
				dy -= dy0;
				r1 = dx*dx + dy*dy;
			} else {
				r1 = -dx*dy0 + dy*dx0;
				r1 *= r1/r0;
			}
			if( r1<rMin ) rMin = r1;
			if( r1>r ) continue;
			return true;
		}
		return false;
	}
	public Rectangle2D computeBounds() {
		Rectangle2D.Double bounds = new Rectangle2D.Double();
		if( points.size()==0 ) return bounds;
		double[] xyz = (double[])points.get(0);
		bounds.x = xyz[0];
		bounds.y = xyz[1];
		for( int i=1 ; i<points.size() ; i++ ) {
			xyz = (double[])points.get(i);
			if( xyz[0]<bounds.x ) {
				bounds.width += bounds.x-xyz[0];
				bounds.x = xyz[0];
			} else if( xyz[0]>bounds.x+bounds.width ) {
				bounds.width = xyz[0]-bounds.x;
			}
			if( xyz[1]<bounds.y ) {
				bounds.height += bounds.y-xyz[1];
				bounds.y = xyz[1];
			} else if( xyz[1]>bounds.y+bounds.height ) {
				bounds.height = xyz[1]-bounds.y;
			}
		}
		return bounds;
	}
	public void setSelected( boolean tf ) {
		if( selected == tf ) return;
		if( !selected ) {
			map.addMouseListener(this);
			map.addMouseMotionListener(this);
			dig.table.setModel( this );
		} else {
			map.removeMouseListener(this);
			map.removeMouseMotionListener(this);
		}
		selected = tf;
	}
	public boolean isSelected() {
		return selected;
	}
	public Icon getIcon() {
		return icon;
	}
	public Icon getDisabledIcon() {
		return invisibleIcon;
	}
	public void draw( java.awt.Graphics2D g ) {
		currentSeg=null;
		if( !visible || points.size()<=0 ) return;
		lineWidth = stroke.getLineWidth();
		g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ));
		GeneralPath path = new GeneralPath();
		double[] xyz = (double[])points.get(0);
		double min = xyz[0];
		double max = xyz[1];
		path.moveTo( (float)xyz[0], (float)xyz[1] );
		GeneralPath path1 = new GeneralPath();
		double dx = 2./map.getZoom();
		if( selected || active) 
			path1.append( new Rectangle2D.Double( xyz[0]-dx, xyz[1]-dx, 2*dx, 2*dx), false);
		for(int i=1 ; i<points.size() ; i++) {
			xyz = (double[])points.get(i);
			if( xyz[0]>max ) max=xyz[0];
			else if( xyz[0]<min ) min=xyz[0];
			path.lineTo( (float)xyz[0], (float)xyz[1] );
			if( selected || active ) 
				path1.append( new Rectangle2D.Double( xyz[0]-dx, xyz[1]-dx, 2*dx, 2*dx), false);
		}
		
		// Draw a star at the coords in a selected table row
		ArrayList<Shape> stars = new ArrayList<Shape>();
		int[] rows = dig.table.getSelectedRows();
		for (int row: rows) {
			if (row != -1) {
				float lon = Float.parseFloat((String) dig.table.getValueAt(row,0));
				float lat = Float.parseFloat((String) dig.table.getValueAt(row,1));
				Point2D pt = map.getProjection().getMapXY( new Point2D.Double(lon, lat) );
				double x_sel = pt.getX();
				double y_sel = pt.getY();
		
				if( x_sel>max ) max=x_sel;
				else if( x_sel<min ) min=x_sel;
				
				double width = 10/map.getZoom();
				Rectangle2D rect = map.getClipRect2D();
				double wrap = map.getWrap();
				if (wrap > 0f) {
					while(x_sel>rect.getX() ) x_sel -= wrap;
					while(x_sel< rect.getX() ) x_sel += wrap;
				}
				
				Shape star = UnknownDataSet.createStar(5, new Point2D.Double(x_sel,y_sel), width/2.0, width/5.0);
				stars.add(star);
			}
		}
		// draw the paths
		if( wrap>0. ) {
			AffineTransform at = g.getTransform();
			Rectangle2D rect = map.getClipRect2D();
			double offset = 0.;
			while( min+offset>rect.getX() ) offset -= wrap;
			while( max+offset< rect.getX() ) offset += wrap;
			g.translate( offset, 0.);
			while( min+offset < rect.getX()+rect.getWidth() ) {
				if( fill != null ) {
					g.setColor(fill);
					g.fill( path );
				}
				if( color!=null ) {
					g.setColor(color);
					g.draw( path );
					g.draw(path1);
				}
				offset += wrap;
				g.translate( wrap, 0.);
			}
			g.setTransform( at);
		} else {
			if( fill != null ) {
				g.setColor(fill);
				g.fill( path );
			}
			if( color!=null ) {
				g.setColor(color);
				g.draw( path );
				g.draw(path1);
			}
		}
		if( !selected ) return;
		// draw the selected path with point markers and a star for a selected table row coords
		g.setXORMode( Color.white );
		g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ));
		if( wrap>0. ) {
			AffineTransform at = g.getTransform();
			Rectangle2D rect = map.getClipRect2D();
			double offset = 0.;
			while( min+offset>rect.getX() ) offset -= wrap;
			while( max+offset< rect.getX() ) offset += wrap;
			g.translate( offset, 0.);
			while( min+offset < rect.getX()+rect.getWidth() ) {
				g.draw( path );
				g.draw( path1 );
				if (stars.size() > 0) {
					Color col = g.getColor();
					g.setPaintMode();
					for (Shape star : stars) {
						g.setColor(Color.RED);
						g.fill(star);
						g.setColor(Color.RED);
						g.draw(star);
					}
					g.setColor(col);
					g.setXORMode( Color.white );
				}
				offset += wrap;
				g.translate( wrap, 0.);
			}
			g.setTransform( at);
		} else {
			g.draw( path );
			g.draw( path1 );
			if (stars.size() > 0) {
				Color col = g.getColor();
				g.setPaintMode();
				for (Shape star : stars) {
					g.setColor(Color.RED);
					g.fill(star);
					g.setColor(Color.BLUE);
					g.draw(star);
				}
				g.setColor(col);
				g.setXORMode( Color.white );
			}
		}
		g.setPaintMode();
	}
	public void mouseEntered( MouseEvent evt ) {
		mouseMoved(evt);
	}
	public void mouseExited( MouseEvent evt ) {
		drawSeg();
		currentSeg = null;
	}
	public void mousePressed( MouseEvent evt ) {
		//finish drawing segment on double click
		if (evt.getClickCount() == 2 && !evt.isConsumed() && dig.startStopBtn.isSelected()) {
			evt.consume();
			dig.finish();
			return;
		}
		
        if (dig.startStopBtn.isSelected()){
			dig.startStopBtn.setForeground(Color.RED);
        	dig.startStopBtn.setText("Stop digitizing");
        } 
		if( evt.isControlDown() || !selected ) return;
		double zoom = map.getZoom();
		double r = 5/zoom;
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		double wrap = map.getWrap();
		for( int i=0 ; i<points.size() ; i++) {
			double[] xyz = (double[])points.get(i);
			if( p.y<xyz[1]-r || p.y>xyz[1]+r )continue;
			currentOffset = 0.;
			if( wrap>0. ) {
				while( p.x<xyz[0]-wrap/2. ) {
					currentOffset -= wrap;
					p.x+=wrap;
				}
				while( p.x>xyz[0]+wrap/2. ) {
					currentOffset += wrap;
					p.x-=wrap;
				}
			}
			if( p.x<xyz[0]-r || p.x>xyz[0]+r )continue;
			when = evt.getWhen();
			currentPoint = i;
			//highlight in table
			try {
				dig.table.setRowSelectionInterval(currentPoint, currentPoint);
		        dig.deletePtsBtn.setEnabled(true);	
				if (SwingUtilities.isRightMouseButton(evt)) {
					rightClickMenu.show(evt.getComponent(), evt.getX(), evt.getY());
				} else {
					mouseDragged( evt );
				}
				return;
				} 
			catch(Exception e) {
				System.out.println(e);
				continue;
			}
			
		}
		currentPoint = -1;
	}
	public void mouseReleased( MouseEvent evt ) {
		if( editShape==null ) {
			currentPoint = -1;
			return;
		}
		drawEdit();
		editShape = null;
		if( evt.isControlDown() || evt.getWhen()-when<500L ) {
			currentPoint = -1;
			return;
		}
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		double[] xyz = (double[])points.get( currentPoint );
		xyz[0] = p.x - currentOffset;
		xyz[1] = p.y;
		xyz[2] = getZ(p);
		fireTableRowsUpdated(currentPoint, currentPoint);
		dig.makeProfile();
		map.repaint();
		
	}
	public void mouseClicked( MouseEvent evt ) {
		if(!active || evt.isControlDown())return;
		drawSeg();
		Point2D.Double p = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		if( points.size()>0 ) {
			double[] xyz1 = (double[])points.get(points.size()-1);
			if( p.x==xyz1[0] && p.y==xyz1[1] ) return;
		}
		double z = getZ( p );
		double[] xyz = new double[] { p.x, p.y, z};
		points.add( xyz );
		fireTableStructureChanged();
		// update the interpolated points and profile every time a new point is added
		dig.makeProfile();
		redraw();
	}
	public double getZ(Point2D p) {
		if( map.getFocus()==null )return Double.NaN;
		return map.getFocus().getZ(map.getProjection().getRefXY(p));
	}
		
	void getProfile() {
		if( points.size()<1 ) return;
		if (points.size() == 1) {
			//need this if deleting and down to one point
			profile = new Vector();
			double z = ((double[])points.get(0))[2];
			profile.add( new float[] { 0f, 0f,(float) z});	
			return;
		}
		grid = map.getFocus();
		if( grid == null || grid.getGrid() == null) return;
		Projection proj = map.getProjection();
		profile = new Vector();
		double x = 0.;
		double[] xyz = (double[])points.get(0);
		Point2D.Double p0 = new Point2D.Double( xyz[0], xyz[1] );
		Point2D.Double p1;
		// sum up the distance between points (in pixels?) in a flat mercator projection 
		for( int k=1 ; k<points.size() ; k++ ) {
			xyz = (double[])points.get(k);
			p1 = new Point2D.Double( xyz[0], xyz[1] );
			//take into account crossing the wrap boundary
			GeneralUtils.wrapPoints(map, p0, p1);
			x += Math.sqrt( Math.pow(p1.x-p0.x,2) + Math.pow(p1.y-p0.y,2) );
			p0 = p1;
		}
		// number of interpolated points - at least 100
		int n = (int)Math.ceil( x+1. );
		if (n < 100) n = 100;
		// distance between interpolated points
		double dx = x/n;
		x = 0.;
		xyz = (double[])points.get(0);
		// start point in lat/lon
		p0 = new Point2D.Double( xyz[0], xyz[1] );
		Point2D.Double point0 = (Point2D.Double)proj.getRefXY(p0);
		// end of first segment in lat/lon
		xyz = (double[])points.get(1);
		p1 = new Point2D.Double( xyz[0], xyz[1] );
		Point2D.Double point1 = (Point2D.Double)proj.getRefXY(p1);

		
		//need copy of original map coords as when we correct for wrap boundary, we can no longer
		//convert back to latlon
		Point2D.Double p0_orig = new Point2D.Double(p0.x, p0.y);
		Point2D.Double p1_orig = new Point2D.Double(p1.x, p1.y);
		
		//take into account crossing the wrap boundary
		GeneralUtils.wrapPoints(map, p0, p1);
		
		int kk = 1;
		double[] xx = new double[] {0., Math.sqrt( Math.pow(p1.x-p0.x,2) + Math.pow(p1.y-p0.y,2) )};
		double distance = 0.;
		for( int k=0 ; k<=n ; k++) {
			x = k*dx;
			// work out which segment we are in
			if( x>xx[1] && kk<points.size()-1) {
				p0 = p1;
				p0_orig.setLocation(p1_orig);
				kk++;
				xx[0] = xx[1];
				xyz = (double[])points.get(kk);
				p1 = new Point2D.Double( xyz[0], xyz[1] );
				p1_orig.setLocation(p1);
				//take into account crossing the wrap boundary
				GeneralUtils.wrapPoints(map, p0, p1);
				xx[1] += Math.sqrt( Math.pow(p1.x-p0.x,2) + Math.pow(p1.y-p0.y,2) );
			}
			// fractional distance along the segment 
			double d = (x-xx[0])/(xx[1]-xx[0]);
			// get the coords for the point at that fractional distance
			Point2D.Double pt = new Point2D.Double( 
					p0_orig.x + (p1.x-p0.x)*d,
					p0_orig.y + (p1.y-p0.y)*d );
			// convert to lat/lon
			point1 = (Point2D.Double)proj.getRefXY( pt );
			if( k!=0 ) {
				// add to the distance summation
				Point2D[] pts = {(Point2D)point0, (Point2D)point1};
				distance += GeneralUtils.distance(pts);
			}
			point0 = point1;
			// get the z value at the point
			float z = map.getFocus().getZ(point1);
			profile.add( new float[] { (float)distance, 
						(float)(kk-1+d), 
						z});
		}
	}
	
	public double[] locationAtDist( float dist ) {
		if( profile==null ) return null;
		int k=0;
		float[] point = (float[])profile.get(0);
		float[] lastP = point;
		for( k=1 ; k<profile.size() ; k++) {
			point = (float[])profile.get(k);
			if( point[0]>dist || k==profile.size()-1 ) break;
			lastP = point;
		}
		float dx = lastP[1] + (point[1]-lastP[1]) * (dist-lastP[0]) / (point[0]-lastP[0]);
		double x = (double)dx;
		k = (int)Math.floor(x);
		x -= k;
		if( k>= points.size() ) {
			k = points.size()-2;
			x = 1.;
		}
		double[] p1 = (double[])points.get(k);
		double[] p2 = (double[])points.get(k+1);
		double[] xy = new double[] {
			p1[0]+x*(p2[0]-p1[0]), p1[1]+x*(p2[1]-p1[1]) };
		return xy;
	}
	public boolean isActive() {
		return active;
	}
	public void redraw() {
		synchronized( map.getTreeLock() ) {
			draw( map.getGraphics2D() );
		}
	}
	public void mouseMoved( MouseEvent evt ) {
		if(selected || evt.isControlDown())return;
		if( map.getCursor() != cursor ) map.setCursor( cursor );
		if( points.size()==0 )return;
		drawSeg();
		double[] xyz = (double[])points.get(points.size()-1);
		Point2D.Double pt = new Point2D.Double(xyz[0], xyz[1]);
		currentSeg = new Line2D.Double( pt,
				map.getScaledPoint( evt.getPoint() ) );
		drawSeg();
	}
	public void mouseDragged( MouseEvent evt ) {
		if( currentPoint < 0)return;
		drawEdit();
		if( evt.isControlDown() ) {
			currentPoint=-1;
			return;
		}		
		editShape = new GeneralPath();
		Point2D.Double p0 = (Point2D.Double)map.getScaledPoint( evt.getPoint() );
		if( currentPoint!=0 ) {
			double[] xyz = (double[])points.get(currentPoint-1);
			editShape.append(new Line2D.Double( p0, 
					new Point2D.Double(xyz[0]+currentOffset, xyz[1])), false);
		}
		if( currentPoint!=points.size()-1 ){
			double[] xyz = (double[])points.get(currentPoint+1);
			editShape.append(new Line2D.Double( p0, 
					new Point2D.Double(xyz[0]+currentOffset, xyz[1])), false);
		}
		double r = 2.5/map.getZoom();
		editShape.append( new Rectangle2D.Double( p0.x-r, p0.y-r, 2.*r, 2.*r ), false);
		drawEdit();	

		// show dragged lat/lon on table
		points.set(currentPoint,  new double[]{p0.x, p0.y, getZ(p0)});
		fireTableDataChanged();
		updateAll();
	}
	public void drawSeg() {
		if( currentSeg==null ) return;
		synchronized( map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke( 2f/(float)map.getZoom() ));
			g.setXORMode( Color.white);
			double max = currentSeg.x1;
			double min = currentSeg.x1;
			if( currentSeg.x2>max ) max=currentSeg.x2;
			else min=currentSeg.x2;
			if( wrap>0. ) {
				Rectangle2D rect = map.getClipRect2D();
				double offset = 0.;
				while( min+offset>rect.getX() ) offset -= wrap;
				while( max+offset< rect.getX() ) offset += wrap;
				g.translate( offset, 0.);
				while( min+offset < rect.getX()+rect.getWidth() ) {
					g.draw( currentSeg );
					offset += wrap;
					g.translate( wrap, 0.);
				}
			} else {
				g.draw( currentSeg );
			}
		}
	}
	public void drawEdit() {
		if( editShape==null ) return;
		synchronized( map.getTreeLock() ) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke( 1f/(float)map.getZoom() ));
			g.setXORMode( Color.white);
			Rectangle2D bounds = editShape.getBounds2D();
			double min = bounds.getX();
			double max = min + bounds.getWidth();
			if( wrap>0. ) {
				Rectangle2D rect = map.getClipRect2D();
				double offset = 0.;
				while( min+offset>rect.getX() ) offset -= wrap;
				while( max+offset< rect.getX() ) offset += wrap;
				g.translate( offset, 0.);
				while( min+offset < rect.getX()+rect.getWidth() ) {
					g.draw( editShape );
					offset += wrap;
					g.translate( wrap, 0.);
				}
			} else {
				g.draw( editShape );
			}
		}
	}
	public void keyPressed( KeyEvent evt ) {
	}
	public void keyReleased( KeyEvent evt ) {
		if( evt.getKeyCode() == evt.VK_ENTER ) dig.finish();
		if( evt.getKeyCode() == evt.VK_BACK_SPACE) {
			if( points.size()>0 ) {
				points.remove( points.size()-1 );
				fireTableStructureChanged();
				map.repaint();
			}
		}
	}
	public void keyTyped( KeyEvent evt ) {
	}
	public void setTable( int which ) {
		if( which == 1&& (profile==null || profile.size()==0) )return;
		table = which;
		fireTableStructureChanged();
	}
	public String getColumnName(int col) {
		if (col == 0) return "Longitude";
		else if (col == 1) return "Latitude";
//		else if (col == 3) return "Distance between points (km)";
//		else if (col == 4) return "Cumulative distance (km)";
		if (grid == null) grid = map.getFocus();
		if (grid != null && grid.getUnits() != null && grid.getUnits() != "") return grid.getDataType() + " (" + grid.getUnits() + ")";
		return null;
	}	
	public int getRowCount() {
		if ( points == null && profile == null) return 0;
		if( table==0 ) return points.size();
		return profile.size();
	}
	public int getColumnCount() {
		return 3;
	}
	
	public Object getValueAt(int row, int column) {
		return getValueAt(row, column, table);
	}
	
	public Object getValueAt(int row, int column, int whichTable) {
		// determine number of decimal places based on the zoom level
		double zoom = map.getZoom();
		double xmin = map.getClipRect2D().getMinX();
		NumberFormat fmt = GeneralUtils.getNumberFormat(zoom);

		if( whichTable==1 ) {
			float[] dxz = (float[])profile.get(row);
			if( column==2 ) {
				if (Float.isNaN((float)dxz[2])) return "-";
				return new Float(dxz[2]);
			}
			if (column == 4) return (int) dxz[0];
			Point2D.Double pt = interpolatePoint(row);
			if (column == 0) return fmt.format(pt.getX());
			if (column == 1) return fmt.format(pt.getY());
			if (column == 3) {
				if (row == 0) return 0;
				Point2D.Double prev_pt = interpolatePoint(row-1);
				Point2D.Double [] pts = {prev_pt, pt};
				return fmt.format(GeneralUtils.distance(pts));
			}
			return "TBD";

		}
		if( row>=points.size() ) return null;
		double[] p = (double[])points.get(row);

		Point2D pt = map.getProjection().getRefXY( new Point2D.Double(p[0], p[1]) );
		if (column == 0)  return fmt.format(pt.getX());
		else if (column == 1) return fmt.format(pt.getY());
		else if(column == 2) {
			if (Float.isNaN((float)p[2])) return "-";
			return new Float(p[2]);
		}
		else if (column == 3) {
			double[] prev_p = (double[])points.get(row);
			if (row > 0) {
				prev_p = (double[])points.get(row-1);
			}
			Point2D prev_pt = map.getProjection().getRefXY( new Point2D.Double(prev_p[0], prev_p[1]) );
			Point2D[] pts = {prev_pt, pt};
			
			boolean longWay = false;
			if (( p[0] > wrap + xmin && prev_p[0] < wrap + xmin) || ( prev_p[0] > wrap + xmin && p[0] < wrap + xmin)) {
				longWay = true;
			}
			return (int) GeneralUtils.distance(pts, longWay);
		}
		else if (column == 4) {
			if (row == 0) return 0;
			double d = 0.;
			boolean longWay = false;
			for (int i=1; i<=row; i++) {
				double[] this_p = (double[])points.get(i);
				double[] prev_p = (double[])points.get(i-1);
				if (( this_p[0] > wrap + xmin && prev_p[0] < wrap + xmin ) || ( prev_p[0] > wrap + xmin && this_p[0] < wrap + xmin )) {
					longWay = true;
				} else {
					longWay = false;
				}
				
				Point2D this_pt = map.getProjection().getRefXY( new Point2D.Double(this_p[0], this_p[1]) );
				Point2D prev_pt = map.getProjection().getRefXY( new Point2D.Double(prev_p[0], prev_p[1]) );
				Point2D[] pts = {prev_pt, this_pt};
				d += GeneralUtils.distance(pts, longWay);
			}
			return (int) d;
		}
		return "TBD";
	}

	// convert interpolated point to lat lon
	private Point2D.Double interpolatePoint(int ind) {
		float[] dxz = (float[])profile.get(ind);
		int i = (int)Math.floor( dxz[1] );
		if( i==points.size()-1 ) i--;
		if (i > -1) {
			double[] xyz1 = (double[])points.get(i);
			double[] xyz2 = (double[])points.get(i+1);
			double d = (double)(dxz[1]-i);
			Point2D.Double pt = (Point2D.Double) map.getProjection().getRefXY( new Point2D.Double(
					xyz1[0]+(xyz2[0]-xyz1[0])*d,
					xyz1[1]+(xyz2[1]-xyz1[1])*d) );
			return pt;
		}
		return null;
	}
	
	
	// reorder the table rows
	public void reorder(int fromIndex, int toIndex, int numRows) {
		// remove the points to be moved and store them in a list
		ArrayList<Object> movingPts = new ArrayList<Object>();
		for (int i = 0; i < numRows; i++) {
			// when a point is removed, the indices get re-assigned,  
			// so we are always removing points[fromIndex]
			movingPts.add(points.remove(fromIndex));
		}
		// add the moved points back at their new position
		if (toIndex > points.size()) {
			points.add(movingPts);
		} else {			
			points.addAll(toIndex, movingPts);
		}
		// update profile, graph and tables
		fireTableDataChanged();
		updateAll();
	}
	
	// update profile, graph and tables
	public void updateAll() {
		getProfile();
		dig.profile.setLine( this );
		dig.graph.setPoints(dig.profile, 0);
		dig.table.revalidate();
		dig.tableSP.revalidate();
		dig.graph.revalidate();
		dig.graph.repaint();
		map.repaint();
		dig.table.clearSelection();
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		if (dig.tabs[0].isSelected()) {
			if (column >= 2) return false;
			return true;
		}
		return false;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {

		if(row>=points.size()) return;
		Double val;
		//check value is a double
		try {
			val = Double.parseDouble(value.toString());
		} catch(Exception e) {
			return;
		}
		
		double[] p = (double[])points.get(row);
		Point2D pt = map.getProjection().getRefXY( new Point2D.Double(p[0], p[1]) );
		Point2D mapPt = map.getProjection().getMapXY(pt);
		// need to convert lat/lon displayed in table to map coords
		// that are stored in points vector
		if( col==0 ) {
			mapPt = map.getProjection().getMapXY(val, pt.getY());
			GeneralUtils.wrapPoint(map, (Point2D.Double) mapPt);
		} else {
			// make sure latitude is in range
			if (val < -90 || val > 90) return;
			// getMapXY can;t handle 90 deg.
			if (val == 90) val = 89.9999;
			mapPt = map.getProjection().getMapXY(pt.getX(), val);
		}
		p = new double[]{mapPt.getX(), mapPt.getY(), getZ(mapPt)};
		points.set(row, p);
		// update profile, graph and tables
		updateAll();
	}	
}

package haxby.db.surveyplanner;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import haxby.map.Overlay;
import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;

public class SurveyLine implements Overlay {
	private double startLat, startLon, endLat, endLon, startDepth, endDepth, cumulativeDistance, duration;
	private int lineNum;
	private static int counter = 0;
	private XMap map;
	private static double totalDistance = 0;
	private static double speed = 0;
	
	public SurveyLine(XMap map) {
		this(map, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
	}
	
	public SurveyLine(XMap map, double startLat, double startLon, double endLat, double endLon) {
		this(map, startLat, startLon, Double.NaN, endLat, endLon, Double.NaN);
	}
	
	public SurveyLine (XMap map, double startLat, double startLon, double startDepth, 
			double endLat, double endLon, double endDepth) {
		this.map = map;
		this.startLat = startLat;
		this.startLon = startLon;
		this.startDepth = startDepth;
		this.endLat = endLat;
		this.endLon = endLon;
		this.endDepth = endDepth;
		this.lineNum = ++counter;
		this.cumulativeDistance = calculateCumulativeDistance();
		this.duration = calculateDuration();
	}
	
	public void setDepths (double startDepth, double endDepth) {
		setStartDepth(startDepth);
		setEndDepth(endDepth);
	}

	public double getGapFromDepths(float swathAngle, float overlap) {
		double meanDepth = (startDepth + endDepth) /-2000d;
		//calculate line spacing based on swath angle and depth
		double gap = (double)Math.round((2d * meanDepth * Math.tan(Math.toRadians(swathAngle / 2d))) * 1000d) / 1000d;
		//take in to account overlap
		gap = gap * (100 - overlap)/100d;
		//round to 3dp
		gap = (double)Math.round(gap * 1000d)/1000d;
		return gap;
	}

	public double calculateCumulativeDistance() {
		Point2D lineStart = getStartPoint();
		Point2D lineEnd = getEndPoint();
		Point2D[] pts = {lineStart, lineEnd};
		totalDistance += GeneralUtils.distance(pts);	
		return totalDistance;
	}

	public double calculateDuration() {
		if (speed == 0) return 0;
		duration = cumulativeDistance / (speed * GeneralUtils.KNOTS_2_KPH);
		return (double)Math.round(duration * 100d) / 100d;
	}
	
	public int getCumulativeDistance() {
		return (int) cumulativeDistance;
	}
	
	public double getDuration() {
		return duration;
	}
	
	public double getStartLat() {
		return startLat;
	}
	
	public double getStartLon() {
		return startLon;
	}
	
	public double getStartDepth() {
		return startDepth;
	}
	
	public double getEndLat() {
		return endLat;
	}
	
	public double getEndLon() {
		return endLon;
	}
	
	public double getEndDepth() {
		return endDepth;
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	public static double getSpeed() {
		return speed;
	}
	
	public void setStartLat(double startLat) {
		this.startLat = startLat;
	}
	
	public void setStartLon(double startLon) {
		this.startLon = startLon;
	}
	
	public void setStartDepth(double startDepth) {
		this.startDepth = startDepth;
	}
	
	public void setEndLat(double endLat) {
		this.endLat = endLat;
	}
	
	public void setEndLon(double endLon) {
		this.endLon = endLon;
	}
	
	public void setEndDepth (double endDepth) {
		this.endDepth = endDepth;
	}

	public void setLineNum (int lineNum) {
		this.lineNum = lineNum;
	}
	
	public void setCumulativeDistance (double distance) {
		this.cumulativeDistance = distance;
	}
	
	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	public static void setSpeed(double s) {
		speed = s;
	}
	
	public static void resetCounter() {
		counter = 0;
	}
	
	public static void resetTotalDistance() {
		totalDistance = 0;
	}
	
	public static void resetAll() {
		resetCounter();
		resetTotalDistance();
		speed = 0d;
	}
	
	public Point2D.Double getStartPoint() {
		return new Point2D.Double(startLon, startLat);
	}
	
	public Point2D getEndPoint() {
		return new Point2D.Double(endLon, endLat);
	}
	
	@Override
	public void draw(Graphics2D g) {
		float wrap = (float)map.getWrap();
		
		//get the limits of the displayed map
		Rectangle2D rect = map.getClipRect2D();
		double xmin = rect.getMinX();
		double xmax = rect.getMaxX();
		
		Projection proj = map.getProjection();
		Point2D.Double pt = new Point2D.Double();
		pt.x = getStartLon();
		pt.y = getStartLat();
		Point2D.Double p_start = (Point2D.Double) proj.getMapXY(pt);

		if( wrap>0f ) {
			while( p_start.x <= xmin ){p_start.x+=wrap;}
			while( p_start.x >= xmax ){p_start.x-=wrap;}
		}
		
		pt.x = getEndLon();
		pt.y = getEndLat();
		Point2D.Double p_end = (Point2D.Double) proj.getMapXY(pt);

		if( wrap>0f ) {
			while( p_end.x <= xmin ){p_end.x+=wrap;}
			while( p_end.x > wrap + xmin ){p_end.x-=wrap;}
		}
		
		//draw the shortest line - either p_start.x to p_end.x or the x+wrap values. 
		if ( ((p_start.x - p_end.x) * (p_start.x - p_end.x)) > 
			((p_start.x - (p_end.x + wrap)) * (p_start.x - (p_end.x + wrap))) )  {p_end.x += wrap;}
		if ( ((p_start.x - p_end.x) * (p_start.x - p_end.x)) > 
		(((p_start.x + wrap) - p_end.x) * ((p_start.x + wrap) - p_end.x)) )  {p_start.x += wrap;}
		
		if (lineNum == 1) g.setColor( Color.RED ); 
		else g.setColor( Color.black );
		
		double zoom = map.getZoom();
		g.setStroke( new BasicStroke( 2f/(float)zoom ));

    	double arr_size = 6./zoom;
    	double sq_size = 6./zoom;
        double dx = p_end.x - p_start.x, dy = p_end.y - p_start.y;
        double angle = Math.atan2(dy, dx);
        double len = Math.sqrt(dx*dx + dy*dy);

        AffineTransform at = g.getTransform(); 
        g.translate(p_start.x, p_start.y);
        g.rotate(angle);

        //draw the line
        g.draw(new Line2D.Double(0, 0, len, 0));
        //draw the arrow
        Path2D arrow = new Path2D.Double();
        double xpts[] = {len/3d, len/3d-arr_size, len/3d-arr_size, len/3d};
        double ypts[] = {0, -arr_size, arr_size, 0};
        arrow.moveTo(xpts[0], ypts[0]);
        arrow.lineTo(xpts[1], ypts[1]);
        arrow.lineTo(xpts[2], ypts[2]);
        arrow.closePath();
		if (lineNum == 1) g.setColor( Color.ORANGE ); 
		else g.setColor( Color.ORANGE );
        g.fill(arrow);
		if (lineNum == 1) g.setColor( Color.RED ); 
		else g.setColor( Color.black );
        //draw squares at the end points
        Rectangle2D startSq = new Rectangle2D.Double(0d-sq_size/2d, 0d-sq_size/2d, sq_size, sq_size);
        Rectangle2D endSq = new Rectangle2D.Double(len-sq_size/2d, 0d-sq_size/2d,  sq_size, sq_size);
        g.fill(startSq);
        g.fill(endSq);

        g.setTransform(at);
        
	}
}

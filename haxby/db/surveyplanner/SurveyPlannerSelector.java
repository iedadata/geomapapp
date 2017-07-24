package haxby.db.surveyplanner;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.geomapapp.geom.XYZ;
import org.geomapapp.util.Cursors;

import haxby.map.XMap;
import haxby.proj.Projection;
import haxby.util.GeneralUtils;

public class SurveyPlannerSelector implements ActionListener, MouseListener, MouseMotionListener, KeyListener {
	private JPanel dialog;
	private JButton submitB, clearB, helpB;
	private JTextField startLatTF, startLonTF, endLatTF, endLonTF, numLinesTF, gapTF, speedTF,
						swathTF, overlapTF;
	private JCheckBox autoGapCB;
	private JRadioButton waypointsRB, surveyLinesRB, mouseRB, manualRB, NERB, NWRB, SERB, SWRB,
						straightLineRB, greatCircleRB, importLinesRB;
	JRadioButton[] dirRBs;
	private JLabel firstLineL, startL, endL, generateL, swathL, overlapL, gapL;
	private ButtonGroup surveyBG, firstLineBG;
	private SurveyPlanner sp;
	private XMap map;
	public static final float EARTH_RADIUS = 6371;
	public static final double EARTH_CIRCUMFERENCE = 2 * Math.PI * EARTH_RADIUS;
	private Point p0;
	private Line2D currentLine, line;
	private GeneralPath currentPath;
	private NumberFormat df = new DecimalFormat("#0.00000");
	private NumberFormat dp3f = new DecimalFormat("#0.000");
	private boolean autogap = false;
	private static Dimension compDim = new Dimension(250, 30); 
	
	public SurveyPlannerSelector(SurveyPlanner sp) {
		this.sp = sp;
		map = sp.getMap();
		map.addMouseListener(this);
		map.addMouseMotionListener(this);

		initDialog();
	}

	/**
	 * align the component to the left side of the panel
	 * @param comp
	 */
	void alignComponent(JComponent comp) {
		comp.setAlignmentX(Component.LEFT_ALIGNMENT);
		comp.setPreferredSize(compDim);
		comp.setMaximumSize(compDim);
	}
	
	void initDialog() {
		dialog = new JPanel( new BorderLayout() );
		dialog.setPreferredSize(new Dimension(250,600));
		dialog.setMaximumSize(new Dimension(250,600));
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));

		helpB = new JButton("Help");
		helpB.addActionListener(this);
		alignComponent(helpB);
		panel1.add(helpB);
		
		waypointsRB = new JRadioButton("Generate waypoints");
		alignComponent(waypointsRB);
		waypointsRB.setActionCommand("waypoints");
		waypointsRB.addActionListener(this);
		waypointsRB.setEnabled(false);
		surveyLinesRB = new JRadioButton("Generate survey lines");
		alignComponent(surveyLinesRB);
		surveyLinesRB.setActionCommand("lines");
		surveyLinesRB.addActionListener(this);
		surveyLinesRB.setSelected(true);
		importLinesRB = new JRadioButton("Import survey lines");
		alignComponent(importLinesRB);
		importLinesRB.setActionCommand("import");
		importLinesRB.addActionListener(this);
		surveyBG = new ButtonGroup();
		surveyBG.add(waypointsRB);
		surveyBG.add(surveyLinesRB);
		surveyBG.add(importLinesRB);
		panel1.add(waypointsRB);
		panel1.add(surveyLinesRB);
		panel1.add(importLinesRB);
		panel1.add(Box.createRigidArea(new Dimension(0,10)));
		
		firstLineL = new JLabel("<html><strong>Define first survey line:</strong></html>");
		alignComponent(firstLineL);
		panel1.add(firstLineL);
		JPanel firstLineP = new JPanel();
		firstLineP.setLayout(new BoxLayout(firstLineP, BoxLayout.LINE_AXIS));
		alignComponent(firstLineP);
		manualRB = new JRadioButton("Manually");
		manualRB.setActionCommand("manual");
		manualRB.addActionListener(this);
		manualRB.setSelected(true);
		mouseRB = new JRadioButton("Use mouse");
		mouseRB.setActionCommand("mouse");
		mouseRB.addActionListener(this);
		firstLineBG = new ButtonGroup();
		firstLineBG.add(manualRB);
		firstLineBG.add(mouseRB);
		firstLineP.add(manualRB);
		firstLineP.add(mouseRB);
		panel1.add(firstLineP);
		
		startL = new JLabel("Start point:");
		alignComponent(startL);
		panel1.add(startL);
		JPanel startP = new JPanel();
		alignComponent(startP);
		startP.setLayout(new BoxLayout(startP, BoxLayout.LINE_AXIS));
		JLabel startLatL = new JLabel("Lat");
		startLatTF = new JTextField(5);
		startLatTF.addKeyListener(this);
		JLabel startLonL = new JLabel("Lon");
		startLonTF = new JTextField(5);
		startLonTF.addKeyListener(this);
		startP.add(startLatL);
		startP.add(startLatTF);
		startP.add(startLonL);
		startP.add(startLonTF);
		panel1.add(startP);
		
		endL = new JLabel("End point:");
		alignComponent(endL);
		panel1.add(endL);
		JPanel endP = new JPanel();
		alignComponent(endP);
		endP.setLayout(new BoxLayout(endP, BoxLayout.LINE_AXIS));
		JLabel endLatL = new JLabel("Lat");
		endLatTF = new JTextField(5);
		endLatTF.addKeyListener(this);
		JLabel endLonL = new JLabel("Lon");
		endLonTF = new JTextField(5);
		endLonTF.addKeyListener(this);
		endP.add(endLatL);
		endP.add(endLatTF);
		endP.add(endLonL);
		endP.add(endLonTF);
		panel1.add(endP);
		
		JPanel lineTypeP = new JPanel();
		lineTypeP.setLayout(new BoxLayout(lineTypeP, BoxLayout.LINE_AXIS));
		alignComponent(lineTypeP);
		straightLineRB = new JRadioButton("Straight Line");
		straightLineRB.setActionCommand("straightLine");
		straightLineRB.addActionListener(this);
		straightLineRB.setSelected(true);
		greatCircleRB = new JRadioButton("Great Circle");
		greatCircleRB.setActionCommand("greatCircle");
		greatCircleRB.addActionListener(this);
		greatCircleRB.setEnabled(false);
		ButtonGroup lineTypeBG = new ButtonGroup();
		lineTypeBG.add(straightLineRB);
		lineTypeBG.add(greatCircleRB);
		lineTypeP.add(straightLineRB);
		lineTypeP.add(greatCircleRB);
		panel1.add(lineTypeP);
		
		JPanel speedP = new JPanel();
		speedP.setLayout(new BoxLayout(speedP, BoxLayout.LINE_AXIS));		
		alignComponent(speedP);
		JLabel speedL = new JLabel("Ship speed (knots)");
		speedTF = new JTextField();
		speedTF.setPreferredSize(new Dimension(50,23));
		speedTF.setMaximumSize(new Dimension(50,23));
		speedP.add(speedL);
		speedP.add(speedTF);
		panel1.add(speedP);
		
		panel1.add(Box.createRigidArea(new Dimension(0,10)));
		generateL = new JLabel("<html><strong>Generate additional parallel lines</strong></html>");
		alignComponent(generateL);
		panel1.add(generateL);
		JPanel linesP = new JPanel();
		alignComponent(linesP);
		linesP.setLayout(new BoxLayout(linesP, BoxLayout.LINE_AXIS));
		JLabel numLinesL = new JLabel("Total number of lines");
		numLinesTF = new JTextField();
		numLinesTF.setPreferredSize(new Dimension(50,30));
		numLinesTF.setMaximumSize(new Dimension(50,30));
		numLinesTF.setText("1");
		numLinesTF.addKeyListener(this);
		linesP.add(numLinesL);
		linesP.add(numLinesTF);
		panel1.add(linesP);
		
		JPanel gapP = new JPanel();
		gapP.setLayout(new BoxLayout(gapP, BoxLayout.LINE_AXIS));
		alignComponent(gapP);
		gapL = new JLabel("Line spacing (km)      ");
		gapL.setEnabled(enableSpacing());
		gapTF = new JTextField(3);
		gapTF.setPreferredSize(new Dimension(50,23));
		gapTF.setMaximumSize(new Dimension(50,23));
		gapTF.setEnabled(enableSpacing());
		gapTF.addKeyListener(this);
		gapP.add(gapL);
		gapP.add(gapTF);
		panel1.add(gapP);
		
		autoGapCB = new JCheckBox("<html>Calculate spacing from depth<br>and multibeam sonar angle</html>");
		alignComponent(autoGapCB);
		autoGapCB.setPreferredSize(new Dimension(250,60));
		autoGapCB.setMaximumSize(new Dimension(250,60));
		autoGapCB.setSelected(autogap);
		autoGapCB.setActionCommand("autogap");
		autoGapCB.addActionListener(this);
		panel1.add(autoGapCB);

		JPanel swathP = new JPanel();
		swathP.setLayout(new BoxLayout(swathP, BoxLayout.LINE_AXIS));
		alignComponent(swathP);
		swathL = new JLabel("Swath Angle");
		swathL.setEnabled(autogap);
		swathTF = new JTextField("120");
		swathTF.setEnabled(autogap);
		swathTF.addKeyListener(this);
		overlapL = new JLabel("Overlap %");
		overlapL.setEnabled(autogap);
		overlapTF = new JTextField("10");
		overlapTF.setEnabled(autogap);
		overlapTF.addKeyListener(this);
		swathP.add(swathL);
		swathP.add(swathTF);
		swathP.add(overlapL);
		swathP.add(overlapTF);
		panel1.add(swathP);

		panel1.add(Box.createRigidArea(new Dimension(0,10)));
		JLabel dirL = new JLabel("<html><strong>Step additional lines to:</strong></html>");
		alignComponent(dirL);
		panel1.add(dirL);
		JPanel dirP = new JPanel();
		dirP.setLayout(new BoxLayout(dirP, BoxLayout.LINE_AXIS));
		alignComponent(dirP);
		NERB = new JRadioButton("NE");
		NERB.setSelected(true);
		NERB.setEnabled(false);
		NWRB = new JRadioButton("NW");
		NWRB.setSelected(true);
		NWRB.setEnabled(false);
		SERB = new JRadioButton("SE");
		SERB.setEnabled(false);
		SWRB = new JRadioButton("SW");
		SWRB.setEnabled(false);
		ButtonGroup newsBG1 = new ButtonGroup();
		newsBG1.add(NERB);
		newsBG1.add(SWRB);
		ButtonGroup newsBG2 = new ButtonGroup();
		newsBG2.add(NWRB);
		newsBG2.add(SERB);
		dirP.add(NERB);
		dirP.add(NWRB);
		dirP.add(SWRB);
		dirP.add(SERB);
		panel1.add(dirP);

		JPanel btnsP = new JPanel();
		alignComponent(btnsP);
		submitB = new JButton("Submit");
		submitB.addActionListener(this);
		submitB.setEnabled(false);
		btnsP.add(submitB);
		clearB = new JButton("Reset");
		clearB.addActionListener(this);
		clearB.setActionCommand("reset");
		btnsP.add(clearB);
		panel1.add(btnsP);
		

		
		dialog.add(panel1, "Center");
	
	}
	
	public JComponent getDialog() {
		return dialog;
	}
	
	/*
	 *check whether enough fields have been filled
	 *in the right format so that we can start calculating survey lines 
	 */
	private boolean readyToSubmit() {
		return (GeneralUtils.isDouble(startLatTF.getText()) &&
				GeneralUtils.isDouble(startLonTF.getText()) &&
				GeneralUtils.isDouble(endLatTF.getText()) &&
				GeneralUtils.isDouble(endLonTF.getText()) &&
				(GeneralUtils.isInteger(numLinesTF.getText()) && Integer.parseInt(numLinesTF.getText()) > 0) &&
				( !enableSpacing() ||	
				 (gapTF.isEnabled() && GeneralUtils.isDouble(gapTF.getText())) ||
				 (autoGapCB.isSelected() && GeneralUtils.isDouble(swathTF.getText()) &&
						 GeneralUtils.isDouble(overlapTF.getText()))
				));
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		String cmd = e.getActionCommand();
		if (cmd.equals("Submit")) {
			//remove all lines and clear table
			sp.clearLines();
			map.repaint();
			
			double startLat = Double.parseDouble(startLatTF.getText());
			double startLon = Double.parseDouble(startLonTF.getText());
			double endLat = Double.parseDouble(endLatTF.getText());
			double endLon = Double.parseDouble(endLonTF.getText());
			
			//if a ship speed has been entered, use this to calculate cruise duration
			try {
				double speed = Double.parseDouble(speedTF.getText());
				SurveyLine.setSpeed(speed);
			} catch(Exception ex) {}
			
			SurveyLine newLine = new SurveyLine(map, startLat, startLon, endLat, endLon);	
			sp.addLine(newLine);

			Graphics2D g = map.getGraphics2D();
			newLine.draw(g);
			
			int numLines = Integer.parseInt(numLinesTF.getText());
			
			double gap;
			if (autogap) {
				//calculate gap between lines based on water depth
				float swath = Float.parseFloat(swathTF.getText());
				float overlap = Float.parseFloat(overlapTF.getText());
				gap = newLine.getGapFromDepths(swath, overlap);
				gapTF.setText(Double.toString(gap));
			} else {
				if (gapTF.isEnabled()) {
					//read in from text field
					gap = Double.parseDouble(gapTF.getText());
				} else gap = 0;
			}
			
			double sLat, sLon, eLat, eLon;
			Point2D startPt = new Point2D.Double(startLon, startLat); 
			Point2D endPt = new Point2D.Double(endLon, endLat); 
			Point2D[] firstLine = {startPt, endPt};
			Point2D[] nextLine;
			
			//determine in which direction extra lines should be added
			byte dir = 1;
			double bearing = GeneralUtils.bearing(firstLine);
			if (NWRB.isEnabled()) {
				if (NWRB.isSelected()) {	
					if (bearing >= 0 && bearing <= 180) {
						dir = -1;
					}
					else dir = 1;
				} else {
					if (bearing >= 0 && bearing <= 180) {
						dir = 1;
					} 
					else dir = -1;
				}
			} else if (NERB.isEnabled()) {
				if (NERB.isSelected()) {	
					if (bearing >= 0 && bearing <= 180) {
						dir = -1;
					}
					else dir = 1;
				} else {
					if (bearing >= 0 && bearing <= 180) {
						dir = 1;
					} 
					else dir = -1;
				}
			} 

			
			for (int i=1; i<numLines; i++) {
				//find the next parallel line
				nextLine = GeneralUtils.parallelLine(firstLine, gap, dir);
				//round to 5 dp
				sLat = (double)Math.round((nextLine[0].getY()) * 100000d) / 100000d;
				sLon = (double)Math.round((nextLine[0].getX()) * 100000d) / 100000d;
				eLat = (double)Math.round((nextLine[1].getY()) * 100000d) / 100000d;
				eLon = (double)Math.round((nextLine[1].getX()) * 100000d) / 100000d;
				
				firstLine = nextLine.clone();
				
				//cruise will go back and forth, so need to switch directions
				if (i % 2 == 0) {					
					newLine = new SurveyLine(map, sLat, sLon , eLat, eLon);
				} else {
					newLine = new SurveyLine(map, eLat, eLon, sLat, sLon);
				}
				
				sp.addLine(newLine);
				newLine.draw( map.getGraphics2D() );
			}
		}
		if (cmd.equals("mouse")) {
			if (mouseRB.isSelected()) {
				//set the pointer icon on the toolbar
				map.getMapTools().tb[0].setSelected(true);
				//change cursor to crosshair
				map.setCursor(Cursors.getCursor(Cursors.CROSS_HAIR));
			}
		}
		if (cmd.equals("reset")) {
			//remove all lines and clear table
			sp.clearLines();
			//repaint the screen
			map.repaint();
			//reset the start and end points
			manualRB.setSelected(true);
			startLatTF.setText("");
			startLonTF.setText("");
			endLatTF.setText("");
			endLonTF.setText("");
			speedTF.setText("");
			numLinesTF.setText("1");
			gapTF.setText("");
			autoGapCB.setSelected(false);
			if (map.getMapTools().getGridDialog().isLoaded()) {
				map.getMapTools().getGridDialog().getToggle().doClick();
			};
			swathTF.setText("120");
			overlapTF.setText("10");
			swathL.setEnabled(false);
			swathTF.setEnabled(false);
			overlapL.setEnabled(false);
			overlapTF.setEnabled(false);
			
		}
		if (cmd.equals("autogap")) {
			//load/unload the grid
			map.getMapTools().getGridDialog().getToggle().doClick();
			//move survey planner layer to top
			map.moveOverlayToFront(sp);	
			
			//set which JSwing components are enabled
			autogap = autoGapCB.isSelected();
			gapL.setEnabled(enableSpacing());
			gapTF.setEnabled(enableSpacing());
			swathL.setEnabled(autogap);
			swathTF.setEnabled(autogap);
			overlapL.setEnabled(autogap);
			overlapTF.setEnabled(autogap);
		}
		if (cmd.equals("import")) {
			loadFromFile();
		}
		if (cmd.equals("Help")) {
			sp.displayHelp();
		}
		
		//check whether we are ready to enable the Submit button
		submitB.setEnabled(readyToSubmit());
		//check whether we can enable NEWS buttons
		enableNEWS();
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		if (mouseRB.isSelected() && map.getMapTools().tb[0].isSelected()) {
			map.setCursor(Cursors.getCursor(Cursors.CROSS_HAIR));
		}
	}
	
	void begin(MouseEvent e) {
		//clear the map
		map.repaint();
		
		//redraw lines
		for (SurveyLine line : sp.getSurveyLines()) {
			line.draw(map.getGraphics2D());;
		}
		//get start point
		p0 = e.getPoint();
	}
	
	public void mouseDragged(MouseEvent e) {

		if (mouseRB.isSelected() && map.getMapTools().tb[0].isSelected()) {
			 // find the current mouse location and draw a line as the mouse is dragged
			if (p0==null) { begin(e); return; }
			drawLine();
			Point2D p = map.getScaledPoint(e.getPoint());
			currentLine = new Line2D.Double(map.getScaledPoint(p0), p);
			Point2D[] pts = getPath(currentLine, 5);
			currentPath =  getGeneralPath(pts);
			drawLine();
			
			//display coords and line distance at the top of the screen
			java.text.NumberFormat fmt = java.text.NumberFormat.getInstance();
			fmt.setMaximumFractionDigits(2);
			map.setXY(e.getPoint());
			map.getMapTools().setInfoText( map.getMapTools().getInfoText() + ", distance = " +
					fmt.format(GeneralUtils.distance(pts)) +" km");
		}
	}
	
	/*
	 * create a GeneralPath that can be plotted
	 */
	GeneralPath getGeneralPath(Point2D[] pts) {
		Projection proj = map.getProjection();
		GeneralPath path = new GeneralPath();
		float[] lastP = null;
		float wrap = (float)map.getWrap()/2f;

		for( int k=0 ; k<pts.length ; k++) {
			Point2D p = proj.getMapXY( pts[k] );
			float x = (float)p.getX();
			float y = (float)p.getY();
			if( lastP!=null && wrap>0f ) {
				while( x-lastP[0] <-wrap ){x+=wrap*2f;}
				while( x-lastP[0] > wrap ){x-=wrap*2f;}
			}
			lastP = new float[] {x, y};
			if( k==0 ) path.moveTo( x, y );
			else path.lineTo( x, y );
		}
		return path;
	}
	
	/*
	 * calculate points along the path
	 */
	Point2D[] getPath(Line2D l, int dec) {
		Point2D p1 = l.getP1();
		Point2D p2 = l.getP2();
		double dist = map.getZoom()*Math.sqrt(
			Math.pow( p1.getX()-p2.getX(),2 ) +
			Math.pow( p1.getY()-p2.getY(),2 )); 
		int npt = (int)Math.ceil( dist/dec);
		Projection proj = map.getProjection();
		Point2D q1 = proj.getRefXY(p1);
		Point2D q2 = proj.getRefXY(p2);
		XYZ r1 = XYZ.LonLat_to_XYZ(q1);
		XYZ r2 = XYZ.LonLat_to_XYZ(q2);
		double angle = Math.acos( r1.dot(r2) )/(npt-1.);
		r2 = r1.cross(r2).cross(r1).normalize();
		Point2D[] path = new Point2D[npt];
		for( int k=0 ; k<npt ; k++) {
			double s = Math.sin(k*angle);
			double c = Math.cos(k*angle);
			XYZ r = r1.times(c).plus( r2.times(s) );
			path[k] = r.getLonLat();
		}
		return path;
	}
	
	/*
	 * draw the line on the screen
	 */
	void drawLine() {
		if( currentPath==null )return;
		double wrap = map.getWrap();
		synchronized(map.getTreeLock()) {
			Graphics2D g = map.getGraphics2D();
			g.setStroke( new BasicStroke(2f/(float)map.getZoom()) );
			//g.setColor( Color.red );
			g.setXORMode(Color.white);
			g.draw( currentPath );
			if( wrap>0.) {
				g.translate(wrap,0.);
				g.draw( currentPath );
			}
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if (mouseRB.isSelected()) {
			finish(e);
		}
		//check whether we are ready to enable the Submit button
		submitB.setEnabled(readyToSubmit());
		//check whether we can enable NEWS buttons
		enableNEWS();
	}

	/*
	 * At the end of a mouse drag, redraw the line in white,
	 * convert the start and end points to lat/lon and display 
	 * in the start end end lat/lon text fields
	 */
	void finish(MouseEvent e) {
		double wrap = map.getWrap();
		if (p0==null) {
			return;
		}
		drawLine();
		Graphics2D g = map.getGraphics2D();
		g.setStroke( new BasicStroke(4f/(float)map.getZoom()) );
		g.setColor( Color.white );
		//g.setXORMode(Color.white);
		g.draw( currentPath );
		if( wrap>0.) {
			g.translate(wrap,0.);
			g.draw( currentPath );
		}
		Point p1 = e.getPoint();
		if( p0.x==p1.x && p0.y==p1.y ) {
			if( line!=null ) {
				line=null;
				if(dialog!=null)dialog.setVisible(false);
				map.repaint();
			}
			p0=null;
			return;
		}

		Point2D pA = map.getScaledPoint( p0 );
		Point2D pB = map.getScaledPoint( e.getPoint());
		Point2D latLonA = map.getProjection().getRefXY(pA);
		Point2D latLonB = map.getProjection().getRefXY(pB);
		
		startLonTF.setText(df.format(latLonA.getX()));
		startLatTF.setText(df.format(latLonA.getY()));
		endLonTF.setText(df.format(latLonB.getX()));
		endLatTF.setText(df.format(latLonB.getY()));
	
		currentLine = null;
		currentPath = null;
		p0=null;
	}
	
	
	private void loadFromFile() {
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		jfc.setDialogTitle("Choose Survey Planning Data File");
		jfc.setMultiSelectionEnabled(false);
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int c = jfc.showOpenDialog(dialog);
		if (c == JFileChooser.CANCEL_OPTION
				|| c == JFileChooser.ERROR_OPTION)
			return;
		File selectedFile = jfc.getSelectedFile();

		double startLat, startLon, endLat, endLon, duration;
		int lineNum;
		int startLatCol = 0;
		int startLonCol = 1;
		int endLatCol = 2; 
		int endLonCol = 3;
		int lineNumCol = Integer.MAX_VALUE;
		int durationCol = Integer.MAX_VALUE;
		SurveyLine newLine;
		boolean dataFound = false;
		
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile)));
			String s;
		
			while ((s = in.readLine()) != null) {
	
				// split the line up into its tab-delimited elements
				String[] elements = s.split("\t|,|;");
	
				try {
					//read in coords and add new line to table and map
					startLat = Double.parseDouble(elements[startLatCol]);
					startLon = Double.parseDouble(elements[startLonCol]);
					endLat = Double.parseDouble(elements[endLatCol]);
					endLon = Double.parseDouble(elements[endLonCol]);
					newLine = new SurveyLine(map, startLat, startLon, endLat, endLon);
					if (lineNumCol != Integer.MAX_VALUE) {
						lineNum = Integer.parseInt(elements[lineNumCol]);
						newLine.setLineNum(lineNum);
					}
					if (durationCol != Integer.MAX_VALUE) {
						duration = Double.parseDouble(elements[durationCol]);
						newLine.setDuration(duration);
					}
					sp.addLine(newLine);
					newLine.draw(map.getGraphics2D());
					dataFound = true;
				} catch (Exception e) {
					try {
						//if above didn't work, probably looking at header line
						for (int i=0; i<elements.length; i++) {
							//try and assign columns based on header
							String col = elements[i].toLowerCase();
							if (col.contains("start") && col.contains("lat")) startLatCol = i;
							if (col.contains("start") && col.contains("lon")) startLonCol = i;
							if (col.contains("end") && col.contains("lat")) endLatCol = i;
							if (col.contains("end") && col.contains("lon")) endLonCol = i;
							if (col.contains("line") && col.contains("num")) lineNumCol = i;
							if (col.contains("duration")) durationCol = i;
						}				
					} catch (Exception e2) {
						continue;
					}				
					continue;
				}

			}
			in.close();
			//if durations included, calculate ship speed in knots
			SurveyLine firstLine = sp.getSurveyLines().get(0);
			if (firstLine.getDuration() != 0) {
				double speed = (firstLine.getCumulativeDistance() / firstLine.getDuration()) / GeneralUtils.KNOTS_2_KPH;
				SurveyLine.setSpeed(speed);
			}
						
			//update text fields
			startLatTF.setText(Double.toString(firstLine.getStartLat()));
			startLonTF.setText(Double.toString(firstLine.getStartLon()));
			endLatTF.setText(Double.toString(firstLine.getEndLat()));
			endLonTF.setText(Double.toString(firstLine.getEndLon()));
			numLinesTF.setText(Integer.toString(sp.getSurveyLines().size()));
			speedTF.setText(dp3f.format(SurveyLine.getSpeed()));
			
			//calculate line spacing
			if (sp.getSurveyLines().size() > 1) {
				Point2D[] pts = {firstLine.getStartPoint(), sp.getSurveyLines().get(1).getEndPoint()};
				double gap = GeneralUtils.distance(pts);
				gapTF.setText(dp3f.format(gap));
			}
			
			//check whether the Line Spacing components can be enabled 
			gapL.setEnabled(enableSpacing());
			gapTF.setEnabled(enableSpacing());
			submitB.setEnabled(readyToSubmit());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(dialog, "Unable to read file " + selectedFile, 
					"Load Error", JOptionPane.ERROR_MESSAGE);
			System.out.println(e);
		}
		
		if (!dataFound) {
			JOptionPane.showMessageDialog(dialog, "Unable to read survey lines from file " + selectedFile, 
					"Load Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {
		//check whether the Line Spacing components can be enabled 
		gapL.setEnabled(enableSpacing());
		gapTF.setEnabled(enableSpacing());
		//check whether we are ready to enable the Submit button
		submitB.setEnabled(readyToSubmit());
		//check whether we can enable NEWS buttons
		enableNEWS();
	}
	
	/*
	 * enable line spacing is autogap is off and number of lines > 1
	 */
	private boolean enableSpacing() {
		return (GeneralUtils.isInteger(numLinesTF.getText()) 
				&& (Integer.parseInt(numLinesTF.getText()) > 1) 
				&& !autogap);
	}

	
	/*
	 * determine which of the NEWS buttons should be enabled based on 
	 * bearing of initial line
	 */
	private void enableNEWS() {
		if (GeneralUtils.isDouble(startLatTF.getText()) &&
				GeneralUtils.isDouble(startLonTF.getText()) &&
				GeneralUtils.isDouble(endLatTF.getText()) &&
				GeneralUtils.isDouble(endLonTF.getText()) ) {
			
			double startLat = Double.parseDouble(startLatTF.getText());
			double startLon = Double.parseDouble(startLonTF.getText());
			double endLat = Double.parseDouble(endLatTF.getText());
			double endLon = Double.parseDouble(endLonTF.getText());
			Point2D startPt = new Point2D.Double(startLon, startLat); 
			Point2D endPt = new Point2D.Double(endLon, endLat); 
			Point2D[] firstLine = {startPt, endPt};
			double bearing = GeneralUtils.bearing(firstLine);
			if ((bearing >= 0 && bearing < 90) || (bearing >= -180 && bearing < -90) ) {
				NWRB.setEnabled(true);
				SERB.setEnabled(true);
				NERB.setEnabled(false);
				SWRB.setEnabled(false);
			} else {
				NWRB.setEnabled(false);
				SERB.setEnabled(false);
				NERB.setEnabled(true);
				SWRB.setEnabled(true);
			}
		} else {
			NWRB.setEnabled(false);
			SERB.setEnabled(false);
			NERB.setEnabled(false);
			SWRB.setEnabled(false);
		}
	}
}

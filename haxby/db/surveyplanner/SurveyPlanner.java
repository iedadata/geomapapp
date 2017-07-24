package haxby.db.surveyplanner;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableModel;

import haxby.db.Database;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.util.BrowseURL;
import haxby.util.URLFactory;

public class SurveyPlanner extends JFrame implements Database, MouseListener {

	private static final long serialVersionUID = 1L;
	private XMap map;
	private boolean enabled;
	private boolean loaded;
	private SurveyPlannerDataDisplay display;
	private SurveyPlannerSelector spSel;
	private ArrayList<SurveyLine> surveyLines;
	private DefaultTableModel tm;
	private static String SURVEY_PLANNER_HELP_URL = MapApp.BASE_URL+"/gma_html/help/survey_planner_help.html";
	
	JDialog disclaimerDialog, helpDialog;
	
	public SurveyPlanner( XMap map ) {
		this.map = map;
		enabled = false;
		loaded = false;
		this.surveyLines = new ArrayList<SurveyLine>();
		System.out.println("Loading Survey Planner");
		display = new SurveyPlannerDataDisplay(this);
		tm = display.getTableModel();
		spSel = new SurveyPlannerSelector(this);
	}


	public void addLine(SurveyLine line) {
		//first check if new, empty line
		if (Double.isNaN(line.getStartLat())) {
			tm.addRow(new Object[]{"-", "-", "-", "-", "-", "-","-","-","-",line});
			surveyLines.add(line);
			return;
		}
		//get depths
		double startDepth = getDepth(line.getStartLat(), line.getStartLon());
		double endDepth = getDepth(line.getEndLat(), line.getEndLon());
		//add depths to survey line
		line.setDepths(startDepth, endDepth);
		//add to list
		surveyLines.add(line);
		//add to table
		tm.addRow(new Object[]{line.getLineNum(), line.getStartLat(), line.getStartLon(), startDepth,
				line.getEndLat(), line.getEndLon(), endDepth, line.getCumulativeDistance(), line.getDuration(), line});
	}
	
	public Integer getDepth(double lat, double lon) {
		if (Double.isNaN(lat) || Double.isNaN(lon)) return null;
		return (int) map.getFocus().getZ(new Point2D.Double(lon,lat));
	}
	
	public void clearLines() {
		//reset table
		tm.setRowCount(0);
		//clear list
		surveyLines.clear();
		//reset static counters and accumulators in surveyLine
		SurveyLine.resetAll();
	}
	
	public void deleteLine(SurveyLine line) {
		surveyLines.remove(line);
	}
	
	public ArrayList<SurveyLine> getSurveyLines() {
		return surveyLines;
	}
	
	public String getDBName() {
		return "Survey Planner";
	}

	public String getCommand() {
		return "survey_planner_cmd";
	}

	public String getDescription() {
		return "Survey Planner";
	}
	@Override
	public void draw(Graphics2D g) {
		if( !loaded ) return;
		double zoom = map.getZoom();
		g.setStroke( new BasicStroke( 2f/(float)zoom ));
		for (SurveyLine sl : surveyLines) {
			sl.draw(g);
		}
	}

	@Override
	public boolean loadDB() {
		if( loaded ) return loaded;
		//map.getMapTools().getGridDialog().getToggle().doClick();
		displayDisclaimer();
		loaded = true;
		return loaded;
	}
	
	public void displayDisclaimer() {
		
//		final Object[] options = {"Accept", "Survey Planner Help"};
		final Object[] options = {"Accept"};
		String disclaimerText = "<html> <p>The Survey Planner portal is released as a Beta test.<br> "
				+ "Expanded functionality is being developed for a future release.</p><br>"
				+ "<p>The displayed maps, images, data tables and this portal are not to be used for navigation purposes.</p></html>";
		

		
		JOptionPane.showOptionDialog(null, disclaimerText, "Survey Planner Disclaimer", 
				JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE,  null,  options, options[0]);
		
//		
//		disclaimerDialog = new JDialog(this,"Survey Planner Disclaimer");
//		disclaimerDialog.setModal(false);
//		JOptionPane diclaimerPane = new JOptionPane(disclaimerText, JOptionPane.WARNING_MESSAGE, 
//				JOptionPane.YES_NO_OPTION, null,  options, options[0]); 
//		diclaimerPane.addPropertyChangeListener(new PropertyChangeListener() {
//			public void propertyChange(PropertyChangeEvent e) {
//				if (e.getPropertyName().equals("value")) {
//					if (e.getNewValue().equals(options[0])) {
//						disclaimerDialog.dispose();
//					} else if (e.getNewValue().equals(options[1])) {
//						displayHelp();
//					}
//				}
//			}
//		});
//		disclaimerDialog.setContentPane(diclaimerPane);
//		disclaimerDialog.pack();
//		disclaimerDialog.setLocationRelativeTo(this);
//		try {
//		    Thread.sleep(2000);                 //1000 milliseconds is one second.
//		} catch(InterruptedException ex) {
//		    Thread.currentThread().interrupt();
//		}
//		disclaimerDialog.setVisible(true);
//		disclaimerDialog.toFront();
		
		
		
//		int selection = JOptionPane.showOptionDialog(null, disclaimerText, "Survey Planner Disclaimer", 
//				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,  null,  options, options[0]);
//		
//		if (selection == JOptionPane.NO_OPTION) {
//			displayHelp();
//		} 
	}
	
	public void displayHelp() {
		String helpText = "";
		String s = "";
		URL helpURL = null;
		if(MapApp.BASE_URL.matches(MapApp.DEV_URL)){
			SURVEY_PLANNER_HELP_URL = SURVEY_PLANNER_HELP_URL.replace("http://app.", "http://app-dev.");
		}
		try {
			helpURL = URLFactory.url(SURVEY_PLANNER_HELP_URL);
			BufferedReader in = new BufferedReader( new InputStreamReader( helpURL.openStream() ) );

			while((s=in.readLine())!=null){
				helpText = helpText.concat(s);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	    // for copying style
	    JLabel label = new JLabel();
	    Font font = label.getFont();

	    // create some css from the label's font
	    StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
	    style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
	    style.append("font-size:" + font.getSize() + "pt;");
		
	    //need to use editor pane and hyperlink listener so that we can include hyperlinks in help text
	    JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
	            + helpText //
	            + "</body></html>");

	    ep.addHyperlinkListener(new HyperlinkListener()
	    {
	        @Override
	        public void hyperlinkUpdate(HyperlinkEvent e)
	        {
	            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
	            	BrowseURL.browseURL(e.getURL().toString());
	        }
	    });
		ep.setEditable(false);
		ep.setBackground(label.getBackground());

		//need to do this to make Help window non-modal so that it can be kept open
		//whilst the user works on the tool
		helpDialog = new JDialog(this,"Survey Planner Help");
		JOptionPane helpPane = new JOptionPane(ep, JOptionPane.PLAIN_MESSAGE); 
		helpPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent e) {
				if (e.getPropertyName().equals("value")) {
					switch ((Integer)e.getNewValue()) {
					case JOptionPane.OK_OPTION:
						break;
					}
					helpDialog.dispose();
				}
			}
		});
		
		helpDialog.setContentPane(helpPane);
		helpDialog.pack();
		helpDialog.setLocationRelativeTo(this);
		helpDialog.setVisible(true);
		helpDialog.toFront();

	}
	
	@Override
	public boolean isLoaded() {
		return loaded;
	}
	
	@Override
	public void disposeDB() {
		setEnabled( false );
		map.removeMouseListener( this );
		clearLines();
		//reset all buttons and field on the right-side menu
		spSel = new SurveyPlannerSelector(this);
		loaded = false;
		System.gc();
	}
	
	@Override
	public void setEnabled(boolean tf) {
		if( tf && enabled ) return;
		map.removeMouseListener( this );
		map.addMouseListener(this);
		enabled = tf;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public JComponent getSelectionDialog() {
		return spSel.getDialog();
	}
	@Override
	public JComponent getDataDisplay() {
		return display.getPanel();
	}

	public int getSurveyLineColumn() {
		return display.getSurveyLineColumn();
	}
	
	public DefaultTableModel getTableModel() {
		return tm;
	}
	
	public XMap getMap() {
		return map;
	}
	
	public void repaint() {
		//clear the map
		map.repaint();

		//redraw all survey lines
		for (SurveyLine sl : surveyLines) {
			sl.draw(map.getGraphics2D());
		}
	}

	public void mousePressed(MouseEvent e) {
	}
	public void mouseReleased(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {
	}
	public void mouseExited(MouseEvent e) {
	}
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseDragged(MouseEvent e) {
	}
	public void mouseClicked(MouseEvent e) {
	}

}

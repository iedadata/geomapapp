package haxby.db.mgg;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import haxby.db.Axes;
import haxby.db.XYGraph;
import haxby.map.MapApp;
import haxby.map.XMap;
import haxby.map.Zoomer;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

public class MGGDataDisplay implements ActionListener, MouseListener {
	protected JLabel cruises;
	protected MGG tracks;
	protected XMap map;
	protected JList cruiseL;
	protected MGGData data;
	protected XYGraph[] xy;
	protected JButton loadProfile;
	protected JButton saveProfile;
	protected JButton getData;
	protected JRadioButton[] dataRB;
	protected JRadioButton[] selectRB;
	protected JPanel dialog;
	protected JPanel panel;
	protected JScrollPane scrollPane;
	protected int dataIndex;
	protected String loadedLeg;
	protected boolean saving = false;
	protected JCheckBox saveViewportCB, saveFullCB;
	protected JRadioButton jpgBtn, pngBtn;
	protected JCheckBox autoscaleCB;
	protected boolean autoscale = false;
	protected boolean trackWidth = false;

	static String MGD77_PATH = PathUtil.getPath("PORTALS/MGD77_PATH",
			MapApp.BASE_URL+"/data/portals/mgd77/");
	
	static final String[] CONVENTION_M77T_LABELS = {"SURVEY_ID","FORMAT_77","CENTER_ID","PARAMS_CO","DATE_CREAT","INST_SRC","COUNTRY",
	                                                "PLATFORM","PLAT_TYPCO","PLAT_TYP","CHIEF","PROJECT","FUNDING","DATE_DEP","PORT_DEP",
	                                                "DATE_ARR","PORT_ARR","NAV_INSTR","POS_INFO","BATH_INSTR","BATH_ADD","MAG_INSTR",
	                                                "MAG_ADD","GRAV_INSTR","GRAV_ADD","SEIS_INSTR","SEIS_FRMTS","LAT_TOP","LAT_BOTTOM",
	                                                "LON_LEFT","LON_RIGHT","BATH_DRATE","BATH_SRATE","SOUND_VEL","VDATUM_CO","BATH_INTRP",
	                                                "MAG_DRATE","MAG_SRATE","MAG_TOWDST","MAG_SNSDEP","MAG_SNSSEP","M_REFFL_CO","MAG_REFFLD",
	                                                "MAG_RF_MTH","GRAV_DRATE","GRAV_SRATE","G_FORMU_CO","GRAV_FORMU","G_RFSYS_CO",
	                                                "GRAV_RFSYS","GRAV_CORR","G_ST_DEP_G","G_ST_DEP","G_ST_ARR_G","G_ST_ARR","IDS_10_NUM",
	                                                "IDS_10DEG","ADD_DOC"};

//	GMA 1.4.8: TESTING
	protected String selectedLeg;

	public MGGDataDisplay(MGG tracks, XMap map) {
		this.tracks = tracks;
		this.map = map;
		cruiseL = new JList(tracks.model);

		
//		1.4.4: Allow multiple cruises to be selected so that many cruises can be downloaded at once
//		cruiseL.setSelectionMode( cruiseL.getSelectionModel().SINGLE_SELECTION );

		initPanel();
		xy = null;
		data = null;
		dataIndex = -1;
		selectedLeg = null;	//GMA 1.4.8: TESTING
	}
	void initPanel() {
		loadProfile = new JButton("View Profile");
		loadProfile.addActionListener(this);
		saveProfile = new JButton("Save Displayed Profile");
		saveProfile.setEnabled(false);
		saveProfile.addActionListener(this);
		getData = new JButton("Download Data");
		getData.addActionListener(this);
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout( panel1, BoxLayout.Y_AXIS ) );
		loadProfile.setAlignmentX( panel1.CENTER_ALIGNMENT );
		saveProfile.setAlignmentX( panel1.CENTER_ALIGNMENT );
		getData.setAlignmentX( panel1.CENTER_ALIGNMENT );
		panel1.add( new JScrollPane(cruiseL) );
		JPanel buttonsPnl = new JPanel(new GridLayout(0,1));
		buttonsPnl.add( loadProfile );
		buttonsPnl.add( saveProfile );
		buttonsPnl.add( getData );
		panel1.add(buttonsPnl);
		cruiseL.addMouseListener(this);
		cruises = new JLabel("cruises");
		cruises.setForeground( Color.black );
		panel1.setBorder( BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder( Color.black),
			BorderFactory.createEmptyBorder(1,1,1,1) ));
		dataRB = new JRadioButton[3];

//		***** 1.6.2: Change the titles of the radio buttons to more clearly convey the data type 
//		being selected.
		dataRB[0] = new JRadioButton("Depth");
		dataRB[1] = new JRadioButton("Gravity");
		dataRB[2] = new JRadioButton("Magnetics");
//		***** 1.6.2

		ButtonGroup bg = new ButtonGroup();
		for( int k=0 ; k<3 ; k++ ) {
			bg.add(dataRB[k]);
			dataRB[k].addActionListener(this);
			dataRB[k].setEnabled(false);
		}
		JPanel panel2 = new JPanel(new GridLayout( 0, 1 ));
		JLabel label = new JLabel("plot:");
		label.setForeground( Color.black );
		panel2.add(label);
		for( int k=0 ; k<3 ; k++ ) panel2.add(dataRB[k]);
		JButton b = new JButton("Next Parameter");
		b.addActionListener(this);
		panel2.add( b );
		autoscaleCB = new JCheckBox("Autoscale");
		autoscaleCB.addActionListener(this);
		autoscaleCB.setSelected(false);
		panel2.add(autoscaleCB);
		
	//	label = new JLabel("select:");
	//	label.setForeground( Color.black );
	//	panel2.add(label);
		panel2.setBorder( BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder( Color.black),
			BorderFactory.createEmptyBorder(1,1,1,1) ));

	//	selectRB = new JRadioButton[2];
	//	selectRB[0] = new JRadioButton("cruise");
	//	selectRB[1] = new JRadioButton("point");
	//	bg = new ButtonGroup();
	//	for( int k=0 ; k<2 ; k++) {
	//		bg.add(selectRB[k]);
	//		panel2.add(selectRB[k]);
	//		selectRB[k].addActionListener(this);
	//	}
	//	selectRB[0].setSelected(true);

		panel2.add( cruises);
		JButton info = new JButton( org.geomapapp.util.Icons.getIcon(26, false) );	// 26 = INFO icon
		panel2.add( info );
		info.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showInfo();
			}
		});
		dialog = new JPanel(new GridLayout( 1, 0 ));
		dialog.add(panel1);
		dialog.add(panel2);
		scrollPane = new JScrollPane( new JComponent() {
			public Dimension getPreferredSize() {
				return new Dimension(600, 100);
			}
			public void paint(Graphics g) {
				g.drawString("no track loaded", 20, 30);
			}
		});

		// GMA 1.6.2: Tool tip text
		scrollPane.setToolTipText("Right-click to digitize");

		
		panel = new JPanel(new BorderLayout());
		panel.add(dialog, "West");
		panel.add(scrollPane, "Center");
	}
	// Info Button from the header file
	void showInfo() {
		String leg = (String)cruiseL.getSelectedValue();
		try {
			BufferedReader in;
			java.net.URL headerFileURL = null;
			JTextArea text = new JTextArea();
			
			// First check if there is an imported header file stored in mgg_header_files
			// on the user's computer
			File MGG_h77_file = new File (MGG.MGG_header_dir, leg + ".h77");
			File MGG_h77t_file = new File (MGG.MGG_header_dir, leg + ".h77t");
			if ( MGG_h77t_file.exists() || MGG_h77_file.exists()) {
				
				File h77File = MGG_h77t_file.exists() ? MGG_h77t_file : MGG_h77_file;
				in = new BufferedReader(new InputStreamReader( new FileInputStream(h77File)));
				
				// check if this file has M77T formatting
				// by convention, we can tell this is the second field in the first line is
				// "FORMAT_77" or "MGD77T"
				String[] firstLine = in.readLine().split("\t");
				in.close();
				
				if (firstLine.length > 1 && 
						(firstLine[1].equals("FORMAT_77") || firstLine[1].equals("MGD77T"))) {
					// if the header file is h77t, then format the output in rows of fields
					readAsH77T(text, h77File);
				} else readAsH77(text, h77File);
										
			}
			// if not an imported file, then look for header file on the server
			else {
				if ( tracks.mggSel.loadedControlFiles.compareTo( "LDEO" ) == 0 ) {
					headerFileURL = haxby.util.URLFactory.url(MGD77_PATH + MGGSelector.LAMONT_CONTROL_LOADED.toLowerCase() + "-mgd77/header/" + leg + ".hldeo");
				}
				else if ( tracks.mggSel.loadedControlFiles.compareTo( "NGDC" ) == 0 ) {
					headerFileURL = haxby.util.URLFactory.url( MGD77_PATH + MGGSelector.NGDC_CONTROL_LOADED.toLowerCase() + "-mgd77/header/" + leg + ".h77");
				}
				else if ( tracks.mggSel.loadedControlFiles.compareTo( "ADGRAV" ) == 0 ) {
					headerFileURL = haxby.util.URLFactory.url( MGD77_PATH + MGGSelector.ADGRAV_CONTROL_LOADED.toLowerCase() + "-mgd77/header/" + leg + ".h77");
				}
				else if ( tracks.mggSel.loadedControlFiles.compareTo( "SIOExplorer" ) == 0 ) {
					headerFileURL = haxby.util.URLFactory.url( MGD77_PATH + MGGSelector.SIOEXPLORER_LOADED.toLowerCase() + "-mgd77/header/" + leg + ".info");
				}
				else if ( tracks.mggSel.loadedControlFiles.compareTo( "USAP" ) == 0 ) {
					headerFileURL = haxby.util.URLFactory.url( MGD77_PATH + MGGSelector.USAP_LOADED.toLowerCase() + "-mgd77/header/" + leg + ".info");
				}
	
				in = new BufferedReader(new InputStreamReader( headerFileURL.openStream() ));
				text.append( in.readLine() );
				String s;
				while( (s=in.readLine())!=null ) {
					text.append("\n"+s);
				}
				in.close();
			}
			
			JPanel p = new JPanel( new BorderLayout() );
			p.add( new JScrollPane(text) );
			p.setPreferredSize( new Dimension(600, 400) );
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(), 
					p, "cruise: "+leg, JOptionPane.INFORMATION_MESSAGE);
		} catch(Exception e) {
			JOptionPane.showMessageDialog( map.getTopLevelAncestor(), 
					"No header information provided for " + leg, "No Header Information", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	/*
	 * read in an H77 non-tabbed text file
	 */
	public void readAsH77 (JTextArea text, File h77File) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader( new FileInputStream(h77File)));
		text.append( in.readLine() );
		String s;
		while( (s=in.readLine())!=null ) {
			text.append("\n"+s);
		}
		in.close();
	}
	
	/*
	 * read in an H77T tabbed text file
	 */
	public void readAsH77T (JTextArea text, File h77File) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader( new FileInputStream(h77File)));
		// read in each line and split into fields
		ArrayList<String[]> lines = new ArrayList<String[]>();
		String l;
		while( ( l=in.readLine())!=null ) {
			lines.add(l.split("\t"));
		}
		// if file has a header line, containing field names, use that, else use convention values
		// by convention we can detect this if the second field name is "FORMAT_77"
		if (!lines.get(0)[1].equals("FORMAT_77")) {
			lines.add(0,  CONVENTION_M77T_LABELS);
		}
					
		int numFields = lines.get(0).length;

		for (int i=0; i < numFields; i++) {

			for (int j=0; j<lines.size(); j++) {
				String[] line = lines.get(j);
				if (j == 0) {
					text.append(line[i] + ":\t");
				} else if (line.length > i) text.append(line[i] + "\t");
			}
			text.append("\n");
		}
		in.close();
	}
	
	public void actionPerformed( ActionEvent evt ) {
		if( evt.getSource() instanceof JRadioButton ) {
			for( int k=0 ; k<3 ; k++) {
				if(evt.getSource()==dataRB[k]) {
					double z = ((XYGraph)scrollPane.getViewport().getView()).getZoom();
					xy[k].setZoom(z);
					JScrollBar bar = scrollPane.getHorizontalScrollBar();
					int value = bar.getValue();
					scrollPane.setViewportView( xy[k] );
					bar = scrollPane.getHorizontalScrollBar();
					scrollPane.validate();
					bar.setValue( value );

//					***** GMA 1.6.2: Send the currently selected data type to the MGGData object so it knows which
//					data to record when the user right-clicks.
					data.setCurrentDataIndex(k);
//					***** GMA 1.6.2
					return;
				}
			}
		}
		String cmd = evt.getActionCommand();

		if(cmd.equals("View Profile")) {
//			***** GMA 1.6.4: Do not repaint the map because it de-selects the cruise in the list
			map.repaint();
//			***** GMA 1.6.4

			String leg = (String)cruiseL.getSelectedValue();
//			System.out.println( tracks.types & tracks.tracks[cruiseL.getSelectedIndex()].getTypes() );
			loadedLeg = leg;
			if( leg==null ) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(), "No Cruise Selected"); // message to user.
				return;
			}
			if (leg.equals("---Imported Files---")) return;
			try {
				dataIndex = tracks.selectedIndex;
				if( tracks.dataIndex!=-1 ) {
					xy[0].removeMouseMotionListener( data );
					xy[1].removeMouseMotionListener( data );
					xy[2].removeMouseMotionListener( data );

					xy[0].removeMouseListener( data );
					xy[1].removeMouseListener( data );
					xy[2].removeMouseListener( data );
				}
				
				data = loadMGGData(map, leg, tracks.mggSel.loadedControlFiles);
				cruises.setText( leg );
				cruises.setForeground( new Color( 100,0,0) );
				tracks.dataIndex = dataIndex;
		
			} catch(IOException ex) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
						"Unable to load profile for "+leg+"\n"
						+ex.getMessage() ); // message to user
				tracks.dataIndex = -1;
				return;
			}
			
			if (data == null) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(),
						"Unable to load profile for "+leg+".\n"
						+ "Not able to find data file." ); // message to user
				tracks.dataIndex = -1;
				return;
			}
			
			for( int k=0 ; k<3 ; k++) {
				dataRB[k].setEnabled(true);
			}
			dataRB[0].setSelected(true);

//			***** GMA 1.6.2: Send the currently selected data type to the MGGData object so it knows which
//			data to record when the user right-clicks.
			data.setCurrentDataIndex(0);
//			***** GMA 1.6.2

			xy = new XYGraph[3];
			xy[0] = new XYGraph( data, 0);
			xy[1] = new XYGraph( data, 1);
			xy[2] = new XYGraph( data, 2);
			xy[0].addMouseMotionListener(data);
			xy[1].addMouseMotionListener(data);
			xy[2].addMouseMotionListener(data);

//			***** GMA 1.6.2: Add mouse listeners for the graph for each data type to the MGGData object so
//			that it can bring up the pop-up menu when the user right-clicks in the graph
			xy[0].addMouseListener(data);
			xy[1].addMouseListener(data);
			xy[2].addMouseListener(data);
//			***** GMA 1.6.2

			int sides = Axes.LEFT | Axes.BOTTOM;
			
			// if width of dataset is smaller than viewport width, expand to fill whole viewport
			if (xy[0].getGraphWidth() < scrollPane.getViewport().getWidth() ) trackWidth = true;
			for(int k=0 ; k<3 ; k++) {
				xy[k].setScrollableTracksViewportHeight( true );	
				if (autoscale) {
					// if autoscale is selected, scale to only display the region displayed on the map
					double[][] newRanges = data.getRangesOnMap(k);
					xy[k].setXRange(newRanges[0]);
					xy[k].setYRange(newRanges[1]);
					xy[k].setScrollableTracksViewportWidth( true ); 
				}
				else {
					xy[k].setScrollableTracksViewportWidth( trackWidth );
				}
				Zoomer z = new Zoomer( xy[k] );
				xy[k].addMouseListener( z );
				xy[k].addKeyListener( z );
				xy[k].setAxesSides(sides);
			}
			scrollPane.setViewportView(xy[0]);
			xy[0].invalidate();
			scrollPane.validate();
		
			saveProfile.setEnabled(true);
		} else if( cmd.equals("Next Parameter") ) {
			for( int k=0 ; k<3 ; k++) {
				if( dataRB[k].isSelected() ) {
					k = (k+1)%3;
					dataRB[k].doClick();

//					***** GMA 1.6.2: Send the currently selected data type to the MGGData object so it knows which 
//					data to record when the user right-clicks.
					data.setCurrentDataIndex(k);
//					***** GMA 1.6.2
					return;
				}
			}
		}

		else if ( cmd.equals("Save Displayed Profile")) {
			try {
				save();
			} catch (IOException e) {}

		}
		
		else if (cmd.equals("Autoscale")) {
			autoscale = autoscaleCB.isSelected();
			if (data != null) {
				if (!autoscale) {
					for (int i=0; i<3; i++) {
						double[][] newRanges = data.getFullRanges(i);
						xy[i].setXRange(newRanges[0]);
						xy[i].setYRange(newRanges[1]);
						//sets the scroll bar
						xy[i].setScrollableTracksViewportWidth( trackWidth );
						xy[i].invalidate();
					}
				} else {
					for (int i=0; i<3; i++) {
						double[][] newRanges = data.getRangesOnMap(i);
						xy[i].setXRange(newRanges[0]);
						xy[i].setYRange(newRanges[1]);
						//removes the scroll bar
						xy[i].setScrollableTracksViewportWidth( true );
						xy[i].invalidate();
					}
				}
				scrollPane.validate();
				scrollPane.getViewport().getView().repaint();
			}
		}
//		1.4.4: Added functionality to allow user to download MGD-77 data files for currently selected legs
		else if ( evt.getSource() == getData ) {
			String cruiseName = (String)cruiseL.getSelectedValue();
			
			if (cruiseName.equals("---Imported Files---")) return;
			
			if ( cruiseL.getSelectedValues().length < 1 ) {
				JOptionPane.showMessageDialog(map.getTopLevelAncestor(), "Please select leg(s) to download");
				return;
			}
			if ( cruiseL.getSelectedValues().length > 100 ) {
				int manyCruises = JOptionPane.showConfirmDialog(map.getTopLevelAncestor(), "Warning: More than 100 legs selected.  Continue?", "MGG Warning", JOptionPane.OK_CANCEL_OPTION );

				if ( manyCruises == JOptionPane.CANCEL_OPTION ) {
					return;
				}
			}
			int confirm = JOptionPane.NO_OPTION;
//			File newDataFile = null;
			File[] newDataFile = new File[cruiseL.getSelectedValues().length];
			for ( int i = 0; i < cruiseL.getSelectedValues().length; i++ ) {
				newDataFile[i] = new File( (String)cruiseL.getSelectedValues()[i] + ".a77" );
			}
			JFileChooser jfc = MapApp.getFileChooser();
			jfc.setMultiSelectionEnabled( true );
			
			jfc.setDialogTitle( "Download MGD77 Files" );
			while ( confirm == JOptionPane.NO_OPTION ) {
//				jfc.setSelectedFile( new File( loadedLeg + ".a77" ));
				jfc.setSelectedFiles( newDataFile );
				int c = jfc.showSaveDialog(jfc);
			if ( c==JFileChooser.CANCEL_OPTION || c == JFileChooser.ERROR_OPTION ) return;
				String selectedDir = null;

				if ( jfc.getSelectedFile().getAbsolutePath().lastIndexOf("\\") != -1 ) {
					selectedDir = jfc.getSelectedFile().getAbsolutePath().substring( 0, jfc.getSelectedFile().getAbsolutePath().lastIndexOf("\\") + 1 );
				}
				else {
					selectedDir = jfc.getSelectedFile().getAbsolutePath().substring( 0, jfc.getSelectedFile().getAbsolutePath().lastIndexOf("/") + 1 );
				}

				for ( int i = 0; i < newDataFile.length; i++ ) {
					if ( selectedDir != null )	{
						if ( newDataFile[i].getName().indexOf( ".a77") == -1 ) {
							newDataFile[i] = new File( selectedDir + newDataFile[i].getName() + ".a77" );
						}
						else {
							newDataFile[i] = new File( selectedDir + newDataFile[i].getName() );
						}
					}
				}

				for ( int i = 0; i < newDataFile.length; i++ ) {
					if ( newDataFile[i].exists() ) {
							confirm = JOptionPane.showConfirmDialog(tracks.map.getTopLevelAncestor(), "File exists, Overwrite?");
						if ( confirm == JOptionPane.CANCEL_OPTION ) return;
					} else {
						confirm = JOptionPane.CANCEL_OPTION;
					}
				}
			}
			for ( int i = 0; i < newDataFile.length; i++ ) {
//				newDataFile[i] = new File( newDataFile[i].getAbsolutePath() + ".a77" );
				newDataFile[i] = new File( newDataFile[i].getAbsolutePath() );

				try {
					String MGGurl = MGD77_PATH ;
					URL url = URLFactory.url( MGGurl + "control_files_list.txt");
					URL dataDirURL;
					URL dataFileURL;

					BufferedReader in;
					BufferedReader inDataDir;
					BufferedReader inDataFile;
					try {
						in = new BufferedReader(
								new InputStreamReader( url.openStream() ));
						String s = "";
						String sDataDir = "";
						String sDataFile = "";
						String sData = "";
						String leg = newDataFile[i].getName();
						if ( leg != null ) {
							while (( s = in.readLine() ) != null ) {
								if ( s.indexOf( "[DIR]" ) != -1 && s.indexOf( "Parent Directory" ) == -1 ) {
									String dirName = s.substring( s.indexOf( "a href=\"" ) + 8, s.indexOf( "\">", s.indexOf( "a href=\"" ) ) );
									dataDirURL = URLFactory.url(MGGurl + dirName + "data/");
									inDataDir = new BufferedReader(
											new InputStreamReader( dataDirURL.openStream() ));
									while ( ( sDataDir = inDataDir.readLine() ) != null ) {
										if ( sDataDir.indexOf( leg ) != -1 ) {
											 // online version contain html markup, AT SEA is just a list of files.  Need to remove the markup.
											if (sDataDir.contains("a href=")) {
												sDataFile = sDataDir.substring( sDataDir.indexOf( "a href=\"" ) + 8, sDataDir.indexOf( "\">", sDataDir.indexOf( "a href=\"" ) ) );
											} else sDataFile = sDataDir;
											dataFileURL = URLFactory.url(MGGurl + dirName + "data/" + sDataFile);
											inDataFile = new BufferedReader(
													new InputStreamReader( dataFileURL.openStream() ) );
											BufferedWriter out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream( newDataFile[i] ) ) );

											while ( ( sData = inDataFile.readLine() ) != null ) {
												out.write(sData + "\n");
											}
											out.flush();
											out.close();
										}
									}
								}
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		 	}
		}
	}

	protected MGGData loadMGGData(XMap map2, String leg, String loadedControlFiles) throws IOException {
		return MGGData.load(map, leg, loadedControlFiles);
	}
//	1.4.4: Allows user to double-click to load the data for the currently selected legs
	public void mouseClicked(MouseEvent e) {
		if ( e.getSource() == cruiseL ) {
			if ( e.getClickCount() >= 2 ) {
				loadProfile.doClick();
			}
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
//		GMA 1.4.8: Clear previous selected leg from map when none of the data types are selected
		if ( !(tracks.mggSel.gravCB.isSelected()) && !(tracks.mggSel.magCB.isSelected()) && !(tracks.mggSel.topoCB.isSelected()) ) {
			tracks.mggSel.noneSelected.doClick();
		}
	}
	public void mouseReleased(MouseEvent e) {
//		***** GMA 1.4.8: Draw the selected leg after the map has been cleared
		if ( !(tracks.mggSel.gravCB.isSelected()) && !(tracks.mggSel.magCB.isSelected()) && !(tracks.mggSel.topoCB.isSelected()) ) {
			int i = tracks.list.getSelectedIndex();
			if(i!=-1) {
				i = tracks.model.indexOf( i );
			}
			if(i==tracks.selectedIndex)return;
			tracks.drawSelectedTrack(Color.black);
			tracks.selectedIndex = i;
			tracks.drawSelectedTrack(Color.white);
		}
//		***** GMA 1.4.8
	}
//	***** 1.4.4
	public void disposeData() {
		if (xy != null) {
			xy[0].removeMouseMotionListener( data );
			xy[1].removeMouseMotionListener( data );
			xy[2].removeMouseMotionListener( data );

			xy[0].removeMouseListener( data );
			xy[1].removeMouseListener( data );
			xy[2].removeMouseListener( data );
		}
		scrollPane.setViewportView( null );
		xy = null;
		data = null;
	}
	
	void save()	throws IOException {
		if (loadedLeg.equals("---Imported Files---")) return;
		
		if(xy[0] == null) {
			JOptionPane.showMessageDialog(dialog,"Image not loaded.");
			return;
		}

		JPanel savePanel = new JPanel( new BorderLayout() );
		savePanel.setBorder(BorderFactory.createEmptyBorder( 0, 5, 0, 5));

		JPanel savePrompt = new JPanel(new GridLayout(0, 1));
		saveViewportCB = new JCheckBox("Save viewport");
		saveFullCB = new JCheckBox("Save full profile");
		ButtonGroup saveGroup = new ButtonGroup();
		saveGroup.add(saveFullCB);
		saveGroup.add(saveViewportCB);
		saveViewportCB.setSelected(true);
		savePrompt.add(saveViewportCB);
		savePrompt.add(saveFullCB);
		savePanel.add(savePrompt, BorderLayout.NORTH);

		JPanel fmtPanel = new JPanel(new GridLayout(0,2,0,1));
		jpgBtn = new JRadioButton("jpeg");
		pngBtn = new JRadioButton("png");
		ButtonGroup saveFmtGroup = new ButtonGroup();
		saveFmtGroup.add(jpgBtn);
		saveFmtGroup.add(pngBtn);
		jpgBtn.setSelected(true);
		fmtPanel.add(jpgBtn);
		fmtPanel.add(pngBtn);
		savePanel.add(fmtPanel, BorderLayout.CENTER);
				
		int s = JOptionPane.showConfirmDialog(dialog, savePanel, "Save Options", JOptionPane.OK_CANCEL_OPTION);
		if(s == 2) {
			return;
		}

		String dataType = dataRB[data.getCurrentDataIndex()].getText();
		String fmt = "jpg";
		if (pngBtn.isSelected()) {
			fmt = "png";
		}
				
		if (saveViewportCB.isSelected()) { // Save viewport image

			File file = new File(loadedLeg + "_" + dataType + "_viewport." + fmt);
			while (true) {
				JFileChooser filechooser = MapApp.getFileChooser();
				filechooser.setSelectedFile(file);
				int c = filechooser.showSaveDialog(null);
				if (c == JFileChooser.CANCEL_OPTION)
					return;
				file = filechooser.getSelectedFile();
				if(!file.exists())
					break;
				int o = JOptionPane.showConfirmDialog(dialog, "File exists, Overwrite?");
				if(o == JOptionPane.CANCEL_OPTION)
					return;
				else if(o == JOptionPane.OK_OPTION)
					break;
			}

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				saveViewport(out, fmt);
				out.close();
				JOptionPane.showMessageDialog(dialog, "Save Successful");
			}
			catch(IOException ex) {
				JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error Writing File", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}

		} else if (saveFullCB.isSelected()) { // Save Full image
			File file = new File(loadedLeg + "_" + dataType + "_full." + fmt);
			while (true) {
				JFileChooser filechooser = MapApp.getFileChooser();
				filechooser.setSelectedFile(file);
				int c = filechooser.showSaveDialog(null);
				if (c == JFileChooser.CANCEL_OPTION)
					return;
				file = filechooser.getSelectedFile();
				if(!file.exists())
					 break;
				int o = JOptionPane.showConfirmDialog(dialog, "File exists, Overwrite?");
				if(o == JOptionPane.CANCEL_OPTION)
					return;
				else if(o == JOptionPane.OK_OPTION)
					break;
			}

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				saveFull(out, fmt);
				out.close();
				JOptionPane.showMessageDialog(dialog, "Save Successful");
			}
			catch(IOException ex) { 
				JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error Writing File", JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		} 
	}

	public void saveViewport(OutputStream out, String fmt) throws IOException {
		BufferedImage image = xy[data.getCurrentDataIndex()].getImage();
		try {
			ImageIO.write(image,
				fmt,
				out);
		} catch(IOException e) {
			JOptionPane.showMessageDialog(dialog,
					" Save failed: "+e.getMessage(),
					" Save failed",
					 JOptionPane.ERROR_MESSAGE);
		}
	}
		
	private void saveFull(OutputStream out, String fmt) throws IOException {
		BufferedImage image = xy[data.getCurrentDataIndex()].getFullImage();
		try {
			ImageIO.write(image,
				fmt,
				out);
		} catch(IOException e) {
			JOptionPane.showMessageDialog(dialog,
					" Save failed: "+e.getMessage(),
					" Save failed",
					 JOptionPane.ERROR_MESSAGE);
		}
		
	}
}
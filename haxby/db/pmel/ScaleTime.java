package haxby.db.pmel;

import haxby.util.*;
import haxby.map.*;
import haxby.image.*;

import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

/**
 * The Locations and Timing of Seafloor Earthquakes and Volcanic Eruptions shows earthquake
 * events in specific regions. The data source is comes from PMEL/NOAA.
 * 
 * @author Samantha Chan
 * @author William F. Haxby
 * @version 3.1.0
 * @since 1.1.6
 */

public class ScaleTime extends JComponent 
			implements Zoomable,
				Runnable,
				ActionListener {
	PMEL pmel;
	double[] zRange;
	double[] zRange0;
	double interval;
	EventHistogram zHist;
	int side;
	int lastX;
	int minX, maxX, middle;
	Calendar cal;
	JPanel panel;
	JTextField frameRate;
	JToggleButton play;
	JButton save,
			reset;

	public ScaleTime( PMEL pmel, EventHistogram hist ) {
		this.pmel = pmel;
		setHist( hist );
		cal = Calendar.getInstance( TimeZone.getTimeZone("GMT") );
		Zoomer zoomer = new Zoomer(this);
		addMouseListener(zoomer);
		addMouseMotionListener(zoomer);
		addKeyListener(zoomer);
		panel = new JPanel( new BorderLayout() );
		JPanel panel1 = new JPanel( new GridLayout(0,1) );

		try {
			ClassLoader cl = getClass().getClassLoader();
			URL url = cl.getResource("org/geomapapp/resources/logos/pmel_h.gif");
			ImageIcon icon = new ImageIcon( url );
			JLabel logo = new JLabel( icon );
			logo.setOpaque( true );
			logo.setBackground( Color.white );
			logo.setBorder( BorderFactory.createEtchedBorder() );
			panel1.add( logo );
		} catch (Exception ex ) {
		}

		play = new JToggleButton("Play");
		panel1.add( play );
		String os = System.getProperty("os.name").trim();
		if( os.toLowerCase().startsWith("mac") ) {
			play.addActionListener(this);
		}
		save = new JButton("Save");
		panel1.add( save );
		save.addActionListener(this);

		reset = new JButton("Reset");
		panel1.add( reset );
		reset.addActionListener(this);

	//	String os = System.getProperty("os.name").trim();
	//	if( os.toLowerCase().startsWith("mac") ) {
	//		play.setEnabled(false);
	//		save.setEnabled(false);
	//	}
		JPanel panel2 = new JPanel ( new GridLayout(1, 0) );
		frameRate = new JTextField("10");
		panel2.add(frameRate);
		panel2.add( new JLabel( "frames/s" ));
		panel1.add( panel2 );
		play.addActionListener( this);
		frameRate.addActionListener( this);

		panel.add( panel1, "West");
		panel.add(this,"Center");
	}
	public JPanel getPanel() {
		return panel;
	}
	public void setHist( EventHistogram hist ) {
		zHist = hist;
		zRange = hist.getRange();
		zRange0 = new double[] {zRange[0], zRange[1]};
		if( getParent() != null) {
			interval = (zRange0[1]-zRange0[0])/(maxX-minX);
			zHist.setRange( zRange0, interval*4. );
			enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
			side = 0;
			lastX = -1;
			minX = maxX = middle = 0;
			repaint();
		} else {
			interval = hist.interval;
			enableEvents(AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
			side = 0;
			lastX = -1;
			minX = maxX = middle = 0;
		}
	}
	void drawLine() {
		synchronized (getTreeLock()) {
			Graphics2D g = (Graphics2D)getGraphics();
			Dimension dim = getSize();
			g.setXORMode( Color.white );
			g.drawLine(lastX,0,lastX,dim.height);
		}
	}
	public Dimension getMinimumSize() {
		return new Dimension( 100, 40 );
	}
	int nearbyTest( int x ) {
		if( x- minX < 3 && x-minX > -3 ||
				x- maxX < 3 && x-maxX > -3 ) {
			return (x- minX < 3) ? -1 : 1;
		}
		if(side!=0)setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
		return 0;
	}
	public void processMouseEvent( MouseEvent evt ) {
		super.processMouseEvent( evt );
		if( evt.isControlDown() )return;
		if( side==0 ) {
			if( evt.getID()==evt.MOUSE_ENTERED) {
				requestFocus();
				int x = evt.getX();
				side = nearbyTest(x);
				if(side == 0) return;
				setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) );
			}
			return;
		}
		if( evt.getID()==evt.MOUSE_PRESSED) {
			int x = evt.getX();
			lastX=x;
			drawLine();
		} else if( evt.getID()==evt.MOUSE_EXITED) {
			drawLine();
			side = 0;
			setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
			lastX = -1;
		} else if( evt.getID()==evt.MOUSE_RELEASED) {
			if(side==-1) {
				zRange[0] = zRange[0] + 
					(zRange[1]-zRange[0]) * (lastX-minX)
					/ ( maxX-minX );
			} else {
				zRange[1] = zRange[0] + 
					(zRange[1]-zRange[0]) * (lastX-minX)
					/ ( maxX-minX );
			}
			double dx = (zRange[1]-zRange[0])/(maxX-minX);
			zHist.setRange( zRange, dx*4. );
			pmel.map.repaint();
		}
	}
	public void processMouseMotionEvent( MouseEvent evt ) {
		super.processMouseMotionEvent( evt );
		if( evt.isControlDown() ){
			return;
		}
		if( evt.getID()==evt.MOUSE_MOVED ) {
			int x = evt.getX();
			side = nearbyTest(x);
			if(side == 0){
				return;
			}else{
				setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) );
			}
		} else if( evt.getID()==evt.MOUSE_DRAGGED ) {
			int x=evt.getX();
			if( side==0 ){
				side = 1;
			}else if((side==1) &&((x-minX)<10)){
				return;
			}else if ((side==-1)&& ((maxX-x)<10)){
				return;
			}
			drawLine();
			lastX = x;
			drawLine();
		}
	}
	public void paint( Graphics g ) {
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = getSize();
		g.setColor( Color.white );
		g.fillRect( 0, 0, dim.width, dim.height );
		middle = dim.width / 2;
		minX = middle - dim.width/3;
		maxX = middle + dim.width/3;
		double z0 = zRange[0] - .25*( zRange[1]-zRange[0]);
		double dz = 1.5 * ( zRange[1] - zRange[0] ) / dim.width;
		double scale = ((double)dim.height-15) / (double)zHist.getMaxCounts();
		double z;
		GeneralPath path = new GeneralPath();
		path.moveTo( 0f, (float)dim.width);
		XYZ grad = new XYZ(0.,0.,1.);
		for( int i=0 ; i<dim.width ; i++ ) {
			z = z0 + (double)i * dz;
			int y =  dim.height-15 - 
					(int)(scale* (double)zHist.getCounts((float)z));
			Rectangle  r = new Rectangle( i, y, 1, dim.height-15-y);
		//	g.setColor( rgb );
			g.setColor( Color.gray );
			g2.fill( r );
			path.lineTo( (float)i, (float)y);
		}
		g.setColor( Color.black );
		g2.draw( path );
		String valStart = dateString(zRange[0]);
		Rectangle2D bounds = g2.getFont().getStringBounds( valStart, g2.getFontRenderContext() );
		int x = minX - (int) (bounds.getWidth()/2.);
		g2.drawString( valStart, x, 12 );
		String valEnd = dateString(zRange[1]);
		bounds = g2.getFont().getStringBounds( valEnd, g2.getFontRenderContext() );
		x = maxX - (int) (bounds.getWidth()/2.);
		g2.drawString( valEnd, x, 12 );
		g.drawLine( 0, dim.height-15, dim.width, dim.height-15);

		//Set color and draw start and end mark lines
		g.setColor( new Color( 0,0,0,100) );
		g.drawLine( minX, 0, minX, dim.height-15);
		g.drawLine( maxX, 0, maxX, dim.height-15);
		//g.drawLine( middle, 0, middle, dim.height-15);
		int t1 = (int) zRange[0];
	}
	String dateString( double secs ) {
		cal.setTime( new Date( (long)Math.rint(secs*1000) ));
		int yr = cal.get(cal.YEAR);
		int day = cal.get(cal.DAY_OF_YEAR);
		int hr = cal.get(cal.HOUR_OF_DAY);
		int min = cal.get(cal.MINUTE);
		int sec = cal.get(cal.SECOND);

		StringBuffer buf = new StringBuffer();
		buf.append( yr + " " );
		if( day<10 ) buf.append(0);
		if( day<100 ) buf.append(0);
		buf.append(day+" ");
		if( hr<10 )buf.append(0);
		buf.append(hr+":");
		if(min<10)buf.append(0);
		buf.append(min+":");
		if(sec<10)buf.append(0);
		buf.append(sec);
		return buf.toString();
	}
	public void setXY(Point p){
	}
	public void setRect(Rectangle rect) {
	}
	public void zoomTo(Rectangle rect) {
	}
	public void zoomIn(Point p) {
		doZoom( p.x, 2.);
	}
	public void zoomOut(Point p) {
		doZoom( p.x, .5);
	}
	void doZoom( int x, double factor) {
		double range = zRange[1]-zRange[0];
		double p = zRange[0] +range*(x-minX)
				/ ( maxX-minX );
		zRange = new double[] { p-range*.5/factor, p+range*.5/factor };
		double dx = (zRange[1]-zRange[0])/(maxX-minX);
		zHist.setRange( zRange, dx*4. );
		pmel.map.repaint();
	}
	public void run() {
		synchronized( pmel.map.getTreeLock() ) {
			Graphics2D g = pmel.map.getGraphics2D();
			Rectangle2D.Double clip = (Rectangle2D.Double)pmel.map.getClipRect2D();
			double dx = (zRange[1]-zRange[0])/(maxX-minX);
			double t = zRange[0];
			int kount=0;
			Vector data = pmel.current;
			if( data.size()<2 )return;
			double zoom = pmel.map.getZoom();
			double wrap = pmel.map.getWrap();
			Rectangle2D.Double rect = new Rectangle2D.Double(-2./zoom, -2./zoom,
									4./zoom, 4./zoom);
			AffineTransform at = g.getTransform();
			g.setColor( Color.black);
			float[] mRange = new float[] {0f, 0f};
			for(int i=0 ; i<data.size() ; i++) {
				float m = ((PMELEvent)data.get(i)).mag;
				if( i==0 ) mRange[0] = mRange[1] = m;
				else if( m>mRange[1] )mRange[1]=m;
				else if( m<mRange[0] )mRange[0]=m;
			}
			double dm = 1.5/(mRange[1]-mRange[0]);
			for(int i=0 ; i<data.size() ; i++) {
				PMELEvent evt = (PMELEvent)data.get(i);
				while( evt.x<clip.x ) evt.x-=wrap;
				while( evt.x>clip.x+clip.width ) evt.x-=wrap;
				g.translate( (double)evt.x, (double)evt.y );
			//	double scale = .5+dm*(evt.mag-mRange[0]);
			//	g.scale( scale, scale);
				g.fill( rect );
				g.setTransform( at );
			}
			long delay;
			try {
				delay = 1000L / Long.parseLong( frameRate.getText() );
			} catch(NumberFormatException ex) {
				delay = 100L;
			}
			Color[] color = new Color[] {
					Color.black,
					new Color(96,0,0),
					new Color(128,0,0),
					new Color(160,0,0),
					new Color(192,0,0),
					new Color(224,0,0),
					new Color(255,0,0),
					new Color(255,64,0),
					new Color(255,128,0),
					new Color(255,192,0)
				};
		//	while( play.isSelected() ) {
				Vector[] plot = new Vector[10];
				for(int k=0 ; k<10 ; k++)plot[k] = new Vector();
				long time = System.currentTimeMillis();
				kount=0;
				int x = -1;
				int h = getSize().height;
				while( t<zRange[1] ) {
					for( int k=0 ; k<10 ; k++) {
						g.setColor( color[k] );
						for(int i=0 ; i<plot[k].size() ; i++) {
							PMELEvent evt = (PMELEvent)plot[k].get(i);
							while( evt.x<clip.x ) evt.x-=wrap;
							while( evt.x>clip.x+clip.width ) evt.x-=wrap;
							g.translate( (double)evt.x, (double)evt.y );
						//	double scale = .5+dm*(evt.mag-mRange[0]);
						//	g.scale( scale, scale);
							g.fill( rect );
							g.setTransform( at );
						}
						plot[k].removeAllElements();
						if(k<9) {
							for(int i=0 ; i<plot[k+1].size() ; i++) {
								plot[k].add(plot[k+1].get(i));
							}
						}
					}
					t+=dx*4;
					Graphics gg = getGraphics();
					gg.setXORMode( Color.yellow );
					if( x>0 ) gg.drawLine( x,0,x,h );
					x = minX + (int) ( (t-zRange[0]) * (maxX-minX)/(zRange[1]-zRange[0]));
					gg.drawLine( x,0,x,h );
					g.setColor( Color.yellow );
					while( kount<data.size() ) {
						// End of animation cycle. Repaint the map and return
						if( !play.isSelected() ){
							repaint();
							pmel.map.repaint();
							return;
						}
						PMELEvent evt = (PMELEvent)data.get(kount);
						if( evt.time>t )break;
						while( evt.x<clip.x ) evt.x-=wrap;
						while( evt.x>clip.x+clip.width ) evt.x-=wrap;
						g.translate( (double)evt.x, (double)evt.y );
					//	double scale = .5+dm*(evt.mag-mRange[0]);
					//	g.scale( scale, scale);
						g.fill( rect );
						g.setTransform( at );
						plot[9].add( evt);
						kount++;
					}
					long now = System.currentTimeMillis();
					if( now-time<delay ) try {
						Thread.currentThread().sleep( delay-(now-time) );
					} catch(Exception ex) {
					}
					time = now;
				}
					Graphics gg = getGraphics();
					gg.setXORMode( Color.yellow );
					if( x>0 ) gg.drawLine( x,0,x,h );
				for( int j=0 ; j<10 ; j++) {
					for( int k=0 ; k<10 ; k++) {
						g.setColor( color[k] );
						for(int i=0 ; i<plot[k].size() ; i++) {
							PMELEvent evt = (PMELEvent)plot[k].get(i);
							while( evt.x<clip.x ) evt.x-=wrap;
							while( evt.x>clip.x+clip.width ) evt.x-=wrap;
							g.translate( (double)evt.x, (double)evt.y );
							g.fill( rect );
						//	double scale = .5+dm*(evt.mag-mRange[0]);
						//	g.scale( scale, scale);
							g.setTransform( at );
						}
						plot[k].removeAllElements();
						if(k<9) {
							for(int i=0 ; i<plot[k+1].size() ; i++) {
								plot[k].add(plot[k+1].get(i));
							}
						}
					}
				}
		//	}
			play.setSelected( false );
		}
	}
	public void saveMov() throws IOException {
		
		File file = new File(pmel.getDatasetName() + ".mov");
		JFileChooser chooser = MapApp.getFileChooser();
		String title = chooser.getDialogTitle();
		chooser.setDialogTitle("save quickTime animation");
		chooser.setSelectedFile(file);
		chooser.setFileFilter( new FileFilter() {
			public String getDescription() {
				return "quickTime animation (.mov)";
			}
			public boolean accept(File f) {
				return  f.isDirectory() ||
					f.getName().toLowerCase().endsWith(".mov");
			}
		});

		int ok = chooser.showSaveDialog(getTopLevelAncestor());
		if( ok==chooser.CANCEL_OPTION ) {
			chooser.setDialogTitle(title);
			chooser.setAccessory( null );
			return;
		}
		File movFile = chooser.getSelectedFile();
		
		while( !movFile.getName().endsWith(".mov") ) {
			JOptionPane.showMessageDialog( getTopLevelAncestor(), 
						"File name must end with \".mov\"");
			ok = chooser.showSaveDialog(getTopLevelAncestor());
			if( ok==chooser.CANCEL_OPTION ) {
				chooser.setDialogTitle(title);
				chooser.setAccessory( null );
				return;
			}
			movFile = chooser.getSelectedFile();
		}
		chooser.setDialogTitle(title);
		chooser.setAccessory( null );
		Rectangle imr = pmel.map.getVisibleRect();
		BufferedImage im0 = new BufferedImage(imr.width, imr.height, BufferedImage.TYPE_INT_RGB);
		BufferedImage im = new BufferedImage(imr.width, imr.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g0 = im0.createGraphics();
		Graphics2D g = im.createGraphics();
		g.translate(-imr.getX(), -imr.getY());
		pmel.visible.setSelected(false);
		AffineTransform xform = g.getTransform();
		pmel.map.paint(g);
		g0.drawImage(im, 0, 0, this);
		g.setTransform(xform);
		pmel.visible.setSelected(true);
		Rectangle2D.Double clip = (Rectangle2D.Double)pmel.map.getClipRect2D();
		double dx = (zRange[1]-zRange[0])/(maxX-minX);
		double t = zRange[0];
		int kount=0;
		Vector data = pmel.current;
		double zoom = pmel.map.getZoom();
		g.scale( zoom, zoom );
		double wrap = pmel.map.getWrap();
		Rectangle2D.Double rect = new Rectangle2D.Double(-2./zoom, -2./zoom,
								4./zoom, 4./zoom);
		AffineTransform at = g.getTransform();
/*
		g.setColor( Color.black );
		for(int i=0 ; i<data.size() ; i++) {
			PMELEvent evt = (PMELEvent)data.get(i);
			while( evt.x<clip.x ) evt.x-=wrap;
			while( evt.x>clip.x+clip.width ) evt.x-=wrap;
			g.translate( (double)evt.x, (double)evt.y );
			g.fill( rect );
			g.setTransform( at );
		}
*/
		int rate = 20;
			try {
				rate = 2*Integer.parseInt( frameRate.getText() );
			} catch(NumberFormatException ex) {
				rate = 20;
			}
		Color[] color = new Color[] {
				new Color(96,0,0,25),
				new Color(96,0,0,50),
				new Color(96,0,0,75),
				new Color(96,0,0,100),
				new Color(96,0,0,125),
				new Color(96,0,0,150),
				new Color(96,0,0,175),
				new Color(96,0,0,200),
				new Color(96,0,0,225),
				new Color(96,0,0),
				new Color(128,0,0),
				new Color(160,0,0),
				new Color(192,0,0),
				new Color(224,0,0),
				new Color(255,0,0),
				new Color(255,64,0),
				new Color(255,128,0),
				new Color(255,192,0),
				new Color(255,255,0),
				new Color(255,255,0)
			};
		Vector[] plot = new Vector[20];
		for(int k=0 ; k<20 ; k++)plot[k] = new Vector();
		kount=0;
		int x = -1;
		int nFrame=20;
		while( t<zRange[1] ) {
			t+=dx*2;
			nFrame++;
		}
		QT qt = new QT( nFrame, rate, imr.width, imr.height, movFile);
		int h = getSize().height;
		t = zRange[0];
		System.out.println(nFrame +" frames");
		nFrame=0;
		g.setFont( new Font("Monospaced", Font.PLAIN, 12 ));
		Rectangle2D tmp = g.getFont().getStringBounds( "2000 222 22:22:22", 
						g.getFontRenderContext() );
	//	Rectangle dateRect = new Rectangle( (int)(tmp.getX()-2),
	//				(int)(tmp.getY()-2),
	//				(int)(tmp.getWidth()+4),
	//				(int)(tmp.getHeight()+4) );
		Rectangle dateRect = new Rectangle( -2, -2, 
					(int)(tmp.getWidth()+4),
					(int)(tmp.getHeight()+4) );

		while( t<zRange[1] ) {
			g = im.createGraphics();
			g.setTransform( new AffineTransform() );
			g.drawImage( im0, 0, 0, this);
			g.setColor( Color.white );
			g.translate( 2, 2 );
			g.fill( dateRect );
			g.setColor( Color.black );
			g.draw( dateRect );
			g.drawString( dateString( t+dx ), 2, 12);
			g.setTransform( at );
			t+=dx*2;
			for( int k=0 ; k<20 ; k++) {
				g.setColor( color[k] );
				for(int i=0 ; i<plot[k].size() ; i++) {
					PMELEvent evt = (PMELEvent)plot[k].get(i);
					while( evt.x<clip.x ) evt.x-=wrap;
					while( evt.x>clip.x+clip.width ) evt.x-=wrap;
					g.translate( (double)evt.x, (double)evt.y );
					g.fill( rect );
					g.setTransform( at );
				}
				plot[k].removeAllElements();
				if(k<19) {
					for(int i=0 ; i<plot[k+1].size() ; i++) {
						plot[k].add(plot[k+1].get(i));
					}
				}
			}
			Graphics gg = getGraphics();
			gg.setXORMode( Color.yellow );
			if( x>0 ) gg.drawLine( x,0,x,h );
			x = minX + (int) ( (t-zRange[0]) * (maxX-minX)/(zRange[1]-zRange[0]));
			gg.drawLine( x,0,x,h );
			g.setColor( Color.yellow );
			while( kount<data.size() ) {
				PMELEvent evt = (PMELEvent)data.get(kount);
				if( evt.time>t )break;
				while( evt.x<clip.x ) evt.x-=wrap;
				while( evt.x>clip.x+clip.width ) evt.x-=wrap;
				g.translate( (double)evt.x, (double)evt.y );
				g.fill( rect );
				g.setTransform( at );
				plot[19].add( evt);
				kount++;
			}
			int size = qt.addImage( im );
			nFrame++;
			System.out.println( nFrame +"\t"+ size +" bytes");
		//	Graphics gg = getGraphics();
		//	gg.setXORMode( Color.yellow );
		//	if( x>0 ) gg.drawLine( x,0,x,h );
		}
		for( int j=0 ; j<20 ; j++) {
			g.setTransform( new AffineTransform() );
			g.drawImage( im0, 0, 0, this);
			g.setTransform( at );
			for( int k=0 ; k<20 ; k++) {
				g.setColor( color[k] );
				for(int i=0 ; i<plot[k].size() ; i++) {
					PMELEvent evt = (PMELEvent)plot[k].get(i);
					while( evt.x<clip.x ) evt.x-=wrap;
					while( evt.x>clip.x+clip.width ) evt.x-=wrap;
					g.translate( (double)evt.x, (double)evt.y );
					g.fill( rect );
					g.setTransform( at );
				}
				plot[k].removeAllElements();
				if(k<19) {
					for(int i=0 ; i<plot[k+1].size() ; i++) {
						plot[k].add(plot[k+1].get(i));
					}
				}
			}
			int size = qt.addImage( im );
			nFrame++;
			System.out.println( nFrame +"\t"+ size +" bytes");
		}
		for (JRadioButton ds: pmel.datasets) {
			if (ds.isSelected()) {
				MapApp.sendLogMessage("Saving_or_Downloading&portal="+pmel.getDBName()+"&what=movie&area=" + ds.getText());
			}
		}
	}

	/**
	 	Save options
	 	--NO POINT IN USING THIS FUNCTION SINCE saveEvents is not enabled - NSS 07/14/21
	 */
	void save() {

		JPanel panel3 = new JPanel(new GridLayout(0,1));

		ButtonGroup saveType = new ButtonGroup();
		JRadioButton saveMovie = new JRadioButton("Save Movie", true);
		JRadioButton saveEvents = new JRadioButton("Save events occuring in selected time.");

		panel3.add(saveMovie);
		// panel3.add(saveEvents); // For now blank out save events, never completed.

		saveMovie.addActionListener(this);
		saveEvents.addActionListener(this);

		saveType.add(saveMovie);
		saveType.add(saveEvents);

		int c = JOptionPane.showConfirmDialog( getTopLevelAncestor(), panel3, "Save", JOptionPane.OK_CANCEL_OPTION);
		if (c == JOptionPane.CANCEL_OPTION)
			return;

		saveMovie.setEnabled(false);
		saveEvents.setEnabled(false);

		if (saveMovie.isSelected()) {
			//FIX: Lets saveMov method receive a file.
			try {
					saveMov();
			} catch (IOException ex ) {
					ex.printStackTrace();
			}
		}
		else if (saveEvents.isSelected()) {
			//Incomplete.
			File file = null;

			int confirm = JOptionPane.NO_OPTION;

			while (confirm == JOptionPane.NO_OPTION) {
				JFileChooser chooser = MapApp.getFileChooser();
				int ok = chooser.showSaveDialog(getTopLevelAncestor());

				if (ok == chooser.CANCEL_OPTION)
					return;
				
				file = chooser.getSelectedFile();

				if (file.exists()) {
					confirm = JOptionPane.showConfirmDialog(getTopLevelAncestor(), "File exists, Overwrite?");
					if (confirm == JOptionPane.CANCEL_OPTION) return;
				}
				else
					break;
			}
		}
	}

	public void actionPerformed(ActionEvent evt) {
		// Action Events to happen on reset,play, and save
		if( evt.getSource()==reset) {
			zRange = new double[] {zRange0[0], zRange0[1]};
			double dx = (zRange[1]-zRange[0])/(maxX-minX);
			zHist.setRange( zRange, dx*4. );
			repaint();
			pmel.map.repaint();
		} else if(evt.getSource()==save) {
			try {
				saveMov();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if(evt.getSource()==play) {
			if( play.isSelected() ) {
				(new Thread(this)).start();
			}
		}
	}
	public Double getZoomValue() {
		return null;
	}
	public void zoomSpeed(Point p, Double d) {

	}
}

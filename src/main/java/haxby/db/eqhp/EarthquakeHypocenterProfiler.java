package haxby.db.eqhp;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.geomapapp.util.XML_Menu;

import haxby.db.Database;
import haxby.db.custom.DBDescription;
import haxby.db.custom.OtherDBInputDialog;
import haxby.db.custom.UnknownDataSet;
import haxby.map.MapApp;
import haxby.map.XMap;

public class EarthquakeHypocenterProfiler implements Database, ActionListener {
	
	private Map<String, UnknownDataSet> data;
	private BidiMap<String, String> urlToName;
	private String[] usgsUrls;
	private XMap map;
	
	private boolean isLoaded = false, isDataShowing = false, enabled = false;
	private JPanel contentPane, dataPane;
	private String currentDataset;
	
	private JComboBox<String> dropdown;
	
	public EarthquakeHypocenterProfiler(XMap mapIn) {
		data = new HashMap<>();
		urlToName = new DualHashBidiMap<>();
		isLoaded = false;
		map = mapIn;
	}
	
	public String nameForUrl(String url) {
		if(null != urlToName && urlToName.containsKey(url)) {
			return urlToName.get(url);
		}
		return null;
	}
	
	public String urlFoName(String name) {
		if(null != urlToName && urlToName.containsValue(name)) {
			return urlToName.getKey(name);
		}
		return null;
	}
	
	private void initContentPane() {
		contentPane = new JPanel();
		contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
		contentPane.setMaximumSize(new Dimension(200, contentPane.getMaximumSize().height));
	}
	
	private void initDataPane() {
		dataPane = new JPanel();
		dataPane.setLayout(new GridLayout(0,1));
		dataPane.setMaximumSize(new Dimension(dataPane.getMaximumSize().width, 200));
		dataPane.setPreferredSize(dataPane.getMaximumSize());
	}
	
	private void resetDataPane() {
		if(null == dataPane) {
			initDataPane();
		}
		else {
			dataPane.removeAll();
			isDataShowing = false;
		}
	}
	
	private void showData() {
		resetDataPane();
		if(null != currentDataset && data.containsKey(currentDataset) && !isDataShowing) {
			data.get(currentDataset).setSymbolShape(XML_Menu.getXML_Menu(currentDataset).symbol_shape);
			data.get(currentDataset).setColor(Color.RED);
			dataPane.add(data.get(currentDataset).tableSP);
			((MapApp)map.getApp()).addDBToDisplay(this);
			isDataShowing = true;
		}
		else {
			isDataShowing = false;
		}
		map.repaint();
	}

	@Override
	public void draw(Graphics2D g) {
		if(null != currentDataset && data.containsKey(currentDataset) && null != g) {
			data.get(currentDataset).draw(g);
		}
	}

	@Override
	public String getDBName() {
		return "Earthquake Hypocenter Profiler";
	}

	@Override
	public String getCommand() {
		return "earthquake_hypocenter_cmd";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "Allows users to view and generate profiles of earthquake depths near a drawn line or great circle arc.";
	}
	
	private UnknownDataSet getData(String url, String name) {
		if(null == url) {
			if(null != name && data.containsKey(name)) {
				return data.get(name);
			}
			return null;
		}
		if(null == name) {
			String[] splitUrl = url.split("/");
			name = splitUrl[splitUrl.length-1];
			name = name.substring(0, name.lastIndexOf("."));
		}
		if(!urlToName.containsKey(url) || !urlToName.get(url).equals(name)) {
			urlToName.put(url, name);
		}
		if(data.containsKey(name)) {
			return data.get(name);
		}
		Container c = map.getParent();
		while(!(c instanceof Frame)) {
			c = c.getParent();
		}
		int datasetType = UnknownDataSet.ASCII_URL;
		//c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		OtherDBInputDialog dialog = new OtherDBInputDialog((Frame)c, name, url, datasetType);
		DBDescription description = dialog.desc;
		String tblStr = dialog.input.getText();
		String delim = dialog.getDelimeter();
		UnknownDataSet uds = new UnknownDataSet(description, tblStr, delim, MapApp.getApp().getMap());
		data.put(name, uds);
		//c.setCursor(Cursor.getDefaultCursor());
		return uds;
	}

	@Override
	public boolean loadDB() {
		if(!isLoaded) {
			if(null == contentPane) {
				initContentPane();
			}
			JMenu menu = ((JMenu)XML_Menu.getMenuItem(XML_Menu.getXML_Menu("Global (USGS-ANSS Catalog)")));
			List<String> usgsUrlsList = new ArrayList<>();
			for(int i = 0; i < menu.getItemCount(); i++) {
				JMenuItem item = menu.getItem(i);
				String text = item.getText();
				if(text.startsWith("Magnitude ")) {
					String url = (String) XML_Menu.getXML_Menu(item).layer_url;
					usgsUrlsList.add(url);
					urlToName.put(url, text);
				}
			}
			//if there's nothing to load, no need to continue
			if(0 == usgsUrlsList.size()) {
				return false;
			}
			usgsUrls = usgsUrlsList.toArray(new String[0]);
			String[] names = urlToName.values().toArray(new String[0]);
			//sort in reverse order for now
			Arrays.sort(names);
			dropdown = new JComboBox<>(names);
			dropdown.insertItemAt("- Select One -", 0);
			dropdown.setSelectedIndex(0);
			contentPane.add(dropdown);
			dropdown.addActionListener(this);
			isLoaded = true;
		}
		return true;
	}

	@Override
	public boolean isLoaded() {
		return isLoaded;
	}

	@Override
	public void unloadDB() {
		isLoaded = false;
	}

	@Override
	public void disposeDB() {
		urlToName.clear();
		data.clear();
		contentPane.removeAll();
		dataPane.removeAll();
		dropdown = null;
		contentPane = null;
		dataPane = null;
		unloadDB();
		System.gc();
	}

	@Override
	public void setEnabled(boolean tf) {
		enabled = tf;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return enabled;
	}

	@Override
	public JComponent getSelectionDialog() {
		if(null == contentPane) {
			initContentPane();
		}
		return contentPane;
	}

	@Override
	public JComponent getDataDisplay() {
		if(null == dataPane) {
			initDataPane();
		}
		return dataPane;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(dropdown) && dropdown.getSelectedIndex() > 0) {
			String name = dropdown.getItemAt(dropdown.getSelectedIndex());
			System.out.println("You selected " + name);
			String url = urlToName.getKey(name);
			System.out.println("Getting data from " + url);
			MapApp.anchor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			getData(url, name);
			System.out.println("Got the data");
			currentDataset = name;
			showData();
			MapApp.anchor.setCursor(Cursor.getDefaultCursor());
			System.out.println("The data should be showing now");
		}
	}

}

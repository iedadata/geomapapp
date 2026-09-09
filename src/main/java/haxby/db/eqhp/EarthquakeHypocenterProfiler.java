package haxby.db.eqhp;

import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.geomapapp.util.XML_Menu;

import haxby.db.Database;
import haxby.db.custom.DBDescription;
import haxby.db.custom.OtherDBInputDialog;
import haxby.db.custom.UnknownDataSet;
import haxby.map.MapApp;

public class EarthquakeHypocenterProfiler implements Database {
	
	private Map<String, UnknownDataSet> data;
	private Map<String, String> urlToName;
	
	private String[] usgsUrls;
	
	public EarthquakeHypocenterProfiler() {
		data = new HashMap<>();
		urlToName = new HashMap<>();
	}

	@Override
	public void draw(Graphics2D g) {
		// TODO Auto-generated method stub
		
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
		return null;
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
		Container c = null == MapApp.getApp() || null == MapApp.getApp().getMap() ? null : MapApp.getApp().getMap().getParent();
		int datasetType = UnknownDataSet.ASCII_URL;
		OtherDBInputDialog dialog = new OtherDBInputDialog((Frame)c, name, url, datasetType);
		DBDescription description = dialog.desc;
		String tblStr = dialog.input.getText();
		String delim = dialog.getDelimeter();
		UnknownDataSet uds = new UnknownDataSet(description, tblStr, delim, MapApp.getApp().getMap());
		data.put(name, uds);
		return uds;
	}

	@Override
	public boolean loadDB() {
		JMenu menu = ((JMenu)XML_Menu.getMenuItem(XML_Menu.getXML_Menu("Global (USGS-ANSS Catalog)")));
		List<String> usgsUrlsList = new ArrayList<>();
		for(int i = 0; i < menu.getItemCount(); i++) {
			JMenuItem item = menu.getItem(i);
			String text = item.getText();
			if(text.startsWith("Magnitude ")) {
				String url = (String) XML_Menu.getXML_Menu(item).layer_url;
				usgsUrlsList.add(url);
				urlToName.put(text, url);
			}
		}
		usgsUrls = usgsUrlsList.toArray(new String[0]);
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLoaded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unloadDB() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disposeDB() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEnabled(boolean tf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public JComponent getSelectionDialog() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JComponent getDataDisplay() {
		// TODO Auto-generated method stub
		return null;
	}

}

package haxby.util;

import haxby.map.MapApp;

import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.util.Map;
import java.util.HashMap;

public class BrowseURL {
	public static String replaceURL =
		PathUtil.getPath("REPLACE_WITH_ROOT_URL", MapApp.BASE_URL);
	public static Map<String, String> urlReplacements = new HashMap<>();
	static {
		urlReplacements.put("http://app.geomapapp.org/gma_html/help/User_Guide/User_Guide.pdf", MapApp.NEW_BASE_URL + "gma_html/help/User_Guide/User_Guide.pdf");
		urlReplacements.put("http://www.geomapapp.org/terms_of_use_gma.html", MapApp.NEW_BASE_URL + "terms_of_use_gma.html");
		urlReplacements.put("https://app.geomapapp.org/gma_html/help/User_Guide/User_Guide.pdf", MapApp.NEW_BASE_URL + "gma_html/help/User_Guide/User_Guide.pdf");
		urlReplacements.put("https://www.geomapapp.org/terms_of_use_gma.html", MapApp.NEW_BASE_URL + "terms_of_use_gma.html");
	}

	public static void browseURL(String url) {
		browseURL(url, true);
	}

	public static void browseURL(String urlStr, boolean showErrorDialog) {
		if (urlStr == null || urlStr.length() == 0) return;
		try {
			URL url = URLFactory.url(urlStr);

			if (MapApp.AT_SEA && url.toString().startsWith("http")) {
				if(urlReplacements.containsKey(urlStr)) {
					urlStr = urlReplacements.get(urlStr);
					url = URLFactory.url(urlStr);
				}
				else if (urlStr.startsWith(replaceURL)) {
					urlStr = urlStr.replace(replaceURL, MapApp.NEW_BASE_URL);
					url = URLFactory.url(urlStr);
				}
				else if (!urlStr.startsWith(MapApp.NEW_BASE_URL)) {
					atSeaErrorMessage(urlStr);
					return;
				}
			}
			com.Ostermiller.util.Browser.displayURL(url.toString());
		} catch (IOException e) {
			if (showErrorDialog) {
				JOptionPane.showMessageDialog(null, "Could not display url: \n" + urlStr);
				e.printStackTrace();
			}
		} 
	}

	private static void atSeaErrorMessage(String url) {
		JPanel p = new JPanel(new BorderLayout());
		p.add( new JLabel("<html>Sorry, you are not permited to visit an external website.<br>" +
				" However the URL you requested is:</html>"),
				BorderLayout.NORTH);
		final JTextArea ta = new JTextArea(url);
		ta.addFocusListener( new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				ta.selectAll();
			}
		});
		ta.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				ta.selectAll();
			}
		});
		p.add(ta);
		JOptionPane.showMessageDialog(null, p, "Cannot Access URL", JOptionPane.ERROR_MESSAGE);
	}
}

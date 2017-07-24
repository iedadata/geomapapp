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

public class BrowseURL {
	public static String replaceURL =
		PathUtil.getPath("REPLACE_WITH_ROOT_URL", MapApp.BASE_URL);

	public static void browseURL(String url) {
		browseURL(url, true);
	}

	public static void browseURL(String urlStr, boolean showErrorDialog) {
		if (urlStr == null) return;
		try {
			URL url = URLFactory.url(urlStr);

			if (MapApp.AT_SEA && url.toString().startsWith("http")) {
				if (urlStr.startsWith(replaceURL)) {
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

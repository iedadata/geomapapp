package org.geomapapp.credit;

import haxby.map.MapApp;
import haxby.util.BrowseURL;
import haxby.util.PathUtil;

import javax.swing.*;

import java.awt.event.*;
import java.awt.*;

import com.Ostermiller.util.Browser;
/**
 * Creation of buttons which will perform a browseURL call to outside sources.
 * Retrieves the destination url from a remote xml file with PathUtil.
 * @version 2.3.1
 * @since 1.1
 */
public class OtherSources {
	JPanel panel;
	//Path on server to locate source xml file
	String CREDIT_PATH_URLS = PathUtil.getPath("CREDIT_PATH_URLS",
			MapApp.BASE_URL+"/gma_credit/html/GMA_Credit_paths.xml");
	String MARINE_URL = "http://www.gebco.net";
	String LAND_URL = "http://asterweb.jpl.nasa.gov/gdem.asp";
	String ANTARCTIC_URL = "http://www.antarctica.ac.uk/bas_research/data/access/bedmap/";
	String ARCTIC_URL = "http://www.ngdc.noaa.gov/mgg/bathymetry/arctic/arctic.html";
	String SRTM_URL = "http://www2.jpl.nasa.gov/srtm/";
	String NED_URL = "http://ned.usgs.gov/";
	
	public OtherSources() {
		panel = new JPanel(new BorderLayout());

		JPanel base = new JPanel(new GridLayout(0,2));
		JLabel marineB = new JLabel("Marine");
		JButton b = new JButton("GEBCO 2014");
		base.add(marineB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_MARINE", MARINE_URL);
				BrowseURL.browseURL(url);
			}
		});

		JLabel landB = new JLabel("Land");
		b = new JButton("ASTER");
		base.add(landB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_LAND_ASTER", LAND_URL);
				BrowseURL.browseURL(url);
			}
		});

		JLabel antB = new JLabel("Antarctic");
		b = new JButton("BEDMAP");
		base.add(antB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_ANTARCTIC_BEDMAP", ANTARCTIC_URL);
				BrowseURL.browseURL(url);
			}
		});

		JLabel arcticB = new JLabel("Arctic");
		b = new JButton("IBCAO");
		base.add(arcticB);
		base.add(b);
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_ARCTIC_IBCAO", ARCTIC_URL);
				BrowseURL.browseURL(url);
			}
		});
		base.setBorder( BorderFactory.createTitledBorder("Base Map"));
		panel.add(base,"North");

		JLabel usB = new JLabel("US");
		b = new JButton("NED");
		b.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				PathUtil.loadNewPaths(CREDIT_PATH_URLS);
				String url = PathUtil.getPath("INFO_PATH_NED", NED_URL);
				BrowseURL.browseURL(url);
			}
		});
		base = new JPanel(new GridLayout(0,2));
		base.add(usB);
		base.add(b);
		base.setBorder( BorderFactory.createTitledBorder("High Resolution Land"));
		panel.add(base,"South");

	}
	public JPanel getPanel() {
		return panel;
	}
}

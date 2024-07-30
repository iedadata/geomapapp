package org.geomapapp.grid;


import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataSourceException;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.geotools.swing.data.JFileDataStoreChooser;

/*
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.io.netcdf.NetCDFReader;
import org.geotools.data.DataSourceException;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
*/

public class ImportGridGT {
	public static Grid2D importGeoTIFF(File gtFile) {
		try {
			GeoTiffReader reader = new GeoTiffReader(gtFile);
			GridCoverage2D coverage = reader.read(null);
			System.out.println(coverage);
			System.out.println();
			CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
			System.out.println(crs);
		} catch (DataSourceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {
		System.out.println("Choose a GeoTIFF file to import");
		File f = JFileDataStoreChooser.showOpenFile("tif", null);
		System.out.println("You chose " + f);
		importGeoTIFF(f);
	}
}

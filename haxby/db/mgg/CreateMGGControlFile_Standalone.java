package haxby.db.mgg;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.JOptionPane;

import haxby.nav.ControlPoint;
import haxby.nav.Nav;
import haxby.proj.Projection;
import haxby.proj.ProjectionFactory;

/*
 * This is the stand-alone version of CreateMGGControlFile.java
 *
 * CreateMGGControlFile.java should be used for importing files within GMA.  This stand-alone version
 * should be used to create a control file on the server for all input files in a directory.  
 * This version is independent of the GMA application.
 * Use the Debug Configurations to set the arguments for the main method.
 * 	args[0] = mgd77 directory name,
 *	eg /Users/Neville/Desktop/seafloor/data/mgds/web/app.geomapapp.org/htdocs/data/portals/mgd77/nevtest-mgd77
 *	args[1] = tracks name, eg NGDC
 */
public class CreateMGGControlFile_Standalone {

	static double[] testLon = new double[5000];
	static double[] testLat = new double[5000];
	public File MGD77file;
	public File mggDir;
	public File outputControlFile;
	public File outputDataFile;
	int nt = 0;
	int ng = 0;
	int nm = 0;

	public CreateMGGControlFile_Standalone(File inputFile, File inputDir, File outputFile) {
		MGD77file = inputFile;
		mggDir = inputDir;
		outputControlFile = outputFile;
	}

	public boolean createControlFile() {
		return createControlFile(false);
	}

	public boolean createControlFile(boolean append) {
		try {
			Projection proj = ProjectionFactory.getMercator(20000);
			String leg, token, s;
			int nan = 0x80000000;
			boolean dataFound = false;

			String extension = MGD77file.getName().substring(MGD77file.getName().lastIndexOf('.'));
			// check if the file extension implies this is a header file
			if (extension.equals(".h77") || extension.equals(".h77t")) {
				importMGD77Header(MGD77file);
				return true;
			}

			DataOutputStream out = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(outputControlFile, append)));

			leg = MGD77file.getName().replace(extension, "");

			// System.out.println(leg);

			readMGD77(MGD77file);
	
			int[] count = { 0, 0, 0 };
			Nav nav = new Nav("01");

			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(outputDataFile)));

			while ((s = in.readLine()) != null) {

				StringTokenizer st = new StringTokenizer(s);
				int[] record = new int[6];
				String temp5 = st.nextToken();
				if (temp5.substring(0, 1).compareTo("-") == 0) {
					record[0] = 0;
				} else {
					record[0] = Integer.parseInt(temp5);
				}
				record[1] = (int) (1000000. * Double.parseDouble(st.nextToken()));
				record[2] = (int) (1000000. * Double.parseDouble(st.nextToken()));
				token = st.nextToken();
				boolean ok = false;

				if (token.equals("NaN")) {
					record[3] = nan;
				} else {
					record[3] = (int) (10. * Double.parseDouble(token));
					count[0] += 1;
					ok = true;
				}
				token = st.nextToken();
				if (token.equals("NaN")) {
					record[4] = nan;
				} else {
					record[4] = (int) (10. * Double.parseDouble(token));
					count[1] += 1;
					ok = true;
				}
				token = st.nextToken();
				record[5] = (int) (10. * Double.parseDouble(token));
				count[2] += 1;
				if (token.equals("NaN")) {
					record[5] = nan;
				} else {
					ok = true;
				}
				if (ok) {
					dataFound = true;
				}
				nav.addPoint(record[0], 1.e-06 * record[1], 1.e-06 * record[2]);
			}

			in.close();

			if (!dataFound) {
				System.out.println("No data found for " + leg);
				out.flush();
				out.close();
				return false;
			}

			//int nraw = nav.getSize();
			nav.computeControlPoints(proj, 20000., 15.);

			Vector cpts = nav.getControlPoints();

			if (cpts.size() == 0) {
				System.out.println("No control points found for " + leg);
				out.flush();
				out.close();
				return false;
			}

			int start = ((ControlPoint) ((Vector) cpts.get(0)).get(0)).time;
			Vector seg = (Vector) cpts.get(cpts.size() - 1);
			int end = ((ControlPoint) seg.get(seg.size() - 1)).time;
			out.writeUTF(leg.toUpperCase());
			out.writeInt(cpts.size());

			// GMA: 1.4.8: TESTING
			// System.out.println("Types loaded:");
			// System.out.println(nt + "\t" + ng + "\t" + nm);

			out.writeInt(nt);
			out.writeInt(ng);
			out.writeInt(nm);
			out.writeInt(start);
			out.writeInt(end);
			int ncpt = 0;
			double[] wesn = new double[] { 0., 0., 0., 0. };
			double lon0 = 0.;

			for (int iseg = 0; iseg < cpts.size(); iseg++) {
				seg = (Vector) cpts.get(iseg);
				out.writeInt(seg.size());
				for (int r = 0; r < seg.size(); r++) {
					ControlPoint cpt = (ControlPoint) seg.get(r);
					out.writeInt((int) Math.rint(1.e06 * cpt.x));
					out.writeInt((int) Math.rint(1.e06 * cpt.y));
					if (ncpt == 0) {
						wesn[0] = wesn[1] = cpt.x;
						lon0 = cpt.x;
						wesn[2] = wesn[3] = cpt.y;
					} else {
						while (cpt.x > lon0 + 180.)
							cpt.x -= 360.;
						while (cpt.x < lon0 - 180.)
							cpt.x += 360.;
						if (cpt.x < wesn[0])
							wesn[0] = cpt.x;
						else if (cpt.x > wesn[1])
							wesn[1] = cpt.x;
						lon0 = (wesn[0] + wesn[1]) / 2.;
						if (cpt.y < wesn[2])
							wesn[2] = cpt.y;
						else if (cpt.y > wesn[3])
							wesn[3] = cpt.y;
					}
					ncpt++;
				}
			}

			out.flush();
			out.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Not able to import: " + MGD77file, "Import Error",
					JOptionPane.ERROR_MESSAGE);
			outputControlFile.delete();
			outputDataFile.delete();
			return false;
		}
	}

	public void readMGD77(File mgd77leg) throws IOException {

		String extension = mgd77leg.getName().substring(mgd77leg.getName().lastIndexOf('.'));
		String leg = MGD77file.getName().replace(extension, "").toUpperCase();
		// check if the file extension implies the file is an MGD77T file
		if (extension.equals(".m77t")) {
			readMGD77T(mgd77leg);
			return;
		} else if (extension.equals(".mgd77")) {
			readMGD77Conv(mgd77leg);
			return;
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(mgd77leg)));
		String s;

		// save the data from the mgd77 file in a data file in the
		// mgg_data_files directory
		MGG.MGG_data_dir.mkdir();
		outputDataFile = new File(MGG.MGG_data_dir, "mgg_data_" + leg);

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDataFile)));

		double[] lon = new double[5000];
		double[] lat = new double[5000];
		float[] topo = new float[5000];
		float[] grav = new float[5000];
		float[] mag = new float[5000];
		String temp = "";
		boolean dataPresent = false;
		int lineNum = 0;
		nt = 0;
		ng = 0;
		nm = 0;

		while ((s = in.readLine()) != null) {
			while (s.length() < 100) {
				s = in.readLine();
				if (s == null) {
					break;
				}
			}

			if (s == null) {
				break;
			}

			// check again if data file is in tabbed format
			if (lineNum == 0) {
				String[] elements = s.split("\t");
				if (elements.length > 2) {
					System.out.println("Looks like this is an MGD77T file");
					in.close();
					out.close();
					readMGD77T(mgd77leg);
					return;
				}
			}

			if (lineNum == lon.length) {
				int len = lon.length;
				double[] tmp = new double[len * 2];
				System.arraycopy(lon, 0, tmp, 0, len);
				lon = tmp;
				tmp = new double[len * 2];
				System.arraycopy(lat, 0, tmp, 0, len);
				lat = tmp;
				float[] tmp1 = new float[len * 2];
				System.arraycopy(topo, 0, tmp1, 0, len);
				topo = tmp1;
				tmp1 = new float[len * 2];
				System.arraycopy(grav, 0, tmp1, 0, len);
				grav = tmp1;
				tmp1 = new float[len * 2];
				System.arraycopy(mag, 0, tmp1, 0, len);
				mag = tmp1;
				// String[] tmp2 = new String[len*2];
				// System.arraycopy( MGD77data, 0, tmp2, 0, len );
				// MGD77data = tmp2;
			}

			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			String tempDate = s.substring(12, 16).trim();

			// int val = Integer.parseInt( s.substring(12, 16) );
			int val = Integer.parseInt(tempDate);
			if (val > 90 && val < 1000) {
				val += 1900;
			} else if (val < 25 && val > -1) {
				val += 2000;
			}
			cal.set(Calendar.YEAR, val);
			val = Integer.parseInt(s.substring(16, 18).trim());
			cal.set(Calendar.MONTH, val - 1);
			val = Integer.parseInt(s.substring(18, 20).trim());
			cal.set(Calendar.DAY_OF_MONTH, val);
			val = Integer.parseInt(s.substring(20, 22).trim());
			cal.set(Calendar.HOUR_OF_DAY, val);
			val = Integer.parseInt(s.substring(22, 27).trim());
			cal.set(Calendar.MINUTE, val / 1000);
			double tempVal = val;
			tempVal /= 1000;
			tempVal %= 1;
			tempVal *= 60;
			val = (int) tempVal;
			cal.set(Calendar.SECOND, val);

			try {
				temp = s.substring(MGGData.MGD77_LON_START_POS, MGGData.MGD77_LON_END_POS + 1);
				for (int i = 0; i < temp.length(); i++) {
					if (!(temp.substring(i, i + 1).equals("9")) && !(temp.substring(i, i + 1).equals("+"))) {
						lon[lineNum] = Double.parseDouble(temp) * MGGData.MGD77_LON_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";

				temp = s.substring(MGGData.MGD77_LAT_START_POS, MGGData.MGD77_LAT_END_POS + 1);
				for (int i = 0; i < temp.length(); i++) {
					if (!(temp.substring(i, i + 1).equals("9")) && !(temp.substring(i, i + 1).equals("+"))) {
						lat[lineNum] = Double.parseDouble(temp) * MGGData.MGD77_LAT_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";

				if (lon[lineNum] < 0) {
					lon[lineNum] += 360.;
				}
			} catch (NumberFormatException ex) {
				continue;
			}
			dataPresent = false;

			try {
				temp = s.substring(MGGData.MGD77_BATHY_START_POS, MGGData.MGD77_BATHY_END_POS + 1);
				for (int i = 0; i < temp.length(); i++) {
					if (!(temp.substring(i, i + 1).equals("9")) && !(temp.substring(i, i + 1).equals("+"))) {
						topo[lineNum] = -1 * Float.parseFloat(temp) / MGGData.MGD77_BATHY_SCALE;
						dataPresent = true;
						// System.out.println(topo[lineNum]);
						break;
					}
				}
				temp = "";
				if (!Float.isNaN(topo[lineNum]) && dataPresent)
					nt++;
				else {
					topo[lineNum] = Float.NaN;
				}
			} catch (NumberFormatException ex) {
				topo[lineNum] = Float.NaN;
			}

			dataPresent = false;

			if (s.length() > MGGData.MGD77_GRAVITY_START_POS) {
				try {
					temp = s.substring(MGGData.MGD77_GRAVITY_START_POS, MGGData.MGD77_GRAVITY_END_POS + 1);
					for (int i = 0; i < temp.length(); i++) {
						if (!(temp.substring(i, i + 1).equals("9")) && !(temp.substring(i, i + 1).equals("+"))) {
							grav[lineNum] = Float.parseFloat(temp) / MGGData.MGD77_GRAVITY_SCALE;
							dataPresent = true;
							break;
						}
					}
					temp = "";
					if (!Float.isNaN(grav[lineNum]) && dataPresent)
						ng++;
					else {
						grav[lineNum] = Float.NaN;
					}
				} catch (NumberFormatException ex) {
					grav[lineNum] = Float.NaN;
				}
			}
			dataPresent = false;

			try {
				temp = s.substring(MGGData.MGD77_MAGNETICS_START_POS, MGGData.MGD77_MAGNETICS_END_POS + 1);
				for (int i = 0; i < temp.length(); i++) {
					if (!(temp.substring(i, i + 1).equals("9")) && !(temp.substring(i, i + 1).equals("+"))) {
						mag[lineNum] = Float.parseFloat(temp) / MGGData.MGD77_MAGNETICS_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";
				if (!Float.isNaN(mag[lineNum]) && dataPresent)
					nm++;
				else {
					mag[lineNum] = Float.NaN;
				}
			} catch (NumberFormatException ex) {
				mag[lineNum] = Float.NaN;
			}

			// System.out.println(1000 + "\t" + lon[lineNum] + "\t" +
			// lat[lineNum] + "\t" + topo[lineNum] + "\t" + mag[lineNum] + "\t"
			// + grav[lineNum] + "\n");
			// out.write(1000 + "\t" + lon[lineNum] + "\t" + lat[lineNum] + "\t"
			// + topo[lineNum] + "\t" + mag[lineNum] + "\t" + grav[lineNum] +
			// "\n");
			String lonString = Double.toString(lon[lineNum]);
			String latString = Double.toString(lat[lineNum]);
			String topoString = Double.toString(topo[lineNum]);
			String gravString = Double.toString(grav[lineNum]);
			String magString = Double.toString(mag[lineNum]);

			if (lonString.length() > 11) {
				lonString = lonString.substring(0, 11);
			}

			if (latString.length() > 8) {
				latString = latString.substring(0, 8);
			}

			if (topoString.indexOf(".") != -1) {
				topoString = topoString.substring(0, topoString.indexOf("."));
			}

			if (gravString.indexOf(".") != -1) {
				gravString = gravString.substring(0, gravString.indexOf("."));
			}

			if (magString.indexOf(".") != -1) {
				magString = magString.substring(0, magString.indexOf("."));
			}

			int defaultDateValue = 0;
			if (cal.getTimeInMillis() / 1000 <= 2147483647 && cal.getTimeInMillis() / 1000 >= -2147483648) {
				// MGD77data[lineNum] = ( cal.getTimeInMillis() / 1000 ) + "\t"
				// + lonString + "\t" + latString + "\t" + topoString + "\t" +
				// magString + "\t" + gravString;

				// GMA 1.4.8: Now outputting data counts in correct order so
				// that types in MGG works correctly
				out.write((cal.getTimeInMillis() / 1000) + "\t" + lonString + "\t" + latString + "\t" + topoString
						+ "\t" + gravString + "\t" + magString + "\n");
			} else {
				// MGD77data[lineNum] = defaultDateValue + "\t" + lonString +
				// "\t" + latString + "\t" + topoString + "\t" + magString +
				// "\t" + gravString;

				// GMA 1.4.8: Now outputting data counts in correct order so
				// that types in MGG works correctly
				out.write(defaultDateValue + "\t" + lonString + "\t" + latString + "\t" + topoString + "\t" + gravString
						+ "\t" + magString + "\n");
			}
			dataPresent = false;
			lineNum++;
		}
		in.close();
		out.flush();
		out.close();
	}

	/*
	 * Read in .mgd77 files that have been created by converting .m77t files using
	 * the command mgd77convert -Fm -Ta *m77t -V
	 */
	public void readMGD77Conv(File mgd77leg) throws IOException {

		String extension = mgd77leg.getName().substring(mgd77leg.getName().lastIndexOf('.'));
		String leg = MGD77file.getName().replace(extension, "").toUpperCase();

		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(mgd77leg)));
		String s;

		// save the data from the mgd77 file in a data file in the
		// mgg_data_files directory
		MGG.MGG_data_dir.mkdir();
		outputDataFile = new File(MGG.MGG_data_dir, "mgg_data_" + leg);

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDataFile)));

		// remove header and save as .a77 file
		File fixedFile = new File(this.mggDir.getPath(), leg + ".a77");
		FileWriter fr = new FileWriter(fixedFile);
		BufferedWriter br = new BufferedWriter(fr);

		// create mgd77 directory to store the .mgd77 files once we have saved them as
		// .a77 files
		File mgd77Dir = new File(MGG.MGG_header_dir.getPath().replace("header", "mgd77"));
		mgd77Dir.mkdir();

		double[] lon = new double[5000];
		double[] lat = new double[5000];
		float[] topo = new float[5000];
		float[] grav = new float[5000];
		float[] mag = new float[5000];
		String temp = "";
		boolean dataPresent = false;
		int lineNum = 0;
		nt = 0;
		ng = 0;
		nm = 0;

		while ((s = in.readLine()) != null) {
			while (s.length() < 100) {
				s = in.readLine();
				if (s == null) {
					break;
				}
			}

			if (s == null) {
				break;
			}

			// check again if data file is in tabbed format
			if (lineNum == 0) {
				String[] elements = s.split("\t");
				if (elements.length > 2) {
					System.out.println("Looks like this is an MGD77T file");
					in.close();
					out.close();
					readMGD77T(mgd77leg);
					return;
				}
			}

			if (lineNum == lon.length) {
				int len = lon.length;
				double[] tmp = new double[len * 2];
				System.arraycopy(lon, 0, tmp, 0, len);
				lon = tmp;
				tmp = new double[len * 2];
				System.arraycopy(lat, 0, tmp, 0, len);
				lat = tmp;
				float[] tmp1 = new float[len * 2];
				System.arraycopy(topo, 0, tmp1, 0, len);
				topo = tmp1;
				tmp1 = new float[len * 2];
				System.arraycopy(grav, 0, tmp1, 0, len);
				grav = tmp1;
				tmp1 = new float[len * 2];
				System.arraycopy(mag, 0, tmp1, 0, len);
				mag = tmp1;
			}

			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			String tempDate = s.substring(12, 16).trim();

			int val = Integer.parseInt(tempDate);
			if (val > 90 && val < 1000) {
				val += 1900;
			} else if (val < 25 && val > -1) {
				val += 2000;
			}
			cal.set(Calendar.YEAR, val);
			val = Integer.parseInt(s.substring(16, 18).trim());
			cal.set(Calendar.MONTH, val - 1);
			val = Integer.parseInt(s.substring(18, 20).trim());
			cal.set(Calendar.DAY_OF_MONTH, val);
			val = Integer.parseInt(s.substring(20, 22).trim());
			cal.set(Calendar.HOUR_OF_DAY, val);
			val = Integer.parseInt(s.substring(22, 27).trim());
			cal.set(Calendar.MINUTE, val / 1000);
			double tempVal = val;
			tempVal /= 1000;
			tempVal %= 1;
			tempVal *= 60;
			val = (int) tempVal;
			cal.set(Calendar.SECOND, val);

			try {
				temp = s.substring(MGGData.MGD77_LON_START_POS, MGGData.MGD77_LON_END_POS + 1);
				for (int i = 0; i < temp.length(); i++) {
					if (!(temp.substring(i, i + 1).equals("0")) && !(temp.substring(i, i + 1).equals("+"))) {
						lon[lineNum] = Double.parseDouble(temp) * MGGData.MGD77_LON_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";

				temp = s.substring(MGGData.MGD77_LAT_START_POS, MGGData.MGD77_LAT_END_POS + 1);
				for (int i = 0; i < temp.length(); i++) {
					if (!(temp.substring(i, i + 1).equals("0")) && !(temp.substring(i, i + 1).equals("+"))) {
						lat[lineNum] = Double.parseDouble(temp) * MGGData.MGD77_LAT_SCALE;
						dataPresent = true;
						break;
					}
				}
				temp = "";

				if (lon[lineNum] < 0) {
					lon[lineNum] += 360.;
				}
			} catch (NumberFormatException ex) {
				continue;
			}
			dataPresent = false;

			try {
				temp = s.substring(MGGData.MGD77_BATHY_START_POS, MGGData.MGD77_BATHY_END_POS + 1);

				if (isValidValue(temp)) {
					topo[lineNum] = -1 * Float.parseFloat(temp) / MGGData.MGD77_BATHY_SCALE;
					dataPresent = true;
				}

				temp = "";
				if (!Float.isNaN(topo[lineNum]) && dataPresent)
					nt++;
				else {
					topo[lineNum] = Float.NaN;
				}
			} catch (NumberFormatException ex) {
				topo[lineNum] = Float.NaN;
			}
			if (!dataPresent) {
				// replace 0's with 9's for empty values
				s = replace0sWith9s(s, MGGData.MGD77_BATHY_START_POS, MGGData.MGD77_BATHY_END_POS);
			}

			dataPresent = false;

			if (s.length() > MGGData.MGD77_GRAVITY_START_POS) {
				try {
					temp = s.substring(MGGData.MGD77_GRAVITY_START_POS, MGGData.MGD77_GRAVITY_END_POS + 1);
					if (isValidValue(temp)) {
						grav[lineNum] = Float.parseFloat(temp) / MGGData.MGD77_GRAVITY_SCALE;
						dataPresent = true;
					}

					temp = "";
					if (!Float.isNaN(grav[lineNum]) && dataPresent)
						ng++;
					else {
						grav[lineNum] = Float.NaN;
					}
				} catch (NumberFormatException ex) {
					grav[lineNum] = Float.NaN;
				}
			}
			if (!dataPresent) {
				// replace 0's with 9's for empty values
				s = replace0sWith9s(s, MGGData.MGD77_GRAVITY_START_POS, MGGData.MGD77_GRAVITY_END_POS);
			}

			dataPresent = false;

			try {
				temp = s.substring(MGGData.MGD77_MAGNETICS_START_POS, MGGData.MGD77_MAGNETICS_END_POS + 1);
				if (isValidValue(temp)) {
					grav[lineNum] = Float.parseFloat(temp) / MGGData.MGD77_MAGNETICS_SCALE;
					dataPresent = true;
				}
				temp = "";
				if (!Float.isNaN(mag[lineNum]) && dataPresent)
					nm++;
				else {
					mag[lineNum] = Float.NaN;
				}
			} catch (NumberFormatException ex) {
				mag[lineNum] = Float.NaN;
			}
			if (!dataPresent) {
				// replace 0's with 9's for empty values
				s = replace0sWith9s(s, MGGData.MGD77_MAGNETICS_START_POS, MGGData.MGD77_MAGNETICS_END_POS);
			}

			String lonString = Double.toString(lon[lineNum]);
			String latString = Double.toString(lat[lineNum]);
			String topoString = Double.toString(topo[lineNum]);
			String gravString = Double.toString(grav[lineNum]);
			String magString = Double.toString(mag[lineNum]);

			if (lonString.length() > 11) {
				lonString = lonString.substring(0, 11);
			}

			if (latString.length() > 8) {
				latString = latString.substring(0, 8);
			}

			if (topoString.indexOf(".") != -1) {
				topoString = topoString.substring(0, topoString.indexOf("."));
			}

			if (gravString.indexOf(".") != -1) {
				gravString = gravString.substring(0, gravString.indexOf("."));
			}

			if (magString.indexOf(".") != -1) {
				magString = magString.substring(0, magString.indexOf("."));
			}

			int defaultDateValue = 0;
			if (cal.getTimeInMillis() / 1000 <= 2147483647 && cal.getTimeInMillis() / 1000 >= -2147483648) {

				out.write((cal.getTimeInMillis() / 1000) + "\t" + lonString + "\t" + latString + "\t" + topoString
						+ "\t" + gravString + "\t" + magString + "\n");
			} else {

				out.write(defaultDateValue + "\t" + lonString + "\t" + latString + "\t" + topoString + "\t" + gravString
						+ "\t" + magString + "\n");
			}
			dataPresent = false;
			lineNum++;

			// write the non-header line to the .a77 file
			br.write(s + "\n");
		}
		in.close();
		out.flush();
		out.close();
		br.close();
		fr.close();

		// move .mgd77 file to mgd77 directory
		File dest = new File(mgd77Dir.getPath(), mgd77leg.getName());
		Files.move(mgd77leg.toPath(), dest.toPath(), REPLACE_EXISTING);
	}

	private String replace0sWith9s(String in, int startInd, int endInd) {
		String nines = "";
		for (int i = startInd; i < endInd + 1; i++) {
			nines += "9";
		}

		String out = in.substring(0, startInd) + nines + in.substring(endInd + 1, in.length());
		return out;
	}

	private Boolean isValidValue(String temp) {
		String nines = "";
		String zeros = "";
		String plusNines = "+";
		String plusZeros = "+";
		for (int i = 0; i < temp.length(); i++) {
			nines += "9";
			zeros += "0";
			if (i != temp.length() - 1) {
				plusNines += "9";
				plusZeros += "0";
			}
		}
		return (!temp.contentEquals(nines) && !temp.contentEquals(zeros) && !temp.contentEquals(plusNines)
				&& !temp.contentEquals(plusZeros));
	}

	/*
	 * Read in tab-delimited MGD77 files
	 */
	public void readMGD77T(File mgd77leg) throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(mgd77leg)));
		String s;
		String extension = mgd77leg.getName().substring(mgd77leg.getName().lastIndexOf('.'));
		String leg = MGD77file.getName().replace(extension, "");
		if (!leg.equals(leg.toUpperCase())) {
			leg = leg.toUpperCase();
			// rename file with uppercase leg name
			File dest = new File(MGG.MGG_header_dir.getPath().replace("header", "data"), leg + extension);
			Files.move(mgd77leg.toPath(), dest.toPath(), REPLACE_EXISTING);
		}

		// save the data from the mgd77 file in a data file in the
		// mgg_data_files directory
		MGG.MGG_data_dir.mkdir();
		outputDataFile = new File(MGG.MGG_data_dir, "mgg_data_" + leg);

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputDataFile)));

		double lon;
		double lat;
		float topo;
		float grav;
		float mag;
		String temp = "";
		boolean dataPresent = false;
		nt = 0;
		ng = 0;
		nm = 0;

		while ((s = in.readLine()) != null) {

			// split the line up into its tab-delimited elements
			String[] elements = s.split("\t");

			// check for header lines by seeing if lat field can be parsed as double
			try {
				Double.parseDouble(elements[MGGData.MGD77T_LAT_FIELD]);
			} catch (Exception e) {
				continue;
			}

			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

			try {
				// get the date field data and split in to year, month and day
				String tempDate = elements[MGGData.MGD77T_DATE_FIELD];

				int val = Integer.parseInt(tempDate.substring(0, 4));
				if (val > 90 && val < 1000) {
					val += 1900;
				} else if (val < 25 && val > -1) {
					val += 2000;
				}
				cal.set(Calendar.YEAR, val);
				val = Integer.parseInt(tempDate.substring(4, 6));
				cal.set(Calendar.MONTH, val - 1);
				val = Integer.parseInt(tempDate.substring(6, 8));
				cal.set(Calendar.DAY_OF_MONTH, val);

				// get the time field and split in to hours, minutes and seconds
				String tempTime = elements[MGGData.MGD77T_TIME_FIELD];
				val = Integer.parseInt(tempTime.substring(0, 2));
				cal.set(Calendar.HOUR_OF_DAY, val);
				val = Integer.parseInt(tempTime.substring(2, 4));
				cal.set(Calendar.MINUTE, val);
				if (tempTime.contains(".")) {
					float decSec = Float.parseFloat(tempTime.substring(tempTime.indexOf(".")));
					cal.set(Calendar.SECOND, (int) (decSec * 60));
				} else
					cal.set(Calendar.SECOND, 0);
			} catch (Exception ex) {
			}

			// get the lat and lon fields
			try {
				temp = elements[MGGData.MGD77T_LON_FIELD];
				lon = Double.parseDouble(temp);
				dataPresent = true;
				if (lon < 0) {
					lon += 360.;
				}
				temp = "";

				temp = elements[MGGData.MGD77T_LAT_FIELD];
				lat = Double.parseDouble(temp);
				temp = "";
			} catch (Exception ex) {
				continue;
			}

			// get any bathymetry data. If none, for this row, add a NaN.
			dataPresent = false;
			try {
				temp = elements[MGGData.MGD77T_BATHY_FIELD];
				topo = -1 * Float.parseFloat(temp);
				dataPresent = true;

				if (!Float.isNaN(topo) && dataPresent && !temp.matches("0"))
					nt++;
				else {
					topo = Float.NaN;
				}

				temp = "";
			} catch (Exception ex) {
				topo = Float.NaN;
			}

			// get any magnetic data. If none, for this row, add a NaN.
			dataPresent = false;
			try {
				temp = elements[MGGData.MGD77T_MAGNETICS_FIELD];
				mag = Float.parseFloat(temp);
				dataPresent = true;

				if (!Float.isNaN(mag) && dataPresent && !temp.matches("0"))
					nm++;
				else {
					mag = Float.NaN;
				}

				temp = "";
			} catch (Exception ex) {
				mag = Float.NaN;
			}

			// get any gravity data. If none, for this row, add a NaN.
			dataPresent = false;
			try {
				temp = elements[MGGData.MGD77T_GRAVITY_FIELD];
				grav = Float.parseFloat(temp);
				dataPresent = true;

				if (!Float.isNaN(grav) && dataPresent && !temp.matches("0"))
					ng++;
				else {
					grav = Float.NaN;
				}

				temp = "";
			} catch (Exception ex) {
				grav = Float.NaN;
			}

			// convert data to strings and tidy up formatting
			String lonString = Double.toString(lon);
			String latString = Double.toString(lat);
			String topoString = Double.toString(topo);
			String gravString = Double.toString(grav);
			String magString = Double.toString(mag);

			if (lonString.length() > 11) {
				lonString = lonString.substring(0, 11);
			}

			if (latString.length() > 8) {
				latString = latString.substring(0, 8);
			}

			if (topoString.indexOf(".") != -1) {
				topoString = topoString.substring(0, topoString.indexOf("."));
			}

			if (gravString.indexOf(".") != -1) {
				gravString = gravString.substring(0, gravString.indexOf("."));
			}

			if (magString.indexOf(".") != -1) {
				magString = magString.substring(0, magString.indexOf("."));
			}

			// write to output data file
			int defaultDateValue = 0;

			if (cal.getTimeInMillis() / 1000 <= 2147483647 && cal.getTimeInMillis() / 1000 >= -2147483648) {
				out.write((cal.getTimeInMillis() / 1000) + "\t" + lonString + "\t" + latString + "\t" + topoString
						+ "\t" + gravString + "\t" + magString + "\n");
			} else {
				out.write(defaultDateValue + "\t" + lonString + "\t" + latString + "\t" + topoString + "\t" + gravString
						+ "\t" + magString + "\n");
			}
			dataPresent = false;
		}

		in.close();
		out.flush();
		out.close();
	}

	/*
	 * import an MGD77 header file
	 */
	public void importMGD77Header(File mgd77leg) throws IOException {
		// copy the file to the mgg_header_files directory

		MGG.MGG_header_dir.mkdir();
		String extension = mgd77leg.getName().substring(mgd77leg.getName().lastIndexOf('.'));
		String leg = MGD77file.getName().replace(extension, "").toUpperCase();
		File dest = new File(MGG.MGG_header_dir, leg + extension);
		Files.copy(mgd77leg.toPath(), dest.toPath(), REPLACE_EXISTING);

	}

	public static void main(String[] args) {
		// args[0] = mgd77 directory name,
		// eg
		// /Users/Neville/Desktop/seafloor/data/mgds/web/app.geomapapp.org/htdocs/data/portals/mgd77/nevtest-mgd77
		// args[1] = tracks name, eg NGDC

		File sioexplorerDir = new File(args[0] + "/data/");
		File[] inputFiles = sioexplorerDir.listFiles();
		java.util.Arrays.sort(inputFiles);

		// set header directory
		MGG.MGG_header_dir = new File(args[0] + "/header/");

		for (File inputFile : inputFiles) {
			// System.out.println(inputFile.getName());
			// if ((int)inputFile.getName().charAt(0) - (int)"S".charAt(0) != 0) continue;
			CreateMGGControlFile_Standalone cmcf = new CreateMGGControlFile_Standalone(inputFile, sioexplorerDir,
					new File(args[0] + "/control/mgg_control_" + args[1]));

			cmcf.createControlFile(true);
		}
	}
}
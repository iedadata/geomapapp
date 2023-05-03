package org.geomapapp.util;

public class OSAdjustment {
	public static enum OS {
		LINUX("current"), MACOS("current_mac"), WINDOWS("current_win");
		public final String gmrt_current;
		private OS(String gmrt_cur_in) {
			gmrt_current = "gmrt/tiles/" + gmrt_cur_in;
		}
	}
	public static OS whichOs;
	public OSAdjustment() {
		String osName = System.getProperty("os.name");
		if(osName.matches(".*Mac.*")) {
			whichOs = OS.MACOS;
		}
		else if(osName.equals("Linux")) {
			whichOs = OS.LINUX;
		}
		else {
			whichOs = OS.WINDOWS;
		}
	}
}

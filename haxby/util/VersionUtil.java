package haxby.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * 
 * @author Alex Strong
 *
 */

public class VersionUtil {
	private static JSONObject versionMap;
	private static URL versionURL;
	
	private VersionUtil() {}
	
	public static void init() {
		versionMap = new JSONObject();
		versionURL = null;
	}
	
	public static void init(String versionURLIn) {
		try {
			versionURL = URLFactory.url(versionURLIn);
			JSONTokener tokener = new JSONTokener(versionURL.openStream());
			versionMap = new JSONObject(tokener);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Object get(String key) {
		return versionMap.get(key);
	}
	
	public static String getVersion(String key) {
		return versionMap.getJSONObject(key).getString("version");
	}
	
	public static String getReleaseDate(String key) {
		return versionMap.getJSONObject(key).getString("release_date");
	}
}

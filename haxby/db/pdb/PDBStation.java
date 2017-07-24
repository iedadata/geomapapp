package haxby.db.pdb;

import haxby.map.MapApp;
import haxby.util.PathUtil;
import haxby.util.URLFactory;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class PDBStation {
	public static PDBStation[] stations = null;
	String id;
	boolean suffix;
	short expedition;
	short location;
	long[] samples;
	byte sampleTechnique;
	byte materialFlags;
	String materialCodes;
	short itemsMeasured;
	byte alterationFlags;
	short rockTypes;
	static boolean loaded = false;

	static String PETDB_PATH = PathUtil.getPath("PORTALS/PETDB_PATH",
			MapApp.BASE_URL+"/data/portals/petdb/");

	public PDBStation(String id,
			long[] samples,
			short expedition,
			short location,
			byte sampleTechnique,
			byte materialFlags,
			short itemsMeasured,
			byte alterationFlags,
			short rockTypes ) {
		if(id.startsWith("@")) {
			this.id = id.substring(1,id.length());
			suffix = true;
		} else {
			this.id = id;
			suffix = false;
		}
		this.samples = samples;
		this.expedition = expedition;
		this.location = location;
		this.sampleTechnique = sampleTechnique;
		this.materialFlags = materialFlags;
		this.itemsMeasured = itemsMeasured;
		this.alterationFlags = alterationFlags;
		this.rockTypes = rockTypes;
	}
	public String getID() {
		if(suffix) {
			if(PDBExpedition.get((int)expedition)==null) return "N/A";
			return PDBExpedition.get((int)expedition).getName()+id;
		} else return id;
	}
	public int getExpeditionIndex() {
		return (int)expedition;
	}
	public PDBExpedition getExpedition() {
		if( expedition<0 || expedition>=PDBExpedition.size() )return null;
		return PDBExpedition.get( (int)expedition );
	}
	public int howManySamples() {
		if(samples==null)return 0;
		return samples.length;
	}
	public int[] getSampleNums() {
		int[] tmp = new int[howManySamples()];
		for( int i=0 ; i<tmp.length ; i++) {
			tmp[i] = (int)samples[i] & 0xffff;
		}
		return tmp;
	}
	public int getTechniqueIndex() {
		return (int)sampleTechnique;
	}
	public int getLocationIndex() {
		return (int)location;
	}
	public PDBLocation getLocation() {
		return PDBLocation.get( (int)location);
	}
	public float getLongitude() {
		return PDBLocation.get( (int)location).lon;
	}
	public float getLatitude() {
		return PDBLocation.get( (int)location).lat;
	}
	public double getX() {
		return PDBLocation.get( (int)location).getX();
	}
	public double getY() {
		return PDBLocation.get( (int)location).getY();
	}
	public Point2D getMapXY() {
		return PDBLocation.get( (int)location).getMapXY();
	}
	public boolean hasMaterials( int materials ) {
		return ((int)materialFlags & materials)!=0;
	}
	public int getMaterials() {
		return (int)materialFlags;
	}
	public boolean hasDataType( int type ) {
		return ((int)itemsMeasured & type)!=0;
	}
	public boolean hasRockType( int type ) {
		return ((int)rockTypes & type)!=0;
	}
	public boolean hasAlterations( int alterations ) {
		return ((int)alterationFlags & alterations)!=0;
	}
	static void init( int n ) {
		stations = new PDBStation[n];
	}
	static void add( PDBStation station, int index ) {
		if(stations == null) {
			init( index );
		} else if(stations.length <= index ) {
			PDBStation[] tmp = new PDBStation[index+100];
			System.arraycopy( stations, 0, tmp, 0, stations.length);
			stations = tmp;
		}
		stations[index] = station;
	System.out.println(stations.length);
	}
	static int trimToSize() {
		int k;
		for( k=stations.length-1 ; k>=0 ; k-- ) {
			if( stations[k] != null ) break;
		}
		if(k==stations.length-1) return size();
		PDBStation[] tmp = new PDBStation[k+1];
		if(k>=0) {
			System.arraycopy( stations, 0, tmp, 0, k+1 );
		}
		stations = tmp;
		return size();
	}
	static void unload() {
		for (int i = 0; i < stations.length; i++)
			stations[i] = null;
		stations = null;
		loaded = false;
	}

	public static void load() throws IOException {
		if(loaded) return;
		//URL url = URLFactory.url(PETDB_PATH + "June2014/stations_new.txt");
		URL url = URLFactory.url(PETDB_PATH + "petdb_new/stations_new.txt");
		URLConnection urlConn = url.openConnection();
		urlConn.setDoInput(true);
		urlConn.setUseCaches(false);
		BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

		// Read Data File
		int index=0;
		String id, s;
		long[] samples;
		short expedition;
		short location;
		byte sampleTechnique;
		byte materialFlags;
		short itemsMeasured;
		byte alterationFlags;
		short rockTypes;

		while ((s = in.readLine())!= null){
			if (s.startsWith("*/")){
				int n =Integer.parseInt(in.readLine());
				init(n);

				while (true) try{
					s = in.readLine();
					String [] results = s.split("\\t");
					index = Short.parseShort(results[0]);
					id = results[1];
					expedition = Short.parseShort(results[2]);
					location = Short.parseShort(results[3]);
					samples = new long[Short.parseShort(results[4])];
					String [] sampleItems = results[5].split(",");
					for( int i=0 ; i<samples.length ; i++) {
						samples[i] = Long.parseLong(sampleItems[i]);
					}
					sampleTechnique = Byte.parseByte(results[6]);
					materialFlags = Byte.parseByte(results[7]);
					itemsMeasured = Short.parseShort(results[8]);
					alterationFlags = Byte.parseByte(results[9]);
					rockTypes = Short.parseShort(results[10]);
					add( new PDBStation( id, samples, expedition, location,
									sampleTechnique, materialFlags, itemsMeasured,
									alterationFlags, rockTypes ), index);
					if( PDBLocation.locations[location]==null) {
						stations[index]=null;
					}
				} catch (NullPointerException ex) {
					break;
				}
			}
		}

		try {
			in.close();
		} catch ( IOException ex ) {}
		loaded = true;
	}
	public static boolean isLoaded() {
		return loaded;
	}
	public static PDBStation get( int index ) {
		
		if( index<0 || index>=size() ) {
			System.err.println("PDBStation:get(index)="+index+" size="+size() );
			return null;
		}
		return stations[index];
	}
	public static int size () {
		if( stations==null )return 0;
		return stations.length;
	}
}
package haxby.db.pdb;

public class PDBRockType {
	static PDBRockType[] rock = null;
	static String[][] rockCode = {
				{ "IPF", "igneous:plutonic:felsic", "1" },
				{ "IPM", "igneous:plutonic:mafic", "2" },
				{ "IPU", "igneous:plutonic:ultramafic", "4" },
				{ "IVC", "igneous:volcanic:clastic", "8" },
				{ "IVF", "igneous:volcanic:felsic", "16" },
				{ "IVI", "igneous:volcanic:intermediate", "32" },
				{ "IVM", "igneous:volcanic:mafic", "64" },
				{ "MET", "metamorphic", "128" },
				{ "SDU", "sedimentary:unknown", "256" },
				{ "VEI", "vein", "512" }};

	static boolean initiallized = false;
	String abbrev;
	String name;
	String code;

	public PDBRockType(String abbrev, String name, String code) {
		this.abbrev = abbrev;
		this.name = name;
		this.code = code;
	}
	static void load() {
		rock = new PDBRockType[rockCode.length];
		for(int i=0 ; i<rock.length ; i++ ) {
			rock[i] = new PDBRockType(rockCode[i][0], rockCode[i][1], rockCode[i][2]);
		}
		initiallized = true;
	}
	static void unload() {
		rock = null;
		initiallized = false;
	}
	public static int size() {
		return rock.length;
	}
	public static PDBRockType get( int index ) {
		return rock[index];
	}
}
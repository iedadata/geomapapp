package haxby.db.pdb;

public class PDBAnalysis {
	PDBBatch parent;
	short[] code;
	float[] val;
	float[] stdDev;
	boolean[] compiled;
	short dataQuality;
	short dataTypes;
	public PDBAnalysis(PDBBatch batch, 
			int dataQuality, 
			short[] code, 
			float[] val, 
			float[] stdDev,
			boolean[] compiled) {
		parent = batch;
		this.dataQuality = (short)dataQuality;
		this.code = code;
		this.val = val;
		this.stdDev = stdDev;
		this.compiled = compiled;
		int data = 0;
		for(int i=0 ; i<code.length ; i++) {
			data |= PDBDataType.getGroupFlag((int)code[i]);
		}
		dataTypes = (short)data;
	}
	public String getName() {
		return parent.getName(this);
	}
	public int getStationNum() {
		return parent.getStationNum();
	}
	public boolean hasDataType(int types) {
		return ((int)dataTypes & types) != 0;
	}
	public int getDataQuality() {
		return (int)dataQuality;
	}
	public void dispose() {
		parent = null;
	}
}

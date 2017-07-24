package haxby.db.surveyplanner;

import javax.swing.table.DefaultTableModel;

public class SurveyPlannerTableModel extends DefaultTableModel {

	private static final long serialVersionUID = 1L;
	private static final String LINE_NUM_COL = "Line Number";
	private static final String START_LAT_COL = "Start Latitude";
	private static final String START_LON_COL = "Start Longitude";
	private static final String START_DEPTH_COL = "Start Depth";
	private static final String END_LAT_COL = "End Latitude";
	private static final String END_LON_COL = "End Longitude";
	private static final String END_DEPTH_COL = "End Depth";
	private static final String DISTANCE_COL = "Cumulative Distance (km)";
	private static final String DURATION_COL = "Duration (hrs)";
	private static final String SURVEY_LINE_COL  ="surveyLine";
	
	private SurveyPlanner sp;
	
	public SurveyPlannerTableModel(Object[] columnNames, int rowCount, SurveyPlanner sp) {
		super(columnNames, rowCount);
		this.sp = sp;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		double v;
		//check entered value can be parsed as a double
		try {
			v = Double.parseDouble(value.toString());
		} catch (Exception e) {
			return;
		}
		//get the SurveyLine for this row
		SurveyLine line = (SurveyLine) getValueAt(row, getSurveyLineColumn());
		String colName = getColumnName(col);
		switch (colName) 
		{
			case LINE_NUM_COL: line.setLineNum((int) v); break; 
			case START_LAT_COL: line.setStartLat(v); break;
			case START_LON_COL: line.setStartLon(v); break;
			case END_LAT_COL: line.setEndLat(v); break;
			case END_LON_COL: line.setEndLon(v); break;
			default: break;
		}

		super.setValueAt(value, row, col);
		
		//recalculate depths
		try {
			double startDepth = sp.getDepth(line.getStartLat(), line.getStartLon());
			double endDepth = sp.getDepth(line.getEndLat(), line.getEndLon());
			//add depths to survey line
			line.setDepths(startDepth, endDepth);
			if (Double.isNaN(startDepth)) {
				super.setValueAt("-", row, getColumnIndex(START_DEPTH_COL));
			} else {
				super.setValueAt(startDepth, row, getColumnIndex(START_DEPTH_COL));
			}
			if (Double.isNaN(endDepth)) {
				super.setValueAt("-", row, getColumnIndex(END_DEPTH_COL));
			} else {
				super.setValueAt(endDepth, row, getColumnIndex(END_DEPTH_COL));
			}
		} catch (Exception e) {}
		
		//recalculate cumulative distances and durations
		recalculateRows();

		//repaint the map and survey lines		
		sp.repaint();
	}
	
	public void recalculateRows() {
		//recalculate cumulative distances and durations
		SurveyLine.resetTotalDistance();
		for (int i=0; i<sp.getSurveyLines().size(); i++) {
			SurveyLine sl = sp.getSurveyLines().get(i);
			sl.setCumulativeDistance(sl.calculateCumulativeDistance());
			super.setValueAt(sl.getCumulativeDistance(), i, getColumnIndex(DISTANCE_COL));
			sl.setDuration(sl.calculateDuration());
			super.setValueAt(sl.getDuration(), i, getColumnIndex(DURATION_COL));
		}
	}
	
	public int getSurveyLineColumn() {
		return getColumnIndex(SURVEY_LINE_COL);
	}

	/**
	 * return the column index for the given column name
	 * @param colName
	 * @return
	 */
	public int getColumnIndex(String colName) {
		for (int i=0; i< getColumnCount(); i++) {
			if (getColumnName(i).equals(colName)) {
				return i;
			}
		}
		return 999;
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		if (getColumnName(col).equals(START_DEPTH_COL) || 
				getColumnName(col).equals(END_DEPTH_COL) ||
				getColumnName(col).equals(DISTANCE_COL) ||
				getColumnName(col).equals(DURATION_COL)) return false;
		return true;
	}
}

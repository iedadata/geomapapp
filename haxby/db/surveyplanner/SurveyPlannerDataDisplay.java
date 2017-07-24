package haxby.db.surveyplanner;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import haxby.map.XMap;


public class SurveyPlannerDataDisplay implements ActionListener{

	private SurveyPlanner sp;
	private JTable table;
	private JScrollPane tableSP;
	private JPanel panel;
	private SurveyPlannerTableModel tm;
	private ArrayList<SurveyLine> surveyLines;
	private JButton addRowB, deleteRowB, saveB;
	private int slCol = 999;
	private XMap map;
	
	public SurveyPlannerDataDisplay( SurveyPlanner sp) {
		this.sp = sp;
		map = sp.getMap();
		surveyLines = sp.getSurveyLines();
		initDisplay();
	}
	void initDisplay() {
	
		tm = new SurveyPlannerTableModel (new Object[]{ "Line Number",
												 "Start Latitude",
										         "Start Longitude",
										         "Start Depth",
											     "End Latitude",
											     "End Longitude",
											     "End Depth",
											     "Km Cumulative Distance",
											     "Hrs Duration",
											     "surveyLine"}, surveyLines.size(), sp);
		
		table = new JTable(tm);
		
		//use the surveyLine column to keep track of the 
		//surveyLine object associated with that row,
		//but keep the column hidden
		TableColumnModel tcm = table.getColumnModel();
		slCol = tm.getSurveyLineColumn();
		if (slCol != 999) {
			tcm.removeColumn(tcm.getColumn(slCol));
		}
		
		tableSP = new JScrollPane(table);

		panel = new JPanel( new BorderLayout() ); // bottom panel
		panel.add( tableSP, "Center" );
		
		JPanel buttons = new JPanel( new GridLayout(0, 1) );
		addRowB = new JButton("Add Row");
		addRowB.setActionCommand("add");
		addRowB.addActionListener( this );
		buttons.add( addRowB );
		
		deleteRowB = new JButton("Delete Row(s)");
		deleteRowB.setActionCommand("delete");
		deleteRowB.addActionListener( this );
		buttons.add( deleteRowB );

		saveB = new JButton("Save");
		saveB.addActionListener(this);
		buttons.add(saveB);
		
		panel.add( buttons, "East" );

	}
	
	public DefaultTableModel getTableModel() {
		return tm;
	}
	
	public JPanel getPanel() {
		return panel;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("add")) {
			//create a new survey line
			SurveyLine line = new SurveyLine(map);
			sp.addLine(line);
			//highlight the new line
			table.setRowSelectionInterval(tm.getRowCount() -1 , tm.getRowCount() -1);
		}
		if (cmd.equals("delete")) {
			//delete the selected rows and remove survey lines from the list
			int selRow = table.getSelectedRow();
			while (selRow != -1) {
				sp.deleteLine((SurveyLine) tm.getValueAt(selRow, slCol));
				tm.removeRow(selRow);
				selRow = table.getSelectedRow();
			}
			//recalculate cumulative distances and durations
			tm.recalculateRows();
			
			//repaint the map and survey lines		
			sp.repaint();
		}
		if (cmd.equals("Save")) {
			//save the file in csv format
			saveToFile();
		}
	}
	public int getSurveyLineColumn() {
		return slCol;
	}
	

	/*
	 * save the survey lines to a text file
	 */
	private void saveToFile() {

		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		File f=new File("SurveyLines.txt");
		jfc.setSelectedFile(f);
		do {
			int c = jfc.showSaveDialog(null);
			if (c==JFileChooser.CANCEL_OPTION||c==JFileChooser.ERROR_OPTION) return;
			f = jfc.getSelectedFile();
			if (f.exists()) {
				c=JOptionPane.showConfirmDialog(null, "File Already Exists\nConfirm Overwrite");
				if (c==JOptionPane.OK_OPTION) break;
				if (c==JOptionPane.CANCEL_OPTION) return;
			}
		} while (f.exists());

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			
			//add disclaimer at the top of the output file
			out.write("NOT TO BE USED FOR NAVIGATION PURPOSES\n");
			boolean firstCol = true;
			for (int i=0;i<tm.getColumnCount();i++) {
				if (i == sp.getSurveyLineColumn()) continue;
				if (firstCol) {
					out.write(tm.getColumnName(i));
					firstCol = false;
				} else {
					out.write("," + tm.getColumnName(i));
				}
			}
			out.write("\n");
			
			int[] ind;
			ind = new int[tm.getRowCount()];
			for (int i=0; i<tm.getRowCount(); i++) ind[i] = i;
			
			for (int i=0;i<tm.getRowCount();i++) {
				firstCol = true;
				for (int j=0; j<tm.getColumnCount();j++) {
					if (j == sp.getSurveyLineColumn()) continue;
					Object o = tm.getValueAt(i, j);
					if (o instanceof String && ((String)o).equals("NaN")) o = "";
					if (firstCol) {
						out.write(o.toString());
						firstCol = false;
					} else {
						out.write(","+ o);
					}
				}
				out.write("\n");
			}
			out.close();
		} catch (IOException e){
			JOptionPane.showMessageDialog(panel, "Unable to save file", "Save Error", JOptionPane.ERROR_MESSAGE);
			System.out.println(e);
		}
	}
}

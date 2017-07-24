package haxby.db.pdb;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.TableModel;

import haxby.util.BrowseURL;
import haxby.util.PathUtil;
import haxby.util.XBTable;

public class SendToPetDB extends JButton {

	static String PETDB_SQL_ROOT_PATH = PathUtil.getPath("PORTALS/PETDB_SQL_ROOT_PATH",
	"http://www.earthchem.org/petdbWeb/search/");

	JTabbedPane pane;
	static String  buttonText = "<html><body><center>"
			+"View one selection in detail<br>"
			+"on the <B>PetDB</B> web page"
			+"</center></body></ntml>";

	public SendToPetDB(JTabbedPane pane) {
		super( buttonText );
		this.pane = pane;
		addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				showURL();
			}
		});
	}
	void showURL() {
		XBTable table = (XBTable) ((JScrollPane) pane.getSelectedComponent()).getViewport().getView();
	//if (pane.getSelectedIndex() == 2)
		//	table = (XBTable) pane.getComponentAt(1);

		TableModel model = table.getModel();

		int[] selectedIndices = table.getSelectedRows();
		if(selectedIndices.length == 0) {
			JOptionPane.showMessageDialog(null,
				"<html>Please select a row from the<br>table or " +
				"an item on the map to continue.</html>",
				"Warning",
				JOptionPane.WARNING_MESSAGE);
			return;
		} else if (selectedIndices.length > 1) {
			JOptionPane.showMessageDialog(null,
					"<html>Too many items are selected!<br>" +
					"Please select one row from the<br>" +
					"table or one item on the map to<br>" +
					"continue.</html>",
					"Warning",
					JOptionPane.WARNING_MESSAGE);
				return;
		}

		String id = table.getRowHeader().getModel().getElementAt(selectedIndices[0]).toString();
		String url;

//		***** GMA 1.6.2: Bring up the new, correct PetDB URL for the selected item
		if (model instanceof PDBStationModel) {
			url = "http://www.earthchem.org/petdbWeb/search/"
				+"statn_info.jsp?"
				+"&station_id="+id
				+"&referrer=MAP_APP";
		} else {
			if(model instanceof PDBAnalysisModel){
				// No Analysis url id yet for search url. Just use Compile url which is id2[0] 
				String id2[] = id.split(":");
				id = id2[0];
			}
			//  url = "http://129.236.40.161:7001/loadpetdb/search/"+
			url = "http://www.earthchem.org/petdbWeb/search/"
					+ "view_samples2.jsp?"
					+ "srch_value=" + id
					+ "&type=srch_id";
		} 
		BrowseURL.browseURL(url);
	//	selectedIndices[0] = model.current[selectedIndices[0]];
	}
}
package haxby.db.pdb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PDBSelectionDialog extends JPanel
			implements ActionListener {
	PDB pdb;
	BasicDialog basic;
	JTabbedPane tb;
	JFrame frame;
	public PDBSelectionDialog( PDB pdb ) {
		super( new BorderLayout() );
		this.pdb = pdb;
		basic = new BasicDialog(pdb);
		tb = new JTabbedPane(JTabbedPane.TOP);
		tb.add( "Make Your Selections Below", basic);
		add( tb, "Center");
	//	frame = new JFrame("Selection Dialog");
	//	frame.getContentPane().add( this, "Center");
	//	JButton button = new JButton("Close");
	//	frame.getContentPane().add( button, "South");
	//	button.addActionListener(this);
		setPreferredSize(new Dimension(450,500) );
	}
	public JTabbedPane getPane() {
		return tb;
	}
	public void showDialog(int x, int y) {
	//	frame.pack();
	//	if(x>=0 && y>=0) frame.setLocation(x, y);
	//	frame.show();
	}
	public void actionPerformed( ActionEvent evt ) {
		frame.dispose();
	}
}

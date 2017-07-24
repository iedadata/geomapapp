package haxby.db.pdb;

import haxby.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BasicDialog extends JPanel
			implements ActionListener {
	PDB pdb;
	JToggleButton[] materials;
	JToggleButton[] dataTypes;
	JToggleButton[] rockTypes;
	JToggleButton[] alterations;
	SimpleBorder on, off;
	public BasicDialog(PDB pdb) {
		super(new GridLayout(1,0, 5, 5));
		this.pdb = pdb;
		init();
	}
	void init() {
		PDBMaterial.load();
		PDBDataType dt = null;
		try {
			dt = new PDBDataType();
		} catch (Exception ex) {
		}
		PDBRockType.load();
	//	PDBAlteration.load();
		on = new SimpleBorder();
		off = new SimpleBorder();
		javax.swing.border.Border bb = BorderFactory.createCompoundBorder(
			BorderFactory.createEtchedBorder(),
			BorderFactory.createEmptyBorder(1,1,1,1));
		javax.swing.border.Border lb = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(Color.black, 1),
			BorderFactory.createEmptyBorder(1,1,1,1));
		javax.swing.border.Border lb1 = BorderFactory.createEmptyBorder(1,1,1,1);
		on.setSelected(true);
		int max = PDBMaterial.size();
		int n = dt.dataCode.length;
		if(  PDBMaterial.size() > max ) max = PDBMaterial.size();
		if(  n > max ) max = n;

		// Material section
		materials = new JToggleButton[PDBMaterial.size()];
		JPanel panel = new JPanel(new GridLayout(0, 1));
		JLabel label = new JLabel("<html><b>Material</b></html>");
		panel.setBorder( lb );
		label.setForeground( Color.black );
		label.setHorizontalAlignment( label.CENTER );
		panel.add( label );

		// Add check boxes
		for(int i=0 ; i<materials.length ; i++) {
			materials[i] = new JCheckBox(PDBMaterial.material[i].abbrev + ": " + PDBMaterial.material[i].name, true);
			panel.add(materials[i]);
			materials[i].addActionListener(this);
			materials[i].setActionCommand("material");
			materials[i].setSelected( true );
			materials[i].setBorder(on);
		}
		for(int i=materials.length ; i<max ; i++) {
			label = new JLabel("");
			label.setBorder(lb1);
			panel.add(label);
		}

		// Add buttons
		JRadioButton buttonS = new JRadioButton("Select All");
		//button.setBorder( bb );
		buttonS.setActionCommand("material");
		buttonS.setSelected(true);

		JRadioButton buttonC = new JRadioButton("Clear All");
		//button.setBorder( bb );
		buttonC.setActionCommand("material");

		ButtonGroup group1 = new ButtonGroup();
		group1.add(buttonS);
		group1.add(buttonC);

		buttonS.addActionListener(this);
		buttonC.addActionListener(this);

		panel.add(buttonS);
		panel.add(buttonC);
		add( panel );

		// Data Type section
		dataTypes = new JToggleButton[dt.dataCode.length];
		panel = new JPanel(new GridLayout(0, 1));
		label = new JLabel("<html><b>Data Type</b></html>");
		panel.setBorder( lb );
		label.setForeground( Color.black );
		label.setHorizontalAlignment( label.CENTER );
		panel.add( label );

		// Add checkboxes
		for(int i=0 ; i<dataTypes.length ; i++) {
			dataTypes[i] = new JCheckBox(PDBDataType.dataCode[i][0] + ": " +PDBDataType.dataCode[i][1], true);
			panel.add(dataTypes[i]);
			dataTypes[i].addActionListener(this);
			dataTypes[i].setActionCommand("dataType");
			dataTypes[i].setSelected( true );
			dataTypes[i].setBorder(on);
		}

		// Add buttons
		JRadioButton buttonS2 = new JRadioButton("Select All");
		buttonS2.setActionCommand("dataType");
		buttonS2.setSelected(true);

		JRadioButton buttonC2 = new JRadioButton("Clear All");
		buttonC2.setActionCommand("dataType");

		ButtonGroup group2 = new ButtonGroup();
		group2.add(buttonS2);
		group2.add(buttonC2);

		buttonS2.addActionListener(this);
		buttonC2.addActionListener(this);

		panel.add(buttonS2);
		panel.add(buttonC2);

		add( panel );

		// Rocktype section
		rockTypes = new JToggleButton[PDBRockType.size()];
		panel = new JPanel(new GridLayout(0, 1));
		label = new JLabel("<html><b>Rock Type</b></html>");
		panel.setBorder( lb );
		label.setForeground( Color.black );
		label.setHorizontalAlignment( label.CENTER );
		panel.add( label );

		// Add checkboxes
		for(int i=0 ; i<rockTypes.length ; i++) {
			String rockDisplay = (PDBRockType.rock[i].abbrev + ": " + PDBRockType.rock[i].name);
			rockTypes[i] = new JCheckBox(PDBRockType.rock[i].abbrev + ": " + PDBRockType.rock[i].name, true);
			panel.add(rockTypes[i]);
			rockTypes[i].addActionListener(this);
			rockTypes[i].setActionCommand("rockType");
			rockTypes[i].setSelected( true );
			rockTypes[i].setBorder(on);
		}

		// Add buttons
		JRadioButton buttonS3 = new JRadioButton("Select All");
		buttonS3.setActionCommand("rockType");
		buttonS3.setSelected(true);

		JRadioButton buttonC3 = new JRadioButton("Clear All");
		buttonC3.setActionCommand("rockType");

		ButtonGroup group3 = new ButtonGroup();
		group3.add(buttonS3);
		group3.add(buttonC3);

		buttonS3.addActionListener(this);
		buttonC3.addActionListener(this);

		panel.add(buttonS3);
		panel.add(buttonC3);
		add( panel );

/*
		alterations = new JToggleButton[PDBAlteration.size()];
		panel = new JPanel(new GridLayout(0, 1));
		label = new JLabel("Alteration");
		label.setBorder( lb );
		label.setForeground( Color.black );
		label.setHorizontalAlignment( label.CENTER );
		panel.add( label );
		button = new JButton("All");
		button.setActionCommand("alteration");
		button.addActionListener(this);
		button.setBorder( bb );
		panel.add(button);
		button = new JButton("Clear");
		button.setBorder( bb );
		button.setActionCommand("alteration");
		button.addActionListener(this);
		panel.add(button);

		for(int i=0 ; i<alterations.length ; i++) {
			alterations[i] = new JToggleButton(PDBAlteration.alteration[i].name
					+"("+PDBAlteration.alteration[i].abbrev+")", true);
			panel.add(alterations[i]);
			alterations[i].addActionListener(this);
			alterations[i].setActionCommand("alteration");
			alterations[i].setSelected( true );
			alterations[i].setBorder(on);
		}
		label = new JLabel("");
		label.setBorder(lb1);
		for(int i=alterations.length ; i<max ; i++) {
			label = new JLabel("");
			label.setBorder(lb1);
			panel.add(label);
		}
		add( panel );
*/
		Font font = new Font("SansSerif", Font.PLAIN, 10);
		setFonts(this, font);
	}
	public void setFonts( Container c, Font font) {
		Component[] comp = c.getComponents();
		for(int i=0 ; i<comp.length ; i++) {
			comp[i].setFont(font);
			if(comp[i] instanceof Container) setFonts((Container)comp[i], font);
		}
	}

	public int getMaterialFlags() {
		int flags = 0;
		for( int i=0 ; i<materials.length ; i++) {
			if( materials[i].isSelected() )  flags |= (1<<i);
		}
		return flags;
	}
	public int getDataFlags() {
		int flags = 0;
		for( int i=0 ; i<dataTypes.length ; i++) {
			if( dataTypes[i].isSelected() )  flags |= (1<<i);
		}
		return flags;
	}
	public int getRockFlags() {
		int flags = 0;
		for( int i=0 ; i<rockTypes.length ; i++) {
			if( rockTypes[i].isSelected() )  flags |= (1<<i);
		}
		return flags;
	}
	public int getAterations() {
		int flags = 0;
		for( int i=0 ; i<alterations.length ; i++) {
			if( alterations[i].isSelected() )  flags |= (1<<i);
		}
		return flags;
	}
	public void actionPerformed( ActionEvent evt ) {
		if(evt.getActionCommand().equals("material")) {
			String text = ((AbstractButton)evt.getSource()).getText();
			if(text.equals("Select All")) {
				for(int i=0 ; i<materials.length ; i++) {
					materials[i].setSelected( true );
					materials[i].setBorder( on );
				}
			} else if(text.equals("Clear All")) {
				for(int i=0 ; i<materials.length ; i++) {
					materials[i].setSelected( false );
					materials[i].setBorder( off );
				}
			} else {
				for(int i=0 ; i<materials.length ; i++) {
					if( materials[i].isSelected() ) materials[i].setBorder( on );
					else materials[i].setBorder( off );
				}
			}
			pdb.getModel().setMaterialFlags( getMaterialFlags() );
		} else if(evt.getActionCommand().equals("dataType")) {
			String text = ((AbstractButton)evt.getSource()).getText();
			if(text.equals("Select All")) {
				for(int i=0 ; i<dataTypes.length ; i++) {
					dataTypes[i].setSelected( true );
					dataTypes[i].setBorder( on );
				}
			} else if(text.equals("Clear All")) {
				for(int i=0 ; i<dataTypes.length ; i++) {
					dataTypes[i].setSelected( false );
					dataTypes[i].setBorder( off );
				}
			} else {
				for(int i=0 ; i<dataTypes.length ; i++) {
					if(dataTypes[i].isSelected())dataTypes[i].setBorder( on );
					else dataTypes[i].setBorder( off );
				}
			}
			pdb.getModel().setDataFlags( getDataFlags() );
		} else if(evt.getActionCommand().equals("rockType")) {
			String text = ((AbstractButton)evt.getSource()).getText();
			if(text.equals("Select All")) {
				for(int i=0 ; i<rockTypes.length ; i++) {
					rockTypes[i].setSelected( true );
					rockTypes[i].setBorder( on );
				}
			} else if(text.equals("Clear All")) {
				for(int i=0 ; i<rockTypes.length ; i++) {
					rockTypes[i].setSelected( false );
					rockTypes[i].setBorder( off );
				}
			} else {
				for(int i=0 ; i<rockTypes.length ; i++) {
					if(rockTypes[i].isSelected())rockTypes[i].setBorder( on );
					else rockTypes[i].setBorder( off );
				}
			}
			pdb.getModel().setRockFlags( getRockFlags() );
		} else if(evt.getActionCommand().equals("alteration")) {
			String text = ((AbstractButton)evt.getSource()).getText();
			if(text.equals("All")) {
				for(int i=0 ; i<alterations.length ; i++) {
					alterations[i].setSelected( true );
					alterations[i].setBorder( on );
				}
			} else if(text.equals("Clear")) {
				for(int i=0 ; i<alterations.length ; i++) {
					alterations[i].setSelected( false );
					alterations[i].setBorder( off );
				}
			} else {
				for(int i=0 ; i<alterations.length ; i++) {
					if(alterations[i].isSelected())alterations[i].setBorder( on );
					else alterations[i].setBorder( off );
				}
			}
			pdb.getModel().setRockFlags( getRockFlags() );
		}
	}
}
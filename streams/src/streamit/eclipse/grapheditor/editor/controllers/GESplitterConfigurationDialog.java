/*
 * Created on Jan 28, 2004
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.graph.Constants;
import streamit.eclipse.grapheditor.graph.utils.StringTranslator;

/**
 * Dialog used to view and set the properties of a GESplitter.
 * Valid values must be entered for all the properties.
 * 
 * @author jcarlos
 */
public class GESplitterConfigurationDialog extends GEStreamNodeConfigurationDialog{


	private String dialogType = "Splitter Configuration";
	
	private JLabel splitterWeightsLabel = new JLabel();
	private JTextField splitterWeightsTextField;
	private JComboBox nameJComboBox;
	
	
	/**
	 * Constructor for the GESplitterConfigurationDialog.
	 * @param parent Frame The parent of the configuration dialog.
	 * @param document GPDocument
	 */
	public GESplitterConfigurationDialog(Frame parent, GPDocument document) 
	{    
		super(parent, document);
		initComponents();
		setTitle(dialogType);
		setName(dialogType);		
		setPosition();
	}

	/**
	 * Set the value of the "Splitter Weights" text field.
	 * @param weights Text value for "Splitter Weights"
	 */
	public void setSplitterWeights(String weights)
	{
		splitterWeightsTextField.setText(weights);	
	}
	
	/**
	 * Get the value of the "Splitter Weights" text field.
	 * @return String value of the "Splitter Weights" text field. 
	 */
	public String getSplitterWeights()
	{
		return splitterWeightsTextField.getText().trim();
	}
	
	
	/**
	 * Checks that the properties that were entered by the user in the configuration
	 * dialog are valid.
	 */ 
	protected boolean action_ok() 
	{
		try
		{		
			StringTranslator.weightsToInt(this.getSplitterWeights());
		}
		catch(Exception e)
		{
			String message = "Please enter a legal weight";
			JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
			return false;	
		}
		
		if ( ! (super.action_ok()))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the value of the "Name" field as text.
	 */
	public String getName() {
		return nameJComboBox.getSelectedItem().toString();
	}

	/**
	 * Set the value of the "Name" text field.
	 */
	public void setName(String text) {
		nameJComboBox.setSelectedItem(text);
	}
		
	/**
	 * Initialize the graphical components of the configuration dialog.
	 * The initial values in the dialog will be the current values for the 
	 * properties of the GEStreamNode or the default values if the GEStreamNode 
	 * was just created. 
	 */ 
	protected void initComponents() 
	{
		super.initComponents();
		jPanel1 = new JPanel(new GridLayout(5,5));
		
		nameLabel = new JLabel();
		parentLabel = new JLabel();
		splitterWeightsLabel = new JLabel();
		nameTextField = new JTextField();
		nameJComboBox = new JComboBox(Constants.SPLITTER_TYPE_NAMES);
		
		splitterWeightsTextField = new JTextField();
		parentsJComboBox = new JComboBox(this.document.getGraphStructure().containerNodes.getAllContainerNamesWithID());

		nameLabel.setText("Name");
		nameLabel.setName("Name");
		jPanel1.add(nameLabel);
		jPanel1.add(nameJComboBox);

		parentLabel.setText("Parent");
		parentLabel.setName("Parent");
		jPanel1.add(parentLabel);
		jPanel1.add(parentsJComboBox);
				
		splitterWeightsLabel.setText("Splitter Weights");
		splitterWeightsLabel.setName("Splitter Weights");
		jPanel1.add(splitterWeightsLabel);
		jPanel1.add(splitterWeightsTextField);
		
		JLabel weightParseReqLabel= new JLabel();
		weightParseReqLabel.setName("Weights Parse Req");
		weightParseReqLabel.setText("(separate weights with commas)");
		jPanel1.add(weightParseReqLabel);
		
		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
		pack();
	}
}
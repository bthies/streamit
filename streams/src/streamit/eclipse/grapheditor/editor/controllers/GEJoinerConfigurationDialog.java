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
 * Dialog used to view and set the properties of a GEJoiner.
 * Valid values must be entered for all the properties.
 * 
 * @author jcarlos
 */
public class GEJoinerConfigurationDialog  extends GEStreamNodeConfigurationDialog{


	private String dialogType = "Joiner Configuration";

	private JLabel joinerWeightsLabel = new JLabel();
	private JTextField joinerWeightsTextField;
	private JComboBox nameJComboBox;
 
	
	/**
	 * Constructor for the GEJoinerConfigurationDialog.
	 * @param parent Frame The parent of the configuration dialog.
	 * @param document GPDocument
	*/
	public GEJoinerConfigurationDialog(Frame parent, GPDocument document) 
	{    
		super(parent, document);
		initComponents();
		setTitle(dialogType);
		setName(dialogType);		
		setPosition();
	}
 
	/**
	 * Set the value of the "Joiner Weights" text field.
	 * @param weights Text value for "Joiner Weights"
	 */
	public void setJoinerWeights(String weights)
	{
		joinerWeightsTextField.setText(weights);	
	}
	
	/**
	 * Get the value of the "Joiner Weights" text field.
	 * @return String value of the "Joiner Weights" text field. 
	 */
	public String getJoinerWeights()
	{
		return joinerWeightsTextField.getText().trim();
	}
	
	
	/**
	 * Checks that the properties that were entered by the user in the configuration
	 * dialog are valid.
	 */ 
	protected boolean action_ok() 
	{
		try
		{		
			StringTranslator.weightsToInt(this.getJoinerWeights());
			
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
		joinerWeightsLabel = new JLabel();

		nameJComboBox = new JComboBox(Constants.JOINER_TYPE_NAMES);
		joinerWeightsTextField = new JTextField();		
		parentsJComboBox = new JComboBox(this.document.getGraphStructure().containerNodes.getAllContainerNamesWithID());

		nameLabel.setText("Name");
		nameLabel.setName("Name");
		jPanel1.add(nameLabel);
		jPanel1.add(nameJComboBox);

		parentLabel.setText("Parent");
		parentLabel.setName("Parent");
		jPanel1.add(parentLabel);
		jPanel1.add(parentsJComboBox);
		
		joinerWeightsLabel.setText("Joiner Weights");
		joinerWeightsLabel.setName("JoinerWeights");
		jPanel1.add(joinerWeightsLabel);
		jPanel1.add(joinerWeightsTextField);
		
		JLabel weightParseReqLabel= new JLabel();
		weightParseReqLabel.setName("Weights Parse Req");
		weightParseReqLabel.setText("(separate weights with commas)");
		jPanel1.add(weightParseReqLabel);
		
		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

		pack();
	}


}
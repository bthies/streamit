/*
 * Created on Dec 8, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.graph.Constants;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEStreamNode;

/**
 * Dialog used to view and set the properties of a GEPhasedFilter.
 * Valid values must be entered for all the properties.
 * 
 * @author jcarlos
 */
public class GEFilterConfigurationDialog extends GEStreamNodeConfigurationDialog
{
	private String dialogType = "Filter Configuration";
	
	protected JLabel pushLabel = new JLabel();
	protected JLabel popLabel = new JLabel();
	protected JLabel peekLabel = new JLabel();
	
	protected JTextField pushTextField;
	protected JTextField popTextField;
	protected JTextField peekTextField;
	
	private String savedPopRate;
	private String savedPeekRate;
	private String savedPushRate;
	
	/**
	 * Constructor for the GEFilterConfigurationDialog.
	 * @param parent Frame The parent of the configuration dialog.
	 * @param document GPDocument
	*/
	public GEFilterConfigurationDialog(Frame parent, GPDocument document)
	{
		super(parent, document);
		initComponents();
        this.savedPopRate = null;
        this.savedPeekRate = null;
        this.savedPushRate = null;
		
		setTitle(dialogType);
		setName(dialogType);
		setPosition();
	}
	
	/**
	 * Checks that the properties that were entered by the user in the configuration
	 * dialog are valid.
	 */ 
	protected boolean action_ok() 
	{
		
		/** Check to see if  no other node has the same name as the name that this one is going to get.*/
		if ( ! (checkSameNameOK()))
		{
			return false;
		}

		/** If the properties of the GEPipeline are changed and there is another GEPipeline with
		 * 	the same name, then we have to change the name of this pipeline. This is so since it 
		 * 	is no longer an equivalent instance of the 
		 */		
		if ((savedOutputTape != null) && (savedInputTape != null) && (savedParent != null) && 
			(savedPopRate != null) && (savedPeekRate != null) && (savedPushRate != null))
		{
			if ((savedOutputTape != this.getOutputTape()) || (savedInputTape != this.getInputTape()) ||
				(savedParent != this.getImmediateParent()) ||  ( !(savedPopRate.equals(this.getPopRate()))) ||
				( ! (savedPeekRate.equals(this.getPeekRate()))) || ( ! (savedPushRate.equals(this.getPushRate()))))
				{
					ArrayList list = GEStreamNode.getNodeNamesWithID(this.document.getGraphStructure().allNodesInGraph());
					if (list.indexOf(this.getName()) != list.lastIndexOf(this.getName()))
					{
						this.setName(this.getName() + GEProperties.id_count++);
					}
				}				
		}
		
		try {
			Integer.parseInt(pushTextField.getText());
			Integer.parseInt(popTextField.getText());
			Integer.parseInt(peekTextField.getText());
			
		} catch (Exception e) {
			String message = "The Pop, peek and push rates must be integer values";
			JOptionPane.showMessageDialog(this, message, Translator.getString("Error"), JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
	
		if ( ! (super.action_ok()))
		{
			return false;
		}
		return true;
		
		
	}
	
	/**
	 * Set the value of the "Push Rate" text field.
	 * @param push Text value for "Push Rate"
	 */
	public void setPushRate(String push)
	{
		pushTextField.setText(push);	
	}
	
	/**
	 * Set the value of the "Pop Rate" text field.
	 * @param push Text value for "Pop Rate"
	 */
	public void setPopRate(String pop)
	{
		popTextField.setText(pop);	
	}
	
	/**
	 * Set the value of the "Peek Rate" text field.
	 * @param push Text value for "Peek Rate"
	 */
	public void setPeekRate(String peek)
	{
		peekTextField.setText(peek);	
	}
	
	/**
	 * Get the value of the "Push Rate" text field.
	 * @return String with contents of "Push Rate" text field.
	 */
	public String getPushRate()
	{
		return pushTextField.getText().trim();
	}
	
	/**
	 * Get the value of the "Peek Rate" text field.
	 * @return String with contents of "Peek Rate" text field.
	 */	
	public String getPeekRate()
	{
		return peekTextField.getText().trim();
	}
	
	/**
	 * Get the value of the "Pop Rate" text field.
	 * @return String with contents of "Pop Rate" text field.
	 */
	public String getPopRate()
	{
		return popTextField.getText().trim();
	}
		
	/**
	 * Get the saved pop rate (before any changes were made)
	 * @param pop String
	 */
	public void saveInitialPopRate(String pop)
	{
		this.savedPopRate = pop;
	}
	
	/**
	 * Get the saved peek rate (before any changes were made)
	 * @param peek String
	 */
	public void saveInitialPeekRate(String peek)
	{
		this.savedPeekRate = peek;
	}
	
	/**
	 * Get the saved pop rate (before any changes were made)
	 * @param push String
	 */	
	public void saveInitialPushRate(String push)
	{
		this.savedPushRate = push;
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
		jPanel1 = new JPanel(new GridLayout(7,7));
	
		nameLabel = new JLabel();
		parentLabel = new JLabel();
		inputTapeLabel = new JLabel();
		outputTapeLabel = new JLabel();
		pushLabel = new JLabel();
		popLabel = new JLabel();
		peekLabel = new JLabel();
	
		nameTextField = new JTextField();
		inputTapeJComboBox = new JComboBox(Constants.TAPE_VALUES);
		outputTapeJComboBox = new JComboBox(Constants.TAPE_VALUES);
		pushTextField = new JTextField();
		popTextField = new JTextField();
		peekTextField = new JTextField();
		
		parentsJComboBox = new JComboBox(this.document.getGraphStructure().containerNodes.getAllContainerNamesWithID());

		nameLabel.setText("Name");
		nameLabel.setName("Name");
		jPanel1.add(nameLabel);
		jPanel1.add(nameTextField);

		parentLabel.setText("Parent");
		parentLabel.setName("Parent");
		jPanel1.add(parentLabel);
		jPanel1.add(parentsJComboBox);
		
		inputTapeLabel.setText("Input Tape");
		inputTapeLabel.setName("Input Tape");
		jPanel1.add(inputTapeLabel);
		jPanel1.add(inputTapeJComboBox);
		
		outputTapeLabel.setText("Output Tape");
		outputTapeLabel.setName("Output Tape");
		jPanel1.add(outputTapeLabel);
		jPanel1.add(outputTapeJComboBox);
		
		pushLabel.setText("Push Rate");
		pushLabel.setName("Push Rate");
		jPanel1.add(pushLabel);
		jPanel1.add(pushTextField);
		
		popLabel.setText("Pop Rate");
		popLabel.setName("Pop Rate");
		jPanel1.add(popLabel);
		jPanel1.add(popTextField);

		peekLabel.setText("Peek Rate");
		peekLabel.setName("Peek Rate");
		jPanel1.add(peekLabel);
		jPanel1.add(peekTextField);

		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

		pack();
		
	}
}




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
import javax.swing.JPanel;
import javax.swing.JTextField;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.graph.Constants;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEStreamNode;

/**
 * Dialog used to view and set the properties of a GESplitJoin.
 * Valid values must be entered for all the properties.
 * 
 * @author jcarlos
 */
public class GESplitJoinConfigurationDialog extends GEStreamNodeConfigurationDialog
{
	private String dialogType = "SplitJoin Configuration";


	/**
	 * Constructor for the GESplitJoinConfigurationDialog.
	 * @param parent Frame The parent of the configuration dialog.
	 * @param document GPDocument
	*/
	public GESplitJoinConfigurationDialog(Frame parent, GPDocument document)
	{
		super(parent, document);
		initComponents();
        
		
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

		/** If some of the properties specific to a container are not satisfied, then return false.**/
		if ( ! (checkContainersOK()))
		{
			return false;
		}
		
		/** If the properties of the GESplitjoin are changed and there is another GESplitjoin with
		 * 	the same name, then we have to change the name of this pipeline. This is so since it 
		 * 	is no longer an equivalent instance of the 
		 */		
		if ((savedOutputTape != null) && (savedInputTape != null) && (savedParent != null))
		{
			if ((savedOutputTape != this.getOutputTape()) || (savedInputTape != this.getInputTape()) ||
				(savedParent != this.getImmediateParent()))
				{
					ArrayList list = GEStreamNode.getNodeNamesWithID(this.document.getGraphStructure().allNodesInGraph());
					if (list.indexOf(this.getName()) != list.lastIndexOf(this.getName()))
					{
						this.setName(this.getName() + GEProperties.id_count++);
					}
				}				
		}
		
		
		/** Check the properties that are common to all nodes */
		if ( ! (super.action_ok()))
		{
			return false;
		}
		return true;
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
		inputTapeLabel = new JLabel();
		outputTapeLabel = new JLabel();
	
		nameTextField = new JTextField();
		inputTapeJComboBox = new JComboBox(Constants.TAPE_VALUES);
		outputTapeJComboBox = new JComboBox(Constants.TAPE_VALUES);
		
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

		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

		pack();
		
	}
}




/*
 * Created on Dec 8, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.graph.Constants;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;

/**
 * Dialog used to view and set the properties of a GEStreamNode.
 * Valid values must be entered for all the properties.
 * 
 * @author jcarlos
 */
public abstract class GEStreamNodeConfigurationDialog extends JDialog{


	 /** Boolean for the cancel operation variables*/
	protected boolean canceled;

	protected JPanel jPanel1;
	
	protected JPanel toolBar;
	protected JButton cancelButton;
	protected JButton finishedButton;

	protected JLabel nameLabel = new JLabel();
	protected JLabel parentLabel = new JLabel();
	protected JLabel inputTapeLabel = new JLabel();
	protected JLabel outputTapeLabel = new JLabel();
	
	protected JTextField nameTextField;
		
	protected JComboBox inputTapeJComboBox;
	protected JComboBox outputTapeJComboBox;	
	protected JComboBox parentsJComboBox;
	
	protected GPDocument document;

	protected String savedName;
	protected int savedLevel;	
	protected String savedOutputTape;
	protected String savedInputTape;
	protected String savedParent;
	protected String isConnected = Constants.DISCONNECTED;
	protected int sjIndex;
	
	/**
	 * Constructor for the GEStreamNodeConfigurationDialog.
	 * @param parent Frame The parent of the configuration dialog.
	 * @param document GPDocument
	 */
	public GEStreamNodeConfigurationDialog(Frame parent, GPDocument document) {
        
		super(parent, true);
		this.document = document;
		
		this.savedLevel = -1;
		this.sjIndex = -1;
		this.savedName = null;
		this.savedOutputTape = null;
		this.savedInputTape = null;
		this.savedParent = null;
	}
   
   /**
    * Set the position of the dialog on the screen.
    */
   protected void setPosition()
   {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screen.width - this.getWidth()) / 2;
		int y = (screen.height - this.getHeight()) / 2;
		setLocation(x, y);
   }
   
   /** 
    * Save the name of the GEStreamNode to be modified (before any changes are made to it)
    * @param String name to be saved.
    */
	protected void saveInitialName(String name)
   	{
		savedName = name;
	}
	
	/**
	 * Save the name of the encapsulating node of the GEStreamNode to be modified 
	 * (before any changes are made to it)
	 * @param parent String 
	 */
	protected void saveInitialParent(String parent)
	{ 
		savedParent = parent;	
	}
	
	/**
	 * Save the connected status of the GEStreamNode.
	 * @param isConn String
	 */
	protected void saveConnected(String isConn)
	{
		this.isConnected = isConn;
	}
	
	/**
	 * Save the name of the output tape of the GEStreamNode to be modified 
	 * (before any changes are made to it) 
	 * @param outputTape String 
	 */
	protected void saveInitialOutputTape(String outputTape)
	{
		
		savedOutputTape = outputTape;
	}
	
	/**
	 * Save the name of the input tape of the GEStreamNode to be modified 
	 * (before any changes are made to it)
	 * @param inputTape String 
	 */
	protected void saveInitialInputTape(String inputTape)
	{
		savedInputTape = inputTape;
	}

	/**
	 * Save the initial level at which the GEStreamNode was before its properties
	 * were modified
	 * @param lev String representation of the initial level of the GEStreamNode
	 */
	protected void saveInitialLevel(String lev)
	{
		if (lev != null)
		{
			savedLevel = Integer.valueOf(lev).intValue();
		}
		else
		{
			savedLevel = -1;
		}
	}

	/** 
	 * Called by pressing the cancel button.
	 */
	protected void action_cancel() {
		setVisible(false);
		dispose();
		canceled = true;
	}
    
	/**
	 * Checks that the properties that were entered by the user in the configuration
	 * dialog are valid.
	 */ 
	protected boolean action_ok() 
	{	
		if (! (checkChangeParent()))
		{
			return false;
		}

		
		
		/** Check the properties that are common to all nodes */
		if ( ! (checkIndexOK()))
		{
			return false;
		}
		else
		{
			return true;
		}
		
	
	}
    
	/**
	 * Returns true if the dialog has been canceled.
	 */
	public boolean canceled() {
		return canceled;
	}
    
	/**
	 * Returns the value of the "Name" field as text.
	 */
	public String getName() {
		return nameTextField.getText().trim();
	}
    
	/**
	 * Returns the value of the "Parent" field as text.
	 */
	public String getImmediateParent() {
		return parentsJComboBox.getSelectedItem().toString();
	}

	/**
	 * Returns the value of the "Input Tape" field as text.
	 */
	public String getInputTape() {
		return inputTapeJComboBox.getSelectedItem().toString();
	}
    
	/**
	 * Returns the value of the "Output Tape" field as text.
	 */
	public String getOutputTape() {
		return outputTapeJComboBox.getSelectedItem().toString();
	}
   
	/**
	 * Set the value of the "Name" text field.
	 */
	public void setName(String text) {
		nameTextField.setText(text);
	}
    
	/**
	 * Set the value of the "Parent" text field.
	 */
	public void setImmediateParent(String text) 
	{	
		if (text == "Toplevel")
		{
			text = document.getGraphStructure().getTopLevel().getName();
		}
		
		this.parentsJComboBox.setSelectedItem(text);
	}
	
	/**
	 * Set the value of the "Input Tape" text field.
	 */
	public void setInputTape(String text) {
		this.inputTapeJComboBox.setSelectedItem(text);
	}
	
	/**
	 * Set the value of the "Output Tape" text field.
	 */
	public void setOutputTape(String text) {
		this.outputTapeJComboBox.setSelectedItem(text);
	}


	public void setCurrentDocument(GPDocument document) {
		this.document = document;
	}
	
	/** 
	 * Set the index within a SplitJoin at which the node will be located
	 * @param index int
	 */
	public void setIndexInSJ(int index)
	{
		this.sjIndex = index;
	}
	
	/**
	 * Get the index within aSplitJoin that the node is located.
	 * @return String Index within SplitJoin
	 */
	public String getIndexInSJ()
	{
		return Integer.toString(this.sjIndex);
	}
	
	/**
	 * Check if everything is legal with setting certain of the properties of a container.
	 * @return boolean True if the selected properties are legal; ohterwise, false.
	 */
	protected boolean checkContainersOK()
	{
		/** The node cannot have itself as its parent */
		if (savedName == parentsJComboBox.getSelectedItem().toString())
		{
			String message = "Illegal parent was selected";
			JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
			return false;					
		}	
		
		/** Check if we selected a legal parent */
		/*
		GEContainer container = this.document.getGraphStructure().containerNodes.getContainerNodeFromName(parentsJComboBox.getSelectedItem().toString());	
		if (savedLevel != -1)
		{
			if (savedLevel < container.getDepthLevel())
			{
				String message = "Illegal parent was selected (hierarchy)";
				JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
				return false;									
			}
		}*/
		return true;
	}
	
	/**
	 * Check if no other node has the same name as the name that this one is going to get.
	 * @return True if there is no other node with the same name; otherwise, false.
	 */
	protected boolean checkSameNameOK()
	{	
		/** Cannot have the same name as a node already present in the graph */
		if ( ! (savedName.equals(this.getName())))
		{
			if (GEStreamNode.getNodeNames(this.document.getGraphStructure().allNodesInGraph()).contains(this.getName()))
			{
				JOptionPane.showMessageDialog(this, 
											  Translator.getString("Error.Node_Same_Names"),
											  "Error",
											  JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		return true;
	}
	
	protected boolean checkIndexOK()
	{
		GEContainer container = this.document.getGraphStructure().containerNodes.getContainerNodeFromNameWithID(parentsJComboBox.getSelectedItem().toString());
		/** Must select an index if the parent of the node is a splitjoin */
		if (container.getType() == GEType.SPLIT_JOIN)
		{
			/** Do not have to do anything else if we encounter a splitter or a joiner.
			 *  We do not want to set the index for a splitter or a joiner  */
			String[] types = Constants.SPLITTER_TYPE_NAMES;
			for (int i = 0; i < types.length; i++)
			{
				if (this.getName() == types[i]) return true;
			}
			types = Constants.JOINER_TYPE_NAMES;
			for (int i =0; i < types.length; i++)
			{
				if (this.getName() == types[i]) return true;
			}
			
			
			/** Determine how many succesors (inner nodes) the splitjoin has. The size + 1
			 * 	will determine the places that can be used as indices.*/
			int size = container.getSuccesors().size() + 1;
			Integer indices[] = new Integer[size];
			
			/** Initialize the array of the possible values for the array */
			for (int i=0; i < size; i++)
			{
				indices[i]= new Integer(i);
			}
			/** Set the existing value of the index. If no index has been set before (this node
			 * has just been created), then the default value will be zero.*/
			Integer initValue = new Integer(0);
			if (this.sjIndex >= 0)
			{
				
				initValue = new Integer(sjIndex);
			}
			
			/** Show the dialog where the user will input an index value */
			Integer index = (Integer) JOptionPane.showInputDialog(this, "Select the index within the SplitJoin", "Index Selection", 
														JOptionPane.PLAIN_MESSAGE, null, indices, initValue);
			/** Set the index value that was selected */						
			if ((index != null) && (index.intValue() >= 0)) 
			{
				this.setIndexInSJ(index.intValue());	
			}	 
		}
		
		
		
		return true;
	}
	
	/**
	 * Determine wheter or not it is legal to change the parent of the node ( it is legal to
	 * change the parent if the node is not connected).
	 * @return True if the node is allowed to change parents; false, otherwise.
	 */
	protected boolean checkChangeParent()
	{
		if ((savedParent != null) && (! (savedParent.equals(parentsJComboBox.getSelectedItem().toString()))) && 
			(this.isConnected.equals(Constants.CONNECTED)))
		{
			JOptionPane.showMessageDialog(this, 
										  Translator.getString("Error.Parent_Change_Connected"), 
										  Translator.getString("Error"), 
										  JOptionPane.ERROR_MESSAGE);
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
		toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT ));
		cancelButton = new JButton();
		finishedButton = new JButton();
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog(evt);
			}
		});

		finishedButton.setText(Translator.getString("OK"));
		finishedButton.setName(Translator.getString("OK"));
		
		finishedButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				finishedButtonActionPerformed(evt);
			}
		});

		toolBar.add(finishedButton);
		getRootPane().setDefaultButton(finishedButton);

		cancelButton.setText(Translator.getString("Cancel"));
		cancelButton.setName(Translator.getString("Cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});
		toolBar.add(cancelButton);

		getContentPane().add(toolBar, BorderLayout.SOUTH );        
	}
	
	/** 
	 * Calls the action_ok method
	 * @see #action_ok
	 */    
	protected void finishedButtonActionPerformed(java.awt.event.ActionEvent evt) 
	{
		if (action_ok())
		{
			setVisible(false);
			dispose();
			canceled = false;
		} 
	}
    
	/** 
	 * Calls the action_cancel method
	 * @see #action_cancel
	 */    
	protected void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) 
	{
		action_cancel();
	}
    
	/** Closes the dialog 
	 * @see #action_cancel
	 * */
	protected void closeDialog(java.awt.event.WindowEvent evt) 
	{
		action_cancel();
	}
}




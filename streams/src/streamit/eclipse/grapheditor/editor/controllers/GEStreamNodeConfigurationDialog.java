/*
 * Created on Dec 8, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.awt.Dimension;
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
import streamit.eclipse.grapheditor.graph.GEContainer;

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
	protected JTextField inputTapeTextField;
	protected JTextField outputTapeTextField;
		
	protected JComboBox parentsJComboBox;
	
	protected GPDocument document;
	protected String savedName;
	protected int savedLevel;
	
	/**
	 * Constructor for the GEStreamNodeConfigurationDialog.
	 * @param parent Frame The parent of the configuration dialog.
	 * @param document GPDocument
	 */
	public GEStreamNodeConfigurationDialog(Frame parent, GPDocument document) {
        
		super(parent, true);
		this.document = document;
		this.savedLevel = -1;
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
   
   /** Save the name of the GEStreamNode to be modified (before any changes are made to it)
    * @param String name to be saved.
    */
	protected void saveInitialName(String name)
   	{
		savedName = name;
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
	protected void action_ok() 
	{
		/** The node cannot have itself as its parent */
		if (savedName == parentsJComboBox.getSelectedItem().toString())
		{
			String message = "Illegal parent was selected";
			JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
			return;					
		}	
		
		if (savedLevel != -1)
		{
			GEContainer container = this.document.getGraphStructure().containerNodes.getContainerNodeFromName(parentsJComboBox.getSelectedItem().toString());
			
			if (savedLevel < container.getDepthLevel())
			{
				String message = "Illegal parent was selected (hierarchy)";
				JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
				return;									
			}
		}
		
		/*
		try {
			Integer.parseInt(nameTextField.getText());
			Integer.parseInt(parentTextField.getText());
			Integer.parseInt(inputTapeTextField.getText());
			Integer.parseInt(outputTapeTextField.getText());
			Integer.parseInt(argumentsTextField.getText());
		} catch (Exception e) {
			String message = Translator.getString("Error.SpacingMustBeNumbers");
			JOptionPane.showMessageDialog(this, message, Translator.getString("Error"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}*/
		
		setVisible(false);
		dispose();
		canceled = false;
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
		return inputTapeTextField.getText().trim();
	}
    
	/**
	 * Returns the value of the "Output Tape" field as text.
	 */
	public String getOutputTape() {
		return outputTapeTextField.getText().trim();
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
	public void setImmediateParent(String text) {
		
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
		inputTapeTextField.setText(text);
	}
	
	/**
	 * Set the value of the "Output Tape" text field.
	 */
	public void setOutputTape(String text) {
		outputTapeTextField.setText(text);
	}


	public void setCurrentDocument(GPDocument document) {
		this.document = document;
	}
	
	/**
	 * Initialize the graphical components of the configuration dialog.
	 * The initial values in the dialog will be the current values for the 
	 * properties of the GEStreamNode or the default values if the GEStreamNode 
	 * was just created. 
	 */  
	protected abstract void initComponents();
	
	/** 
	 * Calls the action_ok method
	 * @see #action_ok
	 */    
	protected void finishedButtonActionPerformed(java.awt.event.ActionEvent evt) 
	{
		action_ok();
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




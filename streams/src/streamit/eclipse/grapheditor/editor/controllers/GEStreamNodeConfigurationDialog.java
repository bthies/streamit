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
import javax.swing.JPanel;
import javax.swing.JTextField;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;

/**
 * @author jcarlos
 *
 */
public abstract class GEStreamNodeConfigurationDialog extends JDialog{


	/** 
	 * Boolean for the cancel operation variables
	 */
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
	
	/**
	 * Creates new form GEPipelineConfigurationDialog
	 */
	public GEStreamNodeConfigurationDialog(Frame parent, GPDocument document) {
        
		super(parent, true);
		this.document = document;



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
	 * Called by pressing the cancel button.
	 */
	protected void action_cancel() {
		setVisible(false);
		dispose();
		canceled = true;
	}
    
	/** 
	 * Called by pressing the ok button.
	 */
	protected void action_ok() {
		
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
	

	/** Initialize the Swing Components
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




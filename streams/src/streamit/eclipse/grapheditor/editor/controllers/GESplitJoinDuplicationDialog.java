/*
 * Created on Mar 6, 2004
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * Dialog that is used so that the user can input the number of times that he 
 * wants a specific inner child of a SplitJoin to be duplicated.
 * 
 * @author jcarlos
 */
public class GESplitJoinDuplicationDialog extends JDialog{

	private String dialogType = "SplitJoin Child Duplication";
	
	protected boolean canceled;

	protected JPanel jPanel1;
	
	protected JPanel toolBar;
	protected JButton cancelButton;
	protected JButton finishedButton;
	
	private JLabel duplicationNumberLabel = new JLabel();
	private JTextField duplicationNumberTextField;
	
	/**
	 * Constructor for the GESplitJoinDuplicationDialog.
	 * @param parent Frame parent of the dialog
	 */
	public GESplitJoinDuplicationDialog(Frame parent) 
	{    
		super(parent, true);
		initComponents();
		setTitle(dialogType);
		setName(dialogType);		
		setPosition();
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
	 * Returns true if the dialog has been canceled.
	 */
	public boolean canceled() {
		return canceled;
	}
    
	/**
	 * Checks that the value entered in the dialog is valid (i.e. that the input value 
	 * from the user is an int)
	 */ 
	protected void action_ok() 
	{
		
		try 
		{
			Integer.parseInt(duplicationNumberTextField.getText());
		} catch (Exception e) 
		{
			String message = "The value entered into the dialog box must be an int";
			JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		setVisible(false);
		dispose();
		canceled = false;
	}
	
	public int getInputValue()
	{
		return Integer.parseInt(duplicationNumberTextField.getText());
	}
	
	
	/**
	 * Initialize the graphical components of the splitjoin child duplication dialog.
	 * It will allow the user to input a valid int that will determine the number of times
	 * that the child will be duplicated. 
	 */ 
	public void initComponents()
	{
		jPanel1 = new JPanel(new GridLayout(2,2));
		toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT ));
		cancelButton = new JButton();
		finishedButton = new JButton();
	
		duplicationNumberLabel = new JLabel();
		duplicationNumberTextField = new JTextField();
	
	
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
	
		duplicationNumberLabel.setText("Number of duplications");
		duplicationNumberLabel.setName("Number of duplications");
		jPanel1.add(duplicationNumberLabel);
		jPanel1.add(duplicationNumberTextField);
	
		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
	
		pack();
	}
	
	
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

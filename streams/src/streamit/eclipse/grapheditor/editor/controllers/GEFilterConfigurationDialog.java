/*
 * Created on Dec 8, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.controllers;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;

/**
 * @author jcarlos
 *
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
	
	public GEFilterConfigurationDialog(Frame parent, GPDocument document)
	{
		super(parent, document);
		initComponents();
        
		
		setTitle(dialogType);
		setName(dialogType);
		setPosition();
	}

	/** 
	 * Called by pressing the ok button.
	 */
	protected void action_ok() {
		
		
		try {
			Integer.parseInt(pushTextField.getText());
			Integer.parseInt(popTextField.getText());
			Integer.parseInt(peekTextField.getText());
		} catch (Exception e) {
			String message = "The Pop, peek and push rates must be integer values";
			JOptionPane.showMessageDialog(this, message, Translator.getString("Error"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		setVisible(false);
		dispose();
		canceled = false;
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
		
	protected void initComponents()
	{
		jPanel1 = new JPanel(new GridLayout(7,7));
		toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT ));
		cancelButton = new JButton();
		finishedButton = new JButton();

		nameLabel = new JLabel();
		parentLabel = new JLabel();
		inputTapeLabel = new JLabel();
		outputTapeLabel = new JLabel();
		pushLabel = new JLabel();
		popLabel = new JLabel();
		peekLabel = new JLabel();
	
		nameTextField = new JTextField();
		inputTapeTextField = new JTextField();
		outputTapeTextField = new JTextField();
		pushTextField = new JTextField();
		popTextField = new JTextField();
		peekTextField = new JTextField();
		
		parentsJComboBox = new JComboBox(this.document.getGraphStructure().containerNodes.getAllContainerNames());

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
		jPanel1.add(inputTapeTextField);
		
		outputTapeLabel.setText("Output Tape");
		outputTapeLabel.setName("Output Tape");
		jPanel1.add(outputTapeLabel);
		jPanel1.add(outputTapeTextField);
		
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




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
public class GESplitJoinConfigurationDialog extends GEStreamNodeConfigurationDialog
{
	private String dialogType = "SplitJoin Configuration";

	public GESplitJoinConfigurationDialog(Frame parent, GPDocument document)
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
			/*
			StringTokenizer strToken = new StringTokenizer(splitterWeightsTextField.getText().trim(), ",");
			while(strToken.hasMoreTokens())
			{
				String token = strToken.nextToken();
				System.out.println("TOKEN " + token);
				Integer.parseInt(token);
			}*/
			//GEProperties.weightsToInt(splitterWeightsTextField.getText().trim()) ;
			
			
		} catch (Exception e) {
			String message = "The weights must be separated by a comma and no spaces in between";
			JOptionPane.showMessageDialog(this, message, Translator.getString("Error"), JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		setVisible(false);
		dispose();
		canceled = false;
	}
	

	
	
			
	protected void initComponents()
	{
		jPanel1 = new JPanel(new GridLayout(5,5));
		toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT ));
		cancelButton = new JButton();
		finishedButton = new JButton();

		nameLabel = new JLabel();
		parentLabel = new JLabel();
		inputTapeLabel = new JLabel();
		outputTapeLabel = new JLabel();
	
		nameTextField = new JTextField();
		inputTapeTextField = new JTextField();
		outputTapeTextField = new JTextField();
		
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

		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

		pack();
		
	}
}




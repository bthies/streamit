/*
 * Created on Jan 28, 2004
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
import streamit.eclipse.grapheditor.graph.utils.StringTranslator;

/**
 * Dialog used to view and set the properties of a GESplitter.
 * Valid values must be entered for all the properties.
 * 
 * @author jcarlos
 */
public class GESplitterConfigurationDialog extends GEStreamNodeConfigurationDialog{


	private String dialogType = "Splitter Configuration";
	
	protected JLabel splitterWeightsLabel = new JLabel();
	protected JTextField splitterWeightsTextField;
	
	
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
	protected void action_ok() 
	{
		try
		{		
			StringTranslator.weightsToInt(this.getSplitterWeights());
		}
		catch(Exception e)
		{
			String message = "Please enter a legal weight";
			JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.INFORMATION_MESSAGE);
			return;	
		}
		setVisible(false);
		dispose();
		canceled = false;
	}
		
		
	/**
	 * Initialize the graphical components of the configuration dialog.
	 * The initial values in the dialog will be the current values for the 
	 * properties of the GEStreamNode or the default values if the GEStreamNode 
	 * was just created. 
	 */ 
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
		splitterWeightsLabel = new JLabel();

		nameTextField = new JTextField();
		inputTapeTextField = new JTextField();
		outputTapeTextField = new JTextField();
		splitterWeightsTextField = new JTextField();
		
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
/*
 * Created on Feb 3, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.controllers.GEStreamNodeConfigurationDialog;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEFeedbackLoop;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that groups the selected cells into a newly created GEFeedbackLoop.
 * A GEFeedbackLoop requires a splitter, a joiner, a loop and a body.
 * The user will have the freedom to determine which of the cells will be the 
 * cell and which one will be the body. 
 * @author jcarlos
 */
public class EditGroupIntoFeedbackLoop extends AbstractActionDefault {

	/**
	 * Constructor for EditGroupIntoFeedbackLoop.
	 * @param graphpad
	 */
	public EditGroupIntoFeedbackLoop(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * Group the selected cells into a newly created GEFeedbackLoop.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();
		GESplitter splitter = null;
		GEJoiner joiner = null;
		ArrayList succList = new ArrayList(); 
		GEContainer toplevel = graphpad.getCurrentDocument().getGraphStructure().getTopLevel();
	
		/** Determine which of the cells are splitters, joiners, and others */
		for (int j = 0; j < cells.length; j++)
		{
			if (cells[j] instanceof GESplitter)
			{
				splitter = (GESplitter) cells[j];
			}
			else if (cells[j] instanceof GEJoiner)
			{
				joiner =  (GEJoiner) cells[j];
			}
			else if (cells[j] instanceof GEStreamNode)
			{
				succList.add(cells[j]);
			}
		}
		
		/** Must have a splitter among the selected cells */
		if (splitter == null)
		{
			JOptionPane.showMessageDialog(graphpad,
				"The SplitJoin does not have an assigned Splitter.",
				"Error",
				JOptionPane.ERROR_MESSAGE);
	
			return;	
		}
		/** Must have a joiner among the selected cells */
		else if (joiner == null)
		{
			JOptionPane.showMessageDialog(graphpad,
				"The SplitJoin does not have an assigned Joiner.",
				"Error",
				JOptionPane.ERROR_MESSAGE);			
			return;
		}
		
		/** Bring out the dialog to select the body and the loop */
		GEFeedbackLoopGroupingDialog dialog = new GEFeedbackLoopGroupingDialog(new JFrame(), 
																	graphpad.getCurrentDocument(),
																	succList);
		dialog.setVisible(true);
		
		/** Clicked cancel in the dialog */	
		if (dialog.canceled()) 
		{
			return;
		}
		/** Clicked OK in the dialog */
		else
		{
			/** Get the properties (body, loop) selected in the dialog. */
			Properties properties = dialog.getPropertiesInDialog();
			String bodyName = properties.getProperty(GEProperties.KEY_FLOOP_BODY);
			String loopName = properties.getProperty(GEProperties.KEY_FLOOP_LOOP);
			
			/** Cannot have the body and the loop be the same */
			if (bodyName == loopName)
			{
				JOptionPane.showMessageDialog(graphpad,
					"Cannot select the same node for both the body and the loop",
					"Error",
					JOptionPane.ERROR_MESSAGE);			
				return;
			}
		
			/** Get the GEStreamNodes corresponding to the body and loop names. */
			GEStreamNode body = null;
			GEStreamNode loop = null;
			for (Iterator nodeIter = succList.iterator(); nodeIter.hasNext();)
			{
				GEStreamNode node = (GEStreamNode) nodeIter.next();
				if (node.getName() == bodyName)
				{
					body = node;
				}
				if (node.getName() == loopName)
				{
					loop = node;
				}
			}
		
			/** Must have selected a valid loop for the feedbackloop */	
			if (loop == null)
			{
				JOptionPane.showMessageDialog(graphpad,
					"Must select a loop for the feedbackloop",
					"Error",
					JOptionPane.ERROR_MESSAGE);			
				return;
			}
			
			/** Must have selected a valid body for the feedbackloop */
			if (body == null)
			{
				JOptionPane.showMessageDialog(graphpad,
					"Must select a body for the feedbackloop",
					"Error",
					JOptionPane.ERROR_MESSAGE);			
				return;				
			}
		
			//TODO: Might be problem if removing node not in succesor list (i.e. splitter, joiner in splitjoin)
			loop.getEncapsulatingNode().removeNodeFromContainer(loop);
			body.getEncapsulatingNode().removeNodeFromContainer(body);
			splitter.getEncapsulatingNode().removeNodeFromContainer(splitter);
			joiner.getEncapsulatingNode().removeNodeFromContainer(joiner);
			
			
			GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure(); 
			
			/** Create the GEFeedbackLoop that will hold the selected components */
			GEFeedbackLoop floop = new GEFeedbackLoop("FeedbackLoop_"+ GEProperties.id_count++, 
														splitter, joiner, body, loop);
			floop.setEncapsulatingNode(toplevel);
			
	
			floop.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
			toplevel.addNodeToContainer(floop);
			
			
			/** Update hierarchy panel */
			EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
															get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
			ac.actionPerformed(null);
		}		
	}
	
	/**
	 * The dialog used to select the body and loop of the GEFeedbackLoop.
	 * @author jcarlos
	 */
	private class GEFeedbackLoopGroupingDialog extends GEStreamNodeConfigurationDialog
	{
		protected String dialogName = "FeedbackLoop Grouping";
		protected boolean canceled;

		protected JPanel jPanel1;
		protected JPanel toolBar;
		protected JButton cancelButton;
		protected JButton finishedButton;

		protected JLabel bodyLabel = new JLabel();
		protected JLabel loopLabel = new JLabel();

		protected JComboBox bodyJComboBox;
		protected JComboBox loopJComboBox;
	
		protected GPDocument document;
		protected ArrayList cells;
		
		/**
		 * Default constructor
		 * @param parent Frame 
		 * @param document GPDocumnet
		 * @param succList ArrayList with the cells that can be selected as body or loop.
		 */	
		public GEFeedbackLoopGroupingDialog(Frame parent, GPDocument document, ArrayList succList)
		{
			super(parent, document);
			this.document = document;
			this.cells = succList;
			
			initComponents();
			setTitle(dialogName);

			setPosition();
		}
		
		/**
		 * Get the properties selected in the dialog.
		 * @return Properties.
		 */
		public Properties getPropertiesInDialog()
		{
			Properties properties = new Properties();
			properties.put(GEProperties.KEY_FLOOP_LOOP, this.getLoopName());
			properties.put(GEProperties.KEY_FLOOP_BODY, this.getBodyName());
			return properties;
		}
		
		/**
		 * Returns the value of the "Loop" field as text.
		 */
		public String getLoopName()
		{
			return loopJComboBox.getSelectedItem().toString();
		}
		
		/**
		 * Returns the value of the "Body" field as text.
		 */
		public String getBodyName()
		{
			return bodyJComboBox.getSelectedItem().toString();
		}
		
		
		/** 
		 * Initialize the Swing Components in the dialog.
		 */   
		protected void initComponents() 
		{
			
			jPanel1 = new JPanel(new GridLayout(3,3));
			toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT ));
			cancelButton = new JButton();
			finishedButton = new JButton();
		
			bodyLabel = new JLabel();
			loopLabel = new JLabel();
		
			Object [] names = GEStreamNode.getNodeNames(cells);
			loopJComboBox = new JComboBox(names);
			bodyJComboBox = new JComboBox(names);
		
		
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
			
			loopLabel.setText("Loop");
			loopLabel.setName("Loop");
			jPanel1.add(loopLabel);
			jPanel1.add(loopJComboBox);
			
			bodyLabel.setText("Body");
			bodyLabel.setName("Body");
			jPanel1.add(bodyLabel);
			jPanel1.add(bodyJComboBox);
			
					
			getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
		
			pack();
		}	
	}
}
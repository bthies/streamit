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
import javax.swing.JPanel;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.controllers.GEStreamNodeConfigurationDialog;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.ErrorCode;
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
 * 
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
			ErrorCode.handleErrorCode(ErrorCode.CODE_NO_SPLITTER);	
			return;	
		}
		/** Must have a joiner among the selected cells */
		else if (joiner == null)
		{
			ErrorCode.handleErrorCode(ErrorCode.CODE_NO_JOINER);			
			return;
		}
		
		
		splitter.getEncapsulatingNode().removeNodeFromContainer(splitter);
		joiner.getEncapsulatingNode().removeNodeFromContainer(joiner);
					
		GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure(); 
		
		/** Create the GEFeedbackLoop that will hold the selected components */
		GEFeedbackLoop floop = new GEFeedbackLoop("FeedbackLoop_"+ GEProperties.id_count++, 
													splitter, joiner, null, null);
		toplevel.addNodeToContainer(floop);
		floop.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());

		
		for (Iterator succListIter = succList.iterator(); succListIter.hasNext();)
		{
			((GEStreamNode) succListIter.next()).changeParentTo(floop);	
		}


		
		/** Update hierarchy panel */
		EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
										get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
		ac.actionPerformed(null);
	}		
}
	
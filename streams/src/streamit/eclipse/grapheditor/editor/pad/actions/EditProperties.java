/*
 * Created on Dec 9, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.controllers.Controller;
import streamit.eclipse.grapheditor.editor.controllers.GEFeedbackLoopController;
import streamit.eclipse.grapheditor.editor.controllers.GEFilterController;
import streamit.eclipse.grapheditor.editor.controllers.GEJoinerController;
import streamit.eclipse.grapheditor.editor.controllers.GEPipelineController;
import streamit.eclipse.grapheditor.editor.controllers.GESplitJoinController;
import streamit.eclipse.grapheditor.editor.controllers.GESplitterController;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action to edit the properties of a GEStreamNode.
 * @author jcarlos
 */
public class EditProperties extends AbstractActionFile {

	
	/**
	 * Constructor for EditProperties.
	 * @param graphpad
	 * @param name
	 */
	public EditProperties(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Action to edit the properties of a GEStreamNode. The properties of the GEStreamNode will
	 * apper and the user will be able to modify those properties.
	 */
	public void actionPerformed(ActionEvent e)
	{
		/** Get the selected cell */
		GEStreamNode cell = (GEStreamNode) graphpad.getCurrentDocument().getGraph().getSelectionCell();
		
		if (cell != null)
		{
			String type = cell.getType();
			Controller _control = null;
			
			/** Determine what type of controller is being used */
			if (type == GEType.PIPELINE)
			{
				_control = new GEPipelineController();	
			}
			else if (type == GEType.PHASED_FILTER)
			{
				_control = new GEFilterController();
			}
			else if (type == GEType.SPLIT_JOIN)
			{
				_control = new GESplitJoinController();
			}
			else if (type ==  GEType.JOINER)
			{
				_control = new GEJoinerController();				
			}
			else if (type == GEType.SPLITTER)
			{
				_control = new GESplitterController();
			}
			else if (type == GEType.FEEDBACK_LOOP)
			{
				_control = new GEFeedbackLoopController();
			}
			
			/** Display the configuration dialog. If the user presses OK and the properties 
			 *  that were selected are valid .... */
			if (_control.configure(graphpad.getCurrentDocument(), cell.getNodeProperties()))
			{
				GraphStructure graphStruct =  graphpad.getCurrentDocument().getGraphStructure();
				/** Set the properties of the GEStreamNode */
				cell.setNodeProperties(_control.getConfiguration(), graphStruct.getJGraph(), graphStruct.containerNodes);
				graphpad.getCurrentDocument().getTreePanel().update();
				graphpad.getCurrentDocument().updateUI();
				graphpad.update();			
			}
		}
	}
}
			

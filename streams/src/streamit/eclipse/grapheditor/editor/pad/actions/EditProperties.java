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
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;

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
		GEStreamNode cell = (GEStreamNode) graphpad.getCurrentDocument().getGraph().getSelectionCell();
		
		if (cell != null)
		{
			String type = cell.getType();
			Controller _control = null;
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
			if (_control.configure(graphpad.getCurrentDocument(), GEProperties.getNodeProperties(cell)))
			{
				GEProperties.setNodeProperties(cell, 
											   _control.getConfiguration(),
											   //graphpad.getCurrentGraph())
											   //graphpad.getCurrentDocument().getGraphStructure().getJGraph()
											   graphpad.getCurrentDocument().getGraphStructure());		
			}
		}
	}
}
			

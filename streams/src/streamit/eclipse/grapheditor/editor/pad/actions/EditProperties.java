/*
 * Created on Dec 9, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.controllers.GEFilterController;
import streamit.eclipse.grapheditor.editor.controllers.GEJoinerController;
import streamit.eclipse.grapheditor.editor.controllers.GEPipelineController;
import streamit.eclipse.grapheditor.editor.controllers.GESplitJoinController;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;

/**
 * @author jcarlos
 *
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
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e)
	{
		GEStreamNode cell = (GEStreamNode) graphpad.getCurrentDocument().getGraph().getSelectionCell();
		
		if (cell != null)
		{
			String type = cell.getType();
			if (type == GEType.PIPELINE)
			{
				GEPipelineController pControl = new GEPipelineController();
				if (pControl.configure(graphpad.getCurrentDocument(), GEProperties.getNodeProperties(cell)))
				{
					GEProperties.setNodeProperties(cell, 
					                               pControl.getConfiguration(),
												   graphpad.getCurrentDocument().getGraphStructure().getJGraph());
				}
			}
			else if (type == GEType.PHASED_FILTER)
			{
				GEFilterController fControl = new GEFilterController();
				if (fControl.configure(graphpad.getCurrentDocument(), GEProperties.getNodeProperties(cell)))
				{
					GEProperties.setNodeProperties(cell, 
												   fControl.getConfiguration(),
												   graphpad.getCurrentGraph());
				}
			}
			else if (type == GEType.SPLIT_JOIN)
			{
				GESplitJoinController sjControl = new GESplitJoinController();
				if (sjControl.configure(graphpad.getCurrentDocument(), GEProperties.getNodeProperties(cell)))
				{
					GEProperties.setNodeProperties(cell, 
												   sjControl.getConfiguration(),
												   graphpad.getCurrentDocument().getGraphStructure().getJGraph());	
				}
			}
			else if (type ==  GEType.JOINER)
			{
				GEJoinerController jControl = new GEJoinerController();
				if (jControl.configure(graphpad.getCurrentDocument(), GEProperties.getNodeProperties(cell)))
				{
					GEProperties.setNodeProperties(cell, 
												   jControl.getConfiguration(),
												   graphpad.getCurrentDocument().getGraphStructure().getJGraph());
				}
				
			}
			else if (type == GEType.SPLITTER)
			{
				GEJoinerController sControl = new GEJoinerController();
				if (sControl.configure(graphpad.getCurrentDocument(), GEProperties.getNodeProperties(cell)))
				{
					GEProperties.setNodeProperties(cell, 
												   sControl.getConfiguration(),
												   graphpad.getCurrentDocument().getGraphStructure().getJGraph());		
				}
			}
		}
	}
}

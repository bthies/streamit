/*
 * Created on Feb 3, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEPipeline;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that groups the selected node into a newly created pipeline.
 * The newly created GEPipeline is added at the toplevel node. The parents
 * for the immediate nodes (oldest ancestors) must be set to the newly created pipeline.
 * @author jcarlos
 */
public class EditGroupIntoPipeline extends AbstractActionDefault {

	/**
	 * Constructor for EditGroupIntoPipeline.
	 * @param graphpad
	 */
	public EditGroupIntoPipeline(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * Group the selected nodes into a new created GEPipeline.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();
	
		/** Check if selection to group is valid (must have at least one GEStreamNode) */
		boolean valid = false;
		for (int j = 0; j < cells.length; j++)
		{
			if (cells[j] instanceof GEStreamNode)
			{
				valid = true;
				break;
			}
		}
		
		if (valid == true)
		{
			ArrayList succList = new ArrayList(); 
			GEContainer toplevel = graphpad.getCurrentDocument().getGraphStructure().getTopLevel();
			GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure(); 
			
			/** Create a new GEPipeline */
			GEPipeline pipeline = new GEPipeline("Pipeline_"+ GEProperties.id_count++);
			
			/** The encapsulating node of all newly created containers is initially the toplevel container.*/
			toplevel.addNodeToContainer(pipeline);
			pipeline.initializeNode(graphStruct, pipeline.getEncapsulatingNode().getDepthLevel() + 1);
			
			/** Go through the selected cells ...*/
			ArrayList containersToGroup = new ArrayList ();
			for (int i = 0; i < cells.length; i++)
			{
				/** Ignore the cells that are not GEStreamNodes (i.e Edges) */
				if (cells[i] instanceof GEStreamNode)
				{				
					GEStreamNode node = (GEStreamNode)cells[i];
					
					/** Only want to change the parent of a node whenever its encapsulating node is the 
					 * toplevel pipeline.  
					 */
					if (node.getEncapsulatingNode() == toplevel)
					{
						GEProperties.setParentProperty(node, pipeline);
					}

					/** If the encapsulating node is not toplevel, then we must change the 
					 *  encapsulating node of the oldest ancestor that is not the toplevel. */
					else
					{
						GEStreamNode cont = node.getOldestContainerAncestor(toplevel);
						/** Adding to arraylist in order to set the parent later. A different loop
						 * is needed since this would be affecting the oldest container.*/
						if ( ! (containersToGroup.contains(cont)))
						{
							containersToGroup.add(cont);
							
						}
					}
				}
			} 
			
			/** Set the newly created pipeline as the parent of the oldest ancestors from the selected nodes.*/
			for (Iterator contToGroupIter= containersToGroup.iterator(); contToGroupIter.hasNext();)
			{
				GEStreamNode container = (GEStreamNode)contToGroupIter.next();
				GEProperties.setParentProperty(container, pipeline);
			}
			
			/** Update the hierarchy panel */
			EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
											get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
			ac.actionPerformed(null);
		}
	}
}
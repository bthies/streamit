/*
 * Created on Apr 3, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * 
 * @author jcarlos
 */

public class EditUngroup extends AbstractActionDefault {

	/**
	* Constructor for EditUngroup.
	* @param graphpad
	*/
	public EditUngroup(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Group the selected cells into a newly created splitjoin.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object cell = getCurrentGraph().getSelectionCell();
		
		if (cell instanceof GEStreamNode)
		{
			GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
			GEContainer container = null;
			
			/** If the selected cell is a container, then we will deal with this node */
			if (cell instanceof GEContainer)
			{
				container = (GEContainer) cell;					
			}
			/** If the selected cell is not a container, then we have to deal with its 
			 * 	encapsulting node. */
			else
			{
				container =  ((GEStreamNode)cell).getEncapsulatingNode();
			}	
			
			/** The only case when we do not ungroup is when we are dealing with the toplevel node */
			if (container != graphStruct.getTopLevel())
			{			 
				/**  Change the parent of all the contained elements to the toplevel*/
				for (Iterator containedIter = container.getContainedElements().iterator(); containedIter.hasNext();)
				{
					graphStruct.getTopLevel().addNodeToContainer((GEStreamNode)containedIter.next());
				}	
				
				/** Remove the container from the list of containers */
				//TODO: Instead of removing the container, it could be saved so that it keeps it properties 
				// once we regroup
				graphStruct.containerNodes.removeContainer(container, new ArrayList());
				
				/** Update the hierarchy panel */
				EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
								get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
				ac.actionPerformed(null);
			}

		}
	}
	
}
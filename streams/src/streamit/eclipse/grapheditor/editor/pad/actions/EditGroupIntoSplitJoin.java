/*
 * Created on Feb 2, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.ErrorCode;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GESplitJoin;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that groups the selected cells into a newly created splitjoin. In order 
 * to be able to group, a splitter and joiner must be present in the selected cells.
 * The newly created splitjoin will have the toplevel node as its parent.
 * @author jcarlos
 */
public class EditGroupIntoSplitJoin extends AbstractActionDefault {

	/**
	 * Constructor for EditGroupIntoSplitJoin.
	 * @param graphpad
	 */
	public EditGroupIntoSplitJoin(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * Group the selected cells into a newly created splitjoin.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();
		GESplitter splitter = null;
		GEJoiner joiner = null;
		ArrayList succList = new ArrayList();
		GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure(); 
		GEContainer toplevel = graphStruct.getTopLevel();
		
		/** Traverse all the cells that were selected to be grouped into a splitjoin*/
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
		
		/** Display error message in case that there is no splitter */
		if (splitter == null)
		{
			ErrorCode.handleErrorCode(ErrorCode.CODE_NO_SPLITTER);
			return;	
		}
		
		/** Display an error message in case that there is no joiner */
		else if (joiner == null)
		{
			ErrorCode.handleErrorCode(ErrorCode.CODE_NO_JOINER);
			return;
		}
		
		/** Remove the splitter and joiner from their previous container */
		splitter.getEncapsulatingNode().removeNodeFromContainer(splitter);
		joiner.getEncapsulatingNode().removeNodeFromContainer(joiner);
		
		/** Create a new splitjoin and add it to the toplevel container*/
		GESplitJoin splitjoin = new GESplitJoin("Splitjoin_"+ GEProperties.id_count++, splitter, joiner);
		toplevel.addNodeToContainer(splitjoin);
		
		
		Object[] nodeList = succList.toArray();
		ArrayList containersToGroup = new ArrayList();
		
		/** Go through the list of selected cells (in this case all cells are GEStreamNodes)*/
		for (int i = 0; i < nodeList.length; i++)
		{
			GEStreamNode node = (GEStreamNode)nodeList[i];
			
			/** Only want to change the parent of a node whenever its encapsulating node is the 
			 * toplevel pipeline. */
			if (node.getEncapsulatingNode() == toplevel)
			{
				node.changeParentTo(splitjoin);	
			}
			/** If the encapsulating node is not toplevel, then we must change the 
			 *  encapsulating node of the oldest ancestor that is not the toplevel. */
			else
			{
				GEStreamNode container = node.getOldestContainerAncestor(toplevel);
				if ( ! (containersToGroup.contains(container)))
				{
					containersToGroup.add(container);
					container.changeParentTo(splitjoin);
				}	
			}
			
		}

		/** Set the newly created splitjoin as the parent of the oldest ancestors from the selected nodes.*/
		for (Iterator contToGroupIter= containersToGroup.iterator(); contToGroupIter.hasNext();)
		{
			GEStreamNode container = (GEStreamNode)contToGroupIter.next();
			container.changeParentTo(splitjoin);
		}

		/** Initialize the splitjoin propreties and add the splitjoin to the toplevel container */
		splitjoin.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
				
		/** Update the hierarchy panel */
		EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
										get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
		ac.actionPerformed(null);
	
	}
}
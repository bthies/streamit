/*
 * Created on Feb 2, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
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
		GEContainer toplevel = graphpad.getCurrentDocument().getGraphStructure().getTopLevel();
		
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
			JOptionPane.showMessageDialog(graphpad,
				"The SplitJoin does not have an assigned Splitter.",
				"Error",
				JOptionPane.ERROR_MESSAGE);

			return;	
		}
		
		/** Display an error message in case that there is no joiner */
		else if (joiner == null)
		{
			JOptionPane.showMessageDialog(graphpad,
				"The SplitJoin does not have an assigned Joiner.",
				"Error",
				JOptionPane.ERROR_MESSAGE);			
			return;
		}
		
		splitter.getEncapsulatingNode().removeNodeFromContainer(splitter);
		joiner.getEncapsulatingNode().removeNodeFromContainer(joiner);
		
		GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure();
		
		/** Create a new splitjoin */
		GESplitJoin splitjoin = new GESplitJoin("Splitjoin_"+ GEProperties.id_count++, splitter, joiner);
		splitjoin.setEncapsulatingNode(toplevel);
		Object[] nodeList = succList.toArray();
		
		/** Set the parent of the selected cells to be the newly create splitjoin */

		for (int i = 0; i < nodeList.length; i++)
		{
			GEStreamNode node = (GEStreamNode)nodeList[i];
			GEProperties.setParentProperty(node, splitjoin);
		}

		
		/** Initialize the splitjoin propreties and add the splitjoin to the toplevel container */
		splitjoin.initiliazeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
		toplevel.addNodeToContainer(splitjoin);
		
		/** Update the hierarchy panel */
		EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
																get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
		ac.actionPerformed(null);
	
	}
}
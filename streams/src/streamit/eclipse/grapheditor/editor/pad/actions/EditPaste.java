/*
 * 
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GEPipeline;
import streamit.eclipse.grapheditor.graph.GESplitJoin;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that pastes a GEStreamNode that has been copied.
 * If no node has been copied yet, then there will be no effect.
 * @author jcarlos
 */
public class EditPaste extends AbstractActionDefault {

	/**
	 * Constructor for EditPaste.
	 * @param graphpad
	 */
	public EditPaste(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Action that will create a new instance of a node that has been copied by pasting it on the screen.
	 * There will be no effect if no such node has been selected yet.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		GPDocument doc = graphpad.getCurrentDocument();
		GraphStructure graphStruct = doc.getGraphStructure();
		
		/** Get the node that was cloned (by doing a copy). If no node has been copied yet, then 
		 *  node will be equal to null 
		 */
		GEStreamNode node = doc.getClonedNode();
		
		/** Only perform the operation in the case when there is a node that has been copied (cloned) */
		if (node != null)
		{
			/** Create a node with the given properties from the cloned node */
			if (node instanceof GEContainer)
			{
				GEContainer cont = (GEContainer) node;
				ArrayList contList = new ArrayList();
				contList.add(cont);
				doc.getGraphStructure().constructNodes(contList, cont.getEncapsulatingNode(), new Rectangle (100,100,100,100));
				
				if (node instanceof GEPipeline)
				{
					ArrayList containedList= cont.getContainedElements();
					graphStruct.constructNodes(containedList, cont, new Rectangle(new Point(10,10)));

					GEStreamNode lastNode = null;
					for (int i=0; i < containedList.size(); i++)
					{
						GEStreamNode currNode = (GEStreamNode)containedList.get(i);
						if (lastNode != null)
						{
							doc.getGraphStructure().connectDraw(lastNode, currNode);
						}
						lastNode = currNode;
					}	
					cont.setDisplay(graphStruct.getJGraph());
				}
				else if (node instanceof GESplitJoin)
				{
					ArrayList succList = cont.getSuccesors();
					GESplitter splitter = ((GESplitJoin)cont).getSplitter();
					GEJoiner joiner = ((GESplitJoin) cont).getJoiner();
					
					if (splitter != null)
					{
						doc.getGraphStructure().constructANode(splitter, new Rectangle (100,100,100,100));
					}
					if (joiner != null)
					{
						doc.getGraphStructure().constructANode(joiner, new Rectangle (100,100,100,100));
					}
					graphStruct.constructNodes(succList, cont, new Rectangle(new Point(10,10)));
					for (int i=0; i < succList.size(); i++)
					{
						GEStreamNode currNode = (GEStreamNode) succList.get(i);
						GEStreamNode lastNode = currNode;
						GEStreamNode firstNode = currNode;
						if (currNode instanceof GEContainer)
						{
							firstNode = ((GEContainer)currNode).getFirstNonContainerNodeInContainer();
							lastNode = ((GEContainer)currNode).getLastNonContainerNodeInContainer();
						}
						
						if (splitter != null)
						{
							doc.getGraphStructure().connectDraw(splitter, firstNode);		
						}
						if (joiner != null)
						{
							doc.getGraphStructure().connectDraw(lastNode, joiner);
						}
					}
					
				}
			}
			else
			{
				doc.getGraphStructure().nodeCreate(node.getNodeProperties(), doc.getGraph(), new Rectangle (100,100,100,100));
			}
			
			/** Layou the graph */
			GraphDoLayout gdl = (GraphDoLayout) graphpad.getCurrentActionMap().
														get(Utilities.getClassNameWithoutPackage(GraphDoLayout.class));
			gdl.actionPerformed(null);
			
			/** Update the hierarchy panel */
			EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
											get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
			ac.actionPerformed(null);
		}
	}
}

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
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class EditGroupIntoPipeline extends AbstractActionDefault {

	/**
	 * Constructor for ShapeToFront.
	 * @param graphpad
	 */
	public EditGroupIntoPipeline(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();

		ArrayList succList = new ArrayList(); 
		GEContainer toplevel = graphpad.getCurrentDocument().getGraphStructure().getTopLevel();
		
		GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure(); 
		GEPipeline pipeline = new GEPipeline("Pipeline_"+ GEProperties.id_count++);
		pipeline.setEncapsulatingNode(toplevel);
		toplevel.addNodeToContainer(pipeline);
		
		
		ArrayList containersToGroup = new ArrayList ();
		for (int i = 0; i < cells.length; i++)
		{
			if (cells[i] instanceof GEStreamNode)
			{
				
				GEStreamNode node = (GEStreamNode)cells[i];
				/** Only want to change the parent of a node whenever its encapsulating node is the 
				 * toplevel pipeline. If it has a different encapsulating node, then that means that 
				 * it has a different parent that is also 
				 */
				if (node.getEncapsulatingNode() == toplevel)
				{
					GEProperties.setParentProperty(node, pipeline);
				}
				else
				{
					GEStreamNode cont = node.getOldestContainerAncestor(toplevel);
					if ( ! (containersToGroup.contains(cont)))
					{
						containersToGroup.add(cont);
					}
				}
			}
		} 
		
		for (Iterator contToGroupIter= containersToGroup.iterator(); contToGroupIter.hasNext();)
		{
			GEProperties.setParentProperty((GEStreamNode)contToGroupIter.next(), pipeline);
		}
		
		
		pipeline.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
		
		
		EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
																get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
		ac.actionPerformed(null);
	}
}
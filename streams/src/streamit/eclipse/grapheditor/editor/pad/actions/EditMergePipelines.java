/*
 * Created on Apr 18, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.jgraph.graph.DefaultGraphModel;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GEPipeline;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Merged the selected pipelines in the order that they were selected. 
 * @author jcarlos
 *
 */
public class EditMergePipelines extends AbstractActionDefault
{
	/**
	 * Constructor for EditGroupIntoSplitJoin.
	 * @param graphpad
	 */
	public EditMergePipelines(GPGraphpad graphpad) {
		super(graphpad);
	}
		
	/**
	 * Action that merges several pipelines.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();
		
		/** Must have selected at least two cells */
		if (cells.length >= 2)
		{
		
			/** Check that the two elements that were selected are pipelines */
			for (int i = 0; i < cells.length; i++)
			{
				/** If the selected cells are not pipelines then alert the user */
				if ( ! (cells[i] instanceof GEPipeline))
				{
					JOptionPane.showMessageDialog(graphpad, 
												  Translator.getString("Error.Must_Select_Pipelines"), 
												  Translator.getString("Error"), 
												  JOptionPane.ERROR_MESSAGE );
					return;	
				}
		
			}
			
			GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
			
			/** Create the new pipeline that will contain the merged elements of the other pipelines*/
			GEPipeline pipeline = new GEPipeline("Pipeline_"+ GEProperties.id_count++);
			
			/** Will add the newly created node to the same parent that contained the first selected pipelines */ 
			((GEPipeline)cells[0]).getEncapsulatingNode().addNodeToContainer(pipeline);
			
			
			/** Create instance of disconnect action (to be able to use its disconnect method)*/
			EditDisconnect disc = (EditDisconnect) graphpad.getCurrentActionMap().
										get(Utilities.getClassNameWithoutPackage(EditDisconnect.class));

			
			/** List that will contain all of the elements to be added to new splitjoin */
			ArrayList nodeList = new ArrayList();
			ArrayList allConts = graphStruct.containerNodes.getAllContainers();
			for (int i = 0; i < cells.length; i++)
			{
				GEPipeline pipe = (GEPipeline)cells[i];
				ArrayList oldPipeContained = pipe.getContainedElements();
				nodeList.addAll(oldPipeContained);
				GEStreamNode firstNode = pipe.getFirstNonContainerNodeInContainer();
				GEStreamNode lastNode = pipe.getLastNonContainerNodeInContainer();
				
				/** Disconnect first node in each pipe from its "source" node (the node that is the source 
				  *	in the edge between them).*/
				for (Iterator pipeSourceIter = firstNode.getSourceNodes().iterator(); pipeSourceIter.hasNext();)
				{
					GEStreamNode sNode = (GEStreamNode)pipeSourceIter.next();
					disc.disconnect(sNode, firstNode);	
				}	
				
				/** Disconnect the joiner in sj2 from its "target" node (the node that is the target 
				 * 	in the edge between them).*/
				for (Iterator pipeTargetIter = lastNode.getTargetNodes().iterator(); pipeTargetIter.hasNext();)
				{
					disc.disconnect(lastNode,(GEStreamNode)pipeTargetIter.next());	
				}
			
									
				
				allConts.remove(pipe);
				/** Remove the splitjoins that we are merging from their currents parents.
				 * 	These splitjoins have ceased to exist */
				pipe.getEncapsulatingNode().removeNodeFromContainer(pipe);
			}
			
			/** Change the parent of all the nodes in both sj1 and sj2 to the newly created splitjoin */
			for (Iterator nodeIter = nodeList.iterator(); nodeIter.hasNext();)
			{
				((GEStreamNode)nodeIter.next()).changeParentTo(pipeline);	
			}
			
			/** Graphically delete the pipelines that we have merged */
			getCurrentGraph().getModel().remove(DefaultGraphModel.getDescendants(getCurrentGraph().getModel(), 
																				 cells).toArray());
			
			/** Initialize the newly created splitjoin */
			pipeline.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
				
			
			/** Update the hierarchy panel */
			EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
										get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
			ac.actionPerformed(null);		
			
		}
		else
		{
			
		}
	}
}
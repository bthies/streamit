/*
 * Created on Apr 11, 2004
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
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GESplitJoin;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that merges two splitjoins. The two splitjoins that are to be merged must be selected.
 * In addition, one must have its splitter removed while the other one should have its joiner removed.
 * 
 * @author jcarlos
 */
public class EditMergeSplitJoins extends AbstractActionDefault {

	/**
	 * Constructor for EditGroupIntoSplitJoin.
	 * @param graphpad
	 */
	public EditMergeSplitJoins(GPGraphpad graphpad) {
		super(graphpad);
	}
		
	/**
	 * Action that merges two splitjoins.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();
		
		/** splitjoin1 is the one that contains the splitter to be used in the merged splitjoin */
		GESplitJoin sj1 = null;
		
		/** splitjoin2 is the one that contains the joiner to be used in the merged splitjoin */
		GESplitJoin sj2 = null;

		
		/** Only two splitjoins must have been selected */
		if (cells.length == 2)
		{
			/** Check that the two elements that were selected are splitjoins */
			for (int i = 0; i < cells.length; i++)
			{
				/** If the selected cells are not splitjoins then alert the user */
				if ( ! (cells[i] instanceof GESplitJoin))
				{
					JOptionPane.showMessageDialog(graphpad, 
												  Translator.getString("Error.Must_Select_Two_SplitJoins"), 
												  Translator.getString("Error"), 
												  JOptionPane.ERROR_MESSAGE );
					return;	
				}
		
			}
			
			GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
			
			/** sj1 is the first cell that was selected */
			sj1 = (GESplitJoin) cells[0];
			
			/** sj2 is the second cell that was selected */
			sj2 = (GESplitJoin) cells[1];
			
			/** Prompt the user wheter or not to delete the unused splitter/joiner from
			 * 	the merged splitjoins automatically */
			int option = JOptionPane.showOptionDialog(graphpad, 
									 	 			  Translator.getString("Message.Merge_Delete_Splitter_Joiner"), 
										 			  Translator.getString("MergeDialogTitle"), 
										 			  JOptionPane.YES_NO_OPTION,
													  JOptionPane.QUESTION_MESSAGE,
										 			  null, null, null);
			
			/** the joiner of sj1 is not used in the merge */
			GEJoiner sj1Joiner = sj1.getJoiner();

			/** The splitter of sj2 is not used in the merge */
			GESplitter sj2Splitter = sj2.getSplitter();
			
			/** The user chose to delete the unused splitter/joiner of the merged splitjoins automatically */
			if (option == 0)
			{
				sj1Joiner.deleteNode(graphStruct.getJGraph());
				sj2Splitter.deleteNode(graphStruct.getJGraph());
				
				/** Graphically delete the unused splitter and joiner*/
				getCurrentGraph().getModel().remove(DefaultGraphModel.getDescendants(getCurrentGraph().getModel(), 
																					 new Object[] {sj1Joiner, sj2Splitter})
																		.toArray());
			}
			else
			{
				sj1Joiner.changeParentTo(graphStruct.getTopLevel());			
				sj2Splitter.changeParentTo(graphStruct.getTopLevel());
			}

			/** Create the new splitjoin that will contain the merged elements of the other two splitjoins */
			GESplitJoin splitjoin = new GESplitJoin("Splitjoin_"+ GEProperties.id_count++, 
													sj1.getSplitter(), 
													sj2.getJoiner());
	
			/** List that will contain all of the elements to be added to new splitjoin */
			ArrayList nodeList = new ArrayList();
			
			/** Add all of the succesors (inner children) of sj1 to the new splitjoin */
			for (Iterator sj1SuccIter = sj1.getSuccesors().iterator(); sj1SuccIter.hasNext();)
			{
				nodeList.add(sj1SuccIter.next());
			}
			
			/** Add all of the succesors (inner children of sj2 to the new splitjoin */
			for (Iterator sj2SuccIter = sj2.getSuccesors().iterator(); sj2SuccIter.hasNext();)
			{
				nodeList.add(sj2SuccIter.next());	
			}
			
			/** Change the parent of all the nodes in both sj1 and sj2 to the newly created splitjoin */
			for (Iterator nodeIter = nodeList.iterator(); nodeIter.hasNext();)
			{
				((GEStreamNode)nodeIter.next()).changeParentTo(splitjoin);	
			}
			
				
			
			/** Create instance of disconnect action (to be able to use its disconnect method)*/
			EditDisconnect disc = (EditDisconnect) graphpad.getCurrentActionMap().
									get(Utilities.getClassNameWithoutPackage(EditDisconnect.class));
			
			
			/** List that will save the source nodes */
			ArrayList savedSource = new ArrayList();
			
			/** Disconnect the splitter in sj1 from its "source" node (the node that is the source 
			 * 	in the edge between them).*/
			for (Iterator sj1SourceIter = sj1.getSplitter().getSourceNodes().iterator(); sj1SourceIter.hasNext();)
			{
				GEStreamNode sNode = (GEStreamNode)sj1SourceIter.next();
				disc.disconnect(sNode, sj1.getSplitter());	
				savedSource.add(sNode);
			}
			
			/** Disconnect the joiner in sj2 from its "target" node (the node that is the target 
			 * 	in the edge between them).*/
			for (Iterator sj2TargetIter = sj2.getJoiner().getTargetNodes().iterator(); sj2TargetIter.hasNext();)
			{
				disc.disconnect(sj2.getJoiner(),(GEStreamNode)sj2TargetIter.next());	
			}

			/** Remove the merged splitjoins from the list of existing containers */
			graphStruct.containerNodes.getAllContainers().remove(sj1);
			graphStruct.containerNodes.getAllContainers().remove(sj2);
			
			
			/** Remove the splitjoins that we are merging from their currents parents.
			 * 	These splitjoins have ceased to exist */
			GEContainer sj1Container = sj1.getEncapsulatingNode();
			if (sj1Container != null)
			{
				sj1Container.removeNodeFromContainer(sj1);
			}
			GEContainer sj2Container = sj2.getEncapsulatingNode();
			if (sj2Container != null)
			{
				sj2Container.removeNodeFromContainer(sj2);
			}
			
			/** Graphically delete the splitjoins that we have merged */
			getCurrentGraph().getModel().remove(DefaultGraphModel.getDescendants(getCurrentGraph().getModel(), 
																				 cells).toArray());
																				 
			/** Will add the newly created node to the same parent that contained sj1 */ 
			sj1Container.addNodeToContainer(splitjoin);
			
			/** Initialize the newly created splitjoin */
			splitjoin.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
			
			/**	Connect the "source" node to the splitter of the newly created splitjoin.
			 * 	The "source" node is the node that was the source in the edge between this node
			 * 	and the splitter of sj1 */ 
			for (Iterator sourceIter = savedSource.iterator(); sourceIter.hasNext();)
			{
				graphStruct.connect((GEStreamNode)sourceIter.next(), splitjoin.getSplitter());	
			}			
			
			/** Update the hierarchy panel */
			EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
										get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
			ac.actionPerformed(null);		
		}		
			
		/** The wrong number of splitjoins was selected. */
		else
		{
			JOptionPane.showMessageDialog(graphpad, 
										  Translator.getString("Error.Must_Select_Two_SplitJoins"), 
										  Translator.getString("Error"), 
										  JOptionPane.ERROR_MESSAGE );
		
		}
		
		
	}
}
/*
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;

/**
 * Action to delete a GEStreamNode or an edge in the GraphStructure.
 * When a GEContainer node is deleted, all of the elements that the container
 * encapsulates are also deleted.
 * The only GEStreamNode that cannot be deleted from the graph is the Toplevel node.
 *
 * * @author jcarlos
 */
public class EditDelete extends AbstractActionDefault {

	/**
	 * Constructor for EditDelete.
	 * @param graphpad
	 * @param name
	 */
	public EditDelete(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Delete a GEStreamNode or an edge in the GraphStructure. When a Container 
	 * node is deleted, 
	 */
	public void actionPerformed(ActionEvent e) 
	{	
		Object[] cells = getCurrentGraph().getSelectionCells();
		GEStreamNode toplevel = graphpad.getCurrentDocument().getGraphStructure().getTopLevel();
		getCurrentGraph().clearSelection();
		if (cells != null) 
		{	
			for (int i=0; i < cells.length; i++)
			{
				/** Case when we are trying to delete the toplevel node */
				if (cells[i] == toplevel)
				{
					/** Set the place in the array where the toplevel is located to null
					 * 	so that the toplevel is not deleted graphically at the end of this method.*/				
					cells[i] = null;
				}
				/** Cannot delete a GESplitter or GEJoiner if the encapsulating node of these nodes is 
				 * 	a GESplitJoin or a GEFeedbackLoop */
				else if ((cells[i] instanceof GESplitter) || (cells[i] instanceof GEJoiner))
				{
					GEStreamNode node = (GEStreamNode)cells[i];
					if ((node.getEncapsulatingNode().getType() == GEType.SPLIT_JOIN) || 
						(node.getEncapsulatingNode().getType() == GEType.FEEDBACK_LOOP))
						{
			
							JOptionPane.showMessageDialog(graphpad, 
									 					  Translator.getString("Error.Cannot_delete_splitter_joiner"), 
														  Translator.getString("Error"), 
														  JOptionPane.ERROR_MESSAGE );
							cells[i] = null;
							continue;
						}
							
				}
				/** Case when we are deleting a GEStreamNode */
				if (cells[i] instanceof GEStreamNode)
				{
					GEStreamNode node = (GEStreamNode) cells[i];
					node.deleteNode(getCurrentGraph());
				}
				
				
				/** Case when we are deleting a DefaultEdge */
				else if ((cells[i] instanceof DefaultEdge) && (cells[i] !=null))
				{
					DefaultEdge edge = (DefaultEdge) cells[i];
					
					/** Get the source of the edge so that we can remove that element */
					if (edge.getSource() != null)
					{
						GEStreamNode sourceNode = (GEStreamNode) ((DefaultPort)edge.getSource()).getParent();
						if (sourceNode != null)
						{
							sourceNode.getSourceEdges().remove(edge);						
						}
					}
					/** Get the target of the edge so that we can remove that element */
					if (edge.getTarget() != null)
					{					
						GEStreamNode targetNode = (GEStreamNode) ((DefaultPort)edge.getTarget()).getParent();
						if (targetNode != null)
						{
							targetNode.getTargetEdges().remove(edge);
						}
					}
				}
			}
			/** Must delete the ports of the cell */
			cells = DefaultGraphModel.getDescendants(getCurrentGraph().getModel(), cells)
									.toArray();
			getCurrentGraph().getModel().remove(cells);
			
			graphpad.getCurrentDocument().getTreePanel().update();
			graphpad.getCurrentDocument().updateUI();
			graphpad.update();	
		}
	}
}
	/*
	public void actionPerformed(ActionEvent e) {
		if (getCurrentDocument().getLibraryPanel().hasFocus()) {
				getCurrentDocument().getLibraryPanel().delete();
*/

/*
 * @(#)EditDelete.java	1.2 30.01.2003
 *
 * Copyright (C) 2003 sven.luzar
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * 
 * @author jcarlos
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
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	
	
	public void actionPerformed(ActionEvent e) 
	{	
		Object[] cells = getCurrentGraph().getSelectionCells();
		getCurrentGraph().clearSelection();
		if (cells != null) 
		{	
			for (int i=0; i < cells.length; i++)
			{
				/** Case when we are deleting a GEStreamNode */
				if (cells[i] instanceof GEStreamNode)
				{
					GEStreamNode node = (GEStreamNode) cells[i];
					GEStreamNode parent = node.getEncapsulatingNode();
					if (parent != null)
					{
						((GEContainer) parent).removeNodeFromContainer(node);
					}
					
					getCurrentGraph().getModel().remove(node.getSourceEdges().toArray());
					getCurrentGraph().getModel().remove(node.getTargetEdges().toArray());;
					
					/** special case when the node is a GEContainer */
					if (node instanceof GEContainer)
					{
						
						GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure();
						ArrayList innerNodesList = new ArrayList();
						if (!(graphStruct.containerNodes.removeContainer(node, innerNodesList))) 
						{
							//TODO: Add warning popup for when it was not possible to delete the node
							System.err.println("UNABLE TO DELETE THE CONTAINER NODE");
						}
						//Object[] containedCells = ((GEContainer)node).getContainedElements().toArray();
						Object[] containedCells = innerNodesList.toArray();
						
						for (int j = 0; j < containedCells.length; j++)
						{
							getCurrentGraph().getModel().remove(((GEStreamNode)containedCells[j]).getSourceEdges().toArray());
							getCurrentGraph().getModel().remove(((GEStreamNode)containedCells[j]).getTargetEdges().toArray());;							
						}
						
						
						containedCells = DefaultGraphModel.getDescendants(getCurrentGraph().getModel(), containedCells)
															.toArray();
									getCurrentGraph().getModel().remove(containedCells);			
						
					}
				}
				/** Case when we are deleting a DefaultEdge */
				if ((cells[i] instanceof DefaultEdge) && (cells[i] !=null))
				{
					DefaultEdge edge = (DefaultEdge) cells[i];
					
					if (edge.getSource() != null)
					{
						GEStreamNode sourceNode = (GEStreamNode) ((DefaultPort)edge.getSource()).getParent();
						if (sourceNode != null)
						{
							sourceNode.getSourceEdges().remove(edge);						
						}
					}
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
		}
	
	}
	
	/*
	public void actionPerformed(ActionEvent e) {

		if (getCurrentDocument().getLibraryPanel().hasFocus()) {
			int r =
				JOptionPane.showConfirmDialog(
					null,
					Translator.getString("DeleteFromLibMessage"),
					Translator.getString("DeleteFromLibTitle"),
					JOptionPane.YES_NO_OPTION);
			if (r == JOptionPane.YES_OPTION)
				getCurrentDocument().getLibraryPanel().delete();
		} else {
			Object[] cells = getCurrentGraph().getSelectionCells();
			if (cells != null) {
				cells =
					DefaultGraphModel
						.getDescendants(getCurrentGraph().getModel(), cells)
						.toArray();
			
				getCurrentGraph().getModel().remove(cells);
			}
		}
	}*/

}

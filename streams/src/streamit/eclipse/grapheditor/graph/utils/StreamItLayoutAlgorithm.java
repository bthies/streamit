/*
 * Created on Jan 17, 2004
 */
package streamit.eclipse.grapheditor.graph.utils;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;

import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * The StreamItLayoutAlgorithm lays out the components that make out the graph according
 * to the layout specified by each of the elements. Each container will be responsible for
 * the layout of its children once it has calculated its dimension. 
 * 
 * @author jcarlos
 */
public class StreamItLayoutAlgorithm {


	public void perform(GraphStructure graphStruct)
	{
				
		/**
		 * Calculate the dimensions for all of the nodes. Must start at the deepest 
		 * level and work all the way back to the toplevel node.
		 *  By calculating the dimension, we can determine how we are going to layout 
		 * 	each of these containers and its components. */
		for (int i = graphStruct.containerNodes.getMaxLevelView(); i >=0; i--)
		{
			Iterator containerIterator = graphStruct.containerNodes.getContainersAtLevel(i).iterator();
			while (containerIterator.hasNext())
			{
				GEContainer node = (GEContainer) containerIterator.next();
				node.calculateDimension();			
			}
		}
		
		/**
		 * Find all the nodes whose container is expanded. This will be used to hide these 
		 * nodes while we are doing the layout and then add them once we are done laying out. */		
		ArrayList nodeList = new ArrayList();
		for (Iterator nodeIter = graphStruct.allNonContainerNodesInGraph().iterator(); nodeIter.hasNext();)
		{
			GEStreamNode node = (GEStreamNode)nodeIter.next();
			if (node.getEncapsulatingNode().isExpanded())
			{
				nodeList.add(node);
			}
		}
			
		/** Hide all the nodes whose container is expanded (to avoid seeing how the layout occurs on 
		 * 	the screen node by node)*/	
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList.toArray(), false);
		
		/** The layout of the components must begin at the toplevel node. The laying out follows
		 * a depth-first pattern. */		
		graphStruct.getTopLevel().layoutChildren();
		
			
		/** Make visible all the nodes that were hidden before the layout was carried out */
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList.toArray(), true);
	
		/** Edit the changes in the graph model */
		graphStruct.getJGraph().getModel().edit(graphStruct.getAttributes(),null,null,null);

	}
}

/*
 * Created on Apr 11, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphModel;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GEStreamNode;

/**
 * Action that disconnects two nodes. 
 * @author jcarlos
 *
 */
public class EditDisconnect extends AbstractActionDefault {


	/**
	 * Constructor for EditDisconnect.
	 * @param graphpad
	 * @param name
	 */
	public EditDisconnect(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * In order to perform the action, one must call the disconnect method.
	 */
	public void actionPerformed(ActionEvent e) {}
	
	/**
	 * Disconnect two nodes.
	 * @param source GEStreamNode
	 * @param target GEStreamNode
	 */
	public void disconnect(GEStreamNode source, GEStreamNode target)
	{
		ArrayList sourceEdges =  source.getSourceEdges();
		ArrayList targetEdges =  target.getTargetEdges();
		ArrayList edgeList = new ArrayList();
		
		/** Search for the edges that connect both the source and the target nodes */
		for (Iterator sourceIter = source.getSourceEdges().iterator(); sourceIter.hasNext();)
		{
			DefaultEdge sEdge = (DefaultEdge)sourceIter.next();
			for (Iterator targetIter = target.getTargetEdges().iterator(); targetIter.hasNext();)
			{
				if ((DefaultEdge)targetIter.next() == sEdge)
				{
					edgeList.add(sEdge);
				}
			}
		}
		
		/** Must remove the edges that source and target have connecting them from their list of nodes*/
		for (Iterator edgeIter = edgeList.iterator(); edgeIter.hasNext();)
		{
			DefaultEdge edge =  (DefaultEdge)edgeIter.next();
			sourceEdges.remove(edge);
			targetEdges.remove(edge);
		}
		
		/** Graphically remove the edges */
		GraphModel model = getCurrentGraph().getModel();
		model.remove(DefaultGraphModel.getDescendants(model, edgeList.toArray()).toArray());
		
	}


}

/*
 * Created on Jul 31, 2003
 */
package streamit.eclipse.grapheditor.graph.utils;

import org.jgraph.JGraph;
import org.jgraph.layout.LayoutAlgorithm;
import org.jgraph.layout.LayoutController;
import org.jgraph.layout.SugiyamaLayoutAlgorithm;
import org.jgraph.layout.SugiyamaLayoutController;

import streamit.eclipse.grapheditor.graph.GraphStructure;



/**
 * JGraphLayoutManager performs the layout of a JGraph according to a LayoutAlgorithm and 
 * LayoutController specified by the user. If these arguments are not specified, then the 
 * JGraphLayoutManager will use the SugiyamaLayoutAlgorithm and the SugiyamaLayoutController 
 * as defaults.
 * @author jcarlos
 *
 */
public class JGraphLayoutManager {
	private GraphStructure graphStruct;
	private LayoutAlgorithm algorithm;
	private LayoutController controller;

	/**
	 * JGraphLayoutManager constructor that will use the SugiyamaLayoutAlgorithm 
	 * and the SugiyamaLayoutController as defaults.
	 * @param jgraph The JGraph structure to be laid out.
	 */


	public JGraphLayoutManager(GraphStructure graphStruct)
	{
		this.graphStruct = graphStruct;
		this.algorithm = new SugiyamaLayoutAlgorithm();
		this.controller = new SugiyamaLayoutController();
	}
	
	/**
	 * JGraphLayoutManager constructor that specifies the LayoutAlgorithm and the 
	 * LayoutController to be used. 
	 * @param jgraph The JGraph structure to be laid out.
	 * @param algorithm The LayoutAlgorithm that will be used to do the layout.
	 * @param controller The LayoutController that specifies the properties of the LayoutAlgorithm.
	 */
	 

	public JGraphLayoutManager(GraphStructure graphStruct, LayoutAlgorithm algorithm, LayoutController controller)
	{
		this.graphStruct = graphStruct;
		this.algorithm = algorithm;
		this.controller = controller;
	}

	/**
	 * Perform the layout of the JGraph accoring to the specified LayoutAlgorithm
	 * and LayoutController.
	 */

	public void arrange()
	{
		StreamItLayoutAlgorithm sla = new StreamItLayoutAlgorithm();
		sla.perform(this.graphStruct);
	}


}

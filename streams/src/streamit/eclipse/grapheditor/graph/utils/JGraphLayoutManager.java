/*
 * Created on Jul 31, 2003
 */
package streamit.eclipse.grapheditor.graph.utils;

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

	/**
	 * JGraphLayoutManager constructor that will use the SugiyamaLayoutAlgorithm 
	 * and the SugiyamaLayoutController as defaults.
	 * @param jgraph The JGraph structure to be laid out.
	 */


	public JGraphLayoutManager(GraphStructure graphStruct)
	{
		this.graphStruct = graphStruct;
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

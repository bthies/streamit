/*
 * Created on Jul 31, 2003
 */
package grapheditor.jgraphextension;

import com.jgraph.JGraph;
import com.jgraph.layout.*;

/**
 * JGraphLayoutManager performs the layout of a JGraph according to a LayoutAlgorithm and 
 * LayoutController specified by the user. If these arguments are not specified, then the 
 * JGraphLayoutManager will use the SugiyamaLayoutAlgorithm and the SugiyamaLayoutController 
 * as defaults.
 * @author jcarlos
 *
 */
public class JGraphLayoutManager {
	
	private JGraph jgraph;
	private LayoutAlgorithm algorithm;
	private LayoutController controller;

	/**
	 * JGraphLayoutManager constructor that will use the SugiyamaLayoutAlgorithm 
	 * and the SugiyamaLayoutController as defaults.
	 * @param jgraph The JGraph structure to be laid out.
	 */
	public JGraphLayoutManager(JGraph jgraph)
	{
		this.jgraph = jgraph;
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
	public JGraphLayoutManager(JGraph jgraph, LayoutAlgorithm algorithm, LayoutController controller)
	{
		this.jgraph = jgraph;
		this.algorithm = algorithm;
		this.controller = controller;
	}

	/**
	 * Perform the layout of the JGraph accoring to the specified LayoutAlgorithm
	 * and LayoutController.
	 */
	public void arrange()
	{
		algorithm.perform(this.jgraph, true, this.controller.getConfiguration());
	}

	
	public void setFrameSize(LiveJGraphInternalFrame frame)
	{
		
	}
}

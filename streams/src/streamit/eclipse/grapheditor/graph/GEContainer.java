/*
 * Created on Feb 19, 2004
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 * @author jcarlos
 */
public class GEContainer extends GEStreamNode implements GEContainerInterface{

	/**
	 * The sub-graph structure that is contained within this GEFeedbackLoop
	 * This subgraph is hidden when the GEFeedbackLoop is collapse and 
	 * visible when expanded. 
	 */
	protected GraphStructure localGraphStruct = null;

	/**
	 * The succesors of the GEContainer. 
	 */
	protected ArrayList succesors;

	/**
	 * The first node in the GEContainer. 
	 */
	protected GEStreamNode firstNode = null;

	/**
	 * Boolean that specifies if the elements contained by the GEFeedbackLoop are 
	 * displayed (it is expanded) or they are hidden (it is collapsed).
	 */
	protected boolean isExpanded;

	/**
	 * Default constructor
	 * @param type String type of the container.
	 * @param name String name of the container.
	 */
	public GEContainer(String type, String name)
	{
		super(type, name);
		this.succesors = new ArrayList();
	}
		
	/** 
	 * Get true when the GESplitJoin is expanded (contained elements are visible), 
	 * otherwise get false.
	 * @return true if expanded; otherwise, return false.
	 */
	public boolean isExpanded()
	{
		return this.isExpanded;
	}
	
	/**
	 * Get the succesors of the GEStreamNode.
	 * @return An ArrayList with the succesors of the GEStreamNode. 
	 */
	public ArrayList getSuccesors()
	{
		return this.succesors;
	}
	
	/**
	 * Get the first node contained by the GEPipeline. 
	 */
	public GEStreamNode getFirstNodeInContainer()
	{
		return this.firstNode;
	}
	
	/**
	 * Set which node is the first one container by the GEPipeline.
	 */
	public void  setFirstNodeInContainer(GEStreamNode firstNode)
	{
		this.firstNode = firstNode;
	
	}
	
	/**
	 * Hide the GEStreamNode in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible.
	 * @return true if it was possible to hide the node; otherwise, return false.
	 */
	public boolean hide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, false);
		return true;
	}
	
	/**
	 * Make the GEStreamNode visible in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible. 
	 * @return true if it was possible to make the node visible; otherwise, return false.
	 */	
	public boolean unhide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, true);
		return true;
	}	

	public void moveNodePositionInContainer(GEStreamNode startNode, GEStreamNode endNode, int position){};
	
	/**
	 * Add the node to this container only if this container does not container the node already.
	 * @param node GEStreamNode
	 */
	public void addNodeToContainer(GEStreamNode node)
	{
		ArrayList elements = this.getSuccesors();
		if ((elements != null) && ( ! (elements.contains(node))))
		{
			elements.add(node);
		}	
	}
	

	
	
	/**
	 * Get the names of the nodes that are contained in the GEContainer.
	 * @return Object[] of the names of the nodes inside the GEContainer.
	 */
	public Object[] getContainedElementsNames()
	{
		Iterator allContIter = this.getContainedElements().iterator();
		ArrayList names = new ArrayList();
		while(allContIter.hasNext())
		{	
			names.add(((GEStreamNode)allContIter.next()).name);
		}
		return names.toArray();
	}
	
	
	/**
	 * Remove the node from the container.
	 * @param node GEStreamNode
	 */
	public void removeNodeFromContainer(GEStreamNode node)
	{
		ArrayList elements = this.getSuccesors();
		if (elements != null)
		{
			elements.remove(node);
		}
	}

	public void calculateDimension(){};
	public void layoutChildren(){};

	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node (can't have any contained elements), then null is returned.
	 * @return ArrayList of contained elements. If <this> is not a container, return null.
	 */
	public ArrayList getContainedElements(){return null;};
	
	
	/**
	 * Expand or collapse the GEStreamNode structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph The JGraph that will be modified to allow the expanding/collapsing.
	 */	
	public void collapseExpand(){};
	public void collapse(){};
	public void expand(){};
	

	/**
	 * Construct the GEStreamNode. The subclasses must implement this method according to
	 * their specific needs.
	 * @param graphStruct GraphStructure to which GEStreamNode belongs.
	 * @return GEStreamNode 
	 */
	GEStreamNode construct(GraphStructure graphStruct, int level){return null;};


	/**
	 * Set the attributes necessary to display the GEStreamNode.
	 * @param graphStruct GraphStructure
	 * @param bounds Rectangle
	 */
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds){};

	/**
	 * Writes the textual representation of the GEStreamNode using the PrintWriter specified by out. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param out PrintWriter that is used to output the textual representation of the graph.  
	 */
	public void outputCode(PrintWriter out){};

}

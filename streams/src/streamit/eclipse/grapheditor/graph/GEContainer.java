/*
 * Created on Feb 19, 2004
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;

import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;

import streamit.eclipse.grapheditor.graph.utils.JGraphLayoutManager;

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
	 * Get the first node in the container. 
	 * @return GEStreamNode first node in the container.
	 */
	public GEStreamNode getFirstNodeInContainer()
	{
		ArrayList containedElements = this.getContainedElements();
		if (containedElements.size() > 0)
		{
			return (GEStreamNode) containedElements.get(0);
		}
		return null;
	}
	
	public GEStreamNode getFirstNonContainerNodeInContainer()
	{
		
		GEStreamNode node = getFirstNodeInContainer();
		while ((node != null) && (node instanceof GEContainer))
		{
			node = ((GEContainer)node).getFirstNodeInContainer();
		}
		return node;
		
	}
	
	
	/**
	 * Get the last node in the container. 
	 * @return GEStreamNode last node in the container. 
	 */
	public GEStreamNode getLastNodeInContainer()
	{
		ArrayList containedElements = this.getContainedElements();
		if (containedElements.size() > 0)
		{
			return (GEStreamNode) containedElements.get(containedElements.size() - 1);
		}
		return null;
		
	}
	
	
	public GEStreamNode getLastNonContainerNodeInContainer()
	{
		
		GEStreamNode node = getLastNodeInContainer();
		while ((node != null) && (node instanceof GEContainer))
		{
			node = ((GEContainer)node).getLastNodeInContainer();
		}
		return node;
		
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
			node.setEncapsulatingNode(this);
		}	
		
	}
	
	/** 
	 * Delete the GEContainer and all of the nodes that it contains.
	 * @param GraphModel model from which the GEContainer and its contained
	 * elements will be deleted. 
	 */
	public void deleteNode(GraphModel model)
	{
		super.deleteNode(model);
	
		ArrayList innerNodesList = new ArrayList();
		if (!(this.localGraphStruct.containerNodes.removeContainer(this, innerNodesList))) 
		{
			//TODO: Add warning popup for when it was not possible to delete the node
			System.err.println("UNABLE TO DELETE THE CONTAINER NODE");
		}
	
		GEStreamNode firstNodeInCont = this.getFirstNodeInContainer();
		GEStreamNode lastNodeInCont = this.getLastNodeInContainer();
		
		if (firstNodeInCont != null)
		{
			firstNodeInCont.deleteNode(model);
		}
		if (lastNodeInCont != null)
		{
			lastNodeInCont.deleteNode(model);
		}
			
		Object[] containedCells = innerNodesList.toArray();
		for (int j = 0; j < containedCells.length; j++)
		{
			model.remove(((GEStreamNode)containedCells[j]).getSourceEdges().toArray());
			model.remove(((GEStreamNode)containedCells[j]).getTargetEdges().toArray());;							
		}
		
		containedCells = DefaultGraphModel.getDescendants(model, containedCells).toArray();
		model.remove(containedCells);	
	}
	
	
	/**
	 * Set the level of the elements contained by this GEContainer to the level 
	 * passed as an argument.
	 * @param level int that represents the new level of the elements contained 
	 * by this GEContainer. 
	 */
	/*
	public void setContainedElementsLevel(int level)
	{
		for (Iterator containedIter = this.getContainedElements().iterator(); containedIter.hasNext();)
		{
			GEStreamNode node = (GEStreamNode) containedIter.next();
			node.setDepthLevel(level);
			if (node instanceof GEContainer)
			{
				//((GEContainer) node).setContainedElementsLevel(level + 1);	
				this.localGraphStruct.containerNodes.moveContainerToLevel(level, this);
			}
		}
	}
	*/
	
	
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


	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node (can't have any contained elements), then null is returned.
	 * @return ArrayList of contained elements. If <this> is not a container, return null.
	 */
	public ArrayList getContainedElements(){return null;};
	
	
	/**
	 * Expand or collapse the GEContainer structure depending on wheter it was already 
	 * collapsed or expanded. 
	 */		
	public void collapseExpand()
	{
		if (isExpanded)
		{
			this.collapse();
				
		}
		else
		{
			this.expand();
		}
	}	

	public GEStreamNode getFirstCollapsedNodeInContainer()
	{
		
		
		GEStreamNode node = getFirstNodeInContainer();
		if (node instanceof GEContainer)
		{
			if (( ! ((GEContainer) node).isExpanded))
			{
				return node;
			}
			else
			{
				((GEContainer) node).getFirstNonContainerNodeInContainer();
			}
		}
		return null;
		
	}
	
	
	public GEStreamNode getLastCollapsedNodeInContainer()
	{
		GEStreamNode node = getLastNodeInContainer();
		if (node instanceof GEContainer)
		{
			if (( ! ((GEContainer) node).isExpanded))
			{
				return node;
			}
			else
			{
				((GEContainer) node).getLastNonContainerNodeInContainer();
			}
		}
		return null;
	}
		/*
		GEStreamNode node = getLastNodeInContainer();
		if (node instanceof GEContainer)
		{
			while ((node != null) && ( ! ((GEContainer) node).isExpanded))
			{
				node = ((GEContainer)node).getFirstNodeInContainer();
				if (!(node instanceof GEContainer))
				{
					return null;
				}

			}
		}
		return null;*/


	/**
	 * Collapse the GEContainer. When the GEContainer is collapsed all of the 
	 * elements that it contains disappear. Edge connnections no longer happen
	 * at the first and last elements of the GEContainer. Now the GEContainer will
	 * be the one that is connected instead.
	 */
	public void collapse()
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, true);
		
		GEStreamNode firstNode = null;
		if (this.getFirstCollapsedNodeInContainer() != null)
		{
			firstNode = this.getFirstCollapsedNodeInContainer();
		}
		else
		{
			firstNode = this.getFirstNonContainerNodeInContainer();
		}

		GEStreamNode lastNode = null;
		if (this.getLastCollapsedNodeInContainer() != null)
		{
			lastNode = this.getLastCollapsedNodeInContainer();
		}
		else
		{
			lastNode = this.getLastNonContainerNodeInContainer();
		}
			
		
		 

		Iterator lastEdgeIter = localGraphStruct.getGraphModel().edges(lastNode.getPort());
		Iterator firstEdgeIter = localGraphStruct.getGraphModel().edges(firstNode.getPort());
		
		ArrayList edgesToRemove =  new ArrayList();
	
		while (firstEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) firstEdgeIter.next();
			Iterator sourceIter = firstNode.getTargetEdges().iterator();
			while(sourceIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) sourceIter.next();
				if(target.equals(edge))
				{
					//System.out.println(" The container of the edge is " + ((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode());
					if (!(this.equals(((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode())))
					{
						cs.disconnect(edge, false);
						cs.connect(edge, this.getPort(), false);
						this.addTargetEdge(edge);
						edgesToRemove.add(edge);
					}
				}
			}
		}
		while (lastEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) lastEdgeIter.next();
			Iterator targetIter = lastNode.getSourceEdges().iterator();
			while(targetIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) targetIter.next();
				if (target.equals(edge))
				{
					//System.out.println(" The container of the edge is " + ((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode());
					if (!(this.equals(((GEStreamNode) ((DefaultPort)edge.getTarget()).getParent()).getEncapsulatingNode())))
					{
						cs.disconnect(edge,true);
						cs.connect(edge, this.getPort(),true);
						this.addSourceEdge(edge);
						edgesToRemove.add(edge);
					}
				}
			}
		}	
		Object[] removeArray = edgesToRemove.toArray();
		for(int i = 0; i<removeArray.length;i++)
		{
			lastNode.removeSourceEdge((DefaultEdge)removeArray[i]);
			firstNode.removeTargetEdge((DefaultEdge)removeArray[i]);
			firstNode.removeSourceEdge((DefaultEdge)removeArray[i]);
			firstNode.removeTargetEdge((DefaultEdge)removeArray[i]);

		}

		GraphConstants.setAutoSize(this.attributes, true);			
		this.localGraphStruct.getGraphModel().edit(localGraphStruct.getAttributes(), cs, null, null);
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList, false);
	
		this.isExpanded = false;
		for (int i = level - 1; i >= 0; i--)
		{
			this.localGraphStruct.containerNodes.hideContainersAtLevel(i);
		}	
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();
	}
	
	/**
	 * Expand the GEContainer. When the GEContainer is expanded, all of the 
	 * nodes that it contains are now visible. Edge connections now occur at
	 * the first and last nodes in the GEContainer. 
	 */
	public void expand()
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList, true);
		
//		GEStreamNode firstNode = this.getFirstNonContainerNodeInContainer(); 
//		GEStreamNode lastNode = this.getLastNonContainerNodeInContainer();

		GEStreamNode firstNode = this.getFirstNodeInContainer();
		GEStreamNode lastNode = this.getLastNodeInContainer();
		
		Iterator eIter = localGraphStruct.getGraphModel().edges(this.getPort());
		ArrayList edgesToRemove =  new ArrayList();
		
		while (eIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) eIter.next();
			Iterator sourceIter = this.getSourceEdges().iterator();	
			while (sourceIter.hasNext())
			{
				DefaultEdge s = (DefaultEdge) sourceIter.next();
				if (s.equals(edge))
				{
					cs.disconnect(edge, true);
					cs.connect(edge, lastNode.getPort(), true);		
					lastNode.addSourceEdge(s);
					edgesToRemove.add(s);
				}
			}
			
			Iterator targetIter = this.getTargetEdges().iterator();
			while(targetIter.hasNext())
			{
				DefaultEdge t = (DefaultEdge) targetIter.next();
				if(t.equals(edge))
				{
						cs.disconnect(edge,false);
						cs.connect(edge, firstNode.getPort(),false);
						firstNode.addTargetEdge(t);
						edgesToRemove.add(t);
				}
			}
			
			Object[] removeArray = edgesToRemove.toArray();
			for(int i = 0; i<removeArray.length;i++)
			{
				this.removeSourceEdge((DefaultEdge)removeArray[i]);
				this.removeTargetEdge((DefaultEdge)removeArray[i]);
			}	
		}

		this.localGraphStruct.getGraphModel().edit(null, cs, null, null);	
		this.isExpanded = true;	
		for (int i = level; i >= 0; i--)
		{
			this.localGraphStruct.containerNodes.hideContainersAtLevel(i);
		}
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();	
		
	}

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
	 * Writes the textual representation of the GEStreamNode to the StringBuffer. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param strBuff StringBuffer that is used to output the textual representation of the graph.  
	 */
	public void outputCode(StringBuffer strBuff){};


	public void calculateDimension(){};
	public void layoutChildren(){};

}

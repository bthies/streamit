/*
 * Created on Feb 19, 2004
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.jgraph.JGraph;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;

/**
 * GEContainer describes a container object. A container object can contain encapsulates
 * other nodes within it. A container can be expanded or collapsed in order to either 
 * unhide or hide the elements that it contains. 
 * 
 * @author jcarlos
 */
public class GEContainer extends GEStreamNode implements GEContainerInterface{

	/**
	 * The GraphStructure that the GEContainer belongs to. 
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
	 * The dimension of the graphical representation of the GEStreamNode.
	 */
	protected Dimension nodeDimension;


	/**
	 * The location of the GEContainer. 
	 */
	protected Point containerLocation;


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
	 * Add the collection of nodes to this container only if this container already 
	 * contain any of the elements.
	 * @param nodes Collection of nodes to be added to this Container
	 */
	public void addNodesToContainer(Collection nodes, ContainerNodes containerNodes, IFile ifile)
	{
		ArrayList elements = this.getSuccesors();
		if ((elements != null) && ( ! elements.containsAll(nodes)))
		{
			elements.addAll(nodes);
			int depthLevel = this.getDepthLevel() + 1;
			for (Iterator nodeIter = nodes.iterator(); nodeIter.hasNext();)
			{
				GEStreamNode node = ((GEStreamNode) nodeIter.next()); 
				node.setEncapsulatingNode(this);
				
				if (node instanceof GEContainer)
				{	
					containerNodes.addContainerToLevel(depthLevel, node);	
					
				}
				IFileMappings.addIFileMappings(node, ifile);
			}
		}	
	}
	
	/**
	 * Remove the node from the container (if it is contained by the container).
	 * @param node GEStreamNode
	 */
	public void removeNodeFromContainer(GEStreamNode node)
	{
		//ArrayList elements = this.getSuccesors();
		ArrayList elements = this.getContainedElements();
		if (elements != null)
		{
			elements.remove(node);
		}
	}
	
	/** 
	 * Delete this GEContainer and all of the nodes that it contains. The edges
	 * that are connected to the container will alsoe be deleted. 
	 * @param GraphModel model from which the GEContainer and its contained
	 * elements will be deleted. 
	 */
	public void deleteNode(JGraph jgraph)
	{
		/** Delete this container and the edges connected to it */
		super.deleteNode(jgraph);
		GraphModel model = jgraph.getModel();
	
		/** Remove the container node from the list of container nodes.
		 *  innerNodeList will contain the all the elements that the container has. */
		ArrayList innerNodesList = new ArrayList();
		if (!(this.localGraphStruct.containerNodes.removeContainer(this, innerNodesList))) 
		{
			//TODO: Add warning popup for when it was not possible to delete the node
			System.err.println("UNABLE TO DELETE THE CONTAINER NODE");
		}
	
		/** Delete the first node and the last node in the container. This will also delete
		 *  the edges that are connected to these nodes */		
		GEStreamNode firstNodeInCont = this.getFirstNodeInContainer();
		GEStreamNode lastNodeInCont = this.getLastNodeInContainer();
		if (firstNodeInCont != null)
		{
			firstNodeInCont.deleteNode(jgraph);
		}
		if (lastNodeInCont != null)
		{
			lastNodeInCont.deleteNode(jgraph);
		}
			
		/** Remove the target/source edges from the model (for all cells in the container)*/
		Object[] containedCells = innerNodesList.toArray();
		for (int j = 0; j < containedCells.length; j++)
		{
			model.remove(((GEStreamNode)containedCells[j]).getSourceEdges().toArray());
			model.remove(((GEStreamNode)containedCells[j]).getTargetEdges().toArray());;							
		}
		
		/** Removes the ports of all the contained cells */
		containedCells = DefaultGraphModel.getDescendants(model, containedCells).toArray();
		model.remove(containedCells);	
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
	
	/**
	 * Get the first node in this container that is not a container.
	 * @return GEStreamNode that is the first non-container node in the container. 
	 */
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
	 * Check wheter or not the GEContainer is valid or not.
	 * @return
	 */
	public int checkValidity(){return 0;}
	
	
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
	
	/** 
	 * Get the last node in the container that is not a container.
	 * @return GEStreamNode that is the last non-container node in the container. 
	 */
	public GEStreamNode getLastNonContainerNodeInContainer()
	{
		
		GEStreamNode node = getLastNodeInContainer();
		while ((node != null) && (node instanceof GEContainer))
		{
			node = ((GEContainer)node).getLastNodeInContainer();
		}
		return node;
		
	}
	
	public GEStreamNode getLastConnectedNode()
	{
		boolean first = true;
		GEStreamNode firstUnconnected = null;
		for (Iterator containedIter = this.getContainedElements().iterator(); containedIter.hasNext();)
		{
			GEStreamNode node = (GEStreamNode) containedIter.next();
			if (node.getTargetNodes().size() != 0)
			{
				if( ! (this.hasAncestorInContainer(node)))
				{
					return node;
				}
			}
			else 
			{
				if (first)
				{
					firstUnconnected = node;	
					first = false;				
				}
			}
		}
		return firstUnconnected;
		
	}
	
	/** 
	 * Get the first node in the container that is collapsed.
	 * @return GEStreamNode that is the first node in the container that is collapsed.
	 */
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
	
	/**
	 * Get the last node in the container that is collapsed.
	 * @return GEStreamNode that is the last node in the container that is collapsed.
	 */
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
	
	/**
	 * Get the location of the GEContainer.
	 * @return Point location of the GEContainer.
	 */
	public Point getLocation()
	{		
		return this.containerLocation;
	}
	
	/**
	 * Set the location of the GEContainer.
	 * @param loc Point The location of the GEContainer.
	 */
	public void setLocation(Point loc)
	{
		this.containerLocation = loc;
	}
	
	
	
	/**
	 * Hide the container (make it invisible).
	 * @return true if it was possible to hide the node; otherwise, return false.
	 */
	public boolean hide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, false);
		return true;
	}
	
	/**
	 * Unhide the container (make it visible).
	 * @return true if it was possible to make the node visible; otherwise, return false.
	 */	
	public boolean unhide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, true);
		return true;
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
	 * Get the index of the node inside the GEContainer.
	 * @return int index of the node within the GEContainer; return -1 if
	 * the container does not contain the node.
	 */
	public int indexOfNode(GEStreamNode node)
	{
		return this.getSuccesors().indexOf(node);
	}



	/** 
	 * Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node (can't have any contained elements), then null is returned.
	 * 
	 * Note: This must be overriden by a subclass of GEContainer. 
	 * @return ArrayList of contained elements. If this is not a container, return null.
	 */
	public ArrayList getContainedElements(){return null;};
	
	/**
	 * Get the dimension of the GEContainer.
	 * @return Dimension of the GEContainer.
	 */
	public Dimension getDimension()
	{
		if (this.isExpanded)
		{
			return this.nodeDimension;
		}
		else
		{
			Dimension dim = GraphConstants.getBounds(GraphConstants.cloneMap((this.attributes))).getSize();
			System.out.println("Container " + this.getName()+ " not expanded, returning SIZE = " + GraphConstants.getSize(GraphConstants.cloneMap((this.attributes))));
			System.out.println("Container not expanded, returning DIMENSION =  " + dim);
			//return dim;
			return Constants.DEFAULT_DIMENSION;
			
		}
	}
	
	/**
	 * Set the dimension of the GEContainer.
	 * @param dim Dimension
	 */
	public void setDimension(Dimension dim)
	{
		this.nodeDimension = dim;
	}
	
	/**
	 * Get the ancestor of the node passed as a parameter that is contained by this GEContainer.
	 * If the container does not contain the ancestor for the GEContainer, then return null
	 * @param node GEStreamNode
	 * @return GEContainer that is the ancestor of the node that is contained by the container.
	 * null if the container does not contain the ancestor of the node.
	 */
	public GEContainer getAncestorInContainer(GEStreamNode node)
	{
		ArrayList succList = this.getSuccesors();
		GEContainer toplevel = this.localGraphStruct.getTopLevel();
		while (node != toplevel)
		{
			if (succList.contains(node.getEncapsulatingNode()))
			{
				return node.getEncapsulatingNode();
			}
			node = node.getEncapsulatingNode();
		}		
		return null;
	}
	
	public boolean hasAncestorInContainer(GEStreamNode node)
	{
		ArrayList succList = this.getSuccesors();
		GEContainer toplevel = this.localGraphStruct.getTopLevel();
		while (node != toplevel)
		{
			if (succList.contains(node))
			{
				return true;
			}
			node = node.getEncapsulatingNode();
		}		
		return false;
	}
	



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
		
		/** Set the container visible */
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, true);
			
		/** Get the first node in the container. The edge connections for this 
		 *  node need to be modified (so that the edge is now connected to the 
		 * 	container instead of being connected to this first node) 
		 * 	The first container will either be a first collapsed container or the first 
		 *  non-container node in the container */ 
		GEStreamNode firstNode = null;
		if (this.getFirstCollapsedNodeInContainer() != null)
		{
			firstNode = this.getFirstCollapsedNodeInContainer();
		}
		else
		{
			firstNode = this.getFirstNonContainerNodeInContainer();
		}

		/** Get the last node in the container. The edge connections for this node 
		 * 	need to be modified (so that the edge is now connected to the 
		 * 	container instead of being connected to this last node) 
		 * 	The first container will either be the last collapsed container or the last 
		 *  non-container node in the container */ 
		GEStreamNode lastNode = null;
		if (this.getLastCollapsedNodeInContainer() != null)
		{
			lastNode = this.getLastCollapsedNodeInContainer();
		}
		else
		{
			lastNode = this.getLastNonContainerNodeInContainer();
		}
			
		/** Get the collection of edges for the first and the last node. These are the 
		 * edge connections that need to be changed */
		Iterator lastEdgeIter = localGraphStruct.getGraphModel().edges(lastNode.getPort());
		Iterator firstEdgeIter = localGraphStruct.getGraphModel().edges(firstNode.getPort());
		
		
		ArrayList edgesToRemove =  new ArrayList();
		while (firstEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) firstEdgeIter.next();
			
			/** Go through the target edges for the first node in the container*/
			Iterator targetIter = firstNode.getTargetEdges().iterator();
			while(targetIter.hasNext())
			{
				/** if the target edge is equal to edge, then edge is a target edge for
				 *  the first node */
				DefaultEdge target = (DefaultEdge) targetIter.next();
				if(target.equals(edge))
				{
					//System.out.println(" The container of the edge is " + ((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode());
					
					if (!(this.equals(((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode())))
					{
						/** Disconnect the edge from the first node */
						cs.disconnect(edge, false);
						/** Connect the edge to the container */
						cs.connect(edge, this.getPort(), false);
						/** Add the edge to the edge target list */
						this.addTargetEdge(edge);
						edgesToRemove.add(edge);
					}
				}
			}
		}
		while (lastEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) lastEdgeIter.next();
			Iterator sourceIter = lastNode.getSourceEdges().iterator();
			while(sourceIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) sourceIter.next();
				if (target.equals(edge))
				{
					//System.out.println(" The container of the edge is " + ((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode());
					if (!(this.equals(((GEStreamNode) ((DefaultPort)edge.getTarget()).getParent()).getEncapsulatingNode())))
					{
						/** Disconnect the edge from the last node */
						cs.disconnect(edge,true);
						/** Connect the edge to the container */
						cs.connect(edge, this.getPort(),true);
						/** Add the edge to the edge source list */
						this.addSourceEdge(edge);
						edgesToRemove.add(edge);
					}
				}
			}
		}	
		
		/** Remove the target/source edges from the corresponding list in the first/last node */
		Object[] removeArray = edgesToRemove.toArray();
		for(int i = 0; i<removeArray.length;i++)
		{
			lastNode.removeSourceEdge((DefaultEdge)removeArray[i]);
			firstNode.removeTargetEdge((DefaultEdge)removeArray[i]);
			firstNode.removeSourceEdge((DefaultEdge)removeArray[i]);
			firstNode.removeTargetEdge((DefaultEdge)removeArray[i]);

		}

		/** Make the size of the container be set automatically */
		GraphConstants.setSize(this.attributes, Constants.DEFAULT_DIMENSION);
		GraphConstants.setMoveable(this.attributes, true);
		//GraphConstants.setAutoSize(this.attributes, true);
		
		/** Make the elements contained by the GEContainer not visible */
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList, false);			
		
		/** Edit the corresponding changes */
		this.localGraphStruct.getGraphModel().edit(localGraphStruct.getAttributes(), cs, null, null);
		
	
		this.isExpanded = false;
		
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
		GraphConstants.setMoveable(this.attributes, false);
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(this, false);
		this.localGraphStruct.getGraphModel().edit(localGraphStruct.getAttributes(), cs, null, null);	
		this.isExpanded = true;	
	}


	public GEStreamNode findDeepestCommonContainer(GEStreamNode node)
	{
		GEStreamNode cont = this;
		
		while (cont != null)
		{
			GEStreamNode nodeCont = node;
			while  (nodeCont != null)
			{
				if (nodeCont == cont)
				{
					return nodeCont; 
				}
				nodeCont = nodeCont.getEncapsulatingNode();
			}
			cont = cont.getEncapsulatingNode();
		}
		return null;
	}


	/**
	 * Construct the GEStreamNode. The subclasses must implement this method according to
	 * their specific needs.
	 * @param graphStruct GraphStructure to which GEStreamNode belongs.
	 * @return GEStreamNode 
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int level){return null;};


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
	 * @param nameList List of the names of the nodes that have already been added to the template code.  
	 */
	public void outputCode(StringBuffer strBuff, ArrayList nameList){};

	


	/**
	 * Determine which GEStreamNodes with the same name are adjacent to each other in this GEContainer. An ArrayList of 
	 * Points will be returned. There will be a Point for each sequence of nodes that is greater that a certain threshold. 
	 * For each Point pt returned in the list, pt.x is the first ocurrence of a sequence of adjacent nodes with the same name. 
	 * The number of occurrences is determined by pt.y.
	 * @return ArrayList of Points (For each Point pt, pt.x is the first ocurrence of a node in an adjacent sequence and pt.y
	 * is the number of such nodes with the same name).
	 */
	protected ArrayList searchAdjacentNodes()
	{
		GEStreamNode prevNode = null;
		ArrayList adjacentList = new ArrayList();
		
		int firstInstance = 0;
		int numberAdjacent = 1;
		
		/** Go through all of the elements in this GEContainer */
		for (Iterator succIter = this.succesors.iterator(); succIter.hasNext();)
		{
			GEStreamNode node = (GEStreamNode) succIter.next();
			
			if (prevNode != null)
			{
				/** If the previous node's name equals the current one, then we increase
				 *  the current count of adjacent nodes */
				if (prevNode.getName().equals(node.getName()))
				{
					numberAdjacent++;
				}
				/** The previous and current node's name are not equal ... */
				else
				{
					/** If we have gone over the threshold, then we add the sequence info to the list */ 
					if (numberAdjacent >= Constants.DEFAULT_ADJACENT)
					{
						adjacentList.add(new Point(firstInstance, numberAdjacent));
					}
					/** Reset the values to continue the search for more sequences */
					firstInstance = this.succesors.indexOf(node);
					numberAdjacent = 1;
				}
			}
			prevNode = node;
			
		}
		/** Handle the case when there was a running sequence at the end of the succesors in the container */
		if (numberAdjacent >= Constants.DEFAULT_ADJACENT)
		{
			adjacentList.add(new Point(firstInstance, numberAdjacent));
		}
		return adjacentList;
	}
	
	/**
	 * Print the "for" loop code for the sequence of adjacent nodes represented by the argument pt.
	 * @param strBuff StringBuffer where the information will be printed.
	 * @param nodeName String name corresponding to the nodes in the sequence.
	 * @param pt Point that contains the information to create the loop (pt.x is the index where
	 * the sequence begins and pt.y is the number of elements in the sequence).
	 */
	protected void printForLoopCode(StringBuffer strBuff, String nodeName, Point pt)
	{
		strBuff.append(Constants.TAB+ "for (int i = 0; i < " + pt.y +" ; i++ {" + Constants.newLine);
		strBuff.append(Constants.TAB + Constants.TAB + "add " + nodeName + "();" + Constants.newLine);
		strBuff.append(Constants.TAB + "}" + Constants.newLine);
		
	}

	/**
	 * Connect startNode to endNode if it is a valid connection.
	 * @param startNode 
	 * @param endNode
	 * @param nodesConnected Determines which of the nodes are already connected (present)
	 * in the graph.
	 * @return 0 if it was possible to connect the nodes, otherwise, return a negative integer.
	 */
	public int connect(GEStreamNode startNode, GEStreamNode endNode)
	{
		GEContainer startParent = this;
		GEContainer endParent = endNode.getEncapsulatingNode();

		/** Do not connect if we are connecting the node to itself or if two nodes were not specified */
		if ((startParent == endNode) || (endParent == startNode) || (startParent == null) || (endParent == null))
		{
			return ErrorCode.CODE_CONNECT_TO_SELF;
		}
		
		/** Enforce the correct amount of edges coming in/out of a node. 
		 * GEPhasedFilter - source/target at most one connection.
		 * GESplitter - at most one target edge. GEJoiner - at most one source edge*/
		if ((!(startNode.getType() == GEType.SPLITTER)) && (startNode.getSourceEdges().size() == 1))
		{
			return ErrorCode.CODE_EDGES_OUT;
		}
		if ((!(endNode.getType() == GEType.JOINER)) && (endNode.getTargetEdges().size() == 1)) 
		{
			return ErrorCode.CODE_EDGES_IN;
		}
	
		/** Check that the input of the startNode is not void and the output of the endNode is not void */
		if ((startNode.getOutputTape().equals(Constants.VOID)) || (endNode.getInputTape().equals(Constants.VOID)))
		{
			return ErrorCode.CODE_CONNECTION_VOID;
		}
		
		/** Checking that the output type of the startNode is equal to the input type of the endNode */
		if (startParent == endParent)
		{
			if ( ! (startNode.getOutputTape().equals(endNode.getInputTape())))
			{
				return ErrorCode.CODE_DIFFERENT_INPUT_OUPUT_TYPES;	
			}
		}

		/** Do not allow connections to an expanded container */
		if (startNode instanceof GEContainer)
		{
			if (((GEContainer) startNode).isExpanded())
			{
				return ErrorCode.CODE_CONTAINER_CONNECTION;	
			}
		}
		if (endNode instanceof GEContainer)
		{
			if (((GEContainer) endNode).isExpanded())
			{
				return ErrorCode.CODE_CONTAINER_CONNECTION;	
			}
		}
		
		/** If connecting between different parents, then the endParent must be the first element
		 * 	in its parent 
		 */
		if (startParent != endParent)
		{
			startParent = (GEContainer) endParent.findDeepestCommonContainer(startNode);
			System.out.println("**CON*1 "+ startParent);
				
			/** Get the ancestor of the startNode that is contained by the new startParent */
			GEStreamNode actualStart = ((GEContainer)startParent).getAncestorInContainer(startNode); 
			System.out.println("**CON*2 "+ actualStart);
				
			/** Get the ancestor of the endNode that is contained by the new endParent */
			GEStreamNode actualEnd = ((GEContainer)startParent).getAncestorInContainer(endNode);
			System.out.println("**CON*3 "+ actualEnd);
		
		
			GEStreamNode node2 = startParent.findDeepestCommonContainer(endNode);
			System.out.println("**CON*4 "+ node2);
			System.out.println("**CON*5 "+((GEContainer)node2).getAncestorInContainer(startNode));
			System.out.println("**CON*6 "+((GEContainer)node2).getAncestorInContainer(endNode));
			
			
			
			
			
		}
		
		
		
		return 0;
	}

	/**
	 * Initialize the fields and draw attributes for the GEFeedbackLoop.
	 * @param graphStruct GraphStructure corresponding to the GEFeedbackLoop.
	 * @param lvel The level at which the GEFeedbackLoop is located.
	 */	
	public void initializeNode(GraphStructure graphStruct, int lvel)
	{
		graphStruct.containerNodes.addContainerToLevel(lvel, this); 
		this.localGraphStruct = graphStruct;	
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100,100)));	
	}

	public void calculateDimension(){};
	public void layoutChildren(){};
	public void moveNodePositionInContainer(GEStreamNode startNode, GEStreamNode endNode, int position){};

}

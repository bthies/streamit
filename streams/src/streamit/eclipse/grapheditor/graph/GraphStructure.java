/*
 * Created on Jun 18, 2003
 */

package streamit.eclipse.grapheditor.graph;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JScrollPane;

import org.eclipse.core.resources.IFile;
import org.jgraph.JGraph;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.utils.JGraphLayoutManager;
//import com.sun.rsasign.t;

/**
 * Graph data structure that has GEStreamNode objects as its nodes. Relies on JGraph 
 * components for its representation and rendering on the screen. 
 * @author jcarlos
 */
public class GraphStructure implements Serializable{

	/**
	 * The graph model.
	 */
	private DefaultGraphModel model;
	
	/**
	 * The JGraph representation that will be used to do the drawing of the graph.
	 */
	private JGraph jgraph;

	/**
	 * Specifies the connections that are present in the JGraph. Necessary
	 * to draw the graph.
	 */
	private ConnectionSet cs;
	
	/**
	 * Specifies attributes required by the JGraph.
	 */
	private Hashtable globalAttributes;
	
	/**
	 * The toplevel GEStreamNode. Typically it should be a GEPipeline object.
	 */   
	private GEContainer topLevel;

	/**
	 * The nodes in the GraphStructure that are currently highlighted.
	 */
	private ArrayList highlightedNodes =null;
	
	/**
	 * The IFile corresponding to this GraphStructure. The IFile contains the
	 * source code representation of the GraphStructure. 
	 */
	private IFile ifile = null;

	/**
	 * The collection of container nodes that are present in the GraphStructure. 
	 */
	public ContainerNodes containerNodes= null;
				
	public JScrollPane panel;
	
	/**
	 * GraphStructure contructor that initializes all of its fields.
	 */	
	public GraphStructure()
	{
		cs = new ConnectionSet();
		globalAttributes= new Hashtable();
		this.model = new DefaultGraphModel();
		this.jgraph = new JGraph();
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph, this));
		containerNodes = new ContainerNodes();
	}

	/**
	 * Get all of the nodes in the graph
	 * @return ArrayList with all of the nodes in the graph.
	 */
	public ArrayList allNodesInGraph()
	{
		ArrayList allNodes = new ArrayList();
		allNodes.add(this.topLevel);
		Iterator containerIter = this.containerNodes.getAllContainers().iterator();
		while(containerIter.hasNext())
		{
			Iterator iterChild = ((GEContainer)containerIter.next()).getContainedElements().iterator();
			while(iterChild.hasNext())
			{
				allNodes.add(iterChild.next());	
			}			
		}
		return allNodes;
	}
	
	/**
	 * Connect startNode to endNode if it is a valid connection.
	 * @param startNode 
	 * @param endNode
	 * @param nodesConnected Determines which of the nodes are already connected (present)
	 * in the graph.
	 * @return 0 if it was possible to connect the nodes, otherwise, return a negative integer.
	 */
	public int connect (GEStreamNode startNode, GEStreamNode endNode, int nodesConnected)
	{
		
		GEContainer startParent = startNode.getEncapsulatingNode();
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

		/** Case when the parent of the start node is a GEPipeline */
		if (startParent.getType() == GEType.PIPELINE)
		{
			ArrayList endParentChildren = endParent.getContainedElements();
			ArrayList startParentChildren = startParent.getContainedElements();
			GEStreamNode endNodeActual = null;
			
			/** If the parent of the start and end nodes are not equal, then
			 *  we must deal with an ancestor of the end node that is contained
			 *  by the GEPipeline. */
			//TODO: Should we make the non-first nodes the first nodes.
			if (endParent != startParent)
			{
				GEStreamNode pNode = endNode;
				boolean containsAncestorInPipe = false;
				while (pNode != this.getTopLevel())
				{
					if (startParentChildren.contains(pNode.getEncapsulatingNode()))
					{
						containsAncestorInPipe = true;
						endNodeActual = pNode.getEncapsulatingNode();
						break;
					}
					pNode = pNode.getEncapsulatingNode();
				}				
				if ( ! (containsAncestorInPipe))
				{
					GEStreamNode lastNodeInCont = ((GEPipeline)startParent).getLastNodeInContainer();
					if ((lastNodeInCont == null) || ( ! (lastNodeInCont == startNode)))
					{
						return ErrorCode.CODE_NO_ANCESTOR_CONNECTION;	
					}		
					else
					{
						endNodeActual = startNode.getOldestContainerAncestor(this.topLevel);
						connectDraw(startNode, endNode);
						return 0;		
					}
				}
			}
			else
			{
				endNodeActual = endNode;
			}
			
			switch (nodesConnected)
			{
				case RelativePosition.BOTH_PRESENT:
				case RelativePosition.START_PRESENT:
				case RelativePosition.NONE_PRESENT:
				{
					((GEContainer) startParent).moveNodePositionInContainer(startNode, endNodeActual, RelativePosition.AFTER);
					break;
				}
				case RelativePosition.END_PRESENT:
				{
					((GEContainer) startParent).moveNodePositionInContainer(startNode, endNodeActual, RelativePosition.BEFORE);
					break;
				}
			}
			connectDraw(startNode, endNode);
	}
			
		/** Case when the parent of the start node is a GEPipeline */
		else if (startParent.getType() == GEType.SPLIT_JOIN)
		{
			GESplitJoin splitjoin = (GESplitJoin) startParent;
			GESplitter splitter = splitjoin.getSplitter();
			GEJoiner joiner =  splitjoin.getJoiner();
			ArrayList splitjoinSuccesors = splitjoin.getSuccesors();
			
			/** In order to make connections, we must have a valid splitjoin with a splitter */
			if (splitter == null)
			{ 
				return ErrorCode.CODE_NO_SPLITTER;
			}
			/** In order to make connections, we must have a valid splitjoin with a joiner */
			if (joiner == null)
			{
				return ErrorCode.CODE_NO_JOINER;
			}
			
			/** The splitter of the splitjoin can never be the endNode */
			if (splitter == endNode)
			{
				return ErrorCode.CODE_SPLITTER_ENDNODE_SJ;
			}
							
			/** Cannot connect the splitter to the joiner (in either direction)*/
			//TODO: should we be using .equals ????
			if ((startNode == splitter) && (endNode == joiner) ||
				(startNode == joiner) && (endNode == splitter))
				{
					return ErrorCode.CODE_SPLITTER_JOINER_CONNECT_SJ;	
				} 	
			
			/** Cannot connect the joiner to an endNode that is inside the same splitjoin */
			/* Does not seem necessary 
			if ((startNode == joiner) && (endNode.getEncapsulatingNode() == splitjoin))
			{
				return ErrorCode.ERRORCODE_JOINER_SAME_PARENT_CONNECT_SJ;
			}*/
			
			/** The inner nodes of the splitjoin can only connect to the joiner */
			if ((startNode != splitter) && (startNode != joiner) && (endNode != joiner))
			{
				return ErrorCode.CODE_INNERNODES_SJ;
			}
			
			/** Connecting splitter to something inside the same parent */
			if (startNode == splitter) 
			{
				GEStreamNode pNode = endNode;
				boolean containsAncestorInPipe = false;
				while (pNode != this.getTopLevel())
				{
					if (splitjoinSuccesors.contains(pNode))
					{
						containsAncestorInPipe = true;
						break;
					}
					pNode = pNode.getEncapsulatingNode();
				}
				if ( ! (containsAncestorInPipe))
				{
					return ErrorCode.CODE_NO_ANCESTOR_CONNECTION;	
				}
				connectDraw(startNode, endNode);
			}
		
			/** Connecting joiner to something inside the same parent */
			else if (endNode == joiner)
			{
				connectDraw(startNode, joiner);
			}
			/** Connecting the joiner to something in a different parent */
			else if (startNode == joiner) 
			{
				GEStreamNode pNode = endNode;
				boolean containsAncestorInPipe = false;
				while (pNode != this.getTopLevel())
				{
					if (splitjoinSuccesors.contains(pNode))
					{
						return ErrorCode.CODE_JOINER_SAME_PARENT_CONNECT_SJ;
					}
					pNode = pNode.getEncapsulatingNode();
				}
				connectDraw(startNode, endNode);
			}
			/** Other alternatives are not allowed (or possible) */
			else 
			{ 
				return -999;
			}
		
		}
		else if (startParent.getType() == GEType.FEEDBACK_LOOP)
		{
			GEFeedbackLoop floop = (GEFeedbackLoop) startParent;
			GESplitter splitter = floop.getSplitter();
			GEJoiner joiner =  floop.getJoiner();

			/** In order to make connections, we must have a valid feedbackloop with a joiner */
			if (joiner == null)
			{
				return ErrorCode.CODE_NO_JOINER;	
			}
			/** In order to make connections, we must have a valid feedbackloop with a splitter */
			if (splitter == null)
			{
				return ErrorCode.CODE_NO_SPLITTER;
			}
		
			/** Cannot connect the splitter to the joiner (in either direction)*/
			//TODO: should we be using .equals ????
			if ((startNode == splitter) && (endNode == joiner) ||
				(startNode == joiner) && (endNode == splitter))
			{
					return ErrorCode.CODE_SPLITTER_JOINER_CONNECT_SJ;	
			} 	
			
			/** Cannot connect the joiner to an endNode that is outside the feedbackloop */
			if ((startNode == joiner) && ( ! (endNode.getEncapsulatingNode() == floop)))
			{
				return ErrorCode.CODE_JOINER_NO_SAME_PARENT_CONNECT;
			}
			connectDraw(startNode, endNode);
		}
			
			
			
			
			
			/*
			
			
			
			switch(nodesConnected)
			{
				case RelativePosition.START_PRESENT:
				{	
					if (endParent == startParent)
					{
						//TODO Add all of the nodes that are connected after endNode
						startParentChildren.add(endNode);
					}
					else
					{
						int startIndex =  startParentChildren.indexOf(startNode);
						startParentChildren.add(startIndex + 1, endNode);
						
					}
					
					break;
				}
				case RelativePosition.END_PRESENT:
				{
					if (endParent == startParent)
					{
						//TODO Add all of the nodes that are connected before startNode
						int endNodeIndex = startParentChildren.indexOf(endNode);
						int addedAtIndex = endNodeIndex == 0 ? 0 : endNodeIndex - 1;
						startParentChildren.add(addedAtIndex, startNode);
					}
					else
					{
						//TODO
						int endNodeIndex = startParentChildren.indexOf(endParent);
						int addedAtIndex = endNodeIndex == 0 ? 0 : endNodeIndex - 1;
						startParentChildren.add(addedAtIndex, startNode);
											
					}
					break;
				}
				case RelativePosition.BOTH_PRESENT:
				{
					if (endParent == startParent)
					{
						if (startParentChildren.contains(startNode))
						{
							
						}
					}
					
					
					
					if ((endParent.getType() == GEType.PIPELINE) || 
						(endParent.getType() == GEType.FEEDBACK_LOOP) ||
						(endParent.getType() == GEType.SPLIT_JOIN))
						{
							int startNodeIndex = startParentChildren.indexOf(startNode);
						// 2/14/04 stackoverflow	startParentChildren.add(startNodeIndex + 1, endParent);
						startParentChildren.add(startNodeIndex + 1, endNode);
						}
					break;
				}
				
				case RelativePosition.NONE_PRESENT:
				{
					startParentChildren.add(endNode);
					startParentChildren.add(startNode);
					break;
				}
			}
			connectDraw(startNode, endNode);
			*/
		
		
		/*

		else if (startParent.getType() == GEType.FEEDBACK_LOOP)
		{
			return false;
		}
		*/
		else
		{
			
			return ErrorCode.CODE_INVALID_PARENT_TYPE;
		}

		return 0;
		
	}

	/**
	 * Construct graph representation.
	 */	
	public void constructGraph(JScrollPane pane)
	{
		System.out.println("Constructor with pane as an argument");
		this.panel = pane;
		this.topLevel.construct(this, 0);
		//model.insert(cells.toArray(), globalAttributes, cs, null, null);
		
		model.edit(globalAttributes, cs, null, null);
		
		this.jgraph.getGraphLayoutCache().setVisible(jgraph.getRoots(), true);
		this.jgraph.getGraphLayoutCache().setVisible(this.containerNodes.getAllContainers().toArray(), false);
				
		this.containerNodes.setCurrentLevelView(this.containerNodes.getMaxLevelView());
		System.out.println("THE CURRENT LEVEL IS " + this.containerNodes.getCurrentLevelView());
		
		
		JGraphLayoutManager manager = new JGraphLayoutManager(this);
		manager.arrange();	

		//******************************************
		// TEST CODE BEGIN
		//******************************************	
		/*	
		Iterator keyIter = this.levelContainers.keySet().iterator();
		Iterator valIter = this.levelContainers.values().iterator();
		while(keyIter.hasNext()) {
			System.out.println("Key = " + keyIter.next());	
		}
		int x =0;
		while(valIter.hasNext()) {
			Iterator  listIter = ((ArrayList) valIter.next()).iterator();
			while (listIter.hasNext()){
				System.out.println("Iter = " + x + " value = "+listIter.next());
			}
			x++;	
		}*/
		//******************************************
		// TEST CODE END
		//******************************************									
	}
	
	/**
	 * Establishes a connection between <lastNode> and <currentNode>.
	 * @param lastNode GEStreamNode that is source of connection.
	 * @param currentNode GEStreamNode that is targetr of connection.
	 */
	public void connectDraw(GEStreamNode lastNode, GEStreamNode currentNode)
	{
		System.out.println("Connecting " + lastNode.getName()+  " to "+ currentNode.getName());
		DefaultEdge edge = new DefaultEdge(); 
			
		Map edgeAttrib = GraphConstants.createMap();
		globalAttributes.put(edge, edgeAttrib);
		
		GraphConstants.setLineEnd(edgeAttrib, GraphConstants.ARROW_CLASSIC);
		GraphConstants.setLineWidth(edgeAttrib, 6);
		GraphConstants.setEndFill(edgeAttrib, true);
		GraphConstants.setDashPattern(edgeAttrib, new float[] {2,4});
	
		cs.connect(edge, lastNode.getPort(), currentNode.getPort());
		
		lastNode.addSourceEdge(edge);
		currentNode.addTargetEdge(edge);		
		
		this.getGraphModel().insert(new Object[] {edge}, null, null, null, null);
		this.getGraphModel().edit(globalAttributes, cs, null, null);
	}
	
	/**
	 * Highlight the GEStreamNode. 
	 * @param strNode GEStreamNode to be highlighted. 
	 */
	public void highlightNodes(ArrayList nodesToHighLight)
	{
		if (highlightedNodes != null)
		{
			Iterator hlIter = highlightedNodes.iterator();
			while (hlIter.hasNext())
			{
				((GEStreamNode) hlIter.next()).highlight(this, false);
			}
			
		}
				
		Iterator hIter = nodesToHighLight.iterator();
		while (hIter.hasNext())
		{
			((GEStreamNode) hIter.next()).highlight(this, true);
		}
		highlightedNodes = nodesToHighLight;
	}


	/**
	 * Get the JGraph of GraphStructure.
	 * @return this.jgraph
	 */
	public JGraph getJGraph()
	{
		return this.jgraph;
	}
	
	/**
 	 * Set the JGraph of GraphStructure to <jgraph>.
	 * @param jgraph
 	 */
	public void setJGraph(JGraph jgraph)
	{
		this.jgraph = jgraph;
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph, this));
	}
	
	/**
	 * Gets the graph model of the GraphStructure.
	 * @return this.model
	 */
	public DefaultGraphModel getGraphModel()
	{
		return this.model;
	}

	/**
	 * Sets the graph model to model..
	 * @param model
	 */
	public void setGraphModel(DefaultGraphModel model)
	{
		this.model = model; 
	}

	/**
	 * Gets the toplevel node.
	 * @return this.topLevel
	 */
	public GEContainer getTopLevel ()
	{
		return this.topLevel;
	}

	/** 
	 * Sets the toplevel node to strNode.
	 * @param strNode
	 */
	public void setTopLevel(GEContainer strNode)
	{
		this.topLevel = strNode;
	}


	/**
	 * Get the global attributes of the GraphStructure.
	 * @return this.globalAttributes
	 */
	public Hashtable getAttributes()
	{
		return this.globalAttributes;
	}
	
	/**
	 * Get the connection set of GraphStructure.
	 * @return this.cs;
	 */
	public ConnectionSet getConnectionSet()
	{
		return this.cs;
	}

	/**
	 * Get the IFile that corresponds to this GraphStructure.
	 * @return IFile
	 */	
	public IFile getIFile()
	{
		return this.ifile;
	}
	
	/**
	 * Set the IFile that corresponds to this GraphStructure.
	 * @param ifile IFile.
	 */
	public void setIFile(IFile ifile)
	{
		this.ifile = ifile;
	}
	
	/**
	 * Output the code representation of the GraphStructure.
	 * @param out
	 */
	public void outputCode(StringBuffer strBuff)
	{
	    this.topLevel.outputCode(strBuff);
	    
	    
/*	    
	    ArrayList childList = this.topLevel.succesors;
	    Iterator childIter = childList.iterator();
	    
	    while (childIter.hasNext())
	    {
	   		((GEStreamNode) childIter.next()).outputCode(out); 	
	    }
	    */	    
	}
}
/*
 * Created on Jun 18, 2003
 */

package streamit.eclipse.grapheditor.graph;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.swing.JScrollPane;

import org.eclipse.core.resources.IFile;
import org.jgraph.JGraph;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.graph.utils.JGraphLayoutManager;
import streamit.eclipse.grapheditor.graph.utils.StringTranslator;


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
	 * Get all of the nodes in the graph (including the toplevel node ).
	 * @return ArrayList with all of the nodes in the graph.
	 */
	public ArrayList allNodesInGraph()
	{
		ArrayList allNodes = new ArrayList();
		/** add the toplevel node to the list */
		allNodes.add(this.topLevel);
		
		/** All the nodes are added since all containers are contained by some 
		 *  other container, except for the toplevel node (that is why we had to
		 * 	add the toplevel explicitly*/
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
	 * Get all of the nodes in the graph that are not containers.
	 * @return ArrayList with all of the nodes in the graph that are not containers.
	 */
	public ArrayList allNonContainerNodesInGraph()
	{
		ArrayList allNodes = new ArrayList();
		Iterator containerIter = this.containerNodes.getAllContainers().iterator();
		while(containerIter.hasNext())
		{
			Iterator iterChild = ((GEContainer)containerIter.next()).getContainedElements().iterator();
			while(iterChild.hasNext())
			{
				GEStreamNode node = (GEStreamNode)iterChild.next();
				/** Only add to the list if it is not a container */
				if (! (node instanceof GEContainer))
				{
					allNodes.add(node);
				}	
			}			
		}
		return allNodes;
	}

	/**
	 * Connect startNode to endNode if it is a valid connection.
	 * @param startNode GEStreamNode
	 * @param endNode GEStreamNode
	 * @return 0 if it was possible to connect the nodes, otherwise, return a negative integer (error code).
	 */
	public int connect(GEStreamNode startNode, GEStreamNode endNode)
	{
		int errorCode = startNode.getEncapsulatingNode().connect(startNode, endNode);
		return errorCode;
	}
	
	/**
	 * Construct graph representation of the streamIt source code and make it visible on the pane.
	 * @param pane JScrollPane where the graph will be constructed.
	 */	
	public void constructGraph(JScrollPane pane)
	{
		System.out.println("Constructor with pane as an argument");
		this.panel = pane;
		this.topLevel.construct(this, 0);
		this.topLevel.setLocation(new Point(Constants.TOPLEVEL_LOC_X, Constants.TOPLEVEL_LOC_Y));
				
		model.edit(globalAttributes, cs, null, null);
		
		/** Set all the nodes visible (except for the container nodes) */
		this.jgraph.getGraphLayoutCache().setVisible(jgraph.getRoots(), true);
		this.jgraph.getGraphLayoutCache().setVisible(this.containerNodes.getAllContainers().toArray(), false);

		/** Set the current level to the max level view available */				
		this.containerNodes.setCurrentLevelView(this.containerNodes.getMaxLevelView());
		System.out.println("THE CURRENT LEVEL IS " + this.containerNodes.getCurrentLevelView());
		
		/** Layout the graph */		
		JGraphLayoutManager manager = new JGraphLayoutManager(this);
		manager.arrange();									
	}
	
	/**
	 * Create the TopLevel GEPipeline for a corresponding to graphStruct.
	 * @param graphStruct GraphStructure.
	 */
	public void createDefaultToplevelPipeline()
	{
		String name  = "TopLevelPipeline";
			
		//TODO: Fix this hack (come up with different way to set graphStruct
		GEContainer node = new GEPipeline(name, this);
		this.setTopLevel(node);
		
		this.containerNodes.addContainerToLevel(0, node);
		IFileMappings.addIFileMappings(node, this.getIFile());
		
		node.initDrawAttributes(this, new Rectangle(30, 30, 700, 700));
		this.getJGraph().getGraphLayoutCache().setVisible(node, true);
		this.getGraphModel().edit(this.getAttributes(), this.getConnectionSet(), null, null);
		
		//		glc.insert(new Object[] {node}, null, null, null, null);
		//		glc.setVisible(node, true);	
	}
	
	/**
	 * Create a GEStreamNode.
	 * @param properties Properties
	 * @param jgraph JGraph
	 * @param bounds Rectangle
	 * @param graphStruct GraphStructure
	 * @return GEStreamNode
	 */
	public GEStreamNode nodeCreate(Properties properties, JGraph jgraph, Rectangle bounds)
	{
		if ((properties != null) && (jgraph != null) && (bounds != null) && (this != null))
		{
	//		GraphLayoutCache glc = jgraph.getGraphLayoutCache();
			GEStreamNode node = null;
		
			String name = properties.getProperty(GEProperties.KEY_NAME);
			String type = properties.getProperty(GEProperties.KEY_TYPE);
			
			if (GEType.PHASED_FILTER == type)
			{
				node = new GEPhasedFilter(name, this);
				((GEPhasedFilter)node).setPushPopPeekRates(Integer.parseInt(properties.getProperty(GEProperties.KEY_PUSH_RATE)),
														   Integer.parseInt(properties.getProperty(GEProperties.KEY_POP_RATE)),
														   Integer.parseInt(properties.getProperty(GEProperties.KEY_PEEK_RATE)));
			}
			else if (GEType.SPLITTER == type)
			{
				node = new GESplitter(name, 
									  StringTranslator.weightsToInt(properties.getProperty(GEProperties.KEY_SPLITTER_WEIGHTS)));	
				((GESplitter) node).setDisplay(jgraph);	
			}
			else if (GEType.JOINER == type)
			{
				node = new GEJoiner(name, 
									StringTranslator.weightsToInt(properties.getProperty(GEProperties.KEY_JOINER_WEIGHTS)));
				((GEJoiner) node).setDisplay(jgraph);	
			}
			else
			{
				throw new IllegalArgumentException("Invalid type for GEStreamNode vertex (NodeCreator.java)");
			}

			node.setOutputTape(properties.getProperty(GEProperties.KEY_OUTPUT_TAPE));
			node.setInputTape(properties.getProperty(GEProperties.KEY_INPUT_TAPE));
			GEContainer parentNode = this.containerNodes.getContainerNodeFromNameWithID(properties.getProperty(GEProperties.KEY_PARENT));
			
			//parentNode.addNodeToContainer(node);
			/** Add the node to its encapsulating node */
			if (parentNode instanceof GESplitJoin)
			{
				GESplitJoin splitjoin = ((GESplitJoin)parentNode); 
				splitjoin.addInnerNodeAtIndex(
												Integer.parseInt(properties.getProperty(GEProperties.KEY_INDEX_IN_SJ)), node);
				parentNode.setDisplay(jgraph);
			}
			else
			{
				parentNode.addNodeToContainer(node);
			}

			/** The node that we are adding is a container, must add it to container list 
			 * at corresponding level **/
			if (node instanceof GEContainer)
			{	
				this.containerNodes.addContainerToLevel(parentNode.getDepthLevel() + 1, node);	
			}
	
			this.constructANode(node,  bounds);
			return node;
		}
		else 
		{
			throw new IllegalArgumentException();
		}
	}
	
	
	
	/**
	 * Construct a the GEStreamNode specified by the properties.
	 * @param node GEStreamNode to be constructed.
	 * @param parentNode GEContainer of the node
	 * @param graphStruct GraphStructure
	 * @param bounds Rectangle
	 */
	public  void constructANode(GEStreamNode node, Rectangle bounds)
	{
		GraphLayoutCache glc = this.getJGraph().getGraphLayoutCache();
	
//		/** Add the node to its encapsulating node */
//		parentNode.addNodeToContainer(node);		
//	
//
//		/** The node that we are adding is a container, must add it to container list 
//		 * at corresponding level **/
//		if (node instanceof GEContainer)
//			{	
//				this.containerNodes.addContainerToLevel(parentNode.getDepthLevel() + 1, node);	
//			}

		/** Add the mapping of the node to its corresponding IFile */
		IFileMappings.addIFileMappings(node, this.getIFile());

		/** Set the draw attributes of the node */
		node.initDrawAttributes(this, bounds);
		
		if (node.getEncapsulatingNode() instanceof GESplitJoin)
		{
			GESplitJoin splitjoin = ((GESplitJoin)node.getEncapsulatingNode());
			GESplitter splitter = splitjoin.getSplitter();
			GEJoiner joiner = splitjoin.getJoiner();
			
			
			/** Make the connection between the splitter of the splitjoin and the node */
			if ((splitter != null) && (node != splitter) && (node != joiner))
			{
				this.connectDraw(splitjoin.getSplitter(), node);
			}
			/** Make the connection between the node and the joiner of the splitjoin */
			if ((joiner != null) && (node != joiner) && (node != splitter))
			{
				this.connectDraw(node, splitjoin.getJoiner());	
			}
			
			
		} 
		
		/** Edit the model to reflect the changes made */
		glc.setVisible(node, true);
		this.getGraphModel().edit(this.getAttributes(), this.getConnectionSet(), null, null);
	}
	
	/**
	 * Construct all the nodes in the Collection of nodes passed as an argument.
	 * @param nodes Collection of nodes to be constructed.
	 * @param parentNode GEContainer
	 * @param graphStruct GraphStructure
	 * @param bounds Rectangle
	 */
	public void constructNodes(Collection nodes, GEContainer parentNode, Rectangle bounds)
	{
		GraphLayoutCache glc = this.getJGraph().getGraphLayoutCache();
		
		/** Add the nodes to its encapsulating node */
		parentNode.addNodesToContainer(nodes, this.containerNodes, this.getIFile());		

		/** Set the draw attributes of the nodes */
		for (Iterator nodeIter = nodes.iterator(); nodeIter.hasNext();)
		{
			((GEStreamNode) nodeIter.next()).initDrawAttributes(this, bounds);
		}
		
		/** Edit the model to reflect the changes made */
		glc.setVisible(nodes.toArray(), true);
		this.getGraphModel().edit(this.getAttributes(), this.getConnectionSet(), null, null);
	}
		
	/**
	 * Establishes a connection between lastNode and currentNode .
	 * @param lastNode GEStreamNode that is source of connection.
	 * @param currentNode GEStreamNode that is targetr of connection.
	 */
	public void connectDraw(GEStreamNode lastNode, GEStreamNode currentNode)
	{
		/** Create the edge */
		DefaultEdge edge = new DefaultEdge(); 
			
		Map edgeAttrib = GraphConstants.createMap();
		globalAttributes.put(edge, edgeAttrib);
		
		/** Set the attributes for the edge */
		GraphConstants.setLineEnd(edgeAttrib, GraphConstants.ARROW_CLASSIC);
		GraphConstants.setLineWidth(edgeAttrib, 6);
		GraphConstants.setEndFill(edgeAttrib, true);
		GraphConstants.setDashPattern(edgeAttrib, new float[] {2,4});
	
		/** Establish the connection */
		cs.connect(edge, lastNode.getPort(), currentNode.getPort());
		
		/** Add the edge as a source edge for the source node */
		lastNode.addSourceEdge(edge);
		
		/** Add the edge as a target edge for the target node */
		currentNode.addTargetEdge(edge);		
		
		/** Add the edge to the graph model */
		this.getGraphModel().insert(new Object[] {edge}, null, null, null, null);
		
		/** Edit the changes in the attributes and connection set of the model */
		this.getGraphModel().edit(globalAttributes, cs, null, null);
	}
	
	/**
	 * Highlight all the GEStreamNodes in the ArrayList passed as a parameter. All other GEStreamNodes 
	 * that were highlighted, cease to be hightlighted (they become "unhighlighted").
	 * @param nodesToHighlight ArrayList of the nodes to be highlighted. 
	 */
	public void highlightNodes(ArrayList nodesToHighLight)
	{
		/** "Unhighlight" all nodes that might have been highlighted */
		if (highlightedNodes != null)
		{
			Iterator hlIter = highlightedNodes.iterator();
			while (hlIter.hasNext())
			{
				((GEStreamNode) hlIter.next()).highlight(this, false);
			}
			
		}
		
		/** Highlight all the nodes in the list passed as a parameter */
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
 	 * Set the JGraph of GraphStructure to jgraph.
	 * @param jgraph JGraph
 	 */
	public void setJGraph(JGraph jgraph)
	{
		this.jgraph = jgraph;
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph, this));
	}
	
	/**
	 * Gets the graph model of the GraphStructure.
	 * @return DefaultGraphModel
	 */
	public DefaultGraphModel getGraphModel()
	{
		return this.model;
	}

	/**
	 * Sets the graph model to model..
	 * @param model DefaultGraphModel
	 */
	public void setGraphModel(DefaultGraphModel model)
	{
		this.model = model; 
	}

	/**
	 * Gets the toplevel node.
	 * @return GEContainer that is the toplevel node.
	 */
	public GEContainer getTopLevel ()
	{
		return this.topLevel;
	}

	/** 
	 * Sets the toplevel node to strNode.
	 * @param strNode GEContainer
	 */
	public void setTopLevel(GEContainer strNode)
	{
		this.topLevel = strNode;
	}


	/**
	 * Get the global attributes of the GraphStructure.
	 * @return Hashtable containing the global attributes.
	 */
	public Hashtable getAttributes()
	{
		return this.globalAttributes;
	}
	
	/**
	 * Get the connection set of GraphStructure.
	 * @return ConnectionSet
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
	public void outputCode(StringBuffer strBuff, ArrayList nameList)
	{
	    this.topLevel.outputCode(strBuff, nameList);   
	}
}
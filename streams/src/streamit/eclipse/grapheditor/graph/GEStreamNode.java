/*
 * Created on Jun 20, 2003
 */ 
package streamit.eclipse.grapheditor.graph;
 
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.Port;

/**
 * GEStreamNode is the graph internal representation of a node. 
 * @author jcarlos
 */
public abstract class GEStreamNode extends DefaultGraphCell implements Serializable{
	
	
	/**
	 * The name of the GEStreamNode.
	 */
	protected String name;	

	/**
	 * The type of the GEStreamNode. Must be one of the types specified by GEType.
	 */
	protected String type;
		
	/**
	 * The immediate GEStreamNode that contains this GEStreamNode.
	 */
	protected GEContainer encapsulatingNode;	
	
	/**
	 * The port of the GEStreamNode (used by JGraph).
	 */
	protected DefaultPort port;
		
	/**
	 * The information that this GEStreamNode contains. Depending on the type,
	 * this data will be different. 
	 */
	protected String info;

	/**
	 * Boolean that determines wheter or not the node has been visited.
	 */
	protected boolean visited;

	/**
	 * The input tape value for the StreamIt representation of the GEPhasedFilter.
	 * The default value for the inputTape should be void. 
	 */
	protected String inputTape;
	
	/**
	 * The output tape value for the StreamIt representation of the GEPhasedFilter.
	 * The default value for the outputTape should be void. 
	 */	
	protected String outputTape;
	
	/**
	 * The arguments that must be passed to the StreamIt representation of the GEPhasedFilter.
	 * The default value for args is: no arguments (empty list).
	 */
	protected ArrayList args;

	protected ArrayList sourceEdges;
	protected ArrayList targetEdges;

	/**
	 * GEStreamNode constructor.
	 * @param type The type of the GEStreamNode (must be defined as a GEType)
	 * @param name The name of the GEStreamNode.
	 */
	public GEStreamNode(String type, String name)
	{
		super("<HTML>" + Constants.HTML_TSIZE_BEGIN + name + Constants.HTML_TSIZE_END+ "</html>");
		this.type = type;
		this.name = name;
		this.setInfo("");
		this.encapsulatingNode = null;
		this.sourceEdges = new ArrayList();
		this.targetEdges = new ArrayList();
		this.args = new ArrayList();
		this.inputTape =  Constants.INT;
		this.outputTape = Constants.INT;
		this.visited = false;	
	}

	/**
 	 * Get the name of this GEStreamNode.
 	 * @return The name of this GEStreamNode.
	 */
	public String getName()
	{
		return this.name;	 
	}
	
	/**
	 * Set the name of this GEStreamNode.
	 * @param name String
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/** 
	 * Get the name with the unique ID of this GEStreamNode.
	 * The format of the name returned is "name"+Constants.ID_TAG + "ident". 
	 * (where "name" is the name of the node, "ident" is the ID number, and
	 * Constants.ID_TAG is the one that separates the two).
	 * @return The name of the GEStreamNode with the unique ID number.
	 */
	public String getNameWithID()
	{
		return (this.name + Constants.ID_TAG + this.hashCode());
	}
	
	
	/**
 	* Get the type of this GEStreamNode.
 	* @return The type of this GEStreamNode.
 	*/	
	public String getType()
	{
		return this.type;	
	}
	
	/**
	 * Get the port of <this>.
	 * @return The port that corresponds to the GEStreamNode
	 */
	public Port getPort()
	{
		return this.port;
	}

	/**
	 * Set the port for <this>
	 * @param port Set the GEStreamNode's port to <port>.
	 */
	public void setPort(DefaultPort port)
	{
		this.port = port;
	}

	/**
	 * Get the input tape of <this>
	 * @return The input tape that corresponds to the GEStreamNode.
	 */
	public String getInputTape()
	{
		return this.inputTape;
	}

	/**
	 * Set the input tape of <this>.
	 * @param in String value to which GEStreamNode's input tape will be set.
	 */
	public void setInputTape(String in)
	{
		this.inputTape = in;
	}

	/**
	 * Get the output tape of <this>
	 * @return The output tape that corresponds to the GEStreamNode.
	 */	
	public String getOutputTape()
	{
		return this.outputTape;
	}

	/**
	 * Set the output tape of <this>.
	 * @param in String value to which GEStreamNode's output tape will be set. 
	 */
	public void setOutputTape(String out)
	{
		this.outputTape = out;
	}

	/**
	 * Returns true when this GEStreamNode is connected to other GEStreamNode in 
	 * either its ded or collapsed states. Otherwise, return false.
	 * @return boolean
	 */

	public boolean isNodeConnected()
	{
		if ((this.getSourceEdges().size() == 0) && (this.getTargetEdges().size() ==0))
		{
			return false;
		}
		return true;
	}

	/**
	 * Sets the node that encapsulates this
	 * @param node The GEStreamNode that encapsulates this
	 */
	public void setEncapsulatingNode(GEContainer node)
	{
		/*
		if (this.encapsulatingNode !=null)
		{
			((GEContainer)this.encapsulatingNode).removeNodeFromContainer(this);
		}*/
		this.encapsulatingNode = node;
		/*
		if (this.encapsulatingNode !=null)
		{
			((GEContainer)this.encapsulatingNode).addNodeToContainer(this);
		}*/
	}
	
	/**
	 * Gets the encapsulating node of this
	 * @return The encapsulating node of GEStreamNode
	 */
	public GEContainer getEncapsulatingNode()
	{
		return this.encapsulatingNode;
	}
	
	
	/**
	 * Set the container as the parent of the GEStreamNode child. 
	 * @param child GEStreamNode
	 * @param container GEContainer
	 */
	public void changeParentTo(GEContainer container)
	{
		/** If the encapsulating node already contains the element,
		 * 	then we should not do anything. */
		if (this.getEncapsulatingNode() !=null) 
		{
			/** If this container node does not already contain the element, then we 
			 * have to remove the node from its current encapsulating  node */
			 
			if ( ! (container.getContainedElements().contains(this))) 
			{	
				this.getEncapsulatingNode().removeNodeFromContainer(this);
				container.addNodeToContainer(this);	
			}
			/** Now we can add the node to this container */
			
		}	
	}	
	
	/**
	 * Get the nodes that are connected to this GEStreamNode, and are the source in the connection.
	 * This GEStreamNode is the target in the edge and we will be returning all the edges that are sources.
	 * @return ArrayList with all the nodes that are connected to this as sources.
	 */
	public ArrayList getSourceNodes()
	{
		ArrayList sourceNodes = new ArrayList();
		for (Iterator edgeIter = this.targetEdges.iterator(); edgeIter.hasNext();)
		{
			sourceNodes.add(((DefaultPort)((DefaultEdge)edgeIter.next()).getSource()).getParent());
		}
		return sourceNodes;		
	}
	
	/**
	 * Get the nodes that are connected to this GEStreamNode, and are the targets in the connection.
	 * This GEStreamNode is the source in the edge and we will be returning all the edges that are targets.
	 * @return ArrayList with all the nodes that are connected to this as targets.
	 */
	public ArrayList getTargetNodes()
	{
		ArrayList targetNodes = new ArrayList();
		for (Iterator edgeIter = this.sourceEdges.iterator(); edgeIter.hasNext();)
		{
			targetNodes.add(((DefaultPort)((DefaultEdge)edgeIter.next()).getTarget()).getParent());
		}
		return targetNodes;
	}
	
	
	
	/**
	 * Get the depth level at which this is located
	 * @return level of the GEStreamNode
	 */
	public int getDepthLevel()
	{
		
		GEContainer container = this.getEncapsulatingNode();
		if (container != null)
		{
			return 1 + container.getDepthLevel();
		}
		else
		{
			return 0;	
		}
	}
	
	/**
	 * Get the info of <this>.
	 * @return The info of this GEStreamNode.
	 */
	public String getInfo()
	{
		return this.info;	
	}

	/**
	 * Set the info of <this>.
	 * @param info Set the info of the GEStreamNode.
	 */
	public void setInfo(String info)
	{
		this.info = info;
	}

	/**
	 * Get the info of <this> as an HTML label.
	 * @return The info of the GEStreamNode as an HTML label.
	 */
	public String getInfoLabel()
	{
		return new String("<HTML>"+ Constants.HTML_TSIZE_BEGIN + this.name + "<BR>" + 
						  this.info + Constants.HTML_TSIZE_END + "</HTML>");
	}
	
	/**
	 * Get the name of <this> as an HTML label.
	 * @return The name of the GEStreamNode as an HTML label.
	 */
	public String getNameLabel()
	{
		return new String("<HTML>" + Constants.HTML_TSIZE_BEGIN + this.name + 
						  Constants.HTML_TSIZE_END + "</HTML>");
	}
	
	/**
	 * Add <target> to the list of edges that have this GEStreamNode as its target.
	 * @param target DefautltEdge that has <this> as its target. 
	 */	
	public void addTargetEdge(DefaultEdge target)
	{
		this.targetEdges.add(target);
	}

	/**
	 * Add <source> to the list of edges that have this GEStreamNode as its source.
	 * @param source DefautltEdge that has <this> as its source. 
	 */		
	public void addSourceEdge(DefaultEdge source)
	{
		this.sourceEdges.add(source);
	}

	/**
	 * Remove <target> from the list of edges that have this GEStreamNode as its target.
	 * @param target DefautltEdge that has <this> as its target. 
	 */	
	public void removeTargetEdge(DefaultEdge target)
	{
		this.targetEdges.remove(target);
	}
	
	/**
	 * Remove <source> from the list of edges that have this GEStreamNode as its source.
	 * @param source DefautltEdge that has <this> as its source. 
	 */		
	public void removeSourceEdge(DefaultEdge source)
	{
		this.sourceEdges.remove(source);
	}
	
	/**
	 * Get a list of edges that have this GEStreamNode as its target. 
	 * @return ArrayList that containes the edges with <this> as its target.
	 */
	public ArrayList getTargetEdges()
	{
		return this.targetEdges;	
	}
	
	/**
	 * Get a list of edges that have this GEStreamNode as its source. 
	 * @return ArrayList that containes the edges with <this> as its source.
	 */
	public ArrayList getSourceEdges()
	{
		return this.sourceEdges;
	}
	
	/**
	 * Get the dimension of theGEStreamNode. Unless this method is overriden, the 
	 * default dimensions for a GEStreamNode will be returned. 
	 * @return Dimension 
	 */
	public Dimension getDimension()
	{
		return Constants.DEFAULT_DIMENSION;
	}
	
	
	/**
	 * Get the location of the node on the screen. 
	 * @return Point location of the node on the screen.
	 */
	public Point getLocation()
	{
		return GraphConstants.getOffset(this.attributes);
	}
	
	/**
	 * Set the location of the node on the screen.
	 * @param loc Point location of the node on the screen.
	 */
	public void setLocation(Point loc)
	{
		GraphConstants.setOffset(this.attributes, loc);
	}
	
	/**
	 * Graphicallly change the location of the node to the specified location.
	 * @param location Point 
	 * @param jgraph JGraph
	 */
	public void setGraphicalLocation(Point location, JGraph jgraph)
	{
		Map change = GraphConstants.createMap();
		GraphConstants.setBounds(change, new Rectangle(location , 
													   this.getDimension()));
		Map nest = new Hashtable ();
		nest.put(this, change);
		jgraph.getModel().edit(nest, null, null, null);
	}
	
	
	/**
	 * Write the textual representation of the arguments that correspond 
	 * to this GEStreamNode. The arguments will be written in the form:
	 * "(arg1, arg2, ... , argn)".
	 * @param out PrintWriter used to output text representation of args. 
	 */
	protected StringBuffer outputArgs()
	{
		StringBuffer strBuff = new StringBuffer();
		int numArgs = this.args.size();	
		if (numArgs > 0)
		{
			strBuff.append(" ( ");
				
			for (int i = 0; i < numArgs; i++)
			{
				strBuff.append((String) args.get(i));	
				if (i != numArgs - 1)
				{
					strBuff.append(",");
				}
			}
			strBuff.append(" ) ");
		}	
		return strBuff;
			
	}

	/**
	 * Get the names of the nodes requested.
	 * @param nodeList ArrayList
	 * @return ArrayList with the names of the nodes requested
	 */

	public static ArrayList getNodeNames(ArrayList nodeList)
	{
		Iterator allContIter = nodeList.iterator();
		ArrayList names = new ArrayList();
		while(allContIter.hasNext())
		{	
			names.add(((GEStreamNode)allContIter.next()).name);
		}
		return names;
		
		
	}
	
	/**
	 * Get the names with ID of the nodes requested.
	 * @param nodeList ArrayList
	 * @return ArrayList with the names of the nodes requested
	 */

	public static ArrayList getNodeNamesWithID(ArrayList nodeList)
	{
		Iterator allContIter = nodeList.iterator();
		ArrayList names = new ArrayList();
		while(allContIter.hasNext())
		{	
			names.add(((GEStreamNode)allContIter.next()).getNameWithID());
		}
		return names;
		
		
	}

	/**
	 * Get the oldest GEContainer ancestor (not including the toplevel pipeline) correspoding
	 * to the GEStreamNode passed as the argument.
	 * @param GEStreamNode node whose oldest ancestor will be determined. 
	 * @return GEStreamNode oldest ancestor
	 */
	public GEStreamNode getOldestContainerAncestor(GEContainer toplevel)
	{
		GEStreamNode pNode = this;
		boolean containsAncestorInPipe = false;
		while (pNode.getEncapsulatingNode() != toplevel)
		{
			pNode = pNode.getEncapsulatingNode();
		}
		return pNode;			
	}


	/**
	 * Delete this node from the graph model. All the edges that are connected 
	 * to the node will also be deleted.
	 * @param model GraphModel 
	 */
	public void deleteNode(JGraph jgraph)
	{
		GEContainer parent = this.getEncapsulatingNode();
		GraphModel model = jgraph.getModel();
	
		/** If the immediate parent of the node is a splitjoin, and the node is not a splitter
		 * 	or a joiner, then we have to remove the weight that corresponds to it in the splitter 
		 * 	and the joiner of the Splitjoin*/
		if ((parent.getType() == GEType.SPLIT_JOIN) && 
			(this.getType() != GEType.SPLITTER) && (this.getType() != GEType.JOINER))
		{
			GESplitJoin splitjoin = ((GESplitJoin)parent); 
			int index = splitjoin.getIndexOfInnerNode(this);
			if (index != -1)
			{
				
				splitjoin.getJoiner().removeWeightAt(index);
				splitjoin.getSplitter().removeWeightAt(index);
				splitjoin.getSplitter().setDisplay(jgraph);
				splitjoin.getJoiner().setDisplay(jgraph); 
			}			
		}
		/** If the immediate parent of the node is a feedbackloop, and the node is not a splitter 
		 * 	or a joiner, then we have to remove the weight that corresponds to it in the splitter
		 * 	and th joiner of the feedbackloop*/
		 
		else if ((parent.getType() == GEType.FEEDBACK_LOOP) &&
				(this.getType() != GEType.SPLITTER) && (this.getType() != GEType.JOINER))
		{
			GEFeedbackLoop floop = ((GEFeedbackLoop)parent);
			int index = 0;
			if (floop.isLoop(this))
			{
				index = 1;
			}
				
			floop.getSplitter().removeWeightAt(index);
			floop.getJoiner().removeWeightAt(index);
			floop.getSplitter().setDisplay(jgraph);
			floop.getJoiner().setDisplay(jgraph);
		}
		
		/** Remove this node from its container */			
		if (parent != null)
		{
			 parent.removeNodeFromContainer(this);
		}

		/** Get all the source and target edeges belonging to the node 
		 *  These nodes are going to be deleted */
		Iterator sourceIter = this.getSourceEdges().iterator();
		Iterator targetIter = this.getTargetEdges().iterator();
		
		/** The edges that are going to be deleted must be removed from the 
		 *  lists of the nodes that are connected to them. 
		 * 
		 *  Remove all edges (where node to be deleted is source) from the list 
		 *  of target edges (where the target is the the edge's target node) */  
		while (sourceIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) sourceIter.next();
			DefaultPort port = (DefaultPort) edge.getTarget();
			if (port != null)
			{
				if (port.getParent() != null)
				{
					((GEStreamNode) port.getParent()).getTargetEdges().remove(edge);
				}
			}	
		}

		/**  Remove all edges (where node to be deleted is source) from the list 
		  *  of target edges (where the target is the the edge's target node) */
		while (targetIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) targetIter.next();
			DefaultPort port = (DefaultPort) edge.getSource();
			if (port != null)
			{
				if (port.getParent() != null)
				{
					((GEStreamNode) port.getParent()).getSourceEdges().remove(edge);
				}
			}	
		}
		
		/** Remove this node's source and target edges */
		model.remove(this.getSourceEdges().toArray());
		model.remove(this.getTargetEdges().toArray());
	}

	/**
	 * Determine wheter or not the node has been already visited when its output code is generated
	 * @return True if this node has been visited by the template generator; otherwise, return false.
	 */
	public boolean visited()
	{
		return this.visited;
	}
	
	/**
	 * Set the visited value of this node
	 * @param visited boolean Set to true if the node has been visited; otherwise, set to false.
	 */
	public void setVisited(boolean visited)
	{
		this.visited = visited;
	}
	

	/**
	 * Highlight or unhighlight the node
	 * @param graphStruct GEStreamNode to be highlighted or unhighlighted
	 * @param doHighlight The node gets highlighted when true, and unhighlighted when false.
	 */
	public void highlight(GraphStructure graphStruct, boolean doHighlight)
	{
		Map change = GraphConstants.createMap();

		if (doHighlight)
		{
			GraphConstants.setBorderColor(change, Color.yellow);
			GraphConstants.setLineWidth(change, 4);
		}
		else
		{
			if ((this instanceof GEPhasedFilter) || (this instanceof GESplitter) || (this instanceof GEJoiner))
			{
				GraphConstants.setBorderColor(change, Color.white);
			}
			else if (this instanceof GEPipeline)
			{
				GraphConstants.setBorderColor(change, Color.red.darker());
			}
			else if (this instanceof GESplitJoin)
			{
				GraphConstants.setBorderColor(change, Color.blue.darker());
			}
			else if (this instanceof GEFeedbackLoop)
			{
				GraphConstants.setBorderColor(change, Color.green.darker());
			}
			
			//GraphConstants.setRemoveAttributes(this.attributes, new Object[] {GraphConstants.BORDER,GraphConstants.BORDERCOLOR});
		}
	
		Map nest = new Hashtable ();
		nest.put(this, change);
		graphStruct.getGraphModel().edit(nest, null, null, null);
	}
	
	/**
	 * Get the Properties of the GEStreamNode.
	 * @param node GEStreamNode
	 * @return Properties of GeStreamNode. 
	 */
	public Properties getNodeProperties()
	{
		String type = this.getType();
		Properties properties = new Properties();
		
		/** Get the name, type, input/output types, level properties from this ndoe */
		properties.put(GEProperties.KEY_NAME, this.getName());
		properties.put(GEProperties.KEY_TYPE, type);
		properties.put(GEProperties.KEY_INPUT_TAPE, this.getInputTape());
		properties.put(GEProperties.KEY_OUTPUT_TAPE, this.getOutputTape());
		properties.put(GEProperties.KEY_LEVEL, Integer.toString(this.getDepthLevel()));
	
		if (this.isNodeConnected())
		{
			properties.put(GEProperties.KEY_IS_CONNECTED, Constants.CONNECTED);	
		}
		else
		{
			properties.put(GEProperties.KEY_IS_CONNECTED, Constants.DISCONNECTED);
		}
		
		GEContainer container = this.getEncapsulatingNode();
		if (container != null)
		{
			/** If the node has an encapsulating node, then set this property */ 
			properties.put(GEProperties.KEY_PARENT, ((GEStreamNode) container).getNameWithID());
		
			/** If the encapsulating node is a splitjoin, then set the index property */
			if (container.getType() == GEType.SPLIT_JOIN)
			{
				properties.put(GEProperties.KEY_INDEX_IN_SJ, 
								Integer.toString(((GESplitJoin) container).getIndexOfInnerNode(this)));
				return properties;
			}	
		}
		properties.put(GEProperties.KEY_INDEX_IN_SJ, "-1");
		return properties;
	}
	
	
	/**
	 * Set the properties of the GEStreamNode specified by the properties argument.  
	 * @param properties Properties
	 * @param jgraph JGraph 
	 * @param containerNodes ContainerNodes
	 */
	public void setNodeProperties(Properties properties, JGraph jgraph, ContainerNodes containerNodes)
	{
		String type = this.getType();
		
		/** Set the name */
		this.setName(properties.getProperty(GEProperties.KEY_NAME));
		
		/** Set the input/output tapes */
		this.setInputTape(properties.getProperty(GEProperties.KEY_INPUT_TAPE));
		this.setOutputTape(properties.getProperty(GEProperties.KEY_OUTPUT_TAPE));
	
		/** Set the encapsulating node */
		GEContainer container =  containerNodes.getContainerNodeFromNameWithID(properties.getProperty(GEProperties.KEY_PARENT));
		
		
		/** If the node is a splitjoin, then we must add this node at a specified index */
		if (container.getType() == GEType.SPLIT_JOIN)
		{
			
			((GESplitJoin)container).addInnerNodeAtIndex(
											Integer.valueOf(properties.getProperty(GEProperties.KEY_INDEX_IN_SJ)).intValue(),
											this);
			((GESplitJoin)container).setDisplay(jgraph);	
		
		}
		else
		{
			this.changeParentTo(container);
		}
	}
	
	/**
	 * Change the display information of the GESplitter.
	 * @param jgraph JGraph
	 */
	public void setDisplay(JGraph jgraph)
	{
		Map change = GraphConstants.createMap();
		GraphConstants.setValue(change, this.getInfoLabel());
		Map nest = new Hashtable ();
		nest.put(this, change);
		jgraph.getModel().edit(nest, null, null, null);
	}
		
	/**
	 * Get a cloned copy of this GEStreamNode.
	 * @return Object clone of the GEStreamNode
	 */
	public Object clone() 
	{
		GEStreamNode clonedNode =  (GEStreamNode) super.clone();
		clonedNode.targetEdges = new ArrayList();
		clonedNode.sourceEdges =  new ArrayList();
		clonedNode.args = new ArrayList(this.args);
		clonedNode.name = this.name;
		return clonedNode;
	}
	
	/**
	 * Construct the GEStreamNode. The subclasses must implement this method according to
	 * their specific needs.
	 * @param graphStruct GraphStructure to which GEStreamNode belongs.
	 * @return GEStreamNode 
	 */
	abstract GEStreamNode construct(GraphStructure graphStruct, int level);


	/**
	 * Set the attributes necessary to display the GEStreamNode.
	 * @param graphStruct GraphStructure
	 * @param bounds Rectangle
	 */
	abstract public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds);

	/**
	 * Writes the textual representation of the GEStreamNode to the StringBuffer. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode.
	 * @param strBuff StringBuffer that is used to output the textual representation of the graph.
	 * @param nameList List of the names of the nodes that have already been added to the template code.  
	 */
	abstract public void outputCode(StringBuffer strBuff, ArrayList nameList);
}

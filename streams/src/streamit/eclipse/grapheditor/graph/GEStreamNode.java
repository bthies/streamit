/*
 * Created on Jun 20, 2003
 */ 
package streamit.eclipse.grapheditor.graph;
 
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

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
	 * The name without the underscore and the numbers that follow it. 
	 */
	protected String nameNoID;

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
	 * Boolean that determines if the information of the GEStreamNode is being displayed.
	 */
	protected boolean isInfoDisplayed;

	/**
	 * Boolean that determines if the GEStreamNode is connected to other elements in 
	 * the graph. isConnected is false whenever the GEStreamNode has no edges connected
	 * to it in either its expanded or collapsed state. Default value is false (the value
	 * must be set explicitly whenever the node has been connected to a different node). 
	 */


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
	
	/**
	 * The level at which the GEStreamNode is located (how deep the GEStreamNode is with 
	 * respect to the TopLevel node). The TopLevel node is at level zero, so 
	 * every immediate node it contains is at level 1. The elements of containers at level 1
	 * will have level corresponding to 2, and so on. The default value of the level is 1 
	 * (so that the node's parent will be the Toplevel node).
	 */
	protected int level;


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
		this.setInfo(name);
		this.isInfoDisplayed = true;
		this.encapsulatingNode = null;
		this.sourceEdges = new ArrayList();
		this.targetEdges = new ArrayList();
		this.args = new ArrayList();
		this.inputTape =  Constants.VOID;
		this.outputTape = Constants.VOID;
	
		this.level = 1;
		setNameNoID();
	}

	/**
	 * Set the name without the ID (this means without the underscore
	 * and without the numbers that follow the underscore.
	 */
	private void setNameNoID()
	{
		int indexUnderscore = this.name.lastIndexOf("_");
		if (indexUnderscore != -1)
		{
			this.nameNoID = this.name.substring(0,indexUnderscore); 
		}
		else
		{
			this.nameNoID = this.name;
		}	
	}

	/**
 	 * Get the name of this GEStreamNode.
 	 * @return The name of this GEStreamNode.
	 */
	public String getName()
	{
		return this.name;	 
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Get the name with no ID (without the underscore 
	 * and the numbers that follow it).
	 * @return The name with no ID of this GEStreamNode.
	 */
	public String getNameNoID()
	{
		return this.nameNoID;
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
	 * either its expanded or collapsed states. Otherwise, return false.
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
	 * Get the depth level at which this is located
	 * @return level of the GEStreamNode
	 */
	public int getDepthLevel()
	{
		//return this.level;
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
	 * Set the depth level at which this is located
	 * @param lvl Level to which the GEStreamNode is locate.
	 */
	public void setDepthLevel(int lvl)
	{
		this.level = lvl;
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
	
	public Dimension getDimension()
	{
		return GraphConstants.getSize(this.attributes);
	}
	
	public void setDimension(Dimension dim)
	{
		GraphConstants.setSize(this.attributes, dim);
	}
	
	public Point getLocation()
	{
		return GraphConstants.getOffset(this.attributes);
	}
	
	public void setLocation(Point loc)
	{
		GraphConstants.setOffset(this.attributes, loc);
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
	 * @return Object[] array with the names of the nodes requested
	 */
	public static Object [] getNodeNames(ArrayList nodeList)
	{
		Iterator allContIter = nodeList.iterator();
		ArrayList names = new ArrayList();
		while(allContIter.hasNext())
		{	
			names.add(((GEStreamNode)allContIter.next()).name);
		}
		return names.toArray();
		
		
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


	public void deleteNode(GraphModel model)
	{
		GEContainer parent = this.getEncapsulatingNode();
					
		if (parent != null)
		{
			 parent.removeNodeFromContainer(this);
		}

		Iterator sourceIter = this.getSourceEdges().iterator();
		Iterator targetIter = this.getTargetEdges().iterator();

		/** 
		 * Must remove the source edge of the node to be deleted from
		 * the list of target edges of the edge's target node.  
		 */
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

		/** 
		 * Must remove the target edge of the node to be deleted from
		 * the list of source edges of the edge's source node.  
		 */					
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
		
		model.remove(this.getSourceEdges().toArray());
		model.remove(this.getTargetEdges().toArray());
	}


	/**
	 * Highlight or unhighlight the node
	 * @param graphStruct GEStreamNode to be highlighted or unhighlighted
	 * @param doHighlight The node gets highlighted when true, and unhighlighted when false.
	 */
	public void highlight(GraphStructure graphStruct, boolean doHighlight)
	{
		Map change = GraphConstants.createMap();
		
		
		//System.out.println("ENTERED HIGHLIGHT NODE CODE");
		//if (doHighlight)
		//demoadd
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
	 */
	abstract public void outputCode(StringBuffer strBuff);
}

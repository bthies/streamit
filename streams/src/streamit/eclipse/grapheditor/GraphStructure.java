/*
 * Created on Jun 18, 2003
 */

package grapheditor;

import java.util.*;
import java.io.*;

//import com.sun.rsasign.t;
import com.jgraph.graph.*;
import com.jgraph.JGraph;

/**
 * Graph data structure that has GEStreamNode objects as its nodes. 
 * @author jcarlos
 */
public class GraphStructure implements Serializable{
	
	private HashMap graph;
	private ConnectionSet cs;
	private ArrayList cells;
	private Map globalAttributes;
	private DefaultGraphModel model;
	private JGraph jgraph;
	
	// The toplevel GEStreamNode. Typically it should be a GEPipeline object.  
	private GEStreamNode topLevel;

	
	public GraphStructure(ArrayList nodes)
	{
		for (int i = 0;  i < nodes.size(); i++)
		{
			GEStreamNode n = (GEStreamNode) nodes.get(i);
			this.graph.put(n, n.getSuccesors());
		}
		
		cs = new ConnectionSet();
		cells = new ArrayList();
		globalAttributes= new Hashtable();
	}
	
	public GraphStructure()
	{
		graph = new HashMap();
		cs = new ConnectionSet();
		cells = new ArrayList();
		globalAttributes= new Hashtable();
		model = new DefaultGraphModel();
		jgraph = new JGraph(model);
	}
	
	/**
	 * Create hierarchy in which <parenNode> encapsulates <children>
	 * @param parentNode
	 * @param children
	 */
	public void addHierarchy(GEStreamNode parentNode, ArrayList children)
	{
		this.graph.put(parentNode, children);
	}
			
	/**
	 * Add <node> that will be a child node of the node <parent>.
	 */
	public void addNode(GEStreamNode node, GEStreamNode parent, int index)
	{
		ArrayList nodeList = this.getSuccesors(parent); 
		nodeList.add(index, node);
	}
	
	/**
	 * Delete <node> and all of the children belonging to that node
	 */ 	
	public void deleteNode(GEStreamNode node)
	{
		ArrayList nodeList = this.getSuccesors(node);
		int listSize = nodeList.size();
		
		for (int i = 0; i < listSize; i++)
		{
			GEStreamNode n = (GEStreamNode) nodeList.get(i);
			this.graph.remove(n);
		}
		this.graph.remove(node);
	}
	
	
	/**
	 * Get the children of <node>
	 * @return ArrayList with the children of <node>
	 */ 
	public ArrayList getSuccesors(GEStreamNode node)
	{
		return (ArrayList) this.graph.get(node);
	}
	
	/**
	 * Construct graph so that it could be drawn by a GUI component
	 */
	public void constructGraph()
	{
		this.topLevel.construct(this);
		model.insert(cells.toArray(), globalAttributes, cs, null, null);
	}
	
	
	
	/** 
	 * Sets the toplevel node to <strNode>
	 * @param strNode
	 */
	public void setTopLevel(GEStreamNode strNode)
	{
		this.topLevel = strNode;
	}
	
	/**
	 * Gets the toplevel node
	 * @return this.topLevel
	 */
	public GEStreamNode getTopLevel ()
	{
		return this.topLevel;
	}
	
	
	public void connectDraw(GEStreamNode lastNode, GEStreamNode currentNode)
	{
		DefaultEdge edge = new DefaultEdge();
		Map edgeAttrib = GraphConstants.createMap();
		globalAttributes.put(edge, edgeAttrib);
		
		GraphConstants.setLineEnd(edgeAttrib, GraphConstants.ARROW_CLASSIC);
		GraphConstants.setEndFill(edgeAttrib, true);
				
		cs.connect(edge, lastNode.getPort(), currentNode.getPort());
				
		cells.add(edge);	
	}
	
}


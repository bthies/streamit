package grapheditor;

import java.util.*;
import java.io.*;

/*
 * Created on Jun 18, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GraphStructure implements Serializable{
	
	private HashMap graph;
	private GEStreamNode parent;

	/*
	 * The Node class represents a Stream. 
	 * 
	 */
	public class StreamType{
		public static final String FILTER = "FILTER";
		public static final String PHASED_FILTER = "PHASED_FILTER"; 
		public static final String SPLITTER =  "SPLITTER";
		public static final String JOINER = "JOINER";
		public static final String WORK_FUNCTION = "WORK_FUNCTION";
		public static final String PIPELINE = "PIPELINE";
		public static final String SPLIT_JOIN = "SPLIT_JOIN";
		public static final String FEEDBACK_LOOP = "FEEDBACK_LOOP";
	}
	 
	 
	/* The Node class is the graph representation of nodes in the Streamit graph
	 * with the necessary properties (type of stream, children belonging to the node,
	 * and other properties that are specific
	 *  to the Stream type.
	 */
	 /* 
	 public class Node {
	
		private String type;
		private ArrayList nodeChildren;
		private ArrayList properties;
		
		public Node(String type, List children, ArrayList properties)
		{
			this.type = type; 
			this.nodeChildren = new ArrayList(children);
			this.properties = properties;
		}
		
		public Node(String type, List children)
		{
			this.type = type;
			this.nodeChildren = new ArrayList(children);
		}
	}
*/

	
	
	public GraphStructure(LinkedList nodes)
	{
		for (int i = 0;  i < nodes.size(); i++)
		{
			GEStreamNode n = (GEStreamNode) nodes.get(i);
			this.graph.put(n, n.getChildren());
		}
	}
	
	public GraphStructure()
	{
		graph = new HashMap();
	}
	
	
	
	
	public void addHierarchy(GEStreamNode parentNode, ArrayList children)
	{
		this.graph.put(parentNode, children);
	}
			
	// Add a node that will be a child node of the node parent.
	public void addNode(GEStreamNode node, GEStreamNode parent, int index)
	{
		ArrayList nodeList = this.getChildren(parent); 
		nodeList.add(index, node);
	}
	
	// Delete node an all of the children belonging to that node	
	public void deleteNode(GEStreamNode node)
	{
		ArrayList nodeList = this.getChildren(node);
		int listSize = nodeList.size();
		
		for (int i = 0; i < listSize; i++)
		{
			GEStreamNode n = (GEStreamNode) nodeList.get(i);
			this.graph.remove(n);
		}
		this.graph.remove(node);
	}
	
	// Provide the children of node n 
	public ArrayList getChildren(GEStreamNode n)
	{
		return (ArrayList) this.graph.get(n);
	}
	
}


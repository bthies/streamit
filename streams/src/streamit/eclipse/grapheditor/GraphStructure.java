package grapheditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;


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
public class GraphStructure {
	
	private HashMap graph;
	private Node parent;

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
	 * and other properties that are specific to the Stream type.
	 */ 
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


	
	
	public GraphStructure(LinkedList nodes)
	{
		for (int i = 0;  i < nodes.size(); i++)
		{
			Node n = (Node) nodes.get(i);
			if (i == 0)
			{
				this.parent = n; 
			}
			this.graph.put(n, n.nodeChildren);
		}
	}
	
	// Add a node that will be a child node of the node parent.
	
	public void addNode(Node n, Node parent)
	{
		
	}
	
	// Delete node an all of the children belonging to that node	
	public void deleteNode(Node n)
	{
	}
	
	// Provide the children of node n that are to be expanded in the graph representation.
	public ArrayList expandNode(Node n)
	{
		return new ArrayList();
	}

	// Provide the children of node n that are to be collapsed in the graph representation.
	public ArrayList collapseNode(Node n)	
	{
		return new ArrayList();
	}
}


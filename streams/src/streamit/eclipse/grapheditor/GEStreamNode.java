/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */ 
package grapheditor; 
import java.util.*;
import java.io.*;

import com.jgraph.graph.*;
import com.jgraph.graph.DefaultGraphCell;

/**
 * GEStremaNode is the graph internal representation of a node. .
 * @author jcarlos
 */
public abstract class GEStreamNode extends DefaultGraphCell implements Serializable{
	
	protected ArrayList children;
	protected String type;
	protected String name;
	protected DefaultPort port;
	protected GEStreamNode encapsulatingNode;


	public GEStreamNode(String type, String name)
	{
		super("<HTML><H5>"+name+"</H5></html>");
		System.out.println("Constructing the stream node");
		this.type = type;
		this.children = new ArrayList();
		this.name = name;
		this.encapsulatingNode = null;
	}

	/**
 	 * Add a child to this GEStreamNode 
 	 */
	public boolean addChild(GEStreamNode strNode)
	{
		return this.children.add(strNode);
	}
		
	/**
 	 * Get the children of <this>
 	 * @return An ArrayList with the children of the GEStreamNode. 
 	 */
	public ArrayList getSuccesors()
	{
		return this.children;
	}

	/**
 	 * Get the name of <this> 
 	 * @return The name of this GEStreamNode.
	 */
	public String getName()
	{
		return this.name;	 
	}
	
	/**
 	* Get the type of <this>
 	* @return The type of this GEStreamNode.
 	*/	
	public String getType()
	{
		return this.type;	
	}
	/**
	 * Get the port of <this>
	 * @return The port that corresponds to the GEStreamNode
	 */
	public Port getPort()
	{
		return this.port;
	}
	
	/**
	 * Sets the node that encapsulates this
	 * @param node The GEStreamNode that encapsulates this
	 */
	public void setEncapsulatingNode(GEStreamNode node)
	{
		this.encapsulatingNode = node;
	}
	
	/**
	 * Gets the encapsulating node of this
	 * @return The encapsulating node of GEStreamNode
	 */
	public GEStreamNode getEncapsulatingNode()
	{
		return this.encapsulatingNode;
	}
	
	

	abstract public void draw();
	abstract GEStreamNode construct(GraphStructure graphStruct);
	abstract public void collapse();
	
}

/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */ 
package grapheditor;
import java.util.*;
import java.io.*;

/**
 * GEStremaNode is the graph internal representation of a node. .
 * @author jcarlos
 */
public abstract class GEStreamNode implements Serializable{
	
	protected ArrayList children;
	protected String type;
	protected String name;

	public GEStreamNode(String type, String name)
	{
		this.type = type;
		this.children = new ArrayList();
		this.name = name;
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
	public ArrayList getChildren()
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

	abstract public void draw();
	abstract public void construct();

	
}

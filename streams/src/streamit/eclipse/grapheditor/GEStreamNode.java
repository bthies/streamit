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

	// Add the next child belonging to the stream node
	public boolean addChild(GEStreamNode strNode)
	{
		return true;
	}
	
	// Return the children of the stream node
	public ArrayList getChildren()
	{
		return this.children;
	}

	public String getName()
	{
		return this.name;
	}

	
	abstract public void draw();
	abstract public void construct();

	
}

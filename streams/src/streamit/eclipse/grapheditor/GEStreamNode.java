/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */ 
package grapheditor;
import java.util.*;

/**
 * GEStremaNode is the graph internal representation of a node. .
 * @author jcarlos
 */
public class GEStreamNode {
	
	protected ArrayList children;
	protected String type;
	protected String label;

	public GEStreamNode(String type, String label)
	{
		this.type = type;
		this.children = new ArrayList();
		this.label = label;
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
	
}

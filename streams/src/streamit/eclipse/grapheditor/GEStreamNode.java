/*
 * Created on Jun 20, 2003
 */ 
package streamit.eclipse.grapheditor;
 
import java.util.*;
import java.io.*;
import com.jgraph.graph.*;
import com.jgraph.graph.DefaultGraphCell;
import com.jgraph.JGraph;

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
	 * The children of the GEStreamNode. 
	 */
	protected ArrayList children;
	
	/**
	 * The immediate GEStreamNode that contains this GEStreamNode.
	 */
	protected GEStreamNode encapsulatingNode;	
	
	
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


	protected ArrayList sourceEdges;
	protected ArrayList targetEdges;

	/**
	 * GEStreamNode constructor.
	 * @param type The type of the GEStreamNode (must be defined as a GEType)
	 * @param name The name of the GEStreamNode.
	 */
	public GEStreamNode(String type, String name)
	{
		super("<HTML><H5>"+name+"</H5></html>");
		this.type = type;
		this.children = new ArrayList();
		this.name = name;
		this.setInfo(name);
		this.isInfoDisplayed = true;
		this.encapsulatingNode = null;
		this.sourceEdges = new ArrayList();
		this.targetEdges = new ArrayList();
	}

	/**
 	 * Add a child to this GEStreamNode.
 	 * @return True if teh child was added succesfully, otherwise false.
 	 */
	public boolean addChild(GEStreamNode strNode)
	{
		return this.children.add(strNode);
	}
		
	/**
 	 * Get the children of <this>.
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
 	* Get the type of <this>.
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
	
	/**
	 * Get the info of <this>.
	 * @return The info of this GEStreamNode.
	 */
	public String getInfo()
	{
		return this.info;	
	}
	
	/**
	 * Get the info of <this> as an HTML label.
	 * @return The info of the GEStreamNode as an HTML label.
	 */
	public String getInfoLabel()
	{
		return new String("<HTML><H5>"+ this.name + "<BR>" + this.info + "</H5></HTML>");
	}
	
	/**
	 * Get the name of <this> as an HTML label.
	 * @return The name of the GEStreamNode as an HTML label.
	 */
	public String getNameLabel()
	{
		return new String("<HTML><H5>"+ this.name + "</H5></HTML>");
	}
	
	/**
	 * Set the info of <this>.
	 * @param info Set the info of the GEStreamNode.
	 */
	public void setInfo(String info)
	{
		this.info = info;
	}
	
	public void addTargetEdge(DefaultEdge target)
	{
		this.targetEdges.add(target);
	}
	
	public void addSourceEdge(DefaultEdge source)
	{
		this.sourceEdges.add(source);
	}
	
	public void removeTargetEdge(DefaultEdge target)
	{
		this.targetEdges.remove(target);
	}
	
	public void removeSourceEdge(DefaultEdge source)
	{
		this.sourceEdges.remove(source);
	}

	
	
	
	public ArrayList getTargetEdges()
	{
		return this.targetEdges;	
	}
	
	public ArrayList getSourceEdges()
	{
		return this.sourceEdges;
	}
	
	/**
	 * Construct the GEStreamNode. The subclasses must implement this method according to
	 * their specific needs.
	 * @param graphStruct GraphStructure to which GEStreamNode belongs.
	 * @return GEStreamNode 
	 */
	abstract GEStreamNode construct(GraphStructure graphStruct, int level);
	
	/**
	 * Expand or collapse the GEStreamNode structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph The JGraph that will be modified to allow the expanding/collapsing.
	 */	
	abstract public void collapseExpand(JGraph jgraph);
	abstract public void collapse(JGraph jgraph);
	abstract public void expand(JGraph jgraph);
	
	abstract public void draw();
	
}

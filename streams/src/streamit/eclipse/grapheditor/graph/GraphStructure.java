/*
 * Created on Jun 18, 2003
 */

package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JScrollPane;

import org.eclipse.core.resources.IFile;
import org.jgraph.JGraph;
import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.PortView;
//import com.sun.rsasign.t;

/**
 * Graph data structure that has GEStreamNode objects as its nodes. Relies on JGraph 
 * components for its representation and rendering on the screen. 
 * @author jcarlos
 */
public class GraphStructure implements Serializable{


	/**
	 * The graph model.
	 */
	private DefaultGraphModel model;
	
	/**
	 * The JGraph representation that will be used to do the drawing of the graph.
	 */
	private JGraph jgraph;

	/**
	 * Specifies the connections that are present in the JGraph. Necessary
	 * to draw the graph.
	 */
	private ConnectionSet cs;
	
	/**
	 * Specifies the cells that are present in the JGraph. Necessary to draw the graph.
	 */
	private ArrayList cells;
	
	/**
	 * Specifies attributes required by the JGraph.
	 */
	private Hashtable globalAttributes;
	
	/**
	 * The toplevel GEStreamNode. Typically it should be a GEPipeline object.
	 */   
	private GEStreamNode topLevel;


	/**
	 * The node in the GraphStructure that is currently highlighted.
	 */
	private GEStreamNode highlightedNode = null;
	

	private ArrayList highlightedNodes =null;

	/**
	 * Specifies container objects (i.e. pipelines, splitjoins, feedback loops) at each 
	 * different level of depth. Level of depth increases as the GEStreamNode is contained
	 * by a container object that is also contained by another container object and so on.
	 * As a consequence, the toplevel GEStreamNode has level zero.
	 * The map has the levels as its keys, and the cotnainer objects within that level as the
	 * values of the map.
	 */
	private HashMap levelContainers;
	
	/**
	 * The current level at which the graph is being examined;
	 */
	public int currentLevelView = 0;


	private IFile ifile = null;

	
	public ArrayList allContainers;
			
	// does not appear to be necessary if this.model and the other JGraph fields
	// will be used to specify the GraphStructure.
	private HashMap graph;



	public JScrollPane panel;
	
	/**
	 * GraphStructure contructor that initializes all of its fields.
	 */	
	public GraphStructure()
	{
		graph = new HashMap();
		levelContainers = new HashMap();
		cs = new ConnectionSet();
		cells = new ArrayList();
		globalAttributes= new Hashtable();
		model = new DefaultGraphModel();
		jgraph = new JGraph(model);
		allContainers =  new ArrayList();
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph, this));
	
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
	 * Get all of the nodes in the graph
	 * @return ArrayList with all of the nodes in the graph.
	 */
	public ArrayList allNodesInGraph()
	{
		ArrayList allNodes = new ArrayList();
		allNodes.add(this.topLevel);
		Iterator containerIter = this.allContainers.iterator();
		while(containerIter.hasNext())
		{
			Iterator iterChild = ((GEStreamNode)containerIter.next()).getContainedElements().iterator();
			while(iterChild.hasNext())
			{
				allNodes.add(iterChild.next());	
			}			
	
		}
		
		return allNodes;
	}
	
	/**
	 * Connect startNode to endNode if it is a valid connection.
	 * @param startNode 
	 * @param endNode
	 * @param nodesConnected Determines which of the nodes are already connected (present)
	 * in the graph.
	 * @return True if it was possible to connect the nodes, false otherwise.
	 */
	public boolean connect (GEStreamNode startNode, GEStreamNode endNode, int nodesConnected)
	{

		GEStreamNode startParent = startNode.getEncapsulatingNode();
		GEStreamNode endParent = endNode.getEncapsulatingNode();
		
		if ((startParent == endNode) || (endParent == startNode) || (startParent == null) || (endParent == null))
		{
			return false;
		}
		
		if (startParent.getType() == GEType.PIPELINE)
		{
			ArrayList startParentChildren = startParent.getSuccesors();
			ArrayList endParentChildren = endParent.getSuccesors();
			
			switch(nodesConnected)
			{
				case RelativePosition.START_PRESENT:
				{	
					if (endParent == startParent)
					{
						//TODO Add all of the nodes that are connected after endNode
						startParentChildren.add(endNode);
					}
					else
					{
						int startIndex =  startParentChildren.indexOf(startNode);
						startParentChildren.add(startIndex + 1, endNode);
						
					}
					
					break;
				}
				case RelativePosition.END_PRESENT:
				{
					if (endParent == startParent)
					{
						//TODO Add all of the nodes that are connected before startNode
						int endNodeIndex = startParentChildren.indexOf(endNode);
						int addedAtIndex = endNodeIndex == 0 ? 0 : endNodeIndex - 1;
						startParentChildren.add(addedAtIndex, startNode);
					}
					else
					{
						//TODO
						int endNodeIndex = startParentChildren.indexOf(endParent);
						int addedAtIndex = endNodeIndex == 0 ? 0 : endNodeIndex - 1;
						startParentChildren.add(addedAtIndex, startNode);
											
					}
					break;
				}
				case RelativePosition.BOTH_PRESENT:
				{
					if ((endParent.getType() == GEType.PIPELINE) || 
						(endParent.getType() == GEType.FEEDBACK_LOOP) ||
						(endParent.getType() == GEType.SPLIT_JOIN))
						{
							int startNodeIndex = startParentChildren.indexOf(startNode);
							startParentChildren.add(startNodeIndex + 1, endParent);
						}
					break;
				}
				
				case RelativePosition.NONE_PRESENT:
				{
					startParentChildren.add(endNode);
					startParentChildren.add(startNode);
					break;
				}
			}
			connectDraw(startNode, endNode);
			
		}
		else if (startParent.getType() == GEType.SPLIT_JOIN)
		{
			GESplitter splitter = ((GESplitJoin) startParent).getSplitter();
			GEJoiner joiner =  ((GESplitJoin) startParent).getJoiner();
			
			switch(nodesConnected)
			{
				
				case RelativePosition.START_PRESENT:
				{
					System.out.println("BEFORE THE TEST splitter");
					
					/** The startNode always has to be the splitter. 
					 * Cannot connect from the splitter to a node outside the parent SplitJoin **/
					if ((startNode == splitter) && (startParent == endParent))
					{
							
						System.out.println("THE STARTNODE EQUALS THE SPLITTER");
						startParent.addChild(endNode);
						connectDraw(splitter, endNode);
						connectDraw(endNode, joiner);
						break;			
					}
					else if (startNode == joiner)
					{ 
						GEStreamNode grandparent = startParent.getEncapsulatingNode();
						if (grandparent != null)
						{
							ArrayList grandparentChildList = grandparent.getSuccesors();
							int parentIndex = grandparentChildList.indexOf(startParent);
							grandparentChildList.add(parentIndex, endNode);
							connectDraw(startNode, endNode);
							break;
						}
						else
						{
							return false;
						}
					}
					else
					{	
						return false;
					}
					
				}
				case RelativePosition.END_PRESENT:
				{
					System.out.println("BEFORE THE TEST joiner");
					if (endNode == joiner)
					{
						System.out.println("THE ENDNODE EQUALS THE JOINER");
						startParent.addChild(startNode);
						connectDraw(splitter, startNode);
						connectDraw(startNode, joiner);
						break;
					}	
					else {
						return false;
					}
				}
				case RelativePosition.NONE_PRESENT:
				case RelativePosition.BOTH_PRESENT:
				default:
				{
					return false;	
				}
			}
			
		}
		else if (startParent.getType() == GEType.FEEDBACK_LOOP)
		{
			return false;
		}
		else
		{
			System.out.println("ERROR : The parent type is invalid");
			return false;
		}

		return true;
	}

	/**
	 * Delete <node> and all of the children belonging to that node
	 */ 	
	public void deleteNode(GEStreamNode node)
	{
		/*
		ArrayList nodeList = this.getSuccesors(node);
		int listSize = nodeList.size();
		
		for (int i = 0; i < listSize; i++)
		{
			GEStreamNode n = (GEStreamNode) nodeList.get(i);
			this.graph.remove(n);
		}
		this.graph.remove(node);
		*/
	}
	
	/*
	/**
	 * Construct graph representation.
	 */
	public void constructGraph()
	{
			System.out.println("Entered constructGraph");
			this.jgraph.addMouseListener(new JGraphMouseAdapter(jgraph, this));
			this.topLevel.construct(this, 0);
			model.insert(cells.toArray(), globalAttributes, cs, null, null);
	}
	
	public void constructGraph(JScrollPane pane)
	{
		System.out.println("Constructor with pane as an argument");
		this.panel = pane;
		this.topLevel.setIsNodeConnected(true);
		this.topLevel.construct(this, 0);
		model.insert(cells.toArray(), globalAttributes, cs, null, null);
	//	this.jgraph.getGraphLayoutCache().setVisible(jgraph.getRoots(), true);	
				
				
				
		int i = 0;
		while(this.expandContainersAtLevel(i))
		{
			i++;
			this.currentLevelView++;
		}
				
		//******************************************
		// TEST CODE BEGIN
		//******************************************	
		/*	
		Iterator keyIter = this.levelContainers.keySet().iterator();
		Iterator valIter = this.levelContainers.values().iterator();
		
		while(keyIter.hasNext())
		{
			System.out.println("Key = " + keyIter.next());	
		}
		int x =0;
		while(valIter.hasNext())
		{
			
			Iterator  listIter = ((ArrayList) valIter.next()).iterator();
			while (listIter.hasNext())
			{
			
				System.out.println("Iter = " + x + " value = "+listIter.next());
			}
			x++;	
		}*/
		//******************************************
		// TEST CODE END
		//******************************************		
								
	}
	
	/**
	 * Establishes a connection between <lastNode> and <currentNode>.
	 * @param lastNode GEStreamNode that is source of connection.
	 * @param currentNode GEStreamNode that is targetr of connection.
	 */
	public void connectDraw(GEStreamNode lastNode, GEStreamNode currentNode)
	{
		DefaultEdge edge = new DefaultEdge(); 
			
		Map edgeAttrib = GraphConstants.createMap();
		globalAttributes.put(edge, edgeAttrib);
		
		GraphConstants.setLineEnd(edgeAttrib, GraphConstants.ARROW_CLASSIC);
		GraphConstants.setLineWidth(edgeAttrib, 6);
		GraphConstants.setEndFill(edgeAttrib, true);
		GraphConstants.setDashPattern(edgeAttrib, new float[] {2,4});
		
		
		cs.connect(edge, lastNode.getPort(), currentNode.getPort());
		
		lastNode.addSourceEdge(edge);
		currentNode.addTargetEdge(edge);		
		cells.add(edge);	
		
		lastNode.setIsNodeConnected(true);
		currentNode.setIsNodeConnected(true);
		
		this.getGraphModel().insert(new Object[] {edge}, null, null, null, null);
		this.getGraphModel().edit(globalAttributes, cs, null, null);
		//TODO: Not sure if this causes double adding when graph is first created 
		//this.getGraphModel().insert(this.getCells().toArray(),
		//								this.getAttributes(), 
		//								this.getConnectionSet(), null, null);
	}
	
	public void connectDraw(PortView lastNode, PortView currentNode)
	{
		DefaultEdge edge = new DefaultEdge();
		Map edgeAttrib = GraphConstants.createMap();
		globalAttributes.put(edge, edgeAttrib);
		
		GraphConstants.setLineEnd(edgeAttrib, GraphConstants.ARROW_CLASSIC);
		GraphConstants.setLineWidth(edgeAttrib, 6);
		GraphConstants.setDashPattern(edgeAttrib, new float[] {2,4});
		GraphConstants.setEndFill(edgeAttrib, true);
			
		cs.connect(edge, lastNode.getCell(), currentNode.getCell());
		
		//lastNode.addSourceEdge(edge);
		//currentNode.addTargetEdge(edge);		
		cells.add(edge);	
		
		this.getGraphModel().insert(this.getCells().toArray(),
									this.getAttributes(), 
									this.getConnectionSet(), null, null);
	}
	
	
	/**
	 * Add the container node at level. 
	 * @param level Level at which the Container will be added.
	 * @param node Container node (GEPipeline, GESplitJoin, GEFeedbackLoop)
	 */
	public void addToLevelContainer(int level, GEStreamNode node)
	{
		ArrayList levelList = null;
		if(this.levelContainers.get(new Integer(level)) == null)
		{
			levelList = new ArrayList();
			levelList.add(node);
			this.levelContainers.put(new Integer(level), levelList);
		}
		else
		{
			levelList = (ArrayList) this.levelContainers.get(new Integer(level));
			levelList.add(node);
		}
		this.allContainers.add(node);
		
	}
	
	/**
	 * Expand all of the container nodes located at level.
	 * @param level The level at which all containers will be expanded.
	 * @return True there were elements at the level to be expanded, false otherwise
	 */		
	public boolean expandContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				 GEStreamNode node = (GEStreamNode) listIter.next();
				 
				 if (node.getSuccesors().size() != 0)
				 {
				 	node.expand(this.getJGraph());
				 }
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Collapse all of the container nodes located at level.
	 * @param level The level at which all containers will be collapsed.
	 */		
	public void collapseContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				 GEStreamNode node = (GEStreamNode) listIter.next();
				 if (node.getSuccesors().size() != 0 )
				 {
				 	node.collapse(this.getJGraph());
				 }
			}
		}		
	}

	/**
	 * Make invisible all of the container nodes located at level.
	 * @param level The level at which all containers will be made invisible.
	 */	
	public void hideContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				 GEStreamNode node = (GEStreamNode) listIter.next();
				 node.hide();
			}
		}				
	}
	
	/**
	 * Make visible all of the container nodes located at level.
	 * @param level The level at which all containers will be made visible.
	 */
	public void unhideContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				 GEStreamNode node = (GEStreamNode) listIter.next();
				 node.unhide();
			}
		}				
	}
	

	/**
	 * Highlight the GEStreamNode. 
	 * @param strNode GEStreamNode to be highlighted. 
	 */
	public void highlightNodes(ArrayList nodesToHighLight)
	{
		if (highlightedNodes != null)
		{
			Iterator hlIter = highlightedNodes.iterator();
			while (hlIter.hasNext())
			{
				((GEStreamNode) hlIter.next()).highlight(this, false);
			}
			
		}
		
		Iterator hIter = nodesToHighLight.iterator();
		while (hIter.hasNext())
		{
			((GEStreamNode) hIter.next()).highlight(this, true);
		}
		
		highlightedNodes = nodesToHighLight;
	}
	
	/**
	 * Return all of the container nodes (GEPipeline, GESplitJoin, GEFeedbackLoop) 
	 * that the GraphStructure contains.
	 * @return Object array with the container nodes
	 */
	public ArrayList getAllContainers()
	{	
		//return this.levelContainers.values().toArray();
		return this.allContainers;
	}
	
	public Object[] getAllContainerNames()
	{
		Iterator allContIter = this.allContainers.iterator();
		ArrayList names = new ArrayList();
		while(allContIter.hasNext())
		{	
			names.add(((GEStreamNode)allContIter.next()).name);
		}
		return names.toArray();
	}
	
	public GEStreamNode getContainerNodeFromName(String name)
	{	
		if (name == "Toplevel")
		{
			System.out.println("Returning toplevel from getContainerNodeFromName");
			return this.topLevel;
		}
		
		
		Iterator aIter = this.allContainers.iterator();
		GEStreamNode node = null;
		while(aIter.hasNext())
		{
			node = (GEStreamNode) aIter.next();
			if (name == node.getName())
			{
				System.out.println("Returning node from getContainerNodeFromName");
				return node;
			}
		}
		
		System.out.println("Returning null from getContainerNodeFromName");
		return null;
		
	}
	
	
	/**
	 * Sets the location of the Container nodes at level. The bounds of the container
	 * node are set in such a way that the elements that it contains are enclosed.
	 * Also, changes the location of the label so that it is more easily viewable.
	 * @param level The level of the containers whose location will be set. 
	 */
	
	public void setLocationContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				GEStreamNode node = (GEStreamNode) listIter.next();
				System.out.println("node is "+ node);
				this.jgraph.getGraphLayoutCache().setVisible(new Object[]{node}, true);
				Object[] containedCells = node.getContainedElements().toArray();
		
				CellView[] containedCellViews = 
					this.jgraph.getGraphLayoutCache().getMapping(containedCells);

				Rectangle cellBounds = AbstractCellView.getBounds(containedCellViews);
				
				
				GraphConstants.setVerticalAlignment(node.getAttributes(), 1);
				GraphConstants.setAutoSize(node.getAttributes(), false);
				if (cellBounds != null)
				{
					cellBounds.height += 60;
					cellBounds.width += 60;
					GraphConstants.setBounds(node.getAttributes(), cellBounds);
				}
				
				// The lines below are supposed to change label location, but they don't
				//GraphConstants.setValue(node.getAttributes(), "hello");
				//GraphConstants.setHorizontalAlignment(node.getAttributes(), 1);
				
				this.model.edit(this.getAttributes(), null , null, null);
			}
		}
	}
	
	/**
	 * Get the JGraph of GraphStructure.
	 * @return this.jgraph
	 */
	public JGraph getJGraph()
	{
		return this.jgraph;
	}
	
	/**
 	 * Set the JGraph of GraphStructure to <jgraph>.
	 * @param jgraph
 	 */
	public void setJGraph(JGraph jgraph)
	{
		this.jgraph = jgraph;
	}
	
	/**
	 * Gets the graph model of the GraphStructure.
	 * @return this.model
	 */
	public DefaultGraphModel getGraphModel()
	{
		return this.model;
	}

	/**
	 * Sets the graph model to <model>.
	 * @param model
	 */
	public void setGraphModel(DefaultGraphModel model)
	{
		this.model = model; 
	}

	/**
	 * Gets the toplevel node.
	 * @return this.topLevel
	 */
	public GEStreamNode getTopLevel ()
	{
		return this.topLevel;
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
	 * Get the current level of expansion/collapsing of the GraphStructure.
	 * @return this.currentLevelView
	 */
	public int getCurrentLevelView()
	{
		return this.currentLevelView;
	}
	
	/**
	 * Sets the level at which the Graph Structure is to be expanded/collapsed. 
	 * @param levelView 
	 */	
	public void setCurrentLevelView(int levelView)
	{
		this.currentLevelView = levelView;
	}

	/**
	 * Get the global attributes of the GraphStructure.
	 * @return this.globalAttributes
	 */
	public Hashtable getAttributes()
	{
		return this.globalAttributes;
	}

	/**
	 * Get the cells of the GraphStructure.
	 * @return this.cells
	 */
	public ArrayList getCells()
	{
		return this.cells;
	}
		
	/**
	 * Get the connection set of GraphStructure.
	 * @return this.cs;
	 */
	public ConnectionSet getConnectionSet()
	{
		return this.cs;
	}
	
	public IFile getIFile()
	{
		return this.ifile;
	}
	
	public void setIFile(IFile ifile)
	{
		this.ifile = ifile;
	}
	
	
	/**
	 * Get the children of <node>
	 * @return ArrayList with the children of <node>
	 */ 
	/*
	public ArrayList getSuccesors(GEStreamNode node)
	{
		return (ArrayList) this.graph.get(node);
	}
	*/
	
	public void outputCode(PrintWriter out)
	{
	    this.topLevel.outputCode(out);
	    
	    ArrayList childList = this.topLevel.children;
	    Iterator childIter = childList.iterator();
	    
	    while (childIter.hasNext())
	    {
	   		((GEStreamNode) childIter.next()).outputCode(out); 	
	    }
	    
	    
	}
	
	
/*	
	public Rectangle setRectCoords(GEStreamNode node)
	{
		Rectangle rect = new Rectangle(x, y, width, height);
		return rect;
	}
*/
}





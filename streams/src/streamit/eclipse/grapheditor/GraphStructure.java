/*
 * Created on Jun 18, 2003
 */

package grapheditor;

import java.util.*;
import java.io.*;
import java.awt.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import grapheditor.jgraphextension.*;

import javax.swing.JScrollPane;
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
	public static int currentLevelView = 0;
			
	// does not appear to be necessary if this.model and the other JGraph fields
	// will be used to specify the GraphStructure.
	private HashMap graph;
	
	private int x;
	private int y;
	private int width;
	private int height;
	
	public GraphEditorFrame editorFrame; 
	public LiveJGraphInternalFrame internalFrame;
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
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph));
		x = 0;
		y = 0;
		width = 100;
		height = 90;		
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
		// create node for GEStreamNode (if it does not already exist)
		
		// insert node inside parent at index i
		if (parent.getType() == GEType.PIPELINE)
		{ 
			ArrayList nodeList = this.getSuccesors(parent); 
		
			GEStreamNode nextNode =  (GEStreamNode) nodeList.get(index + 1);
			Iterator nextNodeEdgesIter = nextNode.getTargetEdges().iterator();
			DefaultEdge edge = (DefaultEdge) nextNodeEdgesIter.next();
			this.cs.disconnect(edge);
		
			if (index > 0 )
			{
				GEStreamNode previousNode = (GEStreamNode) nodeList.get(index -1);
				this.connectDraw(previousNode, node);
			}	
			this.connectDraw(node, nextNode);
		}
		else if (parent.getType() == GEType.SPLITTER)
		{}
		else if (parent.getType() == GEType.FEEDBACK_LOOP)
		{}
		else
		{
			System.out.println("ERROR : The parent type is invalid");
		}
	}
	
	public void createNode(String type, String name)
	{
		GEStreamNode node = null;
		
		if (type == GEType.PHASED_FILTER)
		{
			node = new GEPhasedFilter(name);
		}
		else if (type == GEType.PIPELINE)
		{
			node = new GEPipeline(name);
		}
		else if (type == GEType.SPLIT_JOIN)
		{
			
		}
		else if (type == GEType.JOINER)
		{
			node = new GEJoiner(name);
		}
		else if (type == GEType.SPLITTER)
		{
			node = new GESplitter(name);
		}
		else if (type == GEType.FEEDBACK_LOOP)
		{
			
		}
		else 
		{
			System.out.println("The type that was specified is invalid");
		}
		
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
	 * Construct graph representation.
	 */
	public void constructGraph()
	{
			System.out.println("Entered constructGraph");
			editorFrame = new GraphEditorFrame();
			System.out.println("Finished creating editorFrame");
			
			this.jgraph.addMouseListener(new JGraphMouseAdapter(jgraph));
			this.topLevel.construct(this, 0);
			model.insert(cells.toArray(), globalAttributes, cs, null, null);

			/*
			JFrame frame = new JFrame();			
			jgraph.addMouseListener(new JGraphMouseAdapter(jgraph));
			jgraph.getModel().addGraphModelListener(new JGraphModelListener());
			frame.getContentPane().add(new JScrollPane(jgraph));
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setVisible(true);
			*/
	}
	
	public void constructGraph(JScrollPane pane)
	{
		System.out.println("Constructor with pane as an argument");
		this.panel = pane;
		this.topLevel.construct(this, 0);
		model.insert(cells.toArray(), globalAttributes, cs, null, null);
	//	this.jgraph.getGraphLayoutCache().setVisible(jgraph.getRoots(), true);	
				
				
				
		//******************************************
		// TEST CODE BEGIN
		//******************************************		
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
		}
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
		GraphConstants.setEndFill(edgeAttrib, true);
				
		cs.connect(edge, lastNode.getPort(), currentNode.getPort());
		
		lastNode.addSourceEdge(edge);
		currentNode.addTargetEdge(edge);		
		cells.add(edge);	
	}
	
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
		
	}
	
	public void expandContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				 GEStreamNode node = (GEStreamNode) listIter.next();
				 node.expand(this.getJGraph());
			}
		}
	}
	
	public void collapseContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				 GEStreamNode node = (GEStreamNode) listIter.next();
				 node.collapse(this.getJGraph());
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
	
	/**
	 * Get the children of <node>
	 * @return ArrayList with the children of <node>
	 */ 
	public ArrayList getSuccesors(GEStreamNode node)
	{
		return (ArrayList) this.graph.get(node);
	}


	// TODO remove this method since it is  no longer required
	// with the addition of the layout algorithms.
	public Rectangle setRectCoords(GEStreamNode node)
	{
		Rectangle rect =  new Rectangle(x, y, width, height);
		/*
		if (node.getType() == GEType.PHASED_FILTER) {
			y+=500;	
			rect.y = y;
		}
		else if (node.getType() == GEType.SPLIT_JOIN) {
			x+= 600;
			rect.x = x;
		}
		else {		
			if (node.getEncapsulatingNode() != null) {
				if ((node.getEncapsulatingNode().getType() == GEType.SPLIT_JOIN) && 
					(node.getType() != GEType.JOINER) && 
					(node.getType() != GEType.SPLITTER))  
				{
					x += 100;
				}
				else if (node.getType() == GEType.JOINER) {
					rect.height = 400;
					rect.width = 400;
					y += 120;
					rect.y = y;
					y += 120;
				}
				else {
					y += 120;
				}
			}
			else {
				y += 120;
			}
		}
		*/
		return rect;
	}
}





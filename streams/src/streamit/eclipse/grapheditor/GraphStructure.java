/*
 * Created on Jun 18, 2003
 */

package grapheditor;

import java.util.*;
import java.io.*;

import java.awt.*;

//import com.sun.rsasign.t;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import com.jgraph.layout.*;

import javax.swing.*;
import grapheditor.jgraphextension.*;


/**
 * Graph data structure that has GEStreamNode objects as its nodes. 
 * @author jcarlos
 */
public class GraphStructure implements Serializable{
	
	private HashMap graph;
	private ConnectionSet cs;
	private ArrayList cells;
	private Hashtable globalAttributes;
	private DefaultGraphModel model;
	private JGraph jgraph;
		
	private int x;
	private int y;
	private int width;
	private int height;
	
	public GraphEditorFrame editorFrame; 
	public LiveJGraphInternalFrame internalFrame;
	
	
	// The toplevel GEStreamNode. Typically it should be a GEPipeline object.  
	private GEStreamNode topLevel;
	
	public GraphStructure()
	{
		graph = new HashMap();
		cs = new ConnectionSet();
		cells = new ArrayList();
		globalAttributes= new Hashtable();
		model = new DefaultGraphModel();
		jgraph = new JGraph(model);
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
			editorFrame = new GraphEditorFrame();
			this.topLevel.construct(this);
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
	
	public Rectangle setRectCoords(GEStreamNode node)
	{
		Rectangle rect =  new Rectangle(x, y, width, height);
		/*
		if (node.getType() == GEType.PHASED_FILTER)
		{
			System.out.println("################################## setting COORDS for FILTER");
			y+=500;
			
			rect.y = y;
		}
		else if (node.getType() == GEType.SPLIT_JOIN)
		{
			System.out.println("################################## setting COORDS for SplitJoin");
			x+= 600;
			rect.x = x;
		}
		else
		{		
			if (node.getEncapsulatingNode() != null)
			{
			
				if ((node.getEncapsulatingNode().getType() == GEType.SPLIT_JOIN) && 
			    	(node.getType() != GEType.JOINER) && 
			    	(node.getType() != GEType.SPLITTER))  
				{
					x += 100;
				}
				else if (node.getType() == GEType.JOINER)
				{
					rect.height = 400;
					rect.width = 400;
					y += 120;
					rect.y = y;
					y += 120;
				}
				
				else
				{
					y += 120;
				}
			}
			else
			{
				y += 120;
			}
		}
		*/
		return rect;
	}

	public ArrayList getCells()
	{
		return this.cells;
	}
	
	public Hashtable getAttributes()
	{
		return this.globalAttributes;
	}
	
	public JGraph getJGraph()
	{
		return this.jgraph;
	}
	
	public ConnectionSet getConnectionSet()
	{
		return this.cs;
	}
	
	public DefaultGraphModel getGraphModel()
	{
		return this.model;
	}
	
	public void setGraphModel(DefaultGraphModel model)
	{
		this.model = model; 
	}
	
	public void setJGraph(JGraph jgraph)
	{
		this.jgraph = jgraph;
	}
	
	
	
	
}





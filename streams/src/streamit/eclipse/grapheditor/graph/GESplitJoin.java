/*
 * Created on Jun 24, 2003
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;

import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.utils.JGraphLayoutManager;

/**
 * GESplitJoin is the graph internal representation of  a splitjoin. It is composed 
 * of a splitter, a joiner, and the children of the splitter which in turn are also the 
 * parents of the joiner.
 * @author jcarlos
 */
public class GESplitJoin extends GEStreamNode implements Serializable, GEContainer{

	/**
	 * The splitter belonging to this splitjoin.
	 */	
	private GESplitter splitter;
	
	/**
	 * The joiner belonging to this splitjoin.
	 */
	private GEJoiner joiner;
	
	/**
	 * All of the GEStreamNode structures inside of the GESplitJoin (not including
	 * the GESplitter and the GEJoiner).
	 */
	private ArrayList children;
	 
	/**
	 * The sub-graph structure that is contained within this SplitJoin.
	 * This subgraph is hidden when the SplitJoin is collapse and 
	 * visible when expanded. 
	 */
	private GraphStructure localGraphStruct;

	/**
	 * Boolean that specifies if the elements contained by the GESplitJoin are 
	 * displayed (it is expanded) or they are hidden (it is collapsed).
	 */
	private boolean isExpanded;

	/**
	 * The level of how deep the elements of this splitjoin are with respect to 
	 * other container nodes that they belong to.
	 * The toplevel pipeline has level 0. The elements of another pipeline within
	 * this toplevel pipeline would have its level equal to 1 (same applies to other 
	 * container nodes such as splitjoins and feedback loops).
	 */
	private int level;


	public GEStreamNode firstNode;


	/**
	 * GESplitJoin constructor.
	 * @param name The name of the GESplitJoin.
	 * @param split The GESplitter that corresponds to this GESplitjoin.
	 * @param join The GEJoiner that corresponds to this GESplitJoin.
	 */
	public GESplitJoin(String name, GESplitter split, GEJoiner join)
	{
		super(GEType.SPLIT_JOIN , name);
		this.splitter = split;
		this.joiner = join;
		this.setChildren(split.getSuccesors());
		this.localGraphStruct = new GraphStructure();
		this.isExpanded = false;

	}

	/**
	 * Set the children of <this>
	 * @param children The new value (ArrayList) of the children of GESplitJoin
	 */
	private void setChildren(ArrayList children)
	{
		this.children = children;
	}
	
	/**
	 * Get the splitter part of this
	 * @return GESplitter corresponding to this GESplitJoin
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}
	
	/**
 	 * Get the joiner part of this
	 * @return GESJoiner corresponding to this GESplitJoin
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}
	
	/**
	 * Constructs the splitjoin and returns the last node in the splitjoin that wil be connecting
	 * to the next graph structure.
	 */	
	public GEStreamNode construct(GraphStructure graphStruct, int lvel)
	{
		System.out.println("Constructing the SplitJoin " +this.getName());
		this.level = lvel;
		graphStruct.containerNodes.addContainerToLevel(level, this);
		lvel++;
		
		
		//graphStruct.getJGraph().getGraphLayoutCache().setVisible(this, true);
		
		//this.localGraphStruct.setJGraph(graphStruct.getJGraph());
		this.localGraphStruct = graphStruct;	
		
		this.splitter.construct(graphStruct, lvel); //this.splitter.construct(this.localGraphStruct); 
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
		ArrayList lastNodeList = new ArrayList();
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = ((GEStreamNode) listIter.next());
			lastNodeList.add(strNode.construct(graphStruct,lvel));// lastNodeList.add(strNode.construct(this.localGraphStruct)); 
				
			if (strNode instanceof GEContainer)
			{
				graphStruct.connectDraw(splitter, ((GEContainer)strNode).getFirstNodeInContainer()); //this.localGraphStruct.connectDraw(splitter, strNode);
			}
			else
			{
				graphStruct.connectDraw(splitter, strNode); //this.localGraphStruct.connectDraw(splitter, strNode);
			} 
		}
		
		listIter =  lastNodeList.listIterator();	
		this.joiner.construct(graphStruct, lvel); //this.joiner.construct(this.localGraphStruct); 
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			graphStruct.connectDraw(strNode, joiner); //this.localGraphStruct.connectDraw(strNode, joiner); 
		}	
	
		
		//graphStruct.getGraphModel().insert(graphStruct.getCells().toArray(),graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100,100)));		
		return this.joiner;
	}
	
	/**
	 * Initialize the default attributes that will be used to draw the GESplitJoin.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		this.port = new DefaultPort();
		this.add(this.port);
		
		(graphStruct.getAttributes()).put(this, this.attributes);
		//GraphConstants.setAutoSize(this.attributes, true);

		
		GraphConstants.setBorderColor(this.attributes, Color.blue.darker());
		GraphConstants.setLineWidth(this.attributes, 4);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.TOP);
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(this.getChildren().toArray(), false);	
	}
	
	/**
	 * Get the succesors of the GESplitJoin. The succesors are the children of the splitter
	 * belonging to this splitjoin.
	 * @return ArrayList with the successors of the GESplitjoin.
	 */
	public ArrayList getSuccesors()
	{
		return this.getSplitter().getSuccesors();
	}
	
	 public ArrayList getContainedElements()
	 {
	 	ArrayList tempList = getSplitter().getSuccesors();
	 	tempList.add(0, this.getSplitter());
	 	tempList.add(this.getJoiner());
	 	return tempList;
	 	
	 }
	 

	
	/**
	 * Expand or collapse the GESplitJoin structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph 
	 */	
	public void collapseExpand()
	{
		if (isExpanded)
		{
			this.collapse();
			isExpanded = false;
		}
		else
		{
			
			this.expand();
			isExpanded = true;
		}
	}	
	
	/**
	 * Expand the GESplitJoin. When it is expanded the elements that it contains are
	 * displayed.
	 */
	public void expand()
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList, true);
		
		//Iterator eIter = (DefaultGraphModel.getEdges(localGraphStruct.getGraphModel(), new Object[]{this})).iterator();
		Iterator eIter = localGraphStruct.getGraphModel().edges(this.getPort());
		ArrayList edgesToRemove =  new ArrayList();
		
		while (eIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) eIter.next();
			Iterator sourceIter = this.getSourceEdges().iterator();	
			while (sourceIter.hasNext())
			{
				DefaultEdge s = (DefaultEdge) sourceIter.next();
				System.out.println(" s hash" +s.hashCode());
				if (s.equals(edge))
				{
					
					System.out.println("source edges were equal");
					cs.disconnect(edge, true);
					cs.connect(edge, this.joiner.getPort(), true);		
					this.joiner.addSourceEdge(s);
					//sourceIter.remove();
					edgesToRemove.add(s);
				}
			}
			
			Iterator targetIter = this.getTargetEdges().iterator();
			while(targetIter.hasNext())
			{
				DefaultEdge t = (DefaultEdge) targetIter.next();
				System.out.println(" t hash" +t.hashCode());
				if(t.equals(edge))
				{
					System.out.println("target edges were equal");
					cs.disconnect(edge,false);
					cs.connect(edge, this.splitter.getPort(),false);
					this.splitter.addTargetEdge(t);
					//targetIter.remove();
					edgesToRemove.add(t);
				}
			}
			
			Object[] removeArray = edgesToRemove.toArray();
			for(int i = 0; i<removeArray.length;i++)
			{
				this.removeSourceEdge((DefaultEdge)removeArray[i]);
				this.removeTargetEdge((DefaultEdge)removeArray[i]);
			}
	
		}
		
		
		this.localGraphStruct.getGraphModel().edit(null, cs, null, null);
		//jgraph.getGraphLayoutCache().setVisible(new Object[]{this}, false);
		
		for (int i = level; i >= 0; i--)
		{
			this.localGraphStruct.containerNodes.hideContainersAtLevel(i);
		}
		
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();	
		setLocationAfterExpand();
	}
	
	
	/**
	 * Expand the GESplitJoin. When it is expanded the elements that it contains are
	 * displayed.
 	 */
	
	public void collapse()
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, true);
		
		Iterator splitEdgeIter = localGraphStruct.getGraphModel().edges(this.getSplitter().getPort());
		Iterator joinEdgeIter = localGraphStruct.getGraphModel().edges(this.getJoiner().getPort());
		
		ArrayList edgesToRemove =  new ArrayList();
		
		
		while (splitEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) splitEdgeIter.next();
			Iterator sourceIter = this.getJoiner().getSourceEdges().iterator();
			
		//	Iterator sourceIter = this.getSplitter().getSourceEdges().iterator();
			while(sourceIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) sourceIter.next();
				if(target.equals(edge))
				{
					cs.disconnect(edge, true);
					cs.connect(edge, this.getPort(), true);
					this.addSourceEdge(edge);
					edgesToRemove.add(edge);
				}
			}
			
			Iterator targetIter = this.getSplitter().getTargetEdges().iterator();	
			while(targetIter.hasNext())
			{
				DefaultEdge source = (DefaultEdge) targetIter.next();
				if (source.equals(edge))
				{
					cs.disconnect(edge,false);
					cs.connect(edge, this.getPort(),false);
					this.addTargetEdge(edge);
					edgesToRemove.add(edge);
				}
			}
			
		}
		while (joinEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) joinEdgeIter.next();
		
			
			Iterator sourceIter = this.getJoiner().getSourceEdges().iterator();
			while(sourceIter.hasNext())
			{
				DefaultEdge source = (DefaultEdge) sourceIter.next();
				if(source.equals(edge))
				{
					cs.disconnect(edge, true);
					cs.connect(edge, this.getPort(), true);
					this.addSourceEdge(edge);
					edgesToRemove.add(edge);
				}
			}
			
			
			Iterator targetIter = this.getSplitter().getTargetEdges().iterator();	
			while(targetIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) targetIter.next();
				if (target.equals(edge))
				{
					cs.disconnect(edge,false);
					cs.connect(edge, this.getPort(),false);
					this.addTargetEdge(edge);
					edgesToRemove.add(edge);
				}
			}
			
			
		}	
			
		Object[] removeArray = edgesToRemove.toArray();
		for(int i = 0; i<removeArray.length;i++)
		{
			this.splitter.removeSourceEdge((DefaultEdge)removeArray[i]);
			this.splitter.removeTargetEdge((DefaultEdge)removeArray[i]);
			this.joiner.removeSourceEdge((DefaultEdge)removeArray[i]);
			this.joiner.removeTargetEdge((DefaultEdge)removeArray[i]);

		}
	
		GraphConstants.setAutoSize(this.attributes, true);			
		this.localGraphStruct.getGraphModel().edit(localGraphStruct.getAttributes(), cs, null, null);
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList, false);
		
		for (int i = level - 1; i >= 0; i--)
		{
			this.localGraphStruct.containerNodes.hideContainersAtLevel(i);
		}	

		
		//JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();
		
		for (int i = level - 1; i >= 0; i--)
		{
			this.localGraphStruct.setLocationContainersAtLevel(i);
		}	
		
			
	}
	
	public void layoutChildren(){};
	public void calculateDimension(){};
	
	/**
	 * Sets the location of the Container nodes that have a level less than or equal 
	 * to this.level. The bounds of the container node are set in such a way that the 
	 * elements that it contains are enclosed.
	 * Also, changes the location of the label so that it is more easily viewable.
	 */
	private void setLocationAfterExpand()
	{
		for (int i = level; i >= 0; i--)
		{
			this.localGraphStruct.setLocationContainersAtLevel(i);
		}
	}
	
	/**
	 * Get the first node contained by the GESplitJoin (this will always be
	 * the GESplitter corresponding to the splitjoin). 
	 */
	public GEStreamNode getFirstNodeInContainer()
	{
		return this.splitter;
	}
	
	/**
	 * Set which node is the first one container by the GEPipeline.
	 */
	public void  setFirstNodeInContainer(GEStreamNode firstNode)
	{
		this.splitter = (GESplitter) firstNode;
	}
	
	/**
	 * Hide the GEStreamNode in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible.
	 * @return true if it was possible to hide the node; otherwise, return false.
	 */
	public boolean hide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, false);
		return true;
	}
	
	/**
	 * Make the GEStreamNode visible in the display. Note that some nodes cannot be hidden or 
	 * they cannot be made visible. 
	 * @return true if it was possible to make the node visible; otherwise, return false.
	 */	
	public boolean unhide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
			setVisible(new Object[]{this}, true);
		return true;
	}	
	
	/**
	 * Writes the textual representation of the GEStreamNode using the PrintWriter specified by out. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param out PrintWriter that is used to output the textual representation of the graph.  
	 */
	public void outputCode(PrintWriter out)
	{
		String tab = "     ";
			
		out.println();
		out.print(this.inputTape + "->" + this.outputTape + " splitjoin " + this.name);
		
		if (this.args.size() > 0)
		{
			this.outputArgs(out);
		}
		out.println(" { ");	
					
		out.println(tab + "split " + this.splitter.name + "();");	
		Iterator childIter  = this.children.iterator();
		while(childIter.hasNext())
		{
			out.println(tab + "add " + ((GEStreamNode) childIter.next()).name + "();");
		}
		out.println(tab + "join " + this.splitter.name + "();");
			
		out.println("}");
		out.println();
	}	

}

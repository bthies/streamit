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

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.utils.JGraphLayoutManager;

/**
 * GEFeedbackLoop is the graph internal representation of  a feedback loop.
 * @author jcarlos
 */
public class GEFeedbackLoop extends GEStreamNode implements Serializable, GEContainer{
	
	
	
	/**
	 * The splitter belonging to this feedback loop.
	 */
	private GESplitter splitter;
	
	/**
	 * The joiner belonging to this feedback loop.
	 */
	private GEJoiner joiner;
	
	/**
	 * The body of the feedback loop.
	 */
	private GEStreamNode body;
	
	/**
	 * The loop part of the feedback loop.
	 */
	private GEStreamNode loop;

	/**
	 * The sub-graph structure that is contained within this GEFeedbackLoop
	 * This subgraph is hidden when the GEFeedbackLoop is collapse and 
	 * visible when expanded. 
	 */
	private GraphStructure localGraphStruct;

	/**
	 * Boolean that specifies if the elements contained by the GEFeedbackLoop are 
	 * displayed (it is expanded) or they are hidden (it is collapsed).
	 */
	private boolean isExpanded;

	/**
	 * GEFeedbackLoop constructor.
	 * @param name The name of the GEFeedbackLoop.
	 * @param split The GESplitter that corresponds to this GEFeedbackLoop.
	 * @param join The GEJoiner that corresponds to this GEFeedbackLoop.
	 * @param body The GEStreamNode that represents the body of theGEFeedbackLoop.
	 * @param loop The GEStreamNode that represents the body of the GEFeedbackLoop.
	 */
	public GEFeedbackLoop(String name, GESplitter split, GEJoiner join, 
						  GEStreamNode body, GEStreamNode loop)
	{
		super(GEType.FEEDBACK_LOOP, name);
		this.splitter = split;
		this.joiner = join;
		this.body = body;
		this.loop = loop;
		this.localGraphStruct = new GraphStructure();
		this.isExpanded = false;
		
		//TODO: The children are never set. 
		//Temporary solution: set them to the value returned by this.getContainedElements() 
		this.children = this.getContainedElements();
	}

	/**
	 * GEFeedbackLoop constructor.
	 * @param name The name of the GEFeedbackLoop.
	 * @param split The GESplitter that corresponds to this GEFeedbackLoop.
	 * @param join The GEJoiner that corresponds to this GEFeedbackLoop.
	 * @param body The GEStreamNode that represents the body of theGEFeedbackLoop.
	 * @param loop The GEStreamNode that represents the body of the GEFeedbackLoop.
	 * @param gs The GraphStructure that this is part of.
	 */
	public GEFeedbackLoop(String name, GESplitter split, GEJoiner join, 
						  GEStreamNode body, GEStreamNode loop, GraphStructure gs)
	{
		super(GEType.FEEDBACK_LOOP, name);
		this.splitter = split;
		this.joiner = join;
		this.body = body;
		this.loop = loop;
		this.localGraphStruct = gs;
		this.isExpanded = false;
	}

	/**
	 * Get the splitter part of this.
	 * @return GESplitter corresponding to this GEFeedbackLoop.
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}
	
	/**
	 * Get the joiner part of this.
	 * @return GESJoiner corresponding to this GEFeedbackLoop.
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}	
	
	/**
	 * Get the body of this.
	 * @return GEStreamNode that is the body of GEFeedbackLoop.
	 */
	public GEStreamNode getBody()
	{
		return this.body;
	}
	
	/**
	 * Get the loop of this.
	 * @return GEStreamNode that is the loop of GEFeedbackLoop.
	 */
	public GEStreamNode getLoop()
	{
		return this.loop;
	}

	/**
	 * Construct the GEFeedbackLoop by constructing the nodes that it contains
	 * and making the corresponding connections. 
	 * @return GEStreamNode returns the last node in the GEFeedbackLoop.
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int lvel)
	{
		System.out.println("Constructing the feedback loop " +this.getName());
		this.level = lvel;
		graphStruct.containerNodes.addContainerToLevel(level, this);
		lvel++; 
		this.localGraphStruct = graphStruct;	
					
		joiner.construct(graphStruct, lvel);
		GEStreamNode lastBody = body.construct(graphStruct, level);
		graphStruct.connectDraw(joiner, lastBody );
	
		// Error Fixed but now Sugiyama Layout algorithm does not work since it can't operate on a DAG
		// TODO: Possible Solution: Do layout first. Then make connection that would result in the DAG.
		splitter.construct(graphStruct, level);
		graphStruct.connectDraw(body, splitter);
		
		GEStreamNode lastLoop = loop.construct(graphStruct, level); 
		graphStruct.connectDraw(splitter, loop);
		graphStruct.connectDraw(loop, joiner);
	
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100,100)));
				
		return this.splitter;
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
		GraphConstants.setBorderColor(this.attributes, Color.green.darker());
		GraphConstants.setLineWidth(this.attributes, 4);
		GraphConstants.setBounds(this.attributes, bounds); // without this - problems with expansion in layout
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.TOP);
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
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
	 * Expand the GEFEedbackLoop. When it is expanded the elements that it contains become visible.
	 */
	public void expand()
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(nodeList, true);
		
		Iterator eIter = localGraphStruct.getGraphModel().edges(this.getPort());
		ArrayList edgesToRemove =  new ArrayList();
		
		while (eIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) eIter.next();
			Iterator sourceIter = this.getSourceEdges().iterator();	
			while (sourceIter.hasNext())
			{
				DefaultEdge s = (DefaultEdge) sourceIter.next();
				if (s.equals(edge))
				{
					System.out.println(" GEFeedbackLoop Expand: source edges were equal");
					cs.disconnect(edge, true);
					cs.connect(edge, this.splitter.getPort(), true);		
					this.splitter.addSourceEdge(s);
					edgesToRemove.add(s);
				}
			}
			
			Iterator targetIter = this.getTargetEdges().iterator();
			while(targetIter.hasNext())
			{
				DefaultEdge t = (DefaultEdge) targetIter.next();
				if(t.equals(edge))
				{
					System.out.println("GEFeedbackLoop Expand: target edges were equal");
						cs.disconnect(edge,false);
						cs.connect(edge, this.joiner.getPort(),false);
						this.joiner.addTargetEdge(t);
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
		for (int i = level; i >= 0; i--)
		{
			this.localGraphStruct.containerNodes.hideContainersAtLevel(i);
		}
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();	
		setLocationAfterExpand();
	}

	/**
	 * Collapse the GEFeedbackLoop. The elements contained by the GEFeedbackLoop become
	 * invisible.
	 */
	public void collapse()
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, true);
		
		Iterator splitEdgeIter = localGraphStruct.getGraphModel().edges(this.getSplitter().getPort());
		Iterator joinEdgeIter = localGraphStruct.getGraphModel().edges(this.getJoiner().getPort());
		ArrayList edgesToRemove =  new ArrayList();
		
		while (joinEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) joinEdgeIter.next();
			Iterator sourceIter = this.getJoiner().getTargetEdges().iterator();
			while(sourceIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) sourceIter.next();
				if(target.equals(edge))
				{
					System.out.println(" GEFeedbackLoop Collapse: target edges were equal");
					System.out.println(" The container of the edge is " + ((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode());
					if (!(this.equals(((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode())))
					{
						cs.disconnect(edge, false);
						cs.connect(edge, this.getPort(), false);
						this.addTargetEdge(edge);
						edgesToRemove.add(edge);
					}
				}
			}
		}
		while (splitEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) splitEdgeIter.next();
			Iterator targetIter = this.getSplitter().getSourceEdges().iterator();
			while(targetIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) targetIter.next();
				if (target.equals(edge))
				{
					System.out.println(" GEFeedbackLoop Collapse: source edges were equal");
					System.out.println(" The container of the edge is " + ((GEStreamNode) ((DefaultPort)edge.getSource()).getParent()).getEncapsulatingNode());
					if (!(this.equals(((GEStreamNode) ((DefaultPort)edge.getTarget()).getParent()).getEncapsulatingNode())))
					{
						cs.disconnect(edge,true);
						cs.connect(edge, this.getPort(),true);
						this.addSourceEdge(edge);
						edgesToRemove.add(edge);
					}
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
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();
	
		for (int i = level - 1; i >= 0; i--)
		{
			this.localGraphStruct.setLocationContainersAtLevel(i);
		}				
	}

	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node, then a list with no elements is returned.
	 * @return ArrayList of contained elements. If <this> is not a container, return empty list.
	 */
	public ArrayList getContainedElements()
	{
		ArrayList tempList = new ArrayList();
	  	tempList.add(this.joiner);
	  	tempList.add(this.body);
	  	tempList.add(this.loop);
	  	tempList.add(this.splitter);

	   return tempList;
	 	
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
	 * Get the first node contained by the GEPipeline. 
	 */
	public GEStreamNode getFirstNodeInContainer()
	{
		return this.joiner;
	}
	
	/**
	 * Set which node is the first one container by the GEPipeline.
	 */
	public void  setFirstNodeInContainer(GEStreamNode firstNode)
	{
		this.joiner = (GEJoiner) firstNode;
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
		out.print(this.inputTape + "->" + this.outputTape + " feedbackloop " + this.name);
		
		if (this.args.size() > 0)
		{
			this.outputArgs(out);
		}
		out.println(" { ");	
					
		out.println(tab + "join " + this.joiner.name + "();");
		out.println(tab + "body " + this.body.name + "();");
		out.println(tab + "loop " + this.loop.name + "();");
		out.println(tab + "split " + this.splitter.name + "();");			
		out.println("}");
		out.println();
	}	
}





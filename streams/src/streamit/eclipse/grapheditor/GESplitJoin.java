/*
 * Created on Jun 24, 2003
 */
package streamit.eclipse.grapheditor;

import java.io.*;
import java.util.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import streamit.eclipse.grapheditor.jgraphextension.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Color;
import javax.swing.BorderFactory; 

/**
 * GESplitJoin is the graph internal representation of  a splitjoin. It is composed 
 * of a splitter, a joiner, and the children of the splitter which in turn are also the 
 * parents of the joiner.
 * @author jcarlos
 */
public class GESplitJoin extends GEStreamNode implements Serializable{

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
	 * The frame in which the contents of the SplitJoin (whatever is specified
	 * by localGraphStruct) will be drawn.
	 */
	private LiveJGraphInternalFrame frame;

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
	public GEStreamNode construct(GraphStructure graphStruct, int level)
	{
		System.out.println("Constructing the SplitJoin " +this.getName());
		this.draw();
		
		graphStruct.addToLevelContainer(level, this);
		level++;
		
		/*
		DefaultGraphModel model = new DefaultGraphModel();
		this.localGraphStruct.setGraphModel(model);
		JGraph jgraph = new JGraph(model);
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph));					
		frame = new LiveJGraphInternalFrame(this.localGraphStruct.getJGraph());
		this.localGraphStruct.internalFrame = frame;
		*/
		
		graphStruct.getJGraph().addMouseListener(new JGraphMouseAdapter(graphStruct.getJGraph()));
		//graphStruct.getJGraph().getGraphLayoutCache().setVisible(this, true);
		
		//this.localGraphStruct.setJGraph(graphStruct.getJGraph());
		this.localGraphStruct = graphStruct;	
		
		this.splitter.construct(graphStruct, level); //this.splitter.construct(this.localGraphStruct); 
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
		ArrayList lastNodeList = new ArrayList();
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = ((GEStreamNode) listIter.next());
			lastNodeList.add(strNode.construct(graphStruct,level));// lastNodeList.add(strNode.construct(this.localGraphStruct)); 
			
			System.out.println("Connecting " + splitter.getName()+  " to "+ strNode.getName());	
			graphStruct.connectDraw(splitter, strNode); //this.localGraphStruct.connectDraw(splitter, strNode); 
		}
		
		listIter =  lastNodeList.listIterator();
		
		this.joiner.construct(graphStruct, level); //this.joiner.construct(this.localGraphStruct); 
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			System.out.println("Connecting " + strNode.getName()+  " to "+ joiner.getName());
			graphStruct.connectDraw(strNode, joiner); //this.localGraphStruct.connectDraw(strNode, joiner); 
		}	
	
		//this.localGraphStruct.getGraphModel().insert(localGraphStruct.getCells().toArray(),localGraphStruct.getAttributes(), localGraphStruct.getConnectionSet(), null, null);
		graphStruct.getGraphModel().insert(graphStruct.getCells().toArray(),graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
	
	
		this.port = new DefaultPort();
		this.add(this.port);
		
		/*
		frame.setGraphCell(this);
		frame.setGraphStruct(graphStruct);
		frame.setGraphModel(graphStruct.getGraphModel());
		frame.create(this.getName());
		frame.setSize(400, 400);
		*/
		
		this.initDrawAttributes(graphStruct);
						
		//graphStruct.internalFrame.getContentPane().add(frame);
		/*
		try {	
			frame.setSelected(true);
		} catch(Exception pve) {}
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		manager.arrange();
		*/
		
		return this;
	}
	
	/**
	 * Initialize the default attributes that will be used to draw the GESplitJoin.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct)
	{
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));
		GraphConstants.setBorder(this.attributes , BorderFactory.createLineBorder(Color.green));
		
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
	 * Draw this SplitJoin
	 */
	public void draw()
	{
		System.out.println("Drawing the SplitJoin " +this.getName());
	}	
	
	/**
	 * Expand or collapse the GESplitJoin structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph 
	 */	
	public void collapseExpand(JGraph jgraph)
	{
		if (isExpanded)
		{
			this.collapse(jgraph);
			isExpanded = false;
		}
		else
		{
			
			this.expand(jgraph);
			isExpanded = true;
		}
	}	
	
	/**
	 * Expand the GESplitJoin. When it is expanded the elements that it contains are
	 * displayed.
	 */
	public void expand(JGraph jgraph)
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		jgraph.getGraphLayoutCache().setVisible(nodeList, true);
		
		//Iterator eIter = (DefaultGraphModel.getEdges(localGraphStruct.getGraphModel(), new Object[]{this})).iterator();
		Iterator eIter = localGraphStruct.getGraphModel().edges(this.getPort());
		
		ArrayList edgesToRemove =  new ArrayList();
		
		
		while (eIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) eIter.next();
		
			
			Iterator sourceIter = this.getSourceEdges().iterator();	
			System.out.println(" edge hash" +edge.hashCode());
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
		jgraph.getGraphLayoutCache().setVisible(new Object[]{this}, false);
		
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		manager.arrange();	
		setLocationAfterExpand();
	}
	
	
	/**
	 * Expand the GESplitJoin. When it is expanded the elements that it contains are
	 * displayed.
 	 */
	
	public void collapse(JGraph jgraph)
	{
		Object[] nodeList = this.getContainedElements().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		jgraph.getGraphLayoutCache().setVisible(new Object[]{this}, true);
		
		Iterator splitEdgeIter = localGraphStruct.getGraphModel().edges(this.getSplitter().getPort());
		Iterator joinEdgeIter = localGraphStruct.getGraphModel().edges(this.getJoiner().getPort());
		
		ArrayList edgesToRemove =  new ArrayList();
		
		
		while (splitEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) splitEdgeIter.next();
			
			
			Iterator sourceIter = this.getJoiner().getSourceEdges().iterator();
			while(sourceIter.hasNext())
			{
				DefaultEdge target = (DefaultEdge) sourceIter.next();
				if(target.equals(edge))
				{
					System.out.println("source equals edge");
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
					System.out.println("target equals target");
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
					System.out.println("source equals edge");
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
					System.out.println("target equals target");
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
		jgraph.getGraphLayoutCache().setVisible(nodeList, false);
		
		//JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		JGraphLayoutManager manager = new JGraphLayoutManager(jgraph);
		manager.arrange();
			
	}
	
	private void setLocationAfterExpand()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, true);
		Object[] containedCells = this.getContainedElements().toArray();
		
		CellView[] containedCellViews = 
			this.localGraphStruct.getJGraph().getGraphLayoutCache().getMapping(containedCells);

		Rectangle cellBounds = AbstractCellView.getBounds(containedCellViews);
		
		System.out.println("Bounds of the contained elements is" + cellBounds.toString());
		GraphConstants.setAutoSize(this.attributes, false);
		GraphConstants.setBounds(this.attributes, cellBounds);
		
		this.localGraphStruct.getGraphModel().edit(localGraphStruct.getAttributes(), null , null, null);
	}
	
	
	

}

/*
 * Created on Jun 20, 2003
 */
package streamit.eclipse.grapheditor;

import java.io.*;
import java.util.*;
import streamit.eclipse.grapheditor.jgraphextension.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import java.awt.Color;
import java.awt.Rectangle;
import javax.swing.BorderFactory; 

/**
 * GEPipeline is the graph internal representation of a pipeline. 
 * @author jcarlos
 */
public class GEPipeline extends GEStreamNode implements Serializable{
			
	private GEStreamNode lastNode;	
	
	/**
	 * The sub-graph structure that is contained within this pipeline.
	 * This subgraph is hidden when the pipeline is collapse and 
	 * visible when expanded. 
	 */
	private GraphStructure localGraphStruct;

	/**
	 * Boolean that specifies if the elements contained by the Pipeline are 
	 * displayed (it is expanded) or they are hidden (it is collapsed).
	 */
	private boolean isExpanded;

	/**
	 * The level of how deep the elements of this pipeline are with respect to 
	 * other container nodes that they belong to.
	 * The toplevel pipeline has level 0. The elements of another pipeline within
	 * this toplevel pipeline would have its level equal to 1 (same applies to other 
	 * container nodes such as splitjoins and feedback loops).
	 */
	private int level;
	
	/**
	 * The frame in which the contents of the pipeline (whatever is specified
	 * by localGraphStruct) will be drawn.
	 */
	private LiveJGraphInternalFrame frame;
	

	/**
	 * GEPipeline constructor.
	 * @param name The name of this GEPipeline.
	 */
	public GEPipeline(String name)
	{
		super(GEType.PIPELINE, name);
		localGraphStruct = new GraphStructure();	
	}

	/**
 	 * Constructs the pipeline and returns <this> so that the GEPipeline can 
 	 * be connected to its succesor and predecessor.
 	*/
	public GEStreamNode construct(GraphStructure graphStruct, int level)
	{
		System.out.println("Constructing the pipeline" +this.getName());
		boolean first = true;
		
		graphStruct.addToLevelContainer(level, this);
		level++;
		graphStruct.getJGraph().addMouseListener(new JGraphMouseAdapter(graphStruct.getJGraph()));
		
		this.localGraphStruct = graphStruct;
		
		//graphStruct.getJGraph().getGraphLayoutCache().setVisible(this, true);
		//this.localGraphStruct.setJGraph(graphStruct.getJGraph());		
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
	
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			GEStreamNode lastTemp = strNode.construct(graphStruct, level); //GEStreamNode lastTemp = strNode.construct(this.localGraphStruct);
			
			if(!first)
			{
				System.out.println("Connecting " + lastNode.getName()+  " to "+ strNode.getName());		 
				graphStruct.connectDraw(lastNode, strNode); //this.localGraphStruct.connectDraw(lastNode, strNode);
			}
			lastNode = lastTemp;
			first = false;
		}
	
		//this.localGraphStruct.getGraphModel().insert(this.localGraphStruct.getCells().toArray(), this.localGraphStruct.getAttributes(), this.localGraphStruct.getConnectionSet(), null, null);
		graphStruct.getGraphModel().insert(graphStruct.getCells().toArray(), graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
		this.initDrawAttributes(graphStruct);

		if (graphStruct.getTopLevel() == this)
		{
			graphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, nodeList.toArray());
		}
				
		return this;
	}	
	
	/**
	 * Initialize the default attributes that will be used to draw the GEPipeline.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct)
	{
		this.port = new DefaultPort();
		this.add(this.port);

		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));	
		GraphConstants.setBorder(this.attributes , BorderFactory.createLineBorder(Color.blue));
		GraphConstants.setBackground(this.attributes, Color.blue);	
		
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);	
	}
			
	/**
	 * Expand or collapse the GEStreamNode structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph The JGraph that will be modified to allow the expanding/collapsing.
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
	
	public void expand(JGraph jgraph)
	{
		/*if(this.isInfoDisplayed) {		
			Rectangle rect = GraphConstants.getBounds(this.attributes);
			this.frame.setLocation(new Point(rect.x, rect.y));
			this.frame.setVisible(true);
		} else {
			this.frame.setLocation(GraphConstants.getOffset(this.attributes));
			this.frame.setVisible(false); }
		*/

		Object[] nodeList = this.getSuccesors().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		jgraph.getGraphLayoutCache().setVisible(nodeList, true);
		
		Iterator eIter = localGraphStruct.getGraphModel().edges(this.getPort());
		ArrayList edgesToRemove =  new ArrayList();
		GEStreamNode firstInPipe = (GEStreamNode) nodeList[0];
		GEStreamNode finalInPipe = (GEStreamNode) nodeList[nodeList.length-1];
			
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
					cs.connect(edge, finalInPipe.getPort(), true);	
					finalInPipe.addSourceEdge(s);
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
					cs.connect(edge, firstInPipe.getPort(),false);
					firstInPipe.addTargetEdge(t);
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
	 * Collapse the GraphStructure.
	 */

	public void collapse(JGraph jgraph)
	{
		Object[] nodeList = this.getSuccesors().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		jgraph.getGraphLayoutCache().setVisible(new Object[]{this}, true);
		
		GEStreamNode firstInPipe = (GEStreamNode) nodeList[0];
		GEStreamNode finalInPipe = (GEStreamNode) nodeList[nodeList.length-1];
		
		Iterator initialEdgeIter = localGraphStruct.getGraphModel().edges(firstInPipe.getPort());
		Iterator finalEdgeIter = localGraphStruct.getGraphModel().edges(finalInPipe.getPort());
		
		ArrayList edgesToRemove =  new ArrayList();
		
		
		while (initialEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) initialEdgeIter.next();
				
			Iterator sourceIter = finalInPipe.getSourceEdges().iterator();
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
			
			Iterator targetIter = firstInPipe.getTargetEdges().iterator();	
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
		
		while (finalEdgeIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) finalEdgeIter.next();
			
			Iterator sourceIter = finalInPipe.getSourceEdges().iterator();
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
			
			Iterator targetIter = firstInPipe.getTargetEdges().iterator();	
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
			firstInPipe.removeSourceEdge((DefaultEdge)removeArray[i]);
			firstInPipe.removeTargetEdge((DefaultEdge)removeArray[i]);
			finalInPipe.removeSourceEdge((DefaultEdge)removeArray[i]);
			finalInPipe.removeTargetEdge((DefaultEdge)removeArray[i]);

		}
												
		this.localGraphStruct.getGraphModel().edit(null, cs, null, null);
		jgraph.getGraphLayoutCache().setVisible(nodeList, false);
		
		//JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		JGraphLayoutManager manager = new JGraphLayoutManager(jgraph);
		manager.arrange();	
	}
	
	private void setLocationAfterExpand()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, true);
		Object[] containedCells = this.getSuccesors().toArray();
		
		CellView[] containedCellViews = 
			this.localGraphStruct.getJGraph().getGraphLayoutCache().getMapping(containedCells);

		Rectangle cellBounds = AbstractCellView.getBounds(containedCellViews);
		
		System.out.println("Bounds of the contained elements is" + cellBounds.toString());
		GraphConstants.setAutoSize(this.attributes, false);
		GraphConstants.setBounds(this.attributes, cellBounds);
		
		this.localGraphStruct.getGraphModel().
				edit(localGraphStruct.getAttributes(), null , null, null);
	}
	
	public void hide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
				setVisible(new Object[]{this}, true);
	}
	
	public void unhide()
	{
		this.localGraphStruct.getJGraph().getGraphLayoutCache().
				setVisible(new Object[]{this}, false);
	}
	
	/**
	 * Draw this Pipeline
	 */	
	public void draw()
	{
		System.out.println("Drawing the pipeline " +this.getName());
	}
}

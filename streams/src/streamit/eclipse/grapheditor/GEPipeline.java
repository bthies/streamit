/*
 * Created on Jun 20, 2003
 */
package grapheditor;

import java.io.*;
import java.util.*;
import grapheditor.jgraphextension.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import java.awt.Color;
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
		this.draw();
		
		graphStruct.addToLevelContainer(level, this);
		level++;
	
		/*	
		DefaultGraphModel model = new DefaultGraphModel();
		this.localGraphStruct.setGraphModel(model);
		JGraph jgraph = new JGraph(model);	
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph));
		jgraph.getGraphLayoutCache().setVisible(this, true);
		this.localGraphStruct.setJGraph(jgraph);
		
		frame = new LiveJGraphInternalFrame(this.localGraphStruct.getJGraph());
		this.localGraphStruct.internalFrame = frame;
		*/
		
		graphStruct.getJGraph().addMouseListener(new JGraphMouseAdapter(graphStruct.getJGraph()));
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(this, true);
		this.localGraphStruct.setJGraph(graphStruct.getJGraph());		
		
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
		
		
		this.port = new DefaultPort();
		this.add(this.port);
		
		/*
		frame.setGraphCell(this);
		frame.setGraphStruct(graphStruct);
		frame.setGraphModel(graphStruct.getGraphModel());
		frame.create(this.getName());
		frame.setSize(150, 350);
		*/
				
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));	
		GraphConstants.setBorder(this.attributes , BorderFactory.createLineBorder(Color.blue));
		GraphConstants.setBackground(this.attributes, Color.blue);	
			
			
			
			
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{this}, nodeList.toArray());
		
		
		if (graphStruct.getTopLevel() == this) {
			//graphStruct.editorFrame.getDesktopPane().add(frame);
			//graphStruct.panel.getViewport().setView(frame);
		}
		else {
			graphStruct.internalFrame.getContentPane().add(frame);
		}
		
		/*
		try {	
			frame.setSelected(true);
		}  catch(Exception pve) {}
		frame.setSize(algorithm.max.x + 10, ((algorithm.max.y + ((algorithm.spacing.y * (this.getSuccesors().size()-1)) / 2)))) ; 
		*/
		
		return this;
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
			
		/*
		if(this.isInfoDisplayed) {		
			Rectangle rect = GraphConstants.getBounds(this.attributes);
			this.frame.setLocation(new Point(rect.x, rect.y));
			this.frame.setVisible(true);
		} else {
			this.frame.setLocation(GraphConstants.getOffset(this.attributes));
			this.frame.setVisible(false);
		}
		*/
	
	/*	if(jgraph.getGraphLayoutCache().isPartial()) {
			System.out.println("the graph is partial");
		}*/	 
		
		Object[] nodeList = this.getSuccesors().toArray();
		ConnectionSet cs = this.localGraphStruct.getConnectionSet();	
		jgraph.getGraphLayoutCache().setVisible(nodeList, true);
		
		Iterator eIter = localGraphStruct.getGraphModel().edges(this.getPort());
		
		boolean temp = true;
		
		while (eIter.hasNext())
		{
			DefaultEdge edge = (DefaultEdge) eIter.next();
			
			if (nodeList.length > 0)
			{
			
				if (temp)
				{
					cs.disconnect(edge,false);
					cs.connect(edge, nodeList[0], false);
					temp = false;
				}	
				else
				{
					cs.disconnect(edge, true);
					cs.connect(edge, nodeList[nodeList.length-1], true);		
				}	
			}
		}
		
		this.localGraphStruct.getGraphModel().edit(null, cs, null, null);
		jgraph.getGraphLayoutCache().setVisible(new Object[]{this}, false);
		
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		manager.arrange();	

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
	
	/**
	 * Draw this Pipeline
	 */	
	public void draw()
	{
		System.out.println("Drawing the pipeline " +this.getName());
	}


}

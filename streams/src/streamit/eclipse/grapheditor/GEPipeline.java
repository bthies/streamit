/*
 * Created on Jun 20, 2003
 */
package grapheditor;

import java.io.*;
import java.util.*;
import grapheditor.jgraphextension.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;

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
	public GEStreamNode construct(GraphStructure graphStruct)
	{
		System.out.println("Constructing the pipeline" +this.getName());
		boolean first = true;
		this.draw();
	
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
			GEStreamNode lastTemp = strNode.construct(graphStruct); //GEStreamNode lastTemp = strNode.construct(this.localGraphStruct);
			
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
	 * Draw this Pipeline
	 */	
	public void draw()
	{
		System.out.println("Drawing the pipeline " +this.getName());
	}
	
	/**
	 * Expand or collapse the GEStreamNode structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph The JGraph that will be modified to allow the expanding/collapsing.
	 */
	public void collapseExpand(JGraph jgraph)
	{
		System.out.println("Collapsing/Expanding the Pipeline");
		
		/*
		if(this.isInfoDisplayed) {		
			Rectangle rect = GraphConstants.getBounds(this.attributes);
			this.frame.setLocation(new Point(rect.x, rect.y));
			this.frame.setVisible(true);
		}
		else {
			this.frame.setLocation(GraphConstants.getOffset(this.attributes));
			this.frame.setVisible(false);
		}
		*/
	
		Object[] nodeList = this.getSuccesors().toArray();
		
		if(jgraph.getGraphLayoutCache().isPartial())
		{
			System.out.println("the graph is partial");
		}
		else
		{
			System.out.println("the graph is not partial");
		}
		 
		jgraph.getGraphLayoutCache().setVisible(new Object[]{this}, false);
		jgraph.getGraphLayoutCache().setVisible(nodeList, true);
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		manager.arrange();
	}


}

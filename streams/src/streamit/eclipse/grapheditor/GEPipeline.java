/*
 * Created on Jun 20, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.io.*;
import java.util.*;
import grapheditor.jgraphextension.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import com.jgraph.layout.*;
import javax.swing.*;

/**
 * GEPipeline is the graph internal representation of a node. 
 * @author jcarlos
 *
 */
public class GEPipeline extends GEStreamNode implements Serializable{
			
	private GEStreamNode lastNode;	
	private GraphStructure localGraphStruct;	
	
	public GEPipeline(String name)
	{
	
		super(GEType.PIPELINE, name);
		localGraphStruct = new GraphStructure();
		
	}


/**
 * Constructs the pipeline and returns the last node in the pipeline that will be connecting
 * to the next graph structure.
 */
	public GEStreamNode construct(GraphStructure graphStruct)
	{
		System.out.println("Constructing the pipeline" +this.getName());
		boolean first = true;
		this.draw();
		
		//this.localGraphStruct.editorFrame = graphStruct.liveDemo;
		DefaultGraphModel model = new DefaultGraphModel();
		this.localGraphStruct.setGraphModel(model);
		JGraph jgraph = new JGraph(model);
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph));
		this.localGraphStruct.setJGraph(jgraph);
		
		LiveJGraphInternalFrame frame = new LiveJGraphInternalFrame(this.localGraphStruct.getJGraph());
		this.localGraphStruct.internalFrame = frame;
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
	
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			GEStreamNode lastTemp = strNode.construct(this.localGraphStruct); //////// GEStreamNode lastTemp = strNode.construct(graphStruct);
			
			if(!first)
			{
				System.out.println("Connecting " + lastNode.getName()+  " to "+ strNode.getName());
				this.localGraphStruct.connectDraw(lastNode, strNode); ////////// graphStruct.connectDraw(lastNode, strNode);
			}
			
			lastNode = lastTemp;
			first = false;
		}
		
		/*
		DefaultGraphModel model = new DefaultGraphModel();
		model.insert(localGraph.getCells().toArray(), localGraph.getAttributes(), localGraph.getConnectionSet(), null, null);
		localGraph.setGraphModel(model);
		localGraph.setJGraph(new JGraph(model));
		LiveJGraphInternalFrame frame = new LiveJGraphInternalFrame(localGraphStruct.getJGraph());
		*/

		this.localGraphStruct.getGraphModel().insert(this.localGraphStruct.getCells().toArray(), this.localGraphStruct.getAttributes(), this.localGraphStruct.getConnectionSet(), null, null);
		
		//DefaultGraphCell pipeCell = new DefaultGraphCell(frame);	
		this.port = new DefaultPort();
		//pipeCell.add(this.port);
		this.add(this.port);
		//frame.setGraphCell(pipeCell);
		frame.setGraphCell(this);
		
		
		frame.setGraphStruct(graphStruct);

		//frame.setGraphModel(this.localGraphStruct.getGraphModel());
		frame.setGraphModel(graphStruct.getGraphModel());
		frame.create(this.getName());
		frame.setSize(300, 600);
			
		/*	
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));	
		*/	
			
		//graphStruct.getCells().add(pipeCell);
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		
		//frame.addInternalFrameListener(_fsl);
		//frame.addComponentListener(_fcl);
		 
		// temp change Removed to test recursiveness
		// graphStruct.liveDemo.getDesktopPane().add(frame);
		// must change this so that frame is not equal to null.This fix does not make it work as well as with the split join, should look at splitjoin to see what is missing, could be the line that is just above insertcells
		
		if (graphStruct.getTopLevel() == this)
		{
			graphStruct.editorFrame.getDesktopPane().add(frame);
		}
		else
		{
			graphStruct.internalFrame.getContentPane().add(frame);
			//graphStruct.internalFrame.getDesktopPane().add(frame);
		}
		
		try 
		{	
			frame.setSelected(true);
		} 
		catch(Exception pve) {}
		
		//Thread t= new Thread ("Sugiyama Layout") {
		//				public void run() {
		SugiyamaLayoutAlgorithm algorithm = new SugiyamaLayoutAlgorithm();
		SugiyamaLayoutController aController= new SugiyamaLayoutController();	
		algorithm.perform(this.localGraphStruct.getJGraph(), true, aController.getConfiguration());
		//				}
		//			};
		
		return this;
	}	
	
	/**
	 * Draw this Pipeline
	 */	
	public void draw()
	{
		System.out.println("Drawing the pipeline " +this.getName());
		
		
		
		// TO BE ADDED
	}
	public void collapse()
	{
		// draw shrunk version
	}

	public static void test()
	{
		System.out.println("Testing static method in pipeline");
	}
	
	
	
}

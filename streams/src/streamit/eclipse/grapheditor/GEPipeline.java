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
import javax.swing.*;

/**
 * GEPipeline is the graph internal representation of a node. 
 * @author jcarlos
 *
 */
public class GEPipeline extends GEStreamNode implements Serializable{
			
	private GEStreamNode lastNode;		
	
	public GEPipeline(String name)
	{
	
		super(GEType.PIPELINE, name);
		System.out.println("Constructing the pipeline");
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
		
		// Create a graph structure that will be used contained within a LiveJGraphInternalFrame.
		GraphStructure localGraph = new GraphStructure();
		localGraph.liveDemo = graphStruct.liveDemo;
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
		
		DefaultGraphModel model = new DefaultGraphModel();
		localGraph.setGraphModel(model);
		localGraph.setJGraph(new JGraph(model));
		
		LiveJGraphInternalFrame frame = new LiveJGraphInternalFrame(localGraph.getJGraph());
		localGraph.internalFrame = frame;
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			
			//////// GEStreamNode lastTemp = strNode.construct(graphStruct);
			GEStreamNode lastTemp = strNode.construct(localGraph);
			
			if(!first)
			{
				System.out.println("Connecting " + lastNode.getName()+  " to "+ strNode.getName());
				
				////////// graphStruct.connectDraw(lastNode, strNode);
				localGraph.connectDraw(lastNode, strNode);
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

		
		model.insert(localGraph.getCells().toArray(), localGraph.getAttributes(), localGraph.getConnectionSet(), null, null);

		DefaultGraphCell pipeCell = new DefaultGraphCell(frame);	
		this.port = new DefaultPort();
		pipeCell.add(this.port);	
		frame.setGraphCell(pipeCell);
		
		frame.setGraphStruct(graphStruct);

		frame.setGraphModel(model);
		frame.create(this.getName());
		frame.setSize(450, 550);
		

		
		
		graphStruct.getCells().add(pipeCell);
		//Object insertCells[] = new Object[] {pipeCell};
		//(graphStruct.getGraphModel()).insert(insertCells,null,null,null,null);
		//frame.addInternalFrameListener(_fsl);
		//frame.addComponentListener(_fcl);
		 
		// temp change Removed to test recursiveness
		//graphStruct.liveDemo.getDesktopPane().add(frame);
		// must change this so that frame is not equal to null
		// this fix does not make it work as well as with the split join
		// should look at splitjoin to see what is missing
		// could be the line that is just above insertcells
		
		if (graphStruct.getTopLevel() == this)
		{
			graphStruct.liveDemo.getDesktopPane().add(frame);
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
		
		return this.lastNode;
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

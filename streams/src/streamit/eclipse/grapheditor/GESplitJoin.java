/*
 * Created on Jun 24, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.io.*;
import java.util.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import com.jgraph.layout.*;
import grapheditor.jgraphextension.*;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class GESplitJoin extends GEStreamNode implements Serializable{
	private GESplitter splitter;
	private GEJoiner joiner;
	private ArrayList children;
	
	// graph structure contained within a LiveJGraphInternalFrame
	private GraphStructure localGraphStruct;

	public GESplitJoin(String name, GESplitter split, GEJoiner join)
	{
		super(GEType.SPLIT_JOIN , name);
		this.splitter = split;
		this.joiner = join;
		this.setChildren(split.getSuccesors());
		this.localGraphStruct = new GraphStructure();
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
	public GEStreamNode construct(GraphStructure graphStruct)
	{
		System.out.println("Constructing the SplitJoin " +this.getName());
		this.draw();
					
		//localGraphStruct.editorFrame = graphStruct.liveDemo;
		DefaultGraphModel model = new DefaultGraphModel();
		this.localGraphStruct.setGraphModel(model);
		this.localGraphStruct.setJGraph(new JGraph(model));
				
		LiveJGraphInternalFrame frame = new LiveJGraphInternalFrame(this.localGraphStruct.getJGraph());
		this.localGraphStruct.internalFrame = frame;
		
		this.splitter.construct(this.localGraphStruct); ////// this.splitter.construct(graphStruct);
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
		ArrayList lastNodeList = new ArrayList();
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = ((GEStreamNode) listIter.next());
			lastNodeList.add(strNode.construct(this.localGraphStruct)); ///////// lastNodeList.add(strNode.construct(graphStruct));
			
			System.out.println("Connecting " + splitter.getName()+  " to "+ strNode.getName());	
			this.localGraphStruct.connectDraw(splitter, strNode); ///////// graphStruct.connectDraw(splitter, strNode);
		}
		
		listIter =  lastNodeList.listIterator();
		
		this.joiner.construct(this.localGraphStruct); //////// this.joiner.construct(graphStruct);
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			System.out.println("Connecting " + strNode.getName()+  " to "+ joiner.getName());
			this.localGraphStruct.connectDraw(strNode, joiner); //////// graphStruct.connectDraw(strNode, joiner);
		}	
	
		this.localGraphStruct.getGraphModel().insert(localGraphStruct.getCells().toArray(),localGraphStruct.getAttributes(), localGraphStruct.getConnectionSet(), null, null);

		//DefaultGraphCell splitjoinCell = new DefaultGraphCell(frame);	
		this.port = new DefaultPort();
		//splitjoinCell.add(this.port);
		this.add(this.port);
		//frame.setGraphCell(splitjoinCell);
		frame.setGraphCell(this);

		frame.setGraphStruct(graphStruct);
		
		//frame.setGraphModel(this.localGraphStruct.getGraphModel());
		frame.setGraphModel(graphStruct.getGraphModel());
		frame.create(this.getName());
		frame.setSize(400, 400);
		
		/*
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));
		*/
		
		//graphStruct.getCells().add(splitjoinCell);
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
						
		graphStruct.internalFrame.getContentPane().add(frame);
		//graphStruct.internalFrame.getDesktopPane().add(frame);
	
		try 
		{	
			frame.setSelected(true);
		} 
		catch(Exception pve) {}
		
		SugiyamaLayoutAlgorithm algorithm = new SugiyamaLayoutAlgorithm();
		SugiyamaLayoutController aController= new SugiyamaLayoutController();	
		algorithm.perform(localGraphStruct.getJGraph(), true, aController.getConfiguration());
	
	
		return this;
	}
	
	
	public ArrayList getSuccesors()
	{
		return this.getSplitter().getSuccesors();
	}
	
	
	/**
	 * Draw this SplitJoin
	 */
	public void draw()
	{
		System.out.println("Drawing the SplitJoin " +this.getName());
		// TO BE ADDED
	}	
	
	public void collapse(){};
}

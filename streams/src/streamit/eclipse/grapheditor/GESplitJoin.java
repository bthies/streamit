/*
 * Created on Jun 24, 2003
 */
package grapheditor;

import java.io.*;
import java.util.*;
import com.jgraph.graph.*;
import com.jgraph.JGraph;
import grapheditor.jgraphextension.*;
import java.awt.Point;
import java.awt.Rectangle;

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
					
		DefaultGraphModel model = new DefaultGraphModel();
		this.localGraphStruct.setGraphModel(model);
		JGraph jgraph = new JGraph(model);
		jgraph.addMouseListener(new JGraphMouseAdapter(jgraph));
		this.localGraphStruct.setJGraph(jgraph);
							
		frame = new LiveJGraphInternalFrame(this.localGraphStruct.getJGraph());
		this.localGraphStruct.internalFrame = frame;
		
		this.splitter.construct(this.localGraphStruct); 
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
		ArrayList lastNodeList = new ArrayList();
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = ((GEStreamNode) listIter.next());
			lastNodeList.add(strNode.construct(this.localGraphStruct)); 
			
			System.out.println("Connecting " + splitter.getName()+  " to "+ strNode.getName());	
			this.localGraphStruct.connectDraw(splitter, strNode); 
		}
		
		listIter =  lastNodeList.listIterator();
		
		this.joiner.construct(this.localGraphStruct); 
		
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			System.out.println("Connecting " + strNode.getName()+  " to "+ joiner.getName());
			this.localGraphStruct.connectDraw(strNode, joiner); 
		}	
	
		this.localGraphStruct.getGraphModel().insert(localGraphStruct.getCells().toArray(),localGraphStruct.getAttributes(), localGraphStruct.getConnectionSet(), null, null);
	
		this.port = new DefaultPort();
		this.add(this.port);
		frame.setGraphCell(this);

		frame.setGraphStruct(graphStruct);
		
		frame.setGraphModel(graphStruct.getGraphModel());
		frame.create(this.getName());
		frame.setSize(400, 400);
		
		
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setAutoSize(this.attributes, true);
		GraphConstants.setBounds(this.attributes, graphStruct.setRectCoords(this));
		
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
						
		graphStruct.internalFrame.getContentPane().add(frame);
		//graphStruct.internalFrame.getDesktopPane().add(frame);
	
		try 
		{	
			frame.setSelected(true);
		} 
		catch(Exception pve) {}
		
		/*
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct.getJGraph());
		manager.arrange();
		*/
		return this;
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
	
	 
	/**
	 * Draw this SplitJoin
	 */
	public void draw()
	{
		System.out.println("Drawing the SplitJoin " +this.getName());
		// TO BE ADDED
	}	
	
	
	/**
	 * Expand or collapse the GESplitJoin structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph 
	 */	
	public void collapseExpand(JGraph jgraph)
	{
		if(this.isInfoDisplayed)
		{		
			Rectangle rect = GraphConstants.getBounds(this.attributes);
			this.frame.setLocation(new Point(rect.x, rect.y));
			this.frame.setVisible(true);
		}
		else
		{
			this.frame.setLocation(GraphConstants.getOffset(this.attributes));
			this.frame.setVisible(false);
		}
	}	
}

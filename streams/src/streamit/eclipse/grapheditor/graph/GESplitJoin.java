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
 * of a splitter, a joiner, and the succesors of the splitter which in turn are also the 
 * parents of the joiner.
 * @author jcarlos
 */
public class GESplitJoin extends GEContainer implements Serializable{

	/**
	 * The splitter belonging to this splitjoin.
	 */	
	private GESplitter splitter;
	
	/**
	 * The joiner belonging to this splitjoin.
	 */
	private GEJoiner joiner;

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
	
		this.localGraphStruct = new GraphStructure();
		this.isExpanded = true; 
		//this.succesors = new ArrayList(); // added 2/2/04

		split.setEncapsulatingNode(this);
		join.setEncapsulatingNode(this);
		firstNode = splitter;
	}
	
	/**
	 * Constructs the splitjoin and returns the last node in the splitjoin that wil be connecting
	 * to the next graph structure.
	 */	
	public GEStreamNode construct(GraphStructure graphStruct, int lvel)
	{
		System.out.println("Constructing the SplitJoin " +this.getName());
		this.initiliazeNode(graphStruct, lvel);
		lvel++;
		
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
	
		// is the following line needed
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(this.getContainedElements().toArray(), false);
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
		GraphConstants.setBorderColor(this.attributes, Color.blue.darker());
		GraphConstants.setLineWidth(this.attributes, 4);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.TOP);
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);			
	}


	/**
	 * Initialize the fields and draw attributes for the GESplitJoin.
	 * @param graphStruct GraphStructure corresponding to the GESplitJoin.
	 * @param lvel The level at which the GESplitJoin is located.
	 */	
	public void initiliazeNode(GraphStructure graphStruct, int lvel)
	{
		this.level = lvel;
		graphStruct.containerNodes.addContainerToLevel(level, this);
		this.localGraphStruct = graphStruct;	
		this.initDrawAttributes(graphStruct, new Rectangle(new Point(100,100)));		
	}

	/**
	 * Expand or collapse the GESplitJoin structure depending on wheter it was already 
	 * collapsed or expanded. 
	 */	
	
	public void collapseExpand()
	{
		if (isExpanded)
		{
			this.collapse();
			
		}
		else
		{
			this.expand();
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
		
		this.isExpanded = true;
		
		for (int i = level; i >= 0; i--)
		{
			this.localGraphStruct.containerNodes.hideContainersAtLevel(i);
		}
		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();	
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
		
		this.isExpanded = false;
		
		for (int i = level - 1; i >= 0; i--)
		{
			this.localGraphStruct.containerNodes.hideContainersAtLevel(i);
		}	

		JGraphLayoutManager manager = new JGraphLayoutManager(this.localGraphStruct);
		manager.arrange();
	}
		

	/**
	 * Get all of the elements that are contained inside of the GESplitJoin. This includes
	 * the GESplitter and the GEJoiner.
	 * @return ArrayList The GEStreamNodes that belong to this GESplitJoin.
	 */
	 public ArrayList getContainedElements()
	 {
		ArrayList tempList = new ArrayList();
		tempList.add(this.splitter);
		//2/2/04 Object[] innerElements = this.splitter.getSuccesors().toArray();
		Object[] innerElements = this.getSuccesors().toArray();
		for (int i = 0; i < innerElements.length; i++)
		{
			tempList.add(innerElements[i]);
		}
		tempList.add(this.getJoiner());
		return tempList;
	 	
	 }
	 	 
	/**
	 * Get the joiner part of this GESplitJoin.
	 * @return GESJoiner corresponding to this GESplitJoin
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}
	
	/**
	 * Set the joiner for this GESplitJoin.
	 * @param joiner GEJoiner
	 */
	public void setJoiner(GEJoiner joiner)
	{
		this.joiner = (GEJoiner) joiner;
		this.joiner.setEncapsulatingNode(this);
	}
	
	/**
	 * Get the splitter part of this GESplitJoin.
	 * @return GESplitter corresponding to this GESplitJoin
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}

	/**
	 * Set the splitter for this GESplitJoin
	 * @param splitter GESplitter
	 */
	
	public void setSplitter(GESplitter splitter)
	{
		this.splitter = splitter;
		this.splitter.setEncapsulatingNode(this);
		this.firstNode = splitter;
	}
		
		
	public void moveNodePositionInContainer(GEStreamNode startNode, GEStreamNode endNode, int position){};
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
		Iterator childIter  = this.getSuccesors().iterator();
		while(childIter.hasNext())
		{
			out.println(tab + "add " + ((GEStreamNode) childIter.next()).name + "();");
		}
		out.println(tab + "join " + this.splitter.name + "();");
			
		out.println("}");
		out.println();
	}	

}

/*
 * Created on Jun 20, 2003
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.JLabel;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.utils.LayoutTools;

/**
 * GEPipeline is the graph internal representation of a pipeline. 
 * A GEPipeline is a GEContainer (it can hold other GEStreamNodes).
 * 
 * @author jcarlos
 */
public class GEPipeline extends GEContainer implements Serializable{
			
	private GEStreamNode lastNode;	

	/**
	 * GEPipeline constructor.
	 * @param name The name of this GEPipeline.
	 */
	public GEPipeline(String name)
	{
		super(GEType.PIPELINE, name);
		localGraphStruct = new GraphStructure();	
		this.isExpanded = true;
		this.info = Integer.toString(this.hashCode());
	}

	public GEPipeline(String name, GraphStructure gs)
	{
		super(GEType.PIPELINE, name);
		localGraphStruct = gs;	
		this.isExpanded = true;
		this.info = Integer.toString(this.hashCode());
	}
	
	/**
 	 * Constructs the pipeline and returns the last node so that the 
 	 * GEPipeline can be connected to its succesor and predecessor.
 	*/
	public GEStreamNode construct(GraphStructure graphStruct, int lvel)
	{
		boolean first = true;
		this.initializeNode(graphStruct, lvel);
		lvel++;
		
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
		
		/** Iterate through the elements contained by the GEPipeline.
		 * 	Construct these elements and connect them to each other in their respective order */
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			GEStreamNode lastTemp = strNode.construct(graphStruct, lvel); 	
			
			/** Only connect the previous node in the GEPipeline to the current one if this
			 * 	is not the first node in the GEPipeline */
			if(!first)
			{		 
				/** If we encounter a GEContainer, we must connect the last node to the first node in that container */
				if (strNode instanceof GEContainer)
				{
					graphStruct.connectDraw(lastNode, ((GEContainer)strNode).getFirstNodeInContainer()); 
				}
				else
				{
					graphStruct.connectDraw(lastNode, strNode);
				}
			}
			lastNode = lastTemp;
			first = false;
		}
		
		/** Return the last Node belonging to the GEPipeline */	
		return lastNode;
	}	
	
	/**
	 * Initialize the default attributes that will be used to draw the GEPipeline.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		/** Add the port to the GEPipeline */
		this.port = new DefaultPort();
		this.add(this.port);
		
		/** Set the attributes corresponding to the GEPipeline */
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setBorderColor(this.attributes, Color.red.darker());
		GraphConstants.setLineWidth(this.attributes, 4);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.TOP);
		GraphConstants.setMoveable(this.attributes, false);
		GraphConstants.setValue(this.attributes, this.getInfoLabel());
		
		/** Insert the GEPipeline into the graph model */
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);	
	}
	
	

	
	/**
	 * Writes the textual representation of the GEStreamNode to the StringBuffer. 
	 * In this case, the textual representation corresponds to the the StreamIt source code 
	 * equivalent of the GEStreamNode. 
	 * @param strBuff StringBuffer that is used to output the textual representation of the graph.
	 * @param nameList List of the names of the nodes that have already been added to the template code.  
	 */
	public void outputCode(StringBuffer strBuff, ArrayList nameList)
	{
		if ( ! (nameList.contains(this.getName())))
		{
			nameList.add(this.getName());
					
			/** Create the basic definition for the GEStreamNode */
			strBuff.append(Constants.newLine + this.inputTape)
					.append("->")
					.append(this.outputTape + " ")
					.append(GEType.GETypeToString(this.type)+" ")
					.append(this.getName() + this.outputArgs() + " {" + Constants.newLine);
			
			/** Get the list that contains the information regarding the sequences of adjacent
			 * 	nodes with the same name (the number of adjacent nodes that must have the same 
			 * 	name in order to be included in this list is determined by Constants.DEFAULT_ADJACENT */
			ArrayList adjacentNodes = this.searchAdjacentNodes();
			
			/** Establish the value of the first Point pt that contains information regarding the 
			 * 	sequences of adjacent nodes */
			Point pt = new Point(-1,-1);
			int nextPt = 0;
			if (adjacentNodes.size() != 0)
			{
				pt = (Point) adjacentNodes.remove(nextPt);
			}
			
			/** Specify the inner elements in the GEPipeline*/
			ArrayList containedList = this.getContainedElements();
			for (int index = 0; index < containedList.size(); index++)
			{
				/** Handle the case when we have to substitute a "for" loop instead of 
				   	explicitly printing each of the "add" statements. */
				if (pt.x == index)
				{
					this.printForLoopCode(strBuff, ((GEStreamNode) containedList.get(index)).getName(), pt);
					index += pt.y -1 ;
					/** If there are more items in the adjacentNodes list, then 
					 * 	we have to update the value of pt. */
					if (adjacentNodes.size() != 0)
					{
						pt = (Point)adjacentNodes.remove(nextPt++);
					}
				}
				/** Case when we have to print the "add" statement explicitly */
				else
				{
					strBuff.append(Constants.TAB + "add " + ((GEStreamNode) containedList.get(index)).getName() + "();" + Constants.newLine);
				}
				
				
			}
					
			strBuff.append("}");
		
			/** Output the code for all the elements contained in this GEContainer */
			Iterator containedIter = this.getContainedElements().iterator();		    
			while (containedIter.hasNext())
			{
				((GEStreamNode) containedIter.next()).outputCode(strBuff, nameList); 	
			}
		}
	}

	/** Modify the relative positions of the nodes inside of the GEPipeline when a new node has been 
	 * 	added to the pipeline.
	 * 	@param startNode GEStreamNode 
	 * 	@param endNode GEStreamNode
	 * 	@param position int The relative position at which the endNode will be added with respect to the startNode.
	 */
	public void moveNodePositionInContainer(GEStreamNode startNode, GEStreamNode endNode, int position)
	{
		ArrayList startParentChildren = startNode.getEncapsulatingNode().getSuccesors();
		int index = startParentChildren.indexOf(startNode);
		
		/** Remove the endNode from its previous position */
		startParentChildren.remove(endNode);
		
		/** Place the endnode after the startNode in the pipeline */
		if (position == RelativePosition.AFTER)
		{
			if (index >= startParentChildren.size())
			{
				System.err.println("The index array was larger than expected moveNodePositionInContainer in GEPipeline");
				startParentChildren.add(index, endNode);
			}
			else
			{
				startParentChildren.add(index + 1, endNode);
			}
		}
		/** Place the endNode before the startNode in the pipeline */
		else if (position == RelativePosition.BEFORE)
		{
			startParentChildren.add(index, endNode);
		}
	}
	
	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
 	 * not a container node, then a list with no elements is returned.
 	 * @return ArrayList of contained elements. If <this> is not a container, return empty list.
 	 */
	public ArrayList getContainedElements()
	{
		return this.getSuccesors();
	}	

	/**
	 * Get the properties of the GESplitJoin.
	 * @return Properties of this GESplitJoin.
	 */
	public Properties getNodeProperties()
	{
		Properties properties =  super.getNodeProperties();
		return properties;	
	}

	/**
	 * Set the properties of the GEStreamNode.  
	 * @param properties Properties
	 * @param jgraph JGraph 
	 * @param containerNodes ContainerNodes
	 */
	public void setNodeProperties(Properties properties, JGraph jgraph, ContainerNodes containerNodes)
	{
		super.setNodeProperties(properties, jgraph, containerNodes);
		setDisplay(jgraph);
	}

	/**
 	 * Get a clone that of this instance of the GEPipeline.
 	 * @return Object that is a clone of this instance of the GEPipeline
     */
	public Object clone() 
	{
		/** Clone the supertype of the GEPipeline */
		GEPipeline clonedPipeline = (GEPipeline) super.clone();
		
		/** Clone all the mutable fields contained by the GEPipeline object */
		clonedPipeline.succesors = new ArrayList();
		ArrayList tempList = new ArrayList();
	
		clonedPipeline.info = Integer.toString(clonedPipeline.hashCode());	
	
		/** Must clone all of the elements that are contained by the GEPipeline */
		ArrayList tempIterList =  new ArrayList (this.succesors);
		for (Iterator succIter = tempIterList.iterator(); succIter.hasNext();)
		{
			GEStreamNode clonedSucc = (GEStreamNode) ((GEStreamNode)succIter.next()).clone();
			clonedSucc.encapsulatingNode = clonedPipeline;
			tempList.add(clonedSucc);
		}
		
		clonedPipeline.succesors = tempList;

////		clonedPipeline.localGraphStruct.constructANode(clonedPipeline, clonedPipeline.encapsulatingNode,
////							  								new Rectangle(new Point(10,10)));
		/*					  
		clonedPipeline.localGraphStruct.constructNodes(clonedPipeline.succesors, clonedPipeline, 
															 new Rectangle(new Point(10,10)));
															 */
		return clonedPipeline;
	}

	/**
	 * Connect startNode to endNode if it is a valid connection.
	 * @param startNode 
	 * @param endNode
	 * @return 0 if it was possible to connect the nodes, otherwise, return a negative integer.
	 */
	public int connect (GEStreamNode startNode, GEStreamNode endNode)
	{
		int errorCode = super.connect(startNode, endNode);
		if (errorCode == 0)
		{
			/** Save the value of the nodes that we want to graphically connect. This is so since 
			 * 	the value of the startNode/endNode might change to nodes within a common container */
			GEStreamNode savedStart = startNode;
			GEStreamNode savedEnd  = endNode;
			
			/** Set the value of the startParent initially to the default values that will be used in 
			 * case that the endParent == startParent */
			GEContainer startParent = this;
			GEContainer endParent = endNode.getEncapsulatingNode();		
			ArrayList startParentChildren = startParent.getContainedElements();
			
			/** Handle the case when the endParent is equal to the end parent. The values of the 
			 * 	startParent, startNode and endNode might have to change to reflect the interconnection
			 * 	between different nodes */
			if (endParent != startParent)
			{
				/** Determine the deepest container (i.e. farthest away from the toplevel) that the two
				 * 	two nodes have in common. This will be the new startParent */	
				startParent = (GEContainer) endParent.findDeepestCommonContainer(startNode);
				startParentChildren = startParent.getContainedElements();
				System.out.println("***1 "+ startParent);
				
				/** Get the ancestor of the startNode that is contained by the new startParent */
				GEStreamNode actualStart = ((GEContainer)startParent).getAncestorInContainer(startNode); 
				System.out.println("***2 "+ actualStart);
				
				/** Get the ancestor of the endNode that is contained by the new endParent */
				GEStreamNode actualEnd = ((GEContainer)startParent).getAncestorInContainer(endNode);
				System.out.println("***3 "+ actualEnd);
				
				GEStreamNode node2 = startParent.findDeepestCommonContainer(endNode);
				System.out.println("***1 "+ node2);
				System.out.println("***5 "+((GEContainer)node2).getAncestorInContainer(startNode));
				System.out.println("***6 "+((GEContainer)node2).getAncestorInContainer(endNode));

				/** Either the actualStart of the actualEnd that we have calculated will have a value of
				 *  null. 
				 */
				if (actualStart == null)
				{
					endNode = actualEnd;
				}
				else if (actualEnd == null)
				{
					startNode = actualStart;
				}
				else
				{
					System.err.println("SPECIAL CASE");
					endNode = actualEnd;
					startNode = actualStart;
				}	
			}
			if  ( ! (startParent instanceof GEPipeline))
			{
				return startParent.connect(savedStart, savedEnd);				
			}

			/** Determine which nodes form a connected path ending at the startNode.
			 * 	These nodes have to be in the same parent as the startNode. The nodes that we find
			 * 	will be added to a temporary list. */
			GEStreamNode start = startNode;
			ArrayList startList = new ArrayList();
			startList.add(start);
			
			if (start.getTargetEdges().size() != 0)
			{
				DefaultEdge edge = (DefaultEdge) start.getTargetEdges().get(0);						
				while (true)
				{
					GEStreamNode node = (GEStreamNode)(((DefaultPort)edge.getSource()).getParent());
					if ( ! (startParentChildren.contains(node)))
					{
						GEStreamNode ancestorInCont = startParent.getAncestorInContainer(node);
						if (ancestorInCont == null)
						{
							break;
						}
						if ( ! (startList.contains(ancestorInCont)))
						{
							startList.add(ancestorInCont); // are we adding the same cont multiple times ?
						}
					}
					else
					{
						startList.add(node);	
					}
	
					if (node.getTargetEdges().size() != 0)
					{
						/** Must handle the special case of when the current node belongs to a GEFeedbackLoop
						 *  (in order to avoid getting stuck in an infinite loop when we are examining the connections)*/
						if ((node instanceof GEJoiner) && (node.getEncapsulatingNode().getType() == GEType.FEEDBACK_LOOP))
						{
							/** Boolean that determines wheter or not there is a connection input into the GEFeedbackLoop */
							boolean chainContinue = false; 
							for (Iterator sNodeIter = node.getSourceNodes().iterator(); sNodeIter.hasNext();)
							{
								GEStreamNode sNode = (GEStreamNode)sNodeIter.next();
								if ( ! (node.getEncapsulatingNode().hasAncestorInContainer(sNode)))
								{
									edge = (DefaultEdge) sNode.getSourceEdges().get(0);
									chainContinue = true;
								}	
							}
							/** The connection does not continue into the GEFeedbackLoop so break */
							if ( !chainContinue)
							{
								break;
							}
						}
						/** Standard Case */
						else
						{
							edge = (DefaultEdge)node.getTargetEdges().get(0);
						}	
					}
					else{
						break;
					}
				}
			}
			
			ArrayList newSuccesorList = new ArrayList();
			for (int i = startList.size() - 1; i >= 0; i--)
			{
				System.out.println(startList.get(i));
				newSuccesorList.add(startList.get(i));
			}
			
			/** 
			 * Determine which nodes form a connected path beginning at the endNode.
			 * These nodes have to be in the same parent as the startNode. The nodes that we find 
			 * will be added to a temporary list. */
			//GEStreamNode end =  endNode;
			GEStreamNode end =  savedEnd;
			ArrayList tempList = new ArrayList();
			tempList.add(endNode); 
			if (end.getSourceEdges().size() != 0)
			{
				DefaultEdge edge = (DefaultEdge) end.getSourceEdges().get(0);					
				while (true)
				{			
					GEStreamNode node = (GEStreamNode)(((DefaultPort)edge.getTarget()).getParent());
	
					if ( ! (startParentChildren.contains(node))) {
						GEStreamNode ancestorInCont = startParent.getAncestorInContainer(node);
						if (ancestorInCont == null) {
							break;
						}
						if ( ! (tempList.contains(ancestorInCont))) {
							tempList.add(ancestorInCont);
						}
					}
					else {
						tempList.add(node);	
					}
				
					/** The first condition in the if statement is to guarantee that we are still
					 * 	in the loop as long as the current node has any source edges. */
					if (node.getSourceEdges().size() != 0)  
					{
						/** Must handle the special case of when the current node belongs to a GEFeedbackLoop
						 *  (in order to avoid getting stuck in an infinite loop when we are examining the connections)*/
						if ((node instanceof GESplitter) && (node.getEncapsulatingNode().getType() == GEType.FEEDBACK_LOOP))
						{
							/** Boolean that determines wheter or not there is a connection output from the GEFeedbackLoop */
							boolean chainContinue = false;
							for (Iterator tNodeIter = node.getTargetNodes().iterator(); tNodeIter.hasNext();)
							{
								GEStreamNode tNode = (GEStreamNode)tNodeIter.next();
								if ( ! (node.getEncapsulatingNode().hasAncestorInContainer(tNode)))
								{
									edge = (DefaultEdge) tNode.getTargetEdges().get(0);
									chainContinue = true;
								}	
							}
							/** The connection does not continue out from the GEFeedbackLoop so break */ 
							if (!chainContinue)
							{
								break;
							}
						}
						/** Standard case */
						else
						{
							edge = (DefaultEdge)node.getSourceEdges().get(0);
						}	
					}
					else{
						break;
					}
				}
			}
			
			int j = 0;
			if (startNode == startParent.getFirstNodeInContainer())
			{
				j = startParentChildren.indexOf(startNode) + 1;
			}
			else
			{ 
				//j = startParent.getContainedElements().indexOf(startNode) ;
				GEStreamNode startN = startNode;
				while (startN != null)
				{
					j = startParent.getContainedElements().indexOf(startN) ;
					if (j >= 0)
					{
						startNode = startN;
						break;
					}
					startN = startN.getEncapsulatingNode();
				}
				
			}
			
	
			tempList.ensureCapacity(newSuccesorList.size());
			for (int i=0; i< newSuccesorList.size(); i++)
			{
				tempList.add(i, newSuccesorList.get(i));
			}
			
			int savedStartIndex = j;
			for (int i=0 ; i < tempList.size() ; i++)
			{
				System.out.println(tempList.get(i));
				
				int savedIndex =startParent.getSuccesors().indexOf(tempList.get(i));
				if (savedIndex < 0)
				{
					System.err.println("Encountered negative index in connect (Pipeline.java)");
					continue;
				}
		
				startParent.getSuccesors().remove(tempList.get(i));
				if (savedIndex < savedStartIndex) {
					j--;
				}
				if ( j > startParent.getSuccesors().size()) {
		
					System.out.println("Index larger than size");
					startParent.getSuccesors().add(tempList.get(i));
				}
				else {
					startParent.getSuccesors().add(j, tempList.get(i));
				}			
				j++;
			}
			this.localGraphStruct.connectDraw(savedStart, savedEnd);
			return 0;	
		}
		/** Must return errorCode generated by the GEPipeline's supertype. */
		else
		{
			return errorCode;
		}
	}
	
	
	/**
	 * Determine the dimension of the pipeline. This is determined by how many elements
	 * the pipeline has. The height is the sum of the heights of the elements. 
	 * The width is the maximum width of the elements inside the pipeline  
	 */
	public void calculateDimension()
	{
		Iterator childIter = this.getSuccesors().iterator();
		int height = 0;
		int width  = Constants.MIN_WIDTH;
	
		while (childIter.hasNext())
		{
			GEStreamNode node = (GEStreamNode) childIter.next();
			Dimension dim = node.getDimension();
			
			/** The height is the sum of the heights of the contained elements */
			height += dim.height;
			
			/** Find the contained element with the greatest width */
			if (dim.width > width)
			{
				width = dim.width;
			}	
		}
		
		/** Add the amount taken by the distance between each node (y-separation).
		 * The y-separation amount is one less than the number of elements in the GEContainer */
		height += (Constants.Y_SEPARATION * (this.getSuccesors().size() - 1));
		
		/** Set the dimension for this pipeline */
		this.setDimension(new Dimension(width, height));
	}
	
	/**
	 * Check if the pipeline is valid (all of the innner nodes are connected to something else)
	 * @return int No Error (0) if pipeline is valid; otherwise, return error code.
	 */
	public int checkValidity()
	{
		for (Iterator nodeIter = this.getContainedElements().iterator(); nodeIter.hasNext();)
		{
			GEStreamNode node = (GEStreamNode) nodeIter.next(); 
			if ( ! (node.isNodeConnected())) {
				if (node instanceof GEContainer) {
					if ( ! ((GEContainer) node).isExpanded()) {
						return ErrorCode.CODE_MISSING_CONNECTIONS_IN_PIPE;
					}
					else{
						return ErrorCode.NO_ERROR;
					}
				}
				else{
					return ErrorCode.CODE_MISSING_CONNECTIONS_IN_PIPE;	
				}
			}
		}
		return ErrorCode.NO_ERROR;
			
	}		
	/**
	 * Layout the elements that are contained by the GEPipeline.
	 *  
	 */
	public void layoutChildren()
	{
		if (this.isExpanded)
		{
			/** Get the location of the GEPipeline. This will be used as the base location
			 * 	for the offsets at which the contained elements are.
			 * 	This location is not the one obtained from JGraphs's GraphConstants. */
			Point pipeLoc = this.getLocation();
			
			/** The change in y will start at the pipeline's y location.
			 * 	This will be updated to reflect the position of all of the contained elements */
			int yChange = pipeLoc.y; 
			
			/** Iterate through the contained elements to set their location ... */
			ArrayList childIter = this.getSuccesors();
			for (int i = 0 ; i < childIter.size(); i++)
			{
				GEStreamNode node = (GEStreamNode) childIter.get(i);
				
				if (node instanceof GEContainer)
				{
					GEContainer cont = (GEContainer) node;
					/** Must set the current location of the node(if it is a container), and do the 
					  * layout for this GEContainer. */
					if (cont.isExpanded)
					{
						cont.setLocation(new Point(LayoutTools.center(this, cont), yChange));
						cont.layoutChildren();
					}
					else
					{
						cont.setGraphicalLocation(new Point(LayoutTools.center(this, cont), yChange), 
												  this.localGraphStruct.getJGraph());
					}
				}
				/** If we are not dealing with a container, then we just need to set 
				 * 	the graphical of the GEStreamNode */
				else
				{		
					node.setGraphicalLocation(new Point(LayoutTools.center(this, node), yChange), 
											  this.localGraphStruct.getJGraph());
				}			
				/** Update the change in y-coordinate by adding a separation and
				 * 	the height of the node we just dealt with to the accumulating yChange */
				yChange += (Constants.Y_SEPARATION + node.getDimension().height); 			
			}
		}	
	}
}


/*
 * Created on Jun 24, 2003
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
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;

import streamit.eclipse.grapheditor.graph.utils.LayoutTools;

/**
 * GESplitJoin is the graph internal representation of  a splitjoin.
 * The GESplitJoins is a GEContainer (can contain other GEStreamNodes).
 * A GESplitJoin is composed of a GESplitter, a GEJoiner, and other
 * GEStreamNodes that are connected between the GESplitter and GEJoiner.
 * The input into the GESplitJoin goes to the input of the GESplitter.
 * The ouput of the GESplitJoin comes from the output of the GEJoiner. 
 * 
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
		split.setEncapsulatingNode(this);
		join.setEncapsulatingNode(this);
		this.info = Integer.toString(this.hashCode());
	}
	
	/**
	 * Constructs the splitjoin and returns the last node in the splitjoin that wil be connecting
	 * to the next graph structure.
	 */	
	public GEStreamNode construct(GraphStructure graphStruct, int lvel)
	{
		/** Initialize the properties of the GESplitJoin */
		this.initializeNode(graphStruct, lvel);
		lvel++;
		
		/** Construct the splitter belonging to the GESplitJoin */
		this.splitter.construct(graphStruct, lvel);  
		ArrayList nodeList = (ArrayList) this.getSuccesors();
		Iterator listIter =  nodeList.listIterator();
		ArrayList lastNodeList = new ArrayList();
		
		/** Iterate throught the elements that will be connected between the GESplitter and the GEJoiner.
		 *  Construct these elements and connect them to the splitter. */
		while(listIter.hasNext())
		{
			GEStreamNode strNode = ((GEStreamNode) listIter.next());
			lastNodeList.add(strNode.construct(graphStruct,lvel));
			
			/** If we encounter a GEContainer, we must connect the splitter to the first node in that container */ 
			if (strNode instanceof GEContainer)
			{
				graphStruct.connectDraw(splitter, ((GEContainer)strNode).getFirstNodeInContainer()); 
			}
			else
			{
				graphStruct.connectDraw(splitter, strNode); //this.localGraphStruct.connectDraw(splitter, strNode);
			} 
		}
		listIter =  lastNodeList.listIterator();	
		
		/** Construct the joiner belonging to the GESplitJoin */
		this.joiner.construct(graphStruct, lvel);  
		
		/** Connect the inner elements of the GESplitJoin to the GEJoiner */
		while(listIter.hasNext())
		{
			GEStreamNode strNode = (GEStreamNode) listIter.next();
			// TODO: should we be connecting the last element of each node to the joiner ???
			graphStruct.connectDraw(strNode, joiner);  
		}	
	
		// is the following line needed ??
		graphStruct.getJGraph().getGraphLayoutCache().setVisible(this.getContainedElements().toArray(), false);
		
		/** Return the GEJoiner (which is the last element in the GESplitJoin */
		return this.joiner;
	}
	
	/**
	 * Initialize the default attributes that will be used to draw the GESplitJoin.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		/** Add the port to the GESplitJoin */
		this.port = new DefaultPort();
		this.add(this.port);
		
		/** Set the attributes corresponding to the GESplitJoin */
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setBorderColor(this.attributes, Color.blue.darker());
		GraphConstants.setLineWidth(this.attributes, 4);
		GraphConstants.setBounds(this.attributes, bounds);
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.TOP);
		GraphConstants.setMoveable(this.attributes, false);
		GraphConstants.setValue(this.attributes, this.getInfoLabel());
		
		/** Insert the GESplitJoin into the graph model */
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);			
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
		Object[] innerElements = this.getSuccesors().toArray();
		for (int i = 0; i < innerElements.length; i++)
		{
			tempList.add(innerElements[i]);
		}
		tempList.add(this.getJoiner());
		return tempList;
	 	
	 }
	 	 
	/**
	 * Get the joiner of this GESplitJoin.
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
	 * Get the splitter of this GESplitJoin.
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
	}
	
	/** 
	 * Get the index of the node passed as an argument. If the node is not 
	 * contained in the splitjoin then return a -1.
	 * @param node GEStreamNode
	 * @return The index of the node within the spltitjoin, or -1 if the node is 
	 * not contained by the splitjoin.
	 */
	public int getIndexOfInnerNode(GEStreamNode node)
	{
		return this.getSuccesors().indexOf(node);
	}
	

	/**
	 * Change the display information of the GESplitter.
	 * @param jgraph JGraph
	 */
	public void setDisplay(JGraph jgraph)
	{
		if (this.splitter != null) {
			this.splitter.setDisplay(jgraph);	
		}
		
		if (this.joiner != null) {
			this.joiner.setDisplay(jgraph);	
		}
		
		super.setDisplay(jgraph);
		
	}
	

	/**
	 * Get the properties of the GESplitJoin.
	 * @return Properties of this GESplitJoin.
	 */
	public Properties getNodeProperties()
	{
		Properties properties =  super.getNodeProperties();
		properties.put(GEProperties.KEY_SPLITTER_WEIGHTS, this.getSplitter().getWeightsAsString());
		properties.put(GEProperties.KEY_JOINER_WEIGHTS, this.getJoiner().getWeightsAsString());
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
	 * Add an inner node (the nodes that do not include the splitter and joiner - also
	 * referred as succesors) to the splitjoi
	 * @param index int The index at which the node will be added.
	 * @param node The GEStreamNode that will be added at the given node.
	 */
	public boolean addInnerNodeAtIndex(int index, GEStreamNode node)
	{
		/** Cannot add the innernode at the given index if it is the splitter
		 * or the joiner of the splitjoin */
		if ((node == this.splitter) || (node == this.joiner))
		{
			return false;
		}

		int splitterWeight = 0;
		int joinerWeight = 0;
		
		/** Check if the node that we are trying to add is alredy contained in the node */
		if (this.succesors.contains(node))
		{
			/** Trying to insert the node at the same location. Just
			 * 	get out of the function since there is no need to do anything */
//			if (oldIndex == index)
//			{
//				return false;
//			}

			int oldIndex = this.succesors.indexOf(node);
			/** Remove the node since it is already there, but we are trying to insert
			 * it in a different location */
			this.succesors.remove(node);
		
			splitterWeight = this.splitter.getWeightAt(oldIndex);
			joinerWeight = this.joiner.getWeightAt(oldIndex);
			this.splitter.removeWeightAt(oldIndex);
			this.joiner.removeWeightAt(oldIndex);
		}
		else
		{
			if (node.getEncapsulatingNode() != null)
			{
				node.getEncapsulatingNode().removeNodeFromContainer(node);
			}
			node.setEncapsulatingNode(this);
			splitterWeight = Constants.DEFAULT_WEIGHT;
			joinerWeight = Constants.DEFAULT_WEIGHT;
			
		}
		
		int size = this.succesors.size();
		if ((index >= size) || (index < 0))
		{
			this.succesors.add(node);
			
		}

		/** However, this case was affected by the removal so we have to update 
		 * accordingly */
		else
		{
			this.succesors.add(index, node);	
		}
		
		this.splitter.addWeightAt(index, splitterWeight);		
		this.joiner.addWeightAt(index, joinerWeight);

			
		return true;
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
		/** Only create the template code if the node has not been visited */
		if ( ! (nameList.contains(this.getName())))
		{
			nameList.add(this.getName());
			String tab = "     ";
			String newLine = "\n";
				
			/** Create the basic definition for the GEStreamNode */
			strBuff.append(newLine + this.inputTape)
					.append("->")
					.append(this.outputTape + " ")
					.append(GEType.GETypeToString(this.type)+" ")
					.append(this.getName() + this.outputArgs() + " {" + newLine);
				
			/** Specify the inner elements in the GESplitJoin*/
			strBuff.append(tab + "split " + this.splitter.getName()+ "();" + newLine);	


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
			ArrayList succList = this.getSuccesors();
			for (int index = 0; index < succList.size(); index++)
			{
				/** Handle the case when we have to substitute a "for" loop instead of 
					explicitly printing each of the "add" statements. */
				if (pt.x == index)
				{
					this.printForLoopCode(strBuff, ((GEStreamNode) succList.get(index)).getName(), pt);
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
					strBuff.append(Constants.TAB + "add " + ((GEStreamNode)	succList.get(index)).getName() + "();" + Constants.newLine);
				}	
			}
			strBuff.append(tab + "join " + this.joiner.getName() + "();" + newLine);
				
			strBuff.append("}" +newLine);
			
			
			/** Output the code for all the elements contained in this GEContainer */
			Iterator containedIter = this.getContainedElements().iterator();		    
			while (containedIter.hasNext())
			{
				((GEStreamNode) containedIter.next()).outputCode(strBuff, nameList); 	
			}
			
		}
	}	
	
	/**
	 * Connect startNode to endNode if it is a valid connection.
	 * @param startNode 
	 * @param endNode
	 * @param nodesConnected Determines which of the nodes are already connected (present)
	 * in the graph.
	 * @return 0 if it was possible to connect the nodes, otherwise, return a negative integer.
	*/
	public int connect (GEStreamNode startNode, GEStreamNode endNode)
	{
		int errorCode = super.connect(startNode, endNode);
		if (errorCode == 0)
		{
			GEContainer startParent = this;
			GEContainer endParent = endNode.getEncapsulatingNode();
	
			GESplitJoin splitjoin = (GESplitJoin) startParent;
			GESplitter splitter = splitjoin.getSplitter();
			GEJoiner joiner =  splitjoin.getJoiner();
			ArrayList splitjoinSuccesors = splitjoin.getSuccesors();
				
			/** In order to make connections, we must have a valid splitjoin with a splitter */
			if (splitter == null)
			{ 
				return ErrorCode.CODE_NO_SPLITTER;
			}
			/** In order to make connections, we must have a valid splitjoin with a joiner */
			if (joiner == null)
			{
				return ErrorCode.CODE_NO_JOINER;
			}
				
			/** The splitter of the splitjoin can never be the endNode */
			if (splitter == endNode)
			{
				return ErrorCode.CODE_SPLITTER_ENDNODE_SJ;
			}
								
			/** Cannot connect the splitter to the joiner (in either direction)*/
			//TODO: should we be using .equals ????
			if ((startNode == splitter) && (endNode == joiner) ||
				(startNode == joiner) && (endNode == splitter))
			{
					return ErrorCode.CODE_SPLITTER_JOINER_CONNECT_SJ;	
			} 	
			
			/** The inner nodes of the splitjoin can only connect to the joiner */
			if ((startNode != splitter) && (startNode != joiner) && (endNode != joiner))
			{
				return ErrorCode.CODE_INNERNODES_SJ;
			}
				
			/** Connecting splitter to something inside the same parent */
			if (startNode == splitter) 
			{
				if (this.hasAncestorInContainer(endNode))
				{
					this.localGraphStruct.connectDraw(startNode, endNode);	
				}
				else
				{
					return ErrorCode.CODE_NO_ANCESTOR_CONNECTION;
				}
			}
			
			/** Connecting joiner to something inside the same parent */
			else if (endNode == joiner)
			{
				this.localGraphStruct.connectDraw(startNode, joiner);
			}
			/** Connecting the joiner to something in a different parent */
			else if (startNode == joiner) 
			{
				if (this.hasAncestorInContainer(endNode))
				{
					return ErrorCode.CODE_JOINER_SAME_PARENT_CONNECT_SJ;
				}
				else
				{
					/** Have to adjust the indices of the nodes connected within the parent
					 * 	that contains them */
					return this.getEncapsulatingNode().connect(startNode, endNode);
					//this.localGraphStruct.connectDraw(startNode, endNode);
				}
			}
			/** Other alternatives are not allowed (or possible) */
			else 
			{ 
				return -999;
			}
			return 0;
		}
		else
		{
			return errorCode;
		}
	}
	
	/**
	 * Get a clone that of this instance of the GESplitJoin.
	 * @return Object that is a clone of this instance of the GESplitJoin.
	 */
	public Object clone() 
	{
		/** Clone the supertype of the GESplitJoin */
		GESplitJoin clonedSplitjoin = (GESplitJoin) super.clone();
		
		/** Clone the splitter of the splitjoin */
		if (this.splitter != null)
		{
			clonedSplitjoin.setSplitter((GESplitter)this.splitter.clone());
		}
		
		/** Clone the joiner of the splitjoin */
		if (this.joiner != null)
		{
			clonedSplitjoin.setJoiner((GEJoiner) this.joiner.clone());
		}
		
		/** Clone all the mutable fields contained by the GESplitJoin object */
		clonedSplitjoin.succesors = new ArrayList();
		ArrayList tempList = new ArrayList();
	
		clonedSplitjoin.info = Integer.toString(clonedSplitjoin.hashCode());	
	
		/** Must clone all of the elements that are contained by the GESplitJoin */
		ArrayList tempIterList =  new ArrayList (this.succesors);
		for (Iterator succIter = tempIterList.iterator(); succIter.hasNext();)
		{
			GEStreamNode clonedSucc = (GEStreamNode) ((GEStreamNode)succIter.next()).clone();
			clonedSucc.encapsulatingNode = clonedSplitjoin;
			tempList.add(clonedSucc);
		}
		
		clonedSplitjoin.succesors = tempList;
		return clonedSplitjoin;
	}
	
	
	
	
	
	
	/**
	 * Determine the dimension of the splitjoin. This is determined by how many elements
	 * the splitjoin has. The height is the sum of the heights of joiner, splitter and the
	 * element with the maximum height. The width is the sum of the widths of the elements 
	 * inside of the splitjoin.   
	 */
	public void calculateDimension()
	{
		Iterator childIter = this.getSuccesors().iterator();
		int height = 0;
		int width  = 0;
		while (childIter.hasNext())
		{
			GEStreamNode node = (GEStreamNode) childIter.next();
			Dimension dim = node.getDimension();
			
			/** The height is the sum of the heights of the contained elements */	
			width += dim.width;
			
			/** Find the contained element with the greatest height */
			if (dim.height > height)
			{
				height = dim.height;
			}	
		}
		
		/** Add the amount taken by the distance between each node (x-separation).
		* The x-separation amount is one less than the number of elements in the GEContainer */
		width += (Constants.X_SEPARATION * (this.getSuccesors().size() - 1));
		
		/** We must add the splitter height, joiner height, and their respective
		 * 	y-separations to the accumulating height.*/	
		height += this.splitter.getDimension().height + 
				  (2 * Constants.Y_SEPARATION) + 
				  this.joiner.getDimension().height; 
	
		/** Set the dimension of the splitjoin */
		this.setDimension(new Dimension(width, height));
	}
	
	/**
	 * Layout the elements that are contained by the GEPipeline.
	 * The splitJoin must contain a splitter and a joiner. 
	 *  
	 */
	public void layoutChildren()
	{
		if (this.isExpanded)
		{
			/** The splitjoin must contain a joiner */
			if (this.joiner == null)
			{
				ErrorCode.handleErrorCode(ErrorCode.CODE_NO_JOINER);
				return;
			}
			
			/** The splitjoin must containe a splitter */
			if (this.splitter == null)
			{
				ErrorCode.handleErrorCode(ErrorCode.CODE_NO_SPLITTER);
				return;
			}
			
			
			Point sjLoc = this.getLocation();		
			int yChange = sjLoc.y; 
			int xChange = sjLoc.x;
		
			/** Set the location for the splitter */
			splitter.setGraphicalLocation(new Point(LayoutTools.center(this, splitter), yChange), 
										this.localGraphStruct.getJGraph());
			yChange += (Constants.Y_SEPARATION + splitter.getDimension().height);
		
			/** Set the location for the inner nodes */
			ArrayList childIter = this.getSuccesors();
			int largestHeight = 0;
			for (int i = 0 ; i < childIter.size(); i++)
			{
				GEStreamNode node = (GEStreamNode) childIter.get(i);
				Dimension nodeDim = node.getDimension();
				
		
				if (node instanceof GEContainer)
				{
					GEContainer cont = (GEContainer) node;
					/** If the node is a container,we have to set its location and then do the 
					  * layout of the elements that it contains. */
					if (cont.isExpanded)
					{
						cont.setLocation(new Point(xChange, yChange));
						cont.layoutChildren();
					}
					else
					{
						cont.setGraphicalLocation(new Point(xChange, yChange), 
		 											this.localGraphStruct.getJGraph());	
					}
				}
				/** Otherwise, if the node is not a container, 
				 * 	then just set its graphical location */
				else
				{		
					node.setGraphicalLocation(new Point(xChange, yChange), 
													this.localGraphStruct.getJGraph());
				}
	
				xChange += (Constants.X_SEPARATION + nodeDim.width);
	
				/** Must determine what is the largest height for a node 
				 * in the inner nodes of the splitjoin. This will allow us to determine
				 * where the joiner will be placed */
				if (nodeDim.height > largestHeight)
				{ 
					largestHeight = nodeDim.height;
				}
				
			}
			
			/** Update the yChange with the largest height from one of the inner nodes */
			yChange += (Constants.Y_SEPARATION + largestHeight);
			
			/** Set the location for the joiner */
			joiner.setGraphicalLocation(new Point(LayoutTools.center(this, joiner), yChange), 
										this.localGraphStruct.getJGraph());
			
			yChange += (Constants.Y_SEPARATION + joiner.getDimension().height);
		}
	}
	

	
	/**
	 * Arrange the order of the inner elements of the GESplitJoin.
	 * @param startNode GEStreamNode
	 * @param endNode GEStreamNode
	 * @param position int
	 */
	public void moveNodePositionInContainer(GEStreamNode startNode, GEStreamNode endNode, int position){};	
}
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
 * GEFeedbackLoop is the graph internal representation of  a feedback loop.
 * A GEFeedbackLoop must have a GEJoiner, GESplitter, a body, and a loop.
 * The GEJoiner has as inputs the output of the loop and the output of the 
 * element that is the input to the GEFeedbackLoop. The body has the output of
 * the GEJoiner as its input. The output of the body is connected to the GESplitter.
 * The GESplitter has outputs to the loop and to the element whose input is the output
 * of the GEFeedbackLoop. 
 * 
 * @author jcarlos
 */
public class GEFeedbackLoop extends GEContainer implements Serializable{
	
	/**
	 * The splitter belonging to this feedback loop.
	 */
	private GESplitter splitter;
	
	/**
	 * The joiner belonging to this feedback loop.
	 */
	private GEJoiner joiner;
	
	/**
	 * The body of the feedback loop.
	 */
	private GEStreamNode Initbody;
	
	/**
	 * The loop part of the feedback loop.
	 */
	private GEStreamNode Initloop;

	/**
	 * GEFeedbackLoop constructor.
	 * @param name The name of the GEFeedbackLoop.
	 * @param split The GESplitter that corresponds to this GEFeedbackLoop.
	 * @param join The GEJoiner that corresponds to this GEFeedbackLoop.
	 * @param body The GEStreamNode that represents the body of theGEFeedbackLoop.
	 * @param loop The GEStreamNode that represents the body of the GEFeedbackLoop.
	 */
	public GEFeedbackLoop(String name, GESplitter split, GEJoiner join, 
						  GEStreamNode body, GEStreamNode loop)
	{
		super(GEType.FEEDBACK_LOOP, name);
		this.splitter = split;
		this.joiner = join;
		this.Initbody = body;
		this.Initloop = loop;
		this.localGraphStruct = new GraphStructure();
		
		//TODO: might have to deal with this later 
		this.isExpanded = true;

//		this.addNodeToContainer(join);
		join.setEncapsulatingNode(this);
		if (body != null)
		{
			this.addNodeToContainer(body);
		}
		if (loop != null)
		{
			this.addNodeToContainer(loop);	
		}
	
		split.setEncapsulatingNode(this);
		this.info = Integer.toString(this.hashCode());
//		this.addNodeToContainer(split);
	}

	/**
	 * GEFeedbackLoop constructor.
	 * @param name The name of the GEFeedbackLoop.
	 * @param split The GESplitter that corresponds to this GEFeedbackLoop.
	 * @param join The GEJoiner that corresponds to this GEFeedbackLoop.
	 * @param body The GEStreamNode that represents the body of theGEFeedbackLoop.
	 * @param loop The GEStreamNode that represents the body of the GEFeedbackLoop.
	 * @param gs The GraphStructure that this is part of.
	 */
	public GEFeedbackLoop(String name, GESplitter split, GEJoiner join, 
						  GEStreamNode body, GEStreamNode loop, GraphStructure gs)
	{
		super(GEType.FEEDBACK_LOOP, name);
		this.splitter = split;
		this.joiner = join;
		this.Initbody = body;
		this.Initloop = loop;
		this.localGraphStruct = gs;
		this.isExpanded = true;
	}


	/**
	 * Construct the GEFeedbackLoop by constructing the nodes that it contains
	 * and making the corresponding connections. 
	 * @return GEStreamNode returns the last node in the GEFeedbackLoop.
	 */
	public GEStreamNode construct(GraphStructure graphStruct, int lvel)
	{
		this.initializeNode(graphStruct, lvel);
		lvel++;			
					
		joiner.construct(graphStruct, lvel);
		
		GEStreamNode body = this.Initbody;
		/** Construct the body */
		GEStreamNode lastBody = body.construct(graphStruct, lvel);
		
		/** If body is a container, connect joiner to first element in the body. */
		if (body instanceof GEContainer)
		{
			graphStruct.connectDraw(joiner,((GEContainer)body).getFirstNodeInContainer());
		}
		/** Otherwise, connect the joiner to the body */
		else
		{
			graphStruct.connectDraw(joiner, body);	
		}
		/** Construct the splitter */
		splitter.construct(graphStruct, lvel);
		
		/** Connect the body to the splitter (will connect the last element of the 
		 * 	body in case it is a GEContainer. */
		graphStruct.connectDraw(lastBody, splitter);
	
		GEStreamNode loop = this.Initloop;
		/** Construct the loop */
		GEStreamNode lastLoop = loop.construct(graphStruct, lvel);
		
		/** If loop is a container, connect splitter to first element in the loop. */
		if(loop instanceof GEContainer)
		{
			graphStruct.connectDraw(splitter, ((GEContainer)loop).getFirstNodeInContainer());
		}
		/** Otherwise, connect the loop to the body */
		else
		{
			graphStruct.connectDraw(splitter, loop);	
		}
		
		/** Connect the last element of the loop to the joiner */
		graphStruct.connectDraw(lastLoop, joiner);
		return this.splitter;
	}
	

	
	
	/**
	 * Initialize the default attributes that will be used to draw the GESplitJoin.
	 * @param graphStruct The GraphStructure that will have its attributes set.
	 */	
	public void initDrawAttributes(GraphStructure graphStruct, Rectangle bounds)
	{
		/** Add the port to the GEFeedbackLoop */
		this.port = new DefaultPort();
		this.add(this.port);
		
		/** Set the attributes corresponding to the GEFeedbackLoop */
		(graphStruct.getAttributes()).put(this, this.attributes);
		GraphConstants.setBorderColor(this.attributes, Color.green.darker());
		GraphConstants.setLineWidth(this.attributes, 4);
		GraphConstants.setBounds(this.attributes, bounds); // without this - problems with expansion in layout
		GraphConstants.setVerticalTextPosition(this.attributes, JLabel.TOP);
		GraphConstants.setMoveable(this.attributes, false);
		GraphConstants.setValue(this.attributes, this.getInfoLabel());
		
		/** Insert the GEFeedbackLoop into the graph model */
		(graphStruct.getGraphModel()).insert(new Object[] {this}, null, null, null, null);
		graphStruct.getGraphModel().edit(graphStruct.getAttributes(), graphStruct.getConnectionSet(), null, null);
	}
	
	/** Returns of all the nodes contained by this GEFeedbackLoop. 
	 * 	@return ArrayList of contained elements.
	 */
	public ArrayList getContainedElements()
	{
//		ArrayList tempList = new ArrayList();
//		tempList.addAll(this.succesors);
//		return tempList;
		ArrayList tempList = new ArrayList();
		tempList.add(this.getJoiner());
		tempList.addAll(this.succesors);
		tempList.add(this.getSplitter());
		return tempList;
	}



	/**
	 * Get the splitter belonging to the feedbackloop.
	 * @return GESplitter corresponding to this GEFeedbackLoop.
	 */
	public GESplitter getSplitter()
	{
		return this.splitter;
	}
	
	/**
	 * Get the joiner belonging to the feedbackloop.
	 * @return GESJoiner corresponding to this GEFeedbackLoop.
	 */
	public GEJoiner getJoiner()
	{
		return this.joiner;
	}	
	
	/**
	 * Get the body belonging to the feedbackloop.
	 * @return GEStreamNode that is the body of GEFeedbackLoop.
	 */
	public GEStreamNode getBody()
	{
		GEStreamNode body1 = null;
		GEStreamNode body2 = null;
		if (splitter != null)
		{
			/** Find the node that is connected to the splitter (it is the target in the connection 
			 * 	between the two of them) and has an ancestor in the container */
			for (Iterator splitIter = this.splitter.getSourceNodes().iterator(); splitIter.hasNext();)
			{
				GEStreamNode node = (GEStreamNode)splitIter.next();
				if (this.succesors.contains(node))
				{
					body1 = node;
					break;	
				}
				else if (this.hasAncestorInContainer(node))
				{
					body1 = this.getAncestorInContainer(node);
					break;
				}

			}
		}
		if (joiner != null)
		{
			/** Find the node that is connect to the joiner (it is the source in the connection between 
			 *  the two) and it also has an ancestor in the container. */
			for (Iterator joinerIter = this.joiner.getTargetNodes().iterator(); joinerIter.hasNext();)
			{
				GEStreamNode node = (GEStreamNode)joinerIter.next();
				if (this.succesors.contains(node))
				{
					body2 = node;
					break;	
				}
				else if (this.hasAncestorInContainer(node))
				{
					body2 = this.getAncestorInContainer(node);
					break;
				}
			}

		}

		if ((body1 != null) && (body2 != null) && (body1 == body2))
		{
			return body1;
		}
		/** Return null if there is no node that satisfies the loop conditions */
		else
		{
			return null;	
		}
	}
	
	/**
	 * Get the loop belonging to the feedbackloop.
	 * @return GEStreamNode that is the loop of GEFeedbackLoop.
	 */
	public GEStreamNode getLoop()
	{
		GEStreamNode loop1 = null;
		GEStreamNode loop2 = null;
		if (splitter != null)
		{
			/** Find the node that is connected to the splitter (it is the target in the connection 
			 * 	between the two of them) and has an ancestor in the container */
			for (Iterator splitIter = this.splitter.getTargetNodes().iterator(); splitIter.hasNext();)
			{
				GEStreamNode node = (GEStreamNode)splitIter.next();
				if (this.succesors.contains(node))
				{
					loop1 = node;
					break;	
				}
				else if (this.hasAncestorInContainer(node))
				{
					loop1 = this.getAncestorInContainer(node);
					break;
				}
			}
		}
		if (joiner != null)
		{
			/** Find the node that is connect to the joiner (it is the source in the connection between 
			 *  the two) and it also has an ancestor in the container. */
			for (Iterator joinerIter = this.joiner.getSourceNodes().iterator(); joinerIter.hasNext();)
			{
				GEStreamNode node = (GEStreamNode)joinerIter.next();
				if (this.succesors.contains(node))
				{
					loop2 = node;
					break;	
				}
				else if (this.hasAncestorInContainer(node))
				{
					loop2 = this.getAncestorInContainer(node);
					break;
				}
			}
	
		}
		
		if ((loop1 != null) && (loop2 != null) && (loop1 == loop2))
		{
			return loop1;
		}
		/** Return null if there is no node that satisfies the loop conditions */
		else
		{
			return null;	
		}
	}
	
	
	
	
	/**
	 * Determine if the given node is the loop of the feedbackloop.
	 * @param node GEStreamNode
	 * @return True if the node is the loop of the feeedbackloop; otherwise, false.
	 */
	public boolean isLoop(GEStreamNode node)
	{
		return this.getLoop() == node;
	}
	
	/**
	 * Determine if the given node is the body of the feedbackloop
	 * @param node GEStreamNode
	 * @return True if the node is the body of the feeedbackloop; otherwise, false.
	 */
	public boolean isBody(GEStreamNode node)
	{
		return this.getBody() == node;
	}
	

	/**
	 * Get the properties of the GEFeedbackLoop.
	 * @return Properties of the GEFeeedbackLoop.
	 */
	public Properties getNodeProperties()
	{
		Properties properties =  super.getNodeProperties();
		
		//TODO implement the case for feedbackloop in getNodeProperties()
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
			String tab = "     ";
			String newLine = "\n";
			
			/** Create the basic definition for the GEStreamNode */
			strBuff.append(newLine + this.inputTape)
					.append("->")
					.append(this.outputTape + " ")
					.append(GEType.GETypeToString(this.type)+" ")
					.append(this.getName() + this.outputArgs() + " {" + newLine);
			
			/** Specify the inner elements in the GEFeedbackLoop*/
			strBuff.append(tab + "join " + this.joiner.getName() + "();" + newLine);
			strBuff.append(tab + "body " + this.getBody().getName() + "();" + newLine);
			strBuff.append(tab + "loop " + this.getLoop().getName() + "();" + newLine);
			strBuff.append(tab + "split " + this.splitter.getName() + "();" + newLine);			
			strBuff.append("}" + newLine);
		
			/** Output the code for all the elements contained in this GEContainer */
			Iterator containedIter = this.getContainedElements().iterator();		    
			while (containedIter.hasNext())
			{
				((GEStreamNode) containedIter.next()).outputCode(strBuff, nameList); 	
			}
			this.setVisited(true);
		}
	}
	
	/**
	 * Connect startNode to endNode if it is a valid connection.
	 * @param startNode GEStreamNode 
	 * @param endNode GEStreamNode
	 * @return 0 if it was possible to connect the nodes, 
	 * otherwise, return a negative integer signaling an error code.
	 */
	public int connect (GEStreamNode startNode, GEStreamNode endNode)
	{
		int validConnection = super.connect(startNode, endNode);
		if (validConnection == 0)
		{
			GEContainer startParent = this;
			GEContainer endParent = endNode.getEncapsulatingNode();	

			GEFeedbackLoop floop = (GEFeedbackLoop) startParent;
			GESplitter splitter = floop.getSplitter();
			GEJoiner joiner =  floop.getJoiner();

			/** In order to make connections, we must have a valid feedbackloop with a joiner */
			if (joiner == null)
			{
				return ErrorCode.CODE_NO_JOINER;	
			}
			/** In order to make connections, we must have a valid feedbackloop with a splitter */
			if (splitter == null)
			{
				return ErrorCode.CODE_NO_SPLITTER;
			}
		
			/** Cannot connect the splitter to the joiner (in either direction)*/
			//TODO: should we be using .equals ????
			if ((startNode == splitter) && (endNode == joiner) ||
				(startNode == joiner) && (endNode == splitter))
			{
					return ErrorCode.CODE_SPLITTER_JOINER_CONNECT_SJ;	
			} 	
			
			/** Cannot connect the joiner to an endNode that is outside the feedbackloop */
			if ((startNode == joiner) && ( ! (floop.hasAncestorInContainer(endNode))))
			{
				return ErrorCode.CODE_NO_ANCESTOR_IN_CONTAINER;
			}
			
			/** Cannot connect from a startNode inside the feedbackloop to the splittter */
			if ((endNode == splitter) && ( ! (floop.hasAncestorInContainer(startNode))))
			{
				return ErrorCode.CODE_ANCESTOR_IN_CONTAINER;
			}
			

			/** Cannot have more than two edges coming out of the splitter of the GEFeedbackLoop */
			if ((startNode == splitter) && (splitter.getSourceEdges().size() == 2))
			{
				return ErrorCode.CODE_SPLITTER_MAX_CONNECTIONS;
			}
			
			/** The joiner cannot have more than two target edges coming into it */
			if ((endNode == joiner) && (joiner.getTargetEdges().size() == 2))
			{
				return ErrorCode.CODE_JOINER_MAX_CONNECTIONS;			
			}
			
			/** Making a connection to something that is not contained by the feedbackloop */
			if (startNode == splitter) 
			{
				if (this.hasAncestorInContainer(endNode))
				{
					this.localGraphStruct.connectDraw(startNode, endNode);
				}
				else
				{
					/** Have to adjust the indices of the nodes connected within the parent
					 * 	that contains them */
					return this.getEncapsulatingNode().connect(startNode, endNode);
				}
			}
			else
			{
				this.localGraphStruct.connectDraw(startNode, endNode);	
			}
			return 0;
		}
		else 
		{
			return validConnection;
		}
	}

	/**
	 * Determine the dimension of the feedbackloop. The height is the sum of the heights of 
	 * the joiner, splitter and the maximum of the body and loop.
	 * The width is the sum of the widths of the body and the loop. 
	 */
	public void calculateDimension()
	{
		Iterator childIter = this.getSuccesors().iterator();
		int height = 0;
		int width  = 0;
		
		
		/** Get the dimension of the body. If the body is null, then 
		 * 	set the body dimension to the default dimension */
		GEStreamNode body = this.getBody();
		Dimension bodyDim = null;
		if (body != null)
		{		
			bodyDim = body.getDimension();
		}
		else
		{
			bodyDim = Constants.DEFAULT_DIMENSION;
		}
		
		/** Get the dimension of the loop. If the loop is null, then 
		 * 	set the loop dimension to the default dimension */
		GEStreamNode loop = this.getLoop();
		Dimension loopDim = null;
		if (loop != null)
		{
			loopDim = loop.getDimension();
		}
		else
		{
			loopDim = Constants.DEFAULT_DIMENSION;
		}
		
		width += bodyDim.width + Constants.X_SEPARATION + loopDim.width;
		height += this.joiner.getDimension().height + this.splitter.getDimension().height + 
				  Math.max(bodyDim.height, loopDim.height) + (Constants.Y_SEPARATION * 2);
		
		/** Set the dimension of the feedbackloop */
		this.setDimension(new Dimension(width, height));
	}
	
	/**
	 * Layout the elements that are contained by the GEFeedbackLoop.
	 * The splitJoin must contain a splitter and a joiner. 
	 *  
	 */
	public void layoutChildren()
	{
		/** Only layout the chidren if the feedbackloop is expanded */
		if (this.isExpanded)
		{
			Point floopLoc = this.getLocation();		
			int yChange = floopLoc.y; 
			int xChange = floopLoc.x;
		
			
			/** Set the location for the joiner */
			joiner.setGraphicalLocation(new Point(LayoutTools.center(this, joiner), yChange), 
										this.localGraphStruct.getJGraph());
			
			yChange += (Constants.Y_SEPARATION + joiner.getDimension().height);
		
			/** Set the location for the body (only if the body is not null). 
			 * If the node is a container,we have to set its location and then do the 
			 * layout of the elements that it contains. */
			
			GEStreamNode body = this.getBody();
			Dimension bodyDim = null;
			if (body != null)
			{
				if (body instanceof GEContainer)
				{
					GEContainer cont = (GEContainer) body;
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
					body.setGraphicalLocation(new Point(xChange, yChange), 
					 	   						this.localGraphStruct.getJGraph());
				}
				bodyDim = body.getDimension();
			}	
			else
			{
				bodyDim = Constants.DEFAULT_DIMENSION;		
			}
			xChange += (Constants.X_SEPARATION + bodyDim.width);
			
			/** Set the location for the loop (only if the loop is not null). 
			 * If the node is a container,we have to set its location and then do the 
			 * layout of the elements that it contains. */
			GEStreamNode loop = this.getLoop();
			Dimension loopDim = null;
			if (loop != null)
			{
				if (loop instanceof GEContainer)
				{
					GEContainer cont = (GEContainer) loop;
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
					loop.setGraphicalLocation(new Point(xChange, yChange), 
												this.localGraphStruct.getJGraph());
				}
				
				loopDim = loop.getDimension();
			}
			/** The loop is null (does not exist) so we are going to use the default dimensions */
			else
			{
				loopDim = Constants.DEFAULT_DIMENSION;			
			}
			
			yChange += Constants.Y_SEPARATION + Math.max(bodyDim.getHeight(), 
														loopDim.getHeight());
			
				
			/** Set the location for the splitter */
			splitter.setGraphicalLocation(new Point(LayoutTools.center(this, splitter), yChange), 
										this.localGraphStruct.getJGraph());
			yChange += (Constants.Y_SEPARATION + splitter.getDimension().height);
		}		
		else
		{
			System.err.println("CANNOT DO LAYOUTCHILDREN BECAUSE FLOOP IS NOT EXPANDED");
		}
	}
	
	/**
	 * Check to see wheter this feedbackloop is valid (must contain a  joiner, splitter, body, loop. 
	 * @return An ErrorCode if there was a problem encountered, otherwise, return code for no error.
	 */
	public int checkValidity()
	{
		/** The feedbackloop must contain a joiner */
		if (this.joiner == null)
		{
			return ErrorCode.CODE_NO_JOINER;
		}
		/** The feedbackloop must contain a splitter */
		else if (this.splitter == null)
		{
			return ErrorCode.CODE_NO_SPLITTER;
		}
	
		/** The feedbackloop must contain a body */
		else if (this.getBody()== null)
		{
			return ErrorCode.CODE_NO_BODY_IN_FLOOP;
		}
	
		/** The feedbackloop must contain a loop */
		else if (this.getLoop() == null)
		{
			return ErrorCode.CODE_NO_LOOP_IN_FLOOP;
		}
		else 
		{
			return ErrorCode.NO_ERROR;
		}
		
	}
	
	public void moveNodePositionInContainer(GEStreamNode startNode, GEStreamNode endNode, int position){};	
}

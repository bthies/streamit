/*
 * Created on Jan 17, 2004
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;

/**
 * The ContainerNodes class has all of the container nodes present in the
 * graph structure. GEContainers might be present at different levels of depth.
 * @author jcarlos
 */
public class ContainerNodes {

	/**
	 * Specifies container objects (i.e. pipelines, splitjoins, feedback loops) at each 
	 * different level of depth. Level of depth increases as the GEStreamNode is contained
	 * by a container object that is also contained by another container object and so on.
	 * As a consequence, the toplevel GEStreamNode has level zero.
	 * The map has the levels as its keys, and the cotnainer objects within that level as the
	 * values of the map.
	 */
	private ArrayList allContainers;
	
	/**
	 * Current level at which the graph is being examined. 
	 */
	private int currentLevelView;
	
	/**
	 * Specifies the current maximum level (depth).
	 */
	//TODO: alternative to maxLevel could be to just determine when there are no containers at a certain
	// level. The maxe level will be the first level encountered where there are no nodes. 
	private int maxlevel;

	/**
	 * The default constructor for ContainerNodes. Initially the ContainerNodes data structure
	 * will not contain any ContainerNodes.
	 */
	public ContainerNodes() 
	{
		allContainers = new ArrayList();
		currentLevelView = 0;
		maxlevel = 0;
	}


	/**
	 * Return all of the container nodes that the GraphStructure contains.
	 * @return ArrayList with the container nodes
	 */
	public ArrayList getAllContainers()
	{	
		return this.allContainers;
	}
	
	/**
	 * Get the GEContainers located at the specified level. 
	 * @param level int The level from which the GEContainers will be obtained. 
	 * @return ArrayList that contains the GEContainers at the specified level.
	 * An empty list will be returned if there are no containers at the specified level. 
	 */
	public ArrayList getContainersAtLevel(int level)
	{
		ArrayList tempList = new ArrayList();
		/** Iterate through all the containers ... */
		for (Iterator containerIter = allContainers.iterator(); containerIter.hasNext(); )
		{
			GEContainer cont = (GEContainer)containerIter.next();
			/** Only add the containers at the specified level to the list that will be returned */
			 if (level == cont.getDepthLevel())
			 {
			 	tempList.add(cont);
			 }
		}
		return tempList;
	}

	/**
	 * Add the container node to the specified level. 
	 * @param level Level at which the Container will be added.
	 * @param node Container node (GEPipeline, GESplitJoin, GEFeedbackLoop)
	 */
	public void addContainerToLevel(int level, GEStreamNode node)
	{
		ArrayList levelList = null;
		/** Add the node to the allContainers collection if it is not null */
		if (node != null)
		{
			this.allContainers.add(node);
			/**	if the level at which the node being added is greater than 
			 * the current maximum level, then update the max level. */
			if (level > maxlevel)
			{
				/** Done to keep track until what level nodes are still present*/
				maxlevel = level;
			}
		}
	}
	
	/**
	 * Remove the GEContainer node from ContainerNodes and return a list with all 
	 * the elements that are contained by the container. 
	 * @param node GEContainer to be removed.
	 * @param list ArrayList that will list all of the elements inside of the GEContainer
	 * that is to be deleted.
	 * @return true if it was possible to remove the GEContainer; otherwise, return false. 
	 */
	
	public boolean removeContainer(GEContainer node, ArrayList list)
	{
		
		/** try to remove the container from the allContainers list */
		if ( ! (this.allContainers.remove(node))) { 
			return false;
		}
		/** iterate through the elements contained by the node */
		Iterator listIter  = ((GEContainer) node).getContainedElements().iterator();
		while (listIter.hasNext())
		{
			/** if the inner elements of the GEContainer to be deleted include 
			 * another container, then we must also remove this inner container. */
			GEStreamNode innerContainer = (GEStreamNode) listIter.next();
			if (innerContainer instanceof GEContainer)
			{	
				if (!(removeContainer((GEContainer)innerContainer, list))) return false;
			}
		}
		
		/** add to list all of the elements contained inside of the GEContainer to be deleted*/
		list.addAll(((GEContainer)node).getContainedElements());
		return true;
	}
	
	/**
	 * Expand all of the container nodes located at the specified level.
	 * @param level The level at which all containers will be expanded.
	 * @return true there were elements at the level to be expanded, false otherwise
	 */		
	public boolean expandContainersAtLevel(int level)
	{
		/** list of containers at the specified level */
		ArrayList levelList = (ArrayList) getContainersAtLevel(level);
		if (levelList != null)
		{
			/** iterate through the containers at the specified level */
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				GEContainer node = (GEContainer) listIter.next();
				/** if the node contains any elements, then expand it */
				if (node.getContainedElements().size() != 0)
				{
					node.expand();
				}
			}
			return true;
		}
		return false;	
	}

	/**
	 * Collapse all of the container nodes located at level.
	 * @param level The level at which all containers will be collapsed.
	 */		
	public void collapseContainersAtLevel(int level)
	{
		/** list of containers at the specified level */
		ArrayList levelList = (ArrayList) getContainersAtLevel(level);
		if (levelList != null)
		{
			/** iterate through the containers at the specified level */
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				/** if the node contains any elements, then collapse it */
				GEContainer node = (GEContainer) listIter.next();
				if (node.getContainedElements().size() != 0 )
				 {
					node.collapse();
				 }
			}
		}		
	}

	/**
	 * Make invisible all of the container nodes located at level. Should only make invisible the nodes
	 * that are expanded (collapsed nodes cannot be made invisible)
	 * @param level The level at which all containers will be made invisible.
	 */	
	public void hideContainersAtLevel(int level)
	{
		/** list of containers at the specified level */
		ArrayList levelList = (ArrayList) getContainersAtLevel(level);
		if (levelList != null)
		{
			/** iterate through the containers at the specified level to hide them */
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				GEContainer node = (GEContainer) listIter.next();
				
				/** Should only hide the containers that are expanded */
				if (node.isExpanded)
				{
				 node.hide();
				}
			}
		}				
	}
	
	/**
	 * Make visible all of the container nodes located at level.
	 * @param level The level at which all containers will be made visible.
	 */
	public void unhideContainersAtLevel(int level)
	{
		/** list of containers at the specified level */
		ArrayList levelList = (ArrayList) getContainersAtLevel(level);
		if (levelList != null)
		{
			/** iterate through the containers at the specified level to make them visible*/
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				GEContainer node = (GEContainer) listIter.next();
				 node.unhide();
			}
		}				
	}

	/**
	 * Hide all the expanded containers in the graph. The collapsed nodes remain visible while
	 * the expanded containers wil be hidden.
	 */
	public void hideAllContainers()
	{
		Iterator contIter = this.allContainers.iterator();
		while (contIter.hasNext())
		{
			GEContainer node = (GEContainer) contIter.next();
			/** if the container is expanded, then hide it */
			if (node.isExpanded())
			{
				node.hide();
			}
		}
	}
	
	/**
	 * Unhide (make visible) all of the expanded containers in the graph.
	 */
	public void unhideAllContainers()
	{
		Iterator contIter = this.allContainers.iterator();
		while (contIter.hasNext())
		{
			GEContainer node = (GEContainer) contIter.next();
			/** if the container is expanded, then unhide it */
			if (node.isExpanded())
			{
				node.unhide();
			}
		}
	}

	/**
	 * Get the names of all of the containers.
	 * @return Object[] containing the name of all of the containers. 
	 */	
	public Object[] getAllContainerNamesWithID()
	{
		Iterator allContIter = this.allContainers.iterator();
		ArrayList names = new ArrayList();
		while(allContIter.hasNext())
		{	
			GEStreamNode node = (GEStreamNode)allContIter.next();
			names.add(node.getNameWithID());
		}
		return names.toArray();
	}

	/**
	 * Get the container object corresponding to the specified name with ID.
	 * @param name String name with ID (The name is separated from the ID by the Constants.ID_TAG)
	 * @return the GEContainer corresponding to the name if it is found; otherwise, return null
	 * Return null if the name specified is "TopLevel"
	 */
		
	public GEContainer getContainerNodeFromNameWithID(String name)
	{
		/** Determine the ID part of the name passed as an argument.
		 * 	Constants.ID_TAG is the tag that comes in the string before the ID part
		 * 	of the name with ID */
		int id = Integer.parseInt(name.substring(name.lastIndexOf(Constants.ID_TAG) + Constants.ID_TAG.length(), 
																  name.length()));
		
		/** If the specified name is toplevel, then return null.
		 * 	Null is returned because the toplevel node does not have a parent*/
		if (name == Constants.TOPLEVEL)
		{
			return null;
		}
	
		Iterator aIter = this.allContainers.iterator();
		GEContainer node = null;
		/** iterate through the containers until the one with the matching ID is found */
		while(aIter.hasNext())
		{
			node = (GEContainer) aIter.next();
			if (id == node.hashCode()) 
			{
				return node;
			}
		}
		/** if the name is not found, then return null */
		return null;
	}
	
	
	
	/**
	 * Get the GEContainers located at the specified level. 
	 * @param level int The level from which the GEContainers will be obtained. 
	 * @return ArrayList that contains the GEContainers at the specified level.
	 * An empty list will be returned if there are no containers at the specified level. 
	 */
	private ArrayList getContainersAtLevelLowerThan(int level)
	{
		ArrayList tempList = new ArrayList();
		/** Iterate through all the containers ... */
		for (Iterator containerIter = allContainers.iterator(); containerIter.hasNext(); )
		{
			GEContainer cont = (GEContainer)containerIter.next();
			/** Only add the containers at the specified level to the list that will be returned */
			 if (level >= cont.getDepthLevel())
			 {
				tempList.add(cont);
			 }
		}
		return tempList;
	}
	
	
	/**
	 * Sets the location of the container nodes present at the current level and at 
	 * every level lower tham this. The bounds of the container
	 * node are set in such a way that the elements that it contains are enclosed.
	 * Also, changes the location of the label so that it is more easily viewable.
	 * @param level The level of the containers whose location will be set. 
	 */
	//TODO: Might be able to optimize this method.
	public void setLocationOfContainersAtLevel(GraphStructure graphStruct)
	{	
		/** Iterate through all the levels lower than this. Must begin with highest level 
		 *  in order to ensure that the changes done at each level are captured by the next
		 *  lower level */
		for (int i = this.currentLevelView; i >= 0; i--)
		{
			/** get the list of the nodes at the present level */
			ArrayList levelList = this.getContainersAtLevel(i);
			
			if (levelList != null)
			{
				Iterator listIter = levelList.iterator();
				while(listIter.hasNext())
				{
					GEContainer node = (GEContainer) listIter.next();
					
if (node.isExpanded)
{					

					System.out.println("node is "+ node);
					
					/** Set the current container to visible */
					graphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{node}, true);
						
					/** Get the contained elements of the container */
					Object[] containedCells = node.getContainedElements().toArray();
					CellView[] containedCellViews = graphStruct.getJGraph().getGraphLayoutCache().getMapping(containedCells);
	
					/** Get the bounding rectangle for how the container encloses the elements
					 * that it contains. */
					Rectangle cellBounds = AbstractCellView.getBounds(containedCellViews);
					
					/** Get the attributes of the node */
					Map attribs = ((GEStreamNode) node).getAttributes();
					
					/** display container info at the top, do not allow autosize */
					GraphConstants.setVerticalAlignment(attribs, 1);
				//	GraphConstants.setAutoSize(attribs, false);
					
					/** modify the cellBounds of the container so that it provides more space
					 * for the innner elements */
					if (cellBounds != null)
					{
						cellBounds.x -= (Constants.X_MARGIN_OF_CONTAINER / 2);
						cellBounds.y -= (Constants.Y_MARGIN_OF_CONTAINER / 2);
						cellBounds.width += Constants.X_MARGIN_OF_CONTAINER;
						cellBounds.height += Constants.Y_MARGIN_OF_CONTAINER;
						
						GraphConstants.setBounds(attribs, cellBounds);
					}
					
					/**Edit the graph model according to the attributes that have changed */
					graphStruct.getGraphModel().edit(graphStruct.getAttributes(), null , null, null);
				}
				}
			}
		}
	}



	/**
	 * Get the current level of expansion/collapsing of the GraphStructure.
	 * @return currentLevelView int
	 */
	public int getCurrentLevelView()
	{
		return this.currentLevelView;
	}
	
	/**
	 * Sets the level at which the Graph Structure is to be expanded/collapsed. 
	 * @param levelView 
	 */	
	public void setCurrentLevelView(int levelView)
	{/*
		if (levelView < 0)
		{
			this.currentLevelView = 0;
		}
		else if (levelView > this.maxlevel)
		{
			this.currentLevelView = maxlevel;
		}
		else
		{*/
			this.currentLevelView = levelView;	
		//}
	}

	/**
 	 * Get the maximum level at which nodes are present.
 	 * @return maxlevel int
 	*/
	public int getMaxLevelView()
	{
		return this.maxlevel;
	
	}
	

}

/*
 * Created on Jan 17, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
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
	private HashMap levelContainers;
	private ArrayList allContainers;
	
	/**
	 * Current level at which the graph is being examined. 
	 */
	private int currentLevelView;
	private int maxlevel;
	

	/**
	 * 
	 */
	public ContainerNodes() 
	{
		allContainers = new ArrayList();
		levelContainers = new HashMap();
		currentLevelView = 0;
		maxlevel = 0;
	}


	public ArrayList getContainersAtLevel(int level)
	{
		return (ArrayList) this.levelContainers.get(new Integer(level));
	}

	/**
	 * Add the container node at level. 
	 * @param level Level at which the Container will be added.
	 * @param node Container node (GEPipeline, GESplitJoin, GEFeedbackLoop)
	 */
	public void addContainerToLevel(int level, GEStreamNode node)
	{
		ArrayList levelList = null;
		if(this.levelContainers.get(new Integer(level)) == null)
		{
			levelList = new ArrayList();
			levelList.add(node);
			this.levelContainers.put(new Integer(level), levelList);
		}
		else
		{
			levelList = (ArrayList) this.levelContainers.get(new Integer(level));
			levelList.add(node);
		}
		this.allContainers.add(node);
		if (level > maxlevel)
		{
			maxlevel = level;
		}
		
	}
	
	/**
	 * Expand all of the container nodes located at level.
	 * @param level The level at which all containers will be expanded.
	 * @return True there were elements at the level to be expanded, false otherwise
	 */		
	public boolean expandContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				 GEContainer node = (GEContainer) listIter.next();
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
		System.out.println ("Level to collapse is " + level);
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				GEContainer node = (GEContainer) listIter.next();
				if (node.getContainedElements().size() != 0 )
				 {
					node.collapse();
				 }
			}
		}		
	}

	public boolean removeContainer(GEContainer node, ArrayList list)
	{
		if (node instanceof GEContainer)
		{
			if (!(this.allContainers.remove(node))) return false;
			ArrayList contAtLevel = this.getContainersAtLevel(node.getDepthLevel());
			if (contAtLevel == null) return false;
			if (!(contAtLevel.remove(node))) return false;
			
			Iterator listIter  = ((GEContainer) node).getContainedElements().iterator();
			while (listIter.hasNext())
			{
				GEStreamNode innerContainer = (GEStreamNode) listIter.next();
				if (innerContainer instanceof GEContainer)
				{
					if (!(removeContainer((GEContainer)innerContainer, list))) return false;
				}
			}
			
			list.addAll(((GEContainer)node).getContainedElements());
			return true;
		}
		return false;
	}

	/**
	 * Make invisible all of the container nodes located at level.
	 * @param level The level at which all containers will be made invisible.
	 */	
	public void hideContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				GEContainer node = (GEContainer) listIter.next();
				 node.hide();
			}
		}				
	}
	
	/**
	 * Make visible all of the container nodes located at level.
	 * @param level The level at which all containers will be made visible.
	 */
	public void unhideContainersAtLevel(int level)
	{
		ArrayList levelList = (ArrayList) this.levelContainers.get(new Integer(level));
		if (levelList != null)
		{
			Iterator listIter = levelList.iterator();
			while(listIter.hasNext())
			{
				GEContainer node = (GEContainer) listIter.next();
				 node.unhide();
			}
		}				
	}
	
	/**
	 * Hide all the expanded containers in the graph (the collapsed nodes remain
	 * visible).
	 */
	public void hideAllContainers()
	{
		Iterator contIter = this.allContainers.iterator();
		while (contIter.hasNext())
		{
			GEContainer node = (GEContainer) contIter.next();
			if (node.isExpanded())
			{
				node.hide();
			}
		}
	}
	
	/**
	 * Unhide all of the expanded containers in the graph.
	 */
	public void unhideAllContainers()
	{
		Iterator contIter = this.allContainers.iterator();
		while (contIter.hasNext())
		{
			GEContainer node = (GEContainer) contIter.next();
			if (node.isExpanded())
			{
				node.unhide();
			}
		}
	}

	/**
	 * Return all of the container nodes (GEPipeline, GESplitJoin, GEFeedbackLoop) 
	 * that the GraphStructure contains.
	 * @return Object array with the container nodes
	 */
	public ArrayList getAllContainers()
	{	
		//return this.levelContainers.values().toArray();
		return this.allContainers;
	}
	
	public Object[] getAllContainerNames()
	{
		Iterator allContIter = this.allContainers.iterator();
		ArrayList names = new ArrayList();
		while(allContIter.hasNext())
		{	
			names.add(((GEStreamNode)allContIter.next()).name);
		}
		return names.toArray();
	}
	
	public GEContainer getContainerNodeFromName(String name)
	{	
		if (name == "Toplevel")
		{
			System.out.println("Returning toplevel from getContainerNodeFromName");
	///		return this.topLevel;
	return null;
		}
		
		
		Iterator aIter = this.allContainers.iterator();
		GEContainer node = null;
		while(aIter.hasNext())
		{
			node = (GEContainer) aIter.next();
			if (name == node.getName())
			{
				System.out.println("Returning node from getContainerNodeFromName");
				return node;
			}
		}
		
		System.out.println("Returning null from getContainerNodeFromName");
		return null;
		
	}
	
	
	/**
	 * Sets the location of the Container nodes at level. The bounds of the container
	 * node are set in such a way that the elements that it contains are enclosed.
	 * Also, changes the location of the label so that it is more easily viewable.
	 * @param level The level of the containers whose location will be set. 
	 */
	
	public void setLocationContainersAtLevel(GraphStructure graphStruct)
	{
		
		for (int i = this.currentLevelView; i >= 0; i--)
		{
			ArrayList levelList = this.getContainersAtLevel(i);
			
			if (levelList != null)
			{
				Iterator listIter = levelList.iterator();
				while(listIter.hasNext())
				{
					GEContainer node = (GEContainer) listIter.next();
					System.out.println("node is "+ node);
					graphStruct.getJGraph().getGraphLayoutCache().setVisible(new Object[]{node}, true);
					Object[] containedCells = node.getContainedElements().toArray();
			
					CellView[] containedCellViews = graphStruct.getJGraph().getGraphLayoutCache().getMapping(containedCells);
	
					Rectangle cellBounds = AbstractCellView.getBounds(containedCellViews);
					
					
					Map attribs = ((GEStreamNode) node).getAttributes();
					GraphConstants.setVerticalAlignment(attribs, 1);
					GraphConstants.setAutoSize(attribs, false);
					if (cellBounds != null)
					{
						cellBounds.height += 60;
						cellBounds.width += 60;
						GraphConstants.setBounds(attribs, cellBounds);
					}
					
					// The lines below are supposed to change label location, but they don't
					//GraphConstants.setValue(node.getAttributes(), "hello");
					//GraphConstants.setHorizontalAlignment(node.getAttributes(), 1);
					
					graphStruct.getGraphModel().edit(graphStruct.getAttributes(), null , null, null);
				}
			}
		}
	}


	/**
	 * Get the current level of expansion/collapsing of the GraphStructure.
	 * @return currentLevelView
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
 	 * @return maxlevel
 	*/
	public int getMaxLevelView()
	{
		return this.maxlevel;
	}
	
	/**
	 * Set the maximum level view at which there are nodes present.
	 * @param maxLevel
	 */
	public void setMaxLevelView(int maxLevel)
	{
		this.maxlevel = maxLevel;
	}
}

/*
 * Created on Jan 17, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package streamit.eclipse.grapheditor.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
				 GEStreamNode node = (GEStreamNode) listIter.next();
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
				 GEStreamNode node = (GEStreamNode) listIter.next();
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
	
	public GEStreamNode getContainerNodeFromName(String name)
	{	
		if (name == "Toplevel")
		{
			System.out.println("Returning toplevel from getContainerNodeFromName");
	///		return this.topLevel;
	return null;
		}
		
		
		Iterator aIter = this.allContainers.iterator();
		GEStreamNode node = null;
		while(aIter.hasNext())
		{
			node = (GEStreamNode) aIter.next();
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

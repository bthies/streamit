/*
 * Created on Jan 17, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package streamit.eclipse.grapheditor.graph;

import java.util.ArrayList;


/**
 * @author jcarlos
 *
 * The interface that must be implemented by the container nodes.
 */
public interface GEContainer {

	public void calculateDimension();
	public void layoutChildren();
	
	
	
	/** Returns a list of nodes that are contained by this GEStreamNode. If this GEStreamNode is
	 * not a container node (can't have any contained elements), then null is returned.
	 * @return ArrayList of contained elements. If <this> is not a container, return null.
	 */
	public ArrayList getContainedElements();
	
	
	/**
	 * Expand or collapse the GEStreamNode structure depending on wheter it was already 
	 * collapsed or expanded. 
	 * @param jgraph The JGraph that will be modified to allow the expanding/collapsing.
	 */	
	public void collapseExpand();
	public void collapse();
	public void expand();

}

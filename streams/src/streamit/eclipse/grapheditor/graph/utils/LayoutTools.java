/*
 * Created on Mar 25, 2004
 */
package streamit.eclipse.grapheditor.graph.utils;

import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;

/**
 * Class that provides useful layout tools.
 * 
 * @author jcarlos
 */
public class LayoutTools 
{

	/**
	 * Get the x-coordinate required to center the node relative to 
	 * its parent (encapsulating node).
	 * @param cont GEContainer
	 * @param node GEStreamNode
	 * @return x-coordinate that centers the node relative to its parent.
	 */
	public static int center(GEContainer cont, GEStreamNode node)
	{
		
		int cx = cont.getDimension().width / 2;
		return cx + cont.getLocation().x - (node.getDimension().width / 2);
	}


}

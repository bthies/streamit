/*
 * 
 * Created on Jul 17, 2003
 */
package streamit.eclipse.grapheditor.graph;

import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;

/// NOT USED /////

/**
 * Listener for when there is a change in the GraphModel.
 * @author jcarlos
 */
public class JGraphModelListener implements GraphModelListener {

	public void graphChanged(GraphModelEvent e) 
	{
		System.out.println("Change : " + e.getChange());	
	}

}

/*
 * Created on Jul 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package streamit.eclipse.grapheditor.graph;

import org.jgraph.event.GraphModelEvent;
import org.jgraph.event.GraphModelListener;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class JGraphModelListener implements GraphModelListener {

	/* (non-Javadoc)
	 * @see com.jgraph.event.GraphModelListener#graphChanged(com.jgraph.event.GraphModelEvent)
	 */
	public void graphChanged(GraphModelEvent e) 
	{
		System.out.println("Change : " + e.getChange());	
	}

}

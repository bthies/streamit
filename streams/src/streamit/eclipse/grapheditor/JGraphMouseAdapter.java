/*
 * Created on Jul 17, 2003
 *
 */
package grapheditor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.jgraph.JGraph;
import com.jgraph.graph.GraphConstants;
import java.util.Map;
import java.util.Hashtable;

import grapheditor.jgraphextension.*;

/**
 * MouseAdapter for a given JGraph 
 * @author jcarlos
 */
public class JGraphMouseAdapter extends MouseAdapter {
	
	private JGraph jgraph;

	public JGraphMouseAdapter(JGraph jgraph)
	{
		this.jgraph = jgraph;
	}
   
	public void mousePressed(MouseEvent e)
	{
		if (e.getSource() instanceof LiveJGraphInternalFrame)
		{
			System.out.println("The source of the mouse event  is FRAME");
		}
		// click left mouse button once to modify the corresponsing source code 
		if ((e.getClickCount() == 1) && (e.getModifiers() == MouseEvent.BUTTON1_MASK))
		{
			System.out.println(" CALLING METHOD TO GO TO CORRESPONDING CODE IN .STR FILE");		
		}
	
		// double click right mouse button to display details or just display the name.
		else if ((e.getClickCount() == 2) && (e.getModifiers() == MouseEvent.BUTTON3_MASK))
		{
			int x = e.getX(), y = e.getY();
			
			if (jgraph.getFirstCellForLocation(x,y) instanceof GEStreamNode)
			{
				GEStreamNode node = (GEStreamNode) jgraph.getFirstCellForLocation(x,y);
				
				if (node != null)
				{
					node.collapseExpand(this.jgraph);
					/*
					if (node.isInfoDisplayed)
					{
						Map change = GraphConstants.createMap();
						GraphConstants.setValue(change, "<HTML><H5>"+node.getName()+"</H5></HTML>");
						Map nest = new Hashtable ();
						nest.put(node, change);
						jgraph.getModel().edit(nest, null, null, null);
						
						node.isInfoDisplayed = false;
					}
					else
					{
						Map change = GraphConstants.createMap();
						GraphConstants.setValue(change, node.getInfo());
						Map nest = new Hashtable ();
						nest.put(node, change);
						jgraph.getModel().edit(nest, null, null, null);
																	
						node.isInfoDisplayed = true;
					}
					System.out.println("The user object is " +node.getUserObject().toString());
					System.out.println(jgraph.convertValueToString(node)); 
					*/
				}
				
				
				
				
			}
		}	
	}
}

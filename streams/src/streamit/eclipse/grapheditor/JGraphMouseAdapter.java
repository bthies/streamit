/*
 * Created on Jul 17, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package grapheditor;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.jgraph.JGraph;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class JGraphMouseAdapter extends MouseAdapter {
	
	private JGraph jgraph;

	public JGraphMouseAdapter(JGraph jgraph)
	{
		this.jgraph = jgraph;
	}



	public void mousePressed(MouseEvent e)
	{
		if (e.getClickCount() == 1)
		{
			int x = e.getX(), y = e.getY();
			Object cell = jgraph.getFirstCellForLocation(x,y);
			if (cell != null)
			{
				System.out.println(jgraph.convertValueToString(cell)); 
			}
		}
	}




}

/*
 * Created on Jul 17, 2003
 *
 */
package streamit.eclipse.grapheditor.graph;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.jgraph.JGraph;


/**
 * MouseAdapter for a given JGraph 
 * @author jcarlos
 */
public class JGraphMouseAdapter extends MouseAdapter {
	
	private JGraph jgraph;
	private GraphStructure graphStruct;

	public JGraphMouseAdapter(JGraph jgraph, GraphStructure graphStruct)
	{
		this.jgraph = jgraph;
		this.graphStruct = graphStruct;
		
	}
   
	public void mousePressed(MouseEvent e)
	{
		int x = e.getX(), y = e.getY();
	
		// click left mouse button once to modify the corresponsing source code 
		if ((e.getClickCount() == 1) && (e.getModifiers() == MouseEvent.BUTTON1_MASK))
		{
			
			if (jgraph.getFirstCellForLocation(x,y) instanceof GEStreamNode)
			{
				GEStreamNode node = (GEStreamNode) jgraph.getFirstCellForLocation(x,y);
				
				ArrayList highlightList = new ArrayList();
				highlightList.add(node);
			//	graphStruct.highlightNodes(highlightList);
				
//				System.out.println(" CALLING METHOD TO GO TO CORRESPONDING CODE IN .STR FILE FOR NODE "+node.getName());
				IFile fili = NodeCreator.getIFile(node);
				if (fili != null)
				{
//					System.out.println ("THE IFILE CORRESPONDING TO " + node.getNameNoID()+ "  IS " + fili.toString());
									
					try 
					{
						String message = new String(fili.getFullPath().toString() +" "+ node.getNameNoID() + "\n");
						ByteArrayInputStream bais = new ByteArrayInputStream(message.getBytes());
						LogFile.getIFile().appendContents(bais, false, false, null);
			
						LogFile.getIFile().getParent().refreshLocal(IResource.DEPTH_ONE, null);
					} 
					catch (Exception exception) 
					{
						exception.printStackTrace();
					}
					
				}
				else
				{
					System.out.println ("THE IFILE RETURNED IS NULL");
				}
			}		
		}
	
	}

	public void mouseReleased(MouseEvent e)
	{
		/* demoremove
		int x = e.getX(), y = e.getY();

		// click left mouse button once to modify the corresponsing source code 
		if ((e.getClickCount() == 1) && (e.getModifiers() == MouseEvent.BUTTON1_MASK))
		{
	
			if (jgraph.getFirstCellForLocation(x,y) instanceof GEStreamNode)
			{
				GEStreamNode node = (GEStreamNode) jgraph.getFirstCellForLocation(x,y);
		
				ArrayList highlightList = new ArrayList();
				highlightList.add(node);
				graphStruct.highlightNodes(highlightList);
			}
		}*/
	}


}

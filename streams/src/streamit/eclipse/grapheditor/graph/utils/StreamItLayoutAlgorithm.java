/*
 * Created on Jan 17, 2004
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package streamit.eclipse.grapheditor.graph.utils;

import java.util.Iterator;

import org.jgraph.layout.SugiyamaLayoutAlgorithm;
import org.jgraph.layout.SugiyamaLayoutController;

import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class StreamItLayoutAlgorithm {


	public void perform(GraphStructure graphStruct)
	{
		
		for (int i = graphStruct.containerNodes.maxlevel; i >=0; i--)
		{
			Iterator containerIterator = graphStruct.containerNodes.getContainersAtLevel(i).iterator();
			while (containerIterator.hasNext())
			{
				GEContainer node = (GEContainer) containerIterator.next();
			//	node.calculateDimension();
				
			}
		}
		
		
		SugiyamaLayoutAlgorithm algorithm = new SugiyamaLayoutAlgorithm();
		SugiyamaLayoutController controller = new SugiyamaLayoutController();
		algorithm.perform(graphStruct.getJGraph(), true, controller.getConfiguration());
		
	/*	
		for (int i = 0; i <= graphStruct.containerNodes.maxlevel; i++)
		{
			Iterator containerIterator = graphStruct.containerNodes.getContainersAtLevel(i).iterator();
			while (containerIterator.hasNext())
			{
				GEContainer node = (GEContainer) containerIterator.next();
				node.layoutChildren();
				
			}
			
		}
	*/	
		
		
		
	}
	
		
		
		
		


}

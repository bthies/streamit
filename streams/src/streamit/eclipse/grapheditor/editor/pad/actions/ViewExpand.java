/*
 * Created on Dec 5, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GEPhasedFilter;
import streamit.eclipse.grapheditor.graph.GESplitJoin;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * @author jcarlos
 *
 */
public class ViewExpand extends AbstractActionDefault {

	/**
	 * Constructor for ViewScaleZoomOut.
	 * @param graphpad
	 * @param name
	 */
	public ViewExpand(GPGraphpad graphpad) {
		super(graphpad);
	}


	public void actionPerformed(ActionEvent e) 
	{
		
		graphpad.getCurrentDocument().setResizeAction(null);
					
		GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
		int currentLevelView = graphStruct.getCurrentLevelView();
		
		graphStruct.expandContainersAtLevel(currentLevelView);
		graphStruct.setCurrentLevelView(++currentLevelView);
		
		graphpad.getCurrentDocument().setScale(graphpad.getCurrentGraph().getScale() / 1.2);
		if (graphpad.getCurrentGraph().getSelectionCell() != null)
		{
			graphpad.getCurrentGraph().scrollCellToVisible(getCurrentGraph().getSelectionCell());
		}
		centerLayout();
	}	

	public void oldcenterLayout()
	{
		// The cells that are going to be centered are only the ones that are included inside of the 
		// toplevel node. The Sugiyama layout algorithm places the nodes that are not connected to 
		// the graph outside of the toplevel. 
		Map attributes = graphpad.getCurrentDocument().getGraphStructure().getTopLevel().getAttributes();
	
	
			Object[] cells = graphpad.getCurrentDocument().getGraph().getRoots(GraphConstants.getBounds(attributes));
		//Object[] cells = graphpad.getCurrentDocument().getGraphStructure().getJGraph().getRoots(GraphConstants.getBounds(attributes));
		
		GraphLayoutCache gv = getCurrentGraphLayoutCache();
		if (cells != null) {
			Rectangle r = getCurrentGraph().getCellBounds(cells);
			int cx = r.width / 2;
			Map viewMap = new Hashtable();
			
			for (int i = 0; i < cells.length; i++) 
			{
				int yOffset = 0;
				if ((cells[i] instanceof GEPhasedFilter ) || 
					(cells[i] instanceof GESplitter) ||
					(cells[i] instanceof GEJoiner))
					{
						yOffset = 40;
					}
				GEStreamNode strNode = ((GEStreamNode)cells[i]).getEncapsulatingNode();
			
				
				CellView view = gv.getMapping(cells[i], false);
				Map map = GraphConstants.cloneMap(view.getAllAttributes());
				Rectangle bounds = GraphConstants.getBounds(map);
				if (bounds != null) 
				{
					bounds.setLocation(r.x + cx - bounds.width / 2,
									   bounds.y + yOffset);
					viewMap.put(cells[i], map);
				}
				
			}
			gv.edit(viewMap, null, null, null);
		}
		
		getCurrentGraph().clearSelection();
	}
	
	
	
	public void centerLayout()
		{
			// The cells that are going to be centered are only the ones that are included inside of the 
			// toplevel node. The Sugiyama layout algorithm places the nodes that are not connected to 
			// the graph outside of the toplevel. 
			Map attributes = graphpad.getCurrentDocument().getGraphStructure().getTopLevel().getAttributes();
	
			boolean doCenterLayout = true;
			Object[] cells = graphpad.getCurrentDocument().getGraphStructure().allNodesInGraph().toArray();
			Object[] Acells = graphpad.getCurrentDocument().getGraphStructure().getJGraph().getRoots(GraphConstants.getBounds(attributes));
		
			GraphLayoutCache gv = getCurrentGraphLayoutCache();
			if (cells != null) {
				Rectangle r = getCurrentGraph().getCellBounds(Acells);
				int cx = r.width / 2;
				Map viewMap = new Hashtable();
			
				for (int i = 0; i < cells.length; i++) 
				{
					int yOffset = 0;
					GEStreamNode strNode = (GEStreamNode) cells[i];
					System.out.println(strNode.getName());
					
					if ((strNode instanceof GEPhasedFilter ) || 
						(strNode instanceof GESplitter) ||
						(strNode instanceof GEJoiner))
						{
							yOffset = 40;
						}
					/*** If the parent of the node is a splitjoin and the node
					 * is a filter
					 */
					GEStreamNode parentNode = strNode.getEncapsulatingNode();
					if(parentNode != null)
					{
						if ((parentNode.getType() == GEType.SPLIT_JOIN) &&
							(strNode.getType() == GEType.PHASED_FILTER))
						
						{
							doCenterLayout = false;	
						}						
					}
					
					if (doCenterLayout)
					{
						CellView view = gv.getMapping(strNode, false);
						Map map = GraphConstants.cloneMap(view.getAllAttributes());
						Rectangle bounds = GraphConstants.getBounds(map);
						if (bounds != null) 
						{
							bounds.setLocation(r.x + cx - bounds.width / 2,
											   bounds.y + yOffset);
							viewMap.put(strNode, map);
						}
						doCenterLayout = true;
					}
				}
				gv.edit(viewMap, null, null, null);
				
				
		//		graphpad.getCurrentDocument().getGraphStructure().setLocationContainersAtLevel(2);
				/*
				Iterator containerIter = graphpad.getCurrentDocument().getGraphStructure().getAllContainers().iterator();
				while(containerIter.hasNext())
				{
					GEStreamNode node = (GEStreamNode) containerIter.next();
					if (node.getType() == GEType.SPLIT_JOIN)
					{
						GESplitJoin splitjoin = (GESplitJoin) node;
						Rectangle sjRect = GraphConstants.getBounds(splitjoin.getAttributes());
						ArrayList children = splitjoin.getSuccesors();
						int numberOfChildren = children.size();
						System.out.println("the bounds are " + sjRect);
						
						
						
						
						
						for (int i = 0; i < numberOfChildren; i++)
						{
							GEStreamNode streamNode = (GEStreamNode)children.get(i);
							if (i == 0)
							{
								GraphConstants.setBounds(streamNode.getAttributes(),
																			new Rectangle(sjRect.x +50, sjRect.y+350, 200,120));
							} 
							else
							{
							
								GraphConstants.setBounds(streamNode.getAttributes(),
														new Rectangle(sjRect.x +250, sjRect.y+350, 200,120));
							}
							Map nest = new Hashtable ();
							nest.put(streamNode, streamNode.getAttributes());
							graphpad.getCurrentDocument().getGraph().getModel().edit(nest, null, null, null);
							
							
						}
						
								
					}
					
				}
				*/
				
				
				
			}
		
			getCurrentGraph().clearSelection();
		}
}

/*
 * Created on Mar 6, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JFrame;

import org.jgraph.plaf.basic.BasicGraphUI.RootHandle;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.controllers.GESplitJoinDuplicationDialog;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEPhasedFilter;
import streamit.eclipse.grapheditor.graph.GEPipeline;
import streamit.eclipse.grapheditor.graph.GESplitJoin;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that duplicates an inner child of the SplitJoin. An inner child
 * of the splitjoin is connected to the splitter and the joiner in the GESplitJoin.
 *  
 * 
 * @author jcarlos
 */
public class EditDuplicateInSplitJoin extends AbstractActionDefault {

	/**
	 * Constructor for EditDuplicateInSplitJoin.
	 * @param graphpad
	 */
	public EditDuplicateInSplitJoin(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Duplicate an inner child of a splitjoin.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		GEStreamNode cell = (GEStreamNode) graphpad.getCurrentDocument().getGraph().getSelectionCell();
		GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
		Rectangle defaultBounds =  new Rectangle(new Point(10,10));
		
		
		if (cell != null)
		{
			/** Get the most immediate SplitJoin that has the selected node as its ancestor */
			GEContainer container = cell.getEncapsulatingNode();
			while (container != null)
			{
				if (container.getType() == GEType.SPLIT_JOIN)
				{
					GESplitJoin splitjoin = (GESplitJoin) container;
					System.out.println(" The Container is " + container.toString());
					GESplitJoinDuplicationDialog dialog =  new GESplitJoinDuplicationDialog(new JFrame());
							
					dialog.setVisible(true);
					if (dialog.canceled()) return;
					int duplicationNumber = dialog.getInputValue();
					
					/** List to hold all the cloned elements in order to save computation time by constructing all the elements
					 *  in a single call to the node constructor */
					ArrayList cloneList = new ArrayList();
			
					
					for (int i= 0; i < duplicationNumber; i++)
					{
						GEStreamNode clonedNode = (GEStreamNode)cell.clone();
						cloneList.add(clonedNode);
						
						/*
						if (clonedNode instanceof GEPhasedFilter)
						{
							graphpad.getCurrentDocument().getGraphStructure().connect(splitjoin.getSplitter(), clonedNode);
							graphpad.getCurrentDocument().getGraphStructure().connect(clonedNode, splitjoin.getJoiner());
						}*/
											/*
											NodeCreator.construct(clonedNode, clonedNode.getEncapsulatingNode(), graphStruct, defaultBounds);
											if (clonedNode instanceof GEContainer)
											{
												
												for (Iterator containerIter = ((GEContainer) clonedNode).getContainedElements(); containerIter.hasNext();)
												{
													/// try an optimized NodeCreator constructor
												}
												
											}
											*/
				
					}
					graphStruct.constructNodes(cloneList, splitjoin, defaultBounds);
					
					for (Iterator cloneIter = cloneList.iterator(); cloneIter.hasNext();)
					{
						GEStreamNode clone = (GEStreamNode) cloneIter.next();
						if (clone instanceof GEPhasedFilter)
						{
							graphStruct.connectDraw(splitjoin.getSplitter(), clone);
							graphStruct.connectDraw(clone, splitjoin.getJoiner());
						}
						else if (clone instanceof GEPipeline)
						{
							GEContainer cont = (GEContainer) clone;
							graphStruct.connectDraw(splitjoin.getSplitter(), cont.getFirstNonContainerNodeInContainer());
							graphStruct.connectDraw(cont.getLastNonContainerNodeInContainer(), splitjoin.getJoiner());
							ArrayList containedList= cont.getContainedElements();
							graphStruct.constructNodes(containedList, cont, new Rectangle(new Point(10,10)));
							GEStreamNode lastNode = null;
							for (int i=0; i < containedList.size(); i++)
							{
								GEStreamNode currNode = (GEStreamNode)containedList.get(i);
								if (lastNode != null)
								{
									graphStruct.connectDraw(lastNode, currNode);
								}
								lastNode = currNode;
							}		
							cont.setDisplay(graphStruct.getJGraph());												
						}				
					}
					
					/** Layout the graph */
					GraphDoLayout gdl = (GraphDoLayout) graphpad.getCurrentActionMap().
											get(Utilities.getClassNameWithoutPackage(GraphDoLayout.class));
					gdl.actionPerformed(null);
					
					/** Update the hierarchy panel */
					EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
						get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
					ac.actionPerformed(null);
					break;
				}
				container = container.getEncapsulatingNode();
			}
		}
	}
	
}



//
//
//
//
//
//public void actionPerformed2(ActionEvent e) 
//{
//	GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
//	GEStreamNode cell = (GEStreamNode) graphpad.getCurrentDocument().getGraph().getSelectionCell();
//	RootHandle roothandle = (RootHandle)graphpad.getCurrentDocument().getGraphUI().getHandle(graphpad.getCurrentGraph());
//		
//		
//	if (cell != null)
//	{
//		/** Get the most immediate SplitJoin that has the selected node as its ancestor */
//		GEContainer container = cell.getEncapsulatingNode();
//		if (container.getType() == GEType.SPLIT_JOIN)
//		{
//			GESplitJoin splitjoin = (GESplitJoin) container;
//			System.out.println(" The Container is " + container.toString());
//			GESplitJoinDuplicationDialog dialog =  new GESplitJoinDuplicationDialog(new JFrame());
//				
//			dialog.setVisible(true);
//			if (dialog.canceled()) return;
//			int duplicationNumber = dialog.getInputValue();
//			Map cloneMap = new HashMap();
//				
//			for (int i= 0; i < duplicationNumber; i++)
//			{
//				cloneMap = roothandle.cloneCells(new Object[] {cell});
//					
//				for (Iterator cloneIter = cloneMap.values().iterator(); cloneIter.hasNext();)
//				{
//					Object cloneObj = cloneIter.next();
//					if (cloneObj instanceof GEStreamNode)
//					{
//						GEStreamNode clone = (GEStreamNode)cloneObj;
//						
//						clone.setNodeProperties(clone.getNodeProperties(), graphStruct.getJGraph(), graphStruct.containerNodes);
//						if (clone instanceof GEPhasedFilter)
//						{
//						//	clone.setPort((DefaultPort)clone.getChildAt(0));
//							graphStruct.connect(splitjoin.getSplitter(), clone);
//							graphStruct.connect(clone, splitjoin.getJoiner());
//						}
//						else if (clone instanceof GEPipeline)
//						{
//							graphStruct.connect(splitjoin.getSplitter(), ((GEContainer)clone).getFirstNonContainerNodeInContainer());
//							graphStruct.connect(((GEContainer)clone).getLastNonContainerNodeInContainer(), splitjoin.getJoiner());							
//						}							
//					}
//						
//				}
//					
//	//			cell.setPort(new DefaultPort());
//			}
//
//				
//			/*
//			for (Iterator cloneIter = cloneMap.values().iterator(); cloneIter.hasNext();)
//			{
//				Object cloneObj = cloneIter.next();
//				if (cloneObj instanceof GEStreamNode)
//				{
//						
//					GEStreamNode clone = (GEStreamNode)cloneObj;
//						
//					GEProperties.setNodeProperties(clone, GEProperties.getNodeProperties(clone), graphStruct);
//					if (clone instanceof GEPhasedFilter)
//					{
//						graphpad.getCurrentDocument().getGraphStructure().connect(splitjoin.getSplitter(), clone);
//						graphpad.getCurrentDocument().getGraphStructure().connect(clone, splitjoin.getJoiner());
//					}
//					else if (clone instanceof GEPipeline)
//					{
//						graphpad.getCurrentDocument().getGraphStructure().connect(splitjoin.getSplitter(), ((GEContainer)clone).getFirstNonContainerNodeInContainer());
//						graphpad.getCurrentDocument().getGraphStructure().connect(((GEContainer)clone).getLastNonContainerNodeInContainer(), splitjoin.getJoiner());							
//					}
//				}
//		
//			}*/
//		}
//		else
//		{
//			return;
//		}
//			
//		/** Layout the graph */
//		GraphDoLayout gdl = (GraphDoLayout) graphpad.getCurrentActionMap().
//								get(Utilities.getClassNameWithoutPackage(GraphDoLayout.class));
//		gdl.actionPerformed(null);
//
//		/** Update the hierarchy panel */
//		EditUpdateHierarchy ac = (EditUpdateHierarchy) graphpad.getCurrentActionMap().
//			get(Utilities.getClassNameWithoutPackage(EditUpdateHierarchy.class));
//		ac.actionPerformed(null);
//			
//	}
//		
//} 
//	
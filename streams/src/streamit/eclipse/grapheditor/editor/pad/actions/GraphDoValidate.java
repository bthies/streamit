/*
 * Created on Feb 4, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GEPipeline;

/**
 *  Actions that calidate the Graph Structure so that it represents legal StreamIt code.
 * @author jcarlos
 */
public class GraphDoValidate extends AbstractActionDefault {

	/**
	 * Constructor for GraphDoLayout.
	 * @param graphpad
	 */
	public GraphDoValidate(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Validate the Graph Structure so that it represents legal StreamIt code.
	 */
	public void actionPerformed(ActionEvent e) 
	{
			
//		System.out.println("File Save Action Performed");
//		
//		Object[] cells = getCurrentGraph().getVertices(getCurrentGraph().getSelectionCells());
//		byte errorCode = 0;
//		for (int i=0; i < cells.length;i++)
//		{
//			GEStreamNode node = (GEStreamNode) cells[i];
//			if (node instanceof GEPhasedFilter)
//			{
//				if (!(testFilterEdges(node))) errorCode |=1;
//			}
//			else if (node instanceof GEJoiner)
//			{
//				if (!(testJoinerEdges(node))) errorCode |=2;
//			}
//			else if (node instanceof GESplitter)
//			{
//				if (!(testSplitterEdges(node))) errorCode |=3;
//			}
//			else if (node instanceof GESplitJoin)
//			{
//				if (!(testSplitJoinEdges(node))) errorCode |=4;
//			}
//			else if (node instanceof GEFeedbackLoop)
//			{
//				if (!(testFeedbackLoopEdges(node))) errorCode |=5;
//			}
//		}
//		
//		evaluateErrorCode(errorCode);
//		
//	}
//
//	public void evaluateErrorCode(byte errorCode)
//	{
// 		if ((errorCode & 1) != 0)
// 		{
// 			System.err.println("Edge Error: Filter");
// 		}
// 		else if ((errorCode & 2) != 0)
//		{
//			System.err.println("Edge Error: Joiner");
//		}
//		else if ((errorCode & 3) != 0)
//		{
//			System.err.println("Edge Error: Splitter");
//		}
//		else if ((errorCode & 4) != 0)
//		{
//			System.err.println("Edge Error: SplitJoin");
//		}
//		else if ((errorCode & 5) != 0)
//		{
//			System.err.println("Edge Error: FeedbackLoop");
//		}
//		else 
//		{
//			System.err.println("No Errors");
//		}
//	}
//	
//
//
//	public boolean testFilterEdges(GEStreamNode node)
//	{
//		if ((node.getSourceEdges().size() > 1) || (node.getTargetEdges().size() > 1))
//		{	
//			return false;
//		}
//		return true;
//	}
//	
//	public boolean testJoinerEdges(GEStreamNode node)
//	{
//		if (node.getTargetEdges().size() > 1)
//		{
//			return false;
//		}
//		return true;
//	}
//
//	public boolean testSplitterEdges(GEStreamNode node)
//	{
//		if(node.getSourceEdges().size() > 1)
//		{
//			return false;
//		}
//		return true;
//	}
//
//	
//	public boolean testPipelineEdges(GEStreamNode node)
//	{
//		if ((node.getSourceEdges().size() > 1) || (node.getTargetEdges().size() > 1))
//		{	
//			return false;
//		}
//		return true;		
//	}
//	
//	
//	public boolean testSplitJoinEdges(GEStreamNode node)
//	{
//		if ((node.getSourceEdges().size() > 1) || (node.getTargetEdges().size() > 1))
//		{	
//			return false;
//		}
//		return true;		
//	}
//	
//	public boolean testFeedbackLoopEdges(GEStreamNode node)
//	{
//		if ((node.getSourceEdges().size() > 1) || (node.getTargetEdges().size() > 1))
//		{	
//			return false;
//		}
//		return true;		
//	}
	}
	
}



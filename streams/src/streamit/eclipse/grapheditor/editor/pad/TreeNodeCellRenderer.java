/*
 * Created on Feb 7, 2004
 */
package streamit.eclipse.grapheditor.editor.pad;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import streamit.eclipse.grapheditor.editor.pad.resources.ImageLoader;
import streamit.eclipse.grapheditor.graph.GEFeedbackLoop;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GEPhasedFilter;
import streamit.eclipse.grapheditor.graph.GEPipeline;
import streamit.eclipse.grapheditor.graph.GESplitJoin;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;

/**
 * TreeNodeCellRenderer contains the information necessary to render 
 * the hierarchy tree according to the options set. 
 * @author jcarlos
 */
public class TreeNodeCellRenderer extends DefaultTreeCellRenderer {
	
	/** Default Constructor */
	public TreeNodeCellRenderer(){}

	/**
	 * Get the TreeCellRendererComponent.
	 * @return Component
	 */
	public Component getTreeCellRendererComponent(
						JTree tree,
						Object value,
						boolean sel,
						boolean expanded,
						boolean leaf,
						int row,
						boolean hasFocus) {

		super.getTreeCellRendererComponent(
						tree, value, sel,
						expanded, leaf, row,
						hasFocus);
		if (leaf && (value instanceof TreeNode))
		{
			GEStreamNode node = ((TreeNode)value).getNode();
			if (node instanceof GEPhasedFilter)
			{
				setIcon (ImageLoader.getImageIcon("filterTreeIcon.gif"));	
			}			
			else if (node instanceof GESplitter) 
			{
				setIcon(ImageLoader.getImageIcon("splitterTreeIcon.gif"));
				
			} 
			else if (node instanceof GEJoiner)
			{
				setIcon(ImageLoader.getImageIcon("joinerTreeIcon.gif"));
			} 
		}
		else if (value instanceof TreeNode)
		{
			GEStreamNode node = ((TreeNode)value).getNode();
			if (node instanceof GEPipeline)
			{
				setIcon (ImageLoader.getImageIcon("pipelineTreeIcon.gif"));	
			}			
			else if (node instanceof GESplitJoin) 
			{
				setIcon(ImageLoader.getImageIcon("splitjoinTreeIcon.gif"));
			} 
			else if (node instanceof GEFeedbackLoop)
			{
				setIcon(ImageLoader.getImageIcon("feedbackloopTreeIcon.gif"));
			} 		
		}
		return this;
	}
}

/*
 * Created on Feb 7, 2004
 */
package streamit.eclipse.grapheditor.editor.pad;

import javax.swing.tree.DefaultMutableTreeNode;

import streamit.eclipse.grapheditor.graph.GEStreamNode;

/**
 * TreeNode object is used to display the information and structure
 * in the hierarchy tree. 
 * @author jcarlos
 */
public class TreeNode extends DefaultMutableTreeNode{
	
	/** The GEStreamNode that TreeNode represents */
	private GEStreamNode node;
	
	/** Default Constructor */
	public TreeNode(GEStreamNode node)
	{
		this.node = node;
	}
	
	/**
	 * Get the GEStreamNode belonging to the TreeNode.
	 * @return GEStreamNode
	 */
	public GEStreamNode getNode()
	{
		return this.node;		
	}
	
	/**
	 * Get the String representation of TreeNode.
	 * @return String representation of the object. 
	 */
	public String toString()
	{
		return node.getName();
	}
}
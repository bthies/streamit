/*
 * Created on Feb 6, 2004
 */
package streamit.eclipse.grapheditor.editor.pad;

import java.awt.GridLayout;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Panel that contains the Tree with the Graph hierarchy structure. 
 * @author jcarlos
 */

public class TreePanel extends JPanel implements TreeSelectionListener 
{
	/** The tree to be displayed in the TreePanel */
	private JTree tree;	
	
	/** The GraphStructure used as a model to construct the tree */
	private GraphStructure graphStruct = null;
	
	/** The last node that was visited in the tree */
	private GEStreamNode lastHNode = null;

	/** "Angled" (the default), "Horizontal", and "None". */
	private static String lineStyle = "Horizontal";
 
 	/** 
  	* Default constructor.
  	*/   
	public TreePanel() 
	{
		super(new GridLayout(1,0));
	}

	/**
	 * Create the tree with the hierarchy structure according to the GraphStructure. 
	 */
	public void createJTree()
	{
		DefaultMutableTreeNode toplevel = this.graphStruct.getTopLevel();
		TreeNode top = new TreeNode(this.graphStruct.getTopLevel());
		createNodes(top);
		
		tree = new JTree(top);
		
		/** Create a tree that allows one selection at a time.*/
		tree.getSelectionModel().setSelectionMode
						(TreeSelectionModel.SINGLE_TREE_SELECTION);

		/** Set the line style */
		tree.putClientProperty("JTree.lineStyle", lineStyle);

		/** Set the renderer for the tree */
		tree.setCellRenderer(new TreeNodeCellRenderer());
		
		/** Listen for when the selection changes. */
		tree.addTreeSelectionListener(this);

		/** Create the scroll pane and add the tree to it. */ 
		JScrollPane treeView = new JScrollPane(tree);
		add(treeView);
		
	}

	/** 
	 * Required by TreeSelectionListener interface. 
	 */
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
						   tree.getLastSelectedPathComponent();

		if (node == null) return;
		graphStruct.getJGraph().clearSelection();
		
		if (!(lastHNode == null))
		{
			lastHNode.highlight(this.graphStruct, false);
		}
		lastHNode = ((TreeNode)node).getNode();
		lastHNode.highlight(this.graphStruct, true);
		graphStruct.getJGraph().addSelectionCell(((TreeNode)node).getNode());
	}

	/**
	 * Create the nodes in the graph in a hierarchical manner.
	 * @param top TreeNode that is the parent of the nodes that will be 
	 * added as children. 
	 */
	private void createNodes(TreeNode top) 
	{
		Iterator nodeIter = ((GEContainer)top.getNode()).getContainedElements().iterator();
			while (nodeIter.hasNext())
			{
				TreeNode node = new TreeNode ((GEStreamNode)nodeIter.next());
				top.add(node);
				if (node.getNode() instanceof GEContainer)
				{
					createNodes(node);
				}
			}
	}
 
 	/**
 	 * Set the graphStructure for this.
 	 * @param graphStruct GraphStructure
 	 */
 	public void setGraphStructure(GraphStructure graphStruct)
 	{
 		this.graphStruct = graphStruct;
 	}
 
 	/**
 	 * Update the tree whenever the graphStruct has been modified. 
 	 */
 	public void update()
 	{
 		System.err.println("UPDATING THE PANEL");
 		this.removeAll();
 		this.createJTree();
 //		this.createNodes(new TreeNode(this.graphStruct.getTopLevel()));
 	}
}

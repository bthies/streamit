/*
 * Created on Feb 12, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * Action that updates the hierarchy panel.
 * @author jcarlos
 *
 */
public class EditUpdateHierarchy extends AbstractActionDefault {

	/**
	 * Constructor for EditUpdateHierarchy.
	 * @param graphpad
	 * @param name
	 */
	public EditUpdateHierarchy(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * Delete a GEStreamNode or an edge in the GraphStructure. When a Container 
	 * node is deleted, 
	 */
	public void actionPerformed(ActionEvent e) 
	{	
		graphpad.getCurrentDocument().getTreePanel().update();
		graphpad.getCurrentDocument().updateUI();
		graphpad.update();	
	}
	
}
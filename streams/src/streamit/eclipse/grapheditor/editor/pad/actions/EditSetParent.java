/*
 * Created on Jan 28, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * 
 * @author jcarlos
 */
public class EditSetParent extends AbstractActionDefault {

	/**
	 * Constructor for EditSetParent.
	 * @param graphpad
	 */
	public EditSetParent(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) 
	{
			Object[] cells = getCurrentGraph().getSelectionCells();
			
	}

}

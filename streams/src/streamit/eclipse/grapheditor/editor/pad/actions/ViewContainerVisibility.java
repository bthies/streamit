/*
 * Created on Jan 27, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;


import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.utils.Utilities;



/**
 * @author jcarlos
 */

public class ViewContainerVisibility extends AbstractActionCheckBox {

	static boolean HIDE = true;


	/**
	 * Constructor for GraphOptionsDisconnectable.
	 * @param graphpad
	 */
	public ViewContainerVisibility(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionToggle#isSelected(String)
	 */
	public boolean isSelected(String actionCommand) {
		if (getCurrentGraph() == null)
			return false;
		return (!ViewContainerVisibility.HIDE);
	}

	/**
	 * 
	 */
	public void actionPerformed(ActionEvent e) {
		getCurrentGraph().clearSelection();
		
		if (ViewContainerVisibility.HIDE)
		{
			ViewContainerVisibility.HIDE = false;
			ViewContainersUnhide ac = (ViewContainersUnhide) graphpad.getCurrentActionMap().
													get(Utilities.getClassNameWithoutPackage(ViewContainersUnhide.class));
			ac.actionPerformed(null);	
		}
		else
		{
			ViewContainerVisibility.HIDE = true;
			ViewContainersHide ac = (ViewContainersHide) graphpad.getCurrentActionMap().
													get(Utilities.getClassNameWithoutPackage(ViewContainersHide.class));
			ac.actionPerformed(null);				
		}	
		
		
	}

}
/*
 * Created on Jan 27, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;


import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.utils.Utilities;



/**
 * Action that hides or unhides the container nodes depending on their current status.
 * If the containers are visible, they will be hidden. If the containers are not visible, 
 * they will be unhidden.
 * 
 * @author jcarlos
 */

public class ViewContainerVisibility extends AbstractActionCheckBox {

	/** Static variable that determines if the containers are visible or not*/
	//TODO: change this variable to non-static and put it in GPDocument
//	public static boolean HIDE = true;


	/**
	 * Constructor for ViewContainerVisibility.
	 * @param graphpad GPGraphpad
	 */
	public ViewContainerVisibility(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * @see org.jgraph.pad.actions.AbstractActionToggle#isSelected(String)
	 */
	public boolean isSelected(String actionCommand) {
		GPDocument doc = graphpad.getCurrentDocument();
		if (getCurrentGraph() == null)
			return false;
		if (doc.areContainersInvisible())
		{
			return false;
		}
		else
		{
			return true;
		}
		
	}

	/**
	 * Hide or unhide the container nodes depending on their current status.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		/** Clear selections to avoid any issues when the selected nodes will disappear */
		getCurrentGraph().clearSelection();
		GPDocument doc = graphpad.getCurrentDocument();
		
		/** Case when the containers are not visible */
		if (doc.areContainersInvisible())
		{
			doc.setContainersInvisible(false);
			
			/** Make the containers visible */
			ViewContainersUnhide ac = (ViewContainersUnhide) graphpad.getCurrentActionMap().
													get(Utilities.getClassNameWithoutPackage(ViewContainersUnhide.class));
			ac.actionPerformed(null);	
		}
		/** Case when the containers are visible */
		else
		{
			doc.setContainersInvisible(true);
			
			/** Make the containers invisible */
			ViewContainersHide ac = (ViewContainersHide) graphpad.getCurrentActionMap().
													get(Utilities.getClassNameWithoutPackage(ViewContainersHide.class));
			ac.actionPerformed(null);				
		}	
		
		
	}

}
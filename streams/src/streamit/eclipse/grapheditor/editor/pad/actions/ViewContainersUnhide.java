/*
 * Created on Jan 26, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that unhides all of the expanded container nodes in the graph.
 * @author jcarlos
 */
public class ViewContainersUnhide extends AbstractActionDefault {


	public ViewContainersUnhide(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * Unhide the expanded container nodes in the graph.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
		graphStruct.containerNodes.unhideAllContainers();
		ViewContainerVisibility.HIDE = false;
		ViewSetContainerLocation ac = (ViewSetContainerLocation) graphpad.getCurrentActionMap().
												get(Utilities.getClassNameWithoutPackage(ViewSetContainerLocation.class));
				ac.actionPerformed(null);
		
	}
}

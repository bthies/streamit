/*
 * Created on Jan 26, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that hides all of the expanded container nodes in the graph.
 * @author jcarlos
 */
public class ViewContainersHide extends AbstractActionDefault {


	public ViewContainersHide(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * Hide the expanded container nodes in the graph.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
		graphStruct.containerNodes.hideAllContainers();
	}
}

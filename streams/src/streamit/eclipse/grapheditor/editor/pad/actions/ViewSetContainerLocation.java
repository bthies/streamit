/*
 * Created on Jan 26, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * @author jcarlos
 *

 */
public class ViewSetContainerLocation extends AbstractActionDefault {

	//
	
	/**
	 * Constructor for ViewSetContainerLocation.
	 * @param graphpad
	 * @param name
	 */
	public ViewSetContainerLocation(GPGraphpad graphpad) {
		super(graphpad);
	}
		
	/**
	 */
	public void actionPerformed(ActionEvent e) 
	{	
		GPDocument doc = graphpad.getCurrentDocument();
		GraphStructure graphStruct = doc.getGraphStructure();
		if (!(doc.areContainersInvisible()))
		{
			graphStruct.containerNodes.setLocationOfContainersAtLevel(graphStruct);
			
		}
	}
}

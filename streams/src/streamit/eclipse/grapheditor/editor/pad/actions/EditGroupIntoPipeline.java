/*
 * Created on Feb 3, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEPipeline;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class EditGroupIntoPipeline extends AbstractActionDefault {

	/**
	 * Constructor for ShapeToFront.
	 * @param graphpad
	 */
	public EditGroupIntoPipeline(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();

		ArrayList succList = new ArrayList(); 
		
		GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure(); 
		GEPipeline pipeline = new GEPipeline("Pipeline_"+ GEProperties.id_count++);
			
		for (int i = 0; i < cells.length; i++)
		{
			if (cells[i] instanceof GEStreamNode)
			{
				GEStreamNode node = (GEStreamNode)cells[i];
				GEProperties.setParentProperty(node, pipeline);
			}
		} 
		pipeline.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
	}
}
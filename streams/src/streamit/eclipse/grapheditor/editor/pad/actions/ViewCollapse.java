/*
 * Created on Dec 5, 2003
 *
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * @author jcarlos
 *
 */

public class ViewCollapse extends AbstractActionDefault {

	/**
	 * Constructor for ViewCollapse.
	 * @param graphpad
	 * @param name
	 */
	public ViewCollapse(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	public void actionPerformed(ActionEvent e) 
	{
		
		graphpad.getCurrentDocument().setResizeAction(null);
		if (graphpad.getCurrentGraph().getScale() < 8) 
		{
			GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure();
			int currentLevelView = graphStruct.getCurrentLevelView();
			graphStruct.collapseContainersAtLevel(currentLevelView);
			
			graphStruct.setCurrentLevelView(--currentLevelView);
			
			if (currentLevelView < 0 )
			{
				graphStruct.setCurrentLevelView(0);
			}
			
			graphpad.getCurrentDocument().setScale(graphpad.getCurrentGraph().getScale() * 1.2);
			if (graphpad.getCurrentGraph().getSelectionCell() != null)
			{
				graphpad.getCurrentGraph().scrollCellToVisible(graphpad.getCurrentGraph().getSelectionCell());
				
			}	
		}
	}
}
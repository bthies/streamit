package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;


/**
 * Actiom that copies a GEStreamNode
 * @author jcarlos
 */
public class EditCopy extends AbstractActionDefault {

	/**
	 * Constructor for EditCopy.
	 * @param graphpad
	 */
	public EditCopy(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Action that copies a GEStreamNode.
	 * Currently you can only copy non-container nodes. 
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();
		getCurrentGraph().clearSelection();
		if (cells != null) 
		{
			/** Have one cell selected */
			if (cells.length == 1)
			{
				GEStreamNode node = (GEStreamNode) cells[0]; 
			 	graphpad.getCurrentDocument().setClonedNode((GEStreamNode) node.clone());
			}	
		}
	}
}

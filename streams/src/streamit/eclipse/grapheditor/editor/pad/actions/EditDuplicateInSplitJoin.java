/*
 * Created on Mar 6, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.controllers.GESplitJoinDuplicationDialog;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GEType;

/**
 * Action that duplicates an inner child of the SplitJoin. An inner child
 * of the splitjoin is connected to the splitter and the joiner in the GESplitJoin.
 *  
 * 
 * @author jcarlos
 */
public class EditDuplicateInSplitJoin extends AbstractActionDefault {

	/**
	 * Constructor for EditDuplicateInSplitJoin.
	 * @param graphpad
	 */
	public EditDuplicateInSplitJoin(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * Duplicate an inner child of a splitjoin.
	 */
	public void actionPerformed(ActionEvent e) 
	{
		GEStreamNode cell = (GEStreamNode) graphpad.getCurrentDocument().getGraph().getSelectionCell();
		
		if (cell != null)
		{
			/** Get the most immediate SplitJoin that has the selected node as its ancestor */
			GEContainer container = cell.getEncapsulatingNode();
			while (container != null)
			{
				if (container.getType() == GEType.SPLIT_JOIN)
				{
					System.out.println(" The Container is " + container.toString());
					GESplitJoinDuplicationDialog dialog =  new GESplitJoinDuplicationDialog(new JFrame());
							
					dialog.setVisible(true);
					if (dialog.canceled()) return;
					int duplicationNumber = dialog.getInputValue();
					
					for (int i= 0; i < duplicationNumber; i++)
					{
						System.out.println("Duplication "+ i);			
					}
					break;
				}
				container = container.getEncapsulatingNode();
			}
		}
	}
	
}
/*
 * Created on Feb 3, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.GEFeedbackLoop;
import streamit.eclipse.grapheditor.graph.GEJoiner;
import streamit.eclipse.grapheditor.graph.GEProperties;
import streamit.eclipse.grapheditor.graph.GESplitter;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * @author jcarlos
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class EditGroupIntoFeedbackLoop extends AbstractActionDefault {

	/**
	 * Constructor for EditGroupIntoSplitJoin.
	 * @param graphpad
	 */
	public EditGroupIntoFeedbackLoop(GPGraphpad graphpad) {
		super(graphpad);
	}
	
	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		Object[] cells = getCurrentGraph().getSelectionCells();
		GESplitter splitter = null;
		GEJoiner joiner = null;
		ArrayList succList = new ArrayList(); 
		
		for (int j = 0; j < cells.length; j++)
		{
			if (cells[j] instanceof GESplitter)
			{
				splitter = (GESplitter) cells[j];
			}
			else if (cells[j] instanceof GEJoiner)
			{
				joiner =  (GEJoiner) cells[j];
			}
			else if (cells[j] instanceof GEStreamNode)
			{
				succList.add(cells[j]);
			}
		}
		
		if (splitter == null)
		{
			JOptionPane.showMessageDialog(graphpad,
				"The SplitJoin does not have an assigned Splitter.",
				"Error",
				JOptionPane.ERROR_MESSAGE);

			return;	
		}
		else if (joiner == null)
		{
			JOptionPane.showMessageDialog(graphpad,
				"The SplitJoin does not have an assigned Joiner.",
				"Error",
				JOptionPane.ERROR_MESSAGE);			
			return;
		}
		
		GraphStructure graphStruct  = graphpad.getCurrentDocument().getGraphStructure(); 
		
		// TODO: Should not be setting body and loop to null
		GEFeedbackLoop floop = new GEFeedbackLoop("FeedbackLoop_"+ GEProperties.id_count++, 
													splitter, joiner, null, null);
		
		Object[] nodeList = succList.toArray();
		
		for (int i = 0; i < nodeList.length; i++)
		{
			GEStreamNode node = (GEStreamNode)nodeList[i];
			GEProperties.setParentProperty(node, floop);
		}

		floop.initializeNode(graphStruct, graphStruct.containerNodes.getCurrentLevelView());
	
	}
}
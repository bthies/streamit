
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * Action that closes the a file document in the graph editor.
 * @author jcarlos
 */
public class FileClose extends AbstractActionDefault {

	/**
	 * Constructor for FileClose.
	 * @param graphpad
	 * @param name
	 */
	public FileClose(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Close the current document in the graph editor. 
	 */
	public void actionPerformed(ActionEvent e) 
	{
		if (graphpad.getCurrentDocument() == null)
		{
			return;
		}
		if (graphpad.getCurrentDocument().close(true))
		{
			graphpad.removeDocument(graphpad.getCurrentDocument());
		}
	}

}

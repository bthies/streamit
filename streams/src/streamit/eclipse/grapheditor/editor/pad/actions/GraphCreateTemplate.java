/*
 * Created on Mar 30, 2004
 */
package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.graph.ErrorCode;
import streamit.eclipse.grapheditor.graph.GEContainer;
import streamit.eclipse.grapheditor.graph.GraphStructure;

/**
 * Action that creates the template code for the graph in the editor.
 * @author Administrator
 */
public class GraphCreateTemplate extends AbstractActionDefault 
{

	/**
	 * Constructor for GraphCreateTemplate.
	 * @param graphpad
	 */
	public GraphCreateTemplate(GPGraphpad graphpad) {
		super(graphpad);
	}

	/**
	 * Create the template code for the graph in the editor. 
	 */
	public void actionPerformed(ActionEvent e) 
	{
	
		StringBuffer strBuff = new StringBuffer();
		GraphStructure graphStruct = graphpad.getCurrentDocument().getGraphStructure(); 
		
		
		/** Reset the visited value to false. This way we will keep track of what nodes
		 * 	has been visited so that we do not */

		
	
		for (Iterator contIter = graphStruct.containerNodes.getAllContainers().iterator(); contIter.hasNext();)
		{
			int code = ((GEContainer)contIter.next()).checkValidity();
			if (code < 0)
			{
				ErrorCode.handleErrorCode(code);
				return;
			}
		}

		ArrayList nameList = new ArrayList();
		/** Generate the output code from the graph. This will be stored
		 * 	int the StringBuffer strBuff */
		graphStruct.outputCode(strBuff, nameList);
	
	
		try 
		{
			/** Must update the ifile corresponding to this document */
			IFile ifile = graphpad.getCurrentDocument().getIFile();
			if (ifile != null)
			{
				/** Convert the StringBuffer to a ByteArrayInputStream */ 
				ByteArrayInputStream bais = new ByteArrayInputStream(strBuff.toString().getBytes());
			
				/** The ByteArrayInputStream will be used to set the contents of the 
				 * corresponding ifile */
				ifile.setContents(bais, false, false, null);
				
				/** Refresh */
				ifile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
			}
				
		} 
		catch (Exception exception) 
		{
			exception.printStackTrace();
		}
	}
}

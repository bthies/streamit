package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import streamit.eclipse.grapheditor.editor.GPGraphpad;

/**
 * Action opens a dialog to select the file.
 * After that the action saves the current graph to
 * the selected file.
 *
 * @author sven.luzar
 *
 */
public class FileSave extends AbstractActionFile {

	/**
	 * Constructor for FileSave.
	 * @param graphpad
	 * @param name
	 */
	public FileSave(GPGraphpad graphpad) {
		super(graphpad);
	}


	
	public void actionPerformed(ActionEvent e) 
	{/*
		System.out.println("File Save Action Performed");
		
		PrintWriter out = null;
		
		try 
		{
			//out = new PrintWriter(new FileWriter(graphpad.getCurrentDocument().getFile()));
			 graphpad.getCurrentDocument().getGraphStructure().outputCode(out);

			
			//out.write("This is only a test");
			//out.println("public class HelloWorld6 extends StreamItPipeline");
			printMore(out);		
			//out.write("printing after printwriter");
			
			try 
			{
				IFile ifile = graphpad.getCurrentDocument().getIFile();
				if (ifile != null)
				{
					String hello = new String("Hello\n World\n");
					ByteArrayInputStream bais = new ByteArrayInputStream(hello.getBytes());
					FileInputStream fis = new FileInputStream(graphpad.getCurrentDocument().getFile());
					ifile.appendContents(bais, false, false, null);
					
					ifile.getParent().refreshLocal(IResource.DEPTH_ONE, null);
				}
				
			} 
			catch (Exception exception) 
			{
				exception.printStackTrace();
			}
			
			
		} 
		catch (Exception exc) 
		{
			exc.printStackTrace();
		}
		if (out != null) 
		{
			try 
			{
				out.close();
			} 
			catch (Exception exception) 
			{
				exception.printStackTrace();
			}
		}*/
	}
	
	private void printMore(PrintWriter out)
	{
		out.write("printing inside printwriter");
	}

}

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



	/** Shows a file chooser if the filename from the
	 *  document is null. Otherwise the method
	 *  uses the file name from the document.
	 *
	 *  Furthermore the method uses the registered
	 *  file formats for the save process.
	 */
/*
	public void actionPerformed(ActionEvent e) {
		GPDocument doc = getCurrentDocument();
		URL filename = doc.getFilename();
		GPGraph gpGraph = doc.getGraph();

		// Special case: We are running as a Tiki applet and the file
		// is a remote file -> Save to Tiki
		String savePath = null;
		if (graphpad.getApplet() != null)
			savePath = graphpad.getApplet().getParameter("savepath");
		if (savePath != null && !savePath.equals("") && filename != null &&
		    filename.toString().toLowerCase().startsWith("http://")) {
		    JGraphpad.uploadToTiki(graphpad, doc);
		    doc.setModified(false);
		    return;
		} else if (filename != null && filename.toString().toLowerCase().startsWith("http://")) {
		    filename = null;
		}

		//		GraphModel model = gpGraph.getModel();
		//		JComponent accessory = null;

		// +TODO check if file already exists, if so, then prompt warning "There is already a file by this name, do you want to replace the existing file?  If you select Yes, all contents of the existing file will be permanently lost."; if No then reopen filechooser+
		URL file;
		if (filename == null) {
			// show the file chooser
			GPFileChooser chooser = new GPFileChooser(doc);
			chooser.setDialogTitle(Translator.getString("FileSaveAsLabel"));

			int result = chooser.showSaveDialog(graphpad);
			if (result == JFileChooser.CANCEL_OPTION)
				return;

			// get the file format
			FileFilter fileFilter = chooser.getFileFilter();

			try {
				file = chooser.getSelectedFile().toURL();
				/*
				 * Bug Fix: do not add jgx by default!
				 * use the file extension from the 
				 * file format see (1) 
				 * 
				if (file.toString().indexOf(".") < 0)
					file = new URL(file+".jgx");
				*/
/*				
			} catch (MalformedURLException eurl) {
				JOptionPane.showMessageDialog(
					graphpad,
					eurl.getLocalizedMessage(),
					Translator.getString("Error"),
					JOptionPane.ERROR_MESSAGE);
				return;
			}

			GraphModelFileFormat graphModelFileFormat =
				GraphModelProviderRegistry.getGraphModelFileFormat(
					file.toString());

			// if no file format was found try to specify the
			// file format by using the file filter
			if (graphModelFileFormat == null) {
				graphModelFileFormat =
					GraphModelProviderRegistry.getGraphModelFileFormat(
						fileFilter);
				try {
					// (1) if the file has no file extension
					// add the correct file extension
					file =
						new URL(
							file.toString()
								+ "."
								+ graphModelFileFormat.getFileExtension());
				} catch (MalformedURLException eurl) {
					JOptionPane.showMessageDialog(
						graphpad,
						eurl.getLocalizedMessage(),
						Translator.getString("Error"),
						JOptionPane.ERROR_MESSAGE);
				}
			}

			// sets the writeProperties
			// for the next write process
			if (chooser.getAccessory() != null)
				gpGraph.setWriteProperties(
					graphModelFileFormat.getWriteProperties(
						chooser.getAccessory()));

			// memorize the filename
			filename = file;

		} else {
			// exract the file object
			file = filename;
		}

		// exract the provider and the file format
		//		GraphModelProvider graphModelProvider =
		//			GraphModelProviderRegistry.getGraphModelProvider(file);
		GraphModelFileFormat graphModelFileFormat =
			GraphModelProviderRegistry.getGraphModelFileFormat(file.toString());

		// extract the writer properties
		Hashtable props = gpGraph.getWriteProperties();

		// try to write the file
		try {
			graphModelFileFormat.write(
				file,
				props,
				gpGraph,
				gpGraph.getModel());
			doc.setFilename(filename);
			doc.setModified(false);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(
				graphpad,
				ex.getLocalizedMessage(),
				Translator.getString("Error"),
				JOptionPane.ERROR_MESSAGE);
		} finally {
			graphpad.update();
			graphpad.invalidate();
		}
	
	}
*/
	
	public void actionPerformed(ActionEvent e) 
	{
		System.out.println("File Save Action Performed");
		/*
		PrintWriter out = null;
		
		try 
		{
			//out = new PrintWriter(new FileWriter(graphpad.getCurrentDocument().getFile()));
			// graphpad.getCurrentDocument().getGraphStructure().outputCode(out);

			
			//out.write("This is only a test");
			//out.println("public class HelloWorld6 extends StreamItPipeline");
			//printMore(out);		
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

/*
 */

package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ActionMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.GPFileChooser;
import streamit.eclipse.grapheditor.editor.pad.GPGraph;
import streamit.eclipse.grapheditor.editor.pad.GPSelectProvider;
import streamit.eclipse.grapheditor.editor.pad.GraphModelProvider;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GraphEncoder;

/**
 * @author jcarlos
 *
 */
public class FileOpen extends AbstractActionFile {

	/**
	 * Constructor for FileOpen.
	 * @param graphpad
	 * @param name
	 */
	public FileOpen(GPGraphpad graphpad) {
		super(graphpad);
		System.out.println("Called FileOpen constructor");
	}

	/** Shows a file chooser with the
	 *  file filters from the file formats
	 *  to select a file.
	 *
	 *  Furthermore the method uses the selected
	 *  file format for the read process.
	 *
	 *  @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 *  @see GraphModelProviderRegistry
	 */
	
	public void actionPerformed(ActionEvent e) {
		// show the file chooser
		GPFileChooser chooser = new GPFileChooser(null);
		chooser.setDialogTitle(Translator.getString("openLabel"));

		// return if cancel
		int result = chooser.showOpenDialog(graphpad);
		if (result == JFileChooser.CANCEL_OPTION)
			return;

		this.open(chooser.getSelectedFile());	
	}

	/**
	 * Open the file if it is not open already.
	 * @param file File to be opened
	 * @return true if it was possible to open the file, false otherwise.
	 */
	public boolean open(File file)
	{
		if (!(isFileOpen(file)))
		{	
			return openStreamItFile(file);
		}		
		else
		{
			return false;
		}
	}
	
	/**
	 * Determine if file is already open or not.
	 * @param file File 
	 * @return true if the file is already open, false otherwise. 
	 */
	private boolean isFileOpen(File file)
	{
		URL name;
		try 
		{
			name = file.toURL();
		} 
		catch (MalformedURLException eurl) 
		{
			return false;
		}
		
		if (name != null) 
		{
			// get all open files to test against
			GPDocument[] docs = graphpad.getAllDocuments();

			if (docs != null) 
			{
				for (int i = 0; i < docs.length; i++) 
				{
					URL docname = docs[i].getFilename();
		
					// check if names are the same
					if (docname != null && name.equals(docname)) 
					{
					//	int r = JOptionPane.showConfirmDialog(graphpad, Translator.getString("FileAlreadyOpenWarning"), "Warning", JOptionPane.YES_NO_OPTION);
					
						// if YES then user wants to revert to previously saved version
					//	if (r == JOptionPane.YES_OPTION)  {
							// close existing internal frame without saving
							graphpad.removeGPInternalFrame(
								docs[i].getInternalFrame());

							// open old version to revert to
							//openStreamItFile(file);
							return false;

						//}
						// doesn't want to revert and already open so cancel open
						//return true;
					}
				}
			}
		} 
		return false;
	}
	

	/**
	 * Open the StreamIt file so that it is compiled and can be displayed graphically.
	 * @param file
	 * @return true if it was possible to open the fie and display StreamIt source graphically.
	 */
	public boolean openStreamItFile(File file)
	{	
		GPSelectProvider selectProvider = new GPSelectProvider(graphpad.getFrame() );
		selectProvider.show() ;
		if (selectProvider.getAnswer() == GPSelectProvider.OPTION_CANCEL )
			return false;
		
		
		GraphModelProvider graphModelProvider = selectProvider.getSelectedGraphModelProvider() ;
		if (graphModelProvider == null)
			return false;

		String fileName = "";
		URL fileURL = null;
		try 
		{
			fileName = file.getPath().toString();
			fileURL = file.toURL();
		}
		catch (MalformedURLException eurl) 
		{
			System.out.println("MalformedURLExeption: "+ eurl +" in openEclipseFile (FileOpen.java)");
			return false;
		}
		catch (Exception ex)
		{
			System.out.println("Exception: " + ex + "in openEclipseFile (FileOpen.java)" );
			return false;
		}
		
		System.out.println("FILE NAME = " + fileName);
		System.out.println("Path == "+ System.getProperties().getProperty("java.class.path"));
		//at.dms.kjc.Main.compile(new String[] {"--graph", "-s", "/u/jcarlos/Test/VectAdd/VectAdd.java"});
		System.out.println("JAVA FILE: "+ fileName.substring(0, fileName.indexOf(".")+ 1 )+"java");
		streamit.frontend.ToJava.main(new String[] {"--output", fileName.substring(0, fileName.indexOf(".")+ 1 )+"java", fileName});
		at.dms.kjc.Main.compile(new String[] {"--streamit", "--graph", "--verbose", fileName.substring(0, fileName.indexOf(".")+ 1 )+"java"});
		
		//graphpad.addDocument(graphModelProvider);
		
		GPGraph gGraph = new GPGraph(GraphEncoder.graph.getGraphModel());
		gGraph.setGraphLayoutCache(new GraphLayoutCache(gGraph.getModel(), gGraph, false, true));
		
		//GPDocument doc= graphpad.addDocument(null, graphModelProvider, gGraph , GraphEncoder.graph.getGraphModel(), null);
		GPDocument doc= graphpad.addDocument(fileURL, graphModelProvider, gGraph , gGraph.getModel(), GraphEncoder.graph, null);

		GraphEncoder.graph.setJGraph(gGraph);
		GraphEncoder.graph.constructGraph(doc.getScrollPane());
		//gGraph.getGraphLayoutCache().setVisible(gGraph.getRoots(), true);
		graphpad.update();	
		
		ViewExpand ve = (ViewExpand) graphpad.getCurrentActionMap().get(Utilities.getClassNameWithoutPackage(ViewExpand.class));
		ve.centerLayout();		
		graphpad.update();
		return true;
	}

	protected void addDocument(GPFileChooser chooser) 
	{
		final File file = chooser.getSelectedFile();
		
		GPSelectProvider selectProvider = new GPSelectProvider(graphpad.getFrame() );
		selectProvider.show() ;
		if (selectProvider.getAnswer() == GPSelectProvider.OPTION_CANCEL )
			return;
		
		
		GraphModelProvider graphModelProvider = selectProvider.getSelectedGraphModelProvider() ;
		if (graphModelProvider == null)
			return;

		String fileName = "";
	
		try 
		{
			fileName = file.getPath().toString();
		}
		catch (Exception ex){}

		System.out.println("FILE NAME = " + fileName);
		System.out.println("Path == "+ System.getProperties().getProperty("java.class.path"));
		//at.dms.kjc.Main.compile(new String[] {"--graph", "-s", "/u/jcarlos/Test/VectAdd/VectAdd.java"});
		System.out.println("JAVA FILE: "+ fileName.substring(0, fileName.indexOf(".")+ 1 )+"java");
		streamit.frontend.ToJava.main(new String[] {"--output", fileName.substring(0, fileName.indexOf(".")+ 1 )+"java", fileName});
		at.dms.kjc.Main.compile(new String[] {"--streamit", "--graph", "--verbose", fileName.substring(0, fileName.indexOf(".")+ 1 )+"java"});
		
		//graphpad.addDocument(graphModelProvider);
		GPGraph gGraph = new GPGraph(GraphEncoder.graph.getGraphModel());
		gGraph.setGraphLayoutCache(new GraphLayoutCache(gGraph.getModel(), gGraph, false, true));
		
		
		GPDocument doc= graphpad.addDocument(null, graphModelProvider, gGraph , GraphEncoder.graph.getGraphModel(), null);
		/*GraphEncoder.graph.setJGraph(gGraph);*/
		doc.getGraphStructure().setJGraph(gGraph);

		/*GraphEncoder.graph.constructGraph(doc.getScrollPane());*/
		doc.getGraphStructure().constructGraph(doc.getScrollPane());
		
		
		//gGraph.getGraphLayoutCache().setVisible(gGraph.getRoots(), true);
		graphpad.update();
		
		ViewExpand ve = (ViewExpand) graphpad.getCurrentActionMap().get(Utilities.getClassNameWithoutPackage(ViewExpand.class));
		ve.centerLayout();
		
	}

	
/*
	protected void addDocument(GPFileChooser chooser) {
		// get the file format, provider
		final File file = chooser.getSelectedFile();

		final GraphModelProvider graphModelProvider =
			GraphModelProviderRegistry.getGraphModelProvider(file);
		final GraphModelFileFormat graphModelFileFormat =
			GraphModelProviderRegistry.getGraphModelFileFormat(file);

		// error message if no file format found
		if (graphModelFileFormat == null) {
			JOptionPane.showMessageDialog(
				graphpad,
				Translator.getString("Error.No_GraphModelFileFormat_available"),
				Translator.getString("Error"),
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		// extract the read properties
		final Hashtable props =
			graphModelFileFormat.getReadProperties(chooser.getAccessory());

		Thread t = new Thread("Read File Thread") {

			public void run() {
					// create a new and clean graph
	GPGraph gpGraph =
		graphModelProvider.createCleanGraph(
			graphModelProvider.createCleanGraphModel());

				// try to read the graph model
				GraphModel model = null;

				URL fileURL;
				try {
					fileURL = file.toURL();

					try {
						model =
							graphModelFileFormat.read(fileURL, props, gpGraph);

					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(
							graphpad,
							ex.getLocalizedMessage(),
							Translator.getString("Error"),
							JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					// posibility to cancel the load 
					// process
					if (model == null)
						return; 

					// FIX: A small hack to reset the filename
					// if it has no standard extension
					/*
					 * Sorry, we can't do this!
					 *  
					 * We have got a multi file format support
					 * and it can be that it is not the jgx
					 * file extension and thats absolutly 
					 * correct!
					 * 
					if (!fileURL.toString().toLowerCase().endsWith(".jgx")) {
						graphpad.error(Translator.getString("OldFileFormat"));
						fileURL = null;
					}
					*/
/*
					// add the new document with the new graph and the new model
					graphpad.addDocument(
						fileURL,
						graphModelProvider,
						gpGraph,
						model,
						null);

					graphpad.update();
				} catch (MalformedURLException e) {
				}
			}
		};
		t.start();

	}
*/



	/** Empty implementation.
	 *  This Action should be available
	 *  each time.
	 */
	public void update() {
	};

}
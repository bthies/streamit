/*
 */

package streamit.eclipse.grapheditor.editor.pad.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JFileChooser;

import org.jgraph.graph.GraphLayoutCache;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.GPFileChooser;
import streamit.eclipse.grapheditor.editor.pad.GPGraph;
import streamit.eclipse.grapheditor.editor.pad.GPSelectProvider;
import streamit.eclipse.grapheditor.editor.pad.GraphModelProvider;
import streamit.eclipse.grapheditor.editor.pad.resources.Translator;
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
							graphpad.removeGPInternalFrame(docs[i].getInternalFrame());
							return false;
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
		System.out.println("JAVA FILE: "+ fileName.substring(0, fileName.indexOf(".")+ 1 )+"java");
		
		streamit.frontend.ToJava.main(new String[] {"--output", fileName.substring(0, fileName.indexOf(".")+ 1 )+"java", fileName});
		at.dms.kjc.Main.compile(new String[] {"--streamit", "--graph", "--verbose", fileName.substring(0, fileName.indexOf(".")+ 1 )+"java"});
		
		//graphpad.addDocument(graphModelProvider);
		
		GPGraph gGraph = new GPGraph(GraphEncoder.graph.getGraphModel());
		gGraph.setGraphLayoutCache(new GraphLayoutCache(gGraph.getModel(), gGraph, false, true));
		gGraph.setEditable(false);
		gGraph.setDisconnectable(false);
		
		GPDocument doc= graphpad.addDocument(fileURL, graphModelProvider, gGraph , gGraph.getModel(), GraphEncoder.graph, null);
		//GPDocument doc= graphpad.addDocument(null, graphModelProvider, gGraph , GraphEncoder.graph.getGraphModel(), null);
	
		GraphEncoder.graph.setJGraph(gGraph);
		GraphEncoder.graph.constructGraph(doc.getScrollPane());

		graphpad.getCurrentDocument().treePanel.setGraphStructure(graphpad.getCurrentDocument().getGraphStructure());		
		graphpad.getCurrentDocument().treePanel.createJTree();
		graphpad.getCurrentDocument().updateUI();
		graphpad.update();	
		return true;
	}

	/** Empty implementation.
	 *  This Action should be available
	 *  each time.
	 */
	public void update() {
	};

}

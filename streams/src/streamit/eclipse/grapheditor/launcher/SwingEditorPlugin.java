package streamit.eclipse.grapheditor.launcher;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.ActionMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import streamit.eclipse.grapheditor.editor.GPGraphpad;
import streamit.eclipse.grapheditor.editor.JGraphpad;
import streamit.eclipse.grapheditor.editor.pad.GPDocument;
import streamit.eclipse.grapheditor.editor.pad.actions.FileNew;
import streamit.eclipse.grapheditor.editor.pad.actions.FileOpen;
import streamit.eclipse.grapheditor.editor.utils.Utilities;
import streamit.eclipse.grapheditor.graph.GEStreamNode;
import streamit.eclipse.grapheditor.graph.LogFile;
import streamit.eclipse.grapheditor.graph.NodeCreator;

/**
 * The main plugin class used to mediate the communications to and 
 * from the GraphEditor 
 */
public class SwingEditorPlugin extends AbstractUIPlugin {

	/**
	 * The shared instance.
	 */
	private static SwingEditorPlugin plugin;
	
	/**
	 * Resource bundle.
	 */
	private ResourceBundle resourceBundle;

	/**
	 * The StreamIt GraphEditor main application instance. It 
	 * is a singleton since we only want one window with the 
	 * grapheditor opened.
	 */
	private static GPGraphpad graphpad;

	private boolean workSpaceChanged = false;

	/**
	 * The constructor.
	 */
	public SwingEditorPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		plugin = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle(
					"com.ibm.lab.soln.swing.internalLaunch.extras.SwingEditorPluginResources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}

	/**
	 * Returns the shared instance of the plug-in.
	 */
	public static SwingEditorPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle =
			SwingEditorPlugin.getDefault().getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return key;
		}
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * Open an instance of the GraphEditor 
	 * @param file The file (*.str) that should be opened by the editor.
	 */
	public void launchGraph(IFile file)
	{
		try { 
		
		boolean isFileNew = false;
		InputStream is = file.getContents();
		System.out.println("File has this much in it: "+is.available());
		if (is.available() == 0)
		{
			isFileNew = true;
		}

		System.out.println("Entered launchGraph");
		System.out.println("Path == "+ System.getProperties().getProperty("java.class.path"));
		System.setProperty("java.class.path", System.getProperty("java.class.path")+ ";C:\\Program Files\\eclipse\\workspace\\streamit\\src\\streamit.jar" 
							+";C:\\Program Files\\eclipse\\workspace\\streamit.eclipse.grapheditor.graph\\graph.jar" 
							+";C:\\Program Files\\eclipse\\workspace\\streamit.eclipse.grapheditor.jgraph\\jgraph.jar");

		if (graphpad == null)
		{
			//JGraphpad.main(new String[]{""});
			graphpad = JGraphpad.run();
		}

		
		if (isFileNew)
		{
			ActionMap actMap = graphpad.getCurrentActionMap();
			FileNew ac = (FileNew) actMap.get(Utilities.getClassNameWithoutPackage(FileNew.class));
			ac.actionPerformed(null);
			graphpad.getCurrentDocument().setIFile(file);
			graphpad.getCurrentDocument().setFile(new File(file.getLocation().toString()));
			
			//code to handle the log file
			graphpad.getCurrentDocument().getGraphStructure().setIFile(file);
			Object[] cells = graphpad.getCurrentGraph().getVertices(graphpad.getCurrentGraph().getAll());
			NodeCreator.addIFileMappings(cells, file);
		}
		else
		{
			ActionMap actMap = graphpad.getCurrentActionMap();
			FileOpen ac = (FileOpen) actMap.get(Utilities.getClassNameWithoutPackage(FileOpen.class));
			
			if (ac.open(new File(file.getLocation().toString())))
			{
			
				graphpad.getCurrentDocument().setIFile(file);
				graphpad.getCurrentDocument().setFile(new File(file.getLocation().toString()));
				System.out.println("The IFile " + graphpad.getCurrentDocument().getIFile().toString());
				
				//code to handle the log file
				graphpad.getCurrentDocument().getGraphStructure().setIFile(file);
				Object[] cells = graphpad.getCurrentGraph().getVertices(graphpad.getCurrentGraph().getAll());
				NodeCreator.addIFileMappings(cells, file);
				
			}
		}
		}
		catch (Exception e)
		{
			if ((graphpad != null)&&(graphpad.getDocumentWithIFile(file) == null))
			{
				openNewGraphFile(file);
				
			}
			System.out.println("An Exception has been caught while trying to launch graph");
		}
		
		
		/* *******************************************************
		** TEST CODE *********************************************
		**********************************************************/
		/*
		GPDocument doc = graphpad.getDocumentWithIFile(file);
		if (doc !=null)
		{
			System.out.println("TEST CODE SUCCEDDED (launchGraph in SwingEditorPlugin.java)");
		}
		else
		{
			System.out.println(" **** FAILURE **** FAILURE *** FAILURE");
		}
		highlightNodeInGraph(file, "SingleMultiply");
		highlightNodeInGraph(file, "VectSource");
		highlightNodeInGraph(file, "SingleMultiply");
		
		
		IFile fili = NodeCreator.getIFile(graphpad.getCurrentDocument().getGraphStructure().getTopLevel());
		System.out.println ("THE IFILE CORRESPONDING TO THE TOPLEVEL IS " + fili.toString());
		*/
		//  *****************************************************************
	}
	

	/**
	 * Closes the graph representation of the StreamIt file
	 * @param ifile IFile
	 * @return True if it was possible to close the file, false otherwise.
	 */
	public boolean closeGraphFile(IFile ifile)
	{
		GPDocument doc = graphpad.getDocumentWithIFile(ifile);
		if (doc != null)
		{
			doc.close(false);
			graphpad.removeDocument(doc);
			return true;
		}
		return false;
	}
	
	/**
	 * Get the toplevel node of the graph representation of the StremIt source
	 * that corresponds to the IFile
	 * @param ifile IFile from which the toplevel node will be obtained.
	 * @return GEStreamNode that is the toplevel, or null if there is no toplevel 
	 */
	public GEStreamNode getTopLevelFromGraphFile(IFile ifile)
	{
		GPDocument doc = graphpad.getDocumentWithIFile(ifile);
		if (doc != null)
		{
			return doc.getGraphStructure().getTopLevel();
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Hightlight a node in the graph
	 * @param ifile IFile corresponding to the graph.
	 * @param nodeName String name of the node.
	 * @return True if it was possible to highlight the node, false otherwise.
	 */
	public boolean highlightNodeInGraph(IFile ifile, String nodeName)
	{
		GPDocument doc = graphpad.getDocumentWithIFile(ifile);
		if (doc != null)
		{
			Object nodes[] = doc.getGraph().getVertices(doc.getGraph().getAll());;
			ArrayList nodesToHighlight = new ArrayList();
			for(int i=0; i < nodes.length; i++)
			{
				
				GEStreamNode currNode = (GEStreamNode) nodes[i];
	//			System.out.println("Vertex "+ i+ " "+ currNode.getName());
				int nodeNameIndex = currNode.getName().indexOf(nodeName);
				if (nodeNameIndex == 0)
				{
				//	System.out.println("The node name index is" + nodeNameIndex);
				//	System.out.println("Node to be highlighted" + currNode.getName());
					//doc.getGraphStructure().highlightNode(currNode);
					nodesToHighlight.add(currNode);

				}
			}
			doc.getGraphStructure().highlightNodes(nodesToHighlight);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Open a new StreamIt file.
	 * @param ifile IFile corresponding to the StreamIt source of the new graph file to be opened.
	 */
	
	private void openNewGraphFile(IFile ifile)
	{
		ActionMap actMap = graphpad.getCurrentActionMap();
		FileNew ac = (FileNew) actMap.get(Utilities.getClassNameWithoutPackage(FileNew.class));
		ac.actionPerformed(null);
		graphpad.getCurrentDocument().setIFile(ifile);
		graphpad.getCurrentDocument().setFile(new File(ifile.getLocation().toString())); // should the normal file also be set ???

		//code to handle the log file
		graphpad.getCurrentDocument().getGraphStructure().setIFile(ifile);
		Object[] cells = graphpad.getCurrentGraph().getVertices(graphpad.getCurrentGraph().getAll());
		NodeCreator.addIFileMappings(cells, ifile);
	
	}

	
	/**
	 * Set the log file.
	 * @param file File to which the log file will be set. 
	 */
	public void setLogFile(IFile file)
	{
		LogFile.setIFile(file);
	}
	
	/**
	 * Method used to communicate workspace updates to Eclipse 
	 */
	public void updateWorkspace(IFile file) {
		try {
			if (file != null)
				file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
			workSpaceChanged = true;
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

	/**
	 * A method to save the workspace
	 */
	public void saveWorkspace() throws CoreException {
		if (workSpaceChanged) {
			ResourcesPlugin.getWorkspace().save(true, null);
		}

	}
}
package streamit.eclipse.debugger.grapheditor;

import org.eclipse.core.resources.IFile;

//import streamit.eclipse.grapheditor.launcher.SwingEditorPlugin;

/**
 * @author kkuo
 */
public class TestSwingEditorPlugin {

	private static boolean fRunMode = false;

	private static TestSwingEditorPlugin fInstance = new TestSwingEditorPlugin();
	private TestSwingEditorPlugin() {
	}
	public static TestSwingEditorPlugin getInstance() {
		return fInstance;
	}

	public void toggleGraphEditor() {
		fRunMode = !fRunMode;
	}
	
	public boolean getGraphEditorState() {
		return fRunMode;
	}

	/**
	 * Open an instance of the GraphEditor 
	 * @param file The file (*.str) that should be opened by the editor.
	 */
	public void launchGraph(IFile file) {
		System.out.println("launchGraph " + file.getName());
		
		//if (fRunMode) SwingEditorPlugin.getDefault().launchGraph(file);
	}

	/**
	 * Closes the graph representation of the StreamIt file
	 * @param ifile IFile
	 * @return True if it was possible to close the file, false otherwise.
	 */
	public boolean closeGraphFile(IFile ifile) {
		System.out.println("closeGraphFile " + ifile.getName());
		//if (fRunMode) return SwingEditorPlugin.getDefault().closeGraphFile(ifile);
		return true;
	}
	
	/**
	 * Get the toplevel node of the graph representation of the StremIt 
	source
	 * that corresponds to the IFile
	 * @param ifile IFile from which the toplevel node will be obtained.
	 * @return GEStreamNode that is the toplevel, or null if there is no 
	toplevel 
	 */
	public TestGEStreamNode getTopLevelFromGraphFile(IFile ifile) {
		System.out.println("getTopLevelFromGraphFile");
		return new TestGEStreamNode();
	}

	/**
	  * Hightlight a node in the graph
	  * @param ifile IFile corresponding to the graph.
	  * @param nodeName String name of the node.
	  * @return True if it was possible to highlight the node, false 
	otherwise.
		  */
	public boolean highlightNodeInGraph(IFile ifile, String nodeName) {
		System.out.println("highlightNodeInGraph:  " + ifile.getName() + " " + nodeName);
		//if (fRunMode) return SwingEditorPlugin.getDefault().highlightNodeInGraph(ifile, nodeName);
		return true;
	}
	
	/**
	 * Set the log file.
	 * @param file File to which the log file will be set. 
	 */
	public void setLogFile(IFile file) {
		System.out.println("setLogFile:  " + file.getName());
		//if (fRunMode) SwingEditorPlugin.getDefault().setLogFile(file);
	}
	
}
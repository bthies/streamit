package streamit.eclipse.grapheditor.launcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorLauncher;

/**
 * Class that opens the GraphEditor on the Eclipse input file.
 * The IFile passed must be a valid *.str that compiles.
 * 
 * @author jcarlos
 */
public class GraphEditorLauncher implements IEditorLauncher {

	/**
	 * Opens the GraphEditor on *.str files in the Eclipse workspace.
	 * @param IFile streamit file that will be opened in the graph editor.
	 */
	public void open(IFile file) {
		System.out.println("Launch Successful : file Location = " + file.getLocation().toString());
		SwingEditorPlugin.getDefault().launchGraph(file);
	}

}

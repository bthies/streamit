package streamit.eclipse.grapheditor.launcher;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorLauncher;

/**
 * Class that opens the GraphEditor on the Eclipse input file
 * 
 */
public class GraphEditorLauncher implements IEditorLauncher {

	/**
	 * Opens the GraphEditor on *.str files in the Eclipse workspace.
	 * 
	 * @see org.eclipse.ui.IEditorLauncher#open(IFile)
	 */
	public void open(IFile file) {
		System.out.println( 
			"Launch Successful : file Location = "
				+ file.getLocation().toString());
		//SwingEditorPlugin.getDefault().getAbcEditor().openOnEclipseFile(file);
		SwingEditorPlugin.getDefault().launchGraph(file);
	}

}

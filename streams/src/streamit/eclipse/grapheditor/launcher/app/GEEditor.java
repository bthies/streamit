package streamit.eclipse.grapheditor.launcher.app;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import streamit.eclipse.grapheditor.launcher.SwingEditorPlugin;

/** ABCEditor is a simple example of a Swing application that provides a
 * text editor for files of type .abc. The editor provides a File menu and 
 * a couple of toolbar buttons to perform simple actions following the 
 *<b>Internal-Launch</b> pattern: Launched in a separate window but running
 * in the same JVM as the Eclipse Platform. This allows the tool to integrate
 * with the Platform Core, but not the Platform UI. API integration with
 * tool models provided by other plug-ins is possible.</li>
 * A plug-in manifest file can be used to register the ABCEditor as a command
 * on *.abc files.
 */
public class GEEditor extends JFrame {
	// Model methods...
	static Set openFiles = Collections.synchronizedSet(new HashSet());

	private IFile eclipseEditorInput;
	/** Open an .abc workspace file for editing.
	 * @param fileName the file to open
	 */
	public void openOnEclipseFile(IFile eclipseFileResource) {
		eclipseEditorInput = eclipseFileResource;
		runStandAlone = false;
		open(new File(eclipseFileResource.getLocation().toString()));
		fileOpenAction.setEnabled(false);
	}

	public class FileOpenAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			String fileName = null;
			JFileChooser chooser = new JFileChooser();
			int returnVal = chooser.showOpenDialog(GEEditor.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				fileName = chooser.getSelectedFile().getName();
				open(new File(fileName));
			}
		}
	}

	public class FileSaveAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			save();
		}
	}

	public class AddSomeTextAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			// post a dialog requesting some text to add
			contents.append("this is new contents\n");
		}
	}

	//--------------------------------------------------------------------------------

	private static final int maxURLCount = 10;

	private JPanel rootPanel = null;
	private JMenuBar menuBar = null;
	private JToolBar toolBar = null;

	// Menu and toolBar actions
	
	private Action fileOpenAction = new FileOpenAction();
	private Action fileSaveAction = new FileSaveAction();
	private Action addSomeTextAction = new AddSomeTextAction();

	private File input = new File("newFile");
	private JTextArea contents = null;

	private boolean runStandAlone;

	//--------------------------------------------------------------------------------

	/** Create the ABCEditor
	 * @param frameName the window title
	 */
	public GEEditor(String fileName) {

		// Create the main window
		super(fileName);

		// build the UI
		rootPanel = new JPanel();
		rootPanel.setLayout(new BorderLayout());

		// add the menu bar
		setJMenuBar(createMenuBar());

		// add the toolbar
		toolBar = createToolBar();
		rootPanel.add(toolBar, "North");

		// the panel for everything else
		contents = new JTextArea();
		contents.setLineWrap(true);
		rootPanel.add(new JScrollPane(contents), "Center");
		rootPanel.setPreferredSize(new Dimension(400, 200));
		getContentPane().add(rootPanel);
		pack();
		setLocation(new Point(100, 80));
		setVisible(true);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we_Event) {
				try {
					SwingEditorPlugin.getDefault().saveWorkspace();
				} catch (CoreException e) {
				}
				if (runStandAlone)
					System.exit(0);

			}
		});
	}

	/**
	 * Constructor ABCEditor.
	 */
	public GEEditor() {
		super();

	}

	/** Creaqte the ABCEditor menu bar
	 * @return the menubar
	 */
	protected JMenuBar createMenuBar() {
		menuBar = new JMenuBar();
		menuBar.add(getFileMenu());

		return menuBar;
	}

	/** Create the toolBar for the Editor.
	 * @return the toolBar
	 */
	public JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar();
		toolBar.setBorder(BorderFactory.createEtchedBorder());

		JButton button = null;

		button = toolBar.add(fileOpenAction);
		button.setText("Open");
		button.setToolTipText("Open a .abc file");

		button = toolBar.add(fileSaveAction);
		button.setText("Save");
		button.setToolTipText("save this .abc file");

		toolBar.addSeparator();

		button = toolBar.add(addSomeTextAction);
		button.setText("Add");
		button.setToolTipText("press this button to add some text");

		return toolBar;
	}

	/** Get the File menu.
	 */
	protected JMenu getFileMenu() {
		JMenu fileMenu = new JMenu("File", true);

		JMenuItem menuItem = null;

		menuItem = fileMenu.add(fileOpenAction);
		menuItem.setText("Open...");
		menuItem.setMnemonic('O');

		fileMenu.addSeparator();

		menuItem = fileMenu.add(fileSaveAction);
		menuItem.setText("Save");
		menuItem.setMnemonic('S');

		fileMenu.addSeparator();

		menuItem = new JMenuItem("Exit");
		menuItem.setMnemonic('E');
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (runStandAlone)
					System.exit(0);
				else {
					setVisible(false);
					dispose();
				}

			}
		});
		fileMenu.add(menuItem);

		return fileMenu;
	}

	/** Create an ABCEditor and open it on the given .abc file.
	 * @param argv the command line arguments can be empty, or a single .abc file
	 */
	public final static void main(String[] argv) {
		if (argv.length > 1) {
			System.out.println("Usage: java ABCEditor <fileName>.abc");
			System.exit(1);
		}
		String fileName = "newFile.abc";
		if (argv.length == 1) {
			fileName = argv[0];
		}

		GEEditor editor = new GEEditor("External Launch ABC Editor");
		//As opposed to running inside eclipse 
		editor.setRunStandAlone(true);
		editor.open(new File(fileName));
	}

	// Model methods...

	/** Open a .abc file for editing.
	 * @param fileName the file to open
	 */
	public boolean open(File file) {

		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String s;
			while ((s = in.readLine()) != null) {
				contents.append(s);
				contents.append("\n");
			}
		} catch (FileNotFoundException exc) {
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		if (in != null) {
			try {
				in.close();
			} catch (Exception exc) {
			}
		}
		input = file;
		setTitle("ABC Edit: " + input.getName());
		return true;
	}

	/** Save the current .abc file.
	 */
	public void save() {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new FileWriter(input));
			out.write(contents.getText());
			SwingEditorPlugin.getDefault().updateWorkspace(eclipseEditorInput);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		if (out != null) {
			try {
				out.close();
			} catch (Exception exc) {
			}
		}
	}
	/**
	 * Sets the runStandAlone.
	 * @param runStandAlone The runStandAlone to set
	 */
	private void setRunStandAlone(boolean runStandAlone) {
		this.runStandAlone = runStandAlone;
	}

}

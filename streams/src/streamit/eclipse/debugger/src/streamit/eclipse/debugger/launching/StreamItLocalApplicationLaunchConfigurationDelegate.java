package streamit.eclipse.debugger.launching;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.core.IStreamItCoreConstants;
import streamit.eclipse.debugger.core.PreDebuggingRunnable;
import streamit.eclipse.debugger.core.StrJavaData;
import streamit.eclipse.debugger.core.StreamItLineNumber;
import streamit.eclipse.debugger.ui.StreamItViewsManager;
import streamit.eclipse.debugger.wizards.StreamItProjectNature;

/**
 * @author kkuo
 */
public class StreamItLocalApplicationLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private class ErrorMessage {
		private String message;
		private StreamItLineNumber strLineNumber;
		protected ErrorMessage(String m, StreamItLineNumber l) {
			message = m;
			strLineNumber = l;
		}
		protected String getMessage() {
			return message;
		}
		protected StreamItLineNumber getStrLineNumber() {
			return strLineNumber;
		}
	}

	public static IFile[] getStrFiles(ILaunchConfiguration configuration, IProject project, String mainClassName) throws CoreException {
		String primaryStrFileName = mainClassName + IStreamItLaunchingConstants.PERIOD_CHAR + IStreamItLaunchingConstants.STR_FILE_EXTENSION;
		String secondaryFileNames = configuration.getAttribute(IStreamItLaunchingConstants.ATTR_SECONDARY_CLASSES, IStreamItLaunchingConstants.EMPTY_STRING);
		StringTokenizer st = new StringTokenizer(secondaryFileNames);
		IFile[] strFiles = new IFile[st.countTokens() + 1];
		strFiles[0] = project.getFile(primaryStrFileName);
		for (int i = 1; i < strFiles.length; i++) strFiles[i] = project.getFile(st.nextToken());
		return strFiles;
	}
	
	public static String[] strFilesToAbsoluteNames(IFile[] strFiles, String prefix, String postfix) {
		String[] names = new String[strFiles.length];
		for (int i = 0; i < strFiles.length; i++)
			names[i] = prefix + strFiles[i].getLocation().toOSString() + postfix;
		return names;
	}

	/**
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String, ILaunch, IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// need to do something with ILaunch launch (add processes, etc)

		if (monitor == null) monitor = new NullProgressMonitor();
		
		// Get information from configuration
		String projectName = getJavaProjectName(configuration);
		String mainClassName = getMainTypeName(configuration);
		String javaFileName = getJavaFileName(configuration);

		// verify
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.hasNature(StreamItProjectNature.NATURE_ID)) {
			displayError();
			return;
		}

		// kill any existing launches
		ILaunchManager manager = getLaunchManager();
		killStrThreads(manager, javaFileName);

		// run streamit.frontend.ToJava
		IFile[] strFiles = getStrFiles(configuration, project, mainClassName);
		String[] args = new String[3 + strFiles.length];
		args[0] = IStreamItLaunchingConstants.FILE_OUTPUT_OPTION;
		IFile javaFile = project.getFile(javaFileName);
		args[1] = javaFile.getLocation().toOSString();
		args[2] = IStreamItLaunchingConstants.LIBRARY_OPTION;
		String[] names = strFilesToAbsoluteNames(strFiles, IStreamItLaunchingConstants.EMPTY_STRING, IStreamItLaunchingConstants.EMPTY_STRING);
		for (int i = 3; i < strFiles.length + 3; i++) args[i] = names[i - 3];
		
		// hijack error stream
		PrintStream err = System.err;
		ByteArrayOutputStream newErr = new ByteArrayOutputStream();
		System.setErr(new PrintStream(newErr));

		// run streamit.frontend.ToJava
		javaFile.setReadOnly(false);
		int result = 0;
		try {
			result = new streamit.frontend.ToJava().run(args);			
		} catch (Exception e) {
			reportError(strFiles, names, e.getMessage(), new Vector(), true);
			return;
		}
		javaFile.refreshLocal(IResource.DEPTH_ONE, null);
		javaFile.setReadOnly(true);

		// restore error stream
		System.setErr(err);
		String errors = newErr.toString();
		Vector javaErrors = removeMarkers(strFiles, javaFile); 
		if (result != 0) {
			reportError(strFiles, names, errors, javaErrors, false);
			return;
		}

		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			DebugUIPlugin.getStandardDisplay().syncExec(new PreDebuggingRunnable(project, args[1], javaFileName, javaFile, strFiles[0]));
		} else {
			// filter Debug view even when just running
			DebugUIPlugin.getStandardDisplay().syncExec(new Runnable() { 
				public void run() {
					try {
						StreamItViewsManager.addViewFilters(); 
					} catch (WorkbenchException e) {
					}
				}
			});
		}

		// run java code
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		ILaunchConfigurationWorkingCopy wc = type.newInstance(null, configuration.getName() + IStreamItLaunchingConstants.ID_STR_APPLICATION);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainClassName);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, getProgramArguments(configuration));
		wc.setAttribute(IStreamItLaunchingConstants.ATTR_STR_LAUNCH, true);
		ILaunch tempLaunch = wc.launch(mode, monitor);
		setDefaultSourceLocator(tempLaunch, wc);
	}
	
	public boolean launchJava(ILaunchConfiguration configuration) throws CoreException {
		String projectName = getJavaProjectName(configuration);
		String mainClassName = getMainTypeName(configuration);
		String javaFileName = getJavaFileName(configuration);

		// verify
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.hasNature(StreamItProjectNature.NATURE_ID)) {
			displayError();
			return false;
		}

		// run streamit.frontend.ToJava
		IFile[] strFiles = getStrFiles(configuration, project, mainClassName);
		String[] args = new String[3 + strFiles.length];
		args[0] = IStreamItLaunchingConstants.FILE_OUTPUT_OPTION;
		IFile javaFile = project.getFile(javaFileName);
		args[1] = javaFile.getLocation().toOSString();
		args[2] = IStreamItLaunchingConstants.LIBRARY_OPTION;
		String[] names = strFilesToAbsoluteNames(strFiles, IStreamItLaunchingConstants.EMPTY_STRING, IStreamItLaunchingConstants.EMPTY_STRING);
		for (int i = 3; i < strFiles.length + 3; i++) args[i] = names[i - 3];
		
		// hijack error stream
		PrintStream err = System.err;
		ByteArrayOutputStream newErr = new ByteArrayOutputStream();
		System.setErr(new PrintStream(newErr));
		
		// run streamit.frontend.ToJava
		javaFile.setReadOnly(false);
		int result = 0;
		try {
			result = new streamit.frontend.ToJava().run(args);			
		} catch (Exception e) {
			reportError(strFiles, names, e.getMessage(), new Vector(), true);
			return false;
		}
		javaFile.refreshLocal(IResource.DEPTH_ONE, null);
		javaFile.setReadOnly(true);

		// restore error stream
		System.setErr(err);
		String errors = newErr.toString();
		Vector javaErrors = removeMarkers(strFiles, javaFile); 
		if (javaErrors.size() == 0 && result == 0) return true;
		reportError(strFiles, names, errors, javaErrors, false);
		return false;
	}
	
	private Vector removeMarkers(IFile[] strFiles, IFile javaFile) {
		Vector errors = new Vector();;

		try {
			for (int i = 0; i < strFiles.length; i++) strFiles[i].deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
			
			// find any .java errors from markers of .java
			IMarker[] markers = javaFile.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
			String message, javaText;
			int javaLineNumber, index;
			StrJavaData data = new StrJavaData(javaFile, false);
			IDocument doc = data.getJavaDocument();
			StreamItLineNumber strLineNumber;
			String[] tokens = null;		
			for (int i = 0; i < markers.length; i++) {
				// screen .java problems
				message = markers[i].getAttribute(IMarker.MESSAGE, IStreamItLaunchingConstants.EMPTY_STRING);
				if (message.indexOf(IStreamItLaunchingConstants.THE_IMPORT_STRING) != -1 
					&& message.indexOf(IStreamItLaunchingConstants.NEVER_USED_STRING) != -1) continue;
				javaLineNumber = markers[i].getAttribute(IMarker.LINE_NUMBER, -1);
				if (javaLineNumber == -1) continue;

				// find str line number
				javaText = doc.get(doc.getLineOffset(javaLineNumber-1), doc.getLineLength(javaLineNumber-1));
				if (tokens == null) tokens = strFilesToAbsoluteNames(strFiles, IStreamItCoreConstants.COMMENT_STRING, IStreamItCoreConstants.COLON_STRING);
				for (int j = 0; j < tokens.length; j++) {
					if (javaText.indexOf(tokens[j]) == -1) continue;
					strLineNumber = new StreamItLineNumber(Integer.valueOf(javaText.substring(javaText.indexOf(tokens[j]) + tokens[j].length()).trim()).intValue(), strFiles[j]);
					errors.add(new ErrorMessage(message, strLineNumber));
					break;
				}
			}
		} catch (BadLocationException e) {
		} catch (CoreException ce) {
		}
		return errors;
	}
	
	protected void reportError(IFile[] strFiles, String[] names, String error, Vector javaErrors, boolean exception) {

		// parse error
		StringTokenizer st = new StringTokenizer(error, IStreamItLaunchingConstants.NEWLINE_STRING);
		String strLine, message;
		
		String[] tokens = new String[strFiles.length];
		for (int i = 0; i < tokens.length; i++) tokens[i] = names[i] + IStreamItLaunchingConstants.COLON_CHAR;

		try {
			int line, offset, firstcolon, secondcolon;
			IMarker marker;
			IDocument doc;
			boolean displayError = false;
			while (st.hasMoreTokens()) {
				strLine = st.nextToken();
				for (int i = 0; i < tokens.length; i++) {
					if (strLine.indexOf(tokens[i]) != -1) {
					
						// error= "Unrecognized variable: y at C:\\Program Files\\eclipse\\runtime-workspace\\Minimal\\Minimal.str:9"
						if (exception) {
							message = strLine.substring(0, strLine.indexOf(IStreamItLaunchingConstants.AT));
							line = Integer.parseInt(strLine.substring(strLine.lastIndexOf(IStreamItLaunchingConstants.COLON_CHAR) + 1));
						} else {
							//7:17: expecting RCURLY, found ';'\r\n
							message = strLine.substring(strLine.indexOf(tokens[i]) + tokens[i].length());
							firstcolon = message.indexOf(IStreamItLaunchingConstants.COLON_CHAR);
							line = Integer.valueOf(message.substring(0, firstcolon)).intValue();
							message = message.substring(message.indexOf(IStreamItLaunchingConstants.COLON_CHAR, firstcolon + 1) + 1).trim();
						}

						// get/create strfile's document
						if (displayError) doc = null;
						else doc = getStrDocument(strFiles[i]);
						if (doc == null) displayError = true;
	
						ResourcesPlugin.getWorkspace().run(createProblemMarker(strFiles[i], doc, line, message), null);	
						break;
					}
				}
			}
			
			// add java errors
			ErrorMessage em;
			StreamItLineNumber strLineNumber;
			Iterator i = javaErrors.iterator();
			while (i.hasNext()) {
				em = (ErrorMessage) i.next();
				strLineNumber = em.getStrLineNumber();

				// get/create strfile's document
				if (displayError) doc = null;
				else doc = getStrDocument(strLineNumber.getStrFile());
				if (doc == null) displayError = true;

				ResourcesPlugin.getWorkspace().run(createProblemMarker(strLineNumber.getStrFile(), doc, strLineNumber.getLineNumber(), em.getMessage()), null);	
			}
		} catch (CoreException ce) {
		}
		// tell user
		displayError();
	}
	
	private IDocument getStrDocument(IFile strFile) {
		IWorkbenchWindow window = StreamItViewsManager.getActiveWorkbenchWindow();
		if (window == null) return null;
		IEditorPart strEditorPart = window.getActivePage().findEditor(new FileEditorInput(strFile));
		if (strEditorPart == null || !(strEditorPart instanceof ITextEditor)) return null;
		ITextEditor strEditor = (ITextEditor) strEditorPart;
		return strEditor.getDocumentProvider().getDocument(strEditor.getEditorInput());
	}
	
	private void displayError() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(getShell(), LauncherMessages.getString(IStreamItLaunchingConstants.JAVA_APPLICATION_LAUNCH_SHORTCUT_ERROR), 
				LaunchingMessages.getString(IStreamItLaunchingConstants.JAVA_APPLICATION_LAUNCH_SHORTCUT_EXCEPTION));
			}
		});
	}

	private IWorkspaceRunnable createProblemMarker(final IFile strFile, final IDocument doc, final int line, final String message) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				try {
					IMarker marker = strFile.createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.LINE_NUMBER, line);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.MESSAGE, message);

					if (doc == null) return;
					
					IRegion region = doc.getLineInformation(line - 1); 
					String highlight;
					int offset = doc.getLineOffset(line - 1);
					int start, index;					
					
					if (message.indexOf(IStreamItLaunchingConstants.EXPECTING_STRING) != -1
						&& message.indexOf(IStreamItLaunchingConstants.FOUND_STRING) != -1) {
						// expecting DOT, found '='
						index = message.indexOf(IStreamItLaunchingConstants.QUOTE_CHAR);
						highlight = message.substring(index + 1, message.indexOf(IStreamItLaunchingConstants.QUOTE_CHAR, index + 1));
					} else if (message.indexOf(IStreamItLaunchingConstants.UNEXPECTED_STRING) != -1) {
						// unexpected token: add
						index = message.indexOf(IStreamItLaunchingConstants.COLON_CHAR);
						highlight = message.substring(index + 1).trim();
					} else {
						// mapped java error
						highlight = doc.get(offset, doc.getLineLength(line - 1)).trim();
					}

					start = doc.search(offset, highlight, true, true, false);
					marker.setAttribute(IMarker.CHAR_START, start);
					marker.setAttribute(IMarker.CHAR_END, start + highlight.length());

				} catch (BadLocationException ble) {
				}
			}
		};
	}	

	protected Shell getShell() {
		return JDIDebugUIPlugin.getActiveWorkbenchShell();
	}
	
	public static boolean isStreamItLaunch(ILaunchConfiguration config) {
		return config.getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) != -1;
	}
	
	public static boolean isStreamItLaunch(ILaunch launch) {
		return isStreamItLaunch(launch.getLaunchConfiguration());
	}
	
	public static String getMainClassName(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
	}
	
	public static String getJavaFileName(ILaunchConfiguration configuration) throws CoreException {
		return getMainClassName(configuration) + IStreamItLaunchingConstants.PERIOD_CHAR + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION;
	}
	
	public static String getProjectName(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
	}

	public static IFile getJavaFile(ILaunchConfiguration configuration) throws CoreException {
		String projectName = getProjectName(configuration);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (!project.hasNature(StreamItProjectNature.NATURE_ID)) return null;
		return project.getFile(getJavaFileName(configuration));
	}
	
	public static void killStrThreads(ILaunchManager manager, String javaFileName) throws CoreException {
		// kill any existing launches
		ILaunch[] launches = manager.getLaunches();
		ILaunchConfiguration config;
		for (int i = 0; i < launches.length; i++) {
			config = launches[i].getLaunchConfiguration(); 
			if (!isStreamItLaunch(config)) continue;
			if (javaFileName.equals(getJavaFileName(config))) launches[i].terminate();
		}
	}
}
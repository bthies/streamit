package streamit.eclipse.debugger.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointLocationVerifier;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.launching.StreamItApplicationLaunchShortcut;
import streamit.eclipse.debugger.launching.StreamItLocalApplicationLaunchConfigurationDelegate;
import streamit.eclipse.debugger.ui.StreamItViewsManager;
import streamit.eclipse.debugger.wizards.StreamItProjectNature;

/**
 * @author kkuo
 */
public class StrJavaMapper {

	private static StrJavaMapper fInstance = new StrJavaMapper();
	private static StreamItApplicationLaunchShortcut fLaunchShorcut = new StreamItApplicationLaunchShortcut();
	private static StreamItLocalApplicationLaunchConfigurationDelegate fLaunchDelegate = new StreamItLocalApplicationLaunchConfigurationDelegate();
		
	// key = IResource strFile, entry = StrJavaData
	private static HashMap fPrimaryStrFiles = new HashMap();

	// key = IResource strFile, entry = StrJavaData
	private static HashMap fJavaFiles = new HashMap();

	private StrJavaMapper() {
	}
	
	public static StrJavaMapper getInstance() {
		return fInstance;
	}
	
	public StrJavaData loadStrFile(IFile strFile, boolean force) {
		// strFile may be secondary
		StrJavaData data;
		if (force) {
			data = (StrJavaData) fPrimaryStrFiles.get(strFile);
			if (data != null) {
				fPrimaryStrFiles.remove(strFile);
				fJavaFiles.remove(data.getJavaFile());
			}
		} 
		data = (StrJavaData) fPrimaryStrFiles.get(strFile);
		IWorkbenchPage page = StreamItViewsManager.getActivePage();
		
		try {
			if (data != null) return data;

			int empty = strFile.getContents().available();
			String strName = strFile.getName();
			strName = strName.substring(0, strName.lastIndexOf(IStreamItLaunchingConstants.PERIOD_CHAR + IStreamItLaunchingConstants.STR_FILE_EXTENSION));
			IProject project = strFile.getProject();
			if (!project.hasNature(StreamItProjectNature.NATURE_ID)) return null;
			
			ILaunchConfiguration configuration = fLaunchShorcut.findLaunchConfiguration(project.getName(), strName, ILaunchManager.RUN_MODE);
			String javaFileName = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFileName(configuration);
			IFile javaFile = project.getFile(javaFileName);
			
			boolean success = true;
			if ((force || !javaFile.exists()) && empty != 0) {
				success = fLaunchDelegate.launchJava(configuration);
				javaFile = project.getFile(javaFileName);
			}
			
			data = new StrJavaData(javaFile, !success || empty == 0);
			mapStrJava(configuration, project, StreamItLocalApplicationLaunchConfigurationDelegate.getMainClassName(configuration), data);
			page.addPartListener(StreamItViewsManager.getInstance());
			fPrimaryStrFiles.put(strFile, data);
			fJavaFiles.put(javaFile, data);
		} catch (PartInitException pie) {
		} catch (CoreException ce) {
		} catch (IOException ioe) {
		}
		return data;
	}
	
	public StrJavaData getStrJavaData(IFile javaFile) {
		return (StrJavaData) fJavaFiles.get(javaFile);
	}
	
	private void mapStrJava(ILaunchConfiguration configuration, IProject project, String mainClassName, StrJavaData data) {
		// key = IFile strFile, entry = HashMap (key = Integer, entry = Integer, str to java line numbers)
		// only valid java line numbers for breakpoints
		HashMap strToJavaBreakpoints = new HashMap();
		HashMap strToJavaWatchpoints = new HashMap();
		HashMap javaToStrLineNumbers = new HashMap();
		try {
			
			BreakpointLocationVerifier bv = new BreakpointLocationVerifier();
			int validJavaLineNumber;
		
			// look for commented mappings
			IDocument doc = data.getJavaDocument();
			StringTokenizer st = new StringTokenizer(doc.get(), IStreamItCoreConstants.NEWLINE_STRING);
			String javaLine;
			int javaLineNumber = 0;
			IFile[] strFiles = StreamItLocalApplicationLaunchConfigurationDelegate.getStrFiles(configuration, project, mainClassName);
			String[] tokens = StreamItLocalApplicationLaunchConfigurationDelegate.strFilesToAbsoluteNames(strFiles, IStreamItCoreConstants.COMMENT_STRING, IStreamItCoreConstants.COLON_STRING);
			HashMap temp;
			IEditorInput input = data.getJavaFileEditorInput();
			while (st.hasMoreTokens()) {
				javaLineNumber++;
				javaLine = st.nextToken(); // // C:\Program Files\eclipse\runtime-workspace\HelloWorld6\HelloWorld6.str:2
				for (int i = 0; i < tokens.length; i++) {
					if (javaLine.indexOf(tokens[i]) == -1) continue;

					// get line numbers
					validJavaLineNumber = bv.getValidBreakpointLocation(doc, javaLineNumber - 1);
					Integer strLineNumber = Integer.valueOf(javaLine.substring(javaLine.indexOf(tokens[i]) + tokens[i].length()));

					if (javaLineNumber == validJavaLineNumber) {
						// add line numbers
						// get breakpoint hashmap for specific file i
						temp = (HashMap) strToJavaBreakpoints.get(strFiles[i]);
						if (temp == null) temp = new HashMap();

						Object o = temp.get(strLineNumber);
						if (o == null) {
							temp.put(strLineNumber, new Integer(validJavaLineNumber));
							strToJavaBreakpoints.put(strFiles[i], temp);
						}
					}

					if (isWatchpoint(doc, validJavaLineNumber, input)) {
						// get breakpoint hashmap for specific file i
						temp = (HashMap) strToJavaWatchpoints.get(strFiles[i]);
						if (temp == null) temp = new HashMap();

						Object o = temp.get(strLineNumber);
						if (o == null) {
							temp.put(strLineNumber, new Integer(javaLineNumber));
							strToJavaWatchpoints.put(strFiles[i], temp);
						}
					}

					javaToStrLineNumbers.put(new Integer(javaLineNumber), new StreamItLineNumber(strLineNumber.intValue(), strFiles[i]));
					break;
				}
			}
		} catch (CoreException ce) {
		}	
		data.setStrJava(strToJavaBreakpoints, strToJavaWatchpoints, javaToStrLineNumbers);		
	}
	
	private boolean isWatchpoint(IDocument doc, int validJavaLineNumber, IEditorInput input) {
		try {
			IRegion line = doc.getLineInformation(validJavaLineNumber - 1);
			IType type = StreamItDebugEventFilter.getInstance().getType(input, line);
			if (type == null) return false;
			IJavaProject javaProject = type.getJavaProject();
				
			if (!type.exists() || javaProject == null || !javaProject.isOnClasspath(type)) return false;
			int lineStart = line.getOffset();
			int lineEnd = lineStart + line.getLength();
			
			// find field					
			IField[] fields = type.getFields();
			IField field = null;
			IRegion fieldRegion = null;
			int fieldStart = -1;
			int fieldEnd = -1;
			int checkStart, checkEnd;
			for (int i = 0; i < fields.length && field == null; i++) {
				fieldStart = fields[i].getSourceRange().getOffset();
				fieldRegion = doc.getLineInformationOfOffset(fieldStart);
				checkStart = fieldRegion.getOffset();
				checkEnd = checkStart + fieldRegion.getLength();
				// check if field contained in line
				if (lineStart >= checkStart && checkEnd <= lineEnd) {
					field = fields[i];
					fieldStart = field.getNameRange().getOffset();
					fieldEnd = fieldStart + field.getNameRange().getLength();
				}
			}
				
			if (field == null) return false;
			return true;
		} catch (JavaModelException e) {
		} catch (BadLocationException e) {
		} catch (CoreException e) {
		}
		return false;
	}
}
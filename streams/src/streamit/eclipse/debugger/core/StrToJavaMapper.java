package streamit.eclipse.debugger.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointLocationVerifier;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;
import streamit.eclipse.debugger.launching.StreamItApplicationLaunchShortcut;
import streamit.eclipse.debugger.launching.StreamItLocalApplicationLaunchConfigurationDelegate;

/**
 * @author kkuo
 */
public class StrToJavaMapper {

	private static StrToJavaMapper fInstance = new StrToJavaMapper();
	private static StreamItApplicationLaunchShortcut launchShortcut = new StreamItApplicationLaunchShortcut();
	private static StreamItLocalApplicationLaunchConfigurationDelegate launchDelegate = new StreamItLocalApplicationLaunchConfigurationDelegate();

	// key = IResource strFile, entry = BreakpointRulerData
	private static HashMap fStrFiles = new HashMap();

	private StrToJavaMapper() {
	}
	public static StrToJavaMapper getInstance() {
		return fInstance;
	}
	
	public BreakpointRulerData loadStrFile(IResource strFile, ITextEditor editor) {

		BreakpointRulerData data = (BreakpointRulerData) fStrFiles.get(strFile);
		if (data != null) return data;
		
		String absoluteStrFileName = strFile.getLocation().toOSString(); 
		String strName = strFile.getName();
		strName = strName.substring(0, strName.lastIndexOf('.' + IStreamItLaunchingConstants.STR_FILE_EXTENSION));
		String javaFileName = strName + ".java";
		IProject project = strFile.getProject();
		IFile javaFile = project.getFile(javaFileName);
		IWorkbenchPage page = StreamItViewsManager.getActivePage();
		
		try {
			if (!javaFile.exists() && project.hasNature(JavaCore.NATURE_ID)) {
				ILaunchConfiguration configuration = launchShortcut.findLaunchConfiguration(project.getName(), strName, ILaunchManager.RUN_MODE);
				launchDelegate.launchJava(configuration);

				// handle a secondary file
				String mainClassName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
				if (!mainClassName.equals(strName)) javaFileName = mainClassName + ".java";
				javaFile = project.getFile(javaFileName);
				javaFile.setReadOnly(true);
			}
			IEditorPart javaEditorPart = page.openEditor(javaFile, JavaUI.ID_CU_EDITOR);
			data = new BreakpointRulerData(javaFile, javaEditorPart);
			IDocument doc = JavaUI.getDocumentProvider().getDocument(javaEditorPart.getEditorInput());
			mapStrToJava(absoluteStrFileName, javaFile, doc, data);
		} catch (Exception e) {
		}
		
		page.addPartListener(StreamItViewsManager.getInstance());
		fStrFiles.put(strFile, data);
		return data;
	}
	
	private void mapStrToJava(String absoluteStrFileName, IFile javaFile, IDocument javaDocument, BreakpointRulerData data) {
		// strToJavaMaps:  key = Integer, entry = Integer; str to java line numbers; only valid java line breakpoints
		HashMap strToJavaBreakpoints = new HashMap();
		HashMap strToJavaWatchpoints = new HashMap();
		try {
			
			BreakpointLocationVerifier bv = new BreakpointLocationVerifier();
			int validJavaLineNumber;
		
			// look for commented mappings
			InputStream is = javaFile.getContents();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String strLine = null;
			int javaLineNumber = 0;
			String token = "// " + absoluteStrFileName + ':';
		
			while (true) {
				javaLineNumber++;
				strLine = br.readLine();
				if (strLine == null) break;
				if (strLine.indexOf(token) != -1) {
					Integer strLineNumber = Integer.valueOf(strLine.substring(strLine.indexOf(token) + token.length()));
					Object isThere = strToJavaBreakpoints.get(strLineNumber);			
					if (isThere == null) {
						validJavaLineNumber = bv.getValidBreakpointLocation(javaDocument, javaLineNumber - 1);
						if (javaLineNumber == validJavaLineNumber) {
							strToJavaBreakpoints.put(strLineNumber, new Integer(validJavaLineNumber));
						} else if (validJavaLineNumber != -1) {
							strToJavaWatchpoints.put(strLineNumber, new Integer(javaLineNumber));
						}
					}
				}
			}
			
			is.close();
		} catch (CoreException ce) {
		} catch (IOException ce) {
		}
		
		data.setStrToJava(strToJavaBreakpoints, strToJavaWatchpoints);
		
	}
}

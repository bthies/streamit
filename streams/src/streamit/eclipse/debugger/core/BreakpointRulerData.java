package streamit.eclipse.debugger.core;

import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;

/**
 * @author kkuo
 */
public class BreakpointRulerData {
	
	IFile fJavaFile;
	IEditorPart fJavaEditorPart;
	
	// key = Integer, entry = Integer
	// str to java line numbers
	// only valid java line numbers for breakpoints
	// str line number starts on 1
	// java line number starts on 1
	private HashMap fStrToJavaBreakpoints;
	private HashMap fStrToJavaWatchpoints;

	public BreakpointRulerData(IFile javaFile, IEditorPart javaEditorPart) {
		fJavaFile = javaFile;
		fJavaEditorPart = javaEditorPart;
	}

	public void setStrToJava(HashMap strToJavaBreakpoint, HashMap strToJavaWatchpoints) {
		fStrToJavaBreakpoints = strToJavaBreakpoint;
		fStrToJavaWatchpoints = strToJavaWatchpoints;
		
	}

	// only returns valid java line numbers for breakpoints
	// or -1
	public int getJavaBreakpointLineNumber(int strLineNumber) {
		Integer toReturn = (Integer) fStrToJavaBreakpoints.get(new Integer(strLineNumber));
		if (toReturn == null) return -1;
		return toReturn.intValue();
	}
	
	// only returns valid java line numbers for watchpoints
	// or -1
	public int getJavaWatchpoinLineNumber(int strLineNumber) {
		Integer toReturn = (Integer) fStrToJavaWatchpoints.get(new Integer(strLineNumber));
		if (toReturn == null) return -1;
		return toReturn.intValue();
	}
	
	public IEditorPart getJavaEditorPart() {
		return fJavaEditorPart;
	}

	public IFile getJavaFile() {
		return fJavaFile;
	}

}

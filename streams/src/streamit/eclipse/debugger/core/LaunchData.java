package streamit.eclipse.debugger.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;

/**
 * @author kkuo
 */
public class LaunchData {
	
	// key = Integer javaLineNumber, entry = StreamItLineNumber
	private HashMap fJavaToStrLineNumbers;

	// key = filter name (without id), entry = String[] fields
	private HashMap fFilterNameToStateVariables;
	
	// key = IVariable datum, entry = HashMap attributes
	private static HashMap fDataAttributes = new HashMap();

	// for setting input
	private IJavaMethodBreakpoint fInitBreakpoint;
	
	// 1 based
	private int fPreviousLineNumber;

	protected LaunchData() {
		fJavaToStrLineNumbers = new HashMap();
		fFilterNameToStateVariables = new HashMap();
		fPreviousLineNumber = -1;
	}
	
	protected void mapJavaToStr(IFile javaFile) throws CoreException, IOException {
		fJavaToStrLineNumbers.clear();

		// look for commented mappings
		InputStream inputStream = javaFile.getContents();
		InputStreamReader isr = new InputStreamReader(inputStream);
		BufferedReader br = new BufferedReader(isr);
		String strLine = null;
		int javaLineNumber = 0;
		int index;
		while (true) {
			javaLineNumber++;
			strLine = br.readLine();
			if (strLine == null) {
				inputStream.close();
				return;
			} 
			index = strLine.lastIndexOf(IPath.DEVICE_SEPARATOR);
			if (index != -1) {
				String strFileName = strLine.substring(strLine.lastIndexOf(File.separatorChar) + 1, index); 
				fJavaToStrLineNumbers.put(new Integer(javaLineNumber), 
										new StreamItLineNumber(Integer.valueOf(strLine.substring(index + 1)).intValue(), strFileName));
			} 	
		}
	}

	protected int getLineNumber(IFile javaFile, Integer javaLineNumber) {
		StreamItLineNumber strLineNumber = (StreamItLineNumber) fJavaToStrLineNumbers.get(javaLineNumber);
		if (strLineNumber == null) return -1;
		return strLineNumber.getLineNumber();
	}
	
	protected void addFilterVariables(String filterName, String[] fields) {
		fFilterNameToStateVariables.put(filterName, fields);		
	}
	
	protected String[] getFilterVariables(String filterName) {
		return (String[]) fFilterNameToStateVariables.get(filterName);
	}
	
	protected static void addAttributes(IVariable dataVar, HashMap attributes) {
		fDataAttributes.put(dataVar, attributes);	
	}

	protected static HashMap getAttributes(IVariable dataVar) {
		return (HashMap) fDataAttributes.get(dataVar);	
	}
	
	protected static boolean isHighlighted(IVariable dataVar) {
		HashMap h = getAttributes(dataVar);
		if (h == null) return false;
		return h.containsKey(IStreamItDebuggerConstants.ATTR_HIGHLIGHT);
	}
	
	public static void changeHighlightAttribute(IVariable datumVar, boolean highlight) {
		HashMap h = getAttributes(datumVar);
		if (h == null) h = new HashMap();
		if (highlight) h.put(IStreamItDebuggerConstants.ATTR_HIGHLIGHT, null);
		else h.remove(IStreamItDebuggerConstants.ATTR_HIGHLIGHT);
	}

	protected void setInitBreakpoint(IJavaMethodBreakpoint breakpoint) throws CoreException {
		fInitBreakpoint = breakpoint;
	}
	
	protected boolean isInitBreakpoint(IBreakpoint b) {
		if (fInitBreakpoint == null) return false;
		return fInitBreakpoint.equals(b);
	}

	public int getPreviousLineNumber() {
		return fPreviousLineNumber;
	}

	public void setPreviousLineNumber(int i) {
		fPreviousLineNumber = i;
	}

}

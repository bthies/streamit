package streamit.eclipse.debugger.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

/**
 * @author kkuo
 */
public class StrJavaData {
	
	// Str To Java
	private IFile fJavaFile;
	private IFileEditorInput fJavaFileEditorInput;
	private IDocument fJavaDocument;
	private AbstractMarkerAnnotationModel fJavaResourceMarkerAnnotationModel;
	
	// key = IFile strFile, entry = HashMap (key = Integer, entry = Integer, str to java line numbers)
	// only valid java line numbers for breakpoints
	// str line number starts on 1
	// java line number starts on 1
	private HashMap fStrToJavaBreakpoints;
	private HashMap fStrToJavaWatchpoints;

	// Java To Str
	// key = Integer javaLineNumber, entry = StreamItLineNumber
	private HashMap fJavaToStrLineNumbers; 

	public StrJavaData(IFile javaFile, boolean dummy) throws CoreException {
		// set IFile
		fJavaFile = javaFile;
		
		// set IFileEditorInput
		fJavaFileEditorInput = new FileEditorInput(fJavaFile);

		// set IDocument
		if (dummy) {
			fJavaDocument = new Document(""); 
		} else {
			InputStream is = fJavaFile.getContents(true);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String javaLine = null;
			StringBuffer sb = new StringBuffer(IStreamItCoreConstants.EMPTY_STRING);
			try {
				while (true) {
					javaLine = br.readLine();
					if (javaLine == null) break;
					sb.append(javaLine + IStreamItCoreConstants.NEWLINE_STRING);
				}
				fJavaDocument = new Document(sb.toString());
				is.close();
			} catch (IOException ioe) {
			}
		}

		// set AbstractMarkerAnnotationModel
		fJavaResourceMarkerAnnotationModel = new ResourceMarkerAnnotationModel(fJavaFile);
		fJavaResourceMarkerAnnotationModel.connect(fJavaDocument);
	}

	public void setStrJava(HashMap strToJavaBreakpoint, HashMap strToJavaWatchpoints, HashMap javaToStrLineNumbers) {
		fStrToJavaBreakpoints = strToJavaBreakpoint;
		fStrToJavaWatchpoints = strToJavaWatchpoints;
		fJavaToStrLineNumbers = javaToStrLineNumbers;
	}

	// only returns valid java line numbers for breakpoints
	// or -1
	public int getJavaBreakpointLineNumber(IFile strFile, int strLineNumber) {
		HashMap temp = (HashMap) fStrToJavaBreakpoints.get(strFile);
		if (temp == null) return -1;
		Integer toReturn = (Integer) temp.get(new Integer(strLineNumber));
		if (toReturn == null) return -1;
		return toReturn.intValue();
	}
	
	// only returns valid java line numbers for watchpoints
	// or -1
	public int getJavaWatchpointLineNumber(IFile strFile, int strLineNumber) {
		HashMap temp = (HashMap) fStrToJavaWatchpoints.get(strFile);
		if (temp == null) return -1;
		Integer toReturn = (Integer) temp.get(new Integer(strLineNumber));
		if (toReturn == null) return -1;
		return toReturn.intValue();
	}
	
	public StreamItLineNumber getStreamItLineNumber(int javaLineNumber) {
		return (StreamItLineNumber) fJavaToStrLineNumbers.get(new Integer(javaLineNumber));
	}

	public int getStrLineNumber(int javaLineNumber) {
		// get mapping if already mapped
		StreamItLineNumber line = getStreamItLineNumber(javaLineNumber);
		if (line == null) return -1; 
		return line.getLineNumber();
	}

	public IFile getJavaFile() {
		return fJavaFile;
	}
	
	public IFileEditorInput getJavaFileEditorInput() {
		return fJavaFileEditorInput;
	}
	
	public IDocument getJavaDocument() {
		return fJavaDocument;
	}
	
	public AbstractMarkerAnnotationModel getJavaResourceMarkerAnnotationModel() {
		return fJavaResourceMarkerAnnotationModel;
	}
}

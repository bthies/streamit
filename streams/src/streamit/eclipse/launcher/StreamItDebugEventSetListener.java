/*******************************************************************************
 * StreamIt Debugger
 * @author kkuo
 *******************************************************************************/
package launcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class StreamItDebugEventSetListener implements IDebugEventSetListener {
	
	// map java resources to map str line numbers to java line numbers
	protected static Map fJavaFiles;

	public void handleDebugEvents(DebugEvent[] events) {
		for (int i = 0; i < events.length; i++) {
			if (events[i].getKind() == DebugEvent.SUSPEND) {
				int detail = events[i].getDetail();
				if (detail == DebugEvent.BREAKPOINT || detail == DebugEvent.STEP_END) handleSuspend(events[i]);
			}
		}
	}
	
	// redirect from .java to .str
	protected void handleSuspend(DebugEvent event) {
		// only handle if IThread
		Object o = event.getSource();
		if (!(o instanceof IThread)) return;

		try {
 			// only handle if StreamIt Launch
			IThread thread = (IThread) o;
			ILaunch launch = thread.getLaunch();
			if (!launch.getLaunchConfiguration().getName().equals(StreamItLauncherPlugin.ID_STR_APPLICATION)) return;

			// get cause of launch
			IStackFrame top = thread.getTopStackFrame();
			if (top == null) return;
			o = launch.getSourceLocator().getSourceElement(top);
			
			// only handle if from .java
			if (!(o instanceof ICompilationUnit)) return;
			IFile javaFile = (IFile) ((ICompilationUnit) o).getResource();
			
			// get mapping if already mapped
			Map javaToStr = null;
			if (fJavaFiles != null) {
				if (fJavaFiles.containsKey(javaFile)) javaToStr = (Map) fJavaFiles.get(javaFile);
			} else {
				fJavaFiles = new HashMap();
			}
			
			// map if not already mapped
			String javaFileName = javaFile.getName();
			String strFileName = javaFileName.substring(0, javaFileName.lastIndexOf(".java")) + '.' + StreamItLauncherPlugin.STR_FILE_EXTENSION;
			if (javaToStr == null) {
				System.out.println("mapping");
				javaToStr = mapJavaToStr(javaFile, strFileName);
				fJavaFiles.put(javaFile, javaToStr);										
			}
																										
			// highlight in .str
			DebugUIPlugin.getStandardDisplay().asyncExec(getHighlighter(javaFile.getProject().getFile(strFileName), 
														((Integer) javaToStr.get(new Integer(top.getLineNumber()))).intValue() - 1));
		} catch (Exception e) {
		}
	}

	protected static Map mapJavaToStr(IFile javaFile, String fStrFileName) {
		Map javaToStr = new HashMap();
		
		try {
			// look for commented mappings
			InputStreamReader isr = new InputStreamReader(javaFile.getContents());
			BufferedReader br = new BufferedReader(isr);
			String strLine = null;
			int javaLineNumber = 0;
			String token = "// " + fStrFileName + ':';
			
			while (true) {
				javaLineNumber++;
				strLine = br.readLine();
				if (strLine == null) return javaToStr;
				if (strLine.indexOf(token) != -1) javaToStr.put(new Integer(javaLineNumber), Integer.valueOf(strLine.substring(strLine.indexOf(token) + token.length()))); 	
			}			
		} catch (Exception e) {
		}
		return javaToStr;
	}
	
	protected Runnable getHighlighter(final IFile strFile, final int lineNumber) {
		return new Runnable() {									
			public void run() {
				try {
					// open .str if not already open
					ITextEditor fTextEditor = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(strFile).getAdapter(ITextEditor.class);											
					if (fTextEditor == null) return;

					// goto marker			
					Iterator i = fTextEditor.getDocumentProvider().getAnnotationModel(fTextEditor.getEditorInput()).getAnnotationIterator();
					while (i.hasNext()) {
						Object o = i.next();
						if (o instanceof MarkerAnnotation) {
							IMarker m = ((MarkerAnnotation) o).getMarker();
							o = m.getAttribute(IMarker.LINE_NUMBER);														
							if (o != null && ((Integer) o).intValue() == lineNumber + 1) {
								fTextEditor.gotoMarker(m);
								return;
							}															
						}													
					}
					
					// highlight text just in case search for marker fails
					IDocument doc = fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());
					IRegion strLine = doc.getLineInformation(lineNumber);
					fTextEditor.getEditorSite().getSelectionProvider().setSelection(new TextSelection(doc, strLine.getOffset(), strLine.getLength()));
				} catch (Exception e) {
				}
			}
		};
	}

	public static void addJavaToStrMap(IFile javaFile, Map javaToStr) {
		if (fJavaFiles == null) fJavaFiles = new HashMap();
		fJavaFiles.put(javaFile, javaToStr);
	}
	
	public static Map getJavaToStrMap(IFile javaFile) {
		if (fJavaFiles == null) return null;
		return (Map) fJavaFiles.get(javaFile);
	}
}
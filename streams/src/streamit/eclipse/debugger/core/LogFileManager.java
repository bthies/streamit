package streamit.eclipse.debugger.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.grapheditor.TestSwingEditorPlugin;
import streamit.eclipse.debugger.texteditor.IStreamItEditorConstants;

/**
 * @author kkuo
 */
public class LogFileManager implements IResourceChangeListener {

	private static LogFileManager fInstance = new LogFileManager();
	private static IFile fLogFile = null;
	private static int fLineNumber;

	private LogFileManager() {
		super();		
	}
	
	public static LogFileManager getInstance() {
		return fInstance;
	}

	public void setLogFile(IFile strFile) {
		if (fLogFile != null) return;
		try {
			fLogFile = strFile.getProject().getFile(".streamit");
			File f = new File(fLogFile.getLocation().toOSString());
			f.delete();
			f.createNewFile();
			if (fLogFile.exists()) {
				fLogFile.setContents(new FileInputStream(f), true, false, null);
			} else {
				fLogFile.create(new FileInputStream(f), true, null);
			}
			fLineNumber = 1; 
			TestSwingEditorPlugin.getInstance().setLogFile(fLogFile);
		} catch (IOException e) {
			fLogFile = null;
		} catch (CoreException ce) {
			fLogFile = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		if (fLogFile == null) return;
		if (event.getDelta().getKind() != IResourceDelta.CHANGED) return;
		IResourceDelta delta = event.getDelta().findMember(fLogFile.getFullPath());
		if (delta == null) return;
		if (delta.getKind() != IResourceDelta.CHANGED) return;
		if ((delta.getFlags() & IResourceDelta.CONTENT) == 0) return;
			 
		try {
			InputStream is = fLogFile.getContents();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String strLine = null;
			int curLineNumber = 0;
			StringTokenizer st;
			while (true) {
				strLine = br.readLine();
				if (strLine == null) break;
				curLineNumber++;
				if (curLineNumber < fLineNumber) continue;
				
				fLineNumber++;
				st = new StringTokenizer(strLine);
				if (st.countTokens() != 2) continue;
				DebugUIPlugin.getStandardDisplay().syncExec(doHighlight(st.nextToken(), st.nextToken()));
				// IFile.getFullPath().toString(), like /HelloWorld6/HelloWorld6.str
				// type name, like IntSource 
				// /HelloWorld6/HelloWorld6.str IntSource
			}
			is.close();
		} catch (CoreException ce) {
		} catch (IOException ce) {
		}
	}
	
	private Runnable doHighlight(final String strFileName, final String typeName) {
		return new Runnable() {	
			public void run() {
				try {
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IWorkbenchPage page = StreamItViewsManager.getActivePage();
					ITextEditor strEditor = (ITextEditor) page.openEditor((IFile) root.findMember(strFileName), IStreamItEditorConstants.ID_STREAMIT_EDITOR);
					IDocument doc = strEditor.getDocumentProvider().getDocument(strEditor.getEditorInput());
					int lines = doc.getNumberOfLines();
					String text;
					IRegion line;
					for (int j = 0; j < lines; j++) {
						line = doc.getLineInformation(j);
						text = doc.get(line.getOffset(), line.getLength());
						if (text.indexOf(typeName) != -1) {
							strEditor.selectAndReveal(line.getOffset(), line.getLength());
							return;		
						}
					}
				} catch (PartInitException pie) {
				} catch (BadLocationException jme) {
				}
			}
		};
	}
}
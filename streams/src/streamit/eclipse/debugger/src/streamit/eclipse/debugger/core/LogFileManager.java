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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.grapheditor.TestSwingEditorPlugin;
import streamit.eclipse.debugger.texteditor.IStreamItEditorConstants;
import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class LogFileManager implements IResourceChangeListener {

	private static LogFileManager fInstance = new LogFileManager();
	private IFile fLogFile;
	private int fLineNumber;

	private LogFileManager() {
		super();
		fLogFile = null;
		fLineNumber = -1;
	}
	
	public static LogFileManager getInstance() {
		return fInstance;
	}

	public void setLogFile(IFile strFile) {
		if (fLogFile != null) return;
		try {
			fLogFile = strFile.getProject().getFile(IStreamItCoreConstants.LOGFILE);
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
					ITextEditor strEditor = (ITextEditor) StreamItViewsManager.getActivePage().openEditor((IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(strFileName), IStreamItEditorConstants.ID_STREAMIT_EDITOR);
					IEditorInput strEditorInput = strEditor.getEditorInput();
					IFile strFile = ((IFileEditorInput) strEditorInput).getFile();
					IWorkingCopyManager manager = JavaUI.getWorkingCopyManager();
					StrJavaData data = StrJavaMapper.getInstance().loadStrFile(strFile, false);
					IEditorInput javaEditorInput = data.getJavaFileEditorInput();
					manager.connect(javaEditorInput);
					ICompilationUnit unit = manager.getWorkingCopy(javaEditorInput);
					IType[] types = unit.getTypes();
					for (int i = 0; i < types.length; i++) {
						if (!types[i].getElementName().equals(typeName)) continue;

						ISourceRange range = types[i].getSourceRange();
						IDocument javaDocument = data.getJavaDocument();
						int strLineNumber = data.getStreamItLineNumber(javaDocument.getLineOfOffset(range.getOffset()) + 1).getLineNumber();
						IRegion region = strEditor.getDocumentProvider().getDocument(strEditorInput).getLineInformation(strLineNumber - 1);
						strEditor.selectAndReveal(region.getOffset(), region.getLength());
						break;
					}
				} catch (BadLocationException ble) {
				} catch (PartInitException pie) {
				} catch (CoreException ce) {
				}
			}
		};
	}
}
/*******************************************************************************
 * StreamIt Editor adapted from Example Java Editor
 * modifier - Kimberly Kuo
 *******************************************************************************/

package streamit.eclipse.debugger.texteditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import streamit.eclipse.debugger.core.BreakpointRulerData;
import streamit.eclipse.debugger.core.StrToJavaMapper;
import streamit.eclipse.debugger.grapheditor.TestSwingEditorPlugin;

/**
 * StreamIt specific text editor.
 */
public class StreamItEditor extends TextEditor {

    /** The outline page */
    private StreamItContentOutlinePage fOutlinePage;
    
    /**
     * Default constructor.
     */
    public StreamItEditor() {
		super();
    }
    
    /** The <code>StreamItEditor</code> implementation of this 
     * <code>AbstractTextEditor</code> method performs any extra 
     * disposal actions required by the StreamIt editor.
     */
    public void dispose() {
		if (fOutlinePage != null)
		    fOutlinePage.setInput(null);
		super.dispose();
    }
    
    /** The <code>StreamItEditor</code> implementation of this 
     * <code>AbstractTextEditor</code> method performs any extra 
     * revert behavior required by the StreamIt editor.
     */
    public void doRevertToSaved() {
		super.doRevertToSaved();
		if (fOutlinePage != null)
		    fOutlinePage.update();
    }
	
    /** The <code>StreamItEditor</code> implementation of this 
     * <code>AbstractTextEditor</code> method performs any extra 
     * save behavior required by the StreamIt editor.
     */
    public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);
		if (fOutlinePage != null)
		    fOutlinePage.update();
    }
    
    /** The <code>StreamItEditor</code> implementation of this 
     * <code>AbstractTextEditor</code> method performs any extra 
     * save as behavior required by the StreamIt editor.
     */
    public void doSaveAs() {
		super.doSaveAs();
		if (fOutlinePage != null)
		    fOutlinePage.update();
    }
    
    /** The <code>StreamItEditor</code> implementation of this 
     * <code>AbstractTextEditor</code> method performs sets the 
     * input of the outline page after AbstractTextEditor has set input.
     */ 
    public void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);
		if (fOutlinePage != null)
		    fOutlinePage.setInput(input);
    }
   
    /** The <code>StreamItEditor</code> implementation of this 
     * <code>AbstractTextEditor</code> method performs gets
     * the StreamIt content outline page if request is for a an 
     * outline page.
     */ 
    public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
		    if (fOutlinePage == null) {
			fOutlinePage = new 
			    StreamItContentOutlinePage(getDocumentProvider(), this);
			if (getEditorInput() != null)
			    fOutlinePage.setInput(getEditorInput());
		    }
		    return fOutlinePage;
		}
		return super.getAdapter(required);
    }
		
    /* (non-StreamItdoc)
     * Method declared on AbstractTextEditor
     */
    protected void initializeEditor() {
		super.initializeEditor();
	
		setSourceViewerConfiguration(new StreamItEditorSourceViewerConfiguration(this));
		setRulerContextMenuId("#StreamItRulerContext"); //$NON-NLS-1$
		
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#handleCursorPositionChanged()
	 */
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		
		IFile strFile = ((IFileEditorInput) getEditorInput()).getFile();
		BreakpointRulerData data = StrToJavaMapper.getInstance().loadStrFile(strFile, false);
		int strLineNumber = Integer.parseInt(getCursorPosition().substring(0, getCursorPosition().indexOf(" ")));
		int validJavaLineNumber = data.getJavaBreakpointLineNumber(strLineNumber);
		if (validJavaLineNumber < 0) return;
		IDocument document = JavaUI.getDocumentProvider().getDocument(data.getJavaEditorPart().getEditorInput());
		try {
			IRegion line = document.getLineInformation(validJavaLineNumber - 1);
			IType type = getType(data.getJavaEditorPart().getEditorInput(), line);
			if (type != null) {
				TestSwingEditorPlugin.getInstance().highlightNodeInGraph(strFile, type.getFullyQualifiedName());
				return;
			}
				
			validJavaLineNumber = data.getJavaWatchpoinLineNumber(strLineNumber);
			line = document.getLineInformation(validJavaLineNumber - 1);
			type = getType(data.getJavaEditorPart().getEditorInput(), line);
			if (type != null) {
				TestSwingEditorPlugin.getInstance().highlightNodeInGraph(strFile, type.getFullyQualifiedName());
				return;
			}
				
		} catch (JavaModelException jme) {
		} catch (BadLocationException ble) {
		}
	}
	
	protected static IType getType(IEditorInput editorInput, IRegion line) throws JavaModelException {
		if (editorInput instanceof IFileEditorInput) {
			IWorkingCopyManager manager= JavaUI.getWorkingCopyManager();
			ICompilationUnit unit= manager.getWorkingCopy(editorInput);
						
			if (unit != null) {
				synchronized (unit) {
					unit.reconcile();
				}
				IJavaElement e = unit.getElementAt(line.getOffset());
				if (e instanceof IType) {
					return (IType)e;
				} else if (e instanceof IMember) {
					return ((IMember)e).getDeclaringType();
				}
			}
		}
		return null;
	}
}
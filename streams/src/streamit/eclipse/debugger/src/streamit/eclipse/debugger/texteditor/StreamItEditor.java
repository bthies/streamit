package streamit.eclipse.debugger.texteditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import streamit.eclipse.debugger.core.StrJavaData;
import streamit.eclipse.debugger.core.StrJavaMapper;
import streamit.eclipse.debugger.core.StreamItDebugEventFilter;
import streamit.eclipse.debugger.grapheditor.TestSwingEditorPlugin;

/**
 * StreamIt specific text editor.
 * 
 * @author kkuo
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
		setRulerContextMenuId(IStreamItEditorConstants.STREAMIT_RULER_CONTEXT); //$NON-NLS-1$
		
    }
    
    /* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#handleCursorPositionChanged()
	 */
	protected void handleCursorPositionChanged() {
		super.handleCursorPositionChanged();
		
		IFile strFile = ((IFileEditorInput) getEditorInput()).getFile();
		StrJavaData data = StrJavaMapper.getInstance().loadStrFile(strFile, false);
		int strLineNumber = Integer.parseInt(getCursorPosition().substring(0, getCursorPosition().indexOf(IStreamItEditorConstants.WHITESPACE_STRING)));
		if (data == null) return;
		int validJavaLineNumber = data.getJavaBreakpointLineNumber(strFile, strLineNumber);
		if (validJavaLineNumber < 0) return;
		IDocument document = data.getJavaDocument();
		try {
			IRegion line = document.getLineInformation(validJavaLineNumber - 1);
			IType type = StreamItDebugEventFilter.getInstance().getType(data.getJavaFileEditorInput(), line);
			if (type != null) {
				TestSwingEditorPlugin.getInstance().highlightNodeInGraph(strFile, type.getFullyQualifiedName());
				return;
			}
				
			validJavaLineNumber = data.getJavaWatchpointLineNumber(strFile, strLineNumber);
			line = document.getLineInformation(validJavaLineNumber - 1);
			type = StreamItDebugEventFilter.getInstance().getType(data.getJavaFileEditorInput(), line);
			if (type != null) {
				TestSwingEditorPlugin.getInstance().highlightNodeInGraph(strFile, type.getFullyQualifiedName());
				return;
			}
				
		} catch (JavaModelException jme) {
		} catch (BadLocationException ble) {
		} catch (CoreException ce) {
		}
	}
}
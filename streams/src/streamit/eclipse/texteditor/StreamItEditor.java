/*******************************************************************************
 * StreamIt Editor adapted from Example Java Editor
 * modifier - Kimberly Kuo
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package texteditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;

import org.eclipse.jface.text.source.ISourceViewer;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

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
     * <code>AbstractTextEditor</code> method extend the 
     * actions to add those specific to the receiver
     */
    protected void createActions() {
	super.createActions();
	
	IAction a = 
	    new TextOperationAction(
				    StreamItEditorMessages.getResourceBundle(),
				    "ContentAssistProposal.", 
				    this, 
				    ISourceViewer.CONTENTASSIST_PROPOSALS); 
	                            //$NON-NLS-1$
	a.setActionDefinitionId(ITextEditorActionDefinitionIds.
				CONTENT_ASSIST_PROPOSALS);
	setAction("ContentAssistProposal", a); //$NON-NLS-1$
	
	a = 
	    new TextOperationAction(StreamItEditorMessages.getResourceBundle(),
				    "ContentAssistTip.", this, 
				    ISourceViewer.
				    CONTENTASSIST_CONTEXT_INFORMATION);
	                            //$NON-NLS-1$
	a.setActionDefinitionId(ITextEditorActionDefinitionIds.
				CONTENT_ASSIST_CONTEXT_INFORMATION);
	setAction("ContentAssistTip", a); //$NON-NLS-1$
    }
	
    /** The <code>StreamItEditor</code> implementation of this 
     * <code>AbstractTextEditor</code> method performs any extra 
     * disposal actions required by the StreamIt editor.
     */
    public void dispose() {
	StreamItEditorEnvironment.disconnect(this);
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
     * <code>AbstractTextEditor</code> method adds any 
     * JavaEditor specific entries.
     */ 
    public void editorContextMenuAboutToShow(MenuManager menu) {
	super.editorContextMenuAboutToShow(menu);
	addAction(menu, "ContentAssistProposal"); //$NON-NLS-1$
	addAction(menu, "ContentAssistTip"); //$NON-NLS-1$
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
	StreamItEditorEnvironment.connect(this);
	setSourceViewerConfiguration(new StreamItSourceViewerConfiguration());
	setEditorContextMenuId("#JavaEditorContext"); //$NON-NLS-1$
	setRulerContextMenuId("#JavaRulerContext"); //$NON-NLS-1$
    }
}
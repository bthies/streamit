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
package streamit.eclipse.debugger.texteditor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
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
	public IVerticalRulerInfo getA() {
		return getVerticalRuler();
	}
}
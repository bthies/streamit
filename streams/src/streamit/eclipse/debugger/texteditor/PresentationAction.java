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

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A toolbar action which toggles the presentation model of the
 * connected text editor. The editor shows either the highlight range
 * only or always the whole document.
 */
public class PresentationAction extends TextEditorAction {

    /**
     * Constructs and updates the action.
     */
    public PresentationAction() {
	super(StreamItEditorMessages.getResourceBundle(), "TogglePresentation.", 
	      null); //$NON-NLS-1$
	update();
    }
    
    /* (non-StreamItdoc)
     * Method declared on IAction
     */
    public void run() {

	ITextEditor editor = getTextEditor();
	
	editor.resetHighlightRange();
	boolean show = editor.showsHighlightRangeOnly();
	setChecked(!show);
	editor.showHighlightRangeOnly(!show);
    }
	
    /* (non-StreamItdoc)
     * Method declared on TextEditorAction
     */
    public void update() {
	setChecked(getTextEditor() != null 
		   && getTextEditor().showsHighlightRangeOnly());
	setEnabled(true);
    }
}

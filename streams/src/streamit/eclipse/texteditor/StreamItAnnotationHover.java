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

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;

/** 
 * The StreamItAnnotationHover provides the hover support for StreamIt editors.
 */
 
public class StreamItAnnotationHover implements IAnnotationHover {

    /* (non-StreamItdoc)
     * Method declared on IAnnotationHover
     */
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
	IDocument document = sourceViewer.getDocument();
	
	try {
	    IRegion info = document.getLineInformation(lineNumber);
	    return document.get(info.getOffset(), info.getLength());
	} catch (BadLocationException x) {
	}

	return null;
    }
}


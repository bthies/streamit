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
import org.eclipse.swt.graphics.Point;

/**
 * Example implementation for an <code>ITextHover</code> which hovers over 
 * StreamIt code.
 */
public class StreamItTextHover implements ITextHover {

    /* (non-StreamItdoc)
     * Method declared on ITextHover
     */
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
	if (hoverRegion != null) {
	    try {
		if (hoverRegion.getLength() > -1)
		    return textViewer.getDocument().get(hoverRegion.getOffset(),
							hoverRegion.getLength());
	    } catch (BadLocationException x) {
	    }
	}
	return StreamItEditorMessages.
	    getString("StreamItTextHover.emptySelection"); //$NON-NLS-1$
    }
    
    /* (non-StreamItdoc)
     * Method declared on ITextHover
     */
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
	Point selection = textViewer.getSelectedRange();
	if (selection.x <= offset && offset < selection.x + selection.y)
	    return new Region(selection.x, selection.y);
	return new Region(offset, 0);
    }
}

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
package texteditor.util;

import java.util.*;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Manager for colors used in the StreamIt editor
 */
public class StreamItColorProvider {

    public static final RGB MULTI_LINE_COMMENT = new RGB(128, 0, 0);
    public static final RGB SINGLE_LINE_COMMENT = new RGB(128, 128, 0);
    public static final RGB KEYWORD = new RGB(0, 0, 128);
    // kkuo - added color for StreamIt keywords, commonly used words
    public static final RGB STRKEYWORD = new RGB(128, 0, 128); //
    public static final RGB STRCOMMON = new RGB(128, 0, 128); //
    public static final RGB TYPE = new RGB(0, 0, 128);
    public static final RGB STRING = new RGB(0, 128, 0);
    public static final RGB DEFAULT = new RGB(0, 0, 0);
    public static final RGB STREAMITDOC_KEYWORD = new RGB(0, 128, 0);
    public static final RGB STREAMITDOC_TAG = new RGB(128, 128, 128);
    public static final RGB STREAMITDOC_LINK = new RGB(128, 128, 128);
    public static final RGB STREAMITDOC_DEFAULT = new RGB(0, 128, 128);

    protected Map fColorTable = new HashMap(10);
    
    /**
     * Release all of the color resources held onto by the receiver.
     */	
    public void dispose() {
	Iterator e = fColorTable.values().iterator();
	while (e.hasNext())
	    ((Color) e.next()).dispose();
    }
	
    /**
     * Return the Color that is stored in the Color table as rgb.
     */
    public Color getColor(RGB rgb) {
	Color color = (Color) fColorTable.get(rgb);
	if (color == null) {
	    color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
	}
	return color;
    }
}

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

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * A streamit aware white space detector.
 */
public class StreamItWhitespaceDetector implements IWhitespaceDetector {

    /* (non-StreamItdoc)
     * Method declared on IWhitespaceDetector
     */
    public boolean isWhitespace(char character) {
	return Character.isWhitespace(character);
    }
}

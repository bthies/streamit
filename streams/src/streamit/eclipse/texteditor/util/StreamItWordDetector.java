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

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * A StreamIt aware word detector.
 */
public class StreamItWordDetector implements IWordDetector {

    /* (non-StreamItdoc)
     * Method declared on IWordDetector.
     */
    public boolean isWordPart(char character) {
	return Character.isJavaIdentifierPart(character);
    }
    
    /* (non-StreamItdoc)
     * Method declared on IWordDetector.
     */
    public boolean isWordStart(char character) {
	return Character.isJavaIdentifierStart(character);
    }
}

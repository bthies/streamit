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

import org.eclipse.jface.text.rules.RuleBasedScanner;
import texteditor.streamit.StreamItCodeScanner;
import texteditor.streamitdoc.StreamItDocScanner;
import texteditor.util.StreamItColorProvider;

/** The StreamItEditorEnvironment maintains singletons used by the StreamIt 
 * editor examples.
 */
public class StreamItEditorEnvironment {

    private static StreamItColorProvider fgColorProvider;
    private static StreamItCodeScanner fgCodeScanner;
    private static StreamItDocScanner fgDocScanner;
    
    private static int fgRefCount = 0;
    
    /**
     * A connection has occured - initialize the receiver if it is the first 
     * activation.
     */
    public static void connect(Object client) {
	if (++fgRefCount == 1) {
	    fgColorProvider = new StreamItColorProvider();
	    fgCodeScanner = new StreamItCodeScanner(fgColorProvider);
	    fgDocScanner = new StreamItDocScanner(fgColorProvider);
	}
    }
    
    /**
     * A disconnection has occured - clear the receiver if it is the last 
     *  deactivation.
     */
    public static void disconnect(Object client) {
	if (--fgRefCount == 0) {
	    fgCodeScanner = null;
	    fgDocScanner = null;
	    fgColorProvider.dispose();
	    fgColorProvider = null;
	}
    }
	
    /**
     * Returns the singleton scanner.
     */
    public static RuleBasedScanner getStreamItCodeScanner() {
	return fgCodeScanner;
    }
    
    /**
     * Returns the singleton color provider.
     */
    public static StreamItColorProvider getStreamItColorProvider() {
	return fgColorProvider;
    }
	
    /**
     * Returns the singleton document scanner.
     */
    public static RuleBasedScanner getStreamItDocScanner() {
	return fgDocScanner;
    }
}

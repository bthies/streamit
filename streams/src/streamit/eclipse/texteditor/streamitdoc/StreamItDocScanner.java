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
package texteditor.streamitdoc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;

import texteditor.util.StreamItColorProvider;
import texteditor.util.StreamItWhitespaceDetector;

/**
 * A rule based StreamItDoc scanner.
 */
public class StreamItDocScanner extends RuleBasedScanner {

    /**
     * A key word detector.
     */
    static class StreamItDocWordDetector implements IWordDetector {
	
	/* (non-StreamItdoc)
	 * Method declared on IWordDetector
	 */
	public boolean isWordStart(char c) {
	    return (c == '@');
	}
	
	/* (non-StreamItdoc)
	 * Method declared on IWordDetector
	 */
	public boolean isWordPart(char c) {
	    return Character.isLetter(c);
	}
    };

    private static String[] fgKeywords = 
    { "@author", "@deprecated", "@exception", "@param", "@return", "@see", 
      "@serial", "@serialData", "@serialField", "@since", "@throws", "@version" 
    }; 
    //$NON-NLS-12$ //$NON-NLS-11$ //$NON-NLS-10$ //$NON-NLS-7$ //$NON-NLS-9$ 
    //$NON-NLS-8$ //$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ 
    //$NON-NLS-2$ //$NON-NLS-1$

    /**
     * Create a new streamit doc scanner.
     */
    public StreamItDocScanner(StreamItColorProvider provider) {
	super();
	
	IToken keyword = new 
	    Token(new TextAttribute(provider.
				    getColor(StreamItColorProvider.
					     STREAMITDOC_KEYWORD)));
	IToken tag = new 
	    Token(new TextAttribute(provider.
				    getColor(StreamItColorProvider.
					     STREAMITDOC_TAG)));
	IToken link = new 
	    Token(new TextAttribute(provider.
				    getColor(StreamItColorProvider.
					     STREAMITDOC_LINK)));
	
	List list = new ArrayList();
	
	// Add rule for tags.
	list.add(new SingleLineRule("<", ">", tag)); //$NON-NLS-2$ //$NON-NLS-1$
	
	// Add rule for links.
	list.add(new SingleLineRule("{", "}", link)); //$NON-NLS-2$ //$NON-NLS-1$
	
	// Add generic whitespace rule.
	list.add(new WhitespaceRule(new StreamItWhitespaceDetector()));
	
	// Add word rule for keywords.
	WordRule wordRule = new WordRule(new StreamItDocWordDetector());
	for (int i = 0; i < fgKeywords.length; i++)
	    wordRule.addWord(fgKeywords[i], keyword);
	list.add(wordRule);
	
	IRule[] result = new IRule[list.size()];
	list.toArray(result);
	setRules(result);
    }
}

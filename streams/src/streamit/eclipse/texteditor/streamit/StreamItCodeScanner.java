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
package texteditor.streamit;
import texteditor.IStreamItConstants;
import texteditor.StreamItPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import texteditor.util.*;

/**
 * A StreamIt code scanner.
 */
public class StreamItCodeScanner extends RuleBasedScanner {

	private static List fgKeywords;
	// kkuo - added StreamIt keywords
    private static List fgStrKeywords;
    // kkuo - added StreamIt's bit, complex,
    private static List fgTypes;
    private static List fgConstants;
    // kkuo - added StreamIt's commonly used words 
    private static List fgStrCommon;

    /**
     * Creates a StreamIt code scanner
     */
    public StreamItCodeScanner(StreamItColorProvider provider) {
	
		initializeCategory();
	
		IToken keyword = new 
		    Token(new TextAttribute(provider.
					    getColor(StreamItColorProvider.KEYWORD)));
		IToken strKeyword = new
		    Token(new TextAttribute(provider.
					    getColor(StreamItColorProvider.
						     STRKEYWORD)));
		IToken type = new 
		    Token(new TextAttribute(provider.
					    getColor(StreamItColorProvider.TYPE)));
		IToken string = new 
		    Token(new TextAttribute(provider.
					    getColor(StreamItColorProvider.STRING)));
		IToken comment = new 
		    Token(new TextAttribute(provider.
					    getColor(StreamItColorProvider.
						     SINGLE_LINE_COMMENT)));
		IToken strCommon = new 
		    Token(new TextAttribute(provider.
					    getColor(StreamItColorProvider.
						     STRCOMMON)));
		IToken other = new 
		    Token(new TextAttribute(provider.
					    getColor(StreamItColorProvider.DEFAULT)));
		
		List rules = new ArrayList();
		
		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", comment)); //$NON-NLS-1$
		
		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", string, '\\')); 
		//$NON-NLS-2$ //$NON-NLS-1$
		
		rules.add(new SingleLineRule("'", "'", string, '\\')); //$NON-NLS-2$ 
		//$NON-NLS-1$
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new StreamItWhitespaceDetector()));
		
		// Add word rule for keywords, types, and constants.
		WordRule wordRule = new WordRule(new StreamItWordDetector(), other);
		for (int i = 0; i < fgKeywords.size(); i++)
		    wordRule.addWord((String) fgKeywords.get(i), keyword);
		// kkuo - added word rules for StreamIt keywords
		for (int i = 0; i < fgStrKeywords.size(); i++)
		    wordRule.addWord((String) fgStrKeywords.get(i), strKeyword);
		for (int i = 0; i < fgTypes.size(); i++)
		    wordRule.addWord((String) fgTypes.get(i), type);
		for (int i = 0; i < fgConstants.size(); i++)
		    wordRule.addWord((String) fgConstants.get(i), type);
		// kkuo - added word rules for StreamIt's commonly used words
		for (int i = 0; i < fgStrCommon.size(); i++)
		    wordRule.addWord((String) fgStrCommon.get(i), strCommon);
		rules.add(wordRule);
		
		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
    }

	private void initializeCategory() {
		if (StreamItPlugin.getDefault() == null) {
			for (int i = 0; i < IStreamItConstants.FG_KEYWORD_DEFAULTS.length; 
				i++)
				fgKeywords.add(IStreamItConstants.FG_KEYWORD_DEFAULTS[i]);		
			for (int i = 0; 
				i < IStreamItConstants.FG_STR_KEYWORD_DEFAULTS.length; i++)
				fgStrKeywords.add(IStreamItConstants.
								FG_STR_KEYWORD_DEFAULTS[i]);			
			for (int i = 0; i < IStreamItConstants.FG_TYPE_DEFAULTS.length;
				i++)
				fgTypes.add(IStreamItConstants.FG_TYPE_DEFAULTS[i]);
			for (int i = 0; i < IStreamItConstants.FG_CONSTANT_DEFAULTS.length;
				i++)
				fgConstants.add(IStreamItConstants.FG_CONSTANT_DEFAULTS[i]);
			for (int i = 0; 
				i < IStreamItConstants.FG_STR_COMMON_DEFAULTS.length;
				i++)
				fgStrCommon.add(IStreamItConstants.FG_STR_COMMON_DEFAULTS[i]);
		} else {
			IPreferenceStore store 
				= StreamItPlugin.getDefault().getPreferenceStore();
			StringTokenizer strtok;
			String toUse, temp;
			String[] toIter;
			List toAdd;
			
			for (int i = 0; i < 5; i++) {
				switch (i) {
					case 0: 
						toUse = IStreamItConstants.PRE_KEYWORD;
						toIter = IStreamItConstants.FG_KEYWORD_DEFAULTS; 
						break;
					case 1:
						toUse = IStreamItConstants.PRE_STR_KEYWORD;
						toIter = IStreamItConstants.FG_STR_KEYWORD_DEFAULTS;
						break;
					case 2:
						toUse = IStreamItConstants.PRE_TYPE;
						toIter = IStreamItConstants.FG_TYPE_DEFAULTS; break;
					case 3: 
						toUse = IStreamItConstants.PRE_CONSTANT;
						toIter = IStreamItConstants.FG_CONSTANT_DEFAULTS; 
						break;
					default: 
						toUse = IStreamItConstants.PRE_STR_COMMON;
						toIter = IStreamItConstants.FG_STR_COMMON_DEFAULTS;
						break;
				}
				temp = store.getString(toUse);
				toAdd = new Vector();
				if (temp.equals(IPreferenceStore.STRING_DEFAULT_DEFAULT)) {
					for (int j = 0; j < toIter.length; j++)
						toAdd.add(toIter[j]);
				} else {
					strtok = new StringTokenizer(temp, "\n");				
					while (strtok.hasMoreTokens())
						toAdd.add(strtok.nextToken());
				}
				switch (i) {
					case 0: fgKeywords = toAdd; break;
					case 1: fgStrKeywords = toAdd; break;
					case 2: fgTypes = toAdd; break;
					case 3: fgConstants = toAdd; break;
					default: fgStrCommon = toAdd; break;
				}
			}
		}
	}
}
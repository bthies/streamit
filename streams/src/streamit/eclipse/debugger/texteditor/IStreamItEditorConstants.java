/*******************************************************************************
 * StreamIt Plugin adapted from Example Readme Tool
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

/**
 * This interface contains constants for use only within the
 * StreamItEditor package.
 */
public interface IStreamItEditorConstants {

    public static final String PLUGIN_ID 
	= "streamit.eclipse.debugger.texteditor"; //$NON-NLS-1$
    public static final String PREFIX = PLUGIN_ID + "."; //$NON-NLS-1$

    // Preference constants
	public static final String PRE_KEYWORD = PREFIX + "keyword";
	public static final String PRE_STR_KEYWORD = PREFIX + "strkeyword";
	public static final String PRE_TYPE = PREFIX + "type";
	public static final String PRE_CONSTANT = PREFIX + "constant";
	public static final String PRE_STR_COMMON = PREFIX + "strcommon";
	
    // Help context ids
    public static final String PREFERENCE_PAGE_CONTEXT
	= PREFIX + "preference_page_context"; //$NON-NLS-1$

	public static String[] CATEGORY_DEFAULTS =
	{ "Keyword", "StreamIt Keyword", "Type", "Constant", "StreamIt Commonword" };

	public static String[] FG_STR_KEYWORD_DEFAULTS =
	  { "add", "body", "duplicate", "enqueue", "feedbackloop", "filter", "init", 
	  "join", "loop", "peek", "phase", "pipeline", "pop", "prework", "push", 
	  "roundrobin", "split", "splitjoin", "struct", "work" };
	  
	public static String[] FG_STR_TYPE_DEFAULTS = 
	{ "bit", "complex" };
	//$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ 
	//$NON-NLS-9$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$
    
	public static String[] FG_STR_COMMON_DEFAULTS = { "Identity", "FileReader", "FileWriter" };
}
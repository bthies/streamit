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
package texteditor;

/**
 * This interface contains constants for use only within the
 * StreamItEditor package.
 */
public interface IStreamItConstants {

    public static final String PLUGIN_ID 
	= "texteditor"; //$NON-NLS-1$
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

	public static String[] FG_KEYWORD_DEFAULTS =
		{ "abstract", "break", "case", "catch", "class", "continue", "default", 
		  "do", "else", "extends", "final", "finally", "for", "if", "implements", 
		  "import", "instanceof", "interface", "native", "new", "package", "private",
		  "protected", "public", "return", "static", "super", "switch", 
		  "synchronized", "this", "throw", "throws", "transient", "try", "volatile",
		  "while" };
		//$NON-NLS-36$ //$NON-NLS-35$ //$NON-NLS-34$ //$NON-NLS-33$ //$NON-NLS-32$ 
		//$NON-NLS-31$ //$NON-NLS-30$ //$NON-NLS-29$ //$NON-NLS-28$ //$NON-NLS-27$
		//$NON-NLS-26$ //$NON-NLS-25$ //$NON-NLS-24$ //$NON-NLS-23$ //$NON-NLS-22$ 
		//$NON-NLS-21$ //$NON-NLS-20$ //$NON-NLS-19$ //$NON-NLS-18$ //$NON-NLS-17$ 
		//$NON-NLS-16$ //$NON-NLS-15$ //$NON-NLS-14$ //$NON-NLS-13$ //$NON-NLS-12$ 
		//$NON-NLS-11$ //$NON-NLS-10$ //$NON-NLS-9$ //$NON-NLS-8$ //$NON-NLS-7$ 
		//$NON-NLS-6$ //$NON-NLS-5$ //$NON-NLS-4$ //$NON-NLS-3$ //$NON-NLS-2$ 
		//$NON-NLS-1$

	public static String[] FG_STR_KEYWORD_DEFAULTS =
	  { "add", "body", "duplicate", "enqueue", "feedbackloop", "filter", "init", 
	  "join", "loop", "peek", "phase", "pipeline", "pop", "prework", "push", 
	  "roundrobin", "split", "splitjoin", "struct", "work" };
	  
	public static String[] FG_TYPE_DEFAULTS = 
	{ "void", "boolean", "char", "byte", "short", "int", "long", "float", 
	  "double", "bit", "complex" };
	//$NON-NLS-1$ //$NON-NLS-5$ //$NON-NLS-7$ //$NON-NLS-6$ //$NON-NLS-8$ 
	//$NON-NLS-9$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-2$
    
	public static String[] FG_CONSTANT_DEFAULTS = { "false", "null", "true" }; 
	//$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$

	public static String[] FG_STR_COMMON_DEFAULTS = { "Identity", "FileReader", "FileWriter" };

}
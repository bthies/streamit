/*******************************************************************************
 * StreamIt Plugin adapted from Example Readme Tool
 * modifier - Kimberly Kuo
 *******************************************************************************/
package streamit.eclipse.debugger.texteditor;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * This interface contains constants for use only within the
 * StreamItEditor package.
 */
public interface IStreamItEditorConstants {

    //public static final String PLUGIN_ID = "streamit.eclipse.debugger.texteditor"; //$NON-NLS-1$
    public static final String PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier() + "."; //$NON-NLS-1$
	public static String ID_STREAMIT_EDITOR = PREFIX + "texteditor.StreamItEditor";
	
    // Preference constants
	public static final String PRE_KEYWORD = PREFIX + "keyword";
	public static final String PRE_STR_KEYWORD = PREFIX + "strkeyword";
	public static final String PRE_TYPE = PREFIX + "type";
	public static final String PRE_CONSTANT = PREFIX + "constant";
	public static final String PRE_STR_COMMON = PREFIX + "strcommon";
	
    // Help context ids
    public static final String PREFERENCE_PAGE_CONTEXT = PREFIX + "preference_page_context"; //$NON-NLS-1$

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
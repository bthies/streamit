package streamit.eclipse.debugger.texteditor;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;
import streamit.eclipse.debugger.ui.IStreamItUIConstants;

/**
 * @author kkuo
 */
public interface IStreamItEditorConstants {

    public static final String PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier() + ".texteditor.";
	public static String ID_STREAMIT_EDITOR = PREFIX + "StreamItEditor";

	// PresentationAction
	public static final String TOGGLE_PRESENTATION = "TogglePresentation.";
	
	// StreamItContentProvider
	public static final String WHITESPACED_COLON_STRING = " : ";
	public static final String WHITESPACE_STRING = " ";
	public static final String INIT_METHOD = "init";
	public static final String WORK_METHOD = "work";

	// StreamItEditor
	public static final String STREAMIT_RULER_CONTEXT = "#StreamItRulerContext";

	// StreamItEditorActionContributor
	public static final String STREAMIT_RUN_MENU = IStreamItUIConstants.ID_UI_PREFIX + "run";
	
	// StreamItEditorCodeScanner
	public static final String COMMENTS = "//";
	public static final String DOUBLE_QUOTE_STRING = "\"";
	public static final char BACKSLASH_STRING = '\\';
	public static final String QUOTE_STRING = "'";
	public static final String JAVA_VERSION = "1.4";
	public static final String NEWLINE_STRING = "\n";

	// StreamItEditorMessages
	public static final char EXCLAMATION_CHAR = '!';
	
	// StreamItEditorPreferencePage
	public static final String PRE_KEYWORD = PREFIX + "keyword";
	public static final String PRE_STR_KEYWORD = PREFIX + "strkeyword";
	public static final String PRE_TYPE = PREFIX + "type";
	public static final String PRE_CONSTANT = PREFIX + "constant";
	public static final String PRE_STR_COMMON = PREFIX + "strcommon";
	public static final String PREFERENCE_PAGE_CONTEXT = PREFIX + "preference_page_context";
	public static final String[] CATEGORY_DEFAULTS = { "Keyword", "StreamIt Keyword", "Type", "Constant", "StreamIt Commonword" };
	public static final String[] FG_STR_KEYWORD_DEFAULTS =
	  { "add", "body", "duplicate", "enqueue", "feedbackloop", "filter", "init", 
	  "join", "loop", "peek", "phase", "pipeline", "pop", "prework", "push", 
	  "roundrobin", "split", "splitjoin", "struct", "work" };
	public static final String[] FG_STR_TYPE_DEFAULTS = { "bit", "complex" };
	public static final String[] FG_STR_COMMON_DEFAULTS = { "Identity", "FileReader", "FileWriter" };
	public static final String CATEGORY = "Category(s)";
	public static final String MEMBER = "Member(s)";
	public static final String DELETE_MEMBER = "Delete_Member(s)";
	public static final String ADD_MEMBER = "Add_Member";
	public static final char A_CHAR = 'a';
	public static final char Z_CHAR = 'z';
	
	// StreamItMenuListener
	public static final String MANAGE_BREAKPOINT_RULER_ACTION = "ManageBreakpointRulerAction";
	public static final String MANAGE_METHOD_BREAKPOINT_RULER_ACTION = "ManageMethodBreakpointRulerAction";
	public static final String MANAGE_WATCHPOINT_RULER_ACTION = "ManageWatchpointRulerAction";
	public static final String GRAPH_EDTIOR_ACTION = "GraphEditorAction";
	public static final String ADD_LABEL = ".add.label";
	public static final String REMOVE_LABEL = ".remove.label";
}
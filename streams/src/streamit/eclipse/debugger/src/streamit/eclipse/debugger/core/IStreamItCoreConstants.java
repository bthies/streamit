package streamit.eclipse.debugger.core;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public interface IStreamItCoreConstants {
	
	public static final String ID_PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier();
	public static final String ID_CORE_PREFIX = ID_PREFIX + ".core.";
	
	public static final String STREAMIT_LIBRARY = "streamit.library.";
	public static final String STREAMIT_MISC = "streamit.misc.";

	// BreakpointRulerData
	public static final String EMPTY_STRING = "";
	public static final String NEWLINE_STRING = "\n";

	// LaunchData
	public static final String COMMENT_STRING = "// ";
	
	// LogFileManager
	public static final String LOGFILE = ".streamit";
	
	// PreDebuggingRunnable
	public static final String ID_JAVA_METHOD_BREAKPOINT = "org.eclipse.jdt.debug.javaMethodBreakpointMarker";
	public static final String FILTER_CLASS = "Filter";
	public static final String PIPELINE_CLASS = "Pipeline";
	public static final String SPLITJOIN_CLASS = "SplitJoin";
	public static final String FEEDBACKLOOP_CLASS = "FeedbackLoop";
	public static final String STREAMIT_CLASS = "StreamItPipeline";

	// StreamItDebugEventFilter
	public static final String JAVA_FILE_EXTENSION = "java";
	public static final String ID_FILTER_INST_BREAKPOINT = ID_CORE_PREFIX + "ID_INTERNAL_BREAKPOINT";
	public static final char PERIOD_CHAR = '.';
	public static final String TYPE_NAME = "org.eclipse.jdt.debug.core.typeName";
	
	// StreamItViewsManager
	public static final String MAIN = "main";
	public static final String PROGRAM = "program";
	
	// StrToJavaMapper
	public static final String COLON_STRING = ":";

	// StreamItModelPresentation
	public static final String INIT_METHOD = "init";
	public static final String WORK_METHOD = "work";
	public static final String PREWORK_METHOD = "prework";
}
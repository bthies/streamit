package streamit.eclipse.debugger.ui;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public interface IStreamItUIConstants {
	
	public static final String ID_UI_PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier() + ".ui.";
	public static final String ID_STREAMIT_ACTION_SET = ID_UI_PREFIX + "StreamItDebugActionSet";
	
	// StreamItModelPresentation
	public static final String LEFT_BRACKET_STRING = " [";
	public static final char RIGHT_BRACKET_CHAR = ']';
	public static final String JDI_MODEL_PRESENTATION_OUT_OF_SYNCH = "JDIModelPresentation._(out_of_synch)_1";
	public static final String JDI_MODEL_PRESENTATION_MAY_BE_OUT_OF_SYNCH = "JDIModelPresentation._(may_be_out_of_synch)_2";
	public static final String JDI_MODEL_PRESENTATION_TERMINATED = "JDIModelPresentation.<terminated>_2";
	public static final String JDI_MODEL_PRESENTATION_DISCONNECTED = "JDIModelPresentation.<disconnected>_4";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_TERMINATED = "JDIModelPresentation.System_Thread_[({0}]_(Terminated)_7";
	public static final String JDI_MODEL_PRESENTATION_THREAD_TERMINATED = "JDIModelPresentation.Thread_[({0}]_(Terminated)_8";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_STEPPING = "JDIModelPresentation.System_Thread_[{0}]_(Stepping)_9";
	public static final String JDI_MODEL_PRESENTATION_THREAD_STEPPING = "JDIModelPresentation.Thread_[{0}]_(Stepping)_10";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_EVALUATING = "JDIModelPresentation.System_Thread_[{0}]_(Evaluating)_9";
	public static final String JDI_MODEL_PRESENTATION_THREAD_EVALUTING = "JDIModelPresentation.Thread_[{0}]_(Evaluating)_10";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_RUNNING = "JDIModelPresentation.System_Thread_[{0}]_(Running)_11";
	public static final String JDI_MODEL_PRESENTATION_THREAD_RUNNING = "JDIModelPresentation.Thread_[{0}]_(Running)_12";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_EXCEPTION = "JDIModelPresentation.System_Thread_[{0}]_(Suspended_(exception_{1}))_13";
	public static final String JDI_MODEL_PRESENTATION_THREAD_EXCEPTION = "JDIModelPresentation.Thread_[{0}]_(Suspended_(exception_{1}))_14";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_ACCESS = "JDIModelPresentation.System_Thread_[{0}]_(Suspended_(access_of_field_{1}_in_{2}))_16";
	public static final String JDI_MODEL_PRESENTATION_THREAD_ACCESS = "JDIModelPresentation.Thread_[{0}]_(Suspended_(access_of_field_{1}_in_{2}))_17";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_MODIFICATION = "JDIModelPresentation.System_Thread_[{0}]_(Suspended_(modification_of_field_{1}_in_{2}))_18";
	public static final String JDI_MODEL_PRESENTATION_THREAD_MODIFICATION = "JDIModelPresentation.Thread_[{0}]_(Suspended_(modification_of_field_{1}_in_{2}))_19";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_ENTRY = "JDIModelPresentation.System_Thread_[{0}]_(Suspended_(entry_into_method_{1}_in_{2}))_21";
	public static final String JDI_MODEL_PRESENTATION_THREAD_ENTRY = "JDIModelPresentation.Thread_[{0}]_(Suspended_(entry_into_method_{1}_in_{2}))_22";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_EXIT = "JDIModelPresentation.System_Thread_[{0}]_(Suspended_(exit_of_method_{1}_in_{2}))_21";
	public static final String JDI_MODEL_PRESENTATION_THREAD_EXIT = "JDIModelPresentation.Thread_[{0}]_(Suspended_(exit_of_method_{1}_in_{2}))_22";
	public static final String LINE_NUMBER_STRING = "lineNumber ";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_RUN = "JDIModelPresentation.System_Thread_[{0}]_(Suspended_(run_to_line_{1}_in_{2}))_23";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_BREAKPOINT = "JDIModelPresentation.System_Thread_[{0}]_(Suspended_(breakpoint_at_line_{1}_in_{2}))_24";
	public static final String JDI_MODEL_PRESENTATION_THREAD_RUN = "JDIModelPresentation.Thread_[{0}]_(Suspended_(run_to_line_{1}_in_{2}))_25";
	public static final String JDI_MODEL_PRESENTATION_THREAD_BREAKPOINT = "JDIModelPresentation.Thread_[{0}]_(Suspended_(breakpoint_at_line_{1}_in_{2}))_26";
	public static final String JDI_MODEL_PRESENTATION_SYSTEM_THREAD_SUSPENDED = "JDIModelPresentation.System_Thread_[{0}]_(Suspended)_27";
	public static final String JDI_MODEL_PRESENTATION_THREAD_SUSPENDED = "JDIModelPresentation.Thread_[{0}]_(Suspended)_28";
	public static final String JDI_MODEL_PRESENTATION_UNKNOWN_DECLARING_TYPE = "JDIModelPresentation<unknown_declaring_type>_4";
	public static final String JDI_MODEL_PRESENTATION_OBSOLETE_METHOD = "JDIModelPresentation.<obsolete_method_in__1";
	public static final String JDI_MODEL_PRESENTATION_UNKNOWN_RECEIVING_TYPE = "JDIModelPresentation<unknown_receiving_type>_5";
	public static final char LEFT_PARENTHESIS_CHAR = '(';
	public static final char RIGHT_PARENTHESIS_CHAR = ')';
	public static final char PERIOD_CHAR = '.';
	public static final String JDI_MODEL_PRESENTATION_UNKNOWN_METHOD = "JDIModelPresentation<unknown_method_name>_6";
	public static final String PARENTHESES_STRING = "()";
	public static final String JDI_MODEL_PRESENTATION_LINE = "JDIModelPresentation.line__76";
	public static final char WHITESPACE_CHAR = ' ';
	public static final String JDI_MODEL_PRESENTATION_NOT_AVAILABLE = "JDIModelPresentation.not_available";
	public static final String JDI_MODEL_PRESENTATION_NATIVE_METHOD = "JDIModelPresentation.native_method";
	public static final String JDI_MODEL_PRESENTATION_UNKNOWN = "JDIModelPresentation_<unknown_line_number>_8";
	public static final String JDI_MODEL_PRESENTATION_LOCAL_VARIABLE_UNAVAILABLE = "JDIModelPresentation.local_variables_unavailable";
	public static final String LABEL_STRING = "label ";
	public static final char GREATER_THAN_CHAR = '>';
	
	// VariableViewerFilter
	public static final String STREAM_ELEMENTS_FIELD = "streamElements";
	public static final String STREAMIT_LIBRARY = "streamit.library.";
	public static final String STREAMIT_MISC = "streamit.misc.";
	public static final String STREAM_CLASS = STREAMIT_LIBRARY + "Stream";
	public static final String CHILDREN_STREAMS_FIELD = "childrenStreams";
	public static final String SPLITJOIN_CLASS = STREAMIT_LIBRARY + "SplitJoin";
	public static final String BODY_FIELD = "body";
	public static final String LOOP_FIELD = "loop";
	public static final String FEEDBACKLOOP_CLASS = STREAMIT_LIBRARY + "FeedbackLoop";
	public static final String ALLFILTERS_FIELD = "allFilters";
	public static final String ALL_SINKS_FIELD = "allSinks";
	public static final String DESTROYED_CLASS_FIELD = "DestroyedClass";
	public static final String FULL_CHANNELS_FIELD = "fullChannels";
	public static final String MESSAGE_STUB_FIELD = "MESSAGE_STUB";
	public static final String TOTAL_BUFFER_FIELD = "totalBuffer";
	public static final String ARGS_FIELD = "args";
	public static final String HEADER_FIELD = "header";
	public static final String NEXT_FIELD = "next";
	public static final String ELEMENT_FIELD = "element";
	public static final String NULL_VALUE = "null";
	public static final String FILTER_CLASS = STREAMIT_LIBRARY + "Filter";
	public static final String DESTROYED_CLASS_CLASS = STREAMIT_MISC + DESTROYED_CLASS_FIELD;
	public static final String OPERATOR_CLASS = STREAMIT_LIBRARY + "Operator";
}
package streamit.eclipse.debugger.core;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public interface IStreamItDebuggerConstants {
	
	// used by even set listener
	public static final int INIT_BREAKPOINTS = 0;
	public static final int WORK_BREAKPOINTS = 1;
	public static final int PREWORK_BREAKPOINTS = 2;
	public static final int OTHER_BREAKPOINTS = 3;
	
	// eclipse ids
	public static final String PLUGIN_ID = StreamItDebuggerPlugin.getUniqueIdentifier();	
	public static final String ID_STREAMVIEW = PLUGIN_ID + ".graph.StreamView";
	public static final String ID_STREAMOVERVIEW = PLUGIN_ID + ".graph.StreamOverview";
	public static final String ID_JAVA_METHOD_BREAKPOINT = "org.eclipse.jdt.debug.javaMethodBreakpointMarker";
	 
	//
	public static final String STREAMIT_LIBRARY = "streamit.library.";
	public static final String STREAMIT_MISC = "streamit.misc.";
	
	public static final String INIT_METHOD = "init";
	public static final String WORK_METHOD = "work";
	public static final String PREWORK_METHOD = "prework";
	public static final String MAIN_METHOD = "main";
	
	public static final String FILTER_CLASS = "Filter";
	public static final String PIPELINE_CLASS = "Pipeline";
	public static final String SPLITJOIN_CLASS = "SplitJoin";
	public static final String FEEDBACKLOOP_CLASS = "FeedbackLoop";
	public static final String STREAMIT_CLASS = "StreamItPipeline";
	
	public static final String[] FILTER_VIEW_HEADERS = {
		"Filter Name", "Input Type", "Output Type", "State Variables", "Pop Count", "Push Count", "Peek Count", 
		"Current Stage", "Work Executions"
	};
	public static final String NOT_APPLICABLE = "N/A";
	public static final String CHANNEL_CLASS = STREAMIT_LIBRARY + "Channel";
	public static final String INPUT_FIELD = "input";
	public static final String OUTPUT_FIELD = "output";
	public static final String STREAM_ELEMENTS_FIELD = "streamElements";
	public static final String VOID_VALUE = "void";
	public static final String TYPE_FIELD = "type";
	public static final String CLASS_CLASS = "java.lang.Class";
	public static final String INTEGER_CLASS = "java.lang.Integer";
	public static final String POP_PUSH_COUNT_FIELD = "popPushCount";
	public static final String PEEK_COUNT_FIELD = "peekCount";
	public static final String VALUE_FIELD = "value";
	public static final String INT_VALUE = "int";
	
	public static final String RESUME = StreamItDebuggerPlugin.getUniqueIdentifier() + ".RESUME";
	public static final String CHANNEL = "Channel";
	public static final String TOTALITEMSPUSHED_FIELD = "totalItemsPushed";
	public static final String TOTALITEMSPOPPED_FIELD = "totalItemsPopped";
	public static final String QUEUE_FIELD = "queue";
	public static final String INT = "int";
	public static final String LINKEDLIST_CLASS = "java.util.LinkedList";
	public static final String ID_JAVA_WATCHPOINT = "org.eclipse.jdt.debug.javaWatchpointMarker"; 
	public static final String HEADER_FIELD = "header";
	public static final String NEXT_FIELD = "next";
	public static final String ELEMENT_FIELD = "element";
	public static final String LINKEDLISTENTRY_CLASS = "java.util.LinkedList$Entry";
	public static final String NULL_VALUE = "null";
	public static final String THIS_FIELD = "this";
	public static final String SINK_FIELD = "sink";
	public static final String OPERATOR_CLASS =  STREAMIT_LIBRARY + "Operator";
	
	public static final String[] CHANNEL_VIEW_HEADERS = {
		"Source", "Number Pushed", "Tape View", "Sink", "Number Popped"
	};
	
	// attribute codes
	public static final String ATTR_HIGHLIGHT = PLUGIN_ID + ".HIGHLIGHT_ATTR"; //$NON-NLS-1$

}
package streamit.eclipse.debugger.graph;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public class IStreamItGraphConstants {
	
	public static final String ID_GRAPH_PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier() + ".graph.";
	public static final String ID_STREAMVIEW = ID_GRAPH_PREFIX + "StreamView";
	public static final String ID_STREAMOVERVIEW = ID_GRAPH_PREFIX + "StreamOverview";

	protected static final int MARGIN = 4;
	protected static final int CHANNEL_WIDTH = 24;//16;

	// Channel
	public static final String QUEUE_FIELD = "queue";
	public static final String HEADER_FIELD = "header";
	public static final String NEXT_FIELD = "next";
	public static final String COMPLEX_FIELD = "Complex";
	public static final String VALUE_FIELD = "value";
	public static final String EMPTY_STRING = "";
	public static final String NEWLINE_STRING = "\n";

	// ChannelSelector
	public static final String POPUP_MENU = "#PopUp";
	
	// Datum
	public static final char LEFT_CURLY_BRACKET_CHAR = '{';
	public static final char RIGHT_CURLY_BRACKET_CHAR = '}';
	public static final char LEFT_BRACKET_CHAR = '[';
	public static final char RIGHT_BRACKET_CHAR = ']';
	public static final String REAL_FIELD = "real";
	public static final String IMAG_FIELD = "imag";
	public static final char I_CHAR = 'i';
	public static final char PLUS_CHAR = '+';

	// FeedbackLoop
	public static final String BODY_FIELD = "body";
	public static final String LOOP_FIELD = "loop";
	public static final String STREAMIT_LIBRARY = "streamit.library.";
	public static final String FILTER_FIELD = STREAMIT_LIBRARY + "Filter";
	public static final String IDENTITY_FILTER_FIELD = STREAMIT_LIBRARY + "ChannelConnectFilter";
	public static final String PIPELINE_FIELD = STREAMIT_LIBRARY + "Pipeline";
	public static final String SPLITJOIN_FIELD = STREAMIT_LIBRARY + "SplitJoin";
	public static final String FEEDBACKLOOP_FIELD = STREAMIT_LIBRARY + "FeedbackLoop";
	public static final String INPUT_FIELD = "input";
	public static final String OUTPUT_FIELD = "output";
	public static final char ZERO_CHAR = '0';
	public static final char ONE_CHAR = '1';

	// Filter
	public static final String VOID_STRING = "void";
	public static final String NA_STRING = "N/A";
	public static final String INIT_EXECUTION_COUNT_FIELD = "initExecutionCount";
	public static final String STEADY_EXECUTION_COUNT_FIELD = "steadyExecutionCount";
	public static final String ZERO_STRING = "0";
	public static final String TYPE_FIELD = "type";
	public static final String PAREN_FIELD = "()";
	public static final String PEEKCOUNT_FIELD = "peekCount";
	public static final String POPPUSHCOUNT_FIELD = "popPushCount";
	public static final String TOTALITEMSPOPPEDCOUNT_FIELD = "totalItemsPopped";
	public static final String TOTALITEMSPUSEDCOUNT_FIELD = "totalItemsPushed";
	public static final String POPPED_FIELD = "currentPopped";
	public static final String PUSHED_FIELD = "currentPushed";
	public static final String PEEKED_FIELD = "currentMaxPeek";
	
	public static final String STRUCTURE_TYPE = "Stream Type";
	public static final String INPUT_TYPE = "Input Type";
	public static final String OUTPUT_TYPE = "Output Type";
	public static final String POP_RATE = "Pop Rate";
	public static final String PEEK_RATE = "Peek Rate";
	public static final String PUSH_RATE = "Push Rate";
	public static final String POPPED = "Data Popped";
	public static final String PEEKED = "Index Peeked";
	public static final String PUSHED = "Data Pushed";
	public static final String WORK_EXECUTIONS = "Work Executions";	
	public static final String INIT_EXECUTION_COUNT = "Init Repetitions";
	public static final String STEADY_EXECUTION_COUNT = "Steady Repetitions";
	
	public static final String TAB = ":\t";
	public static final String TABS = TAB + "\t";
	public static final String STRUCTURE_TYPE_LABEL = NEWLINE_STRING + STRUCTURE_TYPE + TABS;
	public static final String FILTER_STRUCTURE = "filter";
	public static final String PIPELINE_STRUCTURE = "pipeline";
	public static final String SPLITJOIN_STRUCTURE = "splitjoin";
	public static final String FEEDBACKLOOP_STRUCTURE = "feedbackloop";
	public static final String INPUT_TYPE_LABEL = NEWLINE_STRING + INPUT_TYPE + TABS;
	public static final String OUTPUT_TYPE_LABEL = NEWLINE_STRING + OUTPUT_TYPE + TABS;
	public static final String POP_RATE_LABEL = NEWLINE_STRING + POP_RATE + TABS;
	public static final String PEEK_RATE_LABEL = NEWLINE_STRING + PEEK_RATE + TABS;
	public static final String PUSH_RATE_LABEL = NEWLINE_STRING + PUSH_RATE + TABS;
	public static final String POPPED_LABEL = NEWLINE_STRING + POPPED + TABS;
	public static final String PEEKED_LABEL = NEWLINE_STRING + PEEKED + TABS;
	public static final String PUSHED_LABEL = NEWLINE_STRING + PUSHED + TABS;
	public static final String INIT_STAGE = "init";
	public static final String WORK_STAGE = "work";
	public static final String WORK_EXECUTIONS_LABEL = NEWLINE_STRING + WORK_EXECUTIONS + TAB;
	public static final String INIT_EXECUTION_COUNT_LABEL = NEWLINE_STRING + "Init Reps" + TABS;
	public static final String STEADY_EXECUTION_COUNT_LABEL = NEWLINE_STRING + "Steady Reps" + TABS;

	// MainPipeline
	public static final String STREAMELEMENTS_FIELD = "streamElements";

	// SplitJoin
	public static final String SPLITTYPE_FIELD = "splitType";
	public static final String JOINTYPE_FIELD = "joinType";
	public static final String CHILDRENSTREAMS_FIELD = "childrenStreams";
	public static final String SPLITTER_FIELD = "splitter";
	public static final String JOINER_FIELD = "joiner";
	public static final String WEIGHT_FIELD = "weight";
	public static final String DEST_WEIGHT_FIELD = "destWeight";
	public static final String SRCS_WEIGHT_FIELD = "srcsWeight";
	public static final String ELEMENT_DATA_FIELD = "elementData";
		
	// StreamItViewFactory
	public static final String ELEMENT_FIELD = "element";
	public static final String NULL_VALUE = "null";
	
	// StreamViewer
	public static final String PROGRAM_FIELD = "program";
}

package streamit.eclipse.debugger.graph;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public class IStreamItGraphConstants {
	
	public static final String PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier() + "."; //$NON-NLS-1$
	public static final String GRAPH_VIEW_CONTEXT = PREFIX + "graph_view_context"; //$NON-NLS-1$
	public static final String GRAPH_OVERVIEW_CONTEXT = PREFIX + "graph_overview_context"; //$NON-NLS-1$
	
	// metrics
	protected static String NA = "N/A";
	protected static String STRUCTURE_TYPE_LABEL = "\nStructure Type:\t\t";
	protected static String FILTER_STRUCTURE = "filter";
	protected static String PIPELINE_STRUCTURE = "pipeline";
	protected static String SPLITJOIN_STRUCTURE = "splitjoin";
	protected static String INPUT_TYPE_LABEL = "\nInput Type:\t\t";
	protected static String OUTPUT_TYPE_LABEL = "\nOutput Type:\t\t";
	protected static String POP_COUNT_LABEL = "\nPop Count:\t\t";
	protected static String PEEK_COUNT_LABEL = "\nPeek Count:\t\t";
	protected static String PUSH_COUNT_LABEL = "\nPush Count:\t\t";
	protected static String CURRENT_STAGE_LABEL = "\nCurrent Stage:\t\t";
	protected static String INIT_STAGE = "init";
	protected static String WORK_STAGE = "work";
	protected static String WORK_EXECUTIONS_LABEL = "\nWork Executions:\t";

	// stream layout
	protected static final int MARGIN = 4;
	protected static final int CHANNEL_WIDTH = 16;
	
	/*
	protected static int FILTER_WIDTH = 150;
	protected static int PIPELINE_WIDTH = 175;
	protected static int CHANNEL_WIDTH = 15;
	
	protected static int FILTER_HEIGHT = 132;
	protected static int PIPELINE_HEIGHT = 70;
	protected static int CHANNEL_HEIGHT = 15;
	
	//	graph overview layout
	 protected static int OVERVIEW_FILTER_WIDTH = 85;
	 protected static int OVERVIEW_PIPELINE_WIDTH = 90;
	 protected static int OVERVIEW_CHANNEL_WIDTH = 15;
	 
	 protected static int OVERVIEW_FILTER_HEIGHT = 15;
	 protected static int OVERVIEW_PIPELINE_HEIGHT = 101;
	 protected static int OVERVIEW_CHANNEL_HEIGHT = 15;
	 */	 
}

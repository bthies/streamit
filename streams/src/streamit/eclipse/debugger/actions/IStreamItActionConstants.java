package streamit.eclipse.debugger.actions;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public interface IStreamItActionConstants {
	
	public static String PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier() + ".actions."; 
	public static String ID_LINE_BREAKPOINT_ACTION = PREFIX + "ManageBreakpointAction";
	public static String ID_METHOD_BREAKPOINT_ACTION = PREFIX + "ManageMethodBreakpointAction";
	public static String ID_WATCHPOINT_ACTION = PREFIX + "ManageWatchpointAction";
	public static String ID_GRAPHEDITOR_ACTION = PREFIX + "GraphEditorAction";
	
	public static String ID_COLLAPSEALL_ACTION = PREFIX + "CollapseAllViewActionDelegate";
	public static String ID_EXPANDALL_ACTION = PREFIX + "ExpandAllViewActionDelegate";
	public static String ID_HIDELINES_ACTION = PREFIX + "HideLinesViewActionDelegate";
}

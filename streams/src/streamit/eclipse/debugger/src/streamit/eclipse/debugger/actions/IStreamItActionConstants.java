package streamit.eclipse.debugger.actions;

import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * @author kkuo
 */
public interface IStreamItActionConstants {
	
	// ActionMessages
	public static final String ID_ACTION_PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier() + ".actions.";
	public static final String MESSAGE_ERROR = "!";

	// AddRuntimeFilterBreakpointAction
	public static final String FILTER_BREAKPOINT_ADD_BREAKPOINT = "FilterBreakpoint.addBreakpoint";
	public static final String FILTER_BREAKPOINT_TOOL_TIP_TEXT = "FilterBreakpoint.toolTipText";
	public static final String FILTER_BREAKPOINT_REMOVE_BREAKPOINT = "FilterBreakpoint.removeBreakpoint";
	public static final char PERIOD_CHAR = '.';	
	
	// ChangeQueueValuesAction, ChangeVariableValueInputDialog, ShowQueueValuesAction, ShowQueueValuesDialog
	public static final String CHANGE_QUEUE_VALUES_TITLE = "ChangeQueueValues.title";
	public static final String CHANGE_QUEUE_VALUES_TOOL_TIP_TEXT = "ChangeQueueValues.toolTipText";
	public static final String EMPTY_STRING = "";
	public static final String CHANGE_QUEUE_VALUES_ERROR_DIALOG_TITLE = "ChangeQueueValues.errorDialogTitle";
	public static final String CHANGE_QUEUE_VALUES_ERROR_DIALOG_MESSAGE = "ChangeQueueValues.errorDialogMessage";
	public static final String CHANGE_QUEUE_VALUES_SET_VARIABLE_VALUE = "ChangeQueueValuesSet_Variable_Value_1";
	public static final String CHANGE_QUEUE_VALUES_ENTER_A_NEW_VALUE = "ChangeQueueValuesEnter_a_new_value_for_2";
	public static final String CHANGE_QUEUE_VALUES_AN_EXCEPTION_OCCURRED = "ChangeQueueValuesAn_exception_occurred_3"; 
	public static final String CHANGE_QUEUE_VALUES_INVALID_VALUE = "ChangeQueueValuesInvalid_value_4"; 
	
	// EnableDisableBreakpointRulerAction
	public static final String ENABLE_DISABLE_BREAKPOINT_RULER_ACTION_ENABLE_BREAKPOINT_1 = "EnableDisableBreakpointRulerAction.&Enable_Breakpoint_1";
	public static final String ENABLE_DISABLE_BREAKPOINT_RULER_ACTION_DISABLING_BREAKPOINTS = "EnableDisableBreakpointRulerAction.Enabling/disabling_breakpoints_2";
	public static final String ENABLE_DISABLE_BREAKPOINT_RULER_ACTION_EXCEPTIONS = "EnableDisableBreakpointRulerAction.Exceptions_occurred_enabling_disabling_the_breakpoint_3";
	public static final String ENABLE_DISABLE_BREAKPOINT_RULER_ACTION_DISABLE_BREAKPOINT = "EnableDisableBreakpointRulerAction.&Disable_Breakpoint_4";
	public static final String ENABLE_DISABLE_BREAKPOINT_RULER_ACTION_DISABLE_BREAKPOINT_5 = "EnableDisableBreakpointRulerAction.&Enable_Breakpoint_5";

	// HighlightDatumAction
	public static final String HIGHLIGHT_DATUM_HIGHLIGHT_DATUM = "HighlightDatum.highlightDatum";
	public static final String HIGHLIGHT_DATUM_HIGHLIGHT_TOOL_TIP_TEXT = "HighlightDatum.toolTipText";
	public static final String HIGHLIGHT_DATUM_HIGHLIGHT_UNHIGHLIGHT_DATUM = "HighlightDatum.unhighlightDatum";
	
	// ManageBreakpointRulerAction
	public static final String MANAGE_BREAKPOINT_RULER_ACTION_ADD_LABEL = "ManageBreakpointRulerAction.add.label";
	public static final String MANAGE_BREAKPOINT_RULER_ACTION_REMOVE_LABEL = "ManageBreakpointRulerAction.remove.label";
	public static final String MANAGE_BREAKPOINT_RULER_ACTION_ERROR_ADDING = "ManageBreakpointRulerAction.error.adding.message1";
	public static final String MANAGE_BREAKPOINT_RULER_ACTION_ERROR_REMOVING = "ManageBreakpointRulerAction.error.removing.message1";
	public static final String HANDLE_ID = JDIDebugUIPlugin.getUniqueIdentifier() + ".JAVA_ELEMENT_HANDLE_ID";

	// ManageMethodBreakpointRulerAction
	public static final String MANAGE_METHOD_BREAKPOINT_RULER_ACTION_ADD_LABEL = "ManageMethodBreakpointRulerAction.add.label";
	public static final String MANAGE_METHOD_BREAKPOINT_RULER_ACTION_REMOVE_LABEL = "ManageMethodBreakpointRulerAction.remove.label";
	public static final String MANAGE_METHOD_BREAKPOINT_RULER_ACTION_INIT = "<init>";
	public static final String MANAGE_METHOD_BREAKPOINT_RULER_ACTION_CANT_ADD = "ManageMethodBreakpointActionDelegate.CantAdd";

	// ManageWatchpointRulerAction
	public static final String MANAGE_WATCHPOINT_RULER_ACTION_ADD_LABEL = "ManageWatchpointRulerAction.add.label";
	public static final String MANAGE_WATCHPOINT_RULER_ACTION_REMOVE_LABEL = "ManageWatchpointRulerAction.remove.label";
	public static final String MANAGE_WATCHPOINT_RULER_ACTION_CANT_ADD = "ManageWatchpointActionDelegate.CantAdd";
	
	// ShowQueueValuesAction
	public static final String SHOW_QUEUE_VALUES_TITLE = "ShowQueueValues.title";
	public static final String SHOW_QUEUE_VALUES_TOOL_TIP_TEXT = "ShowQueueValues.toolTipText";
	public static final String SHOW_QUEUE_VALUES_ERROR_DIALOG_TITLE = "ShowQueueValues.errorDialogTitle";
	public static final String SHOW_QUEUE_VALUES_ERROR_DIALOG_MESSAGE = "ShowQueueValues.errorDialogMessage";
	public static final String SHOW_QUEUE_VALUES_SET_VARIABLE_VALUE = "ShowQueueValuesSet_Variable_Value_1";
	public static final String SHOW_QUEUE_VALUES_ENTER_A_NEW_VALUE = "ShowQueueValuesEnter_a_new_value_for_2";

	// StreamItBreakpointPropertiesDialog
	public static final String STREAMIT_BREAKPOINT_PROPERTIES_DIALOG_TITLE_PATH = "images/pref_dialog_title.gif";
	public static final String STREAMIT_BREAKPOINT_PROPERTIES_DIALOG_ERROR_PATH = "images/message_error.gif";
	public static final String SPACE_STRING = " ";

	// StreamItBreakpointPropertiesRulerAction
	public static final String JAVA_BREAKPOINT_PROPERTIES_RULER_ACTION_BREAKPOINT_PROPERTIES = "JavaBreakpointPropertiesRulerAction.Breakpoint_&Properties_1";

	// StreamItMenuListener
	public static final String ID_LINE_BREAKPOINT_ACTION = ID_ACTION_PREFIX + "ManageBreakpointAction";
	public static final String ID_METHOD_BREAKPOINT_ACTION = ID_ACTION_PREFIX + "ManageMethodBreakpointAction";
	public static final String ID_WATCHPOINT_ACTION = ID_ACTION_PREFIX + "ManageWatchpointAction";
	public static final String ID_GRAPHEDITOR_ACTION = ID_ACTION_PREFIX + "GraphEditorAction";
}

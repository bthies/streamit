package streamit.eclipse.debugger.launching;

import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * This interface contains constants for use only within the
 * StreamItEditor package.
 * 
 * @author kkuo
 */
public interface IStreamItLaunchingConstants {
	
	public static final String ID_DEBUGGER_PREFIX = StreamItDebuggerPlugin.getUniqueIdentifier();
	public static final String ID_LAUNCHING_PREFIX = ID_DEBUGGER_PREFIX + ".launching.";

	// LaunchingMessages
	public static final char EXCLAMATION_CHAR = '!';
	
	// StreamItApplicationLaunchShortcut
	public static final String JAVA_APPLICATION_ACTION_LAUNCH_FAILED = "JavaApplicationAction.Launch_failed_7";
	public static final String JAVA_APPLICATION_ACTION_EDITOR = "JavaApplicationAction.Editor";
	public static final String JAVA_APPLICATION_ACTION_PROJECT = "JavaApplicationAction.Project";
	public static final String JAVA_APPLICATION_ACTION_FILE = "JavaApplicationAction.File";
	public static final String JAVA_APPLICATION_ACTION_SELECTION = "JavaApplicationAction.Selection";
	public static final String EMPTY_STRING = "";
	public static final String DEFAULT_ITERATIONS = "-i 10";
	public static final String JAVA_APPLICATION_LAUNCH_SHORTCUT_ERROR_LAUNCHING = "JavaApplicationLaunchShortcut.Error_Launching_1";
	public static final String JAVA_APPLICATION_LAUNCH_SHORTCUT_SELECTION = "JavaApplicationAction.Launch_Configuration_Selection_1";
	public static final String JAVA_APPLICATION_LAUNCH_SHORTCUT_DEBUG = "JavaApplicationAction.Choose_a_launch_configuration_to_debug_2";
	public static final String JAVA_APPLICATION_LAUNCH_SHORTCUT_RUN = "JavaApplicationAction.Choose_a_launch_configuration_to_run_3";
	public static final String STREAMIT_APPLICATION_ACTION_FILE_SELECTION = "StreamItApplicationAction.StreamIt_File_Selection_1";
	public static final String STREAMIT_APPLICATION_ACTION_DEBUG = "StreamItApplicationAction.Choose_a_file_to_debug_2";
	public static final String STREAMIT_APPLICATION_ACTION_RUN = "StreamItApplicationAction.Choose_a_file_to_run_3";
	public static final String ID_STR_APPLICATION = ID_LAUNCHING_PREFIX + "localStreamItApplication"; //$NON-NLS-1$

	// StreamItArgumentsTab
	public static final String STREAMIT_ARGUMENTS_TAB_SECONDARY = "StreamItArgumentsTab.&Secondary_classes__5";
	public static final String JAVA_ARGUMENTS_TAB_PROGRAM = "JavaArgumentsTab.&Program_arguments__5";
	public static final String JAVA_ARGUMENTS_TAB_EXCEPTION = "JavaArgumentsTab.Exception_occurred_reading_configuration___15";
	public static final String JAVA_ARGUMENTS_TAB_ARGUMENTS = "JavaArgumentsTab.&Arguments_16";
	public static final String ATTR_SECONDARY_CLASSES = ID_DEBUGGER_PREFIX + ".SECONDARY_CLASSES";
	
	// StreamItLocalApplicationLaunchConfigurationDelegate
	public static final String LIBRARY_OPTION = "--library";
	public static final String NEWLINE_STRING = "\n";
	public static final char COLON_CHAR = ':';
	public static final String JAVA_APPLICATION_LAUNCH_SHORTCUT_ERROR = "JavaApplicationLaunchShortcut.Error_Launching_1";
	public static final String JAVA_APPLICATION_LAUNCH_SHORTCUT_EXCEPTION = "JavaApplicationLaunchShortcut.Exception";
	public static final char QUOTE_CHAR = '\'';
	public static final char PERIOD_CHAR = '.';
	public static final String STR_FILE_EXTENSION = "str";
	public static final String JAVA_FILE_EXTENSION = "java";
	public static final String FILE_OUTPUT_OPTION = "--output";
	public static final String THE_IMPORT_STRING = "The import";
	public static final String NEVER_USED_STRING = "is never used";
	public static final String EXPECTING_STRING = "expecting ";
	public static final String FOUND_STRING = ", found ";
	public static final String UNEXPECTED_STRING = "unexpected token: ";
	public static final String ATTR_STR_LAUNCH = ID_LAUNCHING_PREFIX + "strLaunch";
	public static final String ATTR_KILL_LAUNCH = ID_LAUNCHING_PREFIX + "killLaunch";
	public static final String AT = " at ";

	// StreamItMainTab
	/**
	 * Boolean launch configuration attribute indicating that external jars (on
	 * the runtime classpath) should be searched when looking for a main type.
	 * Default value is <code>false</code>.
	 * 
	 * @since 2.1
	 */
	public static final String ATTR_INCLUDE_EXTERNAL_JARS = IJavaDebugUIConstants.PLUGIN_ID + ".INCLUDE_EXTERNAL_JARS"; //$NON-NLS-1$
	public static final String JAVA_MAIN_TAB_PROJECT = "JavaMainTab.&Project__2";
	public static final String JAVA_MAIN_TAB_BROWSE = "JavaMainTab.&Browse_3";
	public static final String JAVA_MAIN_TAB_MAIN_CLASSES = "JavaMainTab.Main_cla&ss__4";
	public static final String JAVA_MAIN_TAB_SEARCH = "JavaMainTab.Searc&h_5";
	public static final String JAVA_MAIN_TAB_CHOOSE_MAIN_TYPE = "JavaMainTab.Choose_Main_Type_11";
	public static final String JAVA_MAIN_TAB_CHOOSE_MAIN_TYPE_LAUNCH = "JavaMainTab.Choose_a_main_&type_to_launch__12";
	public static final String JAVA_MAIN_TAB_PROJECT_SELECTION = "JavaMainTab.Project_Selection_13";
	public static final String JAVA_MAIN_TAB_PROJECT_TO_CONSTRAIN = "JavaMainTab.Choose_a_&project_to_constrain_the_search_for_main_types__14";
	public static final String JAVA_MAIN_TAB_PROJECT_DOES_NOT_EXIST = "JavaMainTab.Project_does_not_exist_15";
	public static final String JAVA_MAIN_TAB_MAIN_TYPE_NOT_SPECIFIED = "JavaMainTab.Main_type_not_specified_16";
	public static final String JAVA_MAIN_TAB_MAIN = "JavaMainTab.&Main_19";



}
/*******************************************************************************
 * StreamIt Plugin adapted from Example Readme Tool
 * modifier - Kimberly Kuo
 *******************************************************************************/
package streamit.eclipse.debugger.launching;

import streamit.eclipse.debugger.StreamItDebuggerPlugin;

/**
 * This interface contains constants for use only within the
 * StreamItEditor package.
 */
public interface IStreamItLaunchingConstants {

	public static final String ATTR_SECONDARY_CLASSES = StreamItDebuggerPlugin.getUniqueIdentifier() + ".SECONDARY_CLASSES";
	public static final String ID_STR_APPLICATION = StreamItDebuggerPlugin.getUniqueIdentifier() + ".launching.localStreamItApplication"; //$NON-NLS-1$
	public static final String STR_FILE_EXTENSION = "str";
	public static final String JAVA_FILE_EXTENSION = "java";
	public static final String FILE_OUTPUT_OPTION = "--output";
	public static final String TO_JAVA_CLASS = "streamit.frontend.ToJava";
	public static final String STR_TO_JAVA_CONFIG = "StrToJavaConfig";
}
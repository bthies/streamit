/*******************************************************************************
 * StreamIt Launcher Plugin
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.launcher;

import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This is the top-level class of the Launcher plugin tool.
 *
 * @see AbstractUIPlugin for additional information on UI plugins
 */
public class StreamItLauncherPlugin extends AbstractUIPlugin {

	// Default instance of the receiver
	private static StreamItLauncherPlugin inst;
	
	public static final String ATTR_SECONDARY_CLASSES = StreamItLauncherPlugin.getUniqueIdentifier() + ".SECONDARY_CLASSES";
	public static final String ID_STR_APPLICATION = StreamItLauncherPlugin.getUniqueIdentifier() + ".localStreamItApplication"; //$NON-NLS-1$
	public static final String STR_FILE_EXTENSION = "str";
	public static final String JAVA_FILE_EXTENSION = "java";
	public static final String FILE_OUTPUT_OPTION = "--output";
	public static final String TO_JAVA_CLASS = "streamit.frontend.ToJava";
	public static final String STR_TO_JAVA_CONFIG = "StrToJavaConfig";
	
	public static final String INIT_METHOD = "init";
	public static final String WORK_METHOD = "work";
	public static final String PREWORK_METHOD = "prework";
	public static final String FILTER_CLASS = "Filter";
	public static final String STREAMIT_CLASS = "StreamIt";
	public static final String[] FILTER_VIEW_HEADERS = {
		"Filter Name", "Input Type", "Output Type", "State Variables", "Pop Count", "Push Count", "Peek Count", 
		"Current Stage", "Work Executions"
	};
	public static final String NOT_APPLICABLE = "N/A";
	public static final String CHANNEL_CLASS = "streamit.Channel";
	public static final String INPUT_FIELD = "input";
	public static final String OUTPUT_FIELD = "output";
	public static final String VOID_VALUE = "void";
	public static final String TYPE_FIELD = "type";
	public static final String CLASS_CLASS = "java.lang.Class";
	public static final String INTEGER_CLASS = "java.lang.Integer";
	public static final String POP_PUSH_COUNT_FIELD = "popPushCount";
	public static final String PEEK_COUNT_FIELD = "peekCount";
	public static final String VALUE_FIELD = "value";
	public static final String INT_VALUE = "int";
	public static final String ID_FILTERVIEW = StreamItLauncherPlugin.getUniqueIdentifier() + ".FilterView";
	public static final String RESUME = StreamItLauncherPlugin.getUniqueIdentifier() + ".RESUME";
	public static final String ID_JAVA_METHOD_BREAKPOINT = "org.eclipse.jdt.debug.javaMethodBreakpointMarker"; 

	/**
	 * Creates the StreamItLauncher plugin and caches its default instance
	 *
	 * @param descriptor  the plugin descriptor which the receiver is made from
	 */
	public StreamItLauncherPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		if (inst == null) inst = this;
	}

	/**
	 * Gets the plugin singleton.
	 *
	 * @return the default StreamItLauncherPlugin instance
	 */
	static public StreamItLauncherPlugin getDefault() {
		return inst;
	}
	
	public static String getUniqueIdentifier() {
		if (getDefault() == null) {
			// If the default instance is not yet initialized,
			// return a static identifier. This identifier must
			// match the plugin id defined in plugin.xml
			return "launcher"; //$NON-NLS-1$
		}
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
}
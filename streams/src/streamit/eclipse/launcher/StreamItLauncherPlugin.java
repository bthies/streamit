/*******************************************************************************
 * StreamIt Launcher Plugin
 * @author kkuo
 *******************************************************************************/
package launcher;

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
	public static final String FILTER_CLASS = "Filter";
	public static final String FILTER_KEYWORD = "filter";
	
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
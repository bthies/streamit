/*******************************************************************************
 * StreamIt Debugger Plugin
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.debugger;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.IPreferenceConstants;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import streamit.eclipse.debugger.texteditor.StreamItEditorPreferencePage;

/**
 * This is the top-level class of the Debugger plugin tool.
 *
 * @see AbstractUIPlugin for additional information on UI plugins
 */
public class StreamItDebuggerPlugin extends AbstractUIPlugin {

	// Default instance of the receiver
	private static StreamItDebuggerPlugin inst;
		
	/**
	 * Creates the StreamItDebugger plugin and caches its default instance
	 *
	 * @param descriptor  the plugin descriptor which the receiver is made from
	 */
	public StreamItDebuggerPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		if (inst == null) inst = this;
	}

	/**
	 * Gets the plugin singleton.
	 *
	 * @return the default StreamItDebuggerPlugin instance
	 */
	static public StreamItDebuggerPlugin getDefault() {
		return inst;
	}
	
	public static String getUniqueIdentifier() {
		if (getDefault() == null) {
			// If the default instance is not yet initialized,
			// return a static identifier. This identifier must
			// match the plugin id defined in plugin.xml
			return "streamit.eclipse.debugger"; //$NON-NLS-1$
		}
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	
	public void startup() throws CoreException {
		super.startup();
		ResourcesPlugin.getWorkspace().addSaveParticipant(this, new StreamItSaveParticipant());
		WorkbenchPlugin.getDefault().getPreferenceStore().setValue(IPreferenceConstants.CLOSE_EDITORS_ON_EXIT, true);
		
		try {
			URL iconUrl = new URL(getDefault().getDescriptor().getInstallURL(), "icons/full/");
			ImageDescriptor desc = ImageDescriptor.createFromURL(new URL(iconUrl, "obj16/plus.gif"));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.PLUS_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, "obj16/minus.gif"));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.MINUS_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, "obj16/next_nav.gif"));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.UP_ARROW_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, "obj16/prev_nav.gif"));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.DOWN_ARROW_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, "ctool16/watchlist_view.gif"));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.SHOW_CHANNEL, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, "ctool16/faded_watchlist_view.gif"));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.FADED_SHOW_CHANNEL, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, "obj16/ref-27.gif"));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.HIGHLIGHT, desc);
			
		} catch (MalformedURLException e) {
		}
	}
	
	/** 
	 * Sets default preference values. These values will be used
	 * until some preferences are actually set using Preference dialog.
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		StreamItEditorPreferencePage.initializeDefaultPreferences(store);
	}
}
package streamit.eclipse.debugger;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import streamit.eclipse.debugger.core.LogFileManager;
import streamit.eclipse.debugger.launching.StreamItRelaunchListener;
import streamit.eclipse.debugger.texteditor.StreamItEditorPreferencePage;

/**
 * This is the top-level class of the Debugger plugin tool.
 * @author kkuo
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
			return IStreamItDebuggerPluginConstants.ID_STREAMIT_DEBUGGER_PLUGIN; //$NON-NLS-1$
		}
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	
	public void startup() throws CoreException {
		super.startup();
		
		try {
			// load images
			URL iconUrl = new URL(getDefault().getDescriptor().getInstallURL(), IStreamItDebuggerPluginConstants.BASE_PATH);
			ImageDescriptor desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.PLUS_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.PLUS_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.MINUS_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.MINUS_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.UP_ARROW_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.UP_ARROW_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.DOWN_ARROW_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.DOWN_ARROW_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.SHOW_CHANNEL_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.SHOW_CHANNEL_IMAGE, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.FADED_SHOW_CHANNEL_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.FADED_SHOW_CHANNEL_IMAGE, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.HIGHLIGHT_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.HIGHLIGHT_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.BREAKPOINT_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.BREAKPOINT_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.FIELD_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.FIELD_IMAGE, desc);
			
			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.METHOD_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.METHOD_IMAGE, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.PIPELINE_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.PIPELINE_IMAGE, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.SPLITJOIN_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.SPLITJOIN_IMAGE, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.FEEDBACKLOOP_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.FEEDBACKLOOP_IMAGE, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.FILTER_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.FILTER_IMAGE, desc);

			desc = ImageDescriptor.createFromURL(new URL(iconUrl, IStreamItDebuggerPluginConstants.DUPLICATE_IMAGE_PATH));
			getImageRegistry().put(IStreamItDebuggerPluginConstants.DUPLICATE_IMAGE, desc);
			
		} catch (MalformedURLException e) {
		}

		// .streamit file to communicate with StreamIt graph editor
		ResourcesPlugin.getWorkspace().addResourceChangeListener(LogFileManager.getInstance(), IResourceChangeEvent.POST_CHANGE);

		// add launch listener to compensate for deleting the real StreamIt Application thread
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		manager.addLaunchListener(StreamItRelaunchListener.getInstance());		
	}
	
	/** 
	 * Sets default preference values. These values will be used
	 * until some preferences are actually set using Preference dialog.
	 */
	protected void initializeDefaultPreferences(IPreferenceStore store) {
		StreamItEditorPreferencePage.initializeDefaultPreferences(store);
	}
}

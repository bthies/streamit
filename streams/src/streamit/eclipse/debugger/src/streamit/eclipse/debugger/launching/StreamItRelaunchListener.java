package streamit.eclipse.debugger.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;

import streamit.eclipse.debugger.core.PreDebuggingRunnable;
import streamit.eclipse.debugger.core.StreamItDebugEventFilter;

/**
 * @author kkuo
 */
public class StreamItRelaunchListener implements ILaunchListener {

	private static StreamItRelaunchListener fInstance = new StreamItRelaunchListener();
	private static StreamItApplicationLaunchShortcut fLaunchShortcut = new StreamItApplicationLaunchShortcut();
	
	/**
	 * Creates a new StreamItRelaunchListener.
	 */
	private StreamItRelaunchListener() {
	}
	
	/**
	 * Returns the singleton StreamItRelaunchListener.
	 */
	public static StreamItRelaunchListener getInstance() {
		return fInstance;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchRemoved(ILaunch launch) {
		// only handle java launched from streamit
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(config)) return;

		// remove launch from StreamItDebugEventFilter
		try {
			IFile javaFile = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFile(config);
			StreamItDebugEventFilter.getInstance().removeLaunch(javaFile);
		} catch (CoreException e) {
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchAdded(ILaunch launch) {
		// only handle java launched from streamit
		ILaunchConfiguration config = launch.getLaunchConfiguration();
		if (!StreamItLocalApplicationLaunchConfigurationDelegate.isStreamItLaunch(config)) return;
		
		try {			 
			if (config.getAttribute(IStreamItLaunchingConstants.ATTR_STR_LAUNCH, false)) {
				// the java launch was from streamit
				((ILaunchConfigurationWorkingCopy) config).setAttribute(IStreamItLaunchingConstants.ATTR_STR_LAUNCH, false);
				
			} else {
				String mode = launch.getLaunchMode();
				if (mode.equals(ILaunchManager.RUN_MODE)) return;

				// the java launch was not from streamit => add debugging stuff
				String projectName = StreamItLocalApplicationLaunchConfigurationDelegate.getProjectName(config);
				String mainClassName = StreamItLocalApplicationLaunchConfigurationDelegate.getMainClassName(config);
				String javaFileName = StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFileName(config);
				config = fLaunchShortcut.findLaunchConfiguration(projectName, mainClassName, mode);
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				IFile[] strFiles = StreamItLocalApplicationLaunchConfigurationDelegate.getStrFiles(config, project, mainClassName);
				IFile javaFile = project.getFile(javaFileName);
				String[] names = StreamItLocalApplicationLaunchConfigurationDelegate.strFilesToAbsoluteNames(strFiles, IStreamItLaunchingConstants.EMPTY_STRING, IStreamItLaunchingConstants.EMPTY_STRING);
				
				if (mode.equals(ILaunchManager.DEBUG_MODE)) {
					DebugUIPlugin.getStandardDisplay().syncExec(new PreDebuggingRunnable(project, javaFile.getLocation().toOSString(), javaFileName, javaFile, strFiles[0]));
				}
				
				// mark any existing streamit launch besides this launch as TO terminate
				ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
				ILaunch[] launches = manager.getLaunches();
				for (int i = 0; i < launches.length; i++) {
					if (launches[i].equals(launch)) continue;
					if (javaFileName.equals(StreamItLocalApplicationLaunchConfigurationDelegate.getJavaFileName(launches[i].getLaunchConfiguration()))) {
						launches[i].setAttribute(IStreamItLaunchingConstants.ATTR_KILL_LAUNCH, IStreamItLaunchingConstants.ATTR_KILL_LAUNCH);
					}
				}
			}
		} catch (CoreException e) {
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchChanged(ILaunch launch) {
	}
}
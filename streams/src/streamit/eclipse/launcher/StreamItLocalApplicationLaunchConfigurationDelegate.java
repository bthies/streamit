/*******************************************************************************
 * StreamIt Launcher
 * @author kkuo
 *******************************************************************************/

/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
//import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.debug.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;

/**
 * Launches a local VM.
 */
public class StreamItLocalApplicationLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private static String STR_FILE_EXTENSION = ".str";
	private static String JAVA_FILE_EXTENSION = ".java";

	/**
	 * @see ILaunchConfigurationDelegate#launch(ILaunchConfiguration, String, ILaunch, IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		// need to do something with ILaunch launch (add processes, etc)

		if (monitor == null) monitor = new NullProgressMonitor();
	
		try {
			// Get information from configuration
			String projectName = getJavaProjectName(configuration);
			String mainClassName = getMainTypeName(configuration);
			String strFileName = mainClassName + STR_FILE_EXTENSION;
			String javaFileName = mainClassName + JAVA_FILE_EXTENSION;
			String secondaryFileNames = configuration.getAttribute(StreamItLauncherPlugin.ATTR_SECONDARY_CLASSES, "");

			// run streamit.frontend.ToJava
			ILaunchManager manager = getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
			ILaunchConfigurationWorkingCopy wc = type.newInstance(null, "StrToJavaConfig");
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "streamit.frontend.ToJava");
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
							"--output " + javaFileName + " " + strFileName + " " + secondaryFileNames);
			ILaunchConfiguration config = wc.doSave();
			ILaunch tempLaunch = config.launch(ILaunchManager.RUN_MODE, monitor);
			IProcess[] processes = tempLaunch.getProcesses();
			while (!tempLaunch.isTerminated()) {}
			
			// show java file to user 
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

			// compile java file
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

			// run java code
			wc = type.newInstance(null, "ToJavaConfig");
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainClassName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, getProgramArguments(configuration));
			config = wc.doSave();
			tempLaunch = config.launch(ILaunchManager.RUN_MODE, monitor);
			while (!tempLaunch.isTerminated()) {}

		} catch (Exception e) {
			System.out.println("StreamIt Launch has failed.");
		}
	}
}
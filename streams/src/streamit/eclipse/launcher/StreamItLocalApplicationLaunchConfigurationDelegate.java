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
package streamit.eclipse.launcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaMethodBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.actions.ManageMethodBreakpointActionDelegate;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Launches a local VM.
 */
public class StreamItLocalApplicationLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	
	private static StreamItDebugEventSetListener listener = new StreamItDebugEventSetListener();
	
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
			String strFileName = mainClassName + '.' + StreamItLauncherPlugin.STR_FILE_EXTENSION;
			String javaFileName = mainClassName + '.' + StreamItLauncherPlugin.JAVA_FILE_EXTENSION;
			String secondaryFileNames = configuration.getAttribute(StreamItLauncherPlugin.ATTR_SECONDARY_CLASSES, "");

			// run streamit.frontend.ToJava
			ILaunchManager manager = getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
			ILaunchConfigurationWorkingCopy wc = type.newInstance(null, StreamItLauncherPlugin.STR_TO_JAVA_CONFIG);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, StreamItLauncherPlugin.TO_JAVA_CLASS);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
							StreamItLauncherPlugin.FILE_OUTPUT_OPTION + " " + javaFileName + " " + strFileName + " " + secondaryFileNames);
			ILaunchConfiguration config = wc.doSave();
			ILaunch tempLaunch = config.launch(ILaunchManager.RUN_MODE, monitor);
			IProcess[] processes = tempLaunch.getProcesses();
			while (!tempLaunch.isTerminated()) {}
	
			// show java file to user 
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

			// compile java file
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

			// in debug mode, add debug listener
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
				DebugPlugin.getDefault().addDebugEventListener(listener);			

				// add breakpoint at end of init function for top level class
				DebugUIPlugin.getStandardDisplay().syncExec(addBreakpoints(project, javaFileName));
			}

			// run java code
			wc = type.newInstance(null, StreamItLauncherPlugin.ID_STR_APPLICATION);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainClassName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, getProgramArguments(configuration));
			config = wc.doSave();
			tempLaunch = config.launch(mode, monitor);
			setDefaultSourceLocator(tempLaunch, config);

		} catch (Exception e) {
		}	
	}
	
	public void launchJava(ILaunchConfiguration configuration) throws CoreException {
		String projectName = getJavaProjectName(configuration);
		String mainClassName = getMainTypeName(configuration);
		String strFileName = mainClassName + '.' + StreamItLauncherPlugin.STR_FILE_EXTENSION;
		String javaFileName = mainClassName + '.' + StreamItLauncherPlugin.JAVA_FILE_EXTENSION;
		String secondaryFileNames = configuration.getAttribute(StreamItLauncherPlugin.ATTR_SECONDARY_CLASSES, "");

		// run streamit.frontend.ToJava
		ILaunchManager manager = getLaunchManager();
		ILaunchConfigurationType type = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
		ILaunchConfigurationWorkingCopy wc = type.newInstance(null, StreamItLauncherPlugin.STR_TO_JAVA_CONFIG);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, StreamItLauncherPlugin.TO_JAVA_CLASS);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
						StreamItLauncherPlugin.FILE_OUTPUT_OPTION + " " + javaFileName + " " + strFileName + " " + secondaryFileNames);
		ILaunchConfiguration config = wc.doSave();
		ILaunch tempLaunch = config.launch(ILaunchManager.RUN_MODE, null);
		IProcess[] processes = tempLaunch.getProcesses();
		while (!tempLaunch.isTerminated()) {}
	
		// show java file to user 
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}
	
	protected Runnable addBreakpoints(final IProject project, final String javaFileName) {
		return new Runnable() {

			private List methodBreakpoints; 

			public void run() {
				try {
					// get all types
					IFile javaFile = project.getFile(javaFileName);
					ICompilationUnit unit = (ICompilationUnit) JavaCore.create(javaFile);
					synchronized (unit) {
						unit.reconcile();
					}
					IType[] types = unit.getAllTypes();
					
					// add method breakpoint to all init and work functions
					StreamItDebugEventSetListener.beforeLaunch();
					setMethodBreakpoints(StreamItLauncherPlugin.ID_JAVA_METHOD_BREAKPOINT);
					for (int i = 0; i < types.length; i++) {
						String superclassName = types[i].getSuperclassName();
						
						// get filter name & variables 
						if (superclassName.equals(StreamItLauncherPlugin.FILTER_CLASS)) {							
							IField[] fields = fields = types[i].getFields();
							String[] vars = new String[fields.length];
							for (int j = 0; j < fields.length; j++) vars[j] = fields[j].getElementName();
							StreamItDebugEventSetListener.addFilter(types[i].getElementName(), vars);
						} else if (superclassName.equals(StreamItLauncherPlugin.STREAMIT_CLASS)) continue;
						
						// add breakpoints to all functions
						IMethod[] methods = types[i].getMethods();
						String methodName;
						boolean entry, exit;
						int methodType;
						for (int j = 0; j < methods.length; j++) {
							methodName = methods[j].getElementName();
							
							if (methodName.equals(StreamItLauncherPlugin.INIT_METHOD)) { // exit only
								entry = false;
								exit = true;
								methodType = StreamItDebugEventSetListener.INIT_BREAKPOINTS;
							} else if (methodName.equals(StreamItLauncherPlugin.WORK_METHOD)) { // entry & exit
								entry = true;
								exit = true;
								methodType = StreamItDebugEventSetListener.WORK_BREAKPOINTS;
							} else if (methodName.equals(StreamItLauncherPlugin.PREWORK_METHOD)) { // entry only
								entry = true;
								exit = false;
								methodType = StreamItDebugEventSetListener.PREWORK_BREAKPOINTS;
							} else { 															// entry & exit
								entry = true;
								exit = true;
								methodType = StreamItDebugEventSetListener.OTHER_BREAKPOINTS;
							}
							
							// add method breakpoint to .java
							int methodStart = methods[j].getNameRange().getOffset();
							int methodEnd = methodStart + methods[j].getNameRange().getLength();
							String typeName = types[i].getFullyQualifiedName();

							// handle if method breakpoint already exists
							IJavaMethodBreakpoint breakpoint = getMethodBreakpoint(typeName, methodStart, methodEnd);
							if (breakpoint == null) {
								Map attributes = new HashMap(10);
								BreakpointUtils.addJavaBreakpointAttributes(attributes, methods[j]);
								methodName = methods[j].getElementName();
								String methodSignature = methods[j].getSignature();
								if (!types[i].isBinary()) methodSignature = ManageMethodBreakpointActionDelegate.resolveMethodSignature(types[i], methodSignature);
								StreamItDebugEventSetListener.addBreakpoint(methodType, true, JDIDebugModel.createMethodBreakpoint(javaFile, typeName, methodName, methodSignature, entry, exit, false, -1, methodStart, methodEnd, 0, true, attributes));
							} else {
								StreamItDebugEventSetListener.addBreakpoint(methodType, !breakpoint.isEntry(), breakpoint);
								breakpoint.setEntry(entry);
								breakpoint.setExit(exit);
							}
						}
					}
				} catch (Exception e) {
				}
			}
			
			private void setMethodBreakpoints(String markerType) throws CoreException {
				methodBreakpoints = new Vector();

				IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
				IBreakpoint[] breakpoints = manager.getBreakpoints(JDIDebugModel.getPluginIdentifier());
				for (int i = 0; i < breakpoints.length; i++) {
					if (!(breakpoints[i] instanceof IJavaMethodBreakpoint)) continue;
					IJavaMethodBreakpoint breakpoint = (IJavaMethodBreakpoint) breakpoints[i];
					if (breakpoint.getMarker().getType().equals(markerType)) methodBreakpoints.add(breakpoint);

				}
			}
			
			private IJavaMethodBreakpoint getMethodBreakpoint(String typeName, int methodStart, int methodEnd) throws CoreException {
				for (int i = 0; i < methodBreakpoints.size(); i++) {
					IJavaMethodBreakpoint breakpoint = (IJavaMethodBreakpoint) methodBreakpoints.get(i);
					if (breakpoint.getTypeName().equals(typeName)
						&& breakpoint.getCharStart() == methodStart && breakpoint.getCharEnd() == methodEnd) {
							return breakpoint;
					}
				}
				return null;
			}
			
		};
	}			

}
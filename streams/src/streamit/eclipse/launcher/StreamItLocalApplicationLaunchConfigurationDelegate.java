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
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.debug.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * Launches a local VM.
 */
public class StreamItLocalApplicationLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

	private static String JAVA_FILE_EXTENSION = "java";
	private static StreamItDebugEventSetListener listener = new StreamItDebugEventSetListener();
	
	protected class StrFilter {
		private String name;
		private String[] fields;
		
		private String inputType;
		private String outputType;

		private int numPops;
		private int numPushes;
		private int numPeeks;

		private int numPhases;
		
		protected StrFilter(String n, String[] f) {
			name = n;
			fields = f;
		}
		
		protected void print() {
			System.out.println("Filter " + name);
			System.out.println("Filter input type "  + inputType);
			System.out.println("Filter output type " + outputType);
			System.out.println("Filter fields :");
			for (int i = 0; i < fields.length; i++) {
				System.out.println("\t " + fields[i]);
			}			
		}
		
		protected String getName() {
			return name;
		}
		
		protected void setInputType(String input) {
			inputType = input;
		}

		protected void setOutputType(String output) {
			outputType = output;
		}
		

	}
	
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
			String javaFileName = mainClassName + '.' + JAVA_FILE_EXTENSION;
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

			// in debug mode, add debug listener
			if (mode.equals(ILaunchManager.DEBUG_MODE)) {
				DebugPlugin.getDefault().addDebugEventListener(listener);
				
				// test:  process .java
				//prepareFilterDisplay(project.getFile(javaFileName), project.getFile(strFileName));
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
			System.out.println("StreamIt Launch has failed.");
		}	
	}
	
	public void launchJava(ILaunchConfiguration configuration) throws CoreException {
		String projectName = getJavaProjectName(configuration);
		String mainClassName = getMainTypeName(configuration);
		String strFileName = mainClassName + '.' + StreamItLauncherPlugin.STR_FILE_EXTENSION;
		String javaFileName = mainClassName + '.' + JAVA_FILE_EXTENSION;
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
		ILaunch tempLaunch = config.launch(ILaunchManager.RUN_MODE, null);
		IProcess[] processes = tempLaunch.getProcesses();
		while (!tempLaunch.isTerminated()) {}
	
		// show java file to user 
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}
		
	protected void prepareFilterDisplay(IFile javaFile, IFile strFile) {
	
		// by this point .java must have compiled correctly, or will fail anyway
		try {
			// get all java types in .java 
			ICompilationUnit unit = (ICompilationUnit) JavaCore.create(javaFile);
			synchronized (unit) {
				unit.reconcile();
			}
			IType[] types = unit.getAllTypes();
						
			// find each filter's name and its state variables
			Vector filters = new Vector(); // filter names in order of source
			String[] vars;
			IField[] fields;
			IMethod[] methods;
			for (int i = 0; i < types.length; i++) {
				if (types[i].getSuperclassName().equals(StreamItLauncherPlugin.FILTER_CLASS)) {
					fields = types[i].getFields();					
					vars = new String[fields.length];
					for (int j = 0; j < fields.length; j++) vars[j] = fields[j].getElementName();					
					filters.add(new StrFilter(types[i].getElementName(), vars));
				}
			}

			findInfoInStr(strFile, filters);

			// debug statement
			for (int i = 0; i < filters.size(); i++) ((StrFilter) filters.get(i)).print();
		} catch (Exception e) {	
		}
	}
	
	protected void findInfoInStr(IFile strFile, Vector filters) throws Exception {
	
		// for reading .str
		InputStreamReader isr = new InputStreamReader(strFile.getContents());
		BufferedReader br = new BufferedReader(isr);
		String strLine = null;	
		StringBuffer buf = new StringBuffer();
		StringTokenizer st;
		String reverse, forward;

		// temporary info storage
		StrFilter filter = null;
		StrFilter f;
		String name, input, output;

		// tokens
		boolean arrowSeen = false;
		boolean bracketSeen = false;
		boolean workSeen = false;
		boolean preworkSeen = false;
		boolean phaseSeen = false;
		String arrow = "->";
		String bracket = "{";
		String work = "work";
		String prework = "prework";
		String phase = "phase";		

		for (strLine = br.readLine(); strLine != null; strLine = br.readLine()) {
			if (!arrowSeen && strLine.indexOf(arrow) != -1) arrowSeen = true;
			if (!bracketSeen && strLine.indexOf(bracket) != -1) bracketSeen = true;
			if (arrowSeen && bracketSeen) { 
				arrowSeen = false;
				bracketSeen = false;
				buf.append(strLine);

				// find what's after arrow
				forward = buf.toString();
				st = new StringTokenizer(forward.substring(forward.indexOf(arrow) + arrow.length()));
				if (st.countTokens() >= 3) output = st.nextToken();
				else return;
				
				// make sure you're getting a filter
				if (st.nextToken().toLowerCase().equals(StreamItLauncherPlugin.FILTER_KEYWORD)) {
					name = st.nextToken();
					
					// add to filters
					for (int i = 0; i < filters.size(); i++) {
						f = (StrFilter) filters.get(i);
						if (f.getName().equals(name)) {
							filter = f;							
							filter.setOutputType(output);
						}
					}
					if (filter == null) break;
					
					// find what's before arrow
					reverse = buf.reverse().toString();
					st = new StringTokenizer(reverse.substring(reverse.indexOf(">-") + 2));
					buf = new StringBuffer(st.nextToken());
					filter.setInputType(buf.reverse().toString());					
					
					filter = null;
				}
				
				// keep going
				buf = new StringBuffer(strLine.substring(strLine.indexOf(bracket) + bracket.length()));
			} else {
				buf.append(strLine + " ");
			}
		}
	}	
}

/*

					
					1.7.2 Number of pops, pushes, peeks - find at work/phase/prework function (between keyword && {
					1.7.4 Number of phases, current phase - find within work function (phase is a sub-work function)




					1.7.5 Current stage - either initial stage (pre-work) or steady state (work)
					1.7.6 Number of executions of work
					1.7.7 Tabular highlighting - highlight currently firing filter
*/

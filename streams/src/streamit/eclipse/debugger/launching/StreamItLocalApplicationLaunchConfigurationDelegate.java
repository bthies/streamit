/*******************************************************************************
 * StreamIt Launcher
 * @author kkuo
 *******************************************************************************/
package streamit.eclipse.debugger.launching;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import streamit.eclipse.debugger.core.PreDebuggingRunnable;

public class StreamItLocalApplicationLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

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
			String strFileName = mainClassName + '.' + IStreamItLaunchingConstants.STR_FILE_EXTENSION;
			String javaFileName = mainClassName + '.' + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION;
			String secondaryFileNames = configuration.getAttribute(IStreamItLaunchingConstants.ATTR_SECONDARY_CLASSES, "");
			
			// run streamit.frontend.ToJava
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			StringTokenizer st = new StringTokenizer(secondaryFileNames);
			String[] args = new String[3 + st.countTokens()];
			args[0] = IStreamItLaunchingConstants.FILE_OUTPUT_OPTION;
			args[2] = project.getFile(strFileName).getLocation().toOSString();
			IFile javaFile = project.getFile(javaFileName);
			javaFile.setReadOnly(false);
			args[1] = javaFile.getLocation().toOSString();
			for (int i = 3; st.hasMoreTokens(); i++) {
				args[i] = project.getFile(st.nextToken()).getLocation().toOSString();
			}
			
			// hijack error stream
			PrintStream err = System.err;
			ByteArrayOutputStream newErr = new ByteArrayOutputStream();
			System.setErr(new PrintStream(newErr));

			// run streamit.frontend.ToJava
			streamit.frontend.ToJava.main(args);
			javaFile.setReadOnly(true);

			// restore error stream
			System.setErr(err);
			String errors = newErr.toString();
			if (!errors.equals("")) {
				reportCreatingJavaFile(errors);
				return;
			} 			

			// show java file to user (doesn't hide .java, but fixes resource problems)
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			
			// compile java file
			/* for trying to debug .java headless
			IJavaProject javaProject = JavaCore.create(project);
			Map options = javaProject.getOptions(false);
			options.put(JavaCore.COMPILER_SOURCE_FILE_ATTR, JavaCore.DO_NOT_GENERATE);
			javaProject.setOptions(options);
			*/
			
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

			if (mode.equals(ILaunchManager.DEBUG_MODE))
				DebugUIPlugin.getStandardDisplay().syncExec(new PreDebuggingRunnable(project, args[1], javaFileName, javaFile));

			// run java code
			ILaunchManager manager = getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
			ILaunchConfigurationWorkingCopy wc = type.newInstance(null, configuration.getName() + IStreamItLaunchingConstants.ID_STR_APPLICATION);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainClassName);
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, getProgramArguments(configuration));
			ILaunch tempLaunch = wc.launch(mode, monitor);
			setDefaultSourceLocator(tempLaunch, wc);

		} catch (Exception e) {
		}	
	}
	
	public boolean launchJava(ILaunchConfiguration configuration) throws CoreException {
		String projectName = getJavaProjectName(configuration);
		String mainClassName = getMainTypeName(configuration);
		String strFileName = mainClassName + '.' + IStreamItLaunchingConstants.STR_FILE_EXTENSION;
		String javaFileName = mainClassName + '.' + IStreamItLaunchingConstants.JAVA_FILE_EXTENSION;
		String secondaryFileNames = configuration.getAttribute(IStreamItLaunchingConstants.ATTR_SECONDARY_CLASSES, "");

		// prepare to run streamit.frontend.ToJava
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		StringTokenizer st = new StringTokenizer(secondaryFileNames);
		String[] args = new String[3 + st.countTokens()];
		args[0] = IStreamItLaunchingConstants.FILE_OUTPUT_OPTION;
		args[2] = project.getFile(strFileName).getLocation().toOSString();
		IFile javaFile = project.getFile(javaFileName);
		args[1] = javaFile.getLocation().toOSString();
		for (int i = 3; st.hasMoreTokens(); i++) {
			args[i] = project.getFile(st.nextToken()).getLocation().toOSString();
		}
		
		// hijack error stream
		PrintStream err = System.err;
		ByteArrayOutputStream newErr = new ByteArrayOutputStream();
		System.setErr(new PrintStream(newErr));

		// run streamit.frontend.ToJava
		streamit.frontend.ToJava.main(args);
			
		// restore error stream
		System.setErr(err);
		String errors = newErr.toString();
		if (errors.equals("")) return true;
		reportCreatingJavaFile(errors);
 
		return false;
	}
	
	protected void reportCreatingJavaFile(final String error) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				
				MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationLaunchShortcut.Error_Launching_1"), 
				LaunchingMessages.getString("JavaApplicationLaunchShortcut.Exception") + "\n" +
				error.substring(0, error.indexOf("\n"))); //new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, exception.getMessage(), exception)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
	}
	
	protected Shell getShell() {
		return JDIDebugUIPlugin.getActiveWorkbenchShell();
	}

}
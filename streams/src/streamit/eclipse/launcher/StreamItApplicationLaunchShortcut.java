/*******************************************************************************
 * StreamIt Launcher adapted from 
 * org.eclipse.jdt.internal.debug.ui.launcher.JavaApplicationLaunchShortcut
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.jdt.internal.debug.ui.launcher.*;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Performs single click launching for local Java applications.
 */
public class StreamItApplicationLaunchShortcut implements ILaunchShortcut {

	/**
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IResource resource = getResource(editor);
		if (resource == null) MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7")); //$NON-NLS-1$ //$NON-NLS-2$

		launch(resource, mode);
	}

	protected void launch(IResource resource, String mode) {
		IProject project = resource.getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				String strFileName = resource.getName();
				ILaunchConfiguration config = findLaunchConfiguration(project.getName(), strFileName.substring(0, strFileName.lastIndexOf('.' + StreamItLauncherPlugin.STR_FILE_EXTENSION)), mode);
				if (config != null) {
					DebugUITools.launch(config, mode);
				}
			} else {
				MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		} catch (Exception e) {
			MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7")); //$NON-NLS-1$ //$NON-NLS-2$
		}	
	}

	/**
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			Object[] selections = ((IStructuredSelection)selection).toArray();
			for (int i = 0; i < selections.length; i++) {
				if (selections[i] instanceof IAdaptable) {
					if (selections[i] instanceof IFile) {				
						launch((IResource) selections[i], mode);
						return;
					} else if (selections[i] instanceof IJavaElement) {
						IJavaProject project = ((IJavaElement) selections[i]).getJavaProject();
						try {
							Object[] nonJavaResources = project.getNonJavaResources();
							List files = new ArrayList(nonJavaResources.length);
							for (int j = 0; j < nonJavaResources.length; j++) {
								if (nonJavaResources[j] instanceof IFile) {
									if (((IFile) nonJavaResources[j]).getFileExtension().equals(StreamItLauncherPlugin.STR_FILE_EXTENSION)) {
										files.add((IFile) nonJavaResources[j]);			
									}
								}
							}
							IResource resource = chooseStreamItFile(files, mode);
							if (resource != null) launch(resource, mode);
							return;
						} catch (Exception e) {
							MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7")); //$NON-NLS-1$ //$NON-NLS-2$
							return;							
						}
					}
				}
			}	
		}
		MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}	

	protected Shell getShell() {
		return JDIDebugUIPlugin.getActiveWorkbenchShell();
	}

	protected ILaunchConfigurationType getStreamItLaunchConfigType() {
		return getLaunchManager().getLaunchConfigurationType(StreamItLauncherPlugin.ID_STR_APPLICATION);		
	}
	
	protected IResource getResource(IEditorPart editor) {
		IEditorInput input= editor.getEditorInput();
		IResource resource= (IResource) input.getAdapter(IFile.class);		
		if (resource == null) {
			resource= (IResource) input.getAdapter(IResource.class);
		}		
		return resource;
	}

	public ILaunchConfiguration findLaunchConfiguration(String projectName, String mainTypeName, String mode) {
		ILaunchConfigurationType configType = getStreamItLaunchConfigType();
		List candidateConfigs = Collections.EMPTY_LIST;
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs = new ArrayList(configs.length);
			for (int i = 0; i < configs.length; i++) {
				ILaunchConfiguration config = configs[i];
				if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(projectName)) //$NON-NLS-1$
					candidateConfigs.add(config);
			}
		} catch (CoreException e) {
			JDIDebugUIPlugin.log(e);
		}
		
		// If there are no existing configs associated with the IType, create one.
		// If there is exactly one config associated with the IType, return it.
		// Otherwise, if there is more than one config associated with the IType, prompt the
		// user to choose one.
		int candidateCount = candidateConfigs.size();
		if (candidateCount < 1) {
			return createConfiguration(projectName, mainTypeName);
		} else if (candidateCount == 1) {
			return (ILaunchConfiguration) candidateConfigs.get(0);
		} else {
			// Prompt the user to choose a config.  A null result means the user
			// cancelled the dialog, in which case this method returns null,
			// since cancelling the dialog should also cancel launching anything.
			ILaunchConfiguration config = chooseConfiguration(candidateConfigs, mode);
			if (config != null) {
				return config;
			}
		}
		
		return null;
	}
	
	protected ILaunchConfiguration createConfiguration(String projectName, String mainTypeName) {
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try {
			ILaunchConfigurationType configType = getStreamItLaunchConfigType();
			wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(mainTypeName));
		} catch (CoreException exception) {
			reportCreatingConfiguration(exception);
			return null;		
		} 
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, mainTypeName);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
		wc.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		wc.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "-i 10");
		try {
			config = wc.doSave();		
		} catch (CoreException exception) {
			reportCreatingConfiguration(exception);			
		}
		return config;
	}
	
	protected void reportCreatingConfiguration(final CoreException exception) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationLaunchShortcut.Error_Launching_1"), LauncherMessages.getString("JavaApplicationLaunchShortcut.Exception"), exception.getStatus()); //new Status(IStatus.ERROR, JDIDebugUIPlugin.getUniqueIdentifier(), IStatus.ERROR, exception.getMessage(), exception)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
	}

	protected ILaunchConfiguration chooseConfiguration(List configList, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(configList.toArray());
		dialog.setTitle(LauncherMessages.getString("JavaApplicationAction.Launch_Configuration_Selection_1"));  //$NON-NLS-1$
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(LauncherMessages.getString("JavaApplicationAction.Choose_a_launch_configuration_to_debug_2"));  //$NON-NLS-1$
		} else {
			dialog.setMessage(LauncherMessages.getString("JavaApplicationAction.Choose_a_launch_configuration_to_run_3")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == MainTypeSelectionDialog.OK) {
			return (ILaunchConfiguration) dialog.getFirstResult();
		}
		return null;		
	}
	
	protected IResource chooseStreamItFile(List files, String mode) {
		IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), labelProvider);
		dialog.setElements(files.toArray());
		dialog.setTitle(LaunchingMessages.getString("StreamItApplicationAction.StreamIt_File_Selection_1"));  //$NON-NLS-1$
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setMessage(LaunchingMessages.getString("StreamItApplicationAction.Choose_a_file_to_debug_2"));  //$NON-NLS-1$
		} else {
			dialog.setMessage(LaunchingMessages.getString("StreamItApplicationAction.Choose_a_file_to_run_3")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		int result = dialog.open();
		labelProvider.dispose();
		if (result == MainTypeSelectionDialog.OK) {
			return (IResource) dialog.getFirstResult();
		}
		return null;		
	}

}
/*******************************************************************************
 * StreamIt Launcher adapted from 
 * org.eclipse.jdt.internal.debug.ui.launcherJavaApplicationLaunchShortcut
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

import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.ui.JavaUISourceLocator;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import org.eclipse.jdt.internal.debug.ui.launcher.*;

/**
 * Performs single click launching for local Java applications.
 */
public class StreamItApplicationLaunchShortcut implements ILaunchShortcut {
	
	/**
	 * @param search the java elements to search for a main type
	 * @param mode the mode to launch in
	 * @param whether activated on an editor (or from a selection in a viewer)
	 */
	public void searchAndLaunch(Object[] search, String mode, boolean editor) {
		IType[] types = null;
		if (search != null) {
			try {
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
				types = MainMethodFinder.findTargets(dialog, search);
			} catch (InterruptedException e) {
				return;
			} catch (InvocationTargetException e) {
				MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), e.getMessage()); //$NON-NLS-1$
				return;
			}
			IType type = null;
			if (types.length == 0) {
				String message = null;
				if (editor) {
					message = LauncherMessages.getString("JavaApplicationLaunchShortcut.The_active_editor_does_not_contain_a_main_type._1"); //$NON-NLS-1$
				} else {
					message = LauncherMessages.getString("JavaApplicationLaunchShortcut.The_selection_does_not_contain_a_main_type._2"); //$NON-NLS-1$
				}
				MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), message); //$NON-NLS-1$
			} else if (types.length > 1) {
				type = chooseType(types, mode);
			} else {
				type = types[0];
			}
			if (type != null) {
				launch(type, mode);
			}
		}

	}	

	/**
	 * Prompts the user to select a type
	 * 
	 * @return the selected type or <code>null</code> if none.
	 */
	protected IType chooseType(IType[] types, String mode) {
		MainTypeSelectionDialog dialog= new MainTypeSelectionDialog(getShell(), types);		
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			dialog.setTitle(LauncherMessages.getString("JavaApplicationAction.Type_Selection_Debug")); //$NON-NLS-1$
		} else {
			dialog.setTitle(LauncherMessages.getString("JavaApplicationAction.Type_Selection_Run")); //$NON-NLS-1$
		}
		dialog.setMultipleSelection(false);
		if (dialog.open() == MainTypeSelectionDialog.OK) {
			return (IType)dialog.getFirstResult();
		}
		return null;
	}
	
	/**
	 * Launches a configuration for the given type
	 */
	protected void launch(IType type, String mode) {
		ILaunchConfiguration config = findLaunchConfiguration(type, mode);
		if (config != null) {
			DebugUITools.launch(config, mode);
		}			
	}
	
	/**
	 * Locate a configuration to relaunch for the given type.  If one cannot be found, create one.
	 * 
	 * @return a re-useable config or <code>null</code> if none
	 */
	protected ILaunchConfiguration findLaunchConfiguration(IType type, String mode) {
		ILaunchConfigurationType configType = getJavaLaunchConfigType();
		List candidateConfigs = Collections.EMPTY_LIST;
		try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(configType);
			candidateConfigs = new ArrayList(configs.length);
			for (int i = 0; i < configs.length; i++) {
				ILaunchConfiguration config = configs[i];
				if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "").equals(type.getFullyQualifiedName())) { //$NON-NLS-1$
					if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(type.getJavaProject().getElementName())) { //$NON-NLS-1$
						candidateConfigs.add(config);
					}
				}
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
			return createConfiguration(type);
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
	
	/**
	 * Show a selection dialog that allows the user to choose one of the specified
	 * launch configurations.  Return the chosen config, or <code>null</code> if the
	 * user cancelled the dialog.
	 */
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
	
	/**
	 * Create & return a new configuration based on the specified <code>IType</code>.
	 */
	protected ILaunchConfiguration createConfiguration(IType type) {
		ILaunchConfiguration config = null;
		ILaunchConfigurationWorkingCopy wc = null;
		try {
			ILaunchConfigurationType configType = getJavaLaunchConfigType();
			wc = configType.newInstance(null, getLaunchManager().generateUniqueLaunchConfigurationNameFrom(type.getElementName()));
		} catch (CoreException exception) {
			reportCreatingConfiguration(exception);
			return null;		
		} 
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, type.getFullyQualifiedName());
		wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, type.getJavaProject().getElementName());
		wc.setAttribute(IDebugUIConstants.ATTR_TARGET_DEBUG_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		wc.setAttribute(IDebugUIConstants.ATTR_TARGET_RUN_PERSPECTIVE, IDebugUIConstants.PERSPECTIVE_DEFAULT);
		wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, JavaUISourceLocator.ID_PROMPTING_JAVA_SOURCE_LOCATOR);
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
	
	/**
	 * Returns the local java launch config type
	 */
	protected ILaunchConfigurationType getJavaLaunchConfigType() {
		return getLaunchManager().getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);		
	}
	
	protected ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}
	
	/**
	 * Convenience method to get the window that owns this action's Shell.
	 */
	protected Shell getShell() {
		return JDIDebugUIPlugin.getActiveWorkbenchShell();
	}
	
	/**
	 * @see ILaunchShortcut#launch(IEditorPart, String)
	 */
	public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IJavaElement je = (IJavaElement) input.getAdapter(IJavaElement.class);
		if (je != null) {
			searchAndLaunch(new Object[] {je}, mode, true);
		} else {
			MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), LauncherMessages.getString("JavaApplicationLaunchShortcut.The_active_editor_does_not_contain_a_main_type._1")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
	}

	/**
	 * @see ILaunchShortcut#launch(ISelection, String)
	 */
	public void launch(ISelection selection, String mode) {
		if (selection instanceof IStructuredSelection) {
			searchAndLaunch(((IStructuredSelection)selection).toArray(), mode, false);
		} else {
			MessageDialog.openError(getShell(), LauncherMessages.getString("JavaApplicationAction.Launch_failed_7"), LauncherMessages.getString("JavaApplicationLaunchShortcut.The_selection_does_not_contain_a_main_type._2")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}

/*******************************************************************************
 * StreamIt Debugger adapted from
 * org.eclipse.jdt.internal.debug.ui.actions.EnableDisableBreakpointRulerActionDelegate
 * modifier - Kimberly Kuo
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
package texteditor;

import org.eclipse.jdt.internal.debug.ui.actions.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import launcher.StreamItApplicationLaunchShortcut;
import launcher.StreamItLauncherPlugin;
import launcher.StreamItLocalApplicationLaunchConfigurationDelegate;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorPart;

import launcher.StreamItDebugEventSetListener;

public class EnableDisableBreakpointRulerAction extends AbstractBreakpointRulerAction {
	
	private IBreakpoint fJavaBreakpoint;

	// From texteditor.ManageBreakpointRulerAction
	protected String fStrFileName;
	protected String fJavaFileName;
	protected String fStrName;
	protected IJavaElement fJavaFile;
	protected IEditorPart fJavaEditorPart;
	protected HashMap fStrToJava;
	protected HashMap fJavaToStr;
	protected StreamItApplicationLaunchShortcut launchShortcut;
	protected StreamItLocalApplicationLaunchConfigurationDelegate launchDelegate;

	/**
	 * Creates the action to enable/disable breakpoints
	 */
	public EnableDisableBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo info) {
		setInfo(info);
		setTextEditor(editor);
		setText(ActionMessages.getString("EnableDisableBreakpointRulerAction.&Enable_Breakpoint_1")); //$NON-NLS-1$

		// From texteditor.ManageBreakpointRulerAction
		fStrFileName = getResource().getName();
		fJavaFileName = fStrFileName.substring(0, fStrFileName.lastIndexOf('.' + StreamItLauncherPlugin.STR_FILE_EXTENSION)) + ".java";
		fStrName = fStrFileName.substring(0, fStrFileName.lastIndexOf('.' + StreamItLauncherPlugin.STR_FILE_EXTENSION));
		fJavaFile = null;
		fJavaEditorPart = null;
		fStrToJava = null;
		launchShortcut = new StreamItApplicationLaunchShortcut();
		launchDelegate = new StreamItLocalApplicationLaunchConfigurationDelegate();
	}

	/**
	 * @see Action#run()
	 */
	public void run() {
		if (getBreakpoint() != null) {
			try {
				getBreakpoint().setEnabled(!getBreakpoint().isEnabled());
				getJavaBreakpoint().setEnabled(!getJavaBreakpoint().isEnabled());			
			} catch (CoreException e) {
				ErrorDialog.openError(getTextEditor().getEditorSite().getShell(), ActionMessages.getString("EnableDisableBreakpointRulerAction.Enabling/disabling_breakpoints_2"), ActionMessages.getString("EnableDisableBreakpointRulerAction.Exceptions_occurred_enabling_disabling_the_breakpoint_3"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		JavaPlugin.getActivePage().bringToTop(getTextEditor());
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		setBreakpoint(determineBreakpoint());
		setJavaBreakpoint(determineJavaBreakpoint());

		if (getBreakpoint() == null) {
			setEnabled(false);
			return;
		}
		setEnabled(true);
		try {
			boolean enabled= getBreakpoint().isEnabled();
			setText(enabled ? ActionMessages.getString("EnableDisableBreakpointRulerAction.&Disable_Breakpoint_4") : ActionMessages.getString("EnableDisableBreakpointRulerAction.&Enable_Breakpoint_5")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (CoreException ce) {
			JDIDebugUIPlugin.log(ce);
		}
		JavaPlugin.getActivePage().bringToTop(getTextEditor());
	}
	
	protected IBreakpoint getJavaBreakpoint() {
		return fJavaBreakpoint;
	}

	protected void setJavaBreakpoint(IBreakpoint breakpoint) {
		fJavaBreakpoint = breakpoint;
	}

	protected IBreakpoint determineJavaBreakpoint() {
		IBreakpoint[] breakpoints= DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(JDIDebugPlugin.getUniqueIdentifier());
		for (int i= 0; i < breakpoints.length; i++) {
			IBreakpoint breakpoint= breakpoints[i];
			if (breakpoint instanceof IJavaLineBreakpoint) {
				IJavaLineBreakpoint jBreakpoint= (IJavaLineBreakpoint)breakpoint;
				try {
					if (javaBreakpointAtRulerLine(jBreakpoint)) {
						return jBreakpoint;
					}
				} catch (CoreException ce) {
					JDIDebugUIPlugin.log(ce);
					continue;
				}
			}
		}
		return null;
	}
	

	protected boolean javaBreakpointAtRulerLine(IJavaLineBreakpoint jBreakpoint) throws CoreException {
		AbstractMarkerAnnotationModel model = getJavaAnnotationModel();
		if (model != null) {
			Position position= model.getMarkerPosition(jBreakpoint.getMarker());
			if (position != null) {
				IDocument doc = getJavaDocument();
				try {
					int markerLineNumber= doc.getLineOfOffset(position.getOffset());
					int rulerLine= getInfo().getLineOfLastMouseButtonActivity() + 1;
					
					// find mapping to .java
					Iterator javaRulerLine;
					Object isThere = fStrToJava.get(new Integer(rulerLine));
					if (isThere == null) return false;
					else javaRulerLine = ((Set) isThere).iterator();
					
					
					while (javaRulerLine.hasNext()) {
						if (((Integer) javaRulerLine.next()).intValue() - 1 == markerLineNumber) {
							if (getTextEditor().isDirty()) {
								return jBreakpoint.getLineNumber() == markerLineNumber + 1;
							}						
							return true;
						}
					}
				} catch (BadLocationException x) {
					JDIDebugUIPlugin.log(x);
				}
			}
		}
		
		return false;
	}
	
	// From texteditor.ManageBreakpointRulerAction
	protected IEditorPart getJavaEditorPart() {
		if (fJavaEditorPart == null) openJavaFile();
		return fJavaEditorPart;
	}
	
	protected IJavaElement getJavaFile() {
		if (fJavaFile == null) openJavaFile();
		return fJavaFile;
	}
	
	protected void openJavaFile() {
		IProject project = getResource().getProject();
		IFile javaFile = project.getFile(fJavaFileName);

		try {
			if (!javaFile.exists() && project.hasNature(JavaCore.NATURE_ID)) {
				ILaunchConfiguration configuration = launchShortcut.findLaunchConfiguration(project.getName(), fStrName, ILaunchManager.RUN_MODE);
				launchDelegate.launchJava(configuration);
				javaFile = project.getFile(fJavaFileName);
			}
			fJavaFile = JavaCore.create(javaFile);
			fJavaEditorPart = JavaUI.openInEditor(fJavaFile);
			mapStrToJava(javaFile);
		} catch (Exception e) {	
		}
	}
	
	protected void mapStrToJava(IFile javaFile) {
		fStrToJava = new HashMap();
		fJavaToStr = new HashMap();
		
		try {
			// look for commented mappings
			InputStreamReader isr = new InputStreamReader(javaFile.getContents());
			BufferedReader br = new BufferedReader(isr);
			String strLine = null;
			int javaLineNumber = 0;
			String token = "// " + fStrFileName + ':';
			
			while (true) {
				javaLineNumber++;
				strLine = br.readLine();
				if (strLine == null) return;
				if (strLine.indexOf(token) != -1) {
					Integer strLineNumber = Integer.valueOf(strLine.substring(strLine.indexOf(token) + token.length()));
					Object isThere = fStrToJava.get(strLineNumber);			
					if (isThere == null) {
						Set javaLNs = new TreeSet();
						javaLNs.add(new Integer(javaLineNumber));
						fStrToJava.put(strLineNumber, javaLNs);
					} else {
						((Set) isThere).add(new Integer(javaLineNumber));				
					}
					fJavaToStr.put(new Integer(javaLineNumber), strLineNumber);
				}		
			}
		} catch (Exception e) {
		}
		
		// cache to listener for debugging speed up
		StreamItDebugEventSetListener.addJavaToStrMap(javaFile, fJavaToStr);	
	}
	
	protected IResource getJavaResource() {
		return getJavaFile().getResource();
	}

	protected IDocument getJavaDocument() {
		return JavaUI.getDocumentProvider().getDocument(getJavaEditorPart().getEditorInput());
	}
	
	protected AbstractMarkerAnnotationModel getJavaAnnotationModel() {
		IDocumentProvider provider = JavaUI.getDocumentProvider();
		IAnnotationModel model = provider.getAnnotationModel(getJavaEditorPart().getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}
}
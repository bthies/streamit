/*******************************************************************************
 * StreamIt Debugger Plugin adapted from
 * org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
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
package texteditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.TreeSet;
import launcher.StreamItApplicationLaunchShortcut;
import launcher.StreamItDebugEventSetListener;
import launcher.StreamItLocalApplicationLaunchConfigurationDelegate;
import launcher.StreamItLauncherPlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.debug.ui.actions.BreakpointLocationVerifier;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

public class ManageBreakpointRulerAction extends Action implements IUpdate {	
	
	protected IVerticalRulerInfo fRuler;
	protected ITextEditor fTextEditor;
	protected String fMarkerType;
	protected List fMarkers;

	protected String fAddLabel;
	protected String fRemoveLabel;

	protected String fStrFileName;
	protected String fJavaFileName;
	protected String fStrName;
	protected IJavaElement fJavaFile;
	protected IEditorPart fJavaEditorPart;
	protected HashMap fStrToJava;
	protected HashMap fJavaToStr;	
	protected static StreamItApplicationLaunchShortcut launchShortcut = new StreamItApplicationLaunchShortcut();
	protected static StreamItLocalApplicationLaunchConfigurationDelegate launchDelegate = new StreamItLocalApplicationLaunchConfigurationDelegate();
	
	public ManageBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		
		fRuler= ruler;
		fTextEditor= editor;
		fMarkerType= IBreakpoint.BREAKPOINT_MARKER;
		fAddLabel= ActionMessages.getString("ManageBreakpointRulerAction.add.label"); //$NON-NLS-1$
		fRemoveLabel= ActionMessages.getString("ManageBreakpointRulerAction.remove.label"); //$NON-NLS-1$

		// TODO when .str changes, .java should also change!
		// TODO error when .str is already open (at start-up)

		fStrFileName = getResource().getName();
		fStrName = fStrFileName.substring(0, fStrFileName.lastIndexOf('.' + StreamItLauncherPlugin.STR_FILE_EXTENSION));
		fJavaFileName = fStrName + ".java";
		fJavaFile = null;
		fJavaEditorPart = null;
		fStrToJava = null;
		fJavaToStr = null;
	}
	
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

				// handle a secondary file
				String mainClassName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "");
				if (!mainClassName.equals(fStrName)) fJavaFileName = mainClassName + ".java";
				javaFile = project.getFile(fJavaFileName);
			}
			fJavaFile = JavaCore.create(javaFile);
			fJavaEditorPart = JavaUI.openInEditor(fJavaFile);
			mapStrToJava(javaFile);
			
			// open graph editor if not already open
			
			
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
	
	/** 
	 * Returns the resource for which to create the marker, 
	 * or <code>null</code> if there is no applicable resource.
	 *
	 * @return the resource for which to create the marker or <code>null</code>
	 */
	protected IResource getResource() {
		IEditorInput input= fTextEditor.getEditorInput();
		
		IResource resource= (IResource) input.getAdapter(IFile.class);
		
		if (resource == null) {
			resource= (IResource) input.getAdapter(IResource.class);
		}
			
		return resource;
	}
	
	protected IResource getJavaResource() {
		return getJavaFile().getResource();
	}
	
	/**
	 * Checks whether a position includes the ruler's line of activity.
	 *
	 * @param position the position to be checked
	 * @param document the document the position refers to
	 * @return <code>true</code> if the line is included by the given position
	 */
	protected boolean includesRulerLine(Position position, IDocument document) {

		if (position != null) {
			try {
				int markerLine= document.getLineOfOffset(position.getOffset());
				int line= fRuler.getLineOfLastMouseButtonActivity();							
				if (line == markerLine) {					
					return true;
				}
			} catch (BadLocationException x) {
			}
		}
		
		return false;
	}
	
	protected boolean includesJavaRulerLine(Position position, IDocument document) {
		if (position != null) {
			try {
				int markerLine= document.getLineOfOffset(position.getOffset());
				int line= fRuler.getLineOfLastMouseButtonActivity() + 1;
				
				// find mapping to .java
				Iterator javaRulerLine;
				Object isThere = fStrToJava.get(new Integer(line));
				if (isThere == null) return false;
				else javaRulerLine = ((Set) isThere).iterator();

				while (javaRulerLine.hasNext()) {
					if (((Integer) javaRulerLine.next()).intValue() - 1 == markerLine)
						return true;
				}
			} catch (BadLocationException x) {
			}
		}		
		return false;
	}
	
	/**
	 * Returns this action's vertical ruler info.
	 *
	 * @return this action's vertical ruler
	 */
	protected IVerticalRulerInfo getVerticalRulerInfo() {
		return fRuler;
	}
	
	/**
	 * Returns this action's editor.
	 *
	 * @return this action's editor
	 */
	protected ITextEditor getTextEditor() {
		return fTextEditor;
	}
	
	/**
	 * Returns the <code>AbstractMarkerAnnotationModel</code> of the editor's input.
	 *
	 * @return the marker annotation model
	 */
	protected AbstractMarkerAnnotationModel getAnnotationModel() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		IAnnotationModel model= provider.getAnnotationModel(fTextEditor.getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}
	
	protected AbstractMarkerAnnotationModel getJavaAnnotationModel() {
		IDocumentProvider provider = JavaUI.getDocumentProvider();
		IAnnotationModel model = provider.getAnnotationModel(getJavaEditorPart().getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}

	/**
	 * Returns the <code>IDocument</code> of the editor's input.
	 *
	 * @return the document of the editor's input
	 */
	protected IDocument getDocument() {
		IDocumentProvider provider= fTextEditor.getDocumentProvider();
		return provider.getDocument(fTextEditor.getEditorInput());
	}
	
	protected IDocument getJavaDocument() {
		return JavaUI.getDocumentProvider().getDocument(getJavaEditorPart().getEditorInput());
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {
		fMarkers= getMarkers();
		setText(fMarkers.isEmpty() ? fAddLabel : fRemoveLabel);
		JavaPlugin.getActivePage().bringToTop(getTextEditor());
	}

	/**
	 * @see Action#run()
	 */
	public void run() {
		if (fMarkers.isEmpty()) {
			addMarker();
		} else {
			removeMarkers(fMarkers);
		}
		JavaPlugin.getActivePage().bringToTop(getTextEditor());
	}
	
	protected List getJavaMarkers() {
		
		List breakpoints= new ArrayList();
		
		IResource resource = getJavaResource();
		IDocument document = getJavaDocument();
		AbstractMarkerAnnotationModel model = getJavaAnnotationModel();
		
		if (model != null) {
			try {
				
				IMarker[] markers= null;
				if (resource instanceof IFile) {
					markers= resource.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);					
				} else {
					IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
					markers= root.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
				}		
			
				if (markers != null) {
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					for (int i= 0; i < markers.length; i++) {
						IBreakpoint breakpoint= breakpointManager.getBreakpoint(markers[i]);
						if (breakpoint != null && breakpointManager.isRegistered(breakpoint) && 
								includesJavaRulerLine(model.getMarkerPosition(markers[i]), document)) {
							breakpoints.add(markers[i]);
						}
					}
				}
			} catch (CoreException x) {
				JDIDebugUIPlugin.log(x.getStatus());
			}
		}		
		return breakpoints;
	}
	
	protected List getMarkers() {

		List breakpoints= new ArrayList();
			
		IResource resource = getResource();
		IDocument document = getDocument();
		AbstractMarkerAnnotationModel model = getAnnotationModel();
			
		if (model != null) {
			try {
					
				IMarker[] markers= getAllMarkers(resource);
					
				if (markers != null) {
					IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
					for (int i= 0; i < markers.length; i++) {
						IBreakpoint breakpoint= breakpointManager.getBreakpoint(markers[i]);
						if (breakpoint != null && breakpointManager.isRegistered(breakpoint) && 
								includesRulerLine(model.getMarkerPosition(markers[i]), document)) {
							breakpoints.add(markers[i]);							
						}
					}
				}
			} catch (CoreException x) {
				JDIDebugUIPlugin.log(x.getStatus());
			}
		}		
		return breakpoints;
	}
	
	protected IMarker[] getAllMarkers(IResource resource) throws CoreException {
		if (resource instanceof IFile) {
			return resource.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);					
		} else {
			IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
			return root.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
		}	
	}
	
	protected void addMarker() {
		IEditorInput editorInput = getJavaEditorPart().getEditorInput();
		IDocument document = getJavaDocument();

		// find mapping between str and Java
		int rulerLine= getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1;
		int javaRulerLine;
		
		Object isThere = fStrToJava.get(new Integer(rulerLine));
		if (isThere == null) return;
		else javaRulerLine = ((Integer) ((Set) isThere).iterator().next()).intValue() - 1;

		// get & verify line number
		try {
			BreakpointLocationVerifier bv = new BreakpointLocationVerifier();
			int lineNumber = bv.getValidBreakpointLocation(document, javaRulerLine);

			// trace back to .str
			rulerLine = ((Integer) fJavaToStr.get(new Integer(lineNumber))).intValue();

			if (lineNumber > 0) {
				IRegion line= document.getLineInformation(lineNumber - 1);
				IType type = setType(editorInput, line);
				
				if (type != null) {
					IJavaProject project= type.getJavaProject();
					if (type.exists() && project != null && project.isOnClasspath(type)) {
						if (JDIDebugModel.lineBreakpointExists(type.getFullyQualifiedName(),lineNumber) == null) {
							Map attributes = new HashMap(10);
							int start= line.getOffset();
							int end= start + line.getLength() - 1;
							BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(attributes, type, start, end);
							IMarker m = JDIDebugModel.createLineBreakpoint(getJavaResource(), type.getFullyQualifiedName(), lineNumber, -1, -1, 0, true, attributes).getMarker();

							// in .java, leave highlighted area on breakpoint just added
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(m, false);
							
							// Add breakpoint in .str
							IRegion strLine= getDocument().getLineInformation(rulerLine - 1);
							start = strLine.getOffset();
							end = start + strLine.getLength() - 1;
							BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(attributes, type, start, end);
							JDIDebugModel.createLineBreakpoint(getResource(), type.getFullyQualifiedName(), rulerLine, -1, -1, 0, true, attributes);
						}
					}
				}	
			}
		} catch (Exception e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.adding.message1"), e); //$NON-NLS-1$
		}
	}
	
	protected IType setType(IEditorInput editorInput, IRegion line) throws JavaModelException {
		if (editorInput instanceof IFileEditorInput) {
			IWorkingCopyManager manager= JavaUI.getWorkingCopyManager();
			ICompilationUnit unit= manager.getWorkingCopy(editorInput);
						
			if (unit != null) {
				synchronized (unit) {
					unit.reconcile();
				}
				IJavaElement e = unit.getElementAt(line.getOffset());
				if (e instanceof IType) {
					return (IType)e;
				} else if (e instanceof IMember) {
					return ((IMember)e).getDeclaringType();
				}
			}
		}
		return null;
	}
		
	protected void removeMarkers(List markers) {

		// remove breakpoints from .java		
		List javaMarkers = getJavaMarkers();
		IBreakpointManager breakpointManager= DebugPlugin.getDefault().getBreakpointManager();
		try {
			Iterator e= javaMarkers.iterator();
			IMarker m;
			while (e.hasNext()) {
				m = (IMarker) e.next();
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(m, false);
				IBreakpoint breakpoint= breakpointManager.getBreakpoint(m);
				breakpointManager.removeBreakpoint(breakpoint, true);
			}
		} catch (CoreException e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.removing.message1"), e); //$NON-NLS-1$
		}
		
		// remove breakpoints from .str
		try {
			Iterator e= markers.iterator();
			while (e.hasNext()) {
				IBreakpoint breakpoint= breakpointManager.getBreakpoint((IMarker) e.next());
				breakpointManager.removeBreakpoint(breakpoint, true);
			}
		} catch (CoreException e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.removing.message1"), e); //$NON-NLS-1$
		}
	}
}
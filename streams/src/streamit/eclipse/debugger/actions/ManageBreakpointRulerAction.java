/*******************************************************************************
 * StreamIt Debugger Plugin adapted from
 * org.eclipse.jdt.internal.debug.ui.actions.ManageBreakpointRulerAction
 * @author kkuo
 *******************************************************************************/

package streamit.eclipse.debugger.actions;

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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.BreakpointUtils;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
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

import streamit.eclipse.debugger.core.BreakpointRulerData;
import streamit.eclipse.debugger.core.StrToJavaMapper;

public class ManageBreakpointRulerAction extends Action implements IUpdate {	
	
	protected IVerticalRulerInfo fRuler;
	protected ITextEditor fTextEditor;
	protected String fMarkerType;
	protected List fMarkers;

	protected String fAddLabel;
	protected String fRemoveLabel;

	protected BreakpointRulerData fData;	

	public ManageBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo ruler) {
		
		fRuler= ruler;
		fTextEditor= editor;
		fMarkerType= IBreakpoint.BREAKPOINT_MARKER;
		fAddLabel= ActionMessages.getString("ManageBreakpointRulerAction.add.label"); //$NON-NLS-1$
		fRemoveLabel= ActionMessages.getString("ManageBreakpointRulerAction.remove.label"); //$NON-NLS-1$

		// TODO when .str changes, .java should also change!
		// TODO error when .str is already open (at start-up)

		fData = StrToJavaMapper.getInstance().loadStrFile(getResource(), editor);
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
		return fData.getJavaFile();
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
				int markerLine = document.getLineOfOffset(position.getOffset());
				int line = fRuler.getLineOfLastMouseButtonActivity() + 1;
				
				// find mapping to .java
				line = fData.getJavaBreakpointLineNumber(line);
				if (line < 0) return false;
				if (line - 1 == markerLine) return true;
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
		IAnnotationModel model = provider.getAnnotationModel(fData.getJavaEditorPart().getEditorInput());
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
		return JavaUI.getDocumentProvider().getDocument(fData.getJavaEditorPart().getEditorInput());
	}
	
	/**
	 * @see IUpdate#update()
	 */
	public void update() {

		fMarkers= getMarkers();
		if (fMarkers.isEmpty()) {
			setText(fAddLabel);
			
			// find mapping between str and Java
			int lineNumber = fData.getJavaBreakpointLineNumber(getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1);
			setEnabled(enableAction(lineNumber));
			
		} else {
			setText(fRemoveLabel);
			setEnabled(true);
		}
	}
	
	public static boolean enableAction(int lineNumber) {
		if (lineNumber > 0) return true;
		return false;
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
		IEditorInput editorInput = fData.getJavaEditorPart().getEditorInput();
		IDocument document = getJavaDocument();

		// find mapping between str and java
		int strRulerLineNumber = getVerticalRulerInfo().getLineOfLastMouseButtonActivity() + 1;
		int validJavaLineNumber = fData.getJavaBreakpointLineNumber(strRulerLineNumber);

		// get & verify line number
		try {
			IRegion line = document.getLineInformation(validJavaLineNumber - 1);
			IType type = getType(editorInput, line);
			
			if (type != null) {
				IJavaProject project = type.getJavaProject();
				if (type.exists() && project != null && project.isOnClasspath(type)) {
					if (getJavaMarkers().size() == 0) {
						Map attributes = new HashMap(10);
						int start= line.getOffset();
						int end= start + line.getLength() - 1;
						BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(attributes, type, start, end);
						IMarker m = JDIDebugModel.createLineBreakpoint(getJavaResource(), type.getFullyQualifiedName(), validJavaLineNumber, -1, -1, 0, true, attributes).getMarker();
						
						// Add breakpoint in .str
						IRegion strLine= getDocument().getLineInformation(strRulerLineNumber - 1);
						start = strLine.getOffset();
						end = start + strLine.getLength() - 1;
						BreakpointUtils.addJavaBreakpointAttributesWithMemberDetails(attributes, type, start, end);
						JDIDebugModel.createLineBreakpoint(getResource(), type.getFullyQualifiedName(), strRulerLineNumber, -1, -1, 0, true, attributes);
					}
				}	
			}
		} catch (Exception e) {
			JDIDebugUIPlugin.errorDialog(ActionMessages.getString("ManageBreakpointRulerAction.error.adding.message1"), e); //$NON-NLS-1$
		}
	}
	
	protected static IType getType(IEditorInput editorInput, IRegion line) throws JavaModelException {
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
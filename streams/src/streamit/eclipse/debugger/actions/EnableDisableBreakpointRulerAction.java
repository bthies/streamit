/*******************************************************************************
 * StreamIt Debugger adapted from
 * modifier - Kimberly Kuo
 *******************************************************************************/
package streamit.eclipse.debugger.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.IJavaWatchpoint;
import org.eclipse.jdt.internal.debug.core.JDIDebugPlugin;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.actions.AbstractBreakpointRulerAction;
import org.eclipse.jdt.internal.debug.ui.actions.ActionMessages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.core.BreakpointRulerData;
import streamit.eclipse.debugger.core.StrToJavaMapper;

public class EnableDisableBreakpointRulerAction extends AbstractBreakpointRulerAction {
	
	private IBreakpoint fJavaBreakpoint;
	protected BreakpointRulerData fData;

	/**
	 * Creates the action to enable/disable breakpoints
	 */
	public EnableDisableBreakpointRulerAction(ITextEditor editor, IVerticalRulerInfo info) {
		setInfo(info);
		setTextEditor(editor);
		setText(ActionMessages.getString("EnableDisableBreakpointRulerAction.&Enable_Breakpoint_1")); //$NON-NLS-1$

		// From streamit.eclipse.debugger.texteditor.ManageBreakpointRulerAction
		fData = StrToJavaMapper.getInstance().loadStrFile(getFile(), false);
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
					int markerLine = doc.getLineOfOffset(position.getOffset());
					int line = getInfo().getLineOfLastMouseButtonActivity() + 1;
					
					// find mapping to .java
					if (jBreakpoint instanceof IJavaWatchpoint) line = fData.getJavaWatchpoinLineNumber(line); 
					else line = fData.getJavaBreakpointLineNumber(line);
					if (line < 0) return false;
					if (line - 1 == markerLine) return true;

				} catch (BadLocationException x) {
					JDIDebugUIPlugin.log(x);
				}
			}
		}
		
		return false;
	}
	
	// From streamit.eclipse.debugger.texteditor.ManageBreakpointRulerAction
	/** 
	 * Returns the resource for which to create the marker, 
	 * or <code>null</code> if there is no applicable resource.
	 *
	 * @return the resource for which to create the marker or <code>null</code>
	 */
	protected IFile getFile() {
		
		IEditorInput input= getTextEditor().getEditorInput();
		
		IFile resource= (IFile) input.getAdapter(IFile.class);
		
		return resource;
	}
	
	protected IResource getJavaResource() {
		return fData.getJavaFile();
	}

	protected IDocument getJavaDocument() {
		return JavaUI.getDocumentProvider().getDocument(fData.getJavaEditorPart().getEditorInput());
	}
	
	protected AbstractMarkerAnnotationModel getJavaAnnotationModel() {
		IDocumentProvider provider = JavaUI.getDocumentProvider();
		IAnnotationModel model = provider.getAnnotationModel(fData.getJavaEditorPart().getEditorInput());
		if (model instanceof AbstractMarkerAnnotationModel) {
			return (AbstractMarkerAnnotationModel) model;
		}
		return null;
	}
}
package streamit.eclipse.debugger.texteditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.ActionSetContributionItem;
import org.eclipse.ui.internal.PluginActionContributionItem;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.actions.IStreamItActionConstants;
import streamit.eclipse.debugger.actions.ManageBreakpointRulerAction;
import streamit.eclipse.debugger.actions.ManageMethodBreakpointRulerAction;
import streamit.eclipse.debugger.actions.ManageWatchpointRulerAction;
import streamit.eclipse.debugger.core.BreakpointRulerData;
import streamit.eclipse.debugger.core.StrToJavaMapper;

/**
 * @author kkuo
 */
public class StreamItMenuListener implements IMenuListener {

	/**
	 * 
	 */
	public StreamItMenuListener() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IMenuListener#menuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	public void menuAboutToShow(IMenuManager manager) {
		boolean enabledLineBreakpoint = false;
		boolean enabledMethodBreakpoint = false;
		boolean enabledWatchpoint = false;
		
		boolean addLineBreakpoint = true;
		boolean addMethodBreakpoint = true;
		boolean addWatchpoint = true;
		
		IEditorPart e = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (e instanceof StreamItEditor) {
			ITextEditor editor = (ITextEditor) e;
			IEditorInput input = editor.getEditorInput();
			IResource resource = (IResource) input.getAdapter(IFile.class);
			if (resource == null) resource = (IResource) input.getAdapter(IResource.class);
			BreakpointRulerData data = StrToJavaMapper.getInstance().loadStrFile(resource, editor);
			ISelection selection = editor.getSelectionProvider().getSelection();

			if (selection instanceof ITextSelection) {
				// check enablement
				int strLineNumber = ((ITextSelection) selection).getStartLine() + 1;
				int javaBreakpointNumber = data.getJavaBreakpointLineNumber(strLineNumber);
				int javaWatchpointNumber = data.getJavaWatchpoinLineNumber(strLineNumber);
				
				if (ManageBreakpointRulerAction.enableAction(javaBreakpointNumber)) {
					enabledLineBreakpoint = true;
					addLineBreakpoint = addPoint(resource, strLineNumber);
				}
				
				IEditorInput javaEditorInput = data.getJavaEditorPart().getEditorInput();
				IDocument doc = JavaUI.getDocumentProvider().getDocument(javaEditorInput);
				if (ManageMethodBreakpointRulerAction.enableAction(javaBreakpointNumber, doc, javaEditorInput)) {
					enabledMethodBreakpoint = true;
					addMethodBreakpoint = addPoint(resource, strLineNumber);
				}

				if (ManageWatchpointRulerAction.enableAction(javaWatchpointNumber, doc, javaEditorInput)) {
					enabledWatchpoint = true;
					addWatchpoint = addPoint(resource, strLineNumber);
				}
			}
		}
		
		// now update actions
		IContributionItem[] items = manager.getItems();
		IContributionItem item;
		String id;
		for (int i = 0; i < items.length; i++) {
			item = items[i];
			id = item.getId();
			if (id.equals(IStreamItActionConstants.ID_LINE_BREAKPOINT_ACTION)) {
				setAction(item, enabledLineBreakpoint, addLineBreakpoint, "ManageBreakpointRulerAction");
			} else if (id.equals(IStreamItActionConstants.ID_METHOD_BREAKPOINT_ACTION)) {
				setAction(item, enabledMethodBreakpoint, addMethodBreakpoint, "ManageMethodBreakpointRulerAction");
			} else if (id.equals(IStreamItActionConstants.ID_WATCHPOINT_ACTION)) {
				setAction(item, enabledWatchpoint, addWatchpoint, "ManageWatchpointRulerAction");
			}
		}
	}

	private void setAction(IContributionItem item, boolean enabled, boolean add, String labelPrefix) {
		if (!(item instanceof ActionSetContributionItem)) return;
		IContributionItem inner = ((ActionSetContributionItem) item).getInnerItem();
		if (!(inner instanceof PluginActionContributionItem)) return;
		IAction action = ((PluginActionContributionItem) inner).getAction();
		action.setEnabled(enabled);
		if (add) action.setText(StreamItEditorMessages.getString(labelPrefix + ".add.label"));
		else action.setText(StreamItEditorMessages.getString(labelPrefix + ".remove.label"));
	}
	
	// check add/remove
	private boolean addPoint(IResource strFile, int strLineNumber) {
		try {
			IMarker[] markers = strFile.findMarkers(IBreakpoint.BREAKPOINT_MARKER, true, IResource.DEPTH_INFINITE);
			if (markers != null) {
				IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
				IBreakpoint breakpoint;
				IMarker m;
				for (int i= 0; i < markers.length; i++) {
					m = markers[i];
					breakpoint = breakpointManager.getBreakpoint(m);
					if (breakpoint != null && breakpointManager.isRegistered(breakpoint) && 
						strLineNumber == m.getAttribute(IMarker.LINE_NUMBER, -1)) {
						return false;
					}
				}
			}
		} catch (CoreException ce) {
		}
		return true;
	}
}
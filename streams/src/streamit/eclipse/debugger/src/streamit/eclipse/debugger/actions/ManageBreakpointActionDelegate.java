package streamit.eclipse.debugger.actions;

import org.eclipse.jdt.core.IType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.texteditor.IStreamItEditorConstants;
import streamit.eclipse.debugger.texteditor.StreamItEditor;
import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class ManageBreakpointActionDelegate implements IPartListener, IWorkbenchWindowActionDelegate {

	protected ManageBreakpointRulerAction fRulerAction;
	protected ManageBreakpointRulerInfo fRulerInfo;
	
	protected boolean fInitialized= false;
	protected IAction fAction= null;
	protected IType fType= null;
	protected ITextEditor fTextEditor= null;
	protected IWorkbenchWindow fWorkbenchWindow= null;

	/**
	 * Manages a breakpoint.
	 */
	protected void manageBreakpoint(IEditorInput editorInput) {
		
		StreamItViewsManager.getInstance().addMenuListener(getTextEditor());
		
		ISelectionProvider sp= getTextEditor().getSelectionProvider();
		ISelection selection= sp.getSelection();
		if (selection instanceof ITextSelection) {
			fRulerInfo.setLineNumber(((ITextSelection) selection).getStartLine());
			fRulerAction.update();
			fRulerAction.run();
		}
	}
	
	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		if (getTextEditor() != null) {
			manageBreakpoint(getTextEditor().getEditorInput());
		}
	}
	
	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (!fInitialized) {
			initialize(action);
		}

	}

	protected void initialize(IAction action) {
		setAction(action);
		if (getWorkbenchWindow() != null) {
			IWorkbenchPage page= getWorkbenchWindow().getActivePage();
			if (page != null) {
				IEditorPart part= page.getActiveEditor();
				if (part instanceof StreamItEditor) setTextEditor((ITextEditor)part);
			}
		}
		fInitialized= true;
	}
	
	protected IAction getAction() {
		return fAction;
	}

	protected void setAction(IAction action) {
		fAction = action;
	}
	
	/**
	 * @see IPartListener#partActivated(IWorkbenchPart)
	 */
	public void partActivated(IWorkbenchPart part) {
		if (part instanceof StreamItEditor) setTextEditor((ITextEditor)part);
	}

	/**
	 * @see IPartListener#partBroughtToTop(IWorkbenchPart)
	 */
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	/**
	 * @see IPartListener#partClosed(IWorkbenchPart)
	 */
	public void partClosed(IWorkbenchPart part) {
		if (part == getTextEditor()) {
			setTextEditor(null);
			if (getAction() != null) {
				getAction().setEnabled(false);
			}
		}
	}

	/**
	 * @see IPartListener#partDeactivated(IWorkbenchPart)
	 */
	public void partDeactivated(IWorkbenchPart part) {
	}

	/**
	 * @see IPartListener#partOpened(IWorkbenchPart)
	 */
	public void partOpened(IWorkbenchPart part) {
		if (part instanceof StreamItEditor) setTextEditor((ITextEditor)part);
	}
	
	protected ITextEditor getTextEditor() {
		return fTextEditor;
	}

	protected void setTextEditor(ITextEditor editor) {
		fTextEditor = editor;
		setEnabledState(editor);
		if (fTextEditor == null) return;

		fRulerInfo = new ManageBreakpointRulerInfo();
		setRulerAction();		
	}
	
	protected void setRulerAction() {
		fRulerAction = new ManageBreakpointRulerAction(fTextEditor, fRulerInfo);
	}

	protected void setEnabledState(ITextEditor editor) {
		if (getAction() != null) {
			getAction().setEnabled(editor != null && editor.getSite().getId().equals(IStreamItEditorConstants.ID_STREAMIT_EDITOR));
		} 
	}
	
	/**
	 * @see IWorkbenchWindowActionDelegate#init(IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		setWorkbenchWindow(window);
		window.getPartService().addPartListener(this);
	}

	/**
	 * @see IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		getWorkbenchWindow().getPartService().removePartListener(this);
	}

	protected IWorkbenchWindow getWorkbenchWindow() {
		return fWorkbenchWindow;
	}

	protected void setWorkbenchWindow(IWorkbenchWindow workbenchWindow) {
		fWorkbenchWindow = workbenchWindow;
	}
}
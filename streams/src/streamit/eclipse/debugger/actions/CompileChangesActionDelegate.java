package streamit.eclipse.debugger.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

import streamit.eclipse.debugger.core.StrToJavaMapper;
import streamit.eclipse.debugger.grapheditor.TestSwingEditorPlugin;
import streamit.eclipse.debugger.texteditor.IStreamItEditorConstants;
import streamit.eclipse.debugger.texteditor.StreamItEditor;

/**
 * @author kkuo
 */
public class CompileChangesActionDelegate implements IWorkbenchWindowActionDelegate, IPartListener {

	protected boolean fInitialized = false;
	protected IAction fAction = null;
	protected ITextEditor fTextEditor = null;
	protected IWorkbenchWindow fWorkbenchWindow = null;
	
	public CompileChangesActionDelegate() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	public void dispose() {
		getWorkbenchWindow().getPartService().removePartListener(this);
	}
	
	protected void initialize(IAction action) {
		setAction(action);
		if (getWorkbenchWindow() != null) {
			IWorkbenchPage page = getWorkbenchWindow().getActivePage();
			if (page != null) {
				IEditorPart part = page.getActiveEditor();
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
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
		setWorkbenchWindow(window);
		window.getPartService().addPartListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partActivated(IWorkbenchPart part) {
		if (part instanceof StreamItEditor) setTextEditor((ITextEditor) part);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partBroughtToTop(IWorkbenchPart part) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partClosed(IWorkbenchPart part) {
		if (part == getTextEditor()) {
			setTextEditor(null);
			if (getAction() != null) {
				getAction().setEnabled(false);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partDeactivated(IWorkbenchPart part) {
		if (part instanceof StreamItEditor) setTextEditor(null);		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart)
	 */
	public void partOpened(IWorkbenchPart part) {
		if (part instanceof StreamItEditor) setTextEditor((ITextEditor) part);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (getTextEditor() != null) {
			// recompile breakpoints
			IFile strFile = ((IFileEditorInput) getTextEditor().getEditorInput()).getFile();
			StrToJavaMapper.getInstance().loadStrFile(strFile, true);

			// reopen graph editor
			TestSwingEditorPlugin.getInstance().launchGraph(strFile);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (!fInitialized) {
			initialize(action);
		}
	}
	
	protected ITextEditor getTextEditor() {
		return fTextEditor;
	}

	protected void setTextEditor(ITextEditor editor) {
		fTextEditor = editor;
		setEnabledState(editor);
		if (fTextEditor == null) return;
	}
	
	protected void setEnabledState(ITextEditor editor) {
		if (getAction() != null) {
			getAction().setEnabled(editor != null && editor.getSite().getId().equals(IStreamItEditorConstants.ID_STREAMIT_EDITOR));
		} 
	}
	
	protected IWorkbenchWindow getWorkbenchWindow() {
		return fWorkbenchWindow;
	}

	protected void setWorkbenchWindow(IWorkbenchWindow workbenchWindow) {
		fWorkbenchWindow = workbenchWindow;
	}
	
}
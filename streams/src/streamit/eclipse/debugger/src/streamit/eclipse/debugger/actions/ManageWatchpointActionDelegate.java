package streamit.eclipse.debugger.actions;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorInput;

/**
 * @author kkuo
 */
public class ManageWatchpointActionDelegate extends ManageBreakpointActionDelegate {

	/**
	 * Manages a breakpoint.
	 */
	protected void manageBreakpoint(IEditorInput editorInput) {
		ISelectionProvider sp= getTextEditor().getSelectionProvider();
		ISelection selection= sp.getSelection();
		if (selection instanceof ITextSelection) {
			fRulerInfo.setLineNumber(((ITextSelection) selection).getStartLine());
			fRulerAction.update();
			fRulerAction.run();
		}
	}

	protected void setRulerAction() {
		fRulerAction = new ManageWatchpointRulerAction(fTextEditor, fRulerInfo);
	}
}

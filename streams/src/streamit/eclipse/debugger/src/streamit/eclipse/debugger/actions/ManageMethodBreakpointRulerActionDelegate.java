package streamit.eclipse.debugger.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author kkuo
 */
public class ManageMethodBreakpointRulerActionDelegate extends AbstractRulerActionDelegate {

	/**
	 * @see AbstractRulerActionDelegate#createAction()
	 */
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new ManageMethodBreakpointRulerAction(editor, rulerInfo);
	}
}

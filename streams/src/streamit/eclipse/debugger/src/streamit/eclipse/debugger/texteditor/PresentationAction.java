package streamit.eclipse.debugger.texteditor;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

/**
 * A toolbar action which toggles the presentation model of the
 * connected text editor. The editor shows either the highlight range
 * only or always the whole document.
 * 
 * @author kkuo
 */
public class PresentationAction extends TextEditorAction {

    /**
     * Constructs and updates the action.
     */
    public PresentationAction() {
		super(StreamItEditorMessages.getResourceBundle(), IStreamItEditorConstants.TOGGLE_PRESENTATION, 
		      null); //$NON-NLS-1$
		update();
    }
    
    /* (non-StreamItdoc)
     * Method declared on IAction
     */
    public void run() {
	
		ITextEditor editor = getTextEditor();
		
		editor.resetHighlightRange();
		boolean show = editor.showsHighlightRangeOnly();
		setChecked(!show);
		editor.showHighlightRangeOnly(!show);
    }
	
    /* (non-StreamItdoc)
     * Method declared on TextEditorAction
     */
    public void update() {
		setChecked(getTextEditor() != null 
			   && getTextEditor().showsHighlightRangeOnly());
		setEnabled(true);
    }
}

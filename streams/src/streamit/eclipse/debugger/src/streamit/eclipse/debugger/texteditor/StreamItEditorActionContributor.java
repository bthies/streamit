package streamit.eclipse.debugger.texteditor;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;

/**
 * @author kkuo
 */
public class StreamItEditorActionContributor extends BasicTextEditorActionContributor {

	/**
	 * 
	 */
	public StreamItEditorActionContributor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void contributeToMenu(IMenuManager menu) {
		super.contributeToMenu(menu);
		IMenuManager streamItMenu = menu.findMenuUsingPath(IStreamItEditorConstants.STREAMIT_RUN_MENU);
		if (streamItMenu == null) return;
		streamItMenu.addMenuListener(StreamItMenuListener.getInstance());
	}

}


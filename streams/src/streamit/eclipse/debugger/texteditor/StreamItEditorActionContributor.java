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
		/*
		IMenuManager runMenu = menu.findMenuUsingPath("org.eclipse.ui.run");
		if (runMenu == null) return;

		IContributionItem[] items = runMenu.getItems();
		boolean found = false;
		int start = 0, end = 0;
		for (int i = 0; i < items.length; i++) {
			IContributionItem item = items[i];
			if (!found && item.isGroupMarker() && item.getId().equals("breakpointGroup")) {
				found = true;
				System.out.println("group marker:  " + item.getId());
			} else if (found) {
				if (item.isGroupMarker()) {
					end = i;

				} else {
					System.out.println("item:  " + item.getId());
					start = i;
					
				}
			}
		}
*/
	}

}


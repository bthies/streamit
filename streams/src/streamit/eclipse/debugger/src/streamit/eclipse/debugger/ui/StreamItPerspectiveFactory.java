package streamit.eclipse.debugger.ui;

import org.eclipse.jdt.internal.ui.JavaPerspectiveFactory;
import org.eclipse.ui.IPageLayout;

/**
 * @author kkuo
 */
public class StreamItPerspectiveFactory extends JavaPerspectiveFactory {

	/**
	 * 
	 */
	public StreamItPerspectiveFactory() {
		super();
	}

	public void createInitialLayout(IPageLayout layout) {
		super.createInitialLayout(layout);
		//layout.addView()
		layout.addActionSet(IStreamItUIConstants.ID_STREAMIT_ACTION_SET);
	}
}

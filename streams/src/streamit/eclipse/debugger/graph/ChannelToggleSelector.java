package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import streamit.eclipse.debugger.core.IStreamItDebuggerConstants;

/**
 * @author kkuo
 */
public class ChannelToggleSelector implements MouseListener {

	/**
	 * 
	 */
	public ChannelToggleSelector(ChannelToggle c) {
		super();
		c.addMouseListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mousePressed(org.eclipse.draw2d.MouseEvent)
	 */
	public void mousePressed(MouseEvent me) {
		getStreamViewer(getActivePage()).toggleChannel(((ChannelToggle) me.getSource()).getId());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mouseReleased(org.eclipse.draw2d.MouseEvent)
	 */
	public void mouseReleased(MouseEvent me) {
		// does nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
	 */
	public void mouseDoubleClicked(MouseEvent me) {
	}
	
	private static IWorkbenchPage getActivePage(){
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}

	private static StreamViewer getStreamViewer(IWorkbenchPage page) {
		IViewPart viewPart = page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
		if (viewPart == null) return null;
		return (StreamViewer) ((StreamView) viewPart).getViewer();
	}
}

package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;

import streamit.eclipse.debugger.ui.StreamItViewsManager;

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
		StreamItViewsManager.getStreamViewer().toggleChannel(((ChannelToggle) me.getSource()).getId());
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
}

package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class OverviewStreamStructureSelector implements MouseListener {

	/**
	 * 
	 */
	public OverviewStreamStructureSelector(Figure panel) {
		super();
		panel.addMouseListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mousePressed(org.eclipse.draw2d.MouseEvent)
	 */
	public void mousePressed(MouseEvent me) {
		Point p = me.getLocation();
		
		StreamOverviewer ov = StreamItViewsManager.getStreamOverviewer();
		ov.setZoom(p);
		
		StreamViewer v = StreamItViewsManager.getStreamViewer();
		if (v == null) return;
		v.setOrigin(p.scale(4));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mouseReleased(org.eclipse.draw2d.MouseEvent)
	 */
	public void mouseReleased(MouseEvent me) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
	 */
	public void mouseDoubleClicked(MouseEvent me) {
	}
}

package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import streamit.eclipse.debugger.core.IStreamItDebuggerConstants;
import streamit.eclipse.debugger.core.StreamItViewsManager;

/**
 * @author kkuo
 */
public class StreamSelector implements MouseListener {

	private static Figure fSelection;
	
	public StreamSelector(Figure f) {
		super();
		if (f instanceof IStream) f.addMouseListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mousePressed(org.eclipse.draw2d.MouseEvent)
	 */
	public void mousePressed(MouseEvent me) {
		if (me.button == 3) {
			ChannelSelector.disableActions();
			return;			
		}
		setSelection((Figure) me.getSource());
		
		IStream is = (IStream) fSelection;
		updateLaunchVariableViews(is);
		
		Point p = me.getLocation();
		if (is.isWithinIcon(me.getLocation())) {
			// expand or collapse
			StreamItViewsManager.getStreamViewer().toggleStream(is.getNameWithId());
		} else {
			StreamOverviewer ov = StreamItViewsManager.getStreamOverviewer();
			if (ov == null) return;
			Point s = p.getScaled(.25);
			ov.setZoom(s);
			ov.setOrigin(s.x, s.y);
			
		}
	}
	
	protected static Figure getSelection() {
		return fSelection;
	}
	
	protected static void setSelection(Figure f) {
		unselected();
		fSelection = f;
		selected();
	}

	private static void selected() {
		fSelection.setForegroundColor(ColorConstants.menuForegroundSelected);
		fSelection.setBackgroundColor(ColorConstants.menuBackgroundSelected);
	}
		
	private static void unselected() {
		if (fSelection != null) {
			fSelection.setForegroundColor(ColorConstants.menuForeground);
			fSelection.setBackgroundColor(ColorConstants.white);
		}
	}
	
	protected static String getSelectionName() {
		if (fSelection == null) return null;
		return ((IStream) fSelection).getNameWithId(); 
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
		setSelection((Figure) me.getSource());
		
		if (fSelection instanceof IStream) {
			IStream is = (IStream) fSelection;
			updateLaunchVariableViews(is);

			// expand or collapse
			StreamItViewsManager.getStreamViewer().toggleStream(is.getNameWithId());
		}
	}
	
	private void updateLaunchVariableViews(IStream is) {
		IWorkbenchPage page = StreamItViewsManager.getActivePage();
		IWorkbenchWindow activeWindow = StreamItViewsManager.getActiveWorkbenchWindow();
		StreamView view = (StreamView) page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
		StreamItViewsManager.removeFromSelectionService(activeWindow, view);
		StreamItViewsManager.updateLaunchVariableViews(is.getNameWithoutId(), is.getId(), page);
		StreamItViewsManager.addToSelectionService(activeWindow, view);
	}
}
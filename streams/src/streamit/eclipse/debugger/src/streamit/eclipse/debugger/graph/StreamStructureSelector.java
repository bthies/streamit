package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class StreamStructureSelector implements MouseListener {

	private static Figure fSelection;
	
	public StreamStructureSelector(Figure f) {
		super();
		if (f instanceof IStreamStructureWidget) f.addMouseListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mousePressed(org.eclipse.draw2d.MouseEvent)
	 */
	public void mousePressed(MouseEvent me) {
		if (ChannelSelector.handleStreamSelection(me)) return;

		setSelection((Figure) me.getSource());		
		IStreamStructureWidget is = (IStreamStructureWidget) fSelection;
		//updateDebugVariablesViews(is);
		
		Point p = me.getLocation();
		StreamViewer v = StreamItViewsManager.getStreamViewer(); 
		if (is.isWithinIcon(me.getLocation())) {
			// expand or collapse
			v.toggleStream(is.getNameWithRuntimeId());
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
		return ((IStreamStructureWidget) fSelection).getNameWithRuntimeId(); 
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
		
		if (fSelection instanceof IStreamStructureWidget) {
			IStreamStructureWidget is = (IStreamStructureWidget) fSelection;
			//updateDebugVariablesViews(is);

			// expand or collapse
			StreamItViewsManager.getStreamViewer().toggleStream(is.getNameWithRuntimeId());
		}
	}
	/*	
	private void updateDebugVariablesViews(IStreamStructureWidget is) {
		IWorkbenchPage page = StreamItViewsManager.getActivePage();
		IWorkbenchWindow activeWindow = StreamItViewsManager.getActiveWorkbenchWindow();
		StreamView view = (StreamView) page.findView(IStreamItGraphConstants.ID_STREAMVIEW);
		StreamItViewsManager.removeFromSelectionService(activeWindow, view);
		StreamItViewsManager.updateDebugVariablesViews(is.getNameWithoutId(), is.getId(), page);
		StreamItViewsManager.addToSelectionService(activeWindow, view);
	}
	*/
}
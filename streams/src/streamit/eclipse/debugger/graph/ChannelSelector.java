package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import streamit.eclipse.debugger.actions.ChangeQueueValuesAction;
import streamit.eclipse.debugger.actions.HighlightDatumAction;
import streamit.eclipse.debugger.actions.ShowQueueValuesAction;
import streamit.eclipse.debugger.core.IStreamItDebuggerConstants;

/**
 * @author kkuo
 */
public class ChannelSelector implements MouseListener {

	private static ChangeQueueValuesAction fChangeQueueValuesAction = new ChangeQueueValuesAction();
	private static ShowQueueValuesAction fShowQueueValuesAction = new ShowQueueValuesAction();
	private static HighlightDatumAction fHighlightAction = new HighlightDatumAction();

	/**
	 * 
	 */
	public ChannelSelector(Channel c) {
		super();
		c.addMouseListener(this);
	}
	
	public ChannelSelector(StreamViewer v, Figure panel) {
		super();
		disableActions();
		createContextMenu(v);
		panel.addMouseListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mousePressed(org.eclipse.draw2d.MouseEvent)
	 */
	public void mousePressed(MouseEvent me) {
		if (me.button == 3) {
			Figure f = (Figure) me.getSource();
			if (f instanceof Channel) {
				Channel c = (Channel) me.getSource();
				fChangeQueueValuesAction.update(c);
				fShowQueueValuesAction.update(c);
				StreamViewer v = getStreamViewer(getActivePage());
				fHighlightAction.update(c, me.getLocation().y, v.getOptionData(false));
				createContextMenu(v);
			} else {
				disableActions();
			}
		}
	}
	
	public static void createContextMenu(StreamViewer v) {
		MenuManager menuMgr = new MenuManager("#PopUp");
		menuMgr.add(fChangeQueueValuesAction);
		menuMgr.add(fHighlightAction);
		menuMgr.add(fShowQueueValuesAction);
		Control canvas = v.getControl();
		Menu menu = menuMgr.createContextMenu(canvas);
		canvas.setMenu(menu);
	}

	protected static void disableActions() {
		fChangeQueueValuesAction.disable();
		fShowQueueValuesAction.disable();
		fHighlightAction.disable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mouseReleased(org.eclipse.draw2d.MouseEvent)
	 */
	public void mouseReleased(MouseEvent me) {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mouseDoubleClicked(org.eclipse.draw2d.MouseEvent)
	 */
	public void mouseDoubleClicked(MouseEvent me) {
		Channel c = (Channel) me.getSource();
		fHighlightAction.update(c, me.getLocation().y, getStreamViewer(getActivePage()).getOptionData(false));
		fHighlightAction.run();
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
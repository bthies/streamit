package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import streamit.eclipse.debugger.actions.AddRuntimeFilterBreakpointAction;
import streamit.eclipse.debugger.actions.ChangeQueueValuesAction;
import streamit.eclipse.debugger.actions.HighlightDatumAction;
import streamit.eclipse.debugger.actions.ShowQueueValuesAction;
import streamit.eclipse.debugger.core.LaunchData;
import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class ChannelSelector implements MouseListener {

	private static ChangeQueueValuesAction fChangeQueueValuesAction = new ChangeQueueValuesAction();
	private static ShowQueueValuesAction fShowQueueValuesAction = new ShowQueueValuesAction();
	private static HighlightDatumAction fHighlightAction = new HighlightDatumAction();
	private static AddRuntimeFilterBreakpointAction fAddRuntimeFilterBreakpointAction = new AddRuntimeFilterBreakpointAction();

	/**
	 * 
	 */
	public ChannelSelector(ChannelWidget c) {
		super();
		c.addMouseListener(this);
	}
	
	public ChannelSelector(StreamViewer v, Figure panel) {
		super();
		panel.addMouseListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.draw2d.MouseListener#mousePressed(org.eclipse.draw2d.MouseEvent)
	 */
	public void mousePressed(MouseEvent me) {
		if (me.button == 3) {
			Figure f = (Figure) me.getSource();
			StreamViewer v = StreamItViewsManager.getStreamViewer(StreamItViewsManager.getActivePage());
			LaunchData data = (LaunchData) v.getInput();
			if (f instanceof ChannelWidget && !data.isTerminated()) {
				ChannelWidget c = (ChannelWidget) me.getSource();
				fChangeQueueValuesAction.update(c);
				fShowQueueValuesAction.update(c);
				fHighlightAction.update(c, me.getLocation(), data.getOptionData(false));

				disableFilterActions();
				createContextMenu(v, true, false);

			} else {
				disableChannelActions();
				disableFilterActions();
				createContextMenu(v, true, true);
			}
		}
	}
	
	public static boolean handleStreamSelection(MouseEvent me) {
		if (me.button != 3) return false;
		
		Figure source = (Figure) me.getSource();
		StreamViewer v = StreamItViewsManager.getStreamViewer(StreamItViewsManager.getActivePage());
		if (v == null) return false;
		LaunchData data = (LaunchData) v.getInput();
		if (source instanceof FilterWidget) {// && !data.isTerminated()) {
			disableChannelActions();
			FilterWidget f = (FilterWidget) me.getSource();
			fAddRuntimeFilterBreakpointAction.update(f, data);
			createContextMenu(v, false, false);
			return true;
		}
		
		disableChannelActions();
		disableFilterActions();
		createContextMenu(v, false, true);
		return false;
	}
	
	public static void createContextMenu(StreamViewer v, boolean channelActions, boolean none) {
		MenuManager menuMgr = new MenuManager(IStreamItGraphConstants.POPUP_MENU);
		
		if (none) return;
		
		if (channelActions) {
			menuMgr.add(fChangeQueueValuesAction);
			menuMgr.add(fHighlightAction);
			menuMgr.add(fShowQueueValuesAction);
		} else {
			menuMgr.add(fAddRuntimeFilterBreakpointAction);
		}
		
		Control canvas = v.getControl();
		Menu menu = menuMgr.createContextMenu(canvas);
		canvas.setMenu(menu);
	}

	
	protected static void disableChannelActions() {
		fChangeQueueValuesAction.disable();
		fShowQueueValuesAction.disable();
		fHighlightAction.disable();
	}
	
	protected static void disableFilterActions() {
		fAddRuntimeFilterBreakpointAction.disable();
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
		Figure f = (Figure) me.getSource();
		StreamViewer v = StreamItViewsManager.getStreamViewer(StreamItViewsManager.getActivePage());
		LaunchData data = (LaunchData) v.getInput();
		if (f instanceof ChannelWidget && !data.isTerminated()) {
			ChannelWidget c = (ChannelWidget) me.getSource();
			fHighlightAction.update(c, me.getLocation(), data.getOptionData(false));
			fHighlightAction.run();
		}	
	}
}
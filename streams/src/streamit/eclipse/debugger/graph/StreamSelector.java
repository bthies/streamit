package streamit.eclipse.debugger.graph;

import java.util.ArrayList;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import streamit.eclipse.debugger.core.IStreamItDebuggerConstants;
import streamit.eclipse.debugger.launching.IStreamItLaunchingConstants;

/**
 * @author kkuo
 */
public class StreamSelector implements MouseListener {

	private static Figure fSelection;

	public StreamSelector(Figure f) {
		super();
		f.addMouseListener(this);
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
		
		if (fSelection instanceof IStream) {
			IStream is = (IStream) fSelection;

			IWorkbenchPage page = getActivePage();
			IWorkbenchWindow activeWindow = getActiveWorkbenchWindow();
			StreamView view = (StreamView) page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
			removeFromSelectionService(activeWindow, view);
			updateLaunchVariableViews(is.getNameWithoutId(), is.getId(), page);
			addToSelectionService(activeWindow, view);

			if (is.isWithinIcon(me.getLocation())) {
				// expand or collapse
				getStreamViewer(page).toggle(is.getNameWithId());
			}
		}
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

			IWorkbenchPage page = getActivePage();
			IWorkbenchWindow activeWindow = getActiveWorkbenchWindow();
			StreamView view = (StreamView) page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
			removeFromSelectionService(activeWindow, view);
			updateLaunchVariableViews(is.getNameWithoutId(), is.getId(), page);
			addToSelectionService(activeWindow, view);

			// expand or collapse
			getStreamViewer(page).toggle(is.getNameWithId());
		}
	}

	protected static void updateLaunchVariableViews(String streamName, String id, IWorkbenchPage page) { 
		// update debug view => will automatically update variables view
		// if no debug view, then no input in variables view
		IViewPart viewPart = page.findView(IDebugUIConstants.ID_DEBUG_VIEW);
		if (viewPart == null) return;
		StructuredViewer debugViewer = (StructuredViewer) ((AbstractDebugView) viewPart).getViewer();
		ISelection selection;
		
		Object root = debugViewer.getInput();
		LaunchManager lm = (LaunchManager) debugViewer.getInput();
		ViewerFilter[] filters = debugViewer.getFilters();
		if (lm == null) return;
		ILaunch[] launches = lm.getLaunches();
		for (int i = 0; i < launches.length; i++) {
			if (!launches[i].isTerminated() &&
				launches[i].getLaunchConfiguration().getName().indexOf(IStreamItLaunchingConstants.ID_STR_APPLICATION) != -1) {
				try {
					IThread[] threads = launches[i].getDebugTarget().getThreads();
					for (int j = 0; j < threads.length; j++) {
						Object[] frames = filter(root, threads[j].getStackFrames(), filters, debugViewer);
						for (int k = 0; k < frames.length; k++) {
							IVariable[] vars = ((IStackFrame) frames[k]).getVariables();
							IValue val;
							for (int x = 0; x < vars.length; x++) {
								val = vars[x].getValue();
								if (streamName.equals(val.getReferenceTypeName()) && id.equals(val.getValueString())) {
									debugViewer.setSelection(new StructuredSelection(frames[k]));
									return; 
								}
							}
						}
					}
				} catch (Exception e) {
				}
			}
		}
	}
	
	private static void addToSelectionService(IWorkbenchWindow window, ISelectionListener l) {
		if (l == null) return;
		ISelectionService service = window.getSelectionService();
		service.addSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, l);
		service.addSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, l);
	}
	
	private static void removeFromSelectionService(IWorkbenchWindow window, ISelectionListener l) {
		if (l == null) return;
		ISelectionService service = window.getSelectionService();
		service.removeSelectionListener(IDebugUIConstants.ID_DEBUG_VIEW, l);
		service.removeSelectionListener(IDebugUIConstants.ID_VARIABLE_VIEW, l);
	}
	
	private static Object[] filter(Object root, Object[] elements, ViewerFilter[] filters, StructuredViewer viewer) {
		if (filters != null) {
			ArrayList filtered = new ArrayList(elements.length);
			for (int i = 0; i < elements.length; i++) {
				boolean add = true;
				for (int j = 0; j < filters.length; j++) {
					add = ((ViewerFilter) filters[j]).select(viewer, root, elements[i]);
					if (!add)
						break;
				}
				if (add)
					filtered.add(elements[i]);
			}
			return filtered.toArray();
		}
		return elements;
	}
	
	private static IWorkbenchPage getActivePage(){
		return getActiveWorkbenchWindow().getActivePage();
	}
	
	private static IWorkbenchWindow getActiveWorkbenchWindow(){
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	private static StreamViewer getStreamViewer(IWorkbenchPage page) {
		IViewPart viewPart = page.findView(IStreamItDebuggerConstants.ID_STREAMVIEW);
		if (viewPart == null) return null;
		return (StreamViewer) ((StreamView) viewPart).getViewer();
	}
}
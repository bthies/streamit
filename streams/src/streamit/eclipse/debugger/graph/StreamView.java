package streamit.eclipse.debugger.graph;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import streamit.eclipse.debugger.core.StreamItViewsManager;

/**
 * @author kkuo
 */
public class StreamView extends ViewPart implements ISelectionListener {

	private StreamViewer fViewer;
	private boolean fDisabled;

	public StreamView() {
		super();
		fDisabled = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		fViewer = new StreamViewer(parent);
	}
	
	public StreamViewer getViewer() {
		return fViewer;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		fViewer.getControl().setFocus();
		if (fDisabled) return;
		
		StreamItViewsManager.setCollapseAll(this, false);
		fDisabled = true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		String id = part.getSite().getId();
		if (id.equals(IDebugUIConstants.ID_DEBUG_VIEW) || id.equals(IDebugUIConstants.ID_VARIABLE_VIEW)) {
			if (selection.isEmpty()) return;
			getViewer().setSelection(selection, false);
		}
	}
}

package streamit.eclipse.debugger.graph;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.PropertySheetPage;

import streamit.eclipse.debugger.core.LaunchData;
import streamit.eclipse.debugger.model.StreamStructureModelFactory;
import streamit.eclipse.debugger.properties.StreamStructurePropertyFactory;
import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class StreamViewer extends Viewer {

	// input
	private LaunchData fInput;

	// widgets, layout
	private Figure fRoot;
	private Canvas fCanvas;
	private Figure fPanel;
	private ScrolledComposite fCanvasC;
	private boolean fHideLines;

	// property sheet
	private PropertySheetPage fStreamStructurePropertySheetPage;

	public StreamViewer(Composite parent) {
		super();

		fInput = null;
		fHideLines = false;
		fRoot = null;
		fStreamStructurePropertySheetPage = null;
		StreamItViewFactory.getInstance().setFont(parent.getFont());

		// create SWT controls for viewer
		fCanvasC = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fCanvasC.setAlwaysShowScrollBars(true);
		fCanvasC.setExpandHorizontal(true);
		fCanvasC.setExpandVertical(true);

		fCanvas = new Canvas(fCanvasC, SWT.NONE);
		fCanvasC.setContent(fCanvas);
		fCanvasC.setMinSize(fCanvasC.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		fCanvas.setBackground(ColorConstants.white);
		fCanvasC.setBackground(ColorConstants.white);

		LightweightSystem lws = new LightweightSystem(fCanvas);
		fPanel = new Figure();
		lws.setContents(fPanel);
		
		new ChannelSelector(this, fPanel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#getControl()
	 */
	public Control getControl() {
		return fCanvas;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IInputProvider#getInput()
	 */
	public Object getInput() {
		return fInput;
	}
	
	protected Figure getRoot() {
		return fRoot;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#refresh()
	 */
	public void refresh() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#setInput(java.lang.Object)
	 */
	public void setInput(Object launchData) {
		if (launchData == null) {
			// clean graph
			fInput = null;
			setRoot(null);
			return;
		}
		
		if (!(launchData instanceof LaunchData)) return;
		fInput = (LaunchData) launchData;
		OptionData od = fInput.getOptionData(false);
		od.setHideLines(fHideLines);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput.getStreamItModel(), IStreamItGraphConstants.EMPTY_STRING, od, false));
	}

	private void setRoot(Figure root) {
		// set root
		fRoot = root;
		fPanel.removeAll();
		if (fRoot == null) {
			fCanvasC.setMinSize(fCanvasC.computeSize(0, 0));
			StreamOverviewer v = StreamItViewsManager.getStreamOverviewer();
			if (v == null) return;
			v.setInput(null);
			return;
		} 
		fPanel.add(fRoot);
		
		// set canvas size
		Dimension d = fRoot.getSize();
		fCanvasC.setMinSize(fCanvasC.computeSize(d.width, d.height));
		
		// set vertical bar incrementer
		ScrollBar bar = fCanvasC.getVerticalBar();
		bar.setIncrement(bar.getMaximum()/50);
		bar.setPageIncrement(bar.getMaximum()/50);
		bar = fCanvasC.getHorizontalBar();
		bar.setIncrement(bar.getMaximum()/50);
		bar.setPageIncrement(bar.getMaximum()/50);

		// update property page
		if (fStreamStructurePropertySheetPage != null)
			fStreamStructurePropertySheetPage.setRootEntry(StreamStructurePropertyFactory.getInstance().makeStream(fInput.getStreamItModel()));
		
		// set focus
		Figure f = StreamStructureSelector.getSelection();
		if (f == null) return;

		Point p = f.getLocation();
		setOrigin(f.getBounds().getCenter());
		
		// update overview
		StreamOverviewer v = StreamItViewsManager.getStreamOverviewer();
		if (v == null) return;
		v.setInput(fRoot);		
	}
	
	public void setOrigin(Point p) {
		
		int height = p.y - fCanvasC.getBounds().height/2;
		int width = p.x - fCanvasC.getBounds().width/2;
		if (height < 0) height = 0;
		if (width < 0) width = 0;
		
		fCanvasC.setOrigin(width, height);
	}
	
	public void setSelection(String streamNameWithId, boolean highlighting) {
		if (fInput == null) return;
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput.getStreamItModel(), streamNameWithId, fInput.getOptionData(highlighting), false));
	}
	
	public void toggleStream(String streamNameWithId) {
		if (fInput == null) return;
		OptionData od = fInput.getOptionData(false);
		od.toggleStream(streamNameWithId);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput.getStreamItModel(), streamNameWithId, od, false));
	}
	
	public void toggleChannel(String channelId) {
		if (fInput == null) return;
		OptionData od = fInput.getOptionData(false);
		od.toggleChannel(channelId);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput.getStreamItModel(), StreamStructureSelector.getSelectionName(), od, false));
	}
	
	// for CollapseAllViewActionDelegate
	public void collapseAll() {
		if (fInput == null) return;
		OptionData od = fInput.getOptionData(false);
		od.clearExpanded();
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput.getStreamItModel(), IStreamItGraphConstants.EMPTY_STRING, od, false));
	}
	
	// for ExpandAllViewActionDelegate
	public void expandAll() {
		if (fInput == null) return;
		OptionData od = fInput.getOptionData(false);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput.getStreamItModel(), IStreamItGraphConstants.EMPTY_STRING, od, true));
	}
	
	// for HideLinesViewActionDelegate
	public void hideLines(boolean hide) {
		fHideLines = hide;
		if (fInput == null) return;
		OptionData od = fInput.getOptionData(false);
		od.setHideLines(fHideLines);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput.getStreamItModel(), IStreamItGraphConstants.EMPTY_STRING, od, false));
	}
	
	// properties sheet
	public PropertySheetPage createPropertySheetPage() {
		fStreamStructurePropertySheetPage = new PropertySheetPage();
		return fStreamStructurePropertySheetPage;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	public void setSelection(ISelection selection, boolean reveal) {
		if (selection instanceof StructuredSelection) {
			Object element = ((StructuredSelection) selection).getFirstElement();
			if (element == null) return;
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (element instanceof IJavaVariable) {
					IJavaVariable v = (IJavaVariable) element;
					setSelection(v.getValue().getReferenceTypeName() + v.getValue().getValueString(), true);
				} else if (element instanceof IJavaStackFrame) {
					IJavaStackFrame frame = (IJavaStackFrame) element;
					IJavaObject o = frame.getThis();
					if (o != null) {
						setSelection(o.getReferenceTypeName() + o.getValueString(), true);
						return;
					}
					
					IVariable[] vars = ((IJavaStackFrame) element).getVariables();
					IVariable var = StreamStructureModelFactory.getInstance().getVariable(vars, IStreamItGraphConstants.PROGRAM_FIELD);
					if (var == null) return;
					setSelection(var.getReferenceTypeName() + var.getValue().getValueString(), true);
				}
			} catch (CoreException e) {
			}
		}		
	}
}
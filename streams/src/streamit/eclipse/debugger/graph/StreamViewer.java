package streamit.eclipse.debugger.graph;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaVariable;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import streamit.eclipse.debugger.IStreamItDebuggerPluginConstants;
import streamit.eclipse.debugger.StreamItDebuggerPlugin;
import streamit.eclipse.debugger.core.StreamItViewsManager;

/**
 * @author kkuo
 */
public class StreamViewer extends Viewer {

	private IVariable fInput;
	private Figure fRoot;

	private Canvas fCanvas;
	private Figure fPanel;
	private ScrolledComposite fCanvasC;

	private Image fPlus;
	private Image fMinus;
	private Image fUpArrow;
	private Image fDownArrow;
	private Font fParentFont;

	public StreamViewer(Composite parent) {
		super();
		
		fInput = null;
		fRoot = null;
		
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
		
		ImageRegistry reg = StreamItDebuggerPlugin.getDefault().getImageRegistry(); 
		fPlus = reg.get(IStreamItDebuggerPluginConstants.PLUS_IMAGE);
		fMinus = reg.get(IStreamItDebuggerPluginConstants.MINUS_IMAGE);
		fUpArrow = reg.get(IStreamItDebuggerPluginConstants.UP_ARROW_IMAGE);
		fDownArrow = reg.get(IStreamItDebuggerPluginConstants.DOWN_ARROW_IMAGE);
		fParentFont = parent.getFont();
		StreamItViewFactory.getInstance().setUtilities(fPlus, fMinus, fUpArrow, fDownArrow, fParentFont);
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
	public void setInput(Object input) {
		if (input == null) {
			// clean graph
			fInput = null;
			setRoot(null);
			StreamItViewsManager.setCollapseAll(false);
			return;
		}
		
		if (!(input instanceof IVariable)) return;
		fInput = (IVariable) input;
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, "", getAllExpanded(false)));
	}
	
	protected Expanded getAllExpanded(boolean highlighting) {
		if (fRoot == null) return new Expanded(highlighting);
		return ((MainPipeline) fRoot).getAllExpanded(highlighting);
	}
	
	private void setRoot(Figure root) {
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
		Dimension d = fRoot.getSize();
		fCanvasC.setMinSize(fCanvasC.computeSize(d.width, d.height));
		
		Figure f = StreamSelector.getSelection();
		if (f == null) return;
		Point p = f.getLocation();
		setOrigin(p);
		
		StreamOverviewer v = StreamItViewsManager.getStreamOverviewer();
		if (v == null) return;
		v.setInput(fRoot);
	}
	
	public void setOrigin(Point p) {
		int height = fCanvasC.getBounds().height;
		if (p.y < height) fCanvasC.setOrigin(0, 0);
		else fCanvasC.setOrigin(p.x, p.y);
	}
	
	public void setSelection(String streamNameWithId, boolean highlighting) {
		if (fInput == null) return;
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, streamNameWithId, getAllExpanded(highlighting)));
	}
	
	public void toggleStream(String streamNameWithId) {
		if (fInput == null) return;
		Expanded e = getAllExpanded(false);
		e.toggleStream(streamNameWithId);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, streamNameWithId, e));
	}
	
	public void toggleChannel(String channelId) {
		if (fInput == null) return;
		Expanded e = getAllExpanded(false);
		e.toggleChannel(channelId);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, StreamSelector.getSelectionName(), e));
	}
	
	public void collapseAll() {
		if (fRoot == null) return;
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, "", new Expanded(false)));
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
					IVariable var = StreamItViewFactory.getInstance().getVariable(vars, "program");
					setSelection(var.getReferenceTypeName() + var.getValue().getValueString(), true);
				}
			} catch (Exception e) {
			}
		}		
	}
}
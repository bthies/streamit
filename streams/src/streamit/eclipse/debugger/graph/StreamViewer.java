package streamit.eclipse.debugger.graph;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.geometry.Dimension;
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

/**
 * @author kkuo
 */
public class StreamViewer extends Viewer {


	private IVariable fInput;
	private Figure fRoot;

	private Canvas fCanvas;
	private Figure fPanel;
	private ScrolledComposite fCanvasC;
	private Font fParentFont;
	
	private Image fPlus;
	private Image fMinus;

	public StreamViewer(Composite parent) {
		super();
		
		fInput = null;
		fRoot = null;
		
		// create SWT controls for viewer
		fCanvasC = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
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
		
		fParentFont = parent.getFont();
		new ChannelSelector(this, fPanel);
		
		ImageRegistry reg = StreamItDebuggerPlugin.getDefault().getImageRegistry(); 
		fPlus = reg.get(IStreamItDebuggerPluginConstants.PLUS_IMAGE);
		fMinus = reg.get(IStreamItDebuggerPluginConstants.MINUS_IMAGE);
		StreamItViewFactory.getInstance().setImages(fPlus, fMinus);

	}
	
	public void dispose() {
		fPlus.dispose();
		fMinus.dispose();
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
			return;
		}
		
		if (!(input instanceof IVariable)) return;
		fInput = (IVariable) input;
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, fParentFont, "", getAllExpanded(false)));
	}
	
	private Expanded getAllExpanded(boolean highlighting) {
		if (fRoot == null) return new Expanded(highlighting);
		return ((MainPipeline) fRoot).getAllExpanded(highlighting);
	}
	
	private void setRoot(Figure root) {
		fRoot = root;
		fPanel.removeAll();
		if (fRoot == null) return;
		fPanel.add(fRoot);
		Dimension d = fRoot.getSize();
		fCanvasC.setMinSize(fCanvasC.computeSize(d.width, d.height));
	}
	
	public void setSelection(String streamNameWithId) {
		if (fInput == null) return;
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, fParentFont, streamNameWithId, getAllExpanded(true)));
	}
	
	public void toggle(String streamNameWithId) {
		if (fInput == null) return;
		Expanded e = getAllExpanded(false);
		e.toggle(streamNameWithId);
		setRoot(StreamItViewFactory.getInstance().makeStream(fInput, fParentFont, streamNameWithId, e));
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
					setSelection(v.getValue().getReferenceTypeName() + v.getValue().getValueString());
				} else if (element instanceof IJavaStackFrame) {
					IVariable[] vars = ((IJavaStackFrame) element).getVariables();
					if (vars.length < 1) return;
					setSelection(vars[0].getReferenceTypeName() + vars[0].getValue().getValueString());
				}
			} catch (Exception e) {
			}
		}		
	}
}
package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.parts.Thumbnail;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;

import streamit.eclipse.debugger.ui.StreamItViewsManager;

/**
 * @author kkuo
 */
public class StreamOverviewer extends Viewer {
	
	private Figure fInput;
	private Thumbnail fRoot;
	private RectangleFigure fZoom;

	private Canvas fCanvas;
	private Figure fPanel;
	private ScrolledComposite fCanvasC;

	public StreamOverviewer(Composite parent) {
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
		
		new OverviewStreamStructureSelector(fPanel);
		
		StreamViewer sv = StreamItViewsManager.getStreamViewer();
		if (sv == null) setRoot(null);
		else setInput(sv.getRoot());
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
		
		if (!(input instanceof IFigure)) return;
		fInput = (Figure) input;
		setRoot(fInput);
	}
	
	private void setRoot(Figure root) {
		// set root
		if (root == null) {
			fRoot = null;
			fPanel.removeAll();
			fCanvasC.setMinSize(fCanvasC.computeSize(0, 0));
			return;
		}
		fRoot = new Thumbnail(root);
		fRoot.setLocation(new Point(IStreamItGraphConstants.MARGIN/4, IStreamItGraphConstants.MARGIN/4));
		
		// set canvas size
		Dimension originalSize = fInput.getSize().expand(IStreamItGraphConstants.MARGIN*5, IStreamItGraphConstants.MARGIN*5);
		fRoot.setSize(originalSize.scale(.25, .25));
		fPanel.removeAll();
		fPanel.add(fRoot);
		Dimension d = fRoot.getSize();
		fCanvasC.setMinSize(fCanvasC.computeSize(d.width, d.height));

		// set vertical bar incrementer
		ScrollBar bar = fCanvasC.getVerticalBar();
		bar.setIncrement(bar.getMaximum()/50);
		bar.setPageIncrement(bar.getMaximum()/50);
		bar = fCanvasC.getHorizontalBar();
		bar.setIncrement(bar.getMaximum()/50);
		bar.setPageIncrement(bar.getMaximum()/50);

		// set focus
		Figure f = StreamStructureSelector.getSelection();
		if (f == null) return;
		Point p = f.getLocation();
		setOrigin(p.x/4, p.y/4);
		
		// set zoom
		fZoom = new RectangleFigure();
		fZoom.setForegroundColor(ColorConstants.menuBackgroundSelected);
		fZoom.setFill(false);
		fZoom.setOutline(true);

		fPanel.add(fZoom);
		setZoom(p);
	}
	
	protected void setOrigin(int x, int y) {
		fCanvasC.setOrigin(x, y);
	}
	
	protected void setZoom(Point p) {
		if (fZoom == null) return;
		int width = fCanvasC.getClientArea().width;
		fZoom.setSize(width, 16);
		fZoom.setLocation(new Point (Math.max(0, p.x - width/2), Math.max(0, p.y - 8)));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection, boolean)
	 */
	public void setSelection(ISelection selection, boolean reveal) {
	}
}
package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import streamit.eclipse.debugger.IStreamItDebuggerPluginConstants;
import streamit.eclipse.debugger.StreamItDebuggerPlugin;
import streamit.eclipse.debugger.model.Pipeline;

/**
 * @author kkuo
 */
public class StreamItViewFactory {

	private static StreamItViewFactory fInstance = new StreamItViewFactory();
	
	private Image fPlus;
	private Image fMinus;
	private Image fUpArrow;
	private Image fDownArrow;
	private Image fBreakpoint;
	private Image fDuplicate;

	private int fIconWidth;
	private int fIconHeight;
	private int fArrowHeight;
	private int fArrowWidth;
	private int fBreakpointHeight;
	private int fBreakpointWidth;
	private int fDuplicateWidth;
	private int fDuplicateHeight;
	
	private Font fParentFont;

	private StreamItViewFactory() {
		ImageRegistry reg = StreamItDebuggerPlugin.getDefault().getImageRegistry(); 
		fPlus = reg.get(IStreamItDebuggerPluginConstants.PLUS_IMAGE);
		fMinus = reg.get(IStreamItDebuggerPluginConstants.MINUS_IMAGE);
		fUpArrow = reg.get(IStreamItDebuggerPluginConstants.UP_ARROW_IMAGE);
		fDownArrow = reg.get(IStreamItDebuggerPluginConstants.DOWN_ARROW_IMAGE);
		fBreakpoint = reg.get(IStreamItDebuggerPluginConstants.BREAKPOINT_IMAGE);
		fDuplicate = reg.get(IStreamItDebuggerPluginConstants.DUPLICATE_IMAGE);

		fIconWidth = Math.max(fPlus.getBounds().width, fMinus.getBounds().width);
		fIconHeight = Math.max(fPlus.getBounds().height, fMinus.getBounds().height);
		fArrowHeight = Math.max(fUpArrow.getBounds().height, fDownArrow.getBounds().height);  
		fArrowWidth = Math.max(fUpArrow.getBounds().width, fDownArrow.getBounds().width); 
		if (fArrowHeight % 2 != 0) fArrowHeight++;
		if (fArrowWidth % 2 != 0) fArrowWidth++;
		fBreakpointWidth = fBreakpoint.getBounds().width;
		fBreakpointHeight = fBreakpoint.getBounds().height;
		fDuplicateWidth = fDuplicate.getBounds().width;
		fDuplicateHeight = fDuplicate.getBounds().height;
	}
	
	/**
	 * Returns the singleton StreamItModelFactory.
	 */
	public static StreamItViewFactory getInstance() {
		return fInstance;
	}
	
	public Figure makeStream(Pipeline topLevelPipeline, String streamNameWithId, OptionData optionData, boolean expandAll) {
		try {
			optionData.setExpandAll(expandAll);
			Figure f = new MainPipelineWidget(topLevelPipeline, streamNameWithId, optionData, getInstance());
			optionData.setExpandAll(false);
			return f;			
		} catch (DebugException e) {
		}
		return null;
	}
	
	public void setFont(Font parentFont) {
		fParentFont = parentFont;
	}

	public Image getMinus() {
		return fMinus;
	}
	
	public Image getPlus() {
		return fPlus;
	}
	
	public Image getArrow(boolean forward) {
		if (forward) return fUpArrow;
		else return fDownArrow;
	}
	
	public Image getBreakpoint() {
		return fBreakpoint;
	}
	
	public Image getDuplicate() {
		return fDuplicate;
	}

	public int getImageWidth() {
		return fIconWidth;
	}
	
	public int getImageHeight() {
		return fIconHeight;
	}
	
	public int getArrowHeight() {
		return fArrowHeight;
	}
	
	public int getArrowWidth() {
		return fArrowWidth;
	}
	
	public int getBreakpointHeight() {
		return fBreakpointHeight;
	}

	public int getBreakpointWidth() {
		return fBreakpointWidth;
	}

	public int getDuplicateHeight() {
		return fDuplicateHeight;
	}
	
	public int getDuplicateWidth() {
		return fDuplicateWidth;
	}
	
	public Font getFont() {
		return fParentFont;
	}
	
	protected void roundUpEven(Dimension d) {
		if (d.height % 2 != 0) d.height++; 
		if (d.width % 2 != 0) d.width++;
	}
	
	protected void createWeights(Vector stringWeights, boolean duplicate, Vector weights, IFigure parent) {
		if (stringWeights == null) return;
		for (int i = 0; i < stringWeights.size(); i++) {
			if (duplicate) {
				ImageFigure f = new ImageFigure(getDuplicate());
				f.setSize(getDuplicateWidth(), getDuplicateHeight());
				weights.add(f);
				parent.add(f);
			} else {
				createRoundRobinWeight(weights, (String) stringWeights.get(i), parent);
			}
		}
	}
	
	protected void createRoundRobinWeight(Vector weights, String weight, IFigure parent) {
		Label weightLabel = new Label(weight);
		Dimension weightSize = FigureUtilities.getTextExtents(weight, getFont());
		weightSize.height = IStreamItGraphConstants.CHANNEL_WIDTH - IStreamItGraphConstants.MARGIN*2;
		weightSize.expand(IStreamItGraphConstants.MARGIN, 0);
		if (weightSize.width < weightSize.height)
			weightSize.width = weightSize.height;

		weightLabel.setSize(weightSize);
		Ellipse e = new Ellipse();
		e.add(weightLabel);
		e.setSize(weightSize);
		weights.add(e);
		parent.add(e);
	}
}
package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.model.FeedbackLoop;
import streamit.eclipse.debugger.model.Filter;
import streamit.eclipse.debugger.model.Pipeline;
import streamit.eclipse.debugger.model.SplitJoin;


/**
 * @author kkuo
 */
public class MainPipelineWidget extends RectangleFigure implements IStreamStructureWidget {
	private Pipeline fModel;
	
	// layout
	private boolean fExpanded;
	
	// widgets
	private Label fHeader;
	private ImageFigure fArrow;
	private Vector fChildren;

	// top level pipeline
	public MainPipelineWidget(Pipeline model, String streamNameWithId, OptionData optionData, StreamItViewFactory factoryInst) throws DebugException {
		super();
		fModel = model;
		
		// create pipeline
		new StreamStructureSelector(this);
		String pipelineName = getNameWithRuntimeId();
		fExpanded = optionData.containsStream(pipelineName, false);
		Dimension pipelineSize = FigureUtilities.getTextExtents(pipelineName, factoryInst.getFont());

		// pipeline style
		setBorder(new LineBorder());
		setOpaque(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		// possible highlight
		if (streamNameWithId.equals(pipelineName)) StreamStructureSelector.setSelection(this);

		if (!fExpanded) {

			// header content
			fHeader = new Label(pipelineName, factoryInst.getPlus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);
		
			// collapsed size
			pipelineSize.expand(IStreamItGraphConstants.MARGIN, factoryInst.getArrowHeight() + IStreamItGraphConstants.MARGIN);
			
		} else {

			// header content
			fHeader = new Label(pipelineName, factoryInst.getMinus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);
		
			// expanded content
			pipelineSize.expand(IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN);

			// expanded children
			Vector children = fModel.getChildStreams();
			Object child;
			Dimension childrenSize = new Dimension(0, 0); // (total height of children, width of widest child)
			fChildren = new Vector();
			int last = children.size() - 1;
			for (int i = 0; i < last + 1; i++) {
				child = children.get(i);
				if (child instanceof Filter) {
					fChildren.add(new FilterWidget((Filter) child, streamNameWithId, optionData, this, true, true, i == last, childrenSize, factoryInst));
				} else if (child instanceof Pipeline) {
					fChildren.add(new PipelineWidget((Pipeline) child, streamNameWithId, optionData, this, true, true, i == last, childrenSize, factoryInst));
				} else if (child instanceof SplitJoin) {
					fChildren.add(new SplitJoinWidget((SplitJoin) child, streamNameWithId, optionData, this, true, true, i == last, childrenSize, factoryInst));	
				} else if (child instanceof FeedbackLoop) {
					fChildren.add(new FeedbackLoopWidget((FeedbackLoop) child, streamNameWithId, optionData, this, true, true, i == last, childrenSize, factoryInst));				
				}
			}

			// expanded size
			pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*2);
			pipelineSize.height = Math.max(childrenSize.height, pipelineSize.height + factoryInst.getArrowHeight());
			
		}
		setSize(pipelineSize);
		
		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(true));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);
		
		setVerticalLocation(null, 0);
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setVerticalLocation(org.eclipse.draw2d.geometry.Point, int)
	 */
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension pipelineSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension arrowSize = fArrow.getSize();

		setLocation(new Point(IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN));
		Point top = getBounds().getTop();
		fHeader.setLocation(top.getTranslated(-pipelineSize.width/2 + IStreamItGraphConstants.MARGIN/2, IStreamItGraphConstants.MARGIN/2));
		
		if (!fExpanded) {
			// collapsed content
			fArrow.setLocation(top.getTranslated(-arrowSize.width/2, headerSize.height + IStreamItGraphConstants.MARGIN/2));

		} else {
			fArrow.setLocation(top.getTranslated(-pipelineSize.width/2, headerSize.height + IStreamItGraphConstants.MARGIN/2));

			// fix locations of children (top to bottom)
			for (int i = 0; i < fChildren.size(); i++)
				currentHeight = ((IStreamStructureWidget) fChildren.get(i)).setVerticalLocation(top, currentHeight);
		}

		return currentHeight;				
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int, int)
	 */
	public int setHorizontalLocation(int centerHeight, boolean stretchTopChannel, boolean stretchBottomChannel, int stretchHeight, int currentWidth) {
		// should not be called
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getInputLabelTopLeft()
	 */
	public Point getTopChannelTopLeft() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getInputLabelTopRight()
	 */
	public Point getTopChannelTopRight() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomLeft()
	 */
	public Point getBottomChannelBottomLeft() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomRight()
	 */
	public Point getBottomChannelBottomRight() {
		// should not be called
		return null;
	}
	
	public void setChannelExpanded(boolean top, boolean bottom) {
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.Stream#getNameWithId()
	 */
	public String getNameWithRuntimeId() {
		return fModel.getNameWithRuntimeId();
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.Stream#getNameWithoutId()
	 */
	public String getNameWithoutId() {
		return fModel.getNameWithoutId();
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getId()
	 */
	public String getRuntimeId() {
		return fModel.getRuntimeId();
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStreamStructureWidget#getStaticId()
	 */
	public String getStaticId() {
		return fModel.getStaticId();
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.Stream#withinIcon()
	 */
	public boolean isWithinIcon(Point p) {
		return fHeader.getIconBounds().contains(p);
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#isExpanded()
	 */
	public boolean isExpanded() {
		return fExpanded;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getTopChannelToggleWidth()
	 */
	public int getTopChannelToggleWidth() {
		return 0;
	}
	
	public Vector getChildStreams() {
		return fChildren;
	}
}
package streamit.eclipse.debugger.graph;

import java.util.Collections;
import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
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
import streamit.eclipse.debugger.model.StreamStructure;

/**
 * @author kkuo
 */
public class PipelineWidget extends RectangleFigure implements IStreamStructureWidget {
	
	private Pipeline fModel;
	private boolean fExpanded;

	// widgets
	private Label fHeader;
	private ImageFigure fArrow;
	private ChannelWidget fTopChannel;
	private ChannelWidget fBottomChannel;
	private Vector fChildren;

	public PipelineWidget(StreamStructure model, String streamNameWithId, OptionData optionData, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
		fModel = (Pipeline) model;
		
		// create pipeline
		new StreamStructureSelector(this);
		String pipelineName = getNameWithRuntimeId();
		fExpanded = optionData.containsStream(pipelineName, false);
		Dimension pipelineSize = FigureUtilities.getTextExtents(pipelineName, factoryInst.getFont());

		// pipeline style
		if (optionData.isHideLines() && fExpanded) setOutline(false);
		else setBorder(new LineBorder());
		setOpaque(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		// possible highlight
		if (streamNameWithId.equals(pipelineName)) StreamStructureSelector.setSelection(this);

		if (!fExpanded) {
			
			// collapsed content
			fHeader = new Label(pipelineName, factoryInst.getPlus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);

			pipelineSize.expand(IStreamItGraphConstants.MARGIN, factoryInst.getArrowHeight() + IStreamItGraphConstants.MARGIN);
		} else {
			
			// expanded content
			fHeader = new Label(pipelineName, factoryInst.getMinus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);

			// expanded children
			Vector children = fModel.getChildStreams();
			int last;
			if (forward) {
				last = children.size() - 1;
			} else {
				last = 0;
				Collections.reverse(children);
			}
			Dimension childrenSize = new Dimension(0, 0); // (width of widest child, total height of children)
			fChildren = new Vector();
			Object child;
			for (int i = 0; i < children.size(); i++) {
				child = children.get(i);
				if (child instanceof Filter) {
					fChildren.add(new FilterWidget((Filter) child, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));
				} else if (child instanceof Pipeline) {
					fChildren.add(new PipelineWidget((Pipeline) child, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));
				} else if (child instanceof SplitJoin) {
					fChildren.add(new SplitJoinWidget((SplitJoin) child, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));	
				} else if (child instanceof FeedbackLoop) {
					fChildren.add(new FeedbackLoopWidget((FeedbackLoop) child, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));				
				}
			}
		
			// expanded size
			if (fChildren.size() == 0) {
				pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*3);
				setOutline(true);
				setBorder(new LineBorder());
			} else {
				pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*3 + ((IStreamStructureWidget) fChildren.get(0)).getTopChannelToggleWidth()*2);
			}
			pipelineSize.height = Math.max(childrenSize.height, pipelineSize.height + factoryInst.getArrowHeight());
		}
		setSize(pipelineSize);

		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(forward));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);

		// create channels
		if (forward) {
			fTopChannel = new ChannelWidget(fModel.getInputChannel(), pipelineName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fBottomChannel = new ChannelWidget(fModel.getOutputChannel(), pipelineName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);

			fTopChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));
		} else {
			fBottomChannel = new ChannelWidget(fModel.getInputChannel(), pipelineName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fTopChannel = new ChannelWidget(fModel.getOutputChannel(), pipelineName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);

			fBottomChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));
		}
		
		fTopChannel.turnOff(fExpanded);
		fBottomChannel.turnOff(fExpanded);
		((IStreamStructureWidget) parent).setChannelExpanded(fTopChannel.isExpanded(), fBottomChannel.isExpanded());
		
		// parent content
		parent.add(this);

		// parent size
		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fTopChannel.getSize().height + pipelineSize.height;
			if (lastChild) parentSize.height += fBottomChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, pipelineSize.width);
		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fTopChannel.getSize().height + pipelineSize.height + fBottomChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(pipelineSize.width, fTopChannel.getSize().width);
		}
	}

	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension topSize = fTopChannel.getSize();
		Dimension pipelineSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension arrowSize = fArrow.getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight), true)) currentHeight += topSize.height;

		setLocation(parentTopCenter.getTranslated(-pipelineSize.width/2, currentHeight));
		fHeader.setLocation(parentTopCenter.getTranslated(-pipelineSize.width/2 + IStreamItGraphConstants.MARGIN/2, IStreamItGraphConstants.MARGIN/2 + currentHeight));
					
		if (!isExpanded()) {
			// collapsed location
			fArrow.setLocation(parentTopCenter.getTranslated(-arrowSize.width/2, IStreamItGraphConstants.MARGIN/2 + headerSize.height + currentHeight));
			currentHeight += pipelineSize.height;
			
		} else {
			// expanded location
			fArrow.setLocation(parentTopCenter.getTranslated(-pipelineSize.width/2 + IStreamItGraphConstants.MARGIN/2, IStreamItGraphConstants.MARGIN/2 + headerSize.height + currentHeight));
			
			int last = fChildren.size() - 1;
			for (int i = 0; i < last + 1; i++)
				currentHeight = ((IStreamStructureWidget) fChildren.get(i)).setVerticalLocation(parentTopCenter, currentHeight);
		}

		if (fBottomChannel.setSelf(parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight), false)) currentHeight += bottomSize.height;

		return currentHeight;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, boolean stretchTopChannel, boolean stretchBottomChannel, int stretchHeight, int currentWidth) {
		Dimension topSize = fTopChannel.getSize();
		Dimension pipelineSize = getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// expand channels
		if (stretchTopChannel) {
			topSize.height = stretchHeight - pipelineSize.height - bottomSize.height;
			fTopChannel.setSize(topSize);
		} 
		if (stretchBottomChannel) {
			bottomSize.height = stretchHeight - pipelineSize.height - topSize.height;
			fBottomChannel.setSize(bottomSize);
		}
		
		setVerticalLocation(new Point(currentWidth + pipelineSize.width/2, currentHeight), 0);
		currentWidth = currentWidth +  pipelineSize.width + IStreamItGraphConstants.MARGIN/2;
		return currentWidth;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getTopChannelTopLeft()
	 */
	public Point getTopChannelTopLeft() {
		return fTopChannel.getLocation();
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getTopChannelTopRight()
	 */
	public Point getTopChannelTopRight() {
		return fTopChannel.getBounds().getTopRight().getTranslated(-1, 0);
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getBottomChannelBottomLeft()
	 */
	public Point getBottomChannelBottomLeft() {
		return fBottomChannel.getBounds().getBottomLeft();
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getBottomChannelBottomRight()
	 */
	public Point getBottomChannelBottomRight() {
		return fBottomChannel.getBounds().getBottomRight().getTranslated(-1, 0);
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
		return fTopChannel.getChannelToggleWidth();
	}
	
	public Vector getChildStreams() {
		return fChildren;
	}
}
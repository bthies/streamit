package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polygon;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.model.FeedbackLoop;
import streamit.eclipse.debugger.model.Filter;
import streamit.eclipse.debugger.model.Pipeline;
import streamit.eclipse.debugger.model.SplitJoin;

/**
 * @author kkuo
 */
public class FeedbackLoopWidget extends Ellipse implements IStreamStructureWidget {
	private FeedbackLoop fModel;
	private boolean fExpanded;
	private int fTallestChild, fChildWidth;
	private boolean fStretchTopChannel, fStretchBottomChannel;
	
	// widgets	
	private Label fHeader;
	private ImageFigure fArrow;
	private ChannelWidget fTopChannel;
	private ChannelWidget fBottomChannel;
	private Polygon fJoiner;
	private Polygon fSplitter;
	private Vector fSplitterWeights;	// Vector of Figures
	private Vector fJoinerWeights;		// Vector of Figures
	private Vector fChildren;

	public FeedbackLoopWidget(FeedbackLoop model, String streamNameWithId, OptionData optionData, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
		fModel = model;
		fStretchTopChannel = false;
		fStretchBottomChannel = false;
		
		// create feedbackloop
		new StreamStructureSelector(this);
		String feedbackloopName = getNameWithRuntimeId();
		fExpanded = optionData.containsStream(feedbackloopName, false);
		fHeader = new Label(feedbackloopName);
		Dimension feedbackloopSize = FigureUtilities.getTextExtents(feedbackloopName, factoryInst.getFont());
		
		// feedbackloop style
		if (optionData.isHideLines() && fExpanded) setOutline(false);
		else setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);
		
		// possible highlight
		if (streamNameWithId.equals(feedbackloopName)) StreamStructureSelector.setSelection(this);

		if (!fExpanded) {

			// collapsed content
			fHeader.setIcon(factoryInst.getPlus());
			feedbackloopSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			StreamItViewFactory.getInstance().roundUpEven(feedbackloopSize);
			fHeader.setSize(feedbackloopSize);
			add(fHeader);
			
			// collapsed size
			feedbackloopSize.expand(IStreamItGraphConstants.MARGIN*4, factoryInst.getArrowHeight()*2 + IStreamItGraphConstants.MARGIN);
			
		} else {
			
			// expanded header
			fHeader.setIcon(factoryInst.getMinus());
			feedbackloopSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(feedbackloopSize);
			fHeader.setSize(feedbackloopSize);
			add(fHeader);
			
			// expanded joiner
			fJoiner = new Polygon();
			add(fJoiner);

			// joiner weights
			fJoinerWeights = new Vector();
			factoryInst.createWeights(fModel.getWeights(false), false, fJoinerWeights, this);

			// flip either body (0) or loop (1)
			int flip;
			if (forward) flip = 1;
			else flip = 0;

			// expanded children
			Vector children = fModel.getChildStreams();
			Dimension childrenSize = new Dimension(IStreamItGraphConstants.MARGIN/2, 0); // (total width of children, height of tallest child)
			fChildren = new Vector();
			Object child;
			int numChildren = children.size(); 
			for (int i = 0; i < numChildren; i++) {
				child = children.get(i);
				if (child instanceof Filter) {
					fChildren.add(new FilterWidget((Filter) child, streamNameWithId, optionData, this, i != flip, false, true, childrenSize, factoryInst));
				} else if (child instanceof Pipeline) {
					fChildren.add(new PipelineWidget((Pipeline) child, streamNameWithId, optionData,  this, i != flip, false, true, childrenSize, factoryInst));
				} else if (child instanceof SplitJoin) {
					fChildren.add(new SplitJoinWidget((SplitJoin) child, streamNameWithId, optionData, this, i != flip, false, true, childrenSize, factoryInst));	
				} else if (child instanceof FeedbackLoop) {
					fChildren.add(new FeedbackLoopWidget((FeedbackLoop) child, streamNameWithId, optionData, this, i != flip, false, true, childrenSize, factoryInst));
				}
				childrenSize.width += IStreamItGraphConstants.MARGIN/2;
			}
			if (numChildren == 0) setOutline(true);

			// expanded splitter
			fSplitter = new Polygon();
			add(fSplitter);

			// splitter weights
			fSplitterWeights = new Vector();
			factoryInst.createWeights(fModel.getWeights(true), true && fModel.isDuplicateSplitter(), fSplitterWeights, this);
		
			// expanded size
			fTallestChild = childrenSize.height;
			fChildWidth = childrenSize.width;
			feedbackloopSize.width = Math.max(childrenSize.width, feedbackloopSize.width*2 + IStreamItGraphConstants.CHANNEL_WIDTH + IStreamItGraphConstants.MARGIN*2) + feedbackloopSize.width*3/4;
			feedbackloopSize.height = Math.max(childrenSize.height, feedbackloopSize.height) + feedbackloopSize.height*2 + IStreamItGraphConstants.MARGIN*2 + IStreamItGraphConstants.CHANNEL_WIDTH*8;
		}
		setSize(feedbackloopSize);		
		
		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(forward));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);

		// create channels
		if (forward) {
			fTopChannel = new ChannelWidget(fModel.getInputChannel(), feedbackloopName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fBottomChannel = new ChannelWidget(fModel.getOutputChannel(), feedbackloopName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fTopChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));
		} else {
			fBottomChannel = new ChannelWidget(fModel.getInputChannel(), feedbackloopName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fTopChannel = new ChannelWidget(fModel.getOutputChannel(), feedbackloopName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fBottomChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));
			
			// swap joiner & splitter
			Vector temp = fJoinerWeights;
			fJoinerWeights = fSplitterWeights;
			fSplitterWeights = temp;
		}
		((IStreamStructureWidget) parent).setChannelExpanded(fTopChannel.isExpanded(), fBottomChannel.isExpanded());

		// collapsed content
		parent.add(this);

		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fTopChannel.getSize().height + feedbackloopSize.height;
			if (lastChild) parentSize.height += fBottomChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, feedbackloopSize.width);
		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fTopChannel.getSize().height + feedbackloopSize.height + fBottomChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(feedbackloopSize.width, fTopChannel.getSize().width);
		}
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setLocation(org.eclipse.draw2d.geometry.Point, int)
	 */
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension topSize = fTopChannel.getSize();
		Dimension feedbackloopSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension arrowSize = fArrow.getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// Channel
		if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight), true)) currentHeight += topSize.height;

		Point feedbackloopTopCenter = parentTopCenter.getTranslated(0, currentHeight);
		setLocation(parentTopCenter.getTranslated(-feedbackloopSize.width/2, currentHeight));
		
		if (!isExpanded()) {
			// collapsed location
			fHeader.setLocation(feedbackloopTopCenter.getTranslated(-headerSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			fArrow.setLocation(feedbackloopTopCenter.getTranslated(-arrowSize.width/2, headerSize.height + arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			currentHeight += feedbackloopSize.height;

			// Channel
			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft, false)) currentHeight += bottomSize.height;
		} else {
			// expanded location
			int numChildren = fChildren.size();
			if (numChildren > 0) {
				// layout children (body and loop)
				int currentWidth = feedbackloopTopCenter.x - fChildWidth/2 + IStreamItGraphConstants.MARGIN/2;
				int childHeight = feedbackloopTopCenter.y + IStreamItGraphConstants.CHANNEL_WIDTH*4 + headerSize.height + IStreamItGraphConstants.MARGIN;
				IStreamStructureWidget child;
				for (int i = 0; i < numChildren; i++) {
					child = (IStreamStructureWidget) fChildren.get(i);
					currentWidth = child.setHorizontalLocation(childHeight, fStretchTopChannel, fStretchBottomChannel, fTallestChild, currentWidth);
				}

				// fJoiner
				Point bottomLeft = ((IStreamStructureWidget) fChildren.get(0)).getTopChannelTopLeft();
				Point bottomRight = ((IStreamStructureWidget) fChildren.get(numChildren - 1)).getTopChannelTopRight();
				Point topLeft = feedbackloopTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0);
				Point topRight = feedbackloopTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 - 1, 0);
				Point middleLeft = new Point(topLeft.x, bottomLeft.y - IStreamItGraphConstants.CHANNEL_WIDTH);
				Point middleRight = new Point(topRight.x, bottomRight.y - IStreamItGraphConstants.CHANNEL_WIDTH);

				fJoiner.addPoint(bottomLeft.getCopy());			
				fJoiner.addPoint(bottomLeft.getTranslated(0, -IStreamItGraphConstants.CHANNEL_WIDTH));
				fJoiner.addPoint(middleLeft);
				fJoiner.addPoint(topLeft);
				fJoiner.addPoint(topRight);
				fJoiner.addPoint(middleRight);
				fJoiner.addPoint(bottomRight.getTranslated(0, -IStreamItGraphConstants.CHANNEL_WIDTH)); 
				fJoiner.addPoint(bottomRight.getCopy());
			}
			
			currentHeight += feedbackloopSize.height;

			// Channel
			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft, false)) currentHeight += bottomSize.height;
			
			if (numChildren > 0) {
				// fSplitter
				Point topLeft = ((IStreamStructureWidget) fChildren.get(0)).getBottomChannelBottomLeft();
				Point topRight = ((IStreamStructureWidget) fChildren.get(numChildren - 1)).getBottomChannelBottomRight();
				Point bottomRight = bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH - 1, 0);
				Point middleLeft = new Point(bottomLeft.x, topLeft.y + IStreamItGraphConstants.CHANNEL_WIDTH);
				Point middleRight = new Point(bottomRight.x, topRight.y + IStreamItGraphConstants.CHANNEL_WIDTH);
				
				fSplitter.addPoint(topLeft.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
				fSplitter.addPoint(topLeft.getCopy());
				fSplitter.addPoint(topRight.getCopy());
				fSplitter.addPoint(topRight.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
				fSplitter.addPoint(middleRight);
				fSplitter.addPoint(bottomRight);
				fSplitter.addPoint(bottomLeft.getCopy());
				fSplitter.addPoint(middleLeft);
			}

			// set fJoinerWeights, fSplitterWeights
			Point p;
			boolean setJoinerWeights = fJoinerWeights.size() == numChildren;
			boolean setSplitterWeights = fSplitterWeights.size() == numChildren;
			for (int i = 0; i < numChildren; i++) {
				if (setJoinerWeights) {
					p = ((IStreamStructureWidget) fChildren.get(i)).getTopChannelTopLeft();
					((IFigure) fJoinerWeights.get(i)).setLocation(p.getTranslated(IStreamItGraphConstants.MARGIN, -IStreamItGraphConstants.CHANNEL_WIDTH + IStreamItGraphConstants.MARGIN));
				}
				if (setSplitterWeights) {
					p = ((IStreamStructureWidget) fChildren.get(i)).getBottomChannelBottomLeft();
					((IFigure) fSplitterWeights.get(i)).setLocation(p.getTranslated(IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN));
				}
			}
									 
			// fHeader
			Point headerLocation = feedbackloopTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2 - IStreamItGraphConstants.MARGIN/2 - headerSize.width, 
																		IStreamItGraphConstants.CHANNEL_WIDTH*4 - headerSize.height - IStreamItGraphConstants.MARGIN*2);
			fHeader.setLocation(headerLocation);
			fArrow.setLocation(headerLocation.getTranslated(0, headerSize.height));
		}
		
		return currentHeight;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, boolean stretchTopChannel, boolean stretchBottomChannel, int stretchHeight, int currentWidth) {
		Dimension topSize = fTopChannel.getSize();
		Dimension feedbackloopSize = getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// expand channels
		if (stretchTopChannel) {
			topSize.height = stretchHeight - feedbackloopSize.height - bottomSize.height;
			fTopChannel.setSize(topSize);
		} 
		if (stretchBottomChannel) {
			bottomSize.height = stretchHeight - feedbackloopSize.height - topSize.height;
			fBottomChannel.setSize(bottomSize);
		}
		
		setVerticalLocation(new Point(currentWidth + feedbackloopSize.width/2, currentHeight), 0);
		currentWidth = currentWidth +  feedbackloopSize.width + IStreamItGraphConstants.MARGIN/2;
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
	 * @see streamit.eclipse.debugger.graph.IStream#getBottomChannelBottomLeft()
	 */
	public Point getBottomChannelBottomRight() {
		return fBottomChannel.getBounds().getBottomRight().getTranslated(-1, 0);
	}

	public void setChannelExpanded(boolean top, boolean bottom) {
		if (!fStretchTopChannel && top) fStretchTopChannel = top;
		if (!fStretchBottomChannel && bottom) fStretchBottomChannel = bottom;
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
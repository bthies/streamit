package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polygon;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaValue;

/**
 * @author kkuo
 */
public class FeedbackLoop extends Ellipse implements IStream {

	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private ImageFigure fArrow;
	
	private boolean fExpanded;
	private Channel fTopChannel;
	private Channel fBottomChannel;
	private Vector fChildren;
	
	private Polygon fSplitter;
	private Polygon fJoiner;
	private int fTallestChild;
	private int fChildWidth;

	public FeedbackLoop(IValue feedbackloopVal, String name, String streamNameWithId, Expanded allExpanded, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
		
		// create feedbackloop
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = feedbackloopVal.getValueString();
		String feedbackloopName = getNameWithId();
		fExpanded = allExpanded.containsStream(feedbackloopName, false);
		fHeader = new Label(feedbackloopName);
		Dimension feedbackloopSize = FigureUtilities.getTextExtents(feedbackloopName, factoryInst.getFont());

		// feedbackloop style
		setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);
		
		// possible highlight
		if (streamNameWithId.equals(feedbackloopName)) StreamSelector.setSelection(this);

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
			
			// expanded splitter
			fSplitter = new Polygon();
			add(fSplitter);

			// flip either body (0) or loop (1)
			int flip;
			if (forward) flip = 1;
			else flip = 0;

			// expanded children
			Vector elements = new Vector();
			elements.add(factoryInst.getVariable(feedbackloopVal.getVariables(), "body"));
			elements.add(factoryInst.getVariable(feedbackloopVal.getVariables(), "loop"));
			IJavaValue val;
			IJavaClassType type;
			String streamType, streamName;
			Dimension childrenSize = new Dimension(IStreamItGraphConstants.MARGIN/2, 0); // (total width of children, height of tallest child)
			fChildren = new Vector();
			for (int i = 0; i < elements.size(); i++) {
				val = (IJavaValue) ((IVariable) elements.get(i)).getValue();
				type = (IJavaClassType) val.getJavaType();
				streamName = type.getName();
				streamType = type.getSuperclass().getName();
				
				if (streamType.equals("streamit.library.Filter")) {
					fChildren.add(new Filter(val, streamName, streamNameWithId, allExpanded, this, i != flip, false, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.Pipeline")) {
					fChildren.add(new Pipeline(val, streamName, streamNameWithId, allExpanded,  this, i != flip, false, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.SplitJoin")) {
					fChildren.add(new SplitJoin(val, streamName, streamNameWithId, allExpanded, this, i != flip, false, true, childrenSize, factoryInst));	
				} else if (streamType.equals("streamit.library.FeedbackLoop")) {
					fChildren.add(new FeedbackLoop(val, streamName, streamNameWithId, allExpanded, this, i != flip, false, true, childrenSize, factoryInst));
				}
				childrenSize.width += IStreamItGraphConstants.MARGIN/2;
			}
			
			// expanded splitter
			fJoiner = new Polygon();
			add(fJoiner);
		
			// expanded size
			fTallestChild = childrenSize.height;
			fChildWidth = childrenSize.width;
			feedbackloopSize.width = Math.max(childrenSize.width, feedbackloopSize.width*2 + IStreamItGraphConstants.CHANNEL_WIDTH + IStreamItGraphConstants.MARGIN*2) + feedbackloopSize.width*3;
			feedbackloopSize.height = Math.max(childrenSize.height, feedbackloopSize.height) + IStreamItGraphConstants.CHANNEL_WIDTH*2 + feedbackloopSize.height*2 + IStreamItGraphConstants.MARGIN*2 + factoryInst.getArrowHeight()*2;
		}
		setSize(feedbackloopSize);
		
		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(forward));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);

		// create channels
		IVariable[] splitjoinVars = feedbackloopVal.getVariables();
		if (forward) {
			fTopChannel = new Channel(factoryInst.findVariables(splitjoinVars, "input"), feedbackloopName + '1', parent, true, forward, lastChild, allExpanded, factoryInst);
			fBottomChannel = new Channel(factoryInst.findVariables(splitjoinVars, "output"), feedbackloopName + '0', parent, false, forward, lastChild, allExpanded, factoryInst);
		} else {
			fBottomChannel = new Channel(factoryInst.findVariables(splitjoinVars, "input"), feedbackloopName + '1', parent, true, forward, lastChild, allExpanded, factoryInst);
			fTopChannel = new Channel(factoryInst.findVariables(splitjoinVars, "output"), feedbackloopName + '0', parent, false, forward, lastChild, allExpanded, factoryInst);
		}

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
		if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight))) currentHeight += topSize.height;

		Point feedbackloopTopCenter = parentTopCenter.getTranslated(0, currentHeight);
		setLocation(parentTopCenter.getTranslated(-feedbackloopSize.width/2, currentHeight));
		
		if (!isExpanded()) {
			// collapsed location
			fHeader.setLocation(feedbackloopTopCenter.getTranslated(-headerSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			fArrow.setLocation(feedbackloopTopCenter.getTranslated(-arrowSize.width/2, headerSize.height + arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			currentHeight += feedbackloopSize.height;

			// Channel
			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft)) currentHeight += bottomSize.height;
		} else {
			// expanded location
			
			// layout children (body and loop)
			int currentWidth = feedbackloopTopCenter.x - fChildWidth/2 + IStreamItGraphConstants.MARGIN/2;
			currentWidth = ((IStream) fChildren.get(0)).setHorizontalLocation(feedbackloopTopCenter.y + IStreamItGraphConstants.CHANNEL_WIDTH + headerSize.height + IStreamItGraphConstants.MARGIN + arrowSize.height, fTallestChild, currentWidth);
			currentWidth = ((IStream) fChildren.get(1)).setHorizontalLocation(feedbackloopTopCenter.y + IStreamItGraphConstants.CHANNEL_WIDTH + headerSize.height + IStreamItGraphConstants.MARGIN + arrowSize.height, fTallestChild, currentWidth);
			
			// "fSplitter"
			Point bottomLeft = ((IStream) fChildren.get(0)).getTopChannelTopLeft();
			Point bottomRight = ((IStream) fChildren.get(1)).getTopChannelTopRight();
			Point topLeft = feedbackloopTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0);
			Point topRight = feedbackloopTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 - 1, 0);
			Point middleLeft = new Point(topLeft.x, bottomLeft.y - IStreamItGraphConstants.CHANNEL_WIDTH);
			Point middleRight = new Point(topRight.x, bottomRight.y - IStreamItGraphConstants.CHANNEL_WIDTH);
				
			fSplitter.addPoint(bottomLeft.getCopy());
			fSplitter.addPoint(bottomLeft.getTranslated(0, -IStreamItGraphConstants.CHANNEL_WIDTH));
			fSplitter.addPoint(middleLeft);
			fSplitter.addPoint(topLeft);
			fSplitter.addPoint(topRight);
			fSplitter.addPoint(middleRight);
			fSplitter.addPoint(bottomRight.getTranslated(0, -IStreamItGraphConstants.CHANNEL_WIDTH)); 
			fSplitter.addPoint(bottomRight.getCopy());
			
			currentHeight += feedbackloopSize.height;

			// Channel
			bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft)) currentHeight += bottomSize.height;
			
			// "fJoiner"	
			topLeft = ((IStream) fChildren.get(0)).getBottomChannelBottomLeft();
			topRight = ((IStream) fChildren.get(1)).getBottomChannelBottomRight();
			bottomRight = bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH - 1, 0);
			middleLeft = new Point(bottomLeft.x, topLeft.y + IStreamItGraphConstants.CHANNEL_WIDTH);
			middleRight = new Point(bottomRight.x, topRight.y + IStreamItGraphConstants.CHANNEL_WIDTH);
				
			fJoiner.addPoint(topLeft.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
			fJoiner.addPoint(topLeft.getCopy());
			fJoiner.addPoint(topRight.getCopy());
			fJoiner.addPoint(topRight.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
			fJoiner.addPoint(middleRight);
			fJoiner.addPoint(bottomRight);
			fJoiner.addPoint(bottomLeft.getCopy());
			fJoiner.addPoint(middleLeft);
			
			// fHeader
			Point headerLocation = feedbackloopTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2 - IStreamItGraphConstants.MARGIN/2 - headerSize.width, arrowSize.height + IStreamItGraphConstants.MARGIN/2); 
			fHeader.setLocation(headerLocation);
			fArrow.setLocation(headerLocation.getTranslated(0, headerSize.height));
		}
		
		return currentHeight;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth) {
		Dimension topSize = fTopChannel.getSize();
		Dimension feedbackloopSize = getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// expand channels
		topSize.height = (stretchHeight - feedbackloopSize.height)/2;
		fTopChannel.setSize(topSize);
		bottomSize.height = (stretchHeight - feedbackloopSize.height)/2;
		fBottomChannel.setSize(bottomSize);
		
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
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.Stream#getNameWithId()
	 */
	public String getNameWithId() {
		return fNameWithoutId + fId;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.Stream#getNameWithoutId()
	 */
	public String getNameWithoutId() {
		return fNameWithoutId;
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
	 * @see streamit.eclipse.debugger.graph.IStream#getId()
	 */
	public String getId() {
		return fId;
	}
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getTopChannelToggleWidth()
	 */
	public int getTopChannelToggleWidth() {
		return fTopChannel.getChannelToggleWidth();
	}	
}
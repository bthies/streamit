package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
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
public class SplitJoin extends Polygon implements IStream {
	
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
	
	public SplitJoin(IValue splitjoinVal, String name, String streamNameWithId, OptionData optionData, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
	
		// create splitjoin
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = splitjoinVal.getValueString();
		String splitjoinName = getNameWithId();
		fExpanded = optionData.containsStream(splitjoinName, false);
		fHeader = new Label(splitjoinName);
		Dimension splitjoinSize = FigureUtilities.getTextExtents(splitjoinName, factoryInst.getFont());

		// splitjoin style
		if (optionData.isHideLines() && fExpanded) setOutline(false);
		else setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		// possible highlight
		if (streamNameWithId.equals(splitjoinName)) StreamSelector.setSelection(this);
		
		if (!fExpanded) {

			// collapsed content
			fHeader.setIcon(factoryInst.getPlus());
			splitjoinSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(splitjoinSize);
			fHeader.setSize(splitjoinSize);
			add(fHeader);
			
			// collapsed size
			splitjoinSize.expand(IStreamItGraphConstants.MARGIN, factoryInst.getArrowHeight()*2 + IStreamItGraphConstants.MARGIN);
		} else {
			
			// expanded header
			fHeader.setIcon(factoryInst.getMinus());
			splitjoinSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(splitjoinSize);
			fHeader.setSize(splitjoinSize);
			add(fHeader);
			
			// expanded splitter
			fSplitter = new Polygon();
			add(fSplitter);

			// expanded children
			IVariable[] vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(splitjoinVal.getVariables(), "childrenStreams"), "header"), "next");
			Vector elements = factoryInst.getLinkedListElements(vars);
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
					fChildren.add(new Filter(val, streamName, streamNameWithId, optionData, this, forward, false, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.Pipeline")) {
					fChildren.add(new Pipeline(val, streamName, streamNameWithId, optionData,  this, forward, false, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.SplitJoin")) {
					fChildren.add(new SplitJoin(val, streamName, streamNameWithId, optionData, this, forward, false, true, childrenSize, factoryInst));	
				} else if (streamType.equals("streamit.library.FeedbackLoop")) {
					fChildren.add(new FeedbackLoop(val, streamName, streamNameWithId, optionData, this, forward, false, true, childrenSize, factoryInst));
				}
				childrenSize.width += IStreamItGraphConstants.MARGIN/2;
			}
			if (fChildren.size() == 0) setOutline(true);
			
			// expanded splitter
			fJoiner = new Polygon();
			add(fJoiner);
		
			// expanded size
			fTallestChild = childrenSize.height;
			fChildWidth = childrenSize.width;			
			splitjoinSize.width = Math.max(childrenSize.width, splitjoinSize.width*2 + IStreamItGraphConstants.CHANNEL_WIDTH + IStreamItGraphConstants.MARGIN*2);
			splitjoinSize.height = Math.max(childrenSize.height, splitjoinSize.height) + IStreamItGraphConstants.CHANNEL_WIDTH*2 + splitjoinSize.height*2 + IStreamItGraphConstants.MARGIN*2 + factoryInst.getArrowHeight()*2;
		}
		setSize(splitjoinSize);
		
		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(forward));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);

		// create channels
		IVariable[] splitjoinVars = splitjoinVal.getVariables();
		if (forward) {
			fTopChannel = new Channel(factoryInst.findVariables(splitjoinVars, "input"), splitjoinName + '1', parent, true, forward, lastChild, optionData, factoryInst);
			fBottomChannel = new Channel(factoryInst.findVariables(splitjoinVars, "output"), splitjoinName + '0', parent, false, forward, lastChild, optionData, factoryInst);
		} else {
			fBottomChannel = new Channel(factoryInst.findVariables(splitjoinVars, "input"), splitjoinName + '1', parent, true, forward, lastChild, optionData, factoryInst);
			fTopChannel = new Channel(factoryInst.findVariables(splitjoinVars, "output"), splitjoinName + '0', parent, false, forward, lastChild, optionData, factoryInst);
		}

		// collapsed content
		parent.add(this);
			
		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fTopChannel.getSize().height + splitjoinSize.height;
			if (lastChild) parentSize.height += fBottomChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, splitjoinSize.width);
		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fTopChannel.getSize().height + splitjoinSize.height + fBottomChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(splitjoinSize.width, fTopChannel.getSize().width);
		}
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setLocation(org.eclipse.draw2d.geometry.Point, int)
	 */
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension topSize = fTopChannel.getSize();
		Dimension splitjoinSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension arrowSize = fArrow.getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight))) currentHeight += topSize.height;

		Point splitjoinTopCenter = parentTopCenter.getTranslated(0, currentHeight);
		setLocation(parentTopCenter.getTranslated(-splitjoinSize.width/2, currentHeight));
		
		if (!isExpanded()) {
			// collapsed location
			fHeader.setLocation(splitjoinTopCenter.getTranslated(-headerSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			fArrow.setLocation(splitjoinTopCenter.getTranslated(-arrowSize.width/2, headerSize.height + arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			currentHeight += splitjoinSize.height;

			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft)) currentHeight += bottomSize.height;
			
			// splitjoin points (8)
			addPoint(splitjoinTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
			addPoint(splitjoinTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 - 1, 0));
			addPoint(splitjoinTopCenter.getTranslated(splitjoinSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			addPoint(bottomLeft.getTranslated(splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height - IStreamItGraphConstants.MARGIN/2));
			addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
			addPoint(bottomLeft.getCopy());
			addPoint(bottomLeft.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height - IStreamItGraphConstants.MARGIN/2));
			addPoint(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
		} else {
			// expanded location
			if (fChildren.size() > 0) {
				
				// layout children
				int currentWidth = splitjoinTopCenter.x - fChildWidth/2 + IStreamItGraphConstants.MARGIN/2;
				for (int i = 0; i < fChildren.size(); i++)
					currentWidth = ((IStream) fChildren.get(i)).setHorizontalLocation(splitjoinTopCenter.y + IStreamItGraphConstants.CHANNEL_WIDTH + headerSize.height + IStreamItGraphConstants.MARGIN + arrowSize.height, fTallestChild, currentWidth);
				
				
				// fSplitter
				Point bottomLeft = ((IStream) fChildren.get(0)).getTopChannelTopLeft();
				Point bottomRight = ((IStream) fChildren.get(fChildren.size() - 1)).getTopChannelTopRight();
				Point topLeft = splitjoinTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0);
				Point topRight = splitjoinTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 - 1, 0);
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
			}
			
			currentHeight += splitjoinSize.height;
			
			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft)) currentHeight += bottomSize.height;
			
			if (fChildren.size() > 0) {
				
				// fJoiner				
				Point topLeft = ((IStream) fChildren.get(0)).getBottomChannelBottomLeft();
				Point topRight = ((IStream) fChildren.get(fChildren.size() - 1)).getBottomChannelBottomRight();
				Point bottomRight = bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH - 1, 0);
				Point middleLeft = new Point(bottomLeft.x, topLeft.y + IStreamItGraphConstants.CHANNEL_WIDTH);
				Point middleRight = new Point(bottomRight.x, topRight.y + IStreamItGraphConstants.CHANNEL_WIDTH);
				
				fJoiner.addPoint(topLeft.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
				fJoiner.addPoint(topLeft.getCopy());
				fJoiner.addPoint(topRight.getCopy());
				fJoiner.addPoint(topRight.getTranslated(0, IStreamItGraphConstants.CHANNEL_WIDTH));
				fJoiner.addPoint(middleRight);
				fJoiner.addPoint(bottomRight);
				fJoiner.addPoint(bottomLeft.getCopy());
				fJoiner.addPoint(middleLeft);
				
			}

			// splitjoin points (8)
			addPoint(splitjoinTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
			addPoint(splitjoinTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));			
			addPoint(splitjoinTopCenter.getTranslated(splitjoinSize.width/2, arrowSize.height));
			addPoint(bottomLeft.getTranslated(splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height));
			addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
			addPoint(bottomLeft.getCopy());
			addPoint(bottomLeft.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -arrowSize.height));
			addPoint(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2, arrowSize.height));
			
			// fHeader
			fHeader.setLocation(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.MARGIN/2, arrowSize.height + IStreamItGraphConstants.MARGIN/2));
			fArrow.setLocation(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.MARGIN/2, headerSize.height + arrowSize.height + IStreamItGraphConstants.MARGIN/2));
		}
		
		return currentHeight;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth) {
		Dimension topSize = fTopChannel.getSize();
		Dimension splitjoinSize = getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// expand channels
		topSize.height = (stretchHeight - splitjoinSize.height)/2;
		fTopChannel.setSize(topSize);
		bottomSize.height = (stretchHeight - splitjoinSize.height)/2;
		fBottomChannel.setSize(bottomSize);
		
		setVerticalLocation(new Point(currentWidth + splitjoinSize.width/2, currentHeight), 0);
		currentWidth = currentWidth +  splitjoinSize.width + IStreamItGraphConstants.MARGIN/2;
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

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getNameWithId()
	 */
	public String getNameWithId() {
		return fNameWithoutId + fId;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getNameWithoutId()
	 */
	public String getNameWithoutId() {
		return fNameWithoutId;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getId()
	 */
	public String getId() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#isWithinIcon(org.eclipse.draw2d.geometry.Point)
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
}
package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polygon;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.swt.graphics.Font;

/**
 * @author kkuo
 */
public class SplitJoin extends Polygon implements IStream {
	
	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private boolean fExpanded;
	private Label fInputChannel;
	private Label fOutputChannel;
	private Vector fChildren;
	
	private Polygon fSplitter;
	private Polygon fJoiner;
	private int fTallestChild;
	private int fChildWidth;
	
	public SplitJoin(IValue splitjoinVal, String name, Font parentFont, String streamNameWithId, Expanded allExpanded, Figure parent, boolean verticalLayout, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
	
		// create splitjoin
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = splitjoinVal.getValueString();
		String splitjoinName = getNameWithId();
		fExpanded = allExpanded.contains(splitjoinName, false);
		fHeader = new Label(splitjoinName);
		Dimension splitjoinSize = FigureUtilities.getTextExtents(splitjoinName, parentFont);

		// splitjoin style
		setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);
		
		// possible highlight
		if (streamNameWithId.equals(splitjoinName)) StreamSelector.setSelection(this);
		
		if (!fExpanded) {

			// collapsed content
			fHeader.setIcon(factoryInst.getPlus());
			splitjoinSize.expand(fHeader.getIconTextGap() + factoryInst.getPlus().getBounds().width, 0);
			factoryInst.roundUpEven(splitjoinSize);
			fHeader.setSize(splitjoinSize);
			add(fHeader);
			
			// collapsed size
			splitjoinSize.expand(IStreamItGraphConstants.MARGIN, splitjoinSize.height + IStreamItGraphConstants.MARGIN);
			
			// highlight
			StreamSelector.setSelection(this);
		} else {
			
			// expanded header
			fHeader.setIcon(factoryInst.getMinus());
			splitjoinSize.expand(fHeader.getIconTextGap() + factoryInst.getMinus().getBounds().width, 0);
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
					fChildren.add(new Filter(val, streamName, parentFont, streamNameWithId, allExpanded, this, false, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.Pipeline")) {
					fChildren.add(new Pipeline(val, streamName, parentFont, streamNameWithId, allExpanded,  this, false, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.SplitJoin")) {
					fChildren.add(new SplitJoin(val, streamName, parentFont, streamNameWithId, allExpanded, this, false, childrenSize, factoryInst));	
				} else if (streamType.equals("streamit.library.FeedbackLoop")) {
					fChildren.add(new FeedbackLoop(val, streamName, parentFont, streamNameWithId, allExpanded, this, false, childrenSize, factoryInst));
				}
				childrenSize.width += IStreamItGraphConstants.MARGIN/2;
			}
			
			// expanded splitter
			fJoiner = new Polygon();
			add(fJoiner);
		
			// expanded size
			fTallestChild = childrenSize.height;
			fChildWidth = childrenSize.width;
			splitjoinSize.width = Math.max(childrenSize.width, splitjoinSize.width*2 + IStreamItGraphConstants.CHANNEL_WIDTH + IStreamItGraphConstants.MARGIN*2);
			splitjoinSize.height = Math.max(childrenSize.height, splitjoinSize.height) + IStreamItGraphConstants.CHANNEL_WIDTH*2 + splitjoinSize.height*4;
		}

		setSize(splitjoinSize);

		// create channels
		IVariable[] splitjoinVars = splitjoinVal.getVariables();
		fInputChannel = new Channel(factoryInst.findVariables(splitjoinVars, "input"), parentFont, true, factoryInst);
		fOutputChannel = new Channel(factoryInst.findVariables(splitjoinVars, "output"), parentFont, false, factoryInst);

		// collapsed content
		parent.add(fInputChannel);
		parent.add(this);
		parent.add(fOutputChannel);
			
		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fInputChannel.getSize().height + splitjoinSize.height + fOutputChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, splitjoinSize.width);
		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fInputChannel.getSize().height + splitjoinSize.height + fOutputChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(splitjoinSize.width, fInputChannel.getSize().width);
		}
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setLocation(org.eclipse.draw2d.geometry.Point, int)
	 */
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension inputSize = fInputChannel.getSize();
		Dimension splitjoinSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension outputSize = fOutputChannel.getSize();

		fInputChannel.setLocation(parentTopCenter.getTranslated(-inputSize.width/2, currentHeight));
		currentHeight += inputSize.height;

		Point splitjoinTopCenter = parentTopCenter.getTranslated(0, currentHeight);
		setLocation(parentTopCenter.getTranslated(-splitjoinSize.width/2, currentHeight));
		
		if (!isExpanded()) {
			// collapsed location
			fHeader.setLocation(splitjoinTopCenter.getTranslated(-headerSize.width/2, headerSize.height/2 + IStreamItGraphConstants.MARGIN/2));
			currentHeight += splitjoinSize.height;

			Point bottomLeft = parentTopCenter.getTranslated(-outputSize.width/2, currentHeight);
			fOutputChannel.setLocation(bottomLeft);
			currentHeight += outputSize.height;

			// splitjoin points (8)
			addPoint(splitjoinTopCenter.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
			addPoint(splitjoinTopCenter.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 - 1, 0));
			addPoint(splitjoinTopCenter.getTranslated(splitjoinSize.width/2, headerSize.height/2 + IStreamItGraphConstants.MARGIN/2));
			addPoint(bottomLeft.getTranslated(splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -headerSize.height/2 - IStreamItGraphConstants.MARGIN/2));
			addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
			addPoint(bottomLeft.getCopy());
			addPoint(bottomLeft.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -headerSize.height/2 - IStreamItGraphConstants.MARGIN/2));
			addPoint(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2, headerSize.height/2 + IStreamItGraphConstants.MARGIN/2));
		} else {
			// expanded location
			
			if (fChildren.size() > 0) {
				
				// layout children
				int currentWidth = splitjoinTopCenter.x - fChildWidth/2 + IStreamItGraphConstants.MARGIN/2;
				for (int i = 0; i < fChildren.size(); i++)
					currentWidth = ((IStream) fChildren.get(i)).setHorizontalLocation(splitjoinTopCenter.y + headerSize.height*2 + IStreamItGraphConstants.CHANNEL_WIDTH, fTallestChild, currentWidth);
				
				// fSplitter
				Point bottomLeft = ((IStream) fChildren.get(0)).getInputChannelTopLeft();
				Point bottomRight = ((IStream) fChildren.get(fChildren.size() - 1)).getInputChannelTopRight();
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
			
			Point bottomLeft = parentTopCenter.getTranslated(-outputSize.width/2, currentHeight);
			fOutputChannel.setLocation(bottomLeft);
			currentHeight += outputSize.height;

			if (fChildren.size() > 0) {
				
				// fJoiner				
				Point topLeft = ((IStream) fChildren.get(0)).getOutputChannelBottomLeft();
				Point topRight = ((IStream) fChildren.get(fChildren.size() - 1)).getOutputChannelBottomRight();
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
			addPoint(splitjoinTopCenter.getTranslated(splitjoinSize.width/2, headerSize.height));
			addPoint(bottomLeft.getTranslated(splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -headerSize.height));
			addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
			addPoint(bottomLeft.getCopy());
			addPoint(bottomLeft.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -headerSize.height));
			addPoint(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2, headerSize.height));
			
			// fHeader
			fHeader.setLocation(splitjoinTopCenter.getTranslated(-splitjoinSize.width/2 + IStreamItGraphConstants.MARGIN/2, headerSize.height));

		}
		
		return currentHeight;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth) {
		Dimension inputSize = fInputChannel.getSize();
		Dimension splitjoinSize = getSize();
		Dimension outputSize = fOutputChannel.getSize();
		
		// expand channels
		inputSize.height = (stretchHeight - splitjoinSize.height)/2;
		fInputChannel.setSize(inputSize);
		outputSize.height = (stretchHeight - splitjoinSize.height)/2;
		fOutputChannel.setSize(outputSize);
		
		setVerticalLocation(new Point(currentWidth + splitjoinSize.width/2, currentHeight), 0);
		currentWidth = currentWidth +  splitjoinSize.width + IStreamItGraphConstants.MARGIN/2;
		return currentWidth;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getInputLabelTopLeft()
	 */
	public Point getInputChannelTopLeft() {
		return fInputChannel.getLocation();
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getInputLabelTopRight()
	 */
	public Point getInputChannelTopRight() {
		return fInputChannel.getBounds().getTopRight().getTranslated(-1, 0);
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomLeft()
	 */
	public Point getOutputChannelBottomLeft() {
		return fOutputChannel.getBounds().getBottomLeft();
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomRight()
	 */
	public Point getOutputChannelBottomRight() {
		return fOutputChannel.getBounds().getBottomRight().getTranslated(-1, 0);
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
	
}
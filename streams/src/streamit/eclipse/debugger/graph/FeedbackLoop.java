package streamit.eclipse.debugger.graph;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.Font;

/**
 * @author kkuo
 */
public class FeedbackLoop extends Ellipse implements IStream {

	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private boolean fExpanded;
	private Label fInputChannel;
	private Label fOutputChannel;

	/**
	 * 
	 */
	public FeedbackLoop(IValue feedbackloopVal, String name, Font parentFont, String streamNameWithId, Expanded allExpanded, Figure parent, boolean verticalLayout, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
		
		// create feedbackloop
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = feedbackloopVal.getValueString();
		String feedbackloopName = getNameWithId();
		fExpanded = allExpanded.contains(feedbackloopName, false);
		fHeader = new Label(feedbackloopName);
		Dimension feedbackloopSize = FigureUtilities.getTextExtents(feedbackloopName, parentFont);

		// feedbackloop style
		setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);
		
		// possible highlight
		if (streamNameWithId.equals(feedbackloopName)) StreamSelector.setSelection(this);

		if (!fExpanded) {

			// collapsed content
			fHeader.setIcon(factoryInst.getPlus());
			feedbackloopSize.expand(fHeader.getIconTextGap() + factoryInst.getPlus().getBounds().width, 0);
			StreamItViewFactory.getInstance().roundUpEven(feedbackloopSize);
			fHeader.setSize(feedbackloopSize);
			add(fHeader);
			
			// collapsed size
			feedbackloopSize.expand(IStreamItGraphConstants.MARGIN*4, feedbackloopSize.height + IStreamItGraphConstants.MARGIN);
			
			// highlight
			StreamSelector.setSelection(this);
		} else {
			// TODO expanded feedbackloop
			
			
			
			
		}

		setSize(feedbackloopSize);

		// create channels
		IVariable[] feedbackloopVars = feedbackloopVal.getVariables();
		fInputChannel = new Channel(factoryInst.findVariables(feedbackloopVars, "input"), parentFont, true, factoryInst);
		fOutputChannel = new Channel(factoryInst.findVariables(feedbackloopVars, "output"), parentFont, false, factoryInst);

		// collapsed content
		parent.add(fInputChannel);
		parent.add(this);
		parent.add(fOutputChannel);

		// parent size
		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fInputChannel.getSize().height + feedbackloopSize.height + fOutputChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, feedbackloopSize.width);

		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fInputChannel.getSize().height + feedbackloopSize.height + fOutputChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(feedbackloopSize.width, fInputChannel.getSize().width);
		}
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setLocation(org.eclipse.draw2d.geometry.Point, int)
	 */
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension inputSize = fInputChannel.getSize();
		Dimension feedbackloopSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension outputSize = fOutputChannel.getSize();

		// collapsed location
		if (!isExpanded()) {
			
			fInputChannel.setLocation(parentTopCenter.getTranslated(-inputSize.width/2, currentHeight));
			currentHeight += inputSize.height;
			
			setLocation(parentTopCenter.getTranslated(-feedbackloopSize.width/2, currentHeight));
			fHeader.setLocation(parentTopCenter.getTranslated(-headerSize.width/2, currentHeight + headerSize.height/2 + IStreamItGraphConstants.MARGIN/2));
			currentHeight += feedbackloopSize.height;

			Point bottomLeft = parentTopCenter.getTranslated(-outputSize.width/2, currentHeight);
			fOutputChannel.setLocation(bottomLeft);
			currentHeight += outputSize.height;

		} else {
			// TODO expanded feedbackloop
		}
		
		return currentHeight;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth) {
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
	
}
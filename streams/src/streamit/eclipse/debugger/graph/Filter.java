package streamit.eclipse.debugger.graph;

import java.util.StringTokenizer;

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

/**
 * @author kkuo
 */
public class Filter extends Polygon implements IStream {

	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private ImageFigure fArrow;
	
	private boolean fExpanded;
	private Channel fTopChannel;
	private Channel fBottomChannel;

	private Label fInfo;
	private String fStaticText;
	private String fDynamicText;
	private String fNumWork;

	public Filter(IValue filterVal, String name, String streamNameWithId, Expanded allExpanded, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();

		// create filter
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = filterVal.getValueString();
		String filterName = getNameWithId();
		fExpanded = allExpanded.containsStream(filterName, true);

		// filter style
		setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		// possible highlight
		if (streamNameWithId.equals(filterName)) StreamSelector.setSelection(this);

		// content header
		fHeader = new Label(filterName);
		Dimension filterSize = FigureUtilities.getTextExtents(filterName, factoryInst.getFont());
		
		// create channels
		IVariable[] filterVars = filterVal.getVariables();
		IVariable[] inputVars = factoryInst.findVariables(filterVars, "input");
		IVariable[] outputVars = factoryInst.findVariables(filterVars, "output");
		if (forward) {
			fTopChannel = new Channel(inputVars, filterName + '1', parent, true, forward, lastChild, allExpanded, factoryInst);
			fBottomChannel = new Channel(outputVars, filterName + '0', parent, false, forward, lastChild, allExpanded, factoryInst);
		} else {
			fBottomChannel = new Channel(inputVars, filterName + '1', parent, true, forward, lastChild, allExpanded, factoryInst);
			fTopChannel = new Channel(outputVars, filterName + '0', parent, false, forward, lastChild, allExpanded, factoryInst);
		}

		if (!fExpanded) {
			// content header
			fHeader.setIcon(factoryInst.getPlus());
			filterSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(filterSize);
			fHeader.setSize(filterSize);
			add(fHeader);
		} else {
			// content header
			fHeader.setIcon(factoryInst.getMinus());
			filterSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(filterSize);
			fHeader.setSize(filterSize);
			add(fHeader);
			
			// expanded content info
			setText(filterVars, inputVars, outputVars, factoryInst);
			fInfo = new Label(fDynamicText + fStaticText);
			Dimension d = FigureUtilities.getTextExtents(fStaticText + fDynamicText, factoryInst.getFont());
			factoryInst.roundUpEven(d);
			fInfo.setSize(d);
			add(fInfo);	
			filterSize.height += d.height;
			filterSize.width = Math.max(filterSize.width, d.width);
		}
		
		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(forward));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);
		
		// size
		filterSize.expand(IStreamItGraphConstants.MARGIN, factoryInst.getArrowHeight() + IStreamItGraphConstants.MARGIN);
		setSize(filterSize);
		
		// parent content
		parent.add(this);

		// parent size
		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fTopChannel.getSize().height + filterSize.height;
			if (lastChild) parentSize.height += fBottomChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, filterSize.width);
		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fTopChannel.getSize().height + filterSize.height + fBottomChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(filterSize.width, fTopChannel.getSize().width);
		}
	}

	private void setText(IVariable[] filterVars, IVariable[] inputVars, IVariable[] outputVars, StreamItViewFactory factoryInst) throws DebugException {
		String inputType = "void";
		String outputType = "void";
		String popCount = "N/A";
		String peekCount = "N/A";
		String pushCount = "N/A";
		
		fNumWork = "0";
		int numWork = 0;

		if (inputVars.length != 0) {
			StringTokenizer st = new StringTokenizer(factoryInst.getValueString(inputVars, "type"), "()");
			if (st.countTokens() > 0) inputType = st.nextToken();
			
			peekCount = factoryInst.getValueString(factoryInst.findVariables(inputVars, "peekCount"), "value");
			popCount = factoryInst.getValueString(factoryInst.findVariables(inputVars, "popPushCount"), "value");

			int divisor = Integer.valueOf(popCount).intValue();
			int totalItemsPopped = Integer.valueOf(factoryInst.getValueString(inputVars, "totalItemsPopped")).intValue();
			numWork = totalItemsPopped/divisor;
			fNumWork = String.valueOf(numWork);
		}
		
		if (outputVars.length != 0) {
			StringTokenizer st = new StringTokenizer(factoryInst.getValueString(outputVars, "type"), "()");
			if (st.countTokens() > 0) outputType = st.nextToken();
			
			pushCount = factoryInst.getValueString(factoryInst.findVariables(outputVars, "popPushCount"), "value");
			
			int divisor = Integer.valueOf(pushCount).intValue();
			int totalItemsPushed = Integer.valueOf(factoryInst.getValueString(outputVars, "totalItemsPushed")).intValue();
			numWork = totalItemsPushed/divisor;
			fNumWork = String.valueOf(numWork);
		}

		fStaticText = IStreamItGraphConstants.POP_COUNT_LABEL + popCount 
					+ IStreamItGraphConstants.PEEK_COUNT_LABEL + peekCount
					+ IStreamItGraphConstants.PUSH_COUNT_LABEL + pushCount
					+ IStreamItGraphConstants.INPUT_TYPE_LABEL + inputType 
					+ IStreamItGraphConstants.OUTPUT_TYPE_LABEL + outputType
					+ IStreamItGraphConstants.STRUCTURE_TYPE_LABEL + IStreamItGraphConstants.FILTER_STRUCTURE;

		fDynamicText = IStreamItGraphConstants.WORK_EXECUTIONS_LABEL + fNumWork;
	}
	
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension topSize = fTopChannel.getSize();
		Dimension filterSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension arrowSize = fArrow.getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight))) currentHeight += topSize.height;
		
		Point topLeft = parentTopCenter.getTranslated(-filterSize.width/2, currentHeight);
		setLocation(topLeft);
		fHeader.setLocation(parentTopCenter.getTranslated(-headerSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2));
		
		if (!isExpanded()) {
			// collapsed location
			fArrow.setLocation(parentTopCenter.getTranslated(-arrowSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height));

			currentHeight += filterSize.height;
			
			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft)) currentHeight += bottomSize.height;

			// filter points
			addPoint(topLeft.getCopy());
			addPoint(topLeft.getTranslated(filterSize.width, 0));
			int h = arrowSize.height;
			addPoint(topLeft.getTranslated(filterSize.width, h));
			addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
			addPoint(bottomLeft.getCopy());
			addPoint(topLeft.getTranslated(0, h));

		} else {
			// expanded location
			Dimension infoSize = fInfo.getSize();

			fInfo.setLocation(parentTopCenter.getTranslated(-infoSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height));
			fArrow.setLocation(parentTopCenter.getTranslated(-arrowSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height + infoSize.height));
			currentHeight += filterSize.height;

			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft)) currentHeight += bottomSize.height;

			// filter points
			addPoint(topLeft.getCopy());
			addPoint(topLeft.getTranslated(filterSize.width, 0));
			int h = headerSize.height + infoSize.height;
			addPoint(topLeft.getTranslated(filterSize.width, h));
			addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
			addPoint(bottomLeft.getCopy());
			addPoint(topLeft.getTranslated(0, h));
		}

		return currentHeight;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth) {
		Dimension topSize = fTopChannel.getSize();
		Dimension filterSize = getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// expand channels
		topSize.height = (stretchHeight - filterSize.height)/2;
		fTopChannel.setSize(topSize);
		bottomSize.height = (stretchHeight - filterSize.height)/2;
		fBottomChannel.setSize(bottomSize);
		
		setVerticalLocation(new Point(currentWidth + filterSize.width/2, currentHeight), 0);
		currentWidth = currentWidth +  filterSize.width + IStreamItGraphConstants.MARGIN/2;
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
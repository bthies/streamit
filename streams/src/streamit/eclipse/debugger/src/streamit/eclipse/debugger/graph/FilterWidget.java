package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.Polygon;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

import streamit.eclipse.debugger.model.Filter;

/**
 * @author kkuo
 */
public class FilterWidget extends Polygon implements IStreamStructureWidget {

	private Filter fModel;
	private boolean fExpanded;
	private boolean fForward;

	// widgets
	private Label fHeader, fInfo;
	private ImageFigure fArrow;
	private ImageFigure fInstanceBreakpoint;
	private ChannelWidget fTopChannel, fBottomChannel;

	public FilterWidget(Filter model, String streamNameWithId, OptionData optionData, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
		fModel = model;
		fForward = forward;

		// create filter
		new StreamStructureSelector(this);
		String filterName;
		filterName = getNameWithRuntimeId();
		fExpanded = optionData.containsStream(filterName, true);

		// filter style
		setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		// possible highlight
		if (streamNameWithId.equals(filterName)) StreamStructureSelector.setSelection(this);

		// content header
		fHeader = new Label(filterName);
		Dimension filterSize = FigureUtilities.getTextExtents(filterName, factoryInst.getFont());
		
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
			String text = getText();
			fInfo = new Label(text);
			Dimension d = FigureUtilities.getTextExtents(text, factoryInst.getFont());
			factoryInst.roundUpEven(d);
			fInfo.setSize(d);
			add(fInfo);	
			filterSize.height += d.height;
			filterSize.width = Math.max(filterSize.width, d.width);
		}
		
		if (forward) {
			fTopChannel = new ChannelWidget(fModel.getInputChannel(), filterName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fBottomChannel = new ChannelWidget(fModel.getOutputChannel(), filterName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);

			fTopChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));
		} else {
			fBottomChannel = new ChannelWidget(fModel.getInputChannel(), filterName + IStreamItGraphConstants.ONE_CHAR, parent, forward, lastChild, optionData, factoryInst);
			fTopChannel = new ChannelWidget(fModel.getOutputChannel(), filterName + IStreamItGraphConstants.ZERO_CHAR, parent, forward, lastChild, optionData, factoryInst);

			fBottomChannel.grayData(Integer.parseInt(fModel.getMaxPeeked()) - Integer.parseInt(fModel.getPopped()));
		}
		((IStreamStructureWidget) parent).setChannelExpanded(fTopChannel.isExpanded(), fBottomChannel.isExpanded());
		
		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(forward));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);
		
		// breakpoint instance
		if (optionData.hasFilterInstanceBreakpoint(fModel.getNameWithoutId(), fModel.getRuntimeId(), fModel.getStaticId())) {
			fInstanceBreakpoint = new ImageFigure(factoryInst.getBreakpoint());
			fInstanceBreakpoint.setSize(factoryInst.getBreakpointWidth(), factoryInst.getBreakpointHeight());
			add(fInstanceBreakpoint);
		} else fInstanceBreakpoint = null;

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

	private String getText() {
		return IStreamItGraphConstants.INIT_EXECUTION_COUNT_LABEL + fModel.getInitExecutionCount()
					+ IStreamItGraphConstants.STEADY_EXECUTION_COUNT_LABEL + fModel.getSteadyExecutionCount()
					+ IStreamItGraphConstants.POPPED_LABEL + fModel.getPopped()
					+ IStreamItGraphConstants.PEEKED_LABEL + fModel.getMaxPeeked()
					+ IStreamItGraphConstants.PUSHED_LABEL + fModel.getPushed()
					+ IStreamItGraphConstants.POP_RATE_LABEL + fModel.getPopRate() 
					+ IStreamItGraphConstants.PEEK_RATE_LABEL + fModel.getPeekRate()
					+ IStreamItGraphConstants.PUSH_RATE_LABEL + fModel.getPushRate()
					+ IStreamItGraphConstants.INPUT_TYPE_LABEL + fModel.getInputType() 
					+ IStreamItGraphConstants.OUTPUT_TYPE_LABEL + fModel.getOutputType()
					+ IStreamItGraphConstants.STRUCTURE_TYPE_LABEL + IStreamItGraphConstants.FILTER_STRUCTURE
					+ IStreamItGraphConstants.WORK_EXECUTIONS_LABEL + fModel.getWorkExecutions();
	}
	
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension topSize = fTopChannel.getSize();
		Dimension filterSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension arrowSize = fArrow.getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight), true)) currentHeight += topSize.height;
		
		Point topLeft = parentTopCenter.getTranslated(-filterSize.width/2, currentHeight);
		setLocation(topLeft);
		if (fForward)
			fHeader.setLocation(parentTopCenter.getTranslated(-headerSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2));
		else
			fHeader.setLocation(parentTopCenter.getTranslated(-headerSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN + headerSize.height));
		
		if (!isExpanded()) {
			// collapsed location
			Point arrowPoint;
			if (fForward) {
				arrowPoint = parentTopCenter.getTranslated(-arrowSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height);
			} else {
				arrowPoint = parentTopCenter.getTranslated(-arrowSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2);
			}
			fArrow.setLocation(arrowPoint);
			if (fInstanceBreakpoint !=  null) fInstanceBreakpoint.setLocation(arrowPoint.getTranslated(-fInstanceBreakpoint.getSize().width + IStreamItGraphConstants.MARGIN, 0));
			
			currentHeight += filterSize.height;
			
			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft, false)) currentHeight += bottomSize.height;

			// filter points
			if (fForward) {
				addPoint(topLeft.getCopy());
				addPoint(topLeft.getTranslated(filterSize.width, 0));
				int h = arrowSize.height;
				addPoint(topLeft.getTranslated(filterSize.width, h));
				addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
				addPoint(bottomLeft.getCopy());
				addPoint(topLeft.getTranslated(0, h));
			} else {				
				addPoint(topLeft.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2 + filterSize.width/2, 0));
				addPoint(topLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 + filterSize.width/2, 0));
				int h = arrowSize.height;
				addPoint(bottomLeft.getTranslated(filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -h));
				addPoint(bottomLeft.getTranslated(filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
				addPoint(bottomLeft.getTranslated(-filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
				addPoint(bottomLeft.getTranslated(-filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -h));
			}			
		} else {
			// expanded location
			Dimension infoSize = fInfo.getSize();
			Point arrowPoint;
			if (fForward) {
				fInfo.setLocation(parentTopCenter.getTranslated(-infoSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height));
				arrowPoint = parentTopCenter.getTranslated(-arrowSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height + infoSize.height);
			} else {
				fInfo.setLocation(parentTopCenter.getTranslated(-infoSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height*2));
				arrowPoint = parentTopCenter.getTranslated(-arrowSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2);
			}
			
			fArrow.setLocation(arrowPoint);
			if (fInstanceBreakpoint !=  null) fInstanceBreakpoint.setLocation(arrowPoint.getTranslated(-fInstanceBreakpoint.getSize().width + IStreamItGraphConstants.MARGIN, 0));
			
			currentHeight += filterSize.height;

			Point bottomLeft = parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight);
			if (fBottomChannel.setSelf(bottomLeft, false)) currentHeight += bottomSize.height;

			// filter points
			if (fForward) {
				addPoint(topLeft.getCopy());
				addPoint(topLeft.getTranslated(filterSize.width, 0));
				int h = headerSize.height + infoSize.height;
				addPoint(topLeft.getTranslated(filterSize.width, h));
				addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
				addPoint(bottomLeft.getCopy());
				addPoint(topLeft.getTranslated(0, h));
			} else {
				addPoint(topLeft.getTranslated(-IStreamItGraphConstants.CHANNEL_WIDTH/2 + filterSize.width/2, 0));
				addPoint(topLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH/2 + filterSize.width/2, 0));
				int h = headerSize.height + infoSize.height;
				addPoint(bottomLeft.getTranslated(filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -h));
				addPoint(bottomLeft.getTranslated(filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
				addPoint(bottomLeft.getTranslated(-filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, 0));
				addPoint(bottomLeft.getTranslated(-filterSize.width/2 + IStreamItGraphConstants.CHANNEL_WIDTH/2, -h));
			}
		}

		return currentHeight;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, boolean stretchTopChannel, boolean stretchBottomChannel, int stretchHeight, int currentWidth) {
		Dimension topSize = fTopChannel.getSize();
		Dimension filterSize = getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// expand channels
		if (stretchTopChannel) {
			topSize.height = stretchHeight - filterSize.height - bottomSize.height;
			fTopChannel.setSize(topSize);
		} 
		if (stretchBottomChannel) {
			bottomSize.height = stretchHeight - filterSize.height - topSize.height;
			fBottomChannel.setSize(bottomSize);
		}
		
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
		return null;
	}
}
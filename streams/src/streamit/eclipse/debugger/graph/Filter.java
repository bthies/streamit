package streamit.eclipse.debugger.graph;

import java.util.StringTokenizer;

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
import org.eclipse.swt.graphics.Font;

/**
 * @author kkuo
 */
public class Filter extends Polygon implements IStream {

	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	
	private boolean fExpanded;
	private Label fInputChannel;
	private Label fOutputChannel;

	private Label fInfo;
	private String fStaticText;
	private String fDynamicText;
	private String fStage;
	private String fNumWork;

	public Filter(IValue filterVal, String name, Font parentFont, String streamNameWithId, Expanded allExpanded, Figure parent, boolean verticalLayout, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();

		// create filter
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = filterVal.getValueString();
		String filterName = getNameWithId();
		fExpanded = allExpanded.contains(filterName, true);

		// filter style
		setOutline(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);		

		// possible highlight
		if (streamNameWithId.equals(filterName)) StreamSelector.setSelection(this);

		// content header
		fHeader = new Label(filterName);
		Dimension filterSize = FigureUtilities.getTextExtents(filterName, parentFont);
		
		// create channels
		IVariable[] filterVars = filterVal.getVariables();
		IVariable[] inputVars = factoryInst.findVariables(filterVars, "input");
		IVariable[] outputVars = factoryInst.findVariables(filterVars, "output");
		fInputChannel = new Channel(inputVars, parentFont, true, factoryInst);
		fOutputChannel = new Channel(outputVars, parentFont, false, factoryInst);

		if (!fExpanded) {
			// content header
			fHeader.setIcon(factoryInst.getPlus());
			filterSize.expand(fHeader.getIconTextGap() + factoryInst.getPlus().getBounds().width, 0);
			factoryInst.roundUpEven(filterSize);
			fHeader.setSize(filterSize);
			add(fHeader);

			// collapsed size
			filterSize.expand(IStreamItGraphConstants.MARGIN, filterSize.height + IStreamItGraphConstants.MARGIN);

		} else {
			
			// content header
			fHeader.setIcon(factoryInst.getMinus());
			filterSize.expand(fHeader.getIconTextGap() + factoryInst.getMinus().getBounds().width, 0);
			factoryInst.roundUpEven(filterSize);
			fHeader.setSize(filterSize);
			add(fHeader);

			// expanded content info
			fStage = IStreamItGraphConstants.INIT_STAGE;
			fNumWork = "0";
			fDynamicText = IStreamItGraphConstants.WORK_EXECUTIONS_LABEL + fNumWork + IStreamItGraphConstants.CURRENT_STAGE_LABEL + fStage;
			setStaticText(inputVars, outputVars, factoryInst);
			fInfo = new Label(fDynamicText + fStaticText);
			Dimension d = FigureUtilities.getTextExtents(fStaticText + fDynamicText, parentFont);
			factoryInst.roundUpEven(d);
			fInfo.setSize(d);
			add(fInfo);	
			filterSize.height += d.height;
			filterSize.width = Math.max(filterSize.width, d.width);
			
			// expanded size
			filterSize.expand(IStreamItGraphConstants.MARGIN, fHeader.getSize().height + IStreamItGraphConstants.MARGIN);		
		}
		
		// size
		setSize(filterSize);
		
		// parent content
		parent.add(fInputChannel);
		parent.add(this);
		parent.add(fOutputChannel);

		// parent size
		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fInputChannel.getSize().height + filterSize.height + fOutputChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, filterSize.width);
		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fInputChannel.getSize().height + filterSize.height + fOutputChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(filterSize.width, fInputChannel.getSize().width);
		}
	}

	private void setStaticText(IVariable[] inputVars, IVariable[] outputVars, StreamItViewFactory factoryInst) throws DebugException {
		String inputType = "void";
		String outputType = "void";
		String popCount = "N/A";
		String peekCount = "N/A";
		String pushCount = "N/A";
		
		if (inputVars.length != 0) {
			StringTokenizer st = new StringTokenizer(factoryInst.getValueString(inputVars, "type"), "()");
			if (st.countTokens() > 0) inputType = st.nextToken();
			
			peekCount = factoryInst.getValueString(factoryInst.findVariables(inputVars, "peekCount"), "value");
			popCount = factoryInst.getValueString(factoryInst.findVariables(inputVars, "popPushCount"), "value");
		}
		
		if (outputVars.length != 0) {
			StringTokenizer st = new StringTokenizer(factoryInst.getValueString(outputVars, "type"), "()");
			if (st.countTokens() > 0) outputType = st.nextToken();
			
			pushCount = factoryInst.getValueString(factoryInst.findVariables(outputVars, "popPushCount"), "value");
		}
		
		fStaticText = IStreamItGraphConstants.POP_COUNT_LABEL + popCount 
					+ IStreamItGraphConstants.PEEK_COUNT_LABEL + peekCount
					+ IStreamItGraphConstants.PUSH_COUNT_LABEL + pushCount
					+ IStreamItGraphConstants.INPUT_TYPE_LABEL + inputType 
					+ IStreamItGraphConstants.OUTPUT_TYPE_LABEL + outputType
					+ IStreamItGraphConstants.STRUCTURE_TYPE_LABEL + IStreamItGraphConstants.FILTER_STRUCTURE;
	}
	
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension inputSize = fInputChannel.getSize();
		Dimension filterSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension outputSize = fOutputChannel.getSize();
		
		fInputChannel.setLocation(parentTopCenter.getTranslated(-inputSize.width/2, currentHeight));
		currentHeight += inputSize.height;
		
		Point topLeft = parentTopCenter.getTranslated(-filterSize.width/2, currentHeight);
		setLocation(topLeft);
		fHeader.setLocation(parentTopCenter.getTranslated(-headerSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2));
		
		// collapsed location
		if (!isExpanded()) {		
			currentHeight += filterSize.height;

			Point bottomLeft = parentTopCenter.getTranslated(-outputSize.width/2, currentHeight);
			fOutputChannel.setLocation(bottomLeft);
			currentHeight += outputSize.height;

			// filter points
			addPoint(topLeft.getCopy());
			addPoint(topLeft.getTranslated(filterSize.width, 0));
			int h = headerSize.height;
			addPoint(topLeft.getTranslated(filterSize.width, h));
			addPoint(bottomLeft.getTranslated(IStreamItGraphConstants.CHANNEL_WIDTH, 0));
			addPoint(bottomLeft.getCopy());
			addPoint(topLeft.getTranslated(0, h));

		} else {
			// expanded location
			Dimension infoSize = fInfo.getSize();

			fInfo.setLocation(parentTopCenter.getTranslated(-infoSize.width/2, currentHeight + IStreamItGraphConstants.MARGIN/2 + headerSize.height));
			currentHeight += filterSize.height;

			Point bottomLeft = parentTopCenter.getTranslated(-outputSize.width/2, currentHeight);
			fOutputChannel.setLocation(bottomLeft);
			currentHeight += outputSize.height;

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
		Dimension inputSize = fInputChannel.getSize();
		Dimension filterSize = getSize();
		Dimension outputSize = fOutputChannel.getSize();

		// expand channels
		inputSize.height = (stretchHeight - filterSize.height)/2;
		fInputChannel.setSize(inputSize);
		outputSize.height = (stretchHeight - filterSize.height)/2;
		fOutputChannel.setSize(outputSize);

		setVerticalLocation(new Point(currentWidth + filterSize.width/2, currentHeight), 0);
		currentWidth = currentWidth +  filterSize.width + IStreamItGraphConstants.MARGIN/2;
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
package streamit.eclipse.debugger.graph;

import java.util.Collections;
import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaValue;

/**
 * @author kkuo
 */
public class Pipeline extends RectangleFigure implements IStream {
	
	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private ImageFigure fArrow;
	
	private boolean fExpanded;
	private Channel fTopChannel;
	private Channel fBottomChannel;
	private Vector fChildren;

	public Pipeline(IValue pipelineVal, String name, String streamNameWithId, OptionData optionData, Figure parent, boolean forward, boolean verticalLayout, boolean lastChild, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
		
		// create pipeline
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = pipelineVal.getValueString();
		String pipelineName = getNameWithId();
		fExpanded = optionData.containsStream(pipelineName, false);
		Dimension pipelineSize = FigureUtilities.getTextExtents(pipelineName, factoryInst.getFont());

		// pipeline style
		if (optionData.isHideLines() && fExpanded) setOutline(false);
		else setBorder(new LineBorder());
		setOpaque(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		// possible highlight
		if (streamNameWithId.equals(pipelineName)) StreamSelector.setSelection(this);

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
			IVariable[] vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(pipelineVal.getVariables(), "streamElements"), "header"), "next");
			Vector elements = factoryInst.getLinkedListElements(vars);
			int last;
			if (forward) {
				last = elements.size() - 1;
			} else {
				last = 0;
				Collections.reverse(elements);
			}
			IJavaValue val;
			IJavaClassType type;
			String streamType, streamName;
			Dimension childrenSize = new Dimension(0, 0); // (width of widest child, total height of children)
			fChildren = new Vector();
			for (int i = 0; i < elements.size(); i++) {
				val = (IJavaValue) ((IVariable) elements.get(i)).getValue();
				type = (IJavaClassType) val.getJavaType();
				streamName = type.getName();
				streamType = type.getSuperclass().getName();
				if (streamType.equals("streamit.library.Filter")) {
					fChildren.add(new Filter(val, streamName, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.Pipeline")) {
					fChildren.add(new Pipeline(val, streamName, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.SplitJoin")) {
					fChildren.add(new SplitJoin(val, streamName, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));	
				} else if (streamType.equals("streamit.library.FeedbackLoop")) {
					fChildren.add(new FeedbackLoop(val, streamName, streamNameWithId, optionData, this, forward, true, i == last, childrenSize, factoryInst));				
				}
			}
		
			// expanded size
			if (fChildren.size() == 0) {
				pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*3);
				setOutline(true);
				setBorder(new LineBorder());
			} else {
				pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*3 + ((IStream) fChildren.get(0)).getTopChannelToggleWidth()*2);
			}
			pipelineSize.height = Math.max(childrenSize.height, pipelineSize.height + factoryInst.getArrowHeight());
		}
		setSize(pipelineSize);

		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(forward));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);

		// create channels
		IVariable[] pipelineVars = pipelineVal.getVariables();
		if (forward) {
			fTopChannel = new Channel(factoryInst.findVariables(pipelineVars, "input"), pipelineName + '1', parent, true, forward, lastChild, optionData, factoryInst);
			fBottomChannel = new Channel (factoryInst.findVariables(pipelineVars, "output"), pipelineName + '0', parent, false, forward, lastChild, optionData, factoryInst);
		} else {
			fBottomChannel = new Channel(factoryInst.findVariables(pipelineVars, "input"), pipelineName + '1', parent, true, forward, lastChild, optionData, factoryInst);
			fTopChannel = new Channel (factoryInst.findVariables(pipelineVars, "output"), pipelineName + '0', parent, false, forward, lastChild, optionData, factoryInst);
		}
		fTopChannel.turnOff(fExpanded);
		fBottomChannel.turnOff(fExpanded);
		
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

		if (fTopChannel.setSelf(parentTopCenter.getTranslated(-topSize.width/2, currentHeight))) currentHeight += topSize.height;

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
				currentHeight = ((IStream) fChildren.get(i)).setVerticalLocation(parentTopCenter, currentHeight);
		}

		if (fBottomChannel.setSelf(parentTopCenter.getTranslated(-bottomSize.width/2, currentHeight))) currentHeight += bottomSize.height;

		return currentHeight;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth) {
		Dimension topSize = fTopChannel.getSize();
		Dimension pipelineSize = getSize();
		Dimension bottomSize = fBottomChannel.getSize();

		// expand channels
		topSize.height = (stretchHeight - pipelineSize.height)/2;
		fTopChannel.setSize(topSize);
		bottomSize.height = (stretchHeight - pipelineSize.height)/2;
		fBottomChannel.setSize(bottomSize);
		
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
package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.swt.graphics.Font;

/**
 * @author kkuo
 */
public class Pipeline extends Label implements IStream {
	
	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private boolean fExpanded;
	private Label fInputChannel;
	private Label fOutputChannel;
	private Vector fChildren;

	public Pipeline(IValue pipelineVal, String name, Font parentFont, String streamNameWithId, Expanded allExpanded, Figure parent, boolean verticalLayout, Dimension parentSize, StreamItViewFactory factoryInst) throws DebugException {
		super();
		
		// create pipeline
		new StreamSelector(this);
		fNameWithoutId = name;
		fId = pipelineVal.getValueString();
		String pipelineName = getNameWithId();
		fExpanded = allExpanded.contains(pipelineName, false);
		Dimension pipelineSize = FigureUtilities.getTextExtents(pipelineName, parentFont);

		// pipeline style
		setBorder(new LineBorder());
		setOpaque(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);		

		// possible highlight
		if (streamNameWithId.equals(pipelineName)) StreamSelector.setSelection(this);

		if (!fExpanded) {
			
			// collapsed content
			setIcon(factoryInst.getPlus());
			setText(pipelineName);
			
			// collapsed size
			pipelineSize.expand(getIconTextGap() + factoryInst.getPlus().getBounds().width + IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN);
			factoryInst.roundUpEven(pipelineSize);
			
		} else {
			
			// expanded content
			fHeader = new Label(pipelineName, factoryInst.getMinus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getMinus().getBounds().width + IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);

			// expanded children
			IVariable[] vars = factoryInst.findVariables(factoryInst.findVariables(factoryInst.findVariables(pipelineVal.getVariables(), "streamElements"), "header"), "next");
			Vector elements = factoryInst.getLinkedListElements(vars);
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
					fChildren.add(new Filter(val, streamName, parentFont, streamNameWithId, allExpanded, this, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.Pipeline")) {
					fChildren.add(new Pipeline(val, streamName, parentFont, streamNameWithId, allExpanded, this, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.SplitJoin")) {
					fChildren.add(new SplitJoin(val, streamName, parentFont, streamNameWithId, allExpanded, this, true, childrenSize, factoryInst));	
				} else if (streamType.equals("streamit.library.FeedbackLoop")) {
					fChildren.add(new FeedbackLoop(val, streamName, parentFont, streamNameWithId, allExpanded, this, true, childrenSize, factoryInst));				
				}
			}
		
			// expanded size
			pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*2);
			pipelineSize.height = Math.max(childrenSize.height, pipelineSize.height);
		}

		setSize(pipelineSize);
			
		// create channels
		IVariable[] pipelineVars = pipelineVal.getVariables();
		fInputChannel = new Channel(factoryInst.findVariables(pipelineVars, "input"), parentFont, true, factoryInst);
		fOutputChannel = new Channel (factoryInst.findVariables(pipelineVars, "output"), parentFont, false, factoryInst);

		// parent content
		parent.add(fInputChannel);
		parent.add(this);
		parent.add(fOutputChannel);

		// parent size
		if (verticalLayout) {
			// (total height of children, width of widest child)
			parentSize.height = parentSize.height + fInputChannel.getSize().height + pipelineSize.height + fOutputChannel.getSize().height;
			parentSize.width = Math.max(parentSize.width, pipelineSize.width);
		} else {
			// (height of tallest child, total width of children)
			parentSize.height = Math.max(parentSize.height, fInputChannel.getSize().height + pipelineSize.height + fOutputChannel.getSize().height);
			parentSize.width = parentSize.width + Math.max(pipelineSize.width, fInputChannel.getSize().width);
		}
	}

	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension inputSize = fInputChannel.getSize();
		Dimension pipelineSize = getSize();
		Dimension outputSize = fOutputChannel.getSize();

		fInputChannel.setLocation(parentTopCenter.getTranslated(-inputSize.width/2, currentHeight));
		currentHeight += inputSize.height;

		setLocation(parentTopCenter.getTranslated(-pipelineSize.width/2, currentHeight));
						
		if (!isExpanded()) {
			// collapsed location
			currentHeight += pipelineSize.height;
			
		} else {
			// expanded location
			Dimension headerSize = fHeader.getSize();
			fHeader.setLocation(parentTopCenter.getTranslated(-pipelineSize.width/2, currentHeight));		

			for (int i = 0; i < fChildren.size(); i++)
				currentHeight = ((IStream) fChildren.get(i)).setVerticalLocation(parentTopCenter, currentHeight);
		}

		fOutputChannel.setLocation(parentTopCenter.getTranslated(-outputSize.width/2, currentHeight));
		currentHeight += outputSize.height;		

		return currentHeight;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int)
	 */
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth) {
		Dimension inputSize = fInputChannel.getSize();
		Dimension pipelineSize = getSize();
		Dimension outputSize = fOutputChannel.getSize();
		
		// expand channels
		inputSize.height = (stretchHeight - pipelineSize.height)/2;
		fInputChannel.setSize(inputSize);
		outputSize.height = (stretchHeight - pipelineSize.height)/2;
		fOutputChannel.setSize(outputSize);
		
		setVerticalLocation(new Point(currentWidth + pipelineSize.width/2, currentHeight), 0);
		currentWidth = currentWidth +  pipelineSize.width + IStreamItGraphConstants.MARGIN/2;
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
		if (!isExpanded()) return getIconBounds().contains(p);
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

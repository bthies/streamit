package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
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
public class MainPipeline extends Label implements IStream {

	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private boolean fExpanded;
	private Label fInputChannel;
	private Label fOutputChannel;
	private Vector fChildren;
	private Expanded fAllExpanded;

	// top level pipeline
	public MainPipeline(IVariable pipelineVar, Font parentFont, String streamNameWithId, Expanded allExpanded, StreamItViewFactory factoryInst) throws DebugException {
		super();
		
		// create pipeline
		new StreamSelector(this);
		IValue pipelineVal = pipelineVar.getValue();
		fNameWithoutId = pipelineVar.getReferenceTypeName();
		fId = pipelineVal.getValueString();
		fInputChannel = null;
		fOutputChannel = null;
		fAllExpanded = allExpanded;
		String pipelineName = getNameWithId();
		fExpanded = fAllExpanded.contains(pipelineName, false);
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
			setSize(pipelineSize);
			
		} else {
			
			// expanded content
			fHeader = new Label(pipelineName, factoryInst.getMinus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getMinus().getBounds().width + IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);
			fHeader.setLocation(getBounds().getTopLeft());		

			// expanded children
			IVariable[] vars = factoryInst.findVariables(StreamItViewFactory.getInstance().findVariables(StreamItViewFactory.getInstance().findVariables(pipelineVal.getVariables(), "streamElements"), "header"), "next");
			Vector elements = factoryInst.getLinkedListElements(vars);
			IJavaValue val;
			IJavaClassType type;
			String streamType, streamName;
			Dimension childrenSize = new Dimension(0, 0); // (total height of children, width of widest child)
			fChildren = new Vector();
			for (int i = 0; i < elements.size(); i++) {
				val = (IJavaValue) ((IVariable) elements.get(i)).getValue();
				type = (IJavaClassType) val.getJavaType();
				streamName = type.getName();
				streamType = type.getSuperclass().getName();
				if (streamType.equals("streamit.library.Filter")) {
					fChildren.add(new Filter(val, streamName, parentFont, streamNameWithId, fAllExpanded, this, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.Pipeline")) {
					fChildren.add(new Pipeline(val, streamName, parentFont, streamNameWithId, fAllExpanded, this, true, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.SplitJoin")) {
					fChildren.add(new SplitJoin(val, streamName, parentFont, streamNameWithId, fAllExpanded, this, true, childrenSize, factoryInst));	
				} else if (streamType.equals("streamit.library.FeedbackLoop")) {
					fChildren.add(new FeedbackLoop(val, streamName, parentFont, streamNameWithId, fAllExpanded, this, true, childrenSize, factoryInst));				
				}
			}
		
			// expanded size
			pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*2);
			pipelineSize.height = Math.max(childrenSize.height, pipelineSize.height);
			setSize(pipelineSize);
		
			// fix locations of children (top to bottom)
			int currentHeight = 0;
			Point top = getBounds().getTop();
			for (int i = 0; i < fChildren.size(); i++)
				currentHeight = ((IStream) fChildren.get(i)).setVerticalLocation(top, currentHeight);
		}
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setVerticalLocation(org.eclipse.draw2d.geometry.Point, int)
	 */
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		// should not be called
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setHorizontalLocation(org.eclipse.draw2d.geometry.Point, int, int, int)
	 */
	public int setHorizontalLocation(int centerHeight, int stretchHeight, int currentWidth) {
		// should not be called
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getInputLabelTopLeft()
	 */
	public Point getInputChannelTopLeft() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getInputLabelTopRight()
	 */
	public Point getInputChannelTopRight() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomLeft()
	 */
	public Point getOutputChannelBottomLeft() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomRight()
	 */
	public Point getOutputChannelBottomRight() {
		// should not be called
		return null;
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

	public Expanded getAllExpanded(boolean highlighting) {
		fAllExpanded.setHighlighting(highlighting);
		return fAllExpanded;
	}

}

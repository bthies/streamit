package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.draw2d.ColorConstants;
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
public class MainPipeline extends RectangleFigure implements IStream {

	private String fNameWithoutId;
	private String fId;
	private Label fHeader;
	private ImageFigure fArrow;
	
	private boolean fExpanded;
	private Vector fChildren;
	
	private Expanded fAllExpanded;

	// top level pipeline
	public MainPipeline(IVariable pipelineVar, String streamNameWithId, Expanded allExpanded, StreamItViewFactory factoryInst) throws DebugException {
		super();
		
		// create pipeline
		new StreamSelector(this);
		IValue pipelineVal = pipelineVar.getValue();
		fNameWithoutId = pipelineVar.getReferenceTypeName();
		fId = pipelineVal.getValueString();
		fAllExpanded = allExpanded;
		String pipelineName = getNameWithId();
		fExpanded = fAllExpanded.containsStream(pipelineName, false);
		Dimension pipelineSize = FigureUtilities.getTextExtents(pipelineName, factoryInst.getFont());

		// pipeline style
		setBorder(new LineBorder());
		setOpaque(true);
		setForegroundColor(ColorConstants.menuForeground);
		setBackgroundColor(ColorConstants.white);

		// possible highlight
		if (streamNameWithId.equals(pipelineName)) StreamSelector.setSelection(this);

		if (!fExpanded) {

			// header content
			fHeader = new Label(pipelineName, factoryInst.getPlus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);
		
			// collapsed size
			pipelineSize.expand(IStreamItGraphConstants.MARGIN, factoryInst.getArrowHeight() + IStreamItGraphConstants.MARGIN);
			
		} else {

			// header content
			fHeader = new Label(pipelineName, factoryInst.getMinus());
			pipelineSize.expand(fHeader.getIconTextGap() + factoryInst.getImageWidth(), 0);
			factoryInst.roundUpEven(pipelineSize);
			fHeader.setSize(pipelineSize);
			add(fHeader);
		
			// expanded content
			pipelineSize.expand(IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN);

			// expanded children
			IVariable[] vars = factoryInst.findVariables(StreamItViewFactory.getInstance().findVariables(StreamItViewFactory.getInstance().findVariables(pipelineVal.getVariables(), "streamElements"), "header"), "next");
			Vector elements = factoryInst.getLinkedListElements(vars);
			IJavaValue val;
			IJavaClassType type;
			String streamType, streamName;
			Dimension childrenSize = new Dimension(0, 0); // (total height of children, width of widest child)
			fChildren = new Vector();
			int last = elements.size() - 1;
			for (int i = 0; i < last + 1; i++) {
				val = (IJavaValue) ((IVariable) elements.get(i)).getValue();
				type = (IJavaClassType) val.getJavaType();
				streamName = type.getName();
				streamType = type.getSuperclass().getName();
				if (streamType.equals("streamit.library.Filter")) {
					fChildren.add(new Filter(val, streamName, streamNameWithId, fAllExpanded, this, true, true, i == last, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.Pipeline")) {
					fChildren.add(new Pipeline(val, streamName, streamNameWithId, fAllExpanded, this, true, true, i == last, childrenSize, factoryInst));
				} else if (streamType.equals("streamit.library.SplitJoin")) {
					fChildren.add(new SplitJoin(val, streamName, streamNameWithId, fAllExpanded, this, true, true, i == last, childrenSize, factoryInst));	
				} else if (streamType.equals("streamit.library.FeedbackLoop")) {
					fChildren.add(new FeedbackLoop(val, streamName, streamNameWithId, fAllExpanded, this, true, true, i == last, childrenSize, factoryInst));				
				}
			}

			// expanded size
			pipelineSize.width = IStreamItGraphConstants.MARGIN*2 + Math.max(childrenSize.width, pipelineSize.width*2 + IStreamItGraphConstants.MARGIN*2);
			pipelineSize.height = Math.max(childrenSize.height, pipelineSize.height + factoryInst.getArrowHeight());
			
		}
		setSize(pipelineSize);
		
		// content arrow
		fArrow = new ImageFigure(factoryInst.getArrow(true));
		fArrow.setSize(factoryInst.getArrowWidth(), factoryInst.getArrowHeight());
		add(fArrow);
		
		setVerticalLocation(null, 0);
	}
	
	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#setVerticalLocation(org.eclipse.draw2d.geometry.Point, int)
	 */
	public int setVerticalLocation(Point parentTopCenter, int currentHeight) {
		Dimension pipelineSize = getSize();
		Dimension headerSize = fHeader.getSize();
		Dimension arrowSize = fArrow.getSize();

		setLocation(new Point(IStreamItGraphConstants.MARGIN, IStreamItGraphConstants.MARGIN));
		Point top = getBounds().getTop();
		fHeader.setLocation(top.getTranslated(-pipelineSize.width/2 + IStreamItGraphConstants.MARGIN/2, IStreamItGraphConstants.MARGIN/2));
		
		if (!fExpanded) {
			// collapsed content
			fArrow.setLocation(top.getTranslated(-arrowSize.width/2, headerSize.height + IStreamItGraphConstants.MARGIN/2));

		} else {
			fArrow.setLocation(top.getTranslated(-pipelineSize.width/2, headerSize.height + IStreamItGraphConstants.MARGIN/2));

			// fix locations of children (top to bottom)
			for (int i = 0; i < fChildren.size(); i++)
				currentHeight = ((IStream) fChildren.get(i)).setVerticalLocation(top, currentHeight);
		}

		return currentHeight;				
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
	public Point getTopChannelTopLeft() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getInputLabelTopRight()
	 */
	public Point getTopChannelTopRight() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomLeft()
	 */
	public Point getBottomChannelBottomLeft() {
		// should not be called
		return null;
	}

	/* (non-Javadoc)
	 * @see streamit.eclipse.debugger.graph.IStream#getOutputLabelBottomRight()
	 */
	public Point getBottomChannelBottomRight() {
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
		fAllExpanded.setHighlightSelect(highlighting);
		return fAllExpanded;
	}
}
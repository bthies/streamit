package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;


/**
 * @author kkuo
 */
public class ChannelToggle extends Label {

	private String fId;
	/**
	 * @param image
	 */
	public ChannelToggle(String id, boolean expand, int realSize, StreamItViewFactory factoryInst) {
		super();
		
		fId = id;
		if (expand) setIcon(factoryInst.getMinus());
		else setIcon(factoryInst.getPlus());
		setText(String.valueOf(realSize));
		setTextPlacement(PositionConstants.SOUTH);
		Dimension d = FigureUtilities.getTextExtents(getText(), factoryInst.getFont());
		setSize(Math.max(factoryInst.getImageWidth(), d.width + IStreamItGraphConstants.MARGIN), 
				factoryInst.getImageHeight() + d.height + IStreamItGraphConstants.MARGIN);
		new ChannelToggleSelector(this);
	}

	public String getId() {
		return fId;
	}
}
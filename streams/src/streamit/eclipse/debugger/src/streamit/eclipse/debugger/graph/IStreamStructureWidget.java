package streamit.eclipse.debugger.graph;

import java.util.Vector;

import org.eclipse.draw2d.geometry.Point;

/**
 * @author kkuo
 */
public interface IStreamStructureWidget {
	// name
	public String getNameWithRuntimeId();
	public String getNameWithoutId();
	public String getRuntimeId();
	public String getStaticId();
	
	// widgets
	public Vector getChildStreams();
	public boolean isWithinIcon(Point p);
	public boolean isExpanded();
	public int getTopChannelToggleWidth();

	// layout
	public int setVerticalLocation(Point parentTopCenter, int currentHeight);
	public int setHorizontalLocation(int currentHeight, boolean stretchTopChannel, boolean stretchBottomChannel, int stretchHeight, int currentWidth);
	public Point getTopChannelTopLeft();
	public Point getTopChannelTopRight();
	public Point getBottomChannelBottomLeft();
	public Point getBottomChannelBottomRight();
	public void setChannelExpanded(boolean top, boolean bottom);
}
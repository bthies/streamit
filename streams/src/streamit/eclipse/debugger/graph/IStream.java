package streamit.eclipse.debugger.graph;

import org.eclipse.draw2d.geometry.Point;

/**
 * @author kkuo
 */
public interface IStream {
	public int setVerticalLocation(Point parentTopCenter, int currentHeight);
	public int setHorizontalLocation(int currentHeight, int stretchHeight, int currentWidth);
	public Point getTopChannelTopLeft();
	public Point getTopChannelTopRight();
	public Point getBottomChannelBottomLeft();
	public Point getBottomChannelBottomRight();	
	public String getNameWithId();
	public String getNameWithoutId();
	public String getId();
	public boolean isWithinIcon(Point p);
	public boolean isExpanded();
}
package at.dms.kjc.spacedynamic;

import java.util.LinkedList;
import at.dms.util.Utils;

/*******************************************************************************
 * Generate a route from <pre>src</pre> to <pre>dst</pre> routing X then Y, just as the dynamic
 * network would do it
 ******************************************************************************/
public class XYRouter {
    /***************************************************************************
     * return the route from src to dest (including both) first x then y, this
     * returns a list of compute nodes (tiles)
     **************************************************************************/
    public static LinkedList<ComputeNode> getRoute(SpdStaticStreamGraph ssg, ComputeNode src,
                                      ComputeNode dst) {
        LinkedList<ComputeNode> route = new LinkedList<ComputeNode>();
        RawChip chip = src.getRawChip();
      
        // if the source or dest is an iodevice we have to
        // route the item to/from neighboring tiles
        if (src.isPort())
            src = ((IOPort) src).getNeighboringTile();
        
        if (dst.isPort())
            dst = ((IOPort) dst).getNeighboringTile();
        
        
        // set this to the dst if the dst is an IODevice so we can
        // add it to the end of the route
        // route x then y
        route.add(src);

        assert src != null && dst != null : "Trying to route from/to null";

        int row = src.getX();
        int column = src.getY();

        // x then y
        if (src.getX() != dst.getX()) {
            if (src.getX() < dst.getX()) {
                for (row = src.getX() + 1; row <= dst.getX(); row++)
                    route.add(chip.getComputeNode(row, column));
                row--;
            } else {
                for (row = src.getX() - 1; row >= dst.getX(); row--)
                    route.add(chip.getComputeNode(row, column));
                row++;
            }
        }
        // column
        if (src.getY() != dst.getY()) {
            if (src.getY() < dst.getY())
                for (column = src.getY() + 1; column <= dst.getY(); column++)
                    route.add(chip.getComputeNode(row, column));
            else
                for (column = src.getY() - 1; column >= dst.getY(); column--)
                    route.add(chip.getComputeNode(row, column));
        }

        return route;
    }

    public static int distance(SpdStaticStreamGraph ssg, ComputeNode src,
                               ComputeNode dst) { // return the manhattan distance for the simple
        // router above
        return getRoute(ssg, src, dst).size();
    }

}

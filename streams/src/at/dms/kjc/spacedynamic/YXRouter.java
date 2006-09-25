package at.dms.kjc.spacedynamic;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;

public class YXRouter implements Router {
    // returns a linked list of coordinates that gives the route
    // including source and dest
    public LinkedList<ComputeNode> getRoute(SpdStaticStreamGraph ssg, ComputeNode src,
                               ComputeNode dst) {
        RawChip rawChip = ((SpdStreamGraph)ssg.getStreamGraph()).getRawChip();
        Layout layout = ((SpdStreamGraph)ssg.getStreamGraph()).getLayout();
        RawTile srcTile, dstTile;
        
        // if the source or dest is an iodevice we have to
        // route the item to/from neighboring tiles
        if (src.isPort())
            srcTile = ((IOPort) src).getNeighboringTile();
        else
            srcTile = (RawTile) src;
        if (dst.isPort())
            dstTile = ((IOPort) dst).getNeighboringTile();
        else
            dstTile = (RawTile) dst;

        
        LinkedList<ComputeNode> route = new LinkedList<ComputeNode>();
        route.add(srcTile);

        if (srcTile == null)
            System.out.println("From RawTile null");

        int row = srcTile.getY();
        int column = srcTile.getX();

        // row then column
        if (srcTile.getY() != dstTile.getY()) {
            if (srcTile.getY() < dstTile.getY()) {
                for (row = srcTile.getY() + 1; row <= dstTile.getY(); row++)
                    route.add(rawChip.getTile(column, row));
                row--;
            } else {
                for (row = srcTile.getY() - 1; row >= dstTile.getY(); row--)
                    route.add(rawChip.getTile(column, row));
                row++;
            }
        }
        // column
        if (srcTile.getX() != dstTile.getX()) {
            if (srcTile.getX() < dstTile.getX())
                for (column = srcTile.getX() + 1; column <= dstTile.getX(); column++)
                    route.add(rawChip.getTile(column, row));
            else
                for (column = srcTile.getX() - 1; column >= dstTile.getX(); column--)
                    route.add(rawChip.getTile(column, row));
        }
        // printRoute(from, to, route);

        // if the src or the dst is a device, me must add them to the route
        if (src.isPort())
            route.addFirst(src);
        if (dst.isPort())
            route.addLast(dst);
        return route;
    }

    public void printRoute(FlatNode from, FlatNode to, List route) {
        System.out.println(from.contents.getName() + " -> "
                           + to.contents.getName());
        Iterator it = route.iterator();
        while (it.hasNext()) {
            RawTile hop = (RawTile) it.next();
            System.out.println(hop.getTileNumber());
        }
    }

}

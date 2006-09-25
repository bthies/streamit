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

/** 
    This class generates a route from <pre>src</pre> to <pre>dst</pre> attempting to minimize the 
    number of tiles assigned to filters or joiners in the route 
**/

public class FreeTileRouter implements Router
{   
    private YXRouter yxRouter;
    
    class RouteAndOccupiedCount
    {
        //the route itself
        public LinkedList<ComputeNode> route;
        //the number of occupied tiles along the route
        public int occupied;
    
        public RouteAndOccupiedCount(LinkedList<ComputeNode> list, 
                                     int occ) 
        {
            this.route = list;
            this.occupied = occ;
        }
    }
    

    public FreeTileRouter() 
    {
        yxRouter = new YXRouter();
    }
    

    //returns a linked list of coordinates that gives the route
    //including source and dest
    public LinkedList<ComputeNode> getRoute(SpdStaticStreamGraph ssg, ComputeNode src, ComputeNode dst) 
    {
        assert src != null && dst != null;
    
        //if we can route this in a straight line, then just do it
        if (src.getX() == dst.getX() || src.getY() == dst.getY()) {
            return yxRouter.getRoute(ssg, src, dst);
        }
    
        Layout layout = ((SpdStreamGraph)ssg.getStreamGraph()).getLayout();

        //if the source or dest is an iodevice we have to 
        //route the item to/from neighboring tiles
        RawTile srcTile, dstTile;
        if (src.isPort())
            srcTile = ((IOPort)src).getNeighboringTile();
        else
            srcTile = (RawTile)src;
        if (dst.isPort())
            dstTile = ((IOPort)dst).getNeighboringTile();
        else
            dstTile = (RawTile)dst;

        

        //only try this scheme if the rawchip isn't too filled with assigned tiles
        //if (((double)layout.getTilesAssigned()) / ((double)layout.getRawChip().getTotalTiles()) >
        //   .85)
        //    return yxRouter.getRoute(ssg, src, dst);

        //call the recursive function to find the route with the least amount of occupied tiles
        RouteAndOccupiedCount bestRoute = 
            findBestRoute(ssg, layout, srcTile, dstTile);
    
        //make sure that if we don't have a route, then that is because we cannot
        //find a path that does not cross another ssg's node
        assert !(bestRoute.route.size() == 0 &&
                 !(bestRoute.occupied == Integer.MAX_VALUE));
    
        LinkedList<ComputeNode> route = bestRoute.route;

        //if we cannot find a legal route, just return a null route...
        if (route.size() == 0)
            return route;

        //if the src or the dst is a device, me must add them to the route
        if (src.isPort())
            route.addFirst(src);
        if (dst.isPort())
            route.addLast(dst);
    
        return route;
    }

    /** find the best route, remember of src is an occupied tile, then it will count it **/
    private RouteAndOccupiedCount findBestRoute(SpdStaticStreamGraph ssg, Layout layout, 
                                                RawTile src, RawTile dst) 
    {
        RawChip rawChip = layout.getRawChip();
    
        LinkedList<ComputeNode> route = new LinkedList<ComputeNode>();
    
        //make sure we do not route thru another SSG, set the route to be really high
        if (layout.getNode(src) != null && 
            !ssg.getFlatNodes().contains(layout.getNode(src))) {
            return new RouteAndOccupiedCount(route, Integer.MAX_VALUE);
        }
    

        //if the source == dst just add the dest and return, the end of the recursion
        if (src == dst) {
            route.add(dst);
            return new RouteAndOccupiedCount(route, 0);
        }
    
        int xDir = rawChip.getXDir(src, dst);
        int yDir = rawChip.getYDir(src, dst);
        //the route and the occupied count if we take X or Y
        //initialize the occupied count to a large integer that can never be 
        //obtained, in this case the total number of tiles +1
        RouteAndOccupiedCount takeX = 
            new RouteAndOccupiedCount(new LinkedList<ComputeNode>(),
                                      Integer.MAX_VALUE);
    
        RouteAndOccupiedCount takeY = 
            new RouteAndOccupiedCount(new LinkedList<ComputeNode>(),
                                      Integer.MAX_VALUE);
    
        if (xDir != 0) {
            takeX = findBestRoute(ssg, layout, 
                                  rawChip.getTile(src.getX() + xDir, 
                                                  src.getY()),
                                  dst);
        }
    
        //only try the y direction if the x direction has an occupied tile
        //and we need to route in that direction
        if (yDir != 0) {
            takeY = findBestRoute(ssg, layout,
                                  rawChip.getTile(src.getX(),
                                                  src.getY() + yDir),
                                  dst);
        }
    
        //we might not be able to make it from this source to the dest staying
        //within our own SSG so check for this and if so, propagate this information
        if (takeX.route.size() == 0 && takeY.route.size() == 0) {
            assert takeX.occupied == Integer.MAX_VALUE &&
                takeY.occupied == Integer.MAX_VALUE :
                takeX.occupied + " " + takeY.occupied;
            //just return, the illegal route
            return takeX;
        }
    

        //get the best route
        RouteAndOccupiedCount bestRoute = 
            takeX.occupied <= takeY.occupied ? takeX : takeY;
    
        //if the source of this route is an occupied tile, then add one 
        //to the count
        if (layout.isAssigned(src))
            bestRoute.occupied++;
    
        //add this src to the beginning of the route list
        bestRoute.route.addFirst(src);

        return bestRoute;
    }
}

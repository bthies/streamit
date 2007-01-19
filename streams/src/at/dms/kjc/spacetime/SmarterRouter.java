/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.HashSet;
import at.dms.util.Utils;

/**
 * @author mgordon
 *
 */
public class SmarterRouter implements Router {
    private int[] tileCosts;
    private RawChip rawChip;
    
    public SmarterRouter(int[] tileCosts, RawChip chip) {
        this.tileCosts = tileCosts;
        this.rawChip = chip;
    }
    
    public LinkedList<RawComputeNode> getRoute(RawComputeNode src, RawComputeNode dst) {
        //set this to the dst if the dst is an IODevice so we can
        //add it to the end of the route
        RawComputeNode realDst = null, realSrc = null;
    
        //we cannot route between IODevices so first route to neighbor
        if (src instanceof IODevice) {
            realSrc = src;
            src = ((IODevice)src).getNeighboringTile();
        }

        //if dst if an iodevice, set dest to be the neighboring tile of the dest
        //and add the real dest to the end of the route 
        if (dst instanceof IODevice) {
            realDst = dst;
            dst = ((IODevice)dst).getNeighboringTile();
        }
    
    
        if (src == null || dst == null)
            Utils.fail("Trying to route from/to null");

        RouteAndHopWork bestRoute = findBestRoute((RawTile)src, (RawTile)dst);        
        
        if (realSrc != null)
            bestRoute.route.addFirst(realSrc);
        
        if (realDst != null)
            bestRoute.route.add(realDst);
        
        return bestRoute.route;
    }
    /** 
     * Find the best route, route with the lowest total work count. 
     */
    private RouteAndHopWork findBestRoute(RawTile src, RawTile dst) 
    {
        LinkedList<RawComputeNode> route = new LinkedList<RawComputeNode>();
    

        //if the source == dst just add the dest and return, the end of the recursion
        if (src == dst) {
            route.add(dst);
            return new RouteAndHopWork(route, 0);
        }
    
        int xDir = rawChip.getXDir(src, dst);
        int yDir = rawChip.getYDir(src, dst);
        
        //the route if we take X or Y
        //initialize the work count to a large integer that can never be 
        //obtained
        RouteAndHopWork takeX = 
            new RouteAndHopWork(new LinkedList<RawComputeNode>(),
                                      Integer.MAX_VALUE);
    
        RouteAndHopWork takeY = 
            new RouteAndHopWork(new LinkedList<RawComputeNode>(),
                                      Integer.MAX_VALUE);
//      only try the x direction if we need to route in that direction
        if (xDir != 0) {
            takeX = findBestRoute(rawChip.getTile(src.getX() + xDir, 
                                                  src.getY()),
                                  dst);
        }
    
//      only try the y direction if we need to route in that direction
        if (yDir != 0) {
            takeY = findBestRoute(rawChip.getTile(src.getX(),
                                                  src.getY() + yDir),
                                  dst);
        }
    
        //get the best route
        RouteAndHopWork bestRoute = 
            takeX.hopWork <= takeY.hopWork ? takeX : takeY;
    
        //Add this source's cost to the cost of the route
        bestRoute.hopWork += tileCosts[src.getTileNumber()];
        
        //add this src to the beginning of the route list
        bestRoute.route.addFirst(src);

        return bestRoute;
    }
    
    
    public int distance(RawComputeNode src, RawComputeNode dst) {
        return getRoute(src, dst).size();
    }
   
    
}

class RouteAndHopWork {
    public int hopWork;
    public LinkedList<RawComputeNode> route;

    public RouteAndHopWork(LinkedList<RawComputeNode> r, int hw) {
        this.route = r;
        this.hopWork = hw;
    }
}

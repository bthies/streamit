/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.LinkedList;
import java.util.Random;

import at.dms.util.Utils;

/**
 * @author mgordon
 *
 */
public class RandomRouter implements Router {
    
    //return the route from src to dest (including both)
    //first x then y
    public LinkedList<RawComputeNode> getRoute(RawComputeNode src, RawComputeNode dst) 
    {
        LinkedList<RawComputeNode> route = new LinkedList<RawComputeNode>();
        RawChip chip = src.getRawChip();
        //set this to the dst if the dst is an IODevice so we can
        //add it to the end of the route
        RawComputeNode realDst = null;
    
        //route x then y
        route.add(src);

        //we cannot route between IODevices so first route to neighbor

        if (src instanceof IODevice) {
            src = ((IODevice)src).getNeighboringTile();
            route.add(src);     
        }

        //if dst if an iodevice, set dest to be the neighboring tile of the dest
        //and add the real dest to the end of the route 
        if (dst instanceof IODevice) {
            realDst = dst;
            dst = ((IODevice)dst).getNeighboringTile();
        }
    
        Random rand = new Random();
        
        if (src == null || dst == null)
            Utils.fail("Trying to route from/to null");

        RawComputeNode current = src;
        //while we have a decision, choose it randomly
        while (chip.getXDir(current, dst) != 0 &&
                chip.getYDir(current, dst) != 0) {
            if (rand.nextBoolean()) {
                //go x
                current = chip.getTile(current.getX() + chip.getXDir(current, dst), 
                        current.getY());
            }
            else {
                //go y
                current = chip.getTile(current.getX(), 
                        current.getY() + chip.getYDir(current, dst));
            }
            route.add(current);
        }
    
        //finish off in a line
        while (chip.getXDir(current, dst) != 0) {
            current = chip.getTile(current.getX() + chip.getXDir(current, dst),
                    current.getY());
            route.add(current);
        }
        while (chip.getYDir(current, dst) != 0) {
            current = chip.getTile(current.getX(), 
                    current.getY() + chip.getYDir(current, dst));
            route.add(current);
        }
        
        assert current == dst;
        
        if (realDst != null)
            route.add(realDst);
    
        return route;
    }
    
    public int distance(RawComputeNode src, RawComputeNode dst) 
    {  //return the manhattan distance for the simple router above
        return getRoute(src, dst).size();
    }
    
}

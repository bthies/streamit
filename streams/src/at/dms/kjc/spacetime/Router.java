package at.dms.kjc.spacetime;

import java.util.LinkedList;
import at.dms.util.Utils;

public class Router 
{
    //return the route from src to dest (including both)
    //first x then y
    public static LinkedList getRoute(ComputeNode src, ComputeNode dst) 
    {
	LinkedList route = new LinkedList();
	RawChip chip = src.getRawChip();
	//set this to the dst if the dst is an IODevice so we can
	//add it to the end of the route
	ComputeNode realDst = null;
	
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
	
	
	if (src == null || dst == null)
	    Utils.fail("Trying to route from/to null");

	int row = src.getX();
	int column = src.getY();

	//For now just route the packets in a stupid manner
	//x then y
	if (src.getX() != dst.getX()) {
	    if (src.getX() < dst.getX()) {
		for (row = src.getX() + 1; 
		     row <= dst.getX(); row++)
		    route.add(chip.getComputeNode(row, column));
		row--;
	    }
	    else {
		for (row = src.getX() - 1; 
		     row >= dst.getX(); row--) 
		    route.add(chip.getComputeNode(row, column));
		row++;
	    }
	}
	//column
	if (src.getY() != dst.getY()) {
	    if (src.getY() < dst.getY())
		for (column = src.getY() + 1; 
		     column <= dst.getY(); column++)
		    route.add(chip.getComputeNode(row, column));
	    else
		for (column = src.getY() - 1; 
		     column >= dst.getY(); column--)
		    route.add(chip.getComputeNode(row, column));
	}
	
	if (realDst != null)
	    route.add(realDst);
	
	return route;
    }
    
    public static int distance(ComputeNode src, ComputeNode dst) 
    {  //return the manhattan distance for the simple router above
	return getRoute(src, dst).size();
    }
    
}


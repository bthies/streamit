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
    This class generates a route from <src> to <dst> attempting to minimize the 
    number of tiles assigned to filters or joiners in the route 
**/

public class FreeTileRouter implements Router
{   
    private YXRouter yxRouter;
    
    class RouteAndOccupiedCount
    {
	//the route itself
	public LinkedList route;
	//the number of occupied tiles along the route
	public int occupied;
	
	public RouteAndOccupiedCount(LinkedList list, 
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
    public LinkedList getRoute(StaticStreamGraph ssg, ComputeNode src, ComputeNode dst) 
    {
	assert src != null && dst != null;
	
	//if we can route this in a straight line, then just do it
	if (src.getX() == dst.getX() || src.getY() == dst.getY()) {
	    return yxRouter.getRoute(ssg, src, dst);
	}
	
	Layout layout = ssg.getStreamGraph().getLayout();

	//only try this scheme if the rawchip isn't too filled with assigned tiles
	if (((double)layout.getTilesAssigned()) / ((double)layout.getRawChip().getTotalTiles()) >
	    .85)
	    return yxRouter.getRoute(ssg, src, dst);

	//call the recursive function to find the route with the last amount of occupied tiles
	return findBestRoute(layout, (RawTile)src, (RawTile)dst).route;
    }

    /** find the best route, remember of src is an occupied tile, then it will count it **/
    private RouteAndOccupiedCount findBestRoute(Layout layout, RawTile src, RawTile dst) 
    {
	RawChip rawChip = layout.getRawChip();
	
	LinkedList route = new LinkedList();
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
	    new RouteAndOccupiedCount(new LinkedList(),
				      rawChip.getTotalTiles() + 1);
	RouteAndOccupiedCount takeY = 
	    new RouteAndOccupiedCount(new LinkedList(),
				      rawChip.getTotalTiles() + 1);
	
	if (xDir != 0) {
	    takeX = findBestRoute(layout, 
				  rawChip.getTile(src.getX() + xDir, 
						  src.getY()),
				  dst);
	}
	
	//only try the y direction if the x direction has an occupied tile
	//and we need to route in that direction
	if (takeX.occupied > 0 && yDir != 0) {
	    takeY = findBestRoute(layout,
				  rawChip.getTile(src.getX(),
						  src.getY() + yDir),
				  dst);
	}
	
	//get the best route
	RouteAndOccupiedCount bestRoute = 
	    takeX.occupied < takeY.occupied ? takeX : takeY;
	
	//if the source of this route is an occupied tile, then add one 
	//to the count
	if (layout.isAssigned(src))
	    bestRoute.occupied++;
	
	//add this src to the beginning of the route list
	bestRoute.route.addFirst(src);

	return bestRoute;
    }
    
}

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

	//route x then y
	route.add(src);

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
	return route;
    }
    
    public static int distance(ComputeNode src, ComputeNode dst) 
    {  //return the manhattan distance for the simple router above
	return Math.abs(src.getX() - dst.getX()) + 
	    Math.abs(src.getY() - dst.getY());
    }
    
}


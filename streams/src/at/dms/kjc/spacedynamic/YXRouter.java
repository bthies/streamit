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

public class YXRouter implements Router
{   
    //returns a linked list of coordinates that gives the route
    //including source and dest
    public LinkedList getRoute(StaticStreamGraph ssg, ComputeNode fromCoord, ComputeNode toCoord)
    {
	RawChip rawChip = ssg.getStreamGraph().getRawChip();
	Layout layout = ssg.getStreamGraph().getLayout();

	LinkedList route = new LinkedList();
	route.add(fromCoord);
	
	if (fromCoord== null)
	    System.out.println("From RawTile null");

	int row = fromCoord.getY();
	int column = fromCoord.getX();

	//row then column
	if (fromCoord.getY() != toCoord.getY()) {
	    if (fromCoord.getY() < toCoord.getY()) {
		for (row = fromCoord.getY() + 1; 
		     row <= toCoord.getY(); row++)
		    route.add(rawChip.getTile(column, row));
		row--;
	    }
	    else {
		for (row = fromCoord.getY() - 1; 
		     row >= toCoord.getY(); row--) 
		    route.add(rawChip.getTile(column, row));
		row++;
	    }
	}
	//column
	if (fromCoord.getX() != toCoord.getX()) {
	    if (fromCoord.getX() < toCoord.getX())
		for (column = fromCoord.getX() + 1; 
		     column <= toCoord.getX(); column++)
		    route.add(rawChip.getTile(column, row));
	    else
		for (column = fromCoord.getX() - 1; 
		     column >= toCoord.getX(); column--)
		    route.add(rawChip.getTile(column, row));
	}
	//printRoute(from, to, route);
	return route;
    }

    public void printRoute(FlatNode from, FlatNode to, List route) {
	System.out.println(from.contents.getName() + " -> " + to.contents.getName());
	Iterator it = route.iterator();
	while (it.hasNext()) {
	    RawTile hop = (RawTile) it.next();
	    System.out.println(hop.getTileNumber());
	}
    }
    
}


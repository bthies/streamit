package at.dms.kjc.raw;

import java.util.LinkedList;

public class Router {
    
    //returns a linked list of coordinates that gives the route
    //including source and dest
    public static LinkedList getRoute(FlatNode from, FlatNode to) 
    {
	LinkedList route = new LinkedList();
	Coordinate fromCoord, toCoord;
	fromCoord = Layout.getTile(from);
	toCoord = Layout.getTile(to);
	
	route.add(Layout.getTile(from));
	
	int row = fromCoord.getRow();
	int column = fromCoord.getColumn();
	//For now just route the packets in a stupid manner
	//row then column
	if (fromCoord.getRow() != toCoord.getRow()) {
	    if (fromCoord.getRow() < toCoord.getRow()) {
		for (row = fromCoord.getRow() + 1; 
		     row <= toCoord.getRow(); row++)
		    route.add(Layout.getTile(row, column));
		row--;
	    }
	    else {
		for (row = fromCoord.getRow() - 1; 
		     row >= toCoord.getRow(); row--) 
		    route.add(Layout.getTile(row, column));
		row++;
	    }
	}
	//column
	if (fromCoord.getColumn() != toCoord.getColumn()) {
	    if (fromCoord.getColumn() < toCoord.getColumn())
		for (column = fromCoord.getColumn() + 1; 
		     column <= toCoord.getColumn(); column++)
		    route.add(Layout.getTile(row, column));
	    else
		for (column = fromCoord.getColumn() - 1; 
		     column >= toCoord.getColumn(); column--)
		    route.add(Layout.getTile(row, column));
	}
	return route;
    }
}


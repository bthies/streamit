package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


/**
 *The Layout class generates mapping of filters to raw tiles.  It assumes that the 
 * namer has been run and that the stream graph has been partitioned.
 */
public class Layout extends at.dms.util.Utils implements FlatVisitor {

    private static HashMap assignment;
    private static Coordinate[][] coordinates;
    private BufferedReader inputBuffer;
    /* hashset of Flatnodes representing all the joiners
       that are mapped to tiles */
    public static HashSet joiners;
    
    public Layout() 
    {
	inputBuffer = new BufferedReader(new InputStreamReader(System.in));
	joiners = new HashSet();
    }
    
    public static void handAssign(FlatNode node) 
    {
	//create the array of tile objects so that we can use them 
	//in hashmaps
	coordinates  =
	    new Coordinate[StreamItOptions.rawRows][StreamItOptions.rawColumns];
	for (int row = 0; row < StreamItOptions.rawRows; row++)
	    for (int column = 0; column < StreamItOptions.rawColumns; column++)
		coordinates[row][column] = new Coordinate(row, column);
		
	assignment = new HashMap();
	
	System.out.println("Enter desired tile for each filter...");
	// assign raw tiles to filters
	node.accept(new Layout(), new HashSet(), false);
    }
    /**
     * Returns the tile number assignment for <str>, or null if none has been assigned.
     */
    public static Coordinate getTile(SIROperator str) 
    {
	if (assignment == null) return null;
	return (Coordinate)assignment.get(str);
    }
    
    public static Coordinate getTile(FlatNode str) 
    {
	if (assignment == null) return null;
	return (Coordinate)assignment.get(str.contents);
    }

    public static Coordinate getTile(int row, int column) 
    {
	return coordinates[row][column];
    }
    
    public static String getDirection(Coordinate from,
				      Coordinate to) {
	if (from == to)
	    return "st";

	
	if (from.getRow() == to.getRow()) {
	    int dir = from.getColumn() - to.getColumn();
	    if (dir == -1)
		return "E";
	    else if (dir == 1)
		return "W";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	if (from.getColumn() == to.getColumn()) {
	    int dir = from.getRow() - to.getRow();
	    if (dir == -1) 
		return "S";
	    else if (dir == 1)
		return "N";
	    else
		Utils.fail("calling getDirection on non-neighbors");
	}
	Utils.fail("calling getDirection on non-neighbors");
	return "";
    }

    public static int getTileNumber(Coordinate tile)
    {
	int row = tile.getRow();
	int column = tile.getColumn();
	return (row * StreamItOptions.rawRows) + column;
    }
    
    public static int getTileNumber(SIROperator str)
    {
	return getTileNumber(getTile(str));
    }	
	

    private void handAssignNode(FlatNode node) 
    {
	//Assign a filter, joiner to a tile 
	//perform some error checking.
	while (true) {
	    try {
		Integer row, column;
		
		//get row
		System.out.print(Namer.getName(node.contents) + "\nRow: ");
		row = Integer.valueOf(inputBuffer.readLine());
		if (row.intValue() < 0) {
		    System.err.println("Negative Value: Try again.");
		    continue;
		}
		if (row.intValue() > (StreamItOptions.rawRows -1)) {
		    System.err.println("Value Too Large: Try again.");
		    continue;
		}
		//get column
		System.out.print("Column: ");
		column = Integer.valueOf(inputBuffer.readLine());
		if (column.intValue() < 0) {
		    System.err.println("Negative Value: Try again.");
		    continue;
		}
		if (column.intValue() > (StreamItOptions.rawColumns -1)) {
		    System.err.println("Value Too Large: Try again.");
		    continue;
		}
		//check if this tile has been already assigned
		Iterator it = assignment.values().iterator();
		while(it.hasNext()) {
		    Coordinate current = (Coordinate)it.next();
		    if (current.getRow() == row.intValue() &&
			current.getColumn() == column.intValue())
			System.err.println("Tile Already Assigned: Try Again.");
		    continue;
		}
		
		assignment.put(node.contents, coordinates[row.intValue()][column.intValue()]);
		return;
	    }
	    catch (Exception e) {
		System.err.println("Error:  Try again.");
	    }
	}
    }
    
    public void visitNode(FlatNode node) 
    {
	if (node.contents instanceof SIRFilter) {
	    handAssignNode(node);
	    return;
	}
	if (node.contents instanceof SIRJoiner) {
	    if (!(node.edges[0].contents instanceof SIRJoiner)) {
		joiners.add(node);
		handAssignNode(node);
		return;
	    }
	}
    }
}  

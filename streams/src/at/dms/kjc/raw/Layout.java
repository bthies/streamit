package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.io.*;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;


/**
 *The Layout class generates mapping of filters to raw tiles.  It assumes that the 
 * namer has been run and that the stream graph has been partitioned.
 */
public class Layout extends at.dms.util.Utils implements StreamVisitor {

    private static HashMap assignment;
    private static Coordinate[][] coordinates;
    private BufferedReader inputBuffer;
    
    public Layout() 
    {
	inputBuffer = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public static void handAssign(SIROperator str) 
    {
	//create the array of tile objects so that we can use them 
	//in hashmaps
	coordinates  =
	    new Coordinate[StreamItOptions.rawRows][StreamItOptions.rawColumns];
	for (int row = 0; row < StreamItOptions.rawRows; row++)
	    for (int column = 0; column < StreamItOptions.rawColumns; column++)
		coordinates[row][column] = new Coordinate(row, column);
		
	assignment = new HashMap();
	// find toplevel stream
	SIROperator toplevel = str;
	while (toplevel.getParent()!=null) {
	    toplevel = toplevel.getParent();
	}
	System.out.println("Enter desired tile for each filter...");
	// assign raw tiles to filters
	toplevel.accept(new Layout());
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
	System.out.println(row + " " + column);
	
	return coordinates[row][column];
    }
    
    public static String getDirection(Coordinate from,
				      Coordinate to) {
	if (from == to)
	    Utils.fail("Calling getDirection on self");

	
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
	

    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init,
			    JMethodDeclaration work,
			    CType inputType, CType outputType) {
	//Assign a filter to a tile 
	//perform some error checking.
	while (true) {
	    try {
		Integer row, column;
		
		//get row
		System.out.print(Namer.getName(self) + "\nRow: ");
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
		
		assignment.put(self, coordinates[row.intValue()][column.intValue()]);
		return;
	    }
	    catch (Exception e) {
		System.err.println("Error:  Try again.");
	    }
	}
    }

    /** 
     * visit a splitter 
     */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      JExpression[] weights) {

    }

    /** 
     * visit a joiner 
     */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    JExpression[] weights) {

    }

	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {

    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {

    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {

    }

	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {

    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {

    }


    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {

    }
}  

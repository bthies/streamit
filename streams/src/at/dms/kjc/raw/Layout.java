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
    
    private BufferedReader inputBuffer;
    
    public Layout() 
    {
	inputBuffer = new BufferedReader(new InputStreamReader(System.in));
    }
    
    public static void handAssign(SIROperator str) 
    {
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

    public static Iterator tileIterator() {
	return assignment.values().iterator();
    }
    
    /**
     * Returns the tile number assignment for <str>, or null if none has been assigned.
     */
    public static int getTile(SIROperator str) 
    {
	if (assignment == null) return -1;
	return ((Integer)(assignment.get(str))).intValue();
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
		Integer tile;
		System.out.print(Namer.getName(self) + ": ");
		tile = Integer.valueOf(inputBuffer.readLine());
		if (tile.intValue() < 0) {
		    System.err.println("Negative Value: Try again.");
		    continue;
		}
		if (tile.intValue() > (StreamItOptions.raw -1)) {
		    System.err.println("Value Too Large: Try again.");
		    continue;
		}
		if (assignment.containsValue(tile)) {
		    System.err.println("Value Already Assigned: Try Again.");
		}
		assignment.put(self, tile);
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

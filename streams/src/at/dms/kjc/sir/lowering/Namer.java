package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.HashMap;

/**
 * This generates a mapping of stream structure to name for each
 * structure in the stream graph.
 */
public class Namer extends at.dms.util.Utils implements StreamVisitor {
    /**
     * Mapping from stream structure (SIROperator) to name (String) of
     * structure in target program.  Assumes that structure is
     * completely connected.
     */
    private static HashMap names;
    /**
     * Prefix of numbers used for naming
     */
    private LinkedList namePrefix;
    
    /**
     * Make a new namer.
     */
    private Namer() {
	this.namePrefix = new LinkedList();
    }

    /**
     * Assigns a name to all nodes that are connected to this, either
     * as parent or child stream structures.  The whole graph is named
     * as a unit to ensure consistency of names between segments.
     * However, the names should only be assigned once the stream
     * structure is completely formed.
     */
    public static void assignNames(SIROperator str) {
	// re-initialize mapping from streams to names
	names = new HashMap();
	// find toplevel stream
	SIROperator toplevel = str;
	while (toplevel.getParent()!=null) {
	    toplevel = toplevel.getParent();
	}
	// name the stream structure
	toplevel.accept(new Namer());
    }

    /**
     * Returns the name for <str>, or null if none has been assigned.
     */
    public static String getName(SIROperator str) {
	return (String)names.get(str);
    }

    /**
     * Associates a name with <str>, saving the result in <names>.
     */
    private void addName(SIROperator str) {
	StringBuffer name = new StringBuffer();
	// start name with the class of the IR object
	name.append(splitQualifiedName(str.getClass().toString(), '.')[1]);
	// end name with list of positions, e.g. 1_2_1_
	for (ListIterator e = namePrefix.listIterator(); e.hasNext(); ) {
	    name.append("_");
	    name.append(e.next());
	}
	// associate name with <str>
	names.put(str, name.toString());
    }

    /**
     * PLAIN-VISITS 
     */
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init,
			    int peek, int pop, int push,
			    JMethodDeclaration work,
			    CType inputType, CType outputType) {
	addName(self);
	// increment the count on the prefix names
	Integer old = (Integer)namePrefix.removeLast();
	namePrefix.add(new Integer(old.intValue()+1));
    }

    /** 
     * visit a splitter 
     */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      int[] weights) {
	fail("Not supported yet.");
    }

    /** 
     * visit a joiner 
     */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    int[] weights) {
	fail("Not supported yet.");
    }

    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {
	addName(self);
	// start counting children with namePrefix
	namePrefix.add(new Integer(1));
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	fail("Not supported yet.");
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	fail("Not supported yet.");
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
	// stop counting children by removing a digit from namePrefix
	namePrefix.removeLast();
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
	fail("Not supported yet.");
    }


    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
	fail("Not supported yet.");
    }
}

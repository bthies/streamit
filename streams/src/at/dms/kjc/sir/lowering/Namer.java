package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.util.Utils;
import java.util.*;

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
     * Returns the stream associated with name <name>.  This takes
     * linear time instead of constant (could optimize by maintaining
     * a reverse map if desired.)  Requires that this contains a
     * stream by the name of <name>.
     */
    public static SIROperator getStream(String name) {
	for (Iterator it = names.entrySet().iterator(); it.hasNext(); ) {
	    Map.Entry entry = (Map.Entry)it.next();
	    if (entry.getValue().equals(name)) {
		return (SIROperator)entry.getKey();
	    }
	}
	Utils.fail("Couldn't find stream by name of \"" + name + "\"");
	// stupid compiler
	return null;
    }

    /**
     * Returns the name for <str>, or null if none has been assigned.
     */
    public static String getName(SIROperator str) {
	if (names==null) { return null; }
	return (String)names.get(str);
    }

    /**
     * Associates a name with <str>, saving the result in <names>.
     */
    private void addName(SIROperator str) {
	StringBuffer name = new StringBuffer();
	// build the prefix of the name as the most descriptive
	// identifier that we can--if the object has an <ident> from
	// the original source file, then use that; otherwise use the
	// class of the IR object
	String prefix;
	if (str instanceof SIRStream) {
	    // if we have a stream, then it should have an identifier
	    prefix = ((SIRStream)str).getIdent();
	    Utils.assert(prefix!=null,
			 "Didn't expect to find null identifier of SIRStream");
	} else {
	    // otherwise, get the class of the IR object
	    prefix = splitQualifiedName(str.getClass().toString(), '.')[1] ;
	}
	// add the prefix to the name
	name.append(prefix);
	// end name with list of positions, e.g. 1_2_1_
	for (ListIterator e = namePrefix.listIterator(); e.hasNext(); ) {
	    name.append("_");
	    name.append(e.next());
	}
	// associate name with <str>
	names.put(str, name.toString());
    }

    /**
     * If the stack of names is not empty, increments the name on the top.
     */
    private void incCount() {
	if (namePrefix.size()!=0) {
	    Integer old = (Integer)namePrefix.removeLast();
	    namePrefix.add(new Integer(old.intValue()+1));
	}
    }

    /**
     * PLAIN-VISITS 
     */

    /**
     * The plain visit just names <self> and increments the count.
     */
    private void plainVisit(SIROperator self) {
	addName(self);
	incCount();
    }
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init,
			    JMethodDeclaration work,
			    CType inputType, CType outputType) {
	plainVisit(self);
    }

    /** 
     * visit a splitter 
     */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      JExpression[] weights) {
	plainVisit(self);
    }

    /** 
     * visit a joiner 
     */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    JExpression[] weights) {
	plainVisit(self);
    }

    /**
     * PRE-VISITS 
     */

    /**
     * The pre-visit adds a stream and starts counting the children.
     */
    private void preVisit(SIRStream self) {
	addName(self);
	// start counting children with namePrefix
	namePrefix.add(new Integer(1));
    }
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {
	preVisit(self);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	preVisit(self);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	preVisit(self);
    }

    /**
     * POST-VISITS 
     */
	    
    /**
     * The post-visit stops counting children and increments the count
     * for the next one in the sequence.
     */
    private void postVisit () {
	// stop counting children by removing a digit from namePrefix
	namePrefix.removeLast();
	// increment the count on the prefix names
	incCount();
    }
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
	postVisit();
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
	postVisit();
    }


    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
	postVisit();
    }
}

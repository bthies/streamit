package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A LinearReplacer is the base class that all replacers inherit from.
 **/
public abstract class LinearReplacer extends EmptyStreamVisitor implements Constants{
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {makeReplacement(self);}
    public void preVisitPipeline(    SIRPipeline self,
				     SIRPipelineIter iter){makeReplacement(self);}
    public void preVisitSplitJoin(   SIRSplitJoin self,
				     SIRSplitJoinIter iter){makeReplacement(self);}
    public void visitFilter(         SIRFilter self,
				     SIRFilterIter iter){makeReplacement(self);}


    /**
     * Visit a pipeline, splitjoin or filter, replacing them with a new filter
     * that presumably calculates things more efficiently.
     **/
    public abstract void makeReplacement(SIRStream self);


    
    /**
     * Gets all children of the specified stream.
     **/
    HashSet getAllChildren(SIRStream self) {
	// basically, push a new visitor through which keeps track of the
	LinearChildCounter kidCounter = new LinearChildCounter();
	// stuff the counter through the stream
	IterFactory.createIter(self).accept(kidCounter);
	return kidCounter.getKids();
	
    }
    /** inner class to get a list of all the children streams. **/
    class LinearChildCounter extends EmptyStreamVisitor {
	HashSet kids = new HashSet();
	public HashSet getKids() {return this.kids;}
	public void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {kids.add(self);}
	public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter){kids.add(self);}
	public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter){kids.add(self);}
	public void visitFilter(SIRFilter self, SIRFilterIter iter){kids.add(self);}
    }

    /* returns an array that is a field declaration for (newField)
     * appended to the original field declarations. **/
    public JFieldDeclaration[] appendFieldDeclaration(JFieldDeclaration[] originals,
						      JVariableDefinition newField) {
	
	/* really simple -- make a new array, copy the elements from the
	 * old array and then stick in the new one. */
	JFieldDeclaration[] returnFields = new JFieldDeclaration[originals.length+1];
	for (int i=0; i<originals.length; i++) {
	    returnFields[i] = originals[i];
	}
	/* now, stick in a new field declaration. */
	returnFields[originals.length]   = new JFieldDeclaration(null, newField, null, null);
	return returnFields;
    }


    
}

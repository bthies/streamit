package at.dms.kjc;

import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import at.dms.compiler.JavaStyleComment;

import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

/**
 * This descends through a stream hierarchy and identifies local
 * variables and stream structures that SHOULD be cloned (since their
 * definition is within the hierarchy).
 */
public class CloningVisitor extends SLIREmptyVisitor implements StreamVisitor {

    /**
     * A list of things that *should* be cloned.  Currently the
     * following types should not be cloned unless they are an
     * element of this list:
     *   - SIRContainer
     *   - JLocalVariable
     */
    private HashSet toBeCloned;

    public CloningVisitor() {
	this.toBeCloned = new HashSet();
    }
    
    /**
     * Used by deepCopy(int offset,JBlock oldObj) above
     */
    public void visitBlockStatement(int offset,
				    JBlock self,
				    JavaStyleComment[] comments) {
	for(;offset<self.size();offset++) {
	    self.getStatement(offset).accept(this);
	}
    }

    /**
     * Return the list of what should be cloned.
     */
    public HashSet getToBeCloned() {
	return toBeCloned;
    }

    /**
     * Right now the super doesn't visit the variable in a jlocal var,
     * but make sure we don't, either.
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
					     String ident) {
    }

    /**
     * Visits a variable decl.
     */
    public void visitVariableDefinition(JVariableDefinition self,
					int modifiers,
					CType type,
					String ident,
					JExpression expr) {
	super.visitVariableDefinition(self, modifiers, type, ident, expr);
	// record that we should clone this, since we reached it
	toBeCloned.add(self);
    }
    
    /**
     * visits a formal param.
     */
    public void visitFormalParameters(JFormalParameter self,
				      boolean isFinal,
				      CType type,
				      String ident) {
	super.visitFormalParameters(self, isFinal, type, ident);
	// record that we should clone this, since we reached it
	toBeCloned.add(self);
    }

    /**
     * Visits an init statement (recurses into the target stream)
     */
    public void visitInitStatement(SIRInitStatement self,
				   SIRStream target) {
	super.visitInitStatement(self, target);
	// also recurse into the stream target
	IterFactory.createIter(target).accept(this);
    }

    /**
     * PLAIN-VISITS 
     */

    /**
     * For visiting all fields and methods of SIRStreams.
     */
    private void visitStream(SIRStream stream) {
	// visit the methods
	JMethodDeclaration[] methods = stream.getMethods();
        if (methods != null) {
            for (int i=0; i<methods.length; i++) {
                methods[i].accept(this);
            }
        }
	// visit the fields
	JFieldDeclaration[] fields = stream.getFields();
	for (int i=0; i<fields.length; i++) {
	    fields[i].accept(this);
	}
    }
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
	// visit node
	visitStream(self);
    }

    /* visit a phased filter */
    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        visitStream(self);
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit children
	for (int i=0; i<self.size(); i++) {
	    iter.get(i).accept(this);
	}
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit children
	for (int i=0; i<self.size(); i++) {
	    iter.get(i).accept(this);
	}
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	// record this container as one that should be cloned
	toBeCloned.add(self);
	// visit node
	visitStream(self);
	// visit body stream
	iter.getBody().accept(this);
	// visit loop stream
	iter.getLoop().accept(this);
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline -- do nothing, visit on way down */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
    }

    /* post-visit a splitjoin -- do nothing, visit on way down */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
    }

    /* post-visit a feedbackloop -- do nothing, visit on way down */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
    }
}

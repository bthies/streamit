package at.dms.kjc.sir.lowering.fusion;

import streamit.scheduler1.*;
import streamit.scheduler1.simple.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

public class Lifter implements StreamVisitor {

    private Lifter() {}

    /**
     * If <str> is a pipeline that either:
     *  1) Has a pipeline parent, or
     *  2) Contains only a single stream
     *
     * then eliminate the pipeline (adjusting pipeline's parent
     * accordingly).  Returns whether or not the lifting was done.
     */
    public static boolean eliminatePipe(final SIRPipeline str) {
	// get parent
	SIRContainer parent = str.getParent();

	// consider only if we have have a parent that's a pipe or if
	// we only have a single child
	if (parent!=null && (str.size()==1 || parent instanceof SIRPipeline)) {
	    
	    // this assumes that we're not worrying about fields and
	    // methods in containers -- otherwise we need renaming and
	    // better handling of possible init function arguments?
	    Utils.assert(str.getFields()==null || str.getFields().length==0,
			 "Not expecting to find fields in container in Lifter.");
	    Utils.assert(str.getMethods()==null || str.getMethods().length==0 ||
			 (str.getMethods().length==1 && str.getMethods()[0]==str.getInit()),
			 "Not expecting to find methods in container in Lifter.");
	    
	    int index = parent.indexOf(str);
	    for (int i=0; i<str.size(); i++) {
		parent.add(index+1+i, str.get(i), str.getParams(i));
	    }

	    parent.remove(index);
	    return true;

	} else {
	    return false;
	}
    }

    /**
     * Lift everything we can in <str> and its children
     */
    public static void lift(SIRStream str) {
	IterFactory.createIter(str).accept(new Lifter());
    }

    /**
     * PLAIN-VISITS 
     */
	    
    /* visit a filter */
    public void visitFilter(SIRFilter self,
			    SIRFilterIter iter) {
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	liftChildren(self);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	liftChildren(self);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	liftChildren(self);
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
	liftChildren(self);
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
	liftChildren(self);
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
	liftChildren(self);
    }

    private void liftChildren(SIRContainer str) {
	for (int i=0; i<str.size(); i++) {
	    SIROperator child = (SIROperator)str.get(i);
	    if (child instanceof SIRPipeline) {
		eliminatePipe((SIRPipeline)child);
	    }
	}
    }
}

package at.dms.kjc.sir.lowering.fusion;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
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

    /* visit a phased filter */
    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
	liftPipelineChildren(self);
	RefactorSplitJoin.removeMatchingSyncPoints(self);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	boolean changing;
	do {
	    changing = false;
	    changing = liftPipelineChildren(self) || changing;
	    changing = liftIntoSplitJoin(self) || changing;
	} while (changing);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	liftPipelineChildren(self);
    }

    /**
     * POST-VISITS
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
	RefactorSplitJoin.removeMatchingSyncPoints(self);
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
    }

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
			 "Not expecting to find methods in container in Lifter, but found " + str.getMethods().length + " in " + str.getName() + ":\n" 
			 + str.getMethods()[0]);
	    
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
     * If <str> is a splitjoin that contains only one child, then
     * eliminate it (adjusting splitjoin's parent appropriately.)
     * Returns whether or not the lifting was done.
     */
    public static boolean eliminateSJ(final SIRSplitJoin str) {
	// get parent
	SIRContainer parent = str.getParent();

	if (parent!=null && str.size()==1) {
	    // this assumes that we're not worrying about fields and
	    // methods in containers -- otherwise we need renaming and
	    // better handling of possible init function arguments?
	    Utils.assert(str.getFields()==null || str.getFields().length==0,
			 "Not expecting to find fields in container in Lifter.");
	    Utils.assert(str.getMethods()==null || str.getMethods().length==0 ||
			 (str.getMethods().length==1 && str.getMethods()[0]==str.getInit()),
			 "Not expecting to find methods in container in Lifter, but found " + str.getMethods().length + " in " + str.getName() + ":\n" 
			 + str.getMethods()[0]);
	    
	    parent.replace(str, str.get(0));
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Returns whether or not something changed.
     */
    private boolean liftPipelineChildren(SIRContainer str) {
	boolean anyChange = false;
	boolean changing;
	do {
	    changing = false;
	    for (int i=0; i<str.size(); i++) {
		SIROperator child = (SIROperator)str.get(i);
		if (child instanceof SIRPipeline) {
		    changing = eliminatePipe((SIRPipeline)child) || changing;
		} else if (child instanceof SIRSplitJoin) {
		    changing = eliminateSJ((SIRSplitJoin)child) || changing;
		}
	    }
	    anyChange = anyChange || changing;
	} while (changing);
	return anyChange;
    }

    /**
     * Returns whether or not something changed.
     */
    private boolean liftIntoSplitJoin(SIRSplitJoin sj) {
	boolean anyChange = false;
	boolean changing;
	do {
	    changing = RefactorSplitJoin.raiseSJChildren(sj);
	    anyChange = anyChange || changing;
	} while (changing);
	return anyChange;
    }
}

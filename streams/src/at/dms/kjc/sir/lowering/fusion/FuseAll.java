package at.dms.kjc.sir.lowering.fusion;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This class fuses all the pipelines that it can in a stream graph.
 * We need to fuse all the children of the current stream we're
 * visiting (rather than the stream itself) so that the iterators
 * don't get confused.
 */
public class FuseAll implements StreamVisitor {

    private FuseAll() {}

    /**
     * Fuse everything we can in <str>
     */
    public static void fuse(SIRStream str) {
	// try fusing toplevel separately since noone contains it
	FuseAll fuseAll = new FuseAll();
	fuseAll.fuseChild(str);
	IterFactory.createIter(str).accept(fuseAll);
	fuseAll.fuseChild(str);
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
	fuseChildren(self);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
	fuseChildren(self);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	fuseChildren(self);
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
	fuseChildren(self);
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
	fuseChildren(self);
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
	fuseChildren(self);
    }

    private void fuseChildren(SIRContainer str) {
	for (int i=0; i<str.size(); i++) {
	    fuseChild((SIROperator)str.get(i));
	}
    }

    private void fuseChild(SIROperator child) {
	if (child instanceof SIRPipeline) {
	    FusePipe.fuse((SIRPipeline)child);
	} else if (child instanceof SIRSplitJoin) {
	    FuseSplit.fuse((SIRSplitJoin)child);
	}
    }
}

package at.dms.kjc.sir.lowering.fusion;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;
import at.dms.util.Utils;

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
     * Fuse everything we can in <str>.  Returns new fused segment,
     * which is also mutated in the stream graph.
     */
    public static SIRPipeline fuse(SIRStream str) {
	// try fusing toplevel separately since noone contains it
	SIRPipeline wrapper = SIRContainer.makeWrapper(str);
	FuseAll fuseAll = new FuseAll();
	boolean hasFused = true;
	while (hasFused) {
	    try {
		Utils.assert(wrapper.get(0).getParent() == wrapper);
		Lifter.lift(wrapper);
		if (wrapper.size()>1) {
		    wrapper = SIRContainer.makeWrapper(wrapper);
		}
		IterFactory.createIter(wrapper.get(0)).accept(fuseAll);
		hasFused = false;
	    } catch (SuccessfulFuseException e) {}
	}
	return wrapper;
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
        // There's an argument to be made that this should flatten the
        // phases into a single work function, but this isn't necessarily
        // correct.  So do nothing instead.
    }
  
    /**
     * PRE-VISITS 
     */
	    
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRPipelineIter iter) {
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
	Utils.fail("Don't yet support fusion of feedback loops.");
    }

    /**
     * POST-VISITS 
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
	int elim = FusePipe.fuse(self);
	Utils.assert(elim>0, "Tried to fuse " + self + " that has " + self.size() + " components, but didn't eliminate anything.");
	throw new SuccessfulFuseException();
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
	SIRStream result = FuseSplit.fuse(self);
	// should always be successful
	throw new SuccessfulFuseException();
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
    }

    /**
     * This exists only for the sake of efficiency, to do a long jump back
     * up to the top of the fusion loop.  For some reason we only get
     * maximal fusion if we always consider fusing things from the very
     * top; fusing within the visitor doesn't quite do the right thing.
     */
    static class SuccessfulFuseException extends RuntimeException {
	public SuccessfulFuseException() { super(); }
	public SuccessfulFuseException(String str) { super(str); }
    }
}

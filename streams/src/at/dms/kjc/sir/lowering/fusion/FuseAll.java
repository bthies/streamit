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
    /**
     * If true, then we will give error message and exit if we don't
     * succeed in fusing down to 1 filter.  Otherwise we will just do
     * our best.
     */
    private boolean strict;

    private FuseAll(boolean strict) {
        this.strict = strict;
    }

    /**
     * Fuse everything we can in <str>.  Returns new fused segment,
     * which is also mutated in the stream graph.  If 'strict' is
     * true, then prints an error message and exits if it can't
     * succeed in fusing down to 1 filter; otherwise it just does its
     * best (i.e., with respect to feedbackloops).
     */
    public static SIRPipeline fuse(SIRStream str, boolean strict) {
        // try fusing toplevel separately since noone contains it
        SIRPipeline wrapper = SIRContainer.makeWrapper(str);
        wrapper.reclaimChildren();
        FuseAll fuseAll = new FuseAll(strict);
        boolean hasFused = true;
        while (hasFused) {
            try {
                assert wrapper.get(0).getParent() == wrapper;
                Lifter.lift(wrapper);
                if (wrapper.size()>1) {
                    wrapper = SIRContainer.makeWrapper(wrapper);
                }
                IterFactory.createFactory().createIter(wrapper.get(0)).accept(fuseAll);
                hasFused = false;
            } catch (SuccessfulFuseException e) {}
        }
        return wrapper;
    }

    /**
     * As above, with strict=true.
     */
    public static SIRPipeline fuse(SIRStream str) {
        return fuse(str, true);
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
        // if we had to fuse things down, then fail if we can't
        if (strict) {
            Utils.fail("Don't yet support fusion of feedback loops.");
        }
    }

    /**
     * POST-VISITS 
     */

    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
                                  SIRPipelineIter iter) {
        int elim = FusePipe.fuse(self);
        // now fusion fuses as much as possible, but might leave a
        // few filters (e.g., filereaders and filewriters) 
        if (elim > 0) {
            throw new SuccessfulFuseException();
        }
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
                                   SIRSplitJoinIter iter) {
        if (FuseSplit.isFusable(self)) {
            SIRStream result = FuseSplit.fuse(self);
            // should always be successful, given that it's fusable
            throw new SuccessfulFuseException();
        }
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

package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import java.util.*;

/**
 * This is a stream visitor that does nothing.
 */
public class EmptyStreamVisitor implements StreamVisitor {

    /**
     * This is called before all visits to a stream structure (Filter,
     * Pipeline, SplitJoin, FeedbackLoop)
     */
    public void preVisitStream(SIRStream self,
                               SIRIterator iter) {
    }


    /**
     * This is called after all visits to a stream structure (Filter,
     * Pipeline, SplitJoin, FeedbackLoop)
     */
    public void postVisitStream(SIRStream self,
                                SIRIterator iter) {
    }

    /**
     * PLAIN-VISITS 
     */
        
    /* visit a filter */
    public void visitFilter(SIRFilter self,
                            SIRFilterIter iter) {
        preVisitStream(self, iter);
        postVisitStream(self, iter);
    }

    /* visit a phased filter */
    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        preVisitStream(self, iter);
        postVisitStream(self, iter);
    }
  
    /**
     * PRE-VISITS 
     */
        
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
                                 SIRPipelineIter iter) {
        preVisitStream(self, iter);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
                                  SIRSplitJoinIter iter) {
        preVisitStream(self, iter);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
                                     SIRFeedbackLoopIter iter) {
        preVisitStream(self, iter);
    }

    /**
     * POST-VISITS 
     */
        
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
                                  SIRPipelineIter iter) {
        postVisitStream(self, iter);
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
                                   SIRSplitJoinIter iter) {
        postVisitStream(self, iter);
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
                                      SIRFeedbackLoopIter iter) {
        postVisitStream(self, iter);
    }

}

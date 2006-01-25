package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import java.util.List;

/**
 * This visitor is for visiting stream structures in the SIR.  It does
 * not visit statement-level constructs like SIRInitStatement,
 * SIRPushStatement, etc.
 *
 * The recursing process from one stream construct to another is
 * automatic--one does not need to write code to visit the children.
 */
public interface StreamVisitor {

    /**
     * PLAIN-VISITS 
     */
        
    /* visit a filter */
    void visitFilter(SIRFilter self,
                     SIRFilterIter iter);
  
    /* visit a phased filter */
    void visitPhasedFilter(SIRPhasedFilter self,
                           SIRPhasedFilterIter iter);

    /**
     * PRE-VISITS 
     */
        
    /* pre-visit a pipeline */
    void preVisitPipeline(SIRPipeline self,
                          SIRPipelineIter iter);

    /* pre-visit a splitjoin */
    void preVisitSplitJoin(SIRSplitJoin self,
                           SIRSplitJoinIter iter);

    /* pre-visit a feedbackloop */
    void preVisitFeedbackLoop(SIRFeedbackLoop self,
                              SIRFeedbackLoopIter iter);

    /**
     * POST-VISITS 
     */
        
    /* post-visit a pipeline */
    void postVisitPipeline(SIRPipeline self,
                           SIRPipelineIter iter);

    /* post-visit a splitjoin */
    void postVisitSplitJoin(SIRSplitJoin self,
                            SIRSplitJoinIter iter);

    /* post-visit a feedbackloop */
    void postVisitFeedbackLoop(SIRFeedbackLoop self,
                               SIRFeedbackLoopIter iter);
}

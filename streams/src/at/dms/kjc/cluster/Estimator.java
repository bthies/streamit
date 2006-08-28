
package at.dms.kjc.cluster;

import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;


/**
 * The class allows forcing of the recalculation of estimates of 
 * code and local variable size for the entire stream program.
 * Forcing is necessary since {@link CodeEstimate} caches 
 * old values.
 * @see CodeEstimate
 */

class Estimator implements StreamVisitor {

    //public Estimator() {}

    /**
     * Force recalculation of code and locals size.
     * @param str the top level stream 
     */

    public static void estimate(SIRStream str) {
        Estimator est = new Estimator();
        System.err.print("Estimating Code size of Filters...");
        IterFactory.createFactory().createIter(str).accept(est);
        System.err.println(" done.");
    }

    /**
     * Recalculate code and locals size for a filter
     */

    public void visitFilter(SIRFilter filter,
                            SIRFilterIter iter) { 

        int code, locals;
    
        CodeEstimate est = CodeEstimate.estimate(filter);
        code = est.getCodeSize();
        locals = est.getLocalsSize();

        //System.out.println("Estimator Filter: "+filter+" Code: "+code+" Locals: "+locals);
    
    }

    /**
     * Phased Filters are not supported!
     */

    public void visitPhasedFilter(SIRPhasedFilter self,
                                  SIRPhasedFilterIter iter) {
        // This is a stub; it'll get filled in once we figure out how phased
        // filters should actually work.
    }
    
    /**
     * PRE-VISITS 
     */
        
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {}
    
    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}
    
    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    
    /**
     * POST-VISITS 
     */
        
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {}
   
    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}


}



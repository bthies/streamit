
package at.dms.kjc.cluster;

import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import java.util.*;

class Estimator implements StreamVisitor {

    public Estimator() {}

    public static void estimate(SIRStream str) {
	Estimator est = new Estimator();
	System.out.print("Estimating Code size of Filters...");
	IterFactory.createFactory().createIter(str).accept(est);
	System.out.println("done.");
    }

    public void visitFilter(SIRFilter filter,
		     SIRFilterIter iter) { 

	int code, locals;
	
	CodeEstimate est = CodeEstimate.estimate(filter);
	code = est.getCodeSize();
	locals = est.getLocalsSize();

	//System.out.println("Estimator Filter: "+filter+" Code: "+code+" Locals: "+locals);
	
    }

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



package at.dms.kjc.sir.lowering.partition;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

public class SJToPipe implements StreamVisitor {

    private SJToPipe() {}

    /**
     * Lift everything we can in <str> and its children
     */
    public static void doit(SIRStream str) {
	IterFactory.createFactory().createIter(str).accept(new SJToPipe());
	Lifter.lift(str);
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
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRSplitJoinIter iter) {
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {
    }

    /**
     * POST-VISITS
     */
	    
    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRPipelineIter iter) {
	convertChildren(self);
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRSplitJoinIter iter) {
	convertChildren(self);
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRFeedbackLoopIter iter) {
	convertChildren(self);
    }

    private void convertChildren(SIRContainer cont) {
	for (int i=0; i<cont.size(); i++) {
	    SIRStream child = cont.get(i);
	    if (child instanceof SIRSplitJoin) {
		cont.replace(child, RefactorSplitJoin.convertToPipeline((SIRSplitJoin)child));
	    }
	}
    }
}

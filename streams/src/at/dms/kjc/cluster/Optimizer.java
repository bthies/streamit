
package at.dms.kjc.cluster;

//import at.dms.kjc.common.*;
//import at.dms.kjc.flatgraph.FlatNode;
//import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
//import at.dms.util.Utils;
//import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
//import at.dms.util.SIRPrinter;

import java.util.*;
//import java.io.*;

class Optimizer implements StreamVisitor {

    public Optimizer() {}

    public static void optimize(SIRStream str) {
	Optimizer est = new Optimizer();
	System.out.print("Optimizing filters...");
	IterFactory.createFactory().createIter(str).accept(est);
	System.out.println("done.");
    }

    public void visitFilter(SIRFilter filter,
		     SIRFilterIter iter) { 

	//unroll all methods
	//System.out.println("Optimizing "+filter+"...");

	for (int i = 0; i < filter.getMethods().length; i++) {
	    JMethodDeclaration method=filter.getMethods()[i];
	    if (!KjcOptions.nofieldprop) {
		Unroller unroller;
		do {
		    do {
			unroller = new Unroller(new Hashtable());
			method.accept(unroller);
		    } while(unroller.hasUnrolled());
		    
		    method.accept(new Propagator(new Hashtable()));
		    unroller = new Unroller(new Hashtable());
                    method.accept(unroller);

		} while(unroller.hasUnrolled());

		method.accept(new BlockFlattener());
		method.accept(new Propagator(new Hashtable()));
               
	    } else {
		filter.getMethods()[i].accept(new BlockFlattener());
	    }
	    
	    filter.getMethods()[i].accept(new VarDeclRaiser());
	}
           
	DeadCodeElimination.doit(filter);
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

package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
//import at.dms.compiler.*;


/**
 * RedundantReplacer.
 * $Id: LinearRedundancyReplacer.java,v 1.1 2003-02-24 18:05:12 aalamb Exp $
 **/
public class LinearRedundancyReplacer extends EmptyStreamVisitor implements Constants{
    /** The field that has all of the redundancy information necessary to create replacements. **/
    LinearRedundancyAnalyzer redundancyInformation;

    
    private LinearRedundancyReplacer(LinearRedundancyAnalyzer lra) {
	this.redundancyInformation = lra;
    }

    
    /** start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearRedundancyAnalyzer lra, SIRStream str) {
	LinearRedundancyReplacer replacer = new LinearRedundancyReplacer(lra);
	// pump the replacer through the stream graph.
	IterFactory.createIter(str).accept(replacer);
    }

    
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {makeReplacement(self, iter);}
    public void preVisitPipeline    (SIRPipeline self,
				     SIRPipelineIter iter){makeReplacement(self, iter);}
    public void preVisitSplitJoin   (SIRSplitJoin self,
				     SIRSplitJoinIter iter){makeReplacement(self, iter);}
    public void visitFilter         (SIRFilter self,
				     SIRFilterIter iter){makeReplacement(self, iter);}
    

    /**
     * Visit a pipeline, splitjoin or filter, replacing them with a new filter
     * that directly implements the linear representation. This only
     * occurs if the replace calculator says that this stream should be replaced.
     **/
    private void makeReplacement(SIRStream self, SIRIterator iter) {
	throw new RuntimeException("not yet implemented");
    }
}

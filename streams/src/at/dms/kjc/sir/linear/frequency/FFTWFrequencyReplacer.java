package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
import at.dms.compiler.*;


/**
 * A FrequencyReplacer replaces FIR filters (those calculating convolution sums,
 * peek=N, pop=1 push=1) of sufficient length with a conversion into the frequency
 * domain, multiplication, and convert the product back into the time domain.
 *
 * In so doing, this also increases the peek, pop and push rates to take advantage of
 * the frequency transformation
 * 
 * $Id: FFTWFrequencyReplacer.java,v 1.1 2002-10-28 15:22:00 aalamb Exp $
 **/
public class FrequencyReplacer extends EmptyStreamVisitor implements Constants{
    /** the linear analyzier which keeps mappings from filters-->linear representations**/
    LinearAnalyzer linearityInformation;
    
    private FrequencyReplacer(LinearAnalyzer lfa) {
	if (lfa == null){
	    throw new IllegalArgumentException("Null linear filter analyzer!");
	}

	this.linearityInformation = lfa;
    }

    /** start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearAnalyzer lfa, SIRStream str) {
	// make a new replacer with the information contained in the analyzer
	FrequencyReplacer replacer = new FrequencyReplacer(lfa);
	// pump the replacer through the stream graph.
	IterFactory.createIter(str).accept(replacer);
    }

    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter){}
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter){}
    public void visitFilter(SIRFilter self, SIRFilterIter iter){}

}

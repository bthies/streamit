package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;


/**
 * The LinearAnalyzer visits all of the Filter definitions in
 * a StreamIT program. It determines which filters calculate linear
 * functions of their inputs, and for those that do, it keeps a mapping from
 * the filter name to the filter's matrix representation.
 *
 * $Id: LinearAnalyzer.java,v 1.1 2002-09-13 18:06:56 aalamb Exp $
 **/
public class LinearAnalyzer extends EmptyStreamVisitor {
    /** Mapping from filters to linear representations. never would have guessed that, would you? **/
    HashMap filtersToLinearRepresentation;

    // counters to keep track of how many of what type of stream constructs we have seen.
    int filtersSeen        = 0;
    int pipelinesSeen      = 0;
    int splitJoinsSeen     = 0;
    int feedbackLoopsSeen  = 0;

    
    
    /** use findLinearFilters to instantiate a LinearAnalyzer **/
    private LinearAnalyzer() {
	this.filtersToLinearRepresentation = new HashMap();
	checkRep();
    }

    /** Returns true if the specified filter has a linear representation that we have found. **/
    public boolean hasLinearRepresentation(SIRStream stream) {
	checkRep();
	// just check to see if the hash set has a mapping to something other than null.
	return (this.filtersToLinearRepresentation.get(stream) != null);
    }

    /**
     * returns the mapping from stream to linear representation that we have. Returns
     * null if we do not have a mapping.
     **/
    public LinearFilterRepresentation getLinearRepresentation(SIRStream stream) {
	checkRep();
	return (LinearFilterRepresentation)this.filtersToLinearRepresentation.get(stream);
    }
    
    /**
     * Main entry point -- searches the passed stream for
     * linear filters and calculates their associated matricies.
     *
     * If the debug flag is set, then we print a lot of debugging information
     **/
    public static LinearAnalyzer findLinearFilters(SIRStream str, boolean debug) {
	// set up the printer to either print or not depending on the debug flag
	LinearPrinter.setOutput(debug);
	LinearPrinter.println("aal--In linear filter visitor");
	LinearAnalyzer lfa = new LinearAnalyzer();
	IterFactory.createIter(str).accept(lfa);
	// generate a report and print it.
	LinearPrinter.println(lfa.generateReport());
	return lfa;
    }
    

    /** More or less get a callback for each stram **/
    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
	this.filtersSeen++;
	LinearPrinter.println("Visiting " + "(" + self + ")");

	// set up the visitor that will actually collect the data
	int peekRate = extractInteger(self.getPeek());
	int pushRate = extractInteger(self.getPush());
	LinearPrinter.println("  Peek rate: " + peekRate);
	LinearPrinter.println("  Push rate: " + pushRate);
	// if we have a peek or push rate of zero, this isn't a linear filter that we care about,
	// so only try and visit the filter if both are non-zero
	if ((peekRate != 0) && (pushRate != 0)) {
	    LinearFilterVisitor theVisitor = new LinearFilterVisitor(self.getIdent(),
								     peekRate, pushRate);

	    // pump the visitor through the work function
	    // (we might need to send it thought the init function as well so that
	    //  we can determine the initial values of fields. However, I think that
	    //  field prop is supposed to take care of this.)
	    self.getWork().accept(theVisitor);

	    // print out the results of pumping the visitor
	    if (theVisitor.computesLinearFunction()) {
		LinearPrinter.println("Linear filter found: " + self +
				   "\n-->Matrix:\n" + theVisitor.getMatrixRepresentation() +
				   "\n-->Constant Vector:\n" + theVisitor.getConstantVector());
		// add a mapping from the filter to its linear form.
		this.filtersToLinearRepresentation.put(self,
						       theVisitor.getLinearRepresentation());
	    }

	    
		
	} else {
	    LinearPrinter.println("  " + self.getIdent() + " is source/sink.");
	}

	// check the rep invariant
	checkRep();
    }

    /**
     * Eventually, this method will handle combining feedback loops (possibly).
     * For now it just keeps track of the number of times we have seen feedback loops.
     **/
    public void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {
	this.feedbackLoopsSeen++;
    }
    /**
     * Eventually, this method will handle combining pipelines.
     * For now it just keeps track of the number of times we have seen pipelines.
     **/
    public void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {
	this.pipelinesSeen++;
    }
    /**
     * Eventually, this method will handle combining splitjoins.
     * For now it just keeps track of the number of times we have seen splitjoins.
     **/
    public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {
	this.splitJoinsSeen++;
    }
	

    /**
     * Make a nice report on the number of stream constructs processed
     * and the number of things that we found linear forms for.
     **/
    private String generateReport() {
	String rpt;
	rpt = "Linearity Report\n";
	rpt += "---------------------\n";
	rpt += "Filters:       " + makePctString(filtersToLinearRepresentation.keySet().size(),
						 this.filtersSeen);
	rpt += "Pipelines:     " + this.pipelinesSeen + "\n";
	rpt += "SplitJoins:    " + this.splitJoinsSeen + "\n";
	rpt += "FeedbackLoops: " + this.feedbackLoopsSeen + "\n";
	rpt += "---------------------\n";
	return rpt;
    }
    /** simple utility for report generation. makes "top/bottom (pct) string **/
    private String makePctString(int top, int bottom) {
	float pct;
	// figure out percent to two decimal places
	pct = ((float)top)/((float)bottom);
	pct *= 10000;
	pct = Math.round(pct);
	pct /= 100;
	return top + "/" + bottom + " (" + pct + "%)\n";
    }
    
    /** extract the actual value from a JExpression that is actually a literal... **/
    private static int extractInteger(JExpression expr) {
	if (expr == null) {throw new RuntimeException("null peek rate");}
	if (!(expr instanceof JIntLiteral)) {throw new RuntimeException("non integer literal peek rate...");}
	JIntLiteral literal = (JIntLiteral)expr;
	return literal.intValue();
    }

    private void checkRep() {
	// make sure that all keys in FiltersToMatricies are strings, and that all
	// values are LinearForms.
	Iterator keyIter = this.filtersToLinearRepresentation.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object o = keyIter.next();
	    if (!(o instanceof SIRStream)) {throw new RuntimeException("Non stream key in LinearAnalyzer");}
	    SIRStream key = (SIRStream)o;
	    Object val = this.filtersToLinearRepresentation.get(key);
	    if (val == null) {throw new RuntimeException("Null value found in LinearAnalyzer");}
	    if (!(val instanceof LinearFilterRepresentation)) {
		throw new RuntimeException("Non LinearFilterRepresentation found in LinearAnalyzer");
	    }
	}
    }
}








// ----------------------------------------
// Code for visitor class.
// ----------------------------------------



























    

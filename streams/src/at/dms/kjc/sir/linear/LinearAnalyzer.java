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
 * $Id: LinearAnalyzer.java,v 1.3 2002-09-17 18:27:18 aalamb Exp $
 **/
public class LinearAnalyzer extends EmptyStreamVisitor {
    /** Mapping from filters to linear representations. never would have guessed that, would you? **/
    HashMap filtersToLinearRepresentation;

    // counters to keep track of how many of what type of stream constructs we have seen.
    int filtersSeen        = 0;
    int pipelinesSeen      = 0;
    int splitJoinsSeen     = 0;
    int feedbackLoopsSeen  = 0;

    
    private LinearAnalyzer() {
	this.filtersToLinearRepresentation = new HashMap();
	checkRep();
    }

    ///////// Accessors
    
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
    

    /////////////////////////////////////
    ///////////////////// Visitor methods
    /////////////////////////////////////
    
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
	if ((peekRate == 0) || (pushRate == 0)) {
	    LinearPrinter.println("  " + self.getIdent() + " is source/sink.");
	    return;
	}

	LinearFilterVisitor theVisitor = new LinearFilterVisitor(self.getIdent(),
								 peekRate, pushRate);
	
	// pump the visitor through the work function
	// (we might need to send it thought the init function as well so that
	//  we can determine the initial values of fields. However, I think that
	//  field prop is supposed to take care of this.)
	self.getWork().accept(theVisitor);
	
	// print out the results of pumping the visitor
	// incidentally, this output is parsed by some perl scripts to verify results,
	// so it probably shouldn't be changed.
	if (theVisitor.computesLinearFunction()) {
	    LinearPrinter.println("Linear filter found: " + self +
				  "\n-->Matrix:\n" + theVisitor.getMatrixRepresentation() +
				  "\n-->Constant Vector:\n" + theVisitor.getConstantVector());
	    // add a mapping from the filter to its linear form.
	    this.filtersToLinearRepresentation.put(self,
						   theVisitor.getLinearRepresentation());
	} 
	// check that we have not violated the rep invariant
	checkRep();
    }


    
    //void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    //void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}


    //void preVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {}
    /**
     * We visit a pipeline after all sub-streams have been visited.
     * If all of the sub components have linear forms, then we will
     * Try and combine those linear forms into a linear form that
     * represents exactly what the pipeline is doing.
     **/
    public void postVisitPipeline(SIRPipeline self, SIRPipelineIter iter) {
	this.pipelinesSeen++;
	LinearPrinter.println("Visiting pipeline: " + "(" + self + ")");
	
	Iterator kidIter = self.getChildren().iterator();
	LinearPrinter.println("Children: ");
	while(kidIter.hasNext()) {
	    SIRStream currentKid = (SIRStream)kidIter.next();
	    String linearString = (this.hasLinearRepresentation(currentKid)) ? "linear" : "non-linear";
	    LinearPrinter.println("  " + currentKid + "(" + linearString + ")");
	}

	// go down the list of children in the pipeline, trying to create 
	// a (possibly) gigantic matrix representation for what is going on.
	kidIter = self.getChildren().iterator();
	// if we don't have any children, we are done
	if (!kidIter.hasNext()) {
	    LinearPrinter.warn("Pipeline: " + self + " has no children ?!?!?!?!");
	    return;
	}

	// grab the first child. If we don't have a linear rep, we are done.
	SIRStream firstKid = (SIRStream)kidIter.next();
	if (!this.hasLinearRepresentation(firstKid)) {return;}
	// get the matrix/vector corresponding to this rep.
	LinearFilterRepresentation overallRep = this.getLinearRepresentation(firstKid);
	
	// now, we should be good, go through the rest of the children, trying to
	// tack on their linear reps to the overall one. If we hit a combination that
	// we can't do, then we set overallRep to null
	while(kidIter.hasNext() && (overallRep != null)) {
	    SIRStream currentKid = (SIRStream)kidIter.next();
	    // see if we have a representation for the current pipeline child
	    if (this.hasLinearRepresentation(currentKid)) {
		LinearFilterRepresentation currentRep = this.getLinearRepresentation(currentKid);
		// calculate what transformations we need to do on the two representations
		// in order to allow them to combine.
		LinearTransform pipelineTransform;
		pipelineTransform = LinearTransformPipeline.calculate(overallRep, currentRep);
		// actually try to do the tranformation
		try {
		    overallRep = pipelineTransform.transform();
		} catch (NoTransformPossibleException e) {
		    LinearPrinter.warn("Can't combine representations: " + overallRep + currentRep +
				       " reason: " + e.getMessage());
		    overallRep = null;
		}
	    } else {
		// we didn't know anything about this filter, so just give up
		overallRep = null;
	    }
	}

	
	// if we have an overall rep, then we should add a mapping from this pipeline to
	// that rep, and return.
	if (overallRep != null) {
	    LinearPrinter.println("Linear pipeline found: " + self +
				  "\n-->Matrix:\n" + overallRep.getA() +
				  "\n-->Constant Vector:\n" + overallRep.getb());
	    // add a mapping from the pipeline to its linear form.
	    this.filtersToLinearRepresentation.put(self, overallRep);
	    checkRep(); // to make sure we didn't shoot ourselves somehow
	}
	return;
    }

    //public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}
    //public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}
    
    











	



     ///////////////////////
     // Utility Methods, etc.
     ///////////////////////     

    /**
     * Make a nice report on the number of stream constructs processed
     * and the number of things that we found linear forms for.
     **/
    private String generateReport() {
	String rpt;
	rpt = "Linearity Report\n";
	rpt += "---------------------\n";
	rpt += "Filters:       " + makePctString(getFilterReps(),
						 this.filtersSeen);
	rpt += "Pipelines:     " + makePctString(getPipelineReps(),
						 this.pipelinesSeen);
	rpt += "SplitJoins:    " + makePctString(getSplitJoinReps(),
						 this.splitJoinsSeen);
	rpt += "FeedbackLoops: " + makePctString(getFeedbackLoopReps(),
						 this.feedbackLoopsSeen);
	rpt += "---------------------\n";
	return rpt;
    }    
    /** simple utility for report generation. makes "top/bottom (pct) string **/
    private String makePctString(int top, int bottom) {
	float pct;
	if (bottom == 0) {
	    pct = 0;
	} else {
	    // figure out percent to two decimal places
	    pct = ((float)top)/((float)bottom);
	    pct *= 10000;
	    pct = Math.round(pct);
	    pct /= 100;
	}
	return top + "/" + bottom + " (" + pct + "%)\n";
    }

    /** returns the number of filters that we have linear reps for. **/
    private int getFilterReps() {
	int filterCount = 0;
	Iterator keyIter = this.filtersToLinearRepresentation.keySet().iterator();
	while(keyIter.hasNext()) {
	    if (keyIter.next() instanceof SIRFilter) {filterCount++;}
	}
	return filterCount;
    }
    /** returns the number of pipelines that we have linear reps for. **/
    private int getPipelineReps() {
	int count = 0;
	Iterator keyIter = this.filtersToLinearRepresentation.keySet().iterator();
	while(keyIter.hasNext()) {
	    if (keyIter.next() instanceof SIRPipeline) {count++;}
	}
	return count;
    }
    /** returns the number of splitjoins that we have linear reps for. **/
    private int getSplitJoinReps() {
	int count = 0;
	Iterator keyIter = this.filtersToLinearRepresentation.keySet().iterator();
	while(keyIter.hasNext()) {
	    if (keyIter.next() instanceof SIRSplitJoin) {count++;}
	}
	return count;
    }
    /** returns the number of feedbackloops that we have linear reps for. **/
    private int getFeedbackLoopReps() {
	int count = 0;
	Iterator keyIter = this.filtersToLinearRepresentation.keySet().iterator();
	while(keyIter.hasNext()) {
	    if (keyIter.next() instanceof SIRFeedbackLoop) {count++;}
	}
	return count;
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
	    if (o == null) {throw new RuntimeException("Null key in LinearAnalyzer.");}
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



























    

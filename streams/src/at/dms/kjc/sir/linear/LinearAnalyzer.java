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
 * $Id: LinearAnalyzer.java,v 1.9 2002-10-23 21:12:44 aalamb Exp $
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
    
    /** removes the specified SIRStream from the linear represention list. **/
    public void removeLinearRepresentation(SIRStream stream) {
	checkRep();
	if (!this.hasLinearRepresentation(stream)) {
	    throw new IllegalArgumentException("Don't know anything about " + stream);
	}
	this.filtersToLinearRepresentation.remove(stream);
    }
    /** adds a mapping from sir stream to linear filter rep. **/
    public void addLinearRepresentation(SIRStream key, LinearFilterRepresentation rep) {
	checkRep();
	if ((key == null) || (rep == null)) {
	    throw new IllegalArgumentException("null in add linear rep");
	}
	if (this.filtersToLinearRepresentation.containsKey(key)) {
	    throw new IllegalArgumentException("tried to add key mapping.");
	}
	if (this.filtersToLinearRepresentation.containsValue(rep)) {
	    throw new IllegalArgumentException("tried to add rep second time.");
	}
	this.filtersToLinearRepresentation.put(key,rep);
	checkRep();
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
	int popRate  = extractInteger(self.getPop());
	LinearPrinter.println("  Peek rate: " + peekRate);
	LinearPrinter.println("  Push rate: " + pushRate);
	LinearPrinter.println("  Pop rate: "  + popRate);
	// if we have a peek or push rate of zero, this isn't a linear filter that we care about,
	// so only try and visit the filter if both are non-zero
	if ((peekRate == 0) || (pushRate == 0)) {
	    LinearPrinter.println("  " + self.getIdent() + " is source/sink.");
	    return;
	}

	LinearFilterVisitor theVisitor = new LinearFilterVisitor(self.getIdent(),
								 peekRate, pushRate, popRate);
	
	// pump the visitor through the work function
	// (we might need to send it thought the init function as well so that
	//  we can determine the initial values of fields. However, I think that
	//  field prop is supposed to take care of this.)
	self.getWork().accept(theVisitor);
	
	// print out the results of pumping the visitor
	// incidentally, this output is parsed by some perl scripts to verify results,
	// so it probably shouldn't be changed.
	if (theVisitor.computesLinearFunction()) {
	    // since printing the matrices takes so long, if debugging is not on,
	    // don't even generate the string.
	    if (LinearPrinter.getOutput()) {
		LinearPrinter.println("Linear filter found: " + self +
				      "\n-->Matrix:\n" + theVisitor.getMatrixRepresentation() +
				      "\n-->Constant Vector:\n" + theVisitor.getConstantVector());
	    }
	    // add a mapping from the filter to its linear form.
	    this.filtersToLinearRepresentation.put(self,
						   theVisitor.getLinearRepresentation());
	} 
	// check that we have not violated the rep invariant
	checkRep();
    }


    
    //void preVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {}
    public void postVisitFeedbackLoop(SIRFeedbackLoop self, SIRFeedbackLoopIter iter) {
	this.feedbackLoopsSeen++;
    }


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
		    LinearPrinter.println("  can't combine representations: " + overallRep + currentRep +
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
	    // check for debugging mode (because assembling the string takes a loooong time)
	    if (LinearPrinter.getOutput()) {
		LinearPrinter.println("Linear pipeline found: " + self +
				      "\n-->Matrix:\n" + overallRep.getA() +
				      "\n-->Constant Vector:\n" + overallRep.getb());
	    }
	    // add a mapping from the pipeline to its linear form.
	    this.filtersToLinearRepresentation.put(self, overallRep);
	    checkRep(); // to make sure we didn't shoot ourselves somehow
	}
	return;
    }

    //public void preVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {}

    /**
     * Visits a split join after all of its childern have been visited. If we have
     * linear representations for all the children, then we can try to combine
     * the entre split join construct into a single linear representation.
     **/
    public void postVisitSplitJoin(SIRSplitJoin self, SIRSplitJoinIter iter) {
	this.splitJoinsSeen++;

	SIRSplitter splitter = self.getSplitter();
	SIRJoiner   joiner   = self.getJoiner();
	int[] splitWeights   = splitter.getWeights();
	int[] joinWeights    = joiner.getWeights();
	
	LinearPrinter.println("Visiting SplitJoin (" + self + ")");
	LinearPrinter.println(" splitter: " + splitter.getType() +
			      makeStringFromIntArray(splitWeights));
	LinearPrinter.println(" joiner: " + joiner.getType() + 
			      makeStringFromIntArray(joinWeights));

	Iterator childIter = self.getChildren().iterator();
	while(childIter.hasNext()) {
	    SIROperator currentChild = (SIROperator)childIter.next();
	    if (currentChild instanceof SIRStream) {
		boolean childLinearity = this.hasLinearRepresentation((SIRStream)currentChild);
		LinearPrinter.println(" child: " + currentChild + "(" +
				      (childLinearity ? "linear" : "non-linear") +
				      ")");
	    } else {
		LinearPrinter.println(" child: " + currentChild);
	    }
	}

	// first things first -- for each child we need a linear rep, so
	// iterate through all children and create a parallel list
	// of linear representations. Remember that the first two elements
	// are the splitter and the joiner so we get rid of them right off the
	// bat.
	LinkedList repList = new LinkedList();
	List childList = self.getChildren();
	childList.remove(0); childList.remove(childList.size()-1); // remove the splitter and the joiner
	childIter = childList.iterator();
	while (childIter.hasNext()) {
	    SIRStream currentChild = (SIRStream)childIter.next();
	    boolean childLinearity = this.hasLinearRepresentation(currentChild);
	    // if we have a linear rep, add it to our list, otherwise we are done
	    if (childLinearity) {
		repList.add(this.getLinearRepresentation(currentChild));
	    } else {
		LinearPrinter.println(" aborting split join combination  -- non-linear child detected.");
		return;
	    }
	}	    
				  
	// the transform that we will set based on the splitter type.
	LinearTransform sjTransform = null;
	
	// dispatch based on the splitter 
	if (splitter.getType() == SIRSplitType.DUPLICATE) {
	    // process duplicate splitter
	    sjTransform = LinearTransformSplitJoin.calculateDuplicate(repList, joinWeights);
	} else if ((splitter.getType() == SIRSplitType.WEIGHTED_RR) ||
		   (splitter.getType() == SIRSplitType.ROUND_ROBIN)) {
	    // process roundrobin splitter
	    sjTransform = LinearTransformSplitJoin.calculateRoundRobin(repList, joinWeights, splitWeights);
	} else if (splitter.getType() == SIRSplitType.NULL) {
	    LinearPrinter.println(" Ignoring null splitter.");
	} else {
	    LinearPrinter.println(" Ignoring unknown splitter type: " + splitter.getType());
	}

	// if we don't have a transform, we are done and return here
	if (sjTransform == null) {  
	    LinearPrinter.println(" no transform found. stop");
	    return;
	}

	// do the actual transform
	LinearPrinter.println(" performing transform.");
	LinearFilterRepresentation newRep = null;
	try {
	    newRep = sjTransform.transform();
	} catch (NoTransformPossibleException e) {
	    LinearPrinter.println(" error performing transform: " + e.getMessage());
	}

	// If the new rep is null, some error occured and we are done
	if (newRep == null) {
	    LinearPrinter.println(" transform unsuccessful. stop.");
	    return;
	}
	
	LinearPrinter.println(" transform successful.");
	// check for debugging so we don't waste time producing output if not needed
	if (LinearPrinter.getOutput()) {
	    LinearPrinter.println("Linear splitjoin found: " + self +
				  "\n-->Matrix:\n" + newRep.getA() +
				  "\n-->Constant Vector:\n" + newRep.getb());
	}
	// add a mapping from this split join to the new linear representation
	this.filtersToLinearRepresentation.put(self, newRep);
    }
    
    


    /** returns a string like "(a[0], a[1], ... a[N-1])". **/
    private String makeStringFromIntArray(int[] arr) {
	// make a nice string with all the weights in it.
	String weightString = "   (";
	for (int i=0; i<arr.length; i++) {
	    weightString += arr[i] + ",";
	}
	weightString = weightString.substring(0, weightString.length()-1) + ")";
	return weightString;
    }
	







	



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



























    

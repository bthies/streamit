package at.dms.kjc.sir.linear;

import java.util.*;
//import at.dms.kjc.*;
//mport at.dms.kjc.sir.*;
//import at.dms.kjc.sir.linear.*;
//import at.dms.kjc.iterator.*;

/**
 * Contains the code for merging all the filters from a split join
 * into a single monolithic matrix.
 * $Id: LinearTransformSplitJoin.java,v 1.2 2002-09-26 17:06:30 aalamb Exp $
 **/
public class LinearTransformSplitJoin extends LinearTransform{
    LinearFilterRepresentation[] linearRepresentations;
    int[] expansionFactors;
    int[] combinationWeights;
    
    /**
     * Create one of these transformations with the appropriate pieces.
     * Doesn't copy the arrays, just copies the pointers.
     **/
    private LinearTransformSplitJoin(LinearFilterRepresentation[] lfr,
				     int[] expandFact,
				     int[] combWeight) {
	this.linearRepresentations = lfr;
	this.expansionFactors = expandFact;
	this.combinationWeights = combWeight;
    }

    /**
     * Implement the actual transformation. This involves expanding all of the
     * representations by the specified factors and then copying the columns
     * of the expanded matricies into a new linear transform in the correct
     * order.
     **/
    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
	int filterCount = linearRepresentations.length;
	LinearPrinter.println(" preparing to combine splitjoin of " +
			      filterCount +
			      " filters");
	LinearPrinter.println("  (expansion)(RR join weight) factors:");
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("   " +
				  "(" + this.expansionFactors[i] + ")" +
				  "(" + this.combinationWeights[i] + ")");
	}
	
	// do the expansion
	LinearFilterRepresentation[] expandedReps = new LinearFilterRepresentation[filterCount];
	int totalCols = 0;
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("  expanding filter by " + this.expansionFactors[i]);
	    expandedReps[i] = this.linearRepresentations[i].expand(this.expansionFactors[i]);
	    totalCols += expandedReps[i].getPushCount();
	}

	// figure how how many columns the "stride" is (eg the sum of the weights)
	int strideLength = 0;
	for (int i=0; i<filterCount; i++) {
	    strideLength += this.combinationWeights[i];
	}

	// now, create a new matrix and vector that have the appropriate size:
	// cols = the sum of the number of columns in the expanded representations
	// rows = (all expanded matrices have the same number of rows)
	FilterMatrix expandedA = new FilterMatrix(expandedReps[0].getPeekCount(), totalCols);
	FilterVector expandedb = new FilterVector(totalCols);

	// now, copy the cols of the matrices (and vectors) into the expanded versions
	// for each expanded matrix, copy joinWeight[i] cols into the new matrix and vector
	// at an offset that makes the output work out correctly. See paper.
	int startOffset = 0;
	for (int i=0; i<filterCount; i++) {
	    // figure out how many groups of joinWeight cols that we have
	    int numGroups = expandedReps[i].getPushCount() / this.combinationWeights[i]; // total # of groups
	    for (int j=0; j<numGroups; j++) {
		// figure out offset into expanded A to copy the columns
		int currentOffset = startOffset + j*strideLength;
		// the offset into the current source matrix is j*joinWeights[i]
		// the number of columns that we are copying is combination weights[i]
		expandedA.copyColumnsAt(currentOffset, expandedReps[i].getA(),
					j*this.combinationWeights[i],
					this.combinationWeights[i]);
		expandedb.copyColumnsAt(currentOffset, expandedReps[i].getb(),
					j*this.combinationWeights[i],
					this.combinationWeights[i]);
					
	    }
	    // update start of the offset for the next expanded rep. 
	    startOffset += this.combinationWeights[i];
	}

	// now, return a new LinearRepresentation that represents the transformed
	// splitjoin. (remember peek=pop)
	return new LinearFilterRepresentation(expandedA, expandedb, expandedA.getRows());
    }


    /**
     * Parses a List of LinearFilter representations into an array of
     * LinearFilterRepresenetations checking for peek=pop rates. If peek!= pop
     * for any of the filters, then we return null. Else we return an array
     * of LinearFilterRepresentation.
     **/
    public static LinearFilterRepresentation[] parseRepList(List representationList) {
	// create an array of LinearFilterRepresentations, and check that peek
	// is the same as pop for all filters.
	LinearFilterRepresentation[] filterReps;
	filterReps = new LinearFilterRepresentation[representationList.size()];

	// for each rep, check the peek=pop condition and grab peek rates
	Iterator repIter = representationList.iterator();
	int currentIndex = 0;
	while(repIter.hasNext()) {
	    LinearFilterRepresentation currentRep = (LinearFilterRepresentation)repIter.next();
	    if (currentRep.getPopCount() != currentRep.getPeekCount()) {
		return null;
	    } else {
		filterReps[currentIndex] = currentRep;
		currentIndex++;
	    }
	}
	LinearPrinter.println("  all filters have peek=pop");
	return filterReps;
    }

    
    /**
     * Calculate the necessary information to combine the splitjoin when the splitter is
     * a duplicate. See the documentation (eg what will be my thesis) for an explanation
     * of what is going on and why the expansion factors are chosen the way that they are.
     *
     * The input is a List of LinearFilterRepresentations and a matching array of joiner
     * weights.
     **/
    public static LinearTransform calculateDuplicate(List representationList,
						     int[] joinerWeights) {
	LinearPrinter.println(" calculating splitjoin transform with duplicate splitter.");
	if (representationList.size() != joinerWeights.length) {
	    throw new IllegalArgumentException("different numbers of reps and weights " +
					       "while transforming splitjoin");
	}

	// calculate the rep list from the passed in list
	LinearFilterRepresentation[] filterReps = parseRepList(representationList);
	if (filterReps == null) { return new LinearTransformNull("Peek didn't match pop rate."); }
	// do the actual work
	return calculateDuplicate(filterReps, joinerWeights);
    }

    /** the function that does the actual work. **/
    private static LinearTransform calculateDuplicate(LinearFilterRepresentation[] filterReps,
						      int[] joinerWeights) {
	int filterCount = filterReps.length;

	// find the peek rates from the filter reps
	int peekRates[] = new int[filterCount];
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("calculating peekrate for filter: " + i + "\n" + filterReps[i]);
	    peekRates[i] = filterReps[i].getPeekCount();
	}

	// calculate the expansion factors needed to match the peek rates of the filters
	// the factors are:fact(i) = lcm(forall [peek(j)]) / peekRate(i)
	int lcmOfPeekRates = lcm(peekRates);
	int rateMatchFactors[] = new int[filterCount];
	int[] newPushRates = new int[filterCount];
	for (int i=0; i<filterCount; i++) {
	    rateMatchFactors[i] = lcmOfPeekRates/filterReps[i].getPeekCount();
	    newPushRates[i] = filterReps[i].getPushCount() * rateMatchFactors[i];
	    LinearPrinter.println("  filter " + i + ":\n" +
				  "   rate match factor: " + rateMatchFactors[i] + "\n" +
				  "   new push rate (eg #cols): " + newPushRates[i]);
	}
	LinearPrinter.println("  lcm of peek rates: " + lcmOfPeekRates); 
	
	// calculate the overall additional expansion necessary to make the
	// # cols match a integer multiple of the round robin weights.
	// eg lcm(cols(A[0]), joinerWeights[0])
	int expandFactor = lcm(newPushRates[0], joinerWeights[0]);
	int filterExpandFactor = expandFactor / newPushRates[0];
	int joinerExpandFactor = expandFactor / joinerWeights[0];
	LinearPrinter.println("  using newRate[0] = " + newPushRates[0] + " and w[0] = " + joinerWeights[0]);
	LinearPrinter.println("  calculated common expansion factor: " + expandFactor);
	LinearPrinter.println("  calculated filter expansion factor: " + filterExpandFactor);
	LinearPrinter.println("  calculated joiner expansion factor: " + joinerExpandFactor);
	// now, make sure that that expansion factor works for all filters
	// if it doesn't that means that the splitjoin is unschedulable.
	for (int i=0; i<filterCount; i++) {
	    int currentExpandFactor = lcm(newPushRates[i], joinerWeights[i]); 
	    if ((currentExpandFactor / newPushRates[i]) != filterExpandFactor) {
		return new LinearTransformNull("Split join is unschedulable! 1:(filter expansion factors don't align)");
	    }
	    if ((currentExpandFactor / joinerWeights[i]) != joinerExpandFactor) {
		return new LinearTransformNull("Split join is unschedulable! 2:(joiner expansion factors don't align)");
	    }
	}

	// calculate the overall expansion rates for the filters and for the joiners
	int[] overallFilterWeights = new int[filterCount];
	int[] overallJoinerWeights = new int[filterCount];
	for (int i=0; i<filterCount; i++) {
	    overallFilterWeights[i] = rateMatchFactors[i] * filterExpandFactor;
	    overallJoinerWeights[i] = joinerWeights[i] * joinerExpandFactor;
	    LinearPrinter.println("  filter " + i + ":\n" +
				  "   rateMatchFactor: " + rateMatchFactors[i] + "\n" +
				  "   new push rate: " + newPushRates[i] + "\n" + 
				  "   overall filter weight: " + overallFilterWeights[i] + "\n" +
				  "   overall joiner weight: " + overallJoinerWeights[i]);
	}
	
	// check that once the joiner weights have been taken into
	// account we still have equal peek rates
	int overallPeekRate = filterReps[0].getPeekCount() * overallFilterWeights[0];
	for (int i=1; i<filterCount; i++) {
	    if ((filterReps[i].getPeekCount() * overallFilterWeights[i]) != overallPeekRate) {
		LinearPrinter.println(" After expansion, peek rates don't match up ==> unschedulable graph");
		return new LinearTransformNull("SplitJoin is unschedulable! 3: after expansion, peek rates don't match");
	    }
	}

	// finally, if we get to here, we have the expansion factors that we need, and
	// we can pass that information into a new LinearTransform object
	return new LinearTransformSplitJoin(filterReps,
					    overallFilterWeights,
					    overallJoinerWeights);
    }

    /**
     * Calculate the necessary information to combine the splitjoin when the splitter is
     * a roundrobin.
     * The basic idea will be to transform the linear reps that we have coming into this 
     * split join such that they describe the equivaled computation using a duplicate
     * splitter. We will do so by expandind the filter reps by inserting rows of
     * zeros in the appropriate places to match the number of rows.
     **/
    public static LinearTransform calculateRoundRobin(List representationList,
					       int[] joinerWeights,
					       int[] splitterWeights) {
       
	LinearPrinter.println(" calculating splitjoin transform with roundrobin splitter.");
	if (representationList.size() != splitterWeights.length) {
	    throw new IllegalArgumentException("different numbers of reps and splitter weights " +
					       "while transforming splitjoin");
	}

	// calculate the rep list from the passed in list
	LinearFilterRepresentation[] filterReps = parseRepList(representationList);
	if (filterReps == null) { return new LinearTransformNull("Peek didn't match pop rate."); }
	int filterCount = filterReps.length;

	// first, we are going to figure out how many times the roundrobin splitter
	// fires in the steady state by determining the lcm of the weight and rows
	// of each A  divided by weightsand then the overall Lcm of that.
	int[] splitterFiringLcms = new int[filterCount];
	for (int i=0; i<filterCount; i++) {
	    int rows = filterReps[i].getPeekCount();
	    int weight = splitterWeights[i];
	    int currentLcm = lcm(rows, weight); // number of items that need to be produced in the steady state
	    // number of times splitter needs to be fired to produce the correct number of outputs
	    int currentSplitterFirings = currentLcm / weight;
	    splitterFiringLcms[i] = currentSplitterFirings;
	    
	    LinearPrinter.println("  lcm of filter " + i + "'s rows (" + rows +
				  ") and weight(" + weight + ") = " + lcm(weight, rows));
	    LinearPrinter.println("  splitter firings: " + splitterFiringLcms[i]);
	}
	int splitterFirings = lcm(splitterFiringLcms);
	LinearPrinter.println("  overall splitter firings: " + splitterFirings);

	// now, figure out the sum of the splitter weights (eg the total number
	// of rows (possibly off by an integer factor) that we would like
	// each of the filter reps to end up having
	int splitterSum = 0;
	for (int i=0; i<filterCount; i++) {
	    splitterSum += splitterWeights[i];
	}
	LinearPrinter.println("  total splitter sum: " + splitterSum);

	// first expand all reps by a factor of splitterFirings. 
	// then, we know that each filter can be expanded into splitterSum * currentRows new rows.
	// with (splitterSum - splitterWeights[i])*rows of zeros
	LinearFilterRepresentation[] duplicateReps = new LinearFilterRepresentation[filterCount];
	int currentOffset = 0;
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("  expanding filter: " + i);
	    LinearFilterRepresentation expandedRep;
	    // the number of items the new filter needs to consume
	    int totalConsumption = (splitterFirings * splitterWeights[i]);
	    LinearPrinter.println("   filter needs to consume " + totalConsumption + " item");
	    // the total number of times that a filter needs to execute (eg how much to expand it by)
	    // note this is always going to be an integer due to the way we calculated splitterFirings
	    int filterExecutions = totalConsumption / filterReps[i].getPeekCount();
	    LinearPrinter.println("   filter needs to execute " + filterExecutions + " times");
	    if ((totalConsumption % filterReps[i].getPeekCount()) != 0) {
		throw new RuntimeException("filter executes a fractional number of times");
	    }
	    // now, just expand the current rep be the factor that we have calculated
	    expandedRep = filterReps[i].expand(filterExecutions);
	    
	    // make a new representation that has the same # of cols
	    // but splitterSum * currentRows rows.
	    int newRows = splitterSum * splitterFirings;
	    int newCols = expandedRep.getPushCount();
	    FilterMatrix newA = new FilterMatrix(newRows, newCols);
	    FilterVector newb = (FilterVector)expandedRep.getb().copy();
	    LinearPrinter.println("  created new A(" + newRows + "," + newCols + ")");
	    LinearPrinter.println("  copied b(" + newCols + ")");
	    
	    // now, copy the rows from old representation into the new representation
	    int numSections = expandedRep.getPeekCount() / splitterWeights[i];
	    LinearPrinter.println("  number of " + splitterWeights[i] + " sections is " + numSections); 
	    // each section has the splitterWeights[i] rows associated with it
	    for (int j=0; j<numSections; j++) { // j is the jth chunk of weights[i] rows
		LinearPrinter.println("  copying chunk " + j + " of " + splitterWeights[i] + " rows into filter " + i);
		// copy the rows into A and b of the new representation
		int newStart = currentOffset + j*splitterSum; // start row in newRep
		int sourceStart = j*splitterWeights[i]; // start row in expandedRep.A
		int numRows = splitterWeights[i]; // number of rows to copy
		newA.copyRowsAt(newStart, expandedRep.getA(), sourceStart, numRows);
	    }
	    // update the current offset (from the top) where we copy data to
	    currentOffset += splitterWeights[i];
	    //LinearPrinter.println("  new A: \n" + newA);
	    //LinearPrinter.println("  new b: \n" + newb);
	    duplicateReps[i] = new LinearFilterRepresentation(newA, newb, newRows);
	}
	// now we are ready to try duplicating
	return calculateDuplicate(duplicateReps, joinerWeights);
    }
    
}




 

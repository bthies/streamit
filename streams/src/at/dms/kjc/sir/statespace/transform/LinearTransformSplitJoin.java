package at.dms.kjc.sir.statespace.transform;

import at.dms.kjc.sir.statespace.*;
import java.util.*;
//import at.dms.kjc.*;
//mport at.dms.kjc.sir.*;
//import at.dms.kjc.sir.statespace.*;
//import at.dms.kjc.iterator.*;

/**
 * Contains the code for merging all the filters from a split join
 * into a single monolithic matrix. See the pldi-03-linear
 * <a href="http://cag.lcs.mit.edu/commit/papers/03/pldi-linear.pdf">
 * paper</a> or Andrew's
 * <a href="http://cag.lcs.mit.edu/commit/papers/03/aalamb-meng-thesis.pdf">
 * thesis</a> for more information.<br>
 *
 * $Id: LinearTransformSplitJoin.java,v 1.2 2004-02-13 17:05:56 thies Exp $
 **/
public class LinearTransformSplitJoin extends LinearTransform{
    LinearFilterRepresentation[] linearRepresentations;
    /** filterExpansionFactors[i] contains the factors by which to expand filter i. **/
    ExpansionRule[] filterExpansionFactors;
    /** the weights of the round robin joiner. **/
    int[] roundRobinJoinerWeights;
    /** joinRep is the (integer) factor by which we need to
	expand all of the weights in the roundrobin joiner. **/ 
    int joinRep;
    
    /**
     * Create one of these transformations with the appropriate pieces.
     * Doesn't copy the arrays, just copies the pointers.
     **/
    private LinearTransformSplitJoin(LinearFilterRepresentation[] lfr,
				     ExpansionRule[] expandFact,
				     int[] rrWeights) {
	this.linearRepresentations = lfr;
	this.filterExpansionFactors = expandFact;
	this.roundRobinJoinerWeights = rrWeights;
    }



    /**
     * Implement the actual transformation for a duplicate splitter splitjoin construct.
     * This involves expanding all of the
     * representations by the specified factors and then copying the columns
     * of the expanded matricies into a new linear transform in the correct
     * order.
     **/
    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
	at.dms.util.Utils.fail("Not implemented yet.");
	return null;
    }
	/*
	int filterCount = linearRepresentations.length;
	LinearPrinter.println(" preparing to combine splitjoin of " +
			      filterCount +
			      " filters");
	LinearPrinter.println(" Filter (expansion) factors:");
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("   " +
				  "(" + this.filterExpansionFactors[i] + ")");
	}
	
	// do the expansion of each of the linear reps.
	LinearFilterRepresentation[] expandedReps = new LinearFilterRepresentation[filterCount];
	int totalCols = 0; // totalCols keeps track of the total number of columns in the expanded reps
	
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("  expanding filter with " + this.filterExpansionFactors[i]);
	    expandedReps[i] = this.linearRepresentations[i].expand(this.filterExpansionFactors[i].peek,
								   this.filterExpansionFactors[i].pop,
								   this.filterExpansionFactors[i].push);
	    totalCols += expandedReps[i].getPushCount();
	}

	// figure how how many columns the "stride" is (eg the sum of the weights)
	int strideLength = 0;
	for (int i=0; i<filterCount; i++) {
	    strideLength += this.roundRobinJoinerWeights[i];
	}

	// now, create a new matrix and vector that have the appropriate size:
	// cols = the sum of the number of columns in the expanded representations
	// rows = (all expanded matrices have the same number of rows)
	FilterMatrix expandedA = new FilterMatrix(expandedReps[0].getPeekCount(), totalCols);
	FilterVector expandedb = new FilterVector(totalCols);

	// just do a little paranoia check and ensure that all of the peek counts are the same
	for (int i=0; i<filterCount; i++) {
	    if (expandedA.getRows() != expandedReps[i].getPeekCount()) {
		throw new RuntimeException("inconsistency -- expanded reps don't all have the same peek!");
	    }
	}
	
	// now, copy the cols of the matrices (and vectors) into the expanded versions
	// for each expanded matrix, copy joinWeight[i] cols into the new matrix and vector
	// at an offset that makes the output work out correctly.
	// See paper (http://cag.lcs.mit.edu/commit/papers/03/pldi-linear.pdf) Figure 9
	// start copying the cols of the right most filter at the appropriate start offset
	int startOffset = 0;
	for (int i=(filterCount-1); i>=0; i--) { // iterate through filters from right to left
	    // figure out how many groups of joinWeight cols that we have
	    int numGroups = expandedReps[i].getPushCount() / this.roundRobinJoinerWeights[i]; // total # of groups
	    LinearPrinter.println("  number of groups for filter " + i + " is " + numGroups);
	    // copy the groups from left (in groups of this.roundRobinJoinerWeights) into the new matrix
	    for (int j=0; j<numGroups; j++) {
		LinearPrinter.println("  doing group : " + j + " of filter " + i);
		LinearPrinter.println("  combination weight: " + this.roundRobinJoinerWeights[i]);
		// figure out offset into expanded A to copy the columns
		int currentOffset = startOffset + j*strideLength;
		// the offset into the current source matrix is (size-j-1)*joinWeights[i]
		// the number of columns that we are copying is combination weights[i]
		expandedA.copyColumnsAt(currentOffset, expandedReps[i].getA(),
					j*this.roundRobinJoinerWeights[i],
					this.roundRobinJoinerWeights[i]);
		expandedb.copyColumnsAt(currentOffset, expandedReps[i].get(),
					j*this.roundRobinJoinerWeights[i],
					this.roundRobinJoinerWeights[i]);
					
	    }
	    // update start of the offset for the next expanded rep. 
	    startOffset += this.roundRobinJoinerWeights[i];
	}

	// calculate what the new pop rate is (it needs to the the same for all expanded filters)
	// if it is not the same, then we have a problem (specifically, this splitjoin is not
	// schedulable....)
	int newPopCount = expandedReps[0].getPopCount();
	for (int i=0; i<filterCount; i++) {
	    if (newPopCount != expandedReps[i].getPopCount()) {
		throw new RuntimeException("Inconsistency -- pop counts are not all the same");
	    }
	}

	// now, return a new LinearRepresentation that represents the transformed
	// splitjoin with the new A and b, along with the new pop count.
	return new LinearFilterRepresentation(expandedA, expandedb, newPopCount);
    }
	*/

    /**
     * Utility method.
     * Parses a List of LinearFilter representations into an array of
     * of LinearFilterRepresentations.
     **/
    public static LinearFilterRepresentation[] parseRepList(List representationList) {
	LinearFilterRepresentation[] filterReps;
	filterReps = new LinearFilterRepresentation[representationList.size()];

	// for each rep, stuff it into the array
	Iterator repIter = representationList.iterator();
	int currentIndex = 0;
	while(repIter.hasNext()) {
	    LinearFilterRepresentation currentRep = (LinearFilterRepresentation)repIter.next();
	    filterReps[currentIndex] = currentRep;
	    currentIndex++;
	}
	return filterReps;
    }

    
    /**
     * Calculate the necessary information to combine the splitjoin when the splitter is
     * a duplicate. See thesis (http://cag.lcs.mit.edu/commit/papers/03/pldi-linear.pdf)
     * for an explanation of what is going on and why the
     * expansion factors are chosen the way that they are.<br>
     *
     * The input is a List of LinearFilterRepresentations and a matching array of joiner
     * weights. Note that this method merely calls the other calculateDuplicate so that
     * I can reuse code in calculateRoundRobin...
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
	return calculateDuplicate(filterReps, joinerWeights);
    }

    /**
     * Does the actual work of calculating the
     * necessary expansion factors for filters and the joiner.
     * Basiclly, it takes as input an array of filter reps and an array of joiner weights
     * and (if all checks pass) calculates the weights necessary to expand each filter
     * by and the factor necessary to expand the roundrobin joiner by. It then returns a
     * LinearTransformSplitjoin which has that information embedded in it.
     **/
    private static LinearTransform calculateDuplicate(LinearFilterRepresentation[] filterReps,
						      int[] joinerWeights) {
	int filterCount = filterReps.length;

	// calculate the expansion factors needed to match the peek rates of the filters
	// to do this, we calculate the lcm (lcm ( all push(i) and weight(i))) = x
	// Each filter needs to be expanded by a factor fact(i) = x/push(i);
	// overall, the joiner needs to be expanded by a factor x/weight(i) which needs to be
	// the same for all i.
	
	// keep track of the min number of times that the joiner needs to fire for
	// each filter
	int joinerFirings[] = new int[filterCount];
	int currentJoinerExpansionFactor = 0;
	for (int i=0; i<filterCount; i++) {
	    int currentLcm = lcm(filterReps[i].getPushCount(),
				 joinerWeights[i]);
	    joinerFirings[i] = currentLcm / joinerWeights[i];
	    LinearPrinter.println("  calculating lcm(pushrate,weight)=" +
				  "lcm(" + filterReps[i].getPushCount() + "," + joinerWeights[i] + ")" +
				  " for filter " +
				  i + ":" + currentLcm +
				  " ==> " + joinerFirings[i] + " firings in the steady state");
	}
	// now, calculate the lcm of all the joiner expansion factors     
	int joinRep = lcm(joinerFirings);

	// the overall number of repetitions for each filter
	int[] overallFilterFirings = new int[filterCount];
	
	LinearPrinter.println("  overall joiner expansion factor: " + joinRep);
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("  expand fact: " + joinRep +
				  " weight: " + joinerWeights[i] +
				  "#cols: " + filterReps[i].getPushCount());
	    overallFilterFirings[i] =  (joinerWeights[i] * joinRep) / filterReps[i].getPushCount();
	    LinearPrinter.println("  overall filter rep for " + i + " is " + overallFilterFirings[i]); 
	}

	// calcluate the maximum peek rate (the max of any of the expanded sub filters)
	int maxPeek = getMaxPeek(filterReps, overallFilterFirings);
	
	// now, calculate the peek, pop and push rates of the individual filters.
	ExpansionRule filterExpansions[] = new ExpansionRule[filterCount];
	for (int i=0; i<filterCount; i++) {
	    filterExpansions[i] = new ExpansionRule(maxPeek,
						    filterReps[i].getPopCount() * overallFilterFirings[i],
						    filterReps[i].getPushCount() * overallFilterFirings[i]);
	}

	// do some sanity checks:
	// 1. The total data pushed is the same as the total consumed by the joiner
	int totalDataProducedByFilters = 0;
	int totalDataConsumedByJoiner = 0;
	for (int i=0; i<filterCount; i++) {
	    totalDataProducedByFilters += filterExpansions[i].push;
	}
	for (int i=0; i<joinerWeights.length; i++) {
	    totalDataConsumedByJoiner += joinerWeights[i] * joinRep;
	}
	if (totalDataProducedByFilters != totalDataConsumedByJoiner) {
	    return new LinearTransformNull("data produced by filters(" + totalDataProducedByFilters +
					   ") doesn't equal data consumed by joiner(" +
					   totalDataConsumedByJoiner +")");
	}
	// 2. The total data poped is the same for all of the filters
	int overallPopCount = filterExpansions[0].pop;
	for (int i=0; i<filterCount; i++) {
	    if (filterExpansions[i].pop != overallPopCount) {
		return new LinearTransformNull("Overall pop count doesn't match --> split join is unschedulable");
	    }
	}
	
	// finally, if we get to here, we have the expansion factors that we need, and
	// we can pass that information into a new LinearTransform object
	return new LinearTransformSplitJoin(filterReps,
					    filterExpansions,
					    joinerWeights);
    }

    /** Calculates the maxmum peek rate (o_i * firings + e_i - o_i) for all of the filters. **/
    public static int getMaxPeek(LinearFilterRepresentation[] filterReps, int[] filterFirings) {
	int maxPeek = -1;
	for (int i=0; i<filterReps.length; i++) {
	    int currentPeek = filterReps[i].getPopCount() * (filterFirings[i] - 1)  + filterReps[i].getPeekCount();
	    if (currentPeek > maxPeek) {maxPeek = currentPeek;}
	}
	return maxPeek;
    }

    /** Structure to hold new peek/pop/push rates for linear reps. **/
    static class ExpansionRule {
	int peek;
	int pop;
	int push;
	/** Create a new ExpansionRule. **/
	ExpansionRule(int e, int o, int u) {
	    this.peek = e;
	    this.pop  = o;
	    this.push = u;
	}
	/** Nice human readable string (for debugging...) **/
	public String toString() {
	    return ("(peek:" + this.peek +
		    ", pop:" + this.pop +
		    ", push:" + this.push +
		    ")");
	}
    }
}

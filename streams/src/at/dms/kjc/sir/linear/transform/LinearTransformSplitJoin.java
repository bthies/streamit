package at.dms.kjc.sir.linear;

import java.util.*;
//import at.dms.kjc.*;
//mport at.dms.kjc.sir.*;
//import at.dms.kjc.sir.linear.*;
//import at.dms.kjc.iterator.*;

/**
 * Contains the code for merging all the filters from a split join
 * into a single monolithic matrix.
 * $Id: LinearTransformSplitJoin.java,v 1.1 2002-09-23 21:26:22 aalamb Exp $
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
     * Calculate the necessary information to combine the splitjoin when the splitter is
     * a duplicate. See the documentation (eg what will be my thesis) for an explanation
     * of what is going on and why the expansion factors are chosen the way that they are.
     *
     * The input is a List of LinearFilterRepresentations and a matching array of joiner
     * weights.
     **/
    public static LinearTransform calculateDuplicate(List representationList,
						     int[] joinerWeights) {
	LinearPrinter.println(" calculating splitjoin transform.");
	if (representationList.size() != joinerWeights.length) {
	    throw new IllegalArgumentException("different numbers of reps and weights " +
					       "while transforming splitjoin");
	}
	int filterCount = representationList.size();

	// create an array of LinearFilterRepresentations and their peek rates
	LinearFilterRepresentation[] filterReps;
	filterReps = new LinearFilterRepresentation[filterCount];
	int peekRates[] = new int[filterCount];

	// for each rep, check the peek=pop condition and grab peek rates
	Iterator repIter = representationList.iterator();
	int currentIndex = 0;
	while(repIter.hasNext()) {
	    LinearFilterRepresentation currentRep = (LinearFilterRepresentation)repIter.next();
	    if (currentRep.getPopCount() != currentRep.getPeekCount()) {
		return new LinearTransformNull("Peek didn't match pop rate.");
	    } else {
		filterReps[currentIndex] = currentRep;
		peekRates[currentIndex] = currentRep.getPeekCount();
		currentIndex++;
	    }
	}
	LinearPrinter.println("  all filters have peek=pop");

	// transform the joinerWeights into their lowest form (eg 2,6 --> 1,3)
	int joinGcd = gcd(joinerWeights);
	int[] reducedJoinerWeights = new int[filterCount];
	for (int i=0; i<filterCount; i++) {
	    reducedJoinerWeights[i] = joinerWeights[i]/joinGcd;
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
	// eg lcm(cols(A[0]), reducedJoinerWeights[0])
	int expandFactor = lcm(newPushRates[0], reducedJoinerWeights[0]);
	int filterExpandFactor = expandFactor / newPushRates[0];
	int joinerExpandFactor = expandFactor / reducedJoinerWeights[0];
	LinearPrinter.println("  using newRate[0] = " + newPushRates[0] + " and w[0] = " + reducedJoinerWeights[0]);
	LinearPrinter.println("  calculated common expansion factor: " + expandFactor);
	LinearPrinter.println("  calculated filter expansion factor: " + filterExpandFactor);
	LinearPrinter.println("  calculated joiner expansion factor: " + joinerExpandFactor);
	// now, make sure that that expansion factor works for all filters
	// if it doesn't that means that the splitjoin is unschedulable.	
	for (int i=0; i<filterCount; i++) {
	    int currentExpandFactor = lcm(newPushRates[i], reducedJoinerWeights[i]); 
	    if ((currentExpandFactor / newPushRates[i]) != filterExpandFactor) {
		return new LinearTransformNull("Split join is unschedulable! 1:");
	    }
	    if ((currentExpandFactor / reducedJoinerWeights[i]) != joinerExpandFactor) {
		return new LinearTransformNull("Split join is unschedulable! 2");
	    }
	}

	// calculate the overall expansion rates for the filters and for the joiners
	int[] overallFilterWeights = new int[filterCount];
	int[] overallJoinerWeights = new int[filterCount];
	for (int i=0; i<filterCount; i++) {
	    overallFilterWeights[i] = rateMatchFactors[i] * filterExpandFactor;
	    overallJoinerWeights[i] = reducedJoinerWeights[i] * joinerExpandFactor;
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
	    }
	}

	// finally, if we get to here, we have the expansion factors that we need.
	return new LinearTransformSplitJoin(filterReps,
					    overallFilterWeights,
					    overallJoinerWeights);
    }

    /**
     * Calculate the necessary information to combine the splitjoin when the splitter is
     * a roundrobin. 
     **/
    public static LinearTransform calculateRoundRobin(List representationList,
					       int[] joinerWeights,
					       int[] splitterWeights) {
	return new LinearTransformNull ("nope.");
    }
    
}




 

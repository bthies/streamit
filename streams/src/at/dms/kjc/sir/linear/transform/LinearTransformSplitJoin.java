package at.dms.kjc.sir.linear;

import java.util.*;
//import at.dms.kjc.*;
//mport at.dms.kjc.sir.*;
//import at.dms.kjc.sir.linear.*;
//import at.dms.kjc.iterator.*;

/**
 * Contains the code for merging all the filters from a split join
 * into a single monolithic matrix.
 * $Id: LinearTransformSplitJoin.java,v 1.3 2002-10-01 21:42:47 aalamb Exp $
 **/
public class LinearTransformSplitJoin extends LinearTransform{
    LinearFilterRepresentation[] linearRepresentations;
    /** filterExpansionFactors[i] contains the (integer) factors by which to expand filter i. **/
    int[] filterExpansionFactors;
    /** the weights of the round robin joiner. **/
    int[] roundRobinJoinerWeights;
    /** combinationWeight is the (integer) factor by which we need to expand the weighted roundrobin joiner. **/ 
    int joinerExpansionFactor;
    
    /**
     * Create one of these transformations with the appropriate pieces.
     * Doesn't copy the arrays, just copies the pointers.
     **/
    private LinearTransformSplitJoin(LinearFilterRepresentation[] lfr,
				     int[] expandFact,
				     int[] rrWeights,
				     int joinExpandFact) {
	this.linearRepresentations = lfr;
	this.filterExpansionFactors = expandFact;
	this.roundRobinJoinerWeights = rrWeights;
	this.joinerExpansionFactor = joinExpandFact;
    }

    /**
     * Implement the actual transformation for a duplicate splitter splitjoin construct.
     * This involves expanding all of the
     * representations by the specified factors and then copying the columns
     * of the expanded matricies into a new linear transform in the correct
     * order.
     **/
    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
	int filterCount = linearRepresentations.length;
	LinearPrinter.println(" preparing to combine splitjoin of " +
			      filterCount +
			      " filters");
	LinearPrinter.println(" joiner expansion factor: " + joinerExpansionFactor);
	LinearPrinter.println(" Filter (expansion) factors:");
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("   " +
				  "(" + this.filterExpansionFactors[i] + ")");
	}
	
	// do the expansion
	LinearFilterRepresentation[] expandedReps = new LinearFilterRepresentation[filterCount];
	int totalCols = 0;
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("  expanding filter by " + this.filterExpansionFactors[i]);
	    expandedReps[i] = this.linearRepresentations[i].expand(this.filterExpansionFactors[i]);
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

	// now, copy the cols of the matrices (and vectors) into the expanded versions
	// for each expanded matrix, copy joinWeight[i] cols into the new matrix and vector
	// at an offset that makes the output work out correctly. See paper.
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
		expandedb.copyColumnsAt(currentOffset, expandedReps[i].getb(),
					j*this.roundRobinJoinerWeights[i],
					this.roundRobinJoinerWeights[i]);
					
	    }
	    // update start of the offset for the next expanded rep. 
	    startOffset += this.roundRobinJoinerWeights[i];
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

	// calculate the expansion factors needed to match the peek rates of the filters
	// to do this, we calculate the lcm (lcm ( all push(i) and weight(i))) = x
	// Each filter needs to be expanded by a factor fact(i) = x/push(i);
	// overall, the joiner needs to be expanded by a factor x/weight(i) which needs to be
	// the same for all i.

	
	// keep track of the min number of times that the joiner needs to fire for
	// each filter, 
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
	int joinerExpandFact = lcm(joinerFirings);
	
	// now, calculate the weights that the individual filters need to be expanded by
	// and the factor by which to expand the round robin joiner
	int[] overallFilterWeights = new int[filterCount];
	
	LinearPrinter.println("  overall joiner expansion factor: " + joinerExpandFact);
	for (int i=0; i<filterCount; i++) {
	    LinearPrinter.println("  expand fact: " + joinerExpandFact +
				  " weight: " + joinerWeights[i] +
				  "#cols: " + filterReps[i].getPushCount());
	    overallFilterWeights[i] =  (joinerExpandFact * joinerWeights[i]) / filterReps[i].getPushCount();
	    if (overallFilterWeights[i] == 0) {throw new RuntimeException("expansion factor was 0");}
	    LinearPrinter.println("  overall filter weight for " + i + " is " + overallFilterWeights[i]); 
	}

	// now, we need to verify that the total # of rows (eg the size of each column) is
	// is the same after the appropriate expansions so that the schedule works out. If
	// not, then the graph is unschedulable.
	int totalInputData = overallFilterWeights[0]*filterReps[0].getPeekCount();
	for (int i=0; i<filterCount; i++) {
	    // if total peeked at
	    if ((overallFilterWeights[i]*filterReps[i].getPeekCount()) != totalInputData) {
		LinearPrinter.println("  graph is unschedulable. aborting combination.");
		return new LinearTransformNull("Unscheduable Graph");
	    }
	}
		    
	

	// finally, if we get to here, we have the expansion factors that we need, and
	// we can pass that information into a new LinearTransform object
	return new LinearTransformSplitJoin(filterReps,
					    overallFilterWeights,
					    joinerWeights,
					    joinerExpandFact);
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
	// iterate through the representations backwards (eg rightmost rep first)
	for (int i=(filterCount-1); i>=0; i--) {
	    LinearPrinter.println("  expanding filter: " + i);
	    LinearFilterRepresentation expandedRep;
	    // the number of items the new filter needs to consume
	    int totalConsumption = (splitterFirings * splitterWeights[i]);
	    LinearPrinter.println("   filter needs to consume " + totalConsumption + " items");
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




 

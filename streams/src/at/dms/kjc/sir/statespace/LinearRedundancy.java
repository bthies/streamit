package at.dms.kjc.sir.statespace;

import java.util.*;

/**
 * A LinearRedundancy contains information about redundant computations
 * that occur across filter invocations. LinearRedundancy is probably a
 * misnomer as this class merely contains information that lets us
 * identify redundant computation. Specifically, we keep a
 * mapping between a computation tuple (eg [input,coefficient]) and
 * "uses." A use of a computation tuple is definied as the number
 * subsequenct work function after the current one that the tuple is re used.<br>
 *
 * More information might be gleaned from:
 * http://cag.lcs.mit.edu/commit/papers/03/aalamb-meng-thesis.pdf<br>
 *
 * $Id: LinearRedundancy.java,v 1.1 2004-02-09 17:55:01 thies Exp $
 **/
public class LinearRedundancy {
    /**
     * The internal representation of a linear redundancy is a hashmap
     * between LinearComputationTuples and a list of executions used in (integers).
     **/
    private HashMap tuplesToUses;
    
    /**
     * Creates a new linear redundancy from the linear filter rep that is passed
     * in (which has a matrix and a vector to describe computation. By creating a
     * LinearRedundancy, we run the actual redundancy analysis. <br>
     *
     * The overall plan for calculating all of the mappings that we need
     * is to realize that any tuple can only be used in up to ceil(peek/pop)
     * filter firings. Therefore, we iterate that many times, adding
     * all of the necessary tuples.
     */
    public LinearRedundancy(LinearFilterRepresentation lfr) {
	// initialize our mappings
	this.tuplesToUses = new HashMap();
	
	// now, we only care about the matrix (the vector is simply added into
	// the final output). We also pull out the peek, pop and push counts
	// to make the following code more readable.
	FilterMatrix A = lfr.getA();
	int peekCount = lfr.getPeekCount();
	int popCount = lfr.getPopCount();
	int pushCount = lfr.getPushCount();
	int maxExecutions = divCeiling(peekCount, popCount);
	for (int currentExecution = 0; currentExecution < maxExecutions; currentExecution++) {
	    // now, for each row and column that we are interested in
	    // add a use (start from the bottom and work up)
	    // (rowsUp is the number of rows from the bottom of the original matrix we are)
	    int rowsUp = (currentExecution * popCount);
	    // row is the current row of the matrix that we are looking at.
	    for (int row = rowsUp; row < peekCount; row++) {
		// col is the column of the matrix that we are looking at (simply
		// copy the whole thing, so we look at the whole column)		
		for (int col = 0; col < pushCount; col++) {
		    // make the appropriate tuple, which is the tricky part
		    // the element from A is easy -- it is just A(row,col) by
		    // contruction. 
		    // However, the tuple position needs to be the size of
		    // the matrix minus the number of rows down that we are
		    // that overlap the matrix. It is probably easier to see
		    // with a graphic.
		    LinearComputationTuple tuple =
			new LinearComputationTuple(rowsUp + (peekCount - 1 - row),	
						   A.getElement(row, col));
		    // only add the tuple if the coefficient is non zero
		    // (eg we don't care about reusing zero coefficients!)
		    if (!tuple.getCoefficient().equals(ComplexNumber.ZERO)) {
			addUse(tuple, currentExecution);
		    }
		}
	    }
	}
    }
    
    /** add an entry for the specified execution use to this tuple **/
    public void addUse(LinearComputationTuple tuple, int use) {
	if (use < 0) {
	    throw new IllegalArgumentException("use was less than zero: " + use);
	}
	// if we don't have a list mapping to this tuple yet, make one.
	if (!this.tuplesToUses.containsKey(tuple)) {
	    this.tuplesToUses.put(tuple, new LinkedList());
	}
	// now, add the specified use to the list that the tuple maps to.
	((List) this.tuplesToUses.get(tuple)).add(new Integer(use));
    }

    /**
     * Accessor into the internal tuples to uses map. Sure this
     * violates encapsulation, but what are you going to do?
     * tuplesToUses maps LinearComputationTuples to
     * lists of Integers, which represent the execution of the work function
     * after the current one that the tuple is used in.
     **/
    public HashMap getTuplesToUses() {
	return this.tuplesToUses;
    }

    /** Make a nice human readable string for this LinearRedundancy. **/
    public String toString() {
	return this.calculateRedundancyString();
    }
    
    /** Calculate redundancy statistics. **/
    public RedundancyStatistics calculateRedundancyStatistics() {
	RedundancyStatistics stats = new RedundancyStatistics();
	Iterator tupleIter = this.tuplesToUses.keySet().iterator();
	while(tupleIter.hasNext()) {
	    LinearComputationTuple currentTuple = (LinearComputationTuple)tupleIter.next();
	    // if this tuple is zero, go to the next iteration.
	    if (currentTuple.getCoefficient().equals(ComplexNumber.ZERO)) {
		// do nothing this iteration
	    } else {
		List useList = (List)this.tuplesToUses.get(currentTuple);
		// now, for each use, update the tuple counts appropriately
		Iterator useIter = useList.iterator();
		// flag that is set when we have 
		//seen the first use of this tuple (so we can begin counting redundancy)
		boolean seenFirstUse = false; 
		while(useIter.hasNext()) {
		    int currentUse = ((Integer)useIter.next()).intValue();
		    // if this tuple is used in the initial execution, add it to the
		    // total original tuple count
		    if (currentUse == 0) {
			stats.totalOriginalTuples++;
			// if we have already seen the first use of this tuple, add
			// a little something on to the redundant tuple count.
			if (seenFirstUse) {
			    stats.reusedTuples++;
			}
			seenFirstUse = true;
			stats.originalTuples.add(currentTuple);
		    } else {
			// this tuple is used in a subsequen execution.
			// if it was calculated in the initial exeuciton, add
			// a count to the redundant tuple count.
			if (stats.originalTuples.contains(currentTuple)) {
			    stats.reusedTuples++;
			    stats.crossFiringReusedTuples++;
			}
		    }
		}
	    }
	}		
	return stats;
    }

    /** Structure class for storing redundancy information. **/
    class RedundancyStatistics {
	/** set to keep track of the tuples calculated in the first invocation. **/
	public HashSet originalTuples = new HashSet(); 
	/** the total number of tuples that are calculated in the first invocation. **/
	public int totalOriginalTuples = 0;
	/** the number of times that an original tuple is reused. **/
	public int reusedTuples = 0;
	/** the number of times that an original tuple is reused in subsequent firings. **/
	public int crossFiringReusedTuples = 0;
    }

    /**
     * Generates a string of the form:
     * totalOriginalTuples:reusedTuples|crossFiringTuples reused pct:subsequent reused pct)
     **/
    public String makeShortRedundancyString() {
	// first, calculate the statistics.
	RedundancyStatistics stats = calculateRedundancyStatistics();
	
	float reusedPercent = 100 * (((float)stats.reusedTuples)/
				     ((float)stats.totalOriginalTuples));
	float subsequentReusedPercent = 100 * (((float)stats.crossFiringReusedTuples)/
					       ((float)stats.totalOriginalTuples));
	reusedPercent = Math.round(reusedPercent);
	subsequentReusedPercent = Math.round(subsequentReusedPercent);
	return ("total:(reused)(fut.reused) (reused%)(fut.reused%)\\n" +
		stats.totalOriginalTuples + ":(" +
		stats.reusedTuples + ")(" +
		stats.crossFiringReusedTuples + ") (" +
		reusedPercent + "%)(" +
		subsequentReusedPercent + "%)");
    }
    
    /** 
     * Returns a number between zero and one that represents the amount of 
     * redundant computation that this LinearRedundancy does. For now,
     * we define the linear redundancy to be the number of tuples that are
     * reused in subsequent firings over the total tuples used in the first
     * iteration (eg size of the filter matrix minus the number of zero entries
     */
    public String calculateRedundancyString() {
	// first, calculate the statistics.
	RedundancyStatistics stats = calculateRedundancyStatistics();
	
	String returnString = "original tuples:" + stats.totalOriginalTuples + "\n";
	returnString +=       "reused tuples:" + stats.reusedTuples + "\n";
	returnString +=       "reused tuples in subsequent firings:" + stats.crossFiringReusedTuples+ "\n";
	returnString +=       "overall redundancy: "; 
	returnString +=       100 * ((float)stats.reusedTuples)/((float)stats.totalOriginalTuples);
	returnString +=       "%\n";
	returnString +=       "subsequent redundancy: "; 
	returnString +=       100 * (((float)stats.crossFiringReusedTuples)/
				     ((float)stats.totalOriginalTuples));
	returnString +=       "%";
	
	return returnString;
    }

    /**
     * Returns a string version of the tuple -> list mapping.
     * This is used for debugging.
     **/
    public String getTupleString() {
	String str = "";
	// iterate through tuples, each tuple iterate through list
	Iterator tupleIter = this.tuplesToUses.keySet().iterator();
	while(tupleIter.hasNext()) {
	    Object tuple = tupleIter.next();
	    str += tuple + "-->(";
	    Iterator listIter = ((List)this.tuplesToUses.get(tuple)).iterator();
	    while(listIter.hasNext()) {
		str += listIter.next() + ",";
	    }
	    // chop off trailing comma
	    str = str.substring(0, str.length()-1) + ")\n";
	}
	return str;
    }
	
    /** Check that the rep invariant holds. **/
    private void checkRep() {
	Iterator tupleIter = this.tuplesToUses.keySet().iterator();
	while(tupleIter.hasNext()) {
	    Object next = tupleIter.next();
	    if (!(next instanceof LinearComputationTuple)) {
			throw new RuntimeException("non comp tuple as key in LinearRedundancy");
	    }
	    LinearComputationTuple nextTuple = (LinearComputationTuple)next;
	    // now pull out the thing that is mapped to (should be a list)
	    Object valObject = this.tuplesToUses.get(nextTuple);
	    if (!(valObject instanceof List)) {
			throw new RuntimeException("non list as value");
	    }
	    List useList = (List)valObject;
	    // list should be non-empty
	    if (useList.isEmpty()) {
		throw new RuntimeException("use list is empty");
	    }
	    // now, each element of the list should be an Integer object
	    Iterator listIter = useList.iterator();
	    while(listIter.hasNext()) {
			Object listObject = listIter.next();
			if (!(listObject instanceof Integer)) {
		    	throw new RuntimeException("non integer in list body");
			}
	    }
	}
    }

    /**
     * Gets the ceiling of the division of two integers. Eg
     * divCeil(5,3) = ceil(5/3) = 2. This is the same code as is in
     * sir.linear.transform.LinearTransform. Copied here so as to not
     * induce a dependence between packages.
     **/
    public static int divCeiling(int a, int b) {
	int dividend = a/b;
	int remainder = a%b;
	if (remainder != 0) {
	    dividend++;
	}
	return dividend;
    }



}
    

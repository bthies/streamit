package at.dms.kjc.sir.linear;

import java.util.*;

/**
 * A LinearRedundancy represents the redundant computations
 * that occur across filter invocations (and any other information
 * that is necessary to reduce computation.) Specifically, we keep a
 * mapping between a computation tuple (eg [input,coefficient]) and
 * "use" which is definied as the number of firings after the current
 * one that the tuple is used.
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
     * LinearRedundancy, we run the actual redundancy analysis. <p>
     *
     * The overall plan for calculating all of the mappings that we need
     * is to realize that any tuple can only be used in up to ceil(peek/pop)
     * filter firings. Therefore, we iterate that many times, adding
     * all of the necessary tuples.<p>
     */
	public LinearRedundancy(LinearFilterRepresentation lfr) {
		// initialize our mappings
		this.tuplesToUses = new HashMap();

		// now, we only care about the matrix (the vector is simply added into
		// the final output).
		// we also pull out the peek, pop and push counts to make the following code
		// more readable.
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
						new LinearComputationTuple(
							rowsUp + (peekCount - 1 - row),	
							A.getElement(row, col));
					addUse(tuple, currentExecution);
				}
			}
		}

//		checkRep();
	}

    /** add an entry for the specified execution use to this tuple **/
	public void addUse(LinearComputationTuple tuple, int use) {
		if (use < 0) {
			throw new IllegalArgumentException(
				"use was less than zero: " + use);
		}
		// if we don't have a list mapping to this tuple yet, make one.
		if (!this.tuplesToUses.containsKey(tuple)) {
			this.tuplesToUses.put(tuple, new LinkedList());
		}
		// now, add the specified use to the list that the tuple
		// maps to.
		 ((List) this.tuplesToUses.get(tuple)).add(new Integer(use));
		//checkRep();
	}

	/** make a nice human readable string for this LinearRedundancy. **/
	public String toString() {
//		checkRep();
//		String returnString = "";
//		Iterator listIter = this.tuplesToUses.keySet().iterator();
//		while (listIter.hasNext()) {
//			LinearComputationTuple currentTuple =
//				(LinearComputationTuple) listIter.next();
//			List tupleList = (List) this.tuplesToUses.get(currentTuple);
//			Iterator useIter = tupleList.iterator();
//			// build the return string using this 
//			returnString += currentTuple.toString() + ":";
//			String useString = "";
//			while (useIter.hasNext()) {
//				useString += useIter.next() + ",";
//			}
//			// chop off the trailing comma, and add parenthesis
//			useString = "(" + useString.substring(0, useString.length() - 1) + ")";
//			returnString += useString + "\n";
//						
//		}
//		returnString += this.calculateRedundancyString();	
//		return returnString;
		return this.calculateRedundancyString();
	}
	
	/** 
	 * Returns a number between zero and one that represents the amount of 
	 * redundant computation that this LinearRedundancy does. For now,
	 * we define the linear redundancy to be the number of tuples that are
	 * reused in subsequent firings over the total tuples used in the first
	 * iteration (eg size of the filter matrix minus the number of zero entries
	 */
	public String calculateRedundancyString() {
		// set to keep track of the tuples calculated in the first invocation
		HashSet originalTuples = new HashSet(); 
		// the total number of tuples that are calculated in the first invocation
		int totalOriginalTuples = 0;
		// the number of times that an original tuple is reused.
		int reusedTuples = 0;
		// the number of times that an original tuple is reused in subsequent firings
		int crossFiringReusedTuples = 0;
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
				 		totalOriginalTuples++;
				 		// if we have already seen the first use of this tuple, add
				 		// a little something on to the redundant tuple count.
				 		if (seenFirstUse) {
				 			reusedTuples++;
				 		}
				 		seenFirstUse = true;
				 		originalTuples.add(currentTuple);
				 	} else {
				 		// this tuple is used in a subsequen execution.
				 		// if it was calculated in the initial exeuciton, add
				 		// a count to the redundant tuple count.
				 		if (originalTuples.contains(currentTuple)) {
				 			reusedTuples++;
				 			crossFiringReusedTuples++;
				 		}
				 	}
				}
			}
		}		
		String returnString = "original tuples:" + totalOriginalTuples + "\n";
		returnString +=       "reused tuples:" + reusedTuples + "\n";
		returnString +=       "reused tuples in subsequent firings:" + crossFiringReusedTuples+ "\n";
		returnString +=       "overall redundancy: "; 
		returnString +=       100 * ((float)reusedTuples)/((float)totalOriginalTuples);
		returnString +=       "%\n";
		returnString +=       "subsequent redundancy: "; 
		returnString +=       100 * ((float)crossFiringReusedTuples)/((float)totalOriginalTuples);
		returnString +=       "%";
		
		// for the moment, enumerate the original tuples (to check algorithm)
//		returnString += 	 "\noriginal tuples:\n";
//		Iterator origIter = originalTuples.iterator();
//		while(origIter.hasNext()) {
//			returnString += " " + origIter.next() + "\n";			
//		}
		
		return returnString;
	}
	
	
    /** check that the rep invariant holds. **/
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
    

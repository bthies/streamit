package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
//import at.dms.kjc.sir.linear.*;
//import at.dms.kjc.sir.linear.transform.*;
//import at.dms.kjc.iterator.*;

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
     * all of the necessary tuples.
     **/    
    public LinearRedundancy(LinearFilterRepresentation lfr) {
	this.tuplesToUses = new HashMap();

	// now, we only care about the matrix (the vector is simply added into the final output).
	FilterMatrix A = lfr.getA();
	int maxExecutions = divCeiling(lfr.getPeekCount(),lfr.getPopCount());
	for (int currentExecution=0; currentExecution < maxExecutions; currentExecution++) {
	    // now, for each row and column that we are interested in
	    // add a use (start from the bottom and work up)
	    int rowsUp = (currentExecution*lfr.getPopCount());
	    for (int i=lfr.getPeekCount() - rowsUp - 1; i >=0; i--) {
		for (int j=0; j<lfr.getPushCount(); j++) {
		    // make the appropriate tuple
		    LinearComputationTuple tuple = new LinearComputationTuple(i+rowsUp, A.getElement(i,j));;
		    addUse(tuple, currentExecution);
		}
	    }
	}
	checkRep();
    }

    /** add an entry for the specified execution use to this tuple **/
    public void addUse(LinearComputationTuple tuple, int use) {
	if (use < 0) {
	    throw new IllegalArgumentException("use was less than zero: " + use);
	}
	LinearPrinter.println("aal: use: " + use);
	// if we don't have a list mapping to this tuple yet, make one.
	if (!this.tuplesToUses.containsKey(tuple)) {
	    this.tuplesToUses.put(tuple, new LinkedList());
	}
	// now, add the specified use to the list that the tuple
	// maps to.
	((List)this.tuplesToUses.get(tuple)).add(new Integer(use));
	checkRep();
    }

    /** make a nice human readable string for this LinearRedundancy. **/ 
    public String toString() {
	checkRep();
	String returnString = "";
	Iterator listIter = this.tuplesToUses.keySet().iterator();
	while(listIter.hasNext()) {
	    LinearComputationTuple currentTuple = (LinearComputationTuple)listIter.next();
	    List tupleList = (List)this.tuplesToUses.get(currentTuple);
	    Iterator useIter = tupleList.iterator();
	    // build the return string using this 
	    returnString += currentTuple.toString() + ":";
	    while(useIter.hasNext()) {
		returnString += useIter.next() + ",";
	    }
	    returnString += "\n";
	}
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
    

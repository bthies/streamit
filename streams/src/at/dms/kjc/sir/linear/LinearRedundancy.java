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
 * mapping between 
 **/
public class LinearRedundancy {
    /**
     * The internal representation of a linear redundancy is a hashmap
     * between LinearComputationTuples and a list of executions used in (integers).
     **/
    private HashMap tuplesToUses;
    
    public LinearRedundancy(LinearFilterRepresentation lfr) {
	this.tuplesToUses = new HashMap();
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
}
    

package at.dms.kjc.sir.statespace;

import java.util.*;
import at.dms.kjc.sir.*;


/**
 * The LinearRedundancyAnalyzer tries to determine redundant computations
 * across the firings of filters. <br>
 *
 * $Id: LinearRedundancyAnalyzer.java,v 1.1 2004-02-09 17:55:01 thies Exp $
 **/
public class LinearRedundancyAnalyzer {
    // the information that we are going to keep is a mapping from filter
    // to redundancy information.
    HashMap filtersToRedundancy;

    /**
     * Main entry point for redundancy analysis. Gets passed a
     * LinearAnalyzer and creates a new LinearRedundancyAnalyzer
     * based on that.
     **/
    public LinearRedundancyAnalyzer(LinearAnalyzer la) {
	this.filtersToRedundancy = new HashMap();
	Iterator filterIterator = la.getFilterIterator();
	while(filterIterator.hasNext()) {
	    SIRStream filter = (SIRStream)filterIterator.next();
	    LinearPrinter.println("analyzing " + filter + " for redundancy...");
	    LinearFilterRepresentation filterRep = la.getLinearRepresentation(filter);
	    // make a new linear redundancy for this filter
	    LinearRedundancy filterRedundancy = new LinearRedundancy(filterRep);
	    // add the redundancy to our data structure
	    this.filtersToRedundancy.put(filter, filterRedundancy);
	}
	// print out redundancy information string via the linear printer.
	LinearPrinter.println(this.toString());
    }


    /**
     * Returns true if this linear redundancy analyzer has redundancy information
     * for the passed SIRStream.
     **/
    public boolean hasRedundancy(SIRStream str) {
	return this.filtersToRedundancy.containsKey(str);
    }

    /**
     * Returns the LinearRedundancy opject associated with the
     * SIRStream str.
     **/
    public LinearRedundancy getRedundancy(SIRStream str) {
	if (!(hasRedundancy(str))) {
	    throw new IllegalArgumentException("no redundancy information for " + str);
	}
	return (LinearRedundancy)this.filtersToRedundancy.get(str);
    }


    /**
     * Prints out the internal state of this analyzer for debugging purposes.
     **/
    public String toString() {
	String returnString = "";
	returnString += "Linear Redundancy Analysis:\n";
	Iterator keyIter = this.filtersToRedundancy.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object key = keyIter.next();
	    Object val = this.filtersToRedundancy.get(key);
	    returnString += key + "\n";
	    returnString += val + "\n";
	}
	returnString += "end.";
	return returnString;
    }
}



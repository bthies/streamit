package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
//import at.dms.compiler.*;


/**
 * RedundantReplacer.
 * $Id: LinearRedundancyReplacer.java,v 1.2 2003-02-26 16:50:12 aalamb Exp $
 **/
public class LinearRedundancyReplacer extends EmptyStreamVisitor implements Constants{
    /** The field that has all of the redundancy information necessary to create replacements. **/
    LinearRedundancyAnalyzer redundancyInformation;

    /** Private constructor for the LinearRedundancyReplacer.**/
    private LinearRedundancyReplacer(LinearRedundancyAnalyzer lra) {
	this.redundancyInformation = lra;
	
    }

    /** start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearRedundancyAnalyzer lra, SIRStream str) {
	LinearRedundancyReplacer replacer = new LinearRedundancyReplacer(lra);
	// pump the replacer through the stream graph.
	IterFactory.createIter(str).accept(replacer);
    }

    
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRFeedbackLoopIter iter) {makeReplacement(self, iter);}
    public void preVisitPipeline    (SIRPipeline self,
				     SIRPipelineIter iter){makeReplacement(self, iter);}
    public void preVisitSplitJoin   (SIRSplitJoin self,
				     SIRSplitJoinIter iter){makeReplacement(self, iter);}
    public void visitFilter         (SIRFilter self,
				     SIRFilterIter iter){makeReplacement(self, iter);}
    

    /**
     * Visit a pipeline, splitjoin or filter, replacing them with a new filter
     * that directly implements the linear representation. This only
     * occurs if the replace calculator says that this stream should be replaced.
     **/
    private void makeReplacement(SIRStream self, SIRIterator iter) {
	LinearPrinter.println("starting redundancy replacement for " + self);

	// if there is no redundancy information about this filter, our
	// work here is done.
	if (!this.redundancyInformation.hasRedundancy(self)) {
	    LinearPrinter.println(" no redundancy information for " + self);
	} else {
	    /** calcluate the data structures that we need to generate the
	     * replaced filter's body. **/
	    RedundancyReplacerData tupleData;
	    tupleData = new RedundancyReplacerData(this.redundancyInformation.getRedundancy(self));
	    
	    System.err.println("\n\nRedundancy information calculated:");
	    System.err.println(tupleData);
	    System.err.println("\n");
	    
	    throw new RuntimeException("not yet implemented");
	}

	LinearPrinter.println("done with redundancy replacement.");
    }





    ///////////////////////////////////////////////////////////////
    // Inner Classes that calculate the necessary data structures
    ///////////////////////////////////////////////////////////////

    /**
     * Inner class that contains data that we need to perform
     * the redundancy elimination optimization.
     **/
    class RedundancyReplacerData {
	/**
	 * Mapping of Tuples to maximum use (eg the max number of work function
	 * executions in the future that this tuple could be used in.
	 **/
	private HashMap minUse;
	/**
	 * Mapping of Tuples to maximum use (eg the max number of work function
	 * executions in the future that this tuple could be used in.
	 **/
	private HashMap maxUse;
	/**
	 * Mapping of Tuples to their "value source," where value source
	 * is either 1) direct computation (DirectComputation) or
	 * 2) the tuples that we are storing (StoredValue).
	 **/
	private HashMap compMap;
	/**
	 * mapping of tuples to an alpha numeric name (this is used
	 * to generate variable names in the replaced code.
	 **/
	private HashMap nameMap;
      

	/** constructor that will generate the appropriate data structures. **/
	RedundancyReplacerData(LinearRedundancy redundancy) {
	    super();

	    LinearPrinter.println("Linear redundancy:\n" + redundancy);
	    LinearPrinter.println("Linear Redundnacy tuples:\n" +
				  redundancy.getTupleString());
	    minUse  = new HashMap();
	    maxUse  = new HashMap();
	    compMap = new HashMap();
	    nameMap = new HashMap();

	    // calculate min and max use along with labeling all of the
	    // computation nodes with some string (a simple number).
	    // even though we know that the list the tuples map to are
	    // always sorted.
	    HashMap tuplesToUses = redundancy.getTuplesToUses();
	    Iterator tupleIter = tuplesToUses.keySet().iterator();
	    int tupleIndex = 0;
	    while(tupleIter.hasNext()) {
		Object tuple = tupleIter.next();
		Iterator useIter = ((List)tuplesToUses.get(tuple)).iterator();
		while(useIter.hasNext()) {
		    Integer currentUse = (Integer)useIter.next();
		    Integer oldMin = (Integer)this.minUse.get(tuple);
		    Integer oldMax = (Integer)this.maxUse.get(tuple);

		    // update min if necessary (eg if null or if current use is less than old use)
		    if ((oldMin == null) || (currentUse.intValue() < oldMin.intValue())) {
			this.minUse.put(tuple, currentUse);
		    }
		    // update min if necessary (eg if null or if current use is less than old use)
		    if ((oldMax == null) || (currentUse.intValue() > oldMax.intValue())) {
			this.maxUse.put(tuple, currentUse);
		    }
		}
		// label this tuple with "tup:tupleIndex" (and inc tupleIndex)
		this.nameMap.put(tuple, ("tup" + tupleIndex++));
	    }

	}


	/** for debugging -- text dump of the data structures. **/
	public String toString() {
	    String returnString = "minUse:\n";
	    returnString += hash2String(this.minUse);
	    returnString += "maxUse:\n";
	    returnString += hash2String(this.maxUse);
	    returnString += "compMap:\n";
	    returnString += hash2String(this.compMap);
	    returnString += "nameMap:\n";
	    returnString += hash2String(this.nameMap);
	    return returnString;
	}
	/** utility to convert a hash to a string. **/
	private String hash2String(HashMap m) {
	    String str = "";
	    Iterator keyIter = m.keySet().iterator();
	    while(keyIter.hasNext()) {
		Object key = keyIter.next();
		str += (key + "-->" +
			m.get(key) + "\n");
	    }
	    return str;
	}
	
    }
}

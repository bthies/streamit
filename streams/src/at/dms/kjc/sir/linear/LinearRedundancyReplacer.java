package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;
//import at.dms.compiler.*;


/**
 * RedundantReplacer.
 * $Id: LinearRedundancyReplacer.java,v 1.3 2003-02-28 22:34:52 aalamb Exp $
 **/
public class LinearRedundancyReplacer extends LinearReplacer implements Constants{
    /** The field that has all of the linear information necessary (eg streams->linear reps). **/
    LinearAnalyzer linearInformation;
    /** The field that has all of the redundancy information necessary to create replacements. **/
    LinearRedundancyAnalyzer redundancyInformation;
    
    /** Private constructor for the LinearRedundancyReplacer.**/
    LinearRedundancyReplacer(LinearAnalyzer lfa,
			     LinearRedundancyAnalyzer lra) {
	linearInformation = lfa;
	redundancyInformation = lra;
    }

    /** start the process of replacement on str using the Linearity information in lfa. **/
    public static void doReplace(LinearAnalyzer lfa,
				 LinearRedundancyAnalyzer lra,
				 SIRStream str) {
	LinearReplacer replacer = new LinearRedundancyReplacer(lfa, lra);
	// pump the replacer through the stream graph.
	IterFactory.createIter(str).accept(replacer);
    }

   
    /**
     * Visit a pipeline, splitjoin or filter, replacing them with a new filter
     * that directly implements the linear representation. This only
     * occurs if the replace calculator says that this stream should be replaced.
     **/
    public void makeReplacement(SIRStream self) {
	LinearPrinter.println("starting redundancy replacement for " + self);
	SIRContainer parent = self.getParent();
	if (parent == null) {
	    // we are done, this is the top level stream
	    LinearPrinter.println(" aborting, top level stream: " + self);
	    LinearPrinter.println(" stop.");
	    return;
	}
	LinearPrinter.println(" parent: " + parent);
	if (!this.linearInformation.hasLinearRepresentation(self)) {
	    LinearPrinter.println(" no linear information about: " + self);
	    LinearPrinter.println(" stop.");
	    return;
	}
	
	// pull out the linear representation
	LinearFilterRepresentation linearRep = linearInformation.getLinearRepresentation(self);

	/* if there is not a real valued FIR (all coefficients are real), we are done. */
	if (!linearRep.isPurelyReal()) {
	    LinearPrinter.println("  aborting -- filter has non real coefficients."); 
	    return;
	}


	
	/** calcluate the data structures that we need to generate the
	 * replaced filter's body. **/
	RedundancyReplacerData tupleData;
	LinearRedundancy redundancy = this.redundancyInformation.getRedundancy(self);
	tupleData = new RedundancyReplacerData(redundancy,linearRep.getPopCount());
	    
	System.err.println("\n\nRedundancy information calculated:");
	System.err.println(tupleData);
	System.err.println("\n");
	
	// since we need a two stage filter to implement this
	// transformation, we need to make a whole new SIRStream and
	// replace self with it.
	SIRStream newImplementation;
	newImplementation = makeEfficientImplementation(linearRep, tupleData);
	newImplementation.setParent(parent);
	// do the acutal replacment of the current pipeline with the new implementation
	parent.replace(self, newImplementation);
	
	LinearPrinter.println("Relative child name: " + newImplementation.getRelativeName());
	
	// remove the mappings from all of the children of this stream in our linearity information
	// first, we need to find them all, and then we need to remove them all
	HashSet oldKeys = getAllChildren(self);
	
	// now, remove the keys from the linear representation (self is also a "child")
	Iterator keyIter = oldKeys.iterator();
	while(keyIter.hasNext()) {
	    SIRStream currentKid = (SIRStream)keyIter.next();
	    LinearPrinter.println(" removing child: " + currentKid);
	    this.linearInformation.removeLinearRepresentation(currentKid);
	}
	// all done.
	
	// add a mapping from the new filter to the old linear rep (because it still computes
	// the same thing add same old linear rep
	this.linearInformation.addLinearRepresentation(newImplementation, linearRep);
    }


    SIRStream makeEfficientImplementation(LinearFilterRepresentation linearRep,
					  RedundancyReplacerData tupleData) {

	// We are tasked to make an efficient implementation using the tuple
	// data and the filter rep. This should be a lot of fun.

	// first of all, make the appropriate 


	return null;
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
	 * mapping of tuples to an alpha numeric name (this is used
	 * to generate variable names in the replaced code.
	 **/
	private HashMap nameMap;
	/**
	 * Mapping of Tuples to their StoredValue. We use a direct
	 * computation unless minUse(t)==0 and maxUse(t) >0, in which
	 * case we a stored value of the form of the form (use) where
	 * use is the number of work function executions ago that
	 * t was calculated.
	 **/
	private HashMap compMap;
	 

	/** constructor that will generate the appropriate data structures. **/
	RedundancyReplacerData(LinearRedundancy redundancy, int popCount) {
	    super();

	    LinearPrinter.println("Linear redundancy:\n" + redundancy);
	    LinearPrinter.println("Linear Redundnacy tuples:\n" +
				  redundancy.getTupleString());
	    minUse       = new HashMap();
	    maxUse       = new HashMap();
	    nameMap      = new HashMap();
	    compMap      = new HashMap();


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
	    
	    // now, we are going to compute the compMap information
	    tupleIter = tuplesToUses.keySet().iterator();
	    while(tupleIter.hasNext()) {
		LinearComputationTuple tuple = (LinearComputationTuple)tupleIter.next();
		// if this tuple is reused (eg minUse=0, maxUse>0) then add a mapping
		// from itself to itself.
		if ((this.getMinUse(tuple) == 0) && (this.getMaxUse(tuple) > 0)) {
		    this.compMap.put(tuple, new TupleCoupling(tuple, 0));
		}
		// get an iterator over the list of uses
		Iterator useIter = ((List)tuplesToUses.get(tuple)).iterator();
		while(useIter.hasNext()) {
		    int use = ((Integer)useIter.next()).intValue();
		    
		    // make new tuple corresponding to the calculation of this tuple
		    // in the first work function execution. If that new tuple has
		    // min use of zero (eg is actually calculated in the first
		    // work function) we add a mapping from the new tuple to the
		    // (original tuple, original use);
		    LinearComputationTuple newTuple;
		    newTuple = new LinearComputationTuple((tuple.getPosition() -
							   use*popCount),
							  tuple.getCoefficient());
		    if (this.getMinUse(newTuple) == 0) {
			// get any old use of this computation
			int oldUse = (this.compMap.containsKey(newTuple) ?
				      ((TupleCoupling)this.compMap.get(newTuple)).use :
				      0);
			// if the old use is greater than the current use,
			// then we want to use the older source
			if (oldUse < use) {
			    this.compMap.put(newTuple, new TupleCoupling(tuple, use));
			}
		    }
		}
	    }
	}

	////// Accessors
	/** get the int value of the min use for this tuple. **/
	public int getMinUse(LinearComputationTuple t) {
	    if (!(this.minUse.containsKey(t))) {
		throw new IllegalArgumentException("unknown tuple for min use: " + t);
	    }
	    return ((Integer)this.minUse.get(t)).intValue();
	}
	/** get the int value of the max use for this tuple. **/
	public int getMaxUse(LinearComputationTuple t) {
	    if (!(this.maxUse.containsKey(t))) {
		throw new IllegalArgumentException("unknown tuple for max use: " + t);
	    }
	    return ((Integer)this.maxUse.get(t)).intValue();
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

    /**
     * internal class that represents the coupling of a tuple to its value in
     * some number of work executions.
     **/
    class TupleCoupling {
	public LinearComputationTuple tuple;
	public int use;
	public TupleCoupling(LinearComputationTuple t, int u) {
	    this.tuple = t;
	    this.use = u;
	}
	public String toString() {
	    return ("(" + tuple + "," + use + ")");
	}
    }
				 
}

package at.dms.kjc.sir.statespace.transform;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import at.dms.kjc.sir.statespace.*;

/**
 * Represents a pipeline combination transform. Combines two filter that
 * come one after another in a pipeline into a single filter that does
 * the same work. This combination might require each of the individual
 * filters to be expanded by some factor, and then a matrix multiplication
 * can be performed.
 * 
 * $Id: LinearTransformPipeline.java,v 1.3 2004-02-24 19:56:36 sitij Exp $
 **/

public class LinearTransformPipeline extends LinearTransform {
    List repList;
    
    private LinearTransformPipeline(List l) {
	// assert that the list has more than one element in it.
	if (l.size() < 2) {
	    throw new IllegalArgumentException("Representation list has fewer than two elements: " +
					       l.size());
	}
	// assert that all arguments are LinearFilterRepresentations
	Iterator iter = l.iterator();
	while(iter.hasNext()) {
	    if (!(iter.next() instanceof LinearFilterRepresentation)) {
		throw new IllegalArgumentException("non LFR in list passed to linear transform pipeline");
	    }
	}
	this.repList = new LinkedList(l);
    }

    public LinearFilterRepresentation transform() throws NoTransformPossibleException {
    
	// we know that our rep list has at least two children in it.
	// start running down the rep list transforming things
	LinearFilterRepresentation rep1; // the current "upstream" filter
	LinearFilterRepresentation rep2; // the current "downstream" filter


	FilterMatrix A1,B1,C1,D1,A2,B2,C2,D2,Aprime,Bprime,Cprime,Dprime;
	FilterVector init1, init2, initprime;

	int newPop1, newPop2, newPush1, newPush2, state1, state2;

	Iterator repIter = this.repList.iterator();
	
	rep1 = (LinearFilterRepresentation)repIter.next();
	// iterate over all of the represenations
	while(repIter.hasNext()) {
	    rep2 = (LinearFilterRepresentation)repIter.next();

	    // pull out pop and push rates

	    int pop1 = rep1.getPopCount();
	    int push1 = rep1.getPushCount();
	    int pop2 = rep2.getPopCount();
	    int push2 = rep2.getPushCount();

	    int offset1 = rep1.getPeekCount() - pop1;
	    int offset2 = rep2.getPeekCount() - pop2;

	    // calculate the factors each representation is multiplied by

	    int total = lcm(push1,pop2);
	    int factor1 = total / push1;
	    int factor2 = total / pop2;


	    LinearPrinter.println("  expansion for upstream filter:(pop,push):" +
				  "(" + pop1 + "," + push1 + ")-->" +
				  "(" + pop1*factor1 + "," + push1*factor1 + ")");
	    LinearPrinter.println("  expansion for downstream filter:(pop,push):" +
				  "(" + pop2 + "," + push2 + ")-->" +
				  "(" + pop2*factor2 + "," + push2*factor2 + ")");

	    
	    // now, actually create the expanded reps.
	    LinearFilterRepresentation rep1Expanded = rep1.expand(factor1);
	    LinearFilterRepresentation rep2Expanded = rep2.expand(factor2);

	    // figure out the matrices of the combined rep
	    /* A' = [A1 0 ; B2*C1 A2]
	       B' = [B1 ; B2*D1]
	       C' = [D2*C1 ; C2]
	       D' = D2*D1;
            */

	    A1 = rep1Expanded.getA();
	    B1 = rep1Expanded.getB();
	    C1 = rep1Expanded.getC();
	    D1 = rep1Expanded.getD();
	    state1 = rep1Expanded.getStateCount();
	    newPop1 = rep1Expanded.getPopCount();
	    newPush1 = rep1Expanded.getPushCount();

	    A2 = rep2Expanded.getA();
	    B2 = rep2Expanded.getB();
	    C2 = rep2Expanded.getC();
	    D2 = rep2Expanded.getD();
	    state2 = rep2Expanded.getStateCount();
	    newPop2 = rep2Expanded.getPopCount();
	    newPush2 = rep2Expanded.getPushCount();

	    Aprime = new FilterMatrix(state1+state2,state1+state2);
	    Aprime.copyAt(0,0,A1);
	    Aprime.copyAt(state1,0,B2.times(C1));
	    Aprime.copyAt(state1,state1,A2);
	
	    Bprime = new FilterMatrix(state1+state2,newPop1);
	    Bprime.copyAt(0,0,B1);
	    Bprime.copyAt(state1,0,B2.times(D1));

	    Cprime = new FilterMatrix(newPush2,state1+state2);
	    Cprime.copyAt(0,0,D2.times(C1));
	    Cprime.copyAt(0,state1,C2);

	    Dprime = D2.times(D1);
	    	    
	    init1 = rep1.getInit();
	    init2 = rep2.getInit();

	    initprime = new FilterVector(state1+state2);
	    initprime.copyAt(0,0,init1);
	    initprime.copyAt(0,state1,init2);

	    // now, assemble the overall linear rep.
	    LinearFilterRepresentation combinedRep;
	    combinedRep = new LinearFilterRepresentation(Aprime, Bprime, Cprime, Dprime, initprime, newPop1 + offset1 + offset2);


	    LinearPrinter.println("Created new linear rep: \n" +
				  " peek=" + combinedRep.getPeekCount() + "\n" +
				  " pop=" + combinedRep.getPopCount() + "\n" +
				  " push=" + combinedRep.getPushCount());
				  

	    // now, we set the combined rep to be rep1 and repeat
	    rep1 = combinedRep;
	}

	// all we have to do is to return the combined rep and we are done
	return rep1;
    }
    

    

    /**
     * Sets up the calculation of the overall linear representation of
     * a sequential list of linear representations.<br>
     *
     * If filter one computes y = xA1 + b1 and filter 2 computes y=xA2 + b2 then
     * the overall filter filter1 --> filter 2 will compute
     * y = (xA1 + b1)A2 + b2 = xA1A2 + (b1A2 + b2), which itself can be represented  
     * with the LFR: A = A1A2 and b = (b1A2 + b2).<br>
     *
     * There are a bunch of subtlties involved with computing the overall representation
     * due to various size restrictions (eg the sizes of the matrices have to be
     * compatible. See the pldi-03-linear paper for the gory details.<p> 
     **/
    public static LinearTransform calculate(List linearRepList) {
	// we punt any actual work until the "transform" method is called.
	return new LinearTransformPipeline(linearRepList);
    }
}

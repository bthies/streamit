package at.dms.kjc.sir.statespace.transform;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import at.dms.kjc.sir.statespace.*;

/**
 * Represents a pipeline combination transform. Combines two filters that
 * come one after another in a pipeline into a single filter that does
 * the same work. This combination might require each of the individual
 * filters to be expanded by some factor, and then a matrix multiplication
 * can be performed.
 * 
 * $Id: LinearTransformPipeline.java,v 1.14 2004-04-28 19:51:11 sitij Exp $
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

	int pop1, push1, pop2, push2, total, factor1, factor2;
	int newPop1, newPop2, newPush1, newPush2, state1, state2;
	int newInputVars1, newInputVars2;

	boolean combinedPreWorkNeeded;
	FilterMatrix preworkA1, preworkB1, preworkA2, preworkB2, preworkAprime, preworkBprime;
	
	int preworkPop1, preworkPop2;

	Iterator repIter = this.repList.iterator();
	
	rep1 = (LinearFilterRepresentation)repIter.next();

	// iterate over all of the represenations
	while(repIter.hasNext()) {

	    rep2 = (LinearFilterRepresentation)repIter.next();


	    init1 = rep1.getInit();
	    init2 = rep2.getInit();

	    // pull out pop and push rates

	    pop1 = rep1.getPopCount();
	    push1 = rep1.getPushCount();
	    pop2 = rep2.getPopCount();
	    push2 = rep2.getPushCount();

	    // calculate the factors each representation is multiplied by

	    total = lcm(push1,pop2);
	    factor1 = total / push1;
	    factor2 = total / pop2;


	    LinearPrinter.println("  expansion for upstream filter:(pop,push):" +
				  "(" + pop1 + "," + push1 + ")-->" +
				  "(" + pop1*factor1 + "," + push1*factor1 + ")");
	    LinearPrinter.println("  expansion for downstream filter:(pop,push):" +
				  "(" + pop2 + "," + push2 + ")-->" +
				  "(" + pop2*factor2 + "," + push2*factor2 + ")");

	    
	    // now, actually create the expanded reps.

	    LinearFilterRepresentation rep1Expanded = rep1.expand(factor1);
	    LinearFilterRepresentation rep2Expanded = rep2.expand(factor2);

	    A1 = rep1Expanded.getA();
	    B1 = rep1Expanded.getB();
	    C1 = rep1Expanded.getC();
	    D1 = rep1Expanded.getD();
	    state1 = rep1Expanded.getStateCount();
	    newPop1 = rep1Expanded.getPopCount();
	    newPush1 = rep1Expanded.getPushCount();
	    newInputVars1 = rep1Expanded.getStoredInputCount();

	    A2 = rep2Expanded.getA();
	    B2 = rep2Expanded.getB();
	    C2 = rep2Expanded.getC();
	    D2 = rep2Expanded.getD();
	    state2 = rep2Expanded.getStateCount();
	    newPop2 = rep2Expanded.getPopCount();
	    newPush2 = rep2Expanded.getPushCount();
	    newInputVars2 = rep2Expanded.getStoredInputCount();

	    // now we must combine the initialization matrices, if necessary

	    if((rep1.preworkNeeded()==false)&&(rep2.preworkNeeded()==false)) {
		combinedPreWorkNeeded = false;   // no prework necessary

		preworkAprime = null;
		preworkBprime = null;
	    }
	    else {
		combinedPreWorkNeeded = true;

		//the easy case, just set the overall prework to be the first filter's prework

		if(rep2.preworkNeeded()==false) {
		    int tot = state1+state2;

		    preworkAprime = new FilterMatrix(tot,tot);
		    preworkAprime.copyAt(0,0,rep1.getPreWorkA());

		    for(int i=state1; i<tot; i++)
			preworkAprime.setElement(i,i,ComplexNumber.ONE);

		    preworkBprime = new FilterMatrix(tot,rep1.getPreWorkB().getCols());
		    preworkBprime.copyAt(0,0,rep1.getPreWorkB());
		}

		else {


		    /* 
The overall prework function must run filter1 n times, and place all of the outputs into states. 
n is the integer: push1*n < peek2 <= push1*(n+1). Note that n CANNOT be zero! 
This is due to the fact that push1 = pop2, and peek2 > pop2 
		    */
  
		    int n = 1;
		    int filter2ExtraInputsNeeded = rep2Expanded.getPreWorkPopCount();

		    while(push1*n < filter2ExtraInputsNeeded)
			n++;

		    int removeVars = newInputVars2;
		    int extraVars = push1*n;

		    LinearPrinter.println("n,old,new = " + n + " " + removeVars + " " + extraVars);

		    int newVar2Total = state2;

		    FilterMatrix preworkA2_new, preworkB2_new;
		    preworkA2_new = rep2Expanded.getPreWorkA();
		    preworkB2_new = rep2Expanded.getPreWorkB();

		    if(extraVars > removeVars) {
			newVar2Total = state2 - removeVars + extraVars;

			LinearFilterRepresentation newRep2;
			newRep2 = rep2Expanded.changeStoredInputs(extraVars);

			A2 = newRep2.getA();
			B2 = newRep2.getB();
			C2 = newRep2.getC();
			D2 = newRep2.getD();
			init2 = newRep2.getInit();
			
			preworkA2_new = newRep2.getPreWorkA();
			preworkB2_new = newRep2.getPreWorkB();

			state2 = newVar2Total;
		    
		    }

		    LinearFilterRepresentation rep1MoreExpanded = rep1.expand_with_prework(n);
		    FilterMatrix preworkA1_new = rep1MoreExpanded.getA();
		    FilterMatrix preworkB1_new = rep1MoreExpanded.getB();
	
		    FilterMatrix C1_temp = rep1MoreExpanded.getC();
		    FilterMatrix D1_temp = rep1MoreExpanded.getD();
 
		    preworkAprime = new FilterMatrix(state1+newVar2Total,state1+newVar2Total);
		    preworkAprime.copyAt(0,0,preworkA1_new);
		    preworkAprime.copyAt(state1,0,preworkB2_new.times(C1_temp));
		    preworkAprime.copyAt(state1,state1,preworkA2_new);

		    preworkBprime = new FilterMatrix(state1+newVar2Total,preworkB1_new.getCols());

		    preworkBprime.copyAt(0,0,preworkB1_new);
		    preworkBprime.copyAt(state1,0,preworkB2_new.times(D1_temp));

		    newInputVars1 = 0;

		}
	    }


	    // figure out the matrices of the combined rep
	    /* A' = [A1 0 ; B2*C1 A2]
	       B' = [B1 ; B2*D1]
	       C' = [D2*C1 ; C2]
	       D' = D2*D1;
            */

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

	    initprime = new FilterVector(state1+state2);
	    initprime.copyAt(0,0,init1);
	    initprime.copyAt(0,state1,init2);


	    // now, assemble the overall linear rep.
	    LinearFilterRepresentation combinedRep;

	    if(combinedPreWorkNeeded) {

		combinedRep = new LinearFilterRepresentation(Aprime,Bprime,Cprime,Dprime,preworkAprime,preworkBprime,newInputVars1,initprime);

	    }
	    else
		combinedRep = new LinearFilterRepresentation(Aprime,Bprime,Cprime,Dprime,newInputVars1,initprime);


	    LinearPrinter.println("Created new linear rep: \n" +
				  " inputvars =" + combinedRep.getStoredInputCount() + "\n" +
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
     **/
    public static LinearTransform calculate(List linearRepList) {
	// we punt any actual work until the "transform" method is called.
	return new LinearTransformPipeline(linearRepList);
    }
}






package at.dms.kjc.sir.statespace;

/* The optimizer has many subroutines. First, all non-zero initial states are eliminated by transformations.
 * 
 *
 *
 */

public class LinearOptimizer {

    FilterMatrix totalMatrix, totalPreMatrix, D;
    FilterVector initVec;
    int states, outputs, inputs, pre_inputs;
    int storedInputs;
    boolean preNeeded;

    /* extracts totalMatrix, totalPreMatrix, and other parameters from linear rep orig, 
       adding a constant state at the beginning

       totalMatrix = | A  B |     totalPreMatrix = | preA  preB |
                     | C  0 |

    */
    public LinearOptimizer(LinearFilterRepresentation orig) {
	
	states = orig.getStateCount() + 1;
	outputs = orig.getPushCount();
	inputs = orig.getPopCount();
	preNeeded = orig.preworkNeeded();

	totalMatrix = new FilterMatrix(states+outputs,states+inputs);
	totalMatrix.copyAt(1,1,orig.getA());
	totalMatrix.copyAt(1,states,orig.getB());
	totalMatrix.copyAt(states,1,orig.getC());
	totalMatrix.setElement(0,0,ComplexNumber.ONE);

	D = orig.getD();

	initVec = new FilterVector(states);
	initVec.copyAt(0,1,orig.getInit());
	initVec.setElement(0,ComplexNumber.ONE);

	storedInputs = orig.getStoredInputCount();

	if(preNeeded) {
	    pre_inputs = orig.getPreWorkPopCount();
	    totalPreMatrix = new FilterMatrix(states,states+pre_inputs);

	    totalPreMatrix.copyAt(1,1,orig.getPreWorkA());
	    totalPreMatrix.copyAt(1,states,orig.getPreWorkB());
	    totalPreMatrix.setElement(0,0,ComplexNumber.ONE);
	}
    }


    // main function that does all the optimizations
    public LinearFilterRepresentation optimize() {

	int s1, s2;

	zeroInitEntries();
	s1 = reduceParameters();
	LinearPrinter.println("got reduction up to value " + s1);

	if(s1 > 0) {
	    if(areRemovable(s1)) {
		LinearPrinter.println("Removable");
		removeStates(s1);
	    }
	}

	/*
		
	transposeSystem();
	s2 = reduceParameters();
	LinearPrinter.println("got reduction up to value " + s2);
	
	transposeSystem();
	*/
	
	return extractRep();
    }


    // extract representation from matrices
    private LinearFilterRepresentation extractRep() {

	LinearFilterRepresentation newRep;
	FilterMatrix A, B, C, preA, preB;

	A = new FilterMatrix(states,states);
	B = new FilterMatrix(states,inputs);
	C = new FilterMatrix(outputs,states);

	A.copyRowsAndColsAt(0,0,totalMatrix,0,0,states,states);
	B.copyRowsAndColsAt(0,0,totalMatrix,0,states,states,inputs);
	C.copyRowsAndColsAt(0,0,totalMatrix,states,0,outputs,states);

	if(preNeeded) {

	    preA = new FilterMatrix(states,states);
	    preB = new FilterMatrix(states,pre_inputs);

	    preA.copyRowsAndColsAt(0,0,totalPreMatrix,0,0,states,states);
	    preB.copyRowsAndColsAt(0,0,totalPreMatrix,0,states,states,pre_inputs);

	    newRep = new LinearFilterRepresentation(A,B,C,D,preA,preB,storedInputs,initVec);
	}
	else
	    newRep = new LinearFilterRepresentation(A,B,C,D,storedInputs,initVec);
	

	return newRep;
    }

    
    // requires that the first init vector entry is 1
    // makes all other init vector entries 0    
    private void zeroInitEntries() {

	double temp;

	for(int i=1; i<states; i++) {	    
	    if(!initVec.getElement(i).equals(ComplexNumber.ZERO)) {
		temp = initVec.getElement(i).getReal();
		addMultiple(0,i,-temp);
	    }
	}

	// now we remove the last state (we will add it at the end)
	/*
	FilterMatrix newA, newB, newC, newTotalMatrix;

	newA = new FilterMatrix(newStates,newStates);
	newB = new FilterMatrix(newStates,inputs);
	newC = new FilterMatrix(outputs,newStates);
	newTotalMatrix = new FilterMatrix(newStates+outputs,newStates+inputs);

	newA.copyRowsAndColsAt(0,0,totalMatrix,0,0,newStates,newStates);
	newB.copyRowsAndColsAt(0,0,totalMatrix,0,newStates+1,newStates,inputs);
	newC.copyRowsAndColsAt(0,0,totalMatrix,newStates+1,0,outputs,newStates);
	newTotalMatrix.copyAt(0,0,newA);
	newTotalMatrix.copyAt(0,newStates,newB);
	newTotalMatrix.copyAt(newStates,0,newC);

	if(preNeeded) {
	    FilterMatrix newPreA, newPreB, newTotalPreMatrix;

	    newPreA = new FilterMatrix(newStates,newStates);
	    newPreB = new FilterMatrix(newStates,pre_inputs);
            newTotalPreMatrix = new FilterMatrix(newStates,newStates+pre_inputs);

            newPreA.copyRowsAndColsAt(0,0,totalPreMatrix,0,0,newStates,newStates);
            newPreB.copyRowsAndColsAt(0,0,totalPreMatrix,0,newStates+1,newStates,pre_inputs);
            newTotalPreMatrix.copyAt(0,0,newPreA);
            newTotalPreMatrix.copyAt(0,newStates,newPreB);

            totalPreMatrix = newTotalPreMatrix; 
	}
	
        totalMatrix = newTotalMatrix;
	states = newStates;
	*/
    }


    // removes states  1..index
    private void removeStates(int index) {

	int newStates = states-index;

	FilterMatrix newTotalMatrix = new FilterMatrix(newStates+outputs,newStates+inputs);	
	int lastCols = states+inputs-(index+1);
	int lastRows = states+outputs-(index+1);

	newTotalMatrix.setElement(0,0,totalMatrix.getElement(0,0));
	
	newTotalMatrix.copyRowsAndColsAt(1,0,totalMatrix,index+1,0,lastRows,1);
	newTotalMatrix.copyRowsAndColsAt(1,1,totalMatrix,index+1,index+1,lastRows,lastCols);
	
	totalMatrix = newTotalMatrix;

	if(preNeeded) {
	    FilterMatrix newPreMatrix = new FilterMatrix(newStates,newStates+pre_inputs);
	    int lastPreCols = states+pre_inputs-(index+1);
	    int lastPreRows = states-(index+1);

	    
	    newPreMatrix.setElement(0,0,totalPreMatrix.getElement(0,0));;
	   
	    if(index < states-1) {
		newPreMatrix.copyRowsAndColsAt(1,0,totalPreMatrix,index+1,0,lastPreRows,1);
		newPreMatrix.copyRowsAndColsAt(1,1,totalPreMatrix,index+1,index+1,lastPreRows,lastPreCols);
	    }
	    totalPreMatrix = newPreMatrix;
	}

	FilterVector newInitVec = new FilterVector(newStates);

	newInitVec.setElement(0,initVec.getElement(0));
	if(index < states-1)
	    newInitVec.copyColumnsAt(1,initVec,index,states-(index+1));

	
	initVec = newInitVec;

	states = newStates;
    }


    // checks whether or not states 1..index are removable
    // assumes state 0 is the constant 1, 
    private boolean areRemovable(int index) {

	for(int i=1; i<= index; i++) {
	    // first check that each state has initial value 0
	    // note that this should automatically be true because we zeroed out these states earlier
	    if(!initVec.getElement(index).equals(ComplexNumber.ZERO))
		return false;

	    // check that each state doesn't get updated by state 0
	    // (we already know each state doesn't get updated by states greater than index)
	    if(!totalMatrix.getElement(i,0).equals(ComplexNumber.ZERO))
		return false;

	    if(preNeeded) {
		// check that each state doesn't get updated by state 0 in the prework matrix
		if(!totalPreMatrix.getElement(i,0).equals(ComplexNumber.ZERO))
		    return false;

		//check that each state doesn't get initialized by inputs
		for(int j=0; j<pre_inputs; j++) {
		    if(!totalPreMatrix.getElement(i,states+j).equals(ComplexNumber.ZERO))
			return false;
		}
	    }
	}
	
	// set of states passes all the tests, so they are removable
	return true;
    }


    private int reduceParameters() {

	int i = states-1;
	int j = states+inputs-1;
	int r;
	boolean found;

	while(i > 0) {
	    
	    found = false;

	    while((j>i)&&(!found)) {
		int k=i;

		while((k >= 0)&&(!found)) {
		    if(!totalMatrix.getElement(k,j).equals(ComplexNumber.ZERO))
			found = true;
		    k--;
		}

		if(!found)
		    j = j-1;
	    }

	    if(i == j)
		break;

	    LinearPrinter.println("hello " + totalMatrix.getElement(i,j));

	    if(totalMatrix.getElement(i,j).equals(ComplexNumber.ZERO)) { 

		LinearPrinter.println("here " + i + " " + j);
		r = i-1;
		while((r >= 0)&&(totalMatrix.getElement(r,j).equals(ComplexNumber.ZERO)))
		    r--;
	    
		if(r >= 0) 
		    swap(r,i);
		
	    }

	    double curr = totalMatrix.getElement(i,j).getReal();
	    
	    LinearPrinter.println("i,j,curr " + i + " " + j + " " + curr);

	    if(curr != 0.0) {
		scale(i,1/curr);
		for(int k=0; k<states; k++) {
		    if((!totalMatrix.getElement(k,j).equals(ComplexNumber.ZERO))&&(k!=i)) {
			double temp = totalMatrix.getElement(k,j).getReal();
			double val = -temp;
			
			addMultiple(i,k,val);					       
		    }
		}
	    }

	    i = i - 1; 
	    j = j - 1; 
	}

	if(i == j)
	    return i;

	else
	    return 0;
    }


    // swaps rows a,b and cols a,b in totalMatrix, totalPreMatrix
    // also swaps elements a,b in init vector
    private void swap(int a, int b) {

	totalMatrix.swapRowsAndCols(a,b);
	if(preNeeded) {
	    totalPreMatrix.swapRowsAndCols(a,b);
	}
	initVec.swapCols(a,b);
	
	LinearPrinter.println("SWAPPED " + a + " " + b);
    }


    // adds val*row a to row b, adds -val*col b to col a in totalMatrix, totalPreMatrix
    // also adds val*element a to element b in init vector
    private void addMultiple(int a, int b, double val) {

	totalMatrix.addRowAndCol(a,b,val);
	if(preNeeded) {
	    totalPreMatrix.addRowAndCol(a,b,val);			    
	}
	initVec.addCol(a,b,val);

	LinearPrinter.println("ADDED MULTIPLE " + a + " " + b + " " + val);			
    }

    // multiplies row a, col a by val in totalMatrix, totalPreMatrix
    // also multiplies element a by val in init vector
    private void scale(int a, double val) {

	totalMatrix.multiplyRowAndCol(a,val);
	if(preNeeded) {
	    totalPreMatrix.multiplyRowAndCol(a,val);			    
	}
	initVec.multiplyCol(a,val);

	LinearPrinter.println("SCALED " + a + " " + val);		

    }


    //transpose the matrices, and swap the values of inputs, outputs
    private void transposeSystem() {
	
	totalMatrix = totalMatrix.transpose();
	if(preNeeded)
	    totalPreMatrix = totalPreMatrix.transpose();

	int temp = outputs;
	outputs = inputs;
	inputs = temp;
    }







    public static LinearFilterRepresentation getObservableRep(LinearFilterRepresentation orig) {

	LinearFilterRepresentation transOrig = orig;

	int states = transOrig.getStateCount();
	int outputs = transOrig.getPushCount();
	int inputs = transOrig.getPopCount();
	int totalRows = states + outputs;
	int totalCols = states + inputs;
	boolean preNeeded = transOrig.preworkNeeded();

	FilterVector init = transOrig.getInit();

	FilterMatrix totalMatrix = new FilterMatrix(totalRows,totalCols);
	totalMatrix.copyAt(0,0,orig.getA());
	totalMatrix.copyAt(0,states,orig.getB());
	totalMatrix.copyAt(states,0,orig.getC());

	FilterMatrix totalPreMatrix = null;

	if(preNeeded) {
	    int totalPreCols = states + transOrig.getPreWorkPopCount();
	    totalPreMatrix = new FilterMatrix(states,totalPreCols);

	    totalPreMatrix.copyAt(0,0,transOrig.getPreWorkA());
	    totalPreMatrix.copyAt(0,states,transOrig.getPreWorkB());
	}


	int i = states-1;
	int j = totalCols-1;
	int r;
	boolean found;

	while(i > 0) {
	    
	    found = false;

	    while((j>i)&&(!found)) {
		int k=i;

		while((k >= 0)&&(!found)) {
		    if(!totalMatrix.getElement(k,j).equals(ComplexNumber.ZERO))
			found = true;
		    k--;
		}

		if(!found)
		    j = j-1;
	    }

	    if(i == j)
		break;

	    LinearPrinter.println("hello " + totalMatrix.getElement(i,j));

	    if(totalMatrix.getElement(i,j).equals(ComplexNumber.ZERO)) { 

		LinearPrinter.println("here " + i + " " + j);
		r = i-1;
		while((r >= 0)&&(totalMatrix.getElement(r,j).equals(ComplexNumber.ZERO)))
		    r--;
	    
		if(r >= 0) {
		    totalMatrix.swapRowsAndCols(r,i);
		    if(preNeeded) {
			totalPreMatrix.swapRowsAndCols(r,i);
		    }

		    init.swapCols(r,i);

		    LinearPrinter.println("SWAPPED " + i + " " + r);
		}
	    }

	    double curr = totalMatrix.getElement(i,j).getReal();
	    
	    LinearPrinter.println("i,j,curr " + i + " " + j + " " + curr);

	    if(curr != 0.0) {
		for(int k=0; k<i; k++) {
		    if(!totalMatrix.getElement(k,j).equals(ComplexNumber.ZERO)) {
			double temp = totalMatrix.getElement(k,j).getReal();
			double val = -temp/curr;
			totalMatrix.addRowAndCol(i,k,val);
			if(preNeeded) {
			    totalPreMatrix.addRowAndCol(i,k,val);			    
			}
			init.addCol(i,k,val);


			LinearPrinter.println("ADDED MULTIPLE " + i + " " + k + " " + val);
			
		    }
		}
	    }

	    i = i - 1; 
	    j = j - 1; 
	}


	int stateOffset = 0;
	int newStates;

	if(i==j) {
	    LinearPrinter.println("i = j = " + i);
	    if(i > 1)
		stateOffset = i;
	}
	newStates= states - stateOffset;

	FilterMatrix newA,newB,newC,newD;
	FilterVector newInit;
	LinearFilterRepresentation newRep;

	newA = new FilterMatrix(newStates,newStates);
	newB = new FilterMatrix(newStates,inputs);
	newC = new FilterMatrix(outputs,newStates);
	newD = orig.getD();

	newA.copyRowsAndColsAt(0,0,totalMatrix,stateOffset,stateOffset,newStates,newStates);
	newB.copyRowsAndColsAt(0,0,totalMatrix,stateOffset,states,newStates,inputs);
	newC.copyRowsAndColsAt(0,0,totalMatrix,states,stateOffset,outputs,newStates);

	newInit = new FilterVector(newStates);
	newInit.copyColumnsAt(0,init,stateOffset,newStates);


	/******************** non reduced matrices ***************/

	FilterMatrix tempA,tempB,tempC,tempD;
	FilterVector tempInit;
	LinearFilterRepresentation tempRep;

	tempA = new FilterMatrix(states,states);
	tempB = new FilterMatrix(states,inputs);
	tempC = new FilterMatrix(outputs,states);
	tempD = orig.getD();

	tempA.copyRowsAndColsAt(0,0,totalMatrix,0,0,states,states);
	tempB.copyRowsAndColsAt(0,0,totalMatrix,0,states,states,inputs);
	tempC.copyRowsAndColsAt(0,0,totalMatrix,states,0,outputs,states);

	tempInit = (FilterVector)init.copy();
	
	tempRep = new LinearFilterRepresentation(tempA,tempB,tempC,tempD,orig.getStoredInputCount(),tempInit);

	LinearPrinter.println("After optimization, before state reduction \n" + tempRep);

	/********************************************************/



	if(preNeeded) {

	    int preInputs = orig.getPreWorkPopCount();

	    FilterMatrix newPreA = new FilterMatrix(newStates,newStates);
	    FilterMatrix newPreB = new FilterMatrix(newStates,preInputs);

	    newPreA.copyRowsAndColsAt(0,0,totalPreMatrix,stateOffset,stateOffset,newStates,newStates);
	    newPreB.copyRowsAndColsAt(0,0,totalPreMatrix,stateOffset,states,newStates,preInputs);

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreA,newPreB,orig.getStoredInputCount(),newInit);

	}
	else
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,orig.getStoredInputCount(),newInit);

	return newRep;
    }


}



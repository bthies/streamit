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

	    qr_Algorithm(s1);
	    removeStates(s1);
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
    }


    private void removeStates(int end_index) {

	int temp_index = end_index;

	for(int i=end_index; i>0; i--) {
	    
	    if(isRemovable(i,temp_index)) {
		removeState(i);
		temp_index--;
	    }
	}

    }


    // removes state index
    private void removeState(int index) {

	int newStates = states-1;

	FilterMatrix newTotalMatrix = new FilterMatrix(newStates+outputs,newStates+inputs);	
	int lastCols = states+inputs-(index+1);
	int lastRows = states+outputs-(index+1);

        newTotalMatrix.copyRowsAndColsAt(0,0,totalMatrix,0,0,index,index);
	newTotalMatrix.copyRowsAndColsAt(0,index,totalMatrix,0,index+1,index,lastCols);
	
	newTotalMatrix.copyRowsAndColsAt(index,0,totalMatrix,index+1,0,lastRows,index);
	newTotalMatrix.copyRowsAndColsAt(index,index,totalMatrix,index+1,index+1,lastRows,lastCols);
	
	totalMatrix = newTotalMatrix;

	if(preNeeded) {
	    FilterMatrix newPreMatrix = new FilterMatrix(newStates,newStates+pre_inputs);
	    int lastPreCols = states+pre_inputs-(index+1);
	    int lastPreRows = states-(index+1);

	    newPreMatrix.copyRowsAndColsAt(0,0,totalPreMatrix,0,0,index,index);
	    newPreMatrix.copyRowsAndColsAt(0,index,totalPreMatrix,0,index+1,index,lastCols);
	   
	    if(index < states-1) {
		newPreMatrix.copyRowsAndColsAt(index,0,totalPreMatrix,index+1,0,lastPreRows,index);
		newPreMatrix.copyRowsAndColsAt(index,index,totalPreMatrix,index+1,index+1,lastPreRows,lastPreCols);
	    }
	    totalPreMatrix = newPreMatrix;
	}

	FilterVector newInitVec = new FilterVector(newStates);

	newInitVec.copyColumnsAt(0,initVec,0,index);

	if(index < states-1)
	    newInitVec.copyColumnsAt(index,initVec,index+1,states-(index+1));
	
	initVec = newInitVec;

	states = newStates;
    }


    // checks whether or not state index is removable 
    private boolean isRemovable(int index, int end_index) {

	// first check that the state has initial value 0
	// note that this should automatically be true because we zeroed out every state earlier
	if(!initVec.getElement(index).equals(ComplexNumber.ZERO))
	    return false;

	// check that state doesn't get updated by a later state
	// we already know each state doesn't get updated by states greater than end_index, and by earlier states
	for(int i=index+1; i<=end_index; i++) {
	    if(!totalMatrix.getElement(index,i).equals(ComplexNumber.ZERO))
		return false;
	}
	
	if(preNeeded) {
	    // check that state doesn't get updated by state 0 in the prework matrix
	    if(!totalPreMatrix.getElement(index,0).equals(ComplexNumber.ZERO))
		return false;

	    //check that state doesn't get initialized by inputs
	    for(int j=0; j<pre_inputs; j++) {
		if(!totalPreMatrix.getElement(index,states+j).equals(ComplexNumber.ZERO))
		    return false;
	    }
	}
		
	// state passes all the tests, so it is removable
	return true;
    }



    private void qr_Algorithm(int end_index) {

	off_diagonalize(end_index);

	FilterMatrix blockA = new FilterMatrix(end_index, end_index);
	FilterMatrix blockC = new FilterMatrix(outputs, end_index);
	FilterMatrix currInit = new FilterMatrix(end_index,1);
	blockA.copyRowsAndColsAt(0,0,totalMatrix,0,0,end_index,end_index);
	blockC.copyRowsAndColsAt(0,0,totalMatrix,states,0,outputs,end_index);

	for(int i=0; i<end_index;i++)
	    currInit.setElement(i,0,initVec.getElement(i));

	FilterMatrix QR, Q, R;

	Q = new FilterMatrix(end_index,end_index);
        R = new FilterMatrix(end_index,end_index);

	int total = 1000*end_index*end_index*end_index;

	for(int i=0; i<total; i++) {

	  QR = blockA.getQR();
	  Q.copyColumnsAt(0,QR,0,end_index);
	  R.copyColumnsAt(0,QR,end_index,end_index);
	  blockA = R.times(Q);

	  blockC = blockC.times(Q.transpose());
	  currInit = Q.times(currInit);
	}

	totalMatrix.copyRowsAndColsAt(0,0,blockA,0,0,end_index,end_index);
	totalMatrix.copyRowsAndColsAt(states,0,blockC,0,0,outputs,end_index);

	for(int i=0; i<end_index; i++)
	    initVec.setElement(i,currInit.getElement(i,0));
    }


    private void off_diagonalize(int end_index) {

	int j = 0;
	double curr, temp, val;
	boolean found = false;

	for(int i=0; i<end_index-1; i++) {

	    curr = totalMatrix.getElement(i+1,i).getReal();
	    if(curr == 0.0) {
		j = i+2;
		found = false;
		while((!found)&&(j<end_index)) {
		    temp = totalMatrix.getElement(j,i).getReal();
		    if(temp != 0.0) {
			swap(j,i+1);
			found = true;
		    }
		    j++;
		}
	    }
	    else {
		found = true;
	    }
		
	    if(found) {
		curr = totalMatrix.getElement(i+1,i).getReal();

		for(int k=i+2; k<end_index; k++) {

		    temp = totalMatrix.getElement(k,i).getReal();
		    if(temp != 0.0) {
			val = -temp/curr;
			addMultiple(i+1,k,val);
		    }
		}
		
	    }

	}
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



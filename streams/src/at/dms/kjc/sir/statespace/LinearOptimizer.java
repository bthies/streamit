package at.dms.kjc.sir.statespace;

/* The optimizer has many subroutines. First we isolate and remove unreachable and
 * unobservable states. Then we put the system in observable and reachable
 * canonical form. We do this to reduce the number of parameters (non-zero, 
 * non-one entries)
 *
 *
 * All the changes done to the matrices are done through row operations. Every time
 * a row operation is performed, the corresponding inverse column operation must be 
 * performed as well. We do partial pivoting (finding the maximum element in a subcolumn
 * to remove all elements below it) to ensure stability. However, removal of elements
 * ABOVE it is not necessarily stable! That's why we have the thresh variable as a check.
 * A value for thresh of 1 or 2 should ensure stability, but doesn't give a good performance 
 * gain. How to deal with this is perhaps an area of future work. Also, any scaling is also 
 * potentially unstable, and we use thresh to check that as well. Note that all this applies 
 * only to finding the canonical forms. For isolating the unreachable/unobservable states,
 * we don't need to remove elements above the sub-column and we don't need to do any scaling.
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

    /* extracts totalMatrix, totalPreMatrix, and other parameters from linear rep orig
       converts inputs to states, so:

       A' = |A B|   B' = |0|    C' = |C D|   D' = |0|
            |0 0|        |I|


       preA' = |preA 0|     preB' = |preB 0|    
               |0    0|             |0    I|    

       totalMatrix = |A'  B'|     totalPreMatrix = |preA'  preB'|
                     |C'  0 |

    */
    public LinearOptimizer(LinearFilterRepresentation orig, boolean doStoreInputs) {	
	
	/*
	  if the D matrix is not empty, and we've been told to store inputs,
	  put all inputs into states.

	  once the D matrix is empty, all the inputs have been stored
	*/
	if(doStoreInputs) {
	    int max = orig.getPopCount();
	    int counter = 0;    // this is just to make sure we don't get stuck in an infinite loop
	    while((!orig.getD().isZero())&&(counter<max)) {	   
		orig = orig.changeStoredInputs(orig.getStoredInputCount() + 1);
		counter++;
	    }	    
	}
								
	// staged execution - right now won't give a gain
	//boolean addExtraStages = true;
	boolean addExtraStages = false;
	int numStages = 2;
	
	if(addExtraStages) {
	    for(int i=0; i<numStages; i++)
		orig = orig.changeStoredInputs(orig.getStoredInputCount() + orig.getPopCount());  
	}
	

	LinearPrinter.println("AFTER ADDING INPUTS, #STATES: " + orig.getStateCount());
		
	//	LinearPrinter.println("After adding input states: " + orig);
	

	outputs = orig.getPushCount();
	inputs = orig.getPopCount();
	states = orig.getStateCount();
	preNeeded = orig.preworkNeeded();

	//save the 4 matrices into one large matrix
	totalMatrix = new FilterMatrix(states+outputs,states+inputs);
	totalMatrix.copyAt(0,0,orig.getA());
	totalMatrix.copyAt(0,states,orig.getB());
	totalMatrix.copyAt(states,0,orig.getC());
	totalMatrix.copyAt(states,states,orig.getD());
	D = orig.getD();

	initVec = new FilterVector(states);
	initVec.copyAt(0,0,orig.getInit());
	storedInputs = orig.getStoredInputCount();

	if(preNeeded) {
	    pre_inputs = orig.getPreWorkPopCount();
	    totalPreMatrix = new FilterMatrix(states,states+pre_inputs);

	    totalPreMatrix.copyAt(0,0,orig.getPreWorkA());
	    totalPreMatrix.copyAt(0,states,orig.getPreWorkB());
	}
    }

    // main function that does all the optimizations
    public LinearFilterRepresentation optimize() {

	int s1, s2;
	LinearFilterRepresentation tempRep;

	LinearPrinter.println("BEGIN");
						
	// isolate the unobservable states
	transposeSystem();
	s1 = rowEchelonForm(false);
	transposeSystem();	
	cleanAll();
	LinearPrinter.println("got reduction up to value " + s1);	

	// remove all possible unobservable states
	// however, there is an an extra constant state added		
	if((s1 >= 0)) {	   
	    zeroInitEntries(true); // the extra state added here will be removed later
	    cleanAll();
	    removeUnobservableStates(s1);
	}

	// remove the constant state, if it is not the only state remaining
	if(isRemovableLast()&&(states>1))
	    removeState(states-1);
	
	tempRep = extractRep();
	//	LinearPrinter.println("After observable reduction, before reachable reduction: \n" + tempRep); 

	// isolate the unreachable states 
	s2 = rowEchelonForm(true);
	cleanAll();
	LinearPrinter.println("got reduction up to value " + s2); 

	//	LinearPrinter.println("Before QR algorithm: \n" + totalMatrix + "\n" + initVec);
	
	// remove all possible unreachable states
	// however, there is an extra constant state added			
	if(s2 >= 0) {
	    LinearPrinter.println("INSIDE UNREACHABLE REMOVAL BLOCK");
	    qr_Algorithm(s2);  
	    cleanAll();  
	    zeroInitEntries(true); // the extra state added here will be removed later
	    cleanAll();  
	    removeUnreachableStates(s2); 
	}  		
	
	// remove the constant state, if it is not the only state remaining
	if(isRemovableLast()&&(states>1))
	    removeState(states-1);

	tempRep = extractRep();
	//	LinearPrinter.println("After state reduction, before min param: \n" + tempRep);
		
	// try to reduce parameters
					
	canonicalForm(true);      // put in reachable canonical form
	cleanAll();
	
	LinearFilterRepresentation tempReachableRep = extractRep();
	LinearCost tempReachableCost = tempReachableRep.getCost();
	
	LinearPrinter.println("Reachable rep: " + tempReachableRep);
	LinearPrinter.println("Cost (multiplies, adds): " + tempReachableCost.getMultiplies() + " " + tempReachableCost.getAdds());
	
	
	transposeSystem();
	canonicalForm(false);     // put in observable cononical form
	transposeSystem();
	cleanAll();
	    
	LinearFilterRepresentation tempObservableRep = extractRep();
	LinearCost tempObservableCost = tempObservableRep.getCost();
	
	
	LinearPrinter.println("Observable rep: " + tempObservableRep);	
	LinearPrinter.println("Cost (multiplies, adds): " + tempObservableCost.getMultiplies() + " " + tempObservableCost.getAdds());
	
	// compare reachable and observable forms, return the better one
	if(tempObservableCost.lessThan(tempReachableCost)) {
	    LinearPrinter.println("Observable rep is better");
	    return tempObservableRep;
	}
	else {
	    LinearPrinter.println("Reachable rep is better");
	    return tempReachableRep;
	}
				  
	//                     return extractRep();
    }


    // extract linear representation from total matrix
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

	    newRep = new LinearFilterRepresentation(A,B,C,D,preA,preB,0,initVec);
	}
	else
	    newRep = new LinearFilterRepresentation(A,B,C,D,0,initVec);
	

	return newRep;
    }


    // adds the constant state with value 1 at the end
    private void addConstantState() {

	int newStates = states+1;
	FilterMatrix newTotalMatrix;
	FilterVector newInitVec;

	newTotalMatrix = new FilterMatrix(newStates+outputs,newStates+inputs);
	newTotalMatrix.copyRowsAndColsAt(0,0,totalMatrix,0,0,states,states);
	newTotalMatrix.copyRowsAndColsAt(0,newStates,totalMatrix,0,states,states,inputs);
	newTotalMatrix.copyRowsAndColsAt(newStates,0,totalMatrix,states,0,outputs,states);
	newTotalMatrix.setElement(states,states,ComplexNumber.ONE);
	totalMatrix = newTotalMatrix;

	newInitVec = new FilterVector(newStates);
	newInitVec.copyAt(0,0,initVec);
	newInitVec.setElement(states,ComplexNumber.ONE);
	initVec = newInitVec;


	if(preNeeded) {
	    FilterMatrix newTotalPreMatrix;
	    newTotalPreMatrix = new FilterMatrix(newStates,newStates+pre_inputs);

	    newTotalPreMatrix.copyRowsAndColsAt(0,0,totalPreMatrix,0,0,states,states);
	    newTotalPreMatrix.copyRowsAndColsAt(0,newStates,totalPreMatrix,0,states,states,pre_inputs);
	    newTotalPreMatrix.setElement(states,states,ComplexNumber.ONE);
	    totalPreMatrix = newTotalPreMatrix;
	}

	states = newStates;
    }
    

    // add constant state 1
    // makes all other init vector entries zero
    private void zeroInitEntries(boolean normal) {
	addConstantState();
	
	ComplexNumber tempComplex;
	double temp;
	
	for(int i=0; i<states-1;i++) {
	    tempComplex = initVec.getElement(i);
	    if(!tempComplex.equals(ComplexNumber.ZERO)) {
		// do NOT do row operations with values too close to MAX_PRECISION
		// therefore, we will use MAX_PRECISION_BUFFER, which is greater	     
		if(Math.abs(tempComplex.getReal()) > ComplexNumber.MAX_PRECISION_BUFFER) {
		    temp = tempComplex.getReal();
		    if(normal)
			addMultiple(states-1,i,-temp,true);
		    else
			addMultiple(i,states-1,temp,false);
		    // set the value to be exactly zero (in case it is very small but non-zero)
		    initVec.setElement(i,ComplexNumber.ZERO);
		}	
	    }
	}
    }

    
    // rounds all entries which are very close to integral values
    private void cleanAll() {
	totalMatrix.cleanEntries();
	initVec.cleanEntries();
	if(preNeeded)
	    totalPreMatrix.cleanEntries();
    }



    // check if the last state (constant 1) can be removed
    private boolean isRemovableLast() {

	// check if other states depend on it
	for(int i=0; i<states-1; i++) {
	    if(!totalMatrix.getElement(i,states-1).equals(ComplexNumber.ZERO))
		return false;
	}

	// check if outputs depend on it
	for(int i=0; i<outputs; i++) {
	    if(!totalMatrix.getElement(states+i,states-1).equals(ComplexNumber.ZERO))
		return false;
	}

	// check if other states in prematrix depend on it
	if(preNeeded) {
	    for(int i=0; i<states-1; i++) {
		if(!totalPreMatrix.getElement(i,states-1).equals(ComplexNumber.ZERO))
		    return false;
	    }
	}

	return true;
    }


    // removes all possible (unreachable) states in the range 0..end_index
    private void removeUnreachableStates(int end_index) {

	int temp_index = end_index;

	for(int i=end_index; i>=0; i--) {
	    
	    if(isRemovableReach(i,temp_index)) {
		removeState(i);
		temp_index--;
	    }
	    else
		LinearPrinter.println("Did Not Remove Unreachable State");
	}
    }

    // checks whether or not state index is removable (for an unreachable state) 
    private boolean isRemovableReach(int index, int end_index) {

	// first check that the state has initial value 0
	if(!initVec.getElement(index).equals(ComplexNumber.ZERO))
	    return false;

	// check that state doesn't get updated by last state (which is the constant 1)
	if(!totalMatrix.getElement(index,states-1).equals(ComplexNumber.ZERO))
	    return false;

	// check that state doesn't get updated by a later state
	// we already know each state doesn't get updated by states greater than end_index, and by earlier states
	for(int i=index+1; i<=end_index; i++) {
	    if(!totalMatrix.getElement(index,i).equals(ComplexNumber.ZERO))
		return false;
	}
	
	if(preNeeded) {
	    //check that state doesn't get initialized by inputs
	    for(int j=0; j<pre_inputs; j++) {
		if(!totalPreMatrix.getElement(index,states+j).equals(ComplexNumber.ZERO))
		    return false;
	    }
	}
		
	// state passes all the tests, so it is removable
	return true;
    }


    // removes all possible (unobservable) states in the range 0..end_index
    private void removeUnobservableStates(int end_index) {

	int temp_index = end_index;

	for(int i=end_index; i>=0; i--) {
	    
	    if(isRemovableObs(i,temp_index)) {
		removeState(i);
		temp_index--;
	    }
	    else
		LinearPrinter.println("Did not remove Unobservable state");
	}
    }

    // checks whether or not state index is removable (for an unobservable state)
    private boolean isRemovableObs(int index, int end_index) {
	
	// if this unobservable state has initial value zero, we can definitely remove it
	if(initVec.getElement(index).equals(ComplexNumber.ZERO))
	    return true;

	// if not, check that no observable state updated by this (unobservable) state in the prework matrix A
	if(preNeeded) {
	    for(int i=end_index+1; i<states; i++) 
		if(!(totalPreMatrix.getElement(i,index).equals(ComplexNumber.ZERO)))
		    return false;
	    
	}
	
	return true;
    }


    // removes state index
    private void removeState(int index) {

	int newStates = states-1;

	FilterMatrix newTotalMatrix = new FilterMatrix(newStates+outputs,newStates+inputs);	
	int lastCols = states+inputs-(index+1);
	int lastRows = states+outputs-(index+1);

	if(index > 0) {
	    newTotalMatrix.copyRowsAndColsAt(0,0,totalMatrix,0,0,index,index);
	    newTotalMatrix.copyRowsAndColsAt(0,index,totalMatrix,0,index+1,index,lastCols);
	    newTotalMatrix.copyRowsAndColsAt(index,0,totalMatrix,index+1,0,lastRows,index);
	}

	newTotalMatrix.copyRowsAndColsAt(index,index,totalMatrix,index+1,index+1,lastRows,lastCols);
	
	totalMatrix = newTotalMatrix;

	if(preNeeded) {
	    FilterMatrix newPreMatrix = new FilterMatrix(newStates,newStates+pre_inputs);
	    int lastPreCols = states+pre_inputs-(index+1);
	    int lastPreRows = states-(index+1);

	    if(index > 0) {
		newPreMatrix.copyRowsAndColsAt(0,0,totalPreMatrix,0,0,index,index);
		newPreMatrix.copyRowsAndColsAt(0,index,totalPreMatrix,0,index+1,index,lastPreCols);
	    }

	    if(index < states-1) {
		if(index > 0)
		    newPreMatrix.copyRowsAndColsAt(index,0,totalPreMatrix,index+1,0,lastPreRows,index);
		newPreMatrix.copyRowsAndColsAt(index,index,totalPreMatrix,index+1,index+1,lastPreRows,lastPreCols);
	    }
	    totalPreMatrix = newPreMatrix;
	}

	FilterVector newInitVec = new FilterVector(newStates);

	if(index > 0)
	    newInitVec.copyColumnsAt(0,initVec,0,index);

	if(index < states-1)
	    newInitVec.copyColumnsAt(index,initVec,index+1,states-(index+1));
	
	initVec = newInitVec;

	states = newStates;
	
	LinearPrinter.println("Removed state ");
    }


    // does the qr algorithm on A[0..end_index, 0..end_index]
    // also does the appropriate transform to matrix C
    // don't need to do anything to matrix B because the relevant part of it is all zeros
    private void qr_Algorithm(int end_index) {

	LinearPrinter.println("Start Off Diagonalize");
	off_diagonalize(end_index);
	LinearPrinter.println("Finished Off Diagonalize");

	int total_entries = end_index + 1;

	FilterMatrix blockA = new FilterMatrix(total_entries, total_entries);
	blockA.copyRowsAndColsAt(0,0,totalMatrix,0,0,total_entries,total_entries);

	FilterMatrix A12 = null;
	FilterMatrix A21 = null;
	int remain_entries = states-total_entries;
	if(remain_entries > 0) {
	    A12 = new FilterMatrix(total_entries,remain_entries);
	    A21 = new FilterMatrix(remain_entries,total_entries);
	    A12.copyRowsAndColsAt(0,0,totalMatrix,0,total_entries,total_entries,remain_entries);
	    A21.copyRowsAndColsAt(0,0,totalMatrix,total_entries,0,remain_entries,total_entries);
	}

	FilterMatrix blockC = new FilterMatrix(outputs, total_entries);
	blockC.copyRowsAndColsAt(0,0,totalMatrix,states,0,outputs,total_entries);

	FilterMatrix currInit = new FilterMatrix(total_entries,1);

	FilterMatrix blockPreA, blockPreB;
	blockPreA = null;
	blockPreB = null;
	if(preNeeded) {
	    blockPreA = new FilterMatrix(total_entries, total_entries);
	    blockPreB = new FilterMatrix(total_entries, pre_inputs);
	    blockPreA.copyRowsAndColsAt(0,0,totalPreMatrix,0,0,total_entries,total_entries);
	    blockPreB.copyRowsAndColsAt(0,0,totalPreMatrix,0,states,total_entries,pre_inputs);
	}

	for(int i=0; i<total_entries;i++)
	    currInit.setElement(i,0,initVec.getElement(i));

	FilterMatrix QR, Q, Q_trans, R;

	Q = new FilterMatrix(total_entries,total_entries);
	Q_trans = new FilterMatrix(total_entries,total_entries);
        R = new FilterMatrix(total_entries,total_entries);

	//int total = 1000*total_entries*total_entries*total_entries;
	int total = 1000*total_entries;

	LinearPrinter.println("total_entries, all states " + total_entries + " " + states);
	LinearPrinter.println("total iterations: " + total);

	for(int i=0; i<total; i++) {
	  QR = blockA.getQR();
	  Q.copyColumnsAt(0,QR,0,total_entries);
	  Q_trans = Q.transpose();
	  R.copyColumnsAt(0,QR,total_entries,total_entries);

	  /*
	  LinearPrinter.println("Block A: \n" + blockA);
	  LinearPrinter.println("Q*R: \n" + Q.times(R));
	  LinearPrinter.println("Q*Q': \n" + Q.times(Q_trans));
	  LinearPrinter.println("Q, R: \n" + Q + "\n" + R);
	  */
	  blockA = R.times(Q);

	  if(remain_entries > 0) {
	      A12 = Q_trans.times(A12);
	      A21 = A21.times(Q);
	  }

	  blockC = blockC.times(Q);
	  currInit = Q_trans.times(currInit);

	  if(preNeeded) {
	      blockPreA = Q_trans.times(blockPreA.times(Q));
	      blockPreB = Q_trans.times(blockPreB);
	  }

	}

	totalMatrix.copyRowsAndColsAt(0,0,blockA,0,0,total_entries,total_entries);
	totalMatrix.copyRowsAndColsAt(states,0,blockC,0,0,outputs,total_entries);

	if(remain_entries > 0) {
	    totalMatrix.copyRowsAndColsAt(0,total_entries,A12,0,0,total_entries,remain_entries);
	    totalMatrix.copyRowsAndColsAt(total_entries,0,A21,0,0,remain_entries,total_entries);
	}

	if(preNeeded) {
	    totalPreMatrix.copyRowsAndColsAt(0,0,blockPreA,0,0,total_entries,total_entries);
	    totalPreMatrix.copyRowsAndColsAt(0,states,blockPreB,0,0,total_entries,pre_inputs);
	}

	for(int i=0; i<total_entries; i++)
	    initVec.setElement(i,currInit.getElement(i,0));
    }


    // zeros out entries below the "off-diagonal" in A[0..end_index, 0..index]
    // the off-diagonal is the diagonal below the main diagonal
    // this is a necessary procedure to start the QR algorithm
    private void off_diagonalize(int end_index) {

	double curr, temp, val;
	int max_index; 
	double max_val, temp_val;
	ComplexNumber tempComplex;

	for(int i=0; i<end_index; i++) {

	    //partial pivoting
	    max_index = i+1;
	    max_val = Math.abs(totalMatrix.getElement(i+1,i).getReal());
	    for(int l=i+2; l<=end_index; l++) {
		temp_val = Math.abs(totalMatrix.getElement(l,i).getReal());
		if(temp_val > max_val) {
		    max_val = temp_val;
		    max_index=l;
		}
	    }
	    swap(max_index,i+1);

		
	    if(!totalMatrix.getElement(i+1,i).equals(ComplexNumber.ZERO)) {
		curr = totalMatrix.getElement(i+1,i).getReal();

		for(int k=i+2; k<=end_index; k++) {
		    tempComplex = totalMatrix.getElement(k,i);
		    if((!tempComplex.equals(ComplexNumber.ZERO))) {
			// do NOT do row operations with values too close to MAX_PRECISION
			// therefore, we will use MAX_PRECISION_BUFFER, which is greater			    
			if(Math.abs(tempComplex.getReal()) > ComplexNumber.MAX_PRECISION_BUFFER) {			
			    temp = tempComplex.getReal();
			    val = -temp/curr;
			    addMultiple(i+1,k,val,true);
			    
			    // set the value to be exactly zero (in case it is very small but non-zero)
			    totalMatrix.setElement(k,i,ComplexNumber.ZERO);
			}
		    }
		}

	    }	    
	}
    }


    /* 
       puts [A B] ([A^T C^T) in row echelon form, which exposes the 
       unreachable (unobservable) states
       
       returns integer s indicating the states 0...s are unreachable (unobservable)
       if normal = true, we're finding unreachable states
       otherwise, we're finding unobservable states
    */
    private int rowEchelonForm(boolean normal) {

	int i = states-1;
	int j = states+inputs-1;
	int r;
	boolean found;

	int max_index; double max_val, temp_val;
	double temp, val, curr;
	ComplexNumber tempComplex;

	while(i >= 0) {
	    
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

	    //	    LinearPrinter.println("hello " + totalMatrix.getElement(i,j));


	    //partial pivoting for stability
	    max_index = i;
	    max_val = Math.abs(totalMatrix.getElement(i,j).getReal());
	    for(int l=0; l<i; l++) {
		temp_val = Math.abs(totalMatrix.getElement(l,j).getReal());
		if(temp_val > max_val) {
		    max_val = temp_val;
		    max_index=l;
		}
	    }
	    swap(max_index,i);

	    curr = totalMatrix.getElement(i,j).getReal();
	    
	    //	    LinearPrinter.println("i,j,curr " + i + " " + j + " " + curr);

	    if(!totalMatrix.getElement(i,j).equals(ComplexNumber.ZERO)) {
		for(int k=0; k<i; k++) {
		    tempComplex = totalMatrix.getElement(k,j);
		    if((!tempComplex.equals(ComplexNumber.ZERO))) {
			// do NOT do row operations with values too close to MAX_PRECISION
			// therefore, we will use MAX_PRECISION_BUFFER, which is greater			    
			if(Math.abs(tempComplex.getReal()) > ComplexNumber.MAX_PRECISION_BUFFER) {			
			    temp = tempComplex.getReal();
			    val = -temp/curr;
			    addMultiple(i,k,val,normal);
		       
			    // set the value to be exactly zero (in case it is very small but non-zero)
			    totalMatrix.setElement(k,j,ComplexNumber.ZERO);
			}
		    }
	    
		}
	    }

	    i = i - 1; 
	    j = j - 1; 
	}

	if(i == j)
	    return i;

	else
	    return -1;
    }


    /* 
       put the matrices into observable (reachable) canonical form
       if normal = true, put into reachable form
       otherwise, put into observable form
    */
    private void canonicalForm(boolean normal) {

	int currRow = 0;
	int currCol = 0;
	int currStage = 0;
	int max_index;
	boolean found;
	double temp_val, max_val, div;
	ComplexNumber tempComplex;

	/* 
	   This value should be just above 1 for stability purposes.
	   However, then there is much less gain in performance.
	   The value 10000 works well for most applications
	*/				       
	double thresh = 10000.0001;

	while((currRow < states)&&(currStage < inputs)) {
	    
	    found = false;

	    // find largest non-zero entry in the B matrix column and swap with the current row
	    // (partial pivoting)

	    max_index = currRow;
	    max_val = Math.abs(totalMatrix.getElement(currRow,states+currStage).getReal());
	    for(int i=currRow+1; i<states; i++) {
		temp_val = Math.abs(totalMatrix.getElement(i,states+currStage).getReal());
		if(temp_val > max_val) {
		    max_val = temp_val;
		    max_index=i;
		}
	    }
	    swap(currRow,max_index);
	    if(Math.abs(totalMatrix.getElement(currRow,states+currStage).getReal()) > ComplexNumber.MAX_PRECISION_BUFFER)
		found = true;
	    else
		found = false;

	    
	    if(found) { 

		temp_val = totalMatrix.getElement(currRow,states+currStage).getReal();
	    
		// make all entries above it to zero IF they aren't much bigger
		for(int i=0; i<currRow; i++) { 
		    tempComplex = totalMatrix.getElement(i,states+currStage);
		    if(!tempComplex.equals(ComplexNumber.ZERO)) {
			// do NOT do row operations with values too close to MAX_PRECISION
			// therefore, we will use MAX_PRECISION_BUFFER, which is greater	     
			if(Math.abs(tempComplex.getReal()) > ComplexNumber.MAX_PRECISION_BUFFER) {
			    div = -tempComplex.getReal()/temp_val;
			    if(Math.abs(div) < thresh) {
				addMultiple(currRow,i,div,normal);
				// set the value to be exactly zero (in case it is very small but non-zero)
				totalMatrix.setElement(i,states+currStage,ComplexNumber.ZERO);
			    }
			}
		    }
		    
		}

		// make all entries below it to zero
		for(int i=currRow+1; i<states; i++) { 
		    tempComplex = totalMatrix.getElement(i,states+currStage);
		    if(!tempComplex.equals(ComplexNumber.ZERO)) {
			// do NOT do row operations with values too close to MAX_PRECISION
			// therefore, we will use MAX_PRECISION_BUFFER, which is greater	     
			if(Math.abs(tempComplex.getReal()) > ComplexNumber.MAX_PRECISION_BUFFER) {
			    addMultiple(currRow,i,-tempComplex.getReal()/temp_val,normal);
			    // set the value to be exactly zero (in case it is very small but non-zero)
			    totalMatrix.setElement(i,states+currStage,ComplexNumber.ZERO);
			}
		    }
		}
		if(Math.abs(1.0/temp_val) < thresh) 
		    scale(currRow,1.0/temp_val,normal);
	    }

	    currRow++;
	    currStage++;

	    // now go diagonally down from the next row

	    while(found && (currRow < states) && (currCol < states)) {

		found = false;

		// find largest non-zero entry in the A matrix column and swap with the current row
		// (partial pivoting)
                max_index = currRow;
		max_val = Math.abs(totalMatrix.getElement(currRow,currCol).getReal());
		for(int i=currRow+1; i<states; i++) {
		    temp_val = Math.abs(totalMatrix.getElement(i,currCol).getReal());
		    if(temp_val > max_val) {
			max_val = temp_val;
			max_index=i;
		    }
		}
		swap(currRow,max_index);
		//		LinearPrinter.println("BIGGEST VALUE: " + totalMatrix.getElement(currRow,currCol).getReal());
		if(Math.abs(totalMatrix.getElement(currRow,currCol).getReal()) > ComplexNumber.MAX_PRECISION_BUFFER)
		    found = true;
		else
		    found = false;

		if(found) {
		    
		    temp_val = totalMatrix.getElement(currRow,currCol).getReal(); 
		    
		    // make all entries above it to zero IF they aren't much bigger
		    for(int i=0; i<currRow; i++) {
			tempComplex = totalMatrix.getElement(i,currCol);
			if(!tempComplex.equals(ComplexNumber.ZERO)) {
			    // do NOT do row operations with values too close to MAX_PRECISION
			    // therefore, we will use MAX_PRECISION_BUFFER, which is greater
			    if(Math.abs(tempComplex.getReal()) > ComplexNumber.MAX_PRECISION_BUFFER) {
				//	   		LinearPrinter.println("GETTING RID OF: " + tempComplex.getReal());
				div = -tempComplex.getReal()/temp_val;
				if(Math.abs(div) < thresh) {
				    addMultiple(currRow,i,div,normal);	
				    // 	    LinearPrinter.println("VALUE: " + totalMatrix.getElement(i,currCol).getReal());
				    // set the value to be exactly zero (in case it is very small but non-zero)
				    totalMatrix.setElement(i,currCol,ComplexNumber.ZERO);
				}
			    }
			}			    
		    }


		    // make all entries below it to zero
		    for(int i=currRow+1; i<states; i++) {
			tempComplex = totalMatrix.getElement(i,currCol);
			if(!tempComplex.equals(ComplexNumber.ZERO)) {
			    // do NOT do row operations with values too close to MAX_PRECISION
			    // therefore, we will use MAX_PRECISION_BUFFER, which is greater
			    if(Math.abs(tempComplex.getReal()) > ComplexNumber.MAX_PRECISION_BUFFER) {
				//	LinearPrinter.println("GETTING RID OF: " + tempComplex.getReal());
				addMultiple(currRow,i,-tempComplex.getReal()/temp_val,normal);	
				//LinearPrinter.println("VALUE: " + totalMatrix.getElement(i,currCol).getReal());
				// set the value to be exactly zero (in case it is very small but non-zero)
				totalMatrix.setElement(i,currCol,ComplexNumber.ZERO);	       
			    }
			}
		    }
		    if(Math.abs(1.0/temp_val) < thresh) 
			scale(currRow,1.0/temp_val,normal);
		    
		    currRow++;
		}

		currCol++;
	    }
		  
	}
    }


    // swaps rows a,b and cols a,b in totalMatrix, totalPreMatrix
    // also swaps elements a,b in init vector
    private void swap(int a, int b) {

	totalMatrix.swapRowsAndCols(a,b);
	if(preNeeded) {
	    totalPreMatrix.swapRowsAndCols(a,b);
	}
	initVec.swapCols(a,b);
	
	//	LinearPrinter.println("SWAPPED " + a + " " + b);
    }


    // adds val*row a to row b, adds -val*col b to col a in totalMatrix, totalPreMatrix
    // also adds val*element a to element b in init vector
    private void addMultiple(int a, int b, double val, boolean normal) {

	totalMatrix.addRowAndCol(a,b,val);
	if(preNeeded) {
	    totalPreMatrix.addRowAndCol(a,b,val);
	}

	if(normal)
	    initVec.addCol(a,b,val);
	else
	    initVec.addCol(b,a,-val);

	//	LinearPrinter.println("ADDED MULTIPLE " + a + " " + b + " " + val);			
    }


    // multiplies row a, col a by val in totalMatrix, totalPreMatrix
    // also multiplies element a by val in init vector
    private void scale(int a, double val, boolean normal) {

	totalMatrix.multiplyRowAndCol(a,val);
	if(preNeeded) {
	    totalPreMatrix.multiplyRowAndCol(a,val);			    
	}
	
	if(normal)
	    initVec.multiplyCol(a,val);
	else
	    initVec.multiplyCol(a,1.0/val);

	//	LinearPrinter.println("SCALED " + a + " " + val);		
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

}









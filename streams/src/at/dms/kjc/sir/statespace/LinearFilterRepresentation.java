package at.dms.kjc.sir.statespace;

/**
 * A LinearFilterRepresentation represents the computations performed by a filter
 * on its input values as four matrices. 
 *
 * This class holds the A, B, C, D in the equations y = Ax+Bu, x' = Cx + Du which calculates the output
 * vector y and new state vector x' using the input vector u and the old state vector x.<br>
 *
 * This class also holds initial matrices initA, initB that are to be used to 
 * update the state exactly ONCE (for a prework function).
 *
 * $Id: LinearFilterRepresentation.java,v 1.20 2004-08-27 20:16:42 sitij Exp $
 * Modified to state space form by Sitij Agrawal  2/9/04
 **/


public class LinearFilterRepresentation {
    /** the A in y=Ax+Bu. **/
    private FilterMatrix A;
    /** the B in y=Ax+Bu. **/
    private FilterMatrix B;
    /** the C in x'=Cx+Du. **/
    private FilterMatrix C;
    /** the D in x'=Cx+Du. **/
    private FilterMatrix D;

    /** the initial state values. **/
    private FilterVector initVec;

    /** a flag for whether a prework stage is needed **/
    private boolean preworkNeeded;

    /** the A in y=Ax+Bu (prework). **/
    private FilterMatrix preworkA;
    /** the B in y=Ax+Bu (prework). **/
    private FilterMatrix preworkB;

    /** the cost of this node */
    private LinearCost cost;

    /**
     * this represents how many states are used to store inputs
     * the FIRST storedInputs states store the inputs, so state 0 represents peek(0),
     * state 1 represents peek(1), state storedInputs-1 represents peek(storedInputs-1)
     **/
    private int storedInputs;

    /**
     * Create a new linear filter representation with matrices A, B, C, and D.
     * Note that we use a copy of all matrices so that we don't end up with
     * an aliasing problem 
     **/
    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterMatrix matrixB,
				      FilterMatrix matrixC,
				      FilterMatrix matrixD,
				      int storedInputs,
				      FilterVector vec) {

	// lots of size checking
	if(matrixA.getRows() != matrixA.getCols())
	    throw new IllegalArgumentException("Matrix A must be square");
	if(matrixA.getRows() != matrixB.getRows())
	    throw new IllegalArgumentException("Matrices A,B must have same number of rows");
	if(matrixB.getCols() != matrixD.getCols())
	    throw new IllegalArgumentException("Matrices B,D must have same number of cols");
	if(matrixC.getRows() != matrixD.getRows())
	    throw new IllegalArgumentException("Matrices C,D must have same number of rows");
	if(matrixC.getCols() != matrixA.getCols())
	    throw new IllegalArgumentException("Matrices A,C must have same number of cols");
	if(vec.getSize() != matrixA.getRows())
	    throw new IllegalArgumentException("initial vector must be same size as A");
	if(storedInputs > matrixA.getRows())
	    throw new IllegalArgumentException("cant have more input states than total states");
	

	this.A = matrixA.copy();
	this.B = matrixB.copy();
	this.C = matrixC.copy();
	this.D = matrixD.copy();
	this.initVec = (FilterVector)vec.copy();	
	this.storedInputs = storedInputs;

	this.A.cleanEntries();
	this.B.cleanEntries();
	this.C.cleanEntries();
	this.D.cleanEntries();
	this.initVec.cleanEntries();


	if(storedInputs == 0) {  // no separate initialization needed
	    this.preworkNeeded = false;
	}
	else {
	    int stateCount = this.getStateCount();

	    // preworkA is the identity
	    this.preworkA = this.createPreWorkA(stateCount);

	    // preworkB puts the inputs into the extra states
	    this.preworkB = this.createPreWorkB(stateCount,storedInputs);
	    
	    this.preworkNeeded = true;
	}

	// we calculate cost on demand (with the linear partitioner)
	this.cost = null;
    }


    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterMatrix matrixB,
				      FilterMatrix matrixC,
				      FilterMatrix matrixD,
				      FilterMatrix preworkA,
				      FilterMatrix preworkB,
				      int storedInputs,
				      FilterVector vec) {

	// lots of size checking
	if(matrixA.getRows() != matrixA.getCols())
	    throw new IllegalArgumentException("Matrix A must be square");
	if(matrixA.getRows() != matrixB.getRows())
	    throw new IllegalArgumentException("Matrices A,B must have same number of rows");
	if(matrixB.getCols() != matrixD.getCols())
	    throw new IllegalArgumentException("Matrices B,D must have same number of cols");
	if(matrixC.getRows() != matrixD.getRows())
	    throw new IllegalArgumentException("Matrices C,D must have same number of rows");
	if(matrixC.getCols() != matrixA.getCols())
	    throw new IllegalArgumentException("Matrices A,C must have same number of cols");
	if(storedInputs > matrixA.getRows())
	    throw new IllegalArgumentException("cant have more input states than total states");
	if(storedInputs > preworkB.getCols())
	    throw new IllegalArgumentException("cant have more input states than initial inputs");
	if(preworkA.getRows() != preworkA.getCols())
	    throw new IllegalArgumentException("Matrix preworkA must be square");
	if(preworkA.getRows() != matrixA.getRows())
	    throw new IllegalArgumentException("Matrices A, prework A must be same size");
	if(preworkA.getRows() != preworkB.getRows())
	    throw new IllegalArgumentException("Matrices preworkA,preworkB must have same number of rows");
	if(vec.getSize() != matrixA.getRows())
	    throw new IllegalArgumentException("initial vector must be same size as A");

	this.A = matrixA.copy();
	this.B = matrixB.copy();
	this.C = matrixC.copy();
	this.D = matrixD.copy();
	this.preworkA = preworkA.copy();
	this.preworkB = preworkB.copy();
	this.preworkNeeded = true;
	this.initVec = (FilterVector)vec.copy();	
	this.storedInputs = storedInputs;

	// we calculate cost on demand (with the linear partitioner)
	this.cost = null;
    }


    //////////////// Accessors ///////////////////
    
    /** Get the A matrix. **/
    public FilterMatrix getA() {return this.A.copy();}
    /** Get the B matrix. **/
    public FilterMatrix getB() {return this.B.copy();}
    /** Get the C matrix. **/
    public FilterMatrix getC() {return this.C.copy();}
    /** Get the D matrix. **/
    public FilterMatrix getD() {return this.D.copy();}

    /** Get the prework A matrix. **/
    public FilterMatrix getPreWorkA() {

	if(this.preworkNeeded())
	    return this.preworkA.copy();
	else
	    return null;
    }

    /** Get the prework B matrix. **/
    public FilterMatrix getPreWorkB() {
	if(this.preworkNeeded())
	    return this.preworkB.copy();
	else
	    return null;
    }

    /** Get the state vector initialization matrix. **/
    public FilterVector getInit() {return (FilterVector)this.initVec.copy();}

    /** Get the stored input count.  **/
    public int getStoredInputCount() {return this.storedInputs;}
    /** Get the push count. (#rows of D or C) **/
    public int getPushCount() {return this.D.getRows();}
    /** Get the pop count. (#cols of D) **/
    public int getPopCount() {return this.D.getCols();}
    /** Get the number of state variables (#rows of A or B, # cols of A or C) **/
    public int getStateCount() { return this.A.getRows();}

    /** Get the prework pop count. (#cols of B - initial) **/
    public int getPreWorkPopCount() {
	if(this.preworkB != null)
	    return this.preworkB.getCols();
	else
	    return 0;
    }

    public boolean preworkNeeded() {
	return this.preworkNeeded;
    }


    //////////////// Utility Functions  ///////////////////


    /**
     * Returns a prework A matrix (which is just the identity!)
     **/
    private FilterMatrix createPreWorkA(int states) {

	FilterMatrix preA = FilterMatrix.getIdentity(states);
	return preA;
    }

    /**
     * Returns a prework B matrix (which is just the identity and zeros underneath!)
     * inputs should be less than states
     **/
    private FilterMatrix createPreWorkB(int states, int inputs) {
	FilterMatrix preB = new FilterMatrix(states,inputs);

	for(int i=0; i<inputs; i++)
	    preB.setElement(i,i,ComplexNumber.ONE);

	return preB;
    }


    public void cleanEntries() {

	A.cleanEntries(); 
	B.cleanEntries();
	C.cleanEntries();
	D.cleanEntries();
    }


    /**
     * Returns true if at least one constant component is non-zero.
     **/
    public boolean hasConstantComponent() {
	return false;
    }
    

    /**
     * Changes the stored input value of this linear Representation (to a greater value)
     **/


		    /* 
The way this filter operates has been altered. Previously, it used its first oldStoredInputs variables to represent peek(0),peek(1),...,peek(oldStoredInputs-1), and used its pop inputs to represent peek(oldStoredInputs),peek(oldStoredInputs+1),...,peek(oldStoredInputs+pop-1).

Now, the filter should use its first newStoredInputs variables to represent peek(0),peek(1),...,peek(newStoredInputs-1), and use its first pop-(newStoredInputs-oldStored) inputs to represent peek(newStoredInputs),peek(newStoredInputs+1),...,peek(oldStoredInputs+pop-1). The remaining inputs should be used to update the extraVars variables.

We know that addVars >= newPeek2-newPop2, so we are adding states. Thus we must expanded some of the matrices, and reorder their contents. Why is that inequality true? addVars = n*newPush1 < newPeek2 <= (n+1)*newPush1, and newPush1 = newPop2, so (n+1)*newPush1 = extraVars + newPush1 = addVars + newPop1 >= newPeek2, so addVars >= newPeek2-newPop1.
		    */


    public LinearFilterRepresentation changeStoredInputs(int newStoredInputs) {

	int oldStoredInputs = this.getStoredInputCount();

	if(newStoredInputs <= oldStoredInputs)
	    throw new IllegalArgumentException("need param old: " + oldStoredInputs + 
					       " to be < new: " + newStoredInputs); 

	int pushCount = this.getPushCount();
	int popCount = this.getPopCount();
	int oldStateCount = this.getStateCount();
	int removeVars = oldStoredInputs;
	int addVars = newStoredInputs;
	int newStateCount = oldStateCount - removeVars + addVars;
	int oldPrePop, newPrePop, shiftPop, endCount;

	FilterMatrix A, B, C, D, oldPreA, oldPreB;
	FilterMatrix newA, newB, newC, newD, newPreA, newPreB;
			
	A = this.getA();
	B = this.getB();
	C = this.getC();
	D = this.getD();

	oldPrePop = this.getPreWorkPopCount();
	if(addVars > oldPrePop) {
	    newPrePop = addVars;
	    shiftPop = addVars - oldPrePop;
	}
	else {
	    newPrePop = oldPrePop;
	    shiftPop = 0;
	}

	newA = new FilterMatrix(newStateCount,newStateCount);
	newB = new FilterMatrix(newStateCount,popCount);
	newC = new FilterMatrix(pushCount,newStateCount);
	newD = new FilterMatrix(pushCount,popCount);
	newPreA = new FilterMatrix(newStateCount,newStateCount);
	newPreB = new FilterMatrix(newStateCount,newPrePop);

	if(this.preworkNeeded()) {
	    oldPreA = this.getPreWorkA();
	    oldPreB = this.getPreWorkB();

	    newPreB.copyRowsAndColsAt(0,0,oldPreB,0,0,removeVars,removeVars);
	    newPreB.copyRowsAndColsAt(addVars,0,oldPreB,removeVars,0,oldStateCount-removeVars,oldPreB.getCols());
	}
	else {
	    oldPreA = new FilterMatrix(oldStateCount,oldStateCount);
	}

	newPreA.copyRowsAndColsAt(addVars,addVars,oldPreA,removeVars,removeVars,oldStateCount-removeVars,oldStateCount-removeVars);
	newPreA.copyRowsAndColsAt(0,addVars,oldPreA,0,removeVars,removeVars,oldStateCount-removeVars);

	for(int i=removeVars; i<addVars; i++)
	    newPreB.setElement(i,i,ComplexNumber.ONE);


	newA.copyRowsAndColsAt(addVars,addVars,A,removeVars,removeVars,oldStateCount-removeVars,oldStateCount-removeVars);
	newA.copyRowsAndColsAt(0,addVars,A,0,removeVars,removeVars,oldStateCount-removeVars);
	newA.copyRowsAndColsAt(addVars,0,A,removeVars,0,oldStateCount-removeVars,removeVars);
	newA.copyRowsAndColsAt(0,0,A,0,0,removeVars,removeVars);

	if(popCount >= shiftPop) {
	    newB.copyRowsAndColsAt(0,0,B,0,shiftPop,removeVars,popCount-shiftPop);
	    newB.copyRowsAndColsAt(addVars,0,B,removeVars,shiftPop,oldStateCount-removeVars,popCount-shiftPop);

	    newD.copyColumnsAt(0,D,shiftPop,popCount-shiftPop);

	    endCount = shiftPop;
	}
	else
	    endCount = popCount;

	newA.copyRowsAndColsAt(addVars,removeVars,B,removeVars,0,oldStateCount-removeVars,endCount);
	newA.copyRowsAndColsAt(0,removeVars,B,0,0,removeVars,endCount);

	for(int i=removeVars; i<addVars; i++) {
	    if(i+popCount<addVars)
		newA.setElement(i,i+popCount,ComplexNumber.ONE);
	    else
		newB.setElement(i,i+popCount-addVars,ComplexNumber.ONE);
	}

	newC.copyColumnsAt(addVars,C,removeVars,oldStateCount-removeVars);
	newC.copyColumnsAt(0,C,0,removeVars);
	newC.copyColumnsAt(removeVars,D,0,endCount);


	LinearPrinter.println("inputvars: " + oldStoredInputs + " -> " + newStoredInputs);	

	FilterVector init = this.getInit();
	FilterVector newInit = new FilterVector(newStateCount);
	newInit.copyAt(0,addVars-removeVars,init);
	
	LinearFilterRepresentation newRep; 

	if(this.preworkNeeded())
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreA,newPreB,newStoredInputs,newInit);
	else
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newStoredInputs,newInit);

	return newRep;
    }

    /* puts outputs into states at end of state vector */
    /*  NOT NECESSARY, adding stored inputs is the same thing */
    public LinearFilterRepresentation addStoredOutputs() {
	
	int oldStates = this.getStateCount();
	int pushVal = this.getPushCount();
	int popVal = this.getPopCount();
	int addValue = this.getPushCount();
	int newStates = oldStates+addValue;

	int storedInputs = this.getStoredInputCount();

	FilterMatrix newA = new FilterMatrix(newStates,newStates);
	FilterMatrix newB = new FilterMatrix(newStates,popVal);
	FilterMatrix newC = new FilterMatrix(pushVal,newStates);
	FilterMatrix newD = new FilterMatrix(pushVal,popVal);

	newA.copyAt(0,0,this.getA());
	newA.copyAt(oldStates,0,this.getC().times(this.getA()));

	newB.copyAt(0,0,this.getB());
	newB.copyAt(oldStates,0,this.getD().plus(this.getC().times(this.getB())));

	newC.copyAt(0,oldStates,FilterMatrix.getIdentity(addValue));

	FilterVector newInit = new FilterVector(newStates);
	newInit.copyAt(0,0,this.getInit());

	LinearFilterRepresentation newRep;
	
	if(this.preworkNeeded()) {

	    int prePopVal = this.getPreWorkPopCount();
	    
	    FilterMatrix newPreA = new FilterMatrix(newStates,newStates);
	    FilterMatrix newPreB = new FilterMatrix(newStates,prePopVal);
	    
	    newPreA.copyAt(oldStates,0,this.getC().times(this.getPreWorkA()));
	    newPreA.copyAt(0,0,this.getPreWorkA());
	    
	    newPreB.copyAt(oldStates,0,this.getC().times(this.getPreWorkB()));
	    newPreB.copyAt(0,0,this.getPreWorkB());

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreA,newPreB,storedInputs,newInit);

	}
	else {

	    FilterMatrix tempInit = this.getC().times(this.getInit().transpose()).transpose();
	    newInit.copyAt(0,oldStates,tempInit);

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,storedInputs,newInit);
	}

	return newRep;
    }
    



    /**
     * Expands this linear representation by factor 
     **/
    public LinearFilterRepresentation expand(int factor) {
    
	// do some argument checks

	if (factor < 1) {
	    throw new IllegalArgumentException("need a positive multiplier");
	}

	if(factor == 1)
	    return this;

	// pull out old values for ease in understanding the code.
	int stateCount = this.getStateCount();
	int oldPush = this.getPushCount();
	int oldPop  = this.getPopCount();
	int inputStates = this.getStoredInputCount();

	// newA = oldA^(factor);
	// newB = [oldA^(factor-1) * B  oldA^(factor-2) * B  ...  oldA * B  B]
	// newC = [C  C * A  C * A^2  ...  C * A^(factor-2)  C * A^(factor-1)] (transposed)

	/* 
	   newD = |D                     0               0          0  ...  0      |       
                  |C * B                 D               0          0  ...  0      |
                  |C * A * B             C * B           D          0  ...  0      |
                  |C * A^2 * B           C * A * B       C * B      D  ...  0      |
                  |  ...
                  |C*A^(factor-2)*B   C*A^(factor-3)*B                 ...  C*B  D | 

         */

	FilterMatrix oldA = this.getA();
	FilterMatrix oldB = this.getB();
	FilterMatrix oldC = this.getC();
        FilterMatrix oldD = this.getD();

	FilterMatrix newA = oldA.copy();
	FilterMatrix newB = new FilterMatrix(stateCount, oldPop*factor);
	FilterMatrix newC = new FilterMatrix(oldPush*factor, stateCount);
        FilterMatrix newD = new FilterMatrix(oldPush*factor, oldPop*factor);

	newB.copyAt(0,oldPop*(factor-1),oldB);
	newC.copyAt(0,0,oldC);

	FilterMatrix tempB = oldB.copy();
	FilterMatrix tempC = oldC.copy();
	FilterMatrix tempD = oldC.times(oldB);

	for(int i=0; i<factor; i++) 
	  newD.copyAt(oldPush*i,oldPop*i,oldD);

	for(int i=1; i<factor; i++) {
	  tempB = newA.times(oldB);
	  tempC = oldC.times(newA);

	  newB.copyAt(0,oldPop*(factor-i-1),tempB);
	  newC.copyAt(oldPush*i,0,tempC);

	  for(int j=0; j<factor-i; j++)
	    newD.copyAt(oldPush*(i+j),oldPop*j,tempD);
	  
	  newA = newA.times(oldA);
	  tempD = tempC.times(oldB); 
	  
	}

	// initial vector is the same
	FilterVector newInitVec = (FilterVector)this.getInit().copy();

	// create a new Linear rep for the expanded filter
	LinearFilterRepresentation newRep;

	if(this.preworkNeeded()) {
	    // prework matrices are the same
	    FilterMatrix newPreWorkA = this.getPreWorkA();
	    FilterMatrix newPreWorkB = this.getPreWorkB();

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreWorkA,newPreWorkB,inputStates,newInitVec);	
	}
	else {
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,inputStates,newInitVec);
	}

	return newRep;
    }
    
				

    /**
     * Expands this linear representation by factor, using the prework matrices as well 
     **/
    public LinearFilterRepresentation expand_with_prework(int factor) {
    
	// do some argument checks
	if (factor < 1) {
	    throw new IllegalArgumentException("need a positive multiplier");
	}

	// pull out old values for ease in understanding the code.
	int stateCount = this.getStateCount();
	int oldPush = this.getPushCount();
	int oldPop  = this.getPopCount();
	int inputStates = this.getStoredInputCount();

	// newA = oldA^(factor) * preA;
	// newB = [oldA^(factor) * preB  oldA^(factor-1) * B  oldA^(factor-2) * B  ...  oldA * B  B]
	// newC = [C * preA  C * A * preA  C * A^2 * preA  ...  C * A^(factor-2) * preA  C * A^(factor-1) * preA] (transposed)

	/* 
	   newD = |C * preB           D                     0               0          0  ...  0      | 
                  |C * A * preB       C * B                 D               0          0  ...  0      |
                  |C * A^2 * preB     C * A * B             C * B           D          0  ...  0      |
                  |C * A^3 * preB     C * A^2 * B           C * A * B       C * B      D  ...  0      |
                  |       ...              ...                                                        | 
                  |C*A^(factor-1)*preB  C*A^(factor-2)*B   C*A^(factor-3)*B                 ...  C*B  D | 

         */

	FilterMatrix oldA = this.getA();
	FilterMatrix oldB = this.getB();
	FilterMatrix oldC = this.getC();
        FilterMatrix oldD = this.getD();
	
	FilterMatrix preA = this.getPreWorkA();
	FilterMatrix preB = this.getPreWorkB();
	int prePop = this.getPreWorkPopCount();
	
	FilterMatrix newA = oldA.copy();
	FilterMatrix newB = new FilterMatrix(stateCount, oldPop*factor + prePop);
	FilterMatrix newC = new FilterMatrix(oldPush*factor, stateCount);
	FilterMatrix newD = new FilterMatrix(oldPush*factor, oldPop*factor + prePop);

	newB.copyAt(0,oldPop*(factor-1) + prePop,oldB);

	if(this.preworkNeeded())
	    newC.copyAt(0,0,oldC.times(preA));
	else
	    newC.copyAt(0,0,oldC);

	FilterMatrix tempB = oldB.copy();
	FilterMatrix tempC = oldC.copy();
	FilterMatrix tempD = oldC.times(oldB);

	for(int i=0; i<factor; i++) 
	  newD.copyAt(oldPush*i,oldPop*i + prePop , oldD);

	if(this.preworkNeeded())
	    newD.copyAt(0,0,oldC.times(preB));

	for(int i=1; i<factor; i++) {
	  tempB = newA.times(oldB);
	  tempC = oldC.times(newA);

	  newB.copyAt(0,oldPop*(factor-i-1) + prePop ,tempB);

	  if(this.preworkNeeded())
	      newC.copyAt(oldPush*i,0,tempC.times(preA));
	  else
	      newC.copyAt(oldPush*i,0,tempC);

	  for(int j=0; j<factor-i; j++)
	    newD.copyAt(oldPush*(i+j),oldPop*j + prePop ,tempD);
	 

	  if(this.preworkNeeded())
	      newD.copyAt(oldPush*i,0,tempC.times(preB));
 
	  newA = newA.times(oldA);
	  tempD = tempC.times(oldB); 
	  
	}

	if(this.preworkNeeded()) {

	    newB.copyAt(0,0,newA.times(preB));
	    newA = newA.times(preA);	    
	}
	
	// initial vector is the same
	FilterVector newInitVec = (FilterVector)this.getInit().copy();

	// create a new Linear rep for the expanded filter
	LinearFilterRepresentation newRep;

	if(this.preworkNeeded()) {

	    FilterMatrix newPreWorkA = this.getPreWorkA();
	    FilterMatrix newPreWorkB = this.getPreWorkB();

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreWorkA,newPreWorkB,inputStates,newInitVec);
	
	}
	else
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,inputStates,newInitVec);
		
	return newRep;
	
    }


    /**
     * returns a LinearCost object that represents the number
     * of multiplies and adds that are necessary to implement this
     * linear filter representation (in steady state).
     **/    
    public LinearCost getCost() {
	if (this.cost==null) {
	  LinearCost tempCost = calculateCost(this.A);
	  tempCost = tempCost.plus(calculateCost(this.B));
	  tempCost = tempCost.plus(calculateCost(this.C));
	  tempCost = tempCost.plus(calculateCost(this.D));
	  this.cost = tempCost;
	}
	return this.cost;
    }


    /**
     * Calculates cost of this.
     */    
    private LinearCost calculateCost(FilterMatrix M) {
	// add up multiplies and adds that are necessary for each *row* of the matrix. 
	int muls = 0;
	int adds = 0;

	int matRows = M.getRows();
	int matCols = M.getCols();
	
	for (int row=0; row<matRows; row++) {
	    // counters for this row (# muls, adds)
	    int rowAdds = 0;
	    int rowMuls =  0;
	    for (int col=0; col<matCols; col++) {
		ComplexNumber currentElement = M.getElement(row,col);
		if (!currentElement.isReal()) {
		    throw new RuntimeException("Non real matrix elements are not supported in cost .");
		}
		// flags on whether or not to increment the counters
		boolean incAdd = true;
		boolean incMul = true;
		// if it is zero, no add or mult is necessary
		if (currentElement.equals(ComplexNumber.ZERO)) {
		    incAdd = false;
		    incMul = false;
		// if one, no need to do a multiplication.
		} else if (currentElement.equals(ComplexNumber.ONE)) {
		    incMul = false;
		}
		// now, increment if our increment flags are set.
		if (incAdd) {rowAdds++;}
		if (incMul) {rowMuls++;}
	    }


	    // basically, we need one less add per row because adds take two operands
	    // however, we don't want to blindly subtract one, because that might give
	    // us a negative number
	    if (rowAdds > 0) {rowAdds--;}
	    // stick row counters onto overall counters
	    muls += rowMuls;
	    adds += rowAdds;
	}
	return new LinearCost(muls, adds, matRows, matCols);
    }	    
    

    /** Returns true if and only if all coefficients in this filter rep are real valued. **/
    public boolean isPurelyReal() {
	// check the matrix(A), element by element.
	for (int i=0; i<A.getRows(); i++) {
	    for (int j=0; j<A.getCols(); j++) {
		if (!A.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(B), element by element.
	for (int i=0; i<B.getRows(); i++) {
	    for (int j=0; j<B.getCols(); j++) {
		if (!B.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(C), element by element.
	for (int i=0; i<C.getRows(); i++) {
	    for (int j=0; j<C.getCols(); j++) {
		if (!C.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}
	// check the matrix(D), element by element.
	for (int i=0; i<D.getRows(); i++) {
	    for (int j=0; j<D.getCols(); j++) {
		if (!D.getElement(i,j).isReal()) {
		    return false;
		}
	    }
	}	

	// if we get here, there are only real elements in this rep
	return true;
    }

    public String toString() {

	String retString = new String( "\n Matrix A:\n" + A + 
				      "\n Matrix B:\n" + B + 
				      "\n Matrix C:\n" + C + 
				      "\n Matrix D:\n" + D +
				      "\n PreworkA:\n" + preworkA +
				      "\n PreworkB:\n" + preworkB +
				      "\n Init:\n" + initVec);
	return retString;
    }


}







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
 * $Id: LinearFilterRepresentation.java,v 1.11 2004-03-15 21:36:02 sitij Exp $
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
     * The peek count of the filter. This is necessary for doing pipeline combinations
     * and it is information not stored in the dimensions of the
     * representation matrix or vector. peekCount - popCount is the number of variables that
     * must be popped initially (to be done with the initialization matrices)
     **/
    private int peekCount;

    /**
     * Create a new linear filter representation with matrices A, B, C, and D.
     * Note that we use a copy of all matrices so that we don't end up with
     * an aliasing problem. peekc is the peek count of the filter that this represenation is for,
     * which we need for combining filters together (because the difference between
     * the peek count and the pop count tells us about the buffers that the program is using).
     **/
    public LinearFilterRepresentation(FilterMatrix matrixA,
				      FilterMatrix matrixB,
				      FilterMatrix matrixC,
				      FilterMatrix matrixD,
				      FilterVector vec,
				      int peekc) {

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
	if(peekc < matrixD.getCols())
	    throw new IllegalArgumentException("peek count cannot be less than pop count");
	//	if(peekc-matrixD.getCols() > matrixA.getRows())
	//	    throw new IllegalArgumentException("number of states must not be less than peek-pop count");

	this.A = matrixA.copy();
	this.B = matrixB.copy();
	this.C = matrixC.copy();
	this.D = matrixD.copy();
	this.initVec = (FilterVector)vec.copy();	
	this.peekCount = peekc;
	int popCount = this.getPopCount();

	if(popCount == peekc) {  // no separate initialization needed, so use the same matrices
	    this.preworkNeeded = false;
	}
	else {
	    int stateCount = this.getStateCount();
	    int offset = peekc - popCount;

	    // preworkA is the identity
	    this.preworkA = this.createPreWorkA(stateCount);

	    // preworkB puts the inputs into the extra states
	    this.preworkB = this.createPreWorkB(stateCount,offset);
	    
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
	if(preworkA.getRows() != preworkA.getCols())
	    throw new IllegalArgumentException("Matrix preworkA must be square");
	if(preworkA.getRows() != matrixA.getRows())
	    throw new IllegalArgumentException("Matrices A, prework A must be same size");
	if(preworkA.getRows() != preworkB.getRows())
	    throw new IllegalArgumentException("Matrices preworkA,preworkB must have same number of rows");
	//	if(preworkB.getCols() > matrixA.getRows())
	//	    throw new IllegalArgumentException("number of states must be not be less than peek-pop count " + preworkB.getCols() + " " + matrixA.getRows());
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
	this.peekCount = this.B.getCols() + this.preworkB.getCols();

	// we calculate cost on demand (with the linear partitioner)
	this.cost = null;
    }


    //////////////// Accessors ///////////////////
    
    /** Get the A matrix. **/
    public FilterMatrix getA() {return this.A;}
    /** Get the B matrix. **/
    public FilterMatrix getB() {return this.B;}
    /** Get the C matrix. **/
    public FilterMatrix getC() {return this.C;}
    /** Get the D matrix. **/
    public FilterMatrix getD() {return this.D;}

    /** Get the prework A matrix. **/
    public FilterMatrix getPreWorkA() {

	if(this.preworkNeeded())
	    return this.preworkA;
	else
	    return null;
    }

    /** Get the prework B matrix. **/
    public FilterMatrix getPreWorkB() {
	if(this.preworkNeeded())
	    return this.preworkB;
	else
	    return null;
    }

    /** Get the state vector initialization matrix. **/
    public FilterVector getInit() {return this.initVec;}

    /** Get the peek count.  **/
    public int getPeekCount() {return this.peekCount;}
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


    /**
     * Returns true if at least one constant component is non-zero.
     **/
        
    public boolean hasConstantComponent() {
	/*
        int index = A.zeroRow();
	if(index == -1)
	    return false;
	else
	  return initVec.getElement(index) != ComplexNumber.ZERO;
	*/


	return false;
    }
    

    /**
     * Changes the peek value of this linear Representation (to a greater value)
     **/


		    /* 
The way this filter operates has been altered. Previously, it used its first newPeek2-newPop2 variables to represent peek(0),peek(1),...,peek(newPeek2-newPop2-1), and used its newPop2 inputs to represent peek(newPeek2-newPop2),peek(newPeek2-newPop2+1),...,peek(newPeek2-1).

Now, the filter should use its first addVars variables to represent peek(0),peek(1),...,peek(extraVars-1), and use its first newPop2-extraVars inputs to represent peek(extraVars),peek(extraVars+1),...,peek(newPeek2-1), The remaining inputs should be used to update the extraVars variables.

We know that addVars >= newPeek2-newPop2, so we are adding states. Thus we must expanded some of the matrices, and reorder their contents. Why is that inequality true? addVars = n*newPush1 < newPeek2 <= (n+1)*newPush1, and newPush1 = newPop2, so (n+1)*newPush1 = extraVars + newPush1 = addVars + newPop1 >= newPeek2, so addVars >= newPeek2-newPop1.
		    */


    public LinearFilterRepresentation changePeek(int newPeek) {

	int oldPeek = this.peekCount;

	if(oldPeek >= newPeek)
	    throw new IllegalArgumentException("need param newPeek(" + newPeek + ") to be < oldPeek(" + oldPeek + ")"); 

	int pushCount = this.getPushCount();
	int popCount = this.getPopCount();
	int oldStateCount = this.getStateCount();
	int removeVars = oldPeek - popCount;
	int addVars = newPeek - popCount;
	int newStateCount = oldStateCount - removeVars + addVars;

	FilterMatrix A, B, C, D, preA, preB;
	FilterMatrix newA, newB, newC, newD, newPreA, newPreB;
			
	A = this.getA();
	B = this.getB();
	C = this.getC();
	D = this.getD();

	newA = new FilterMatrix(newStateCount,newStateCount);
	newA.copyRowsAndColsAt(addVars,addVars,A,removeVars,removeVars,oldStateCount-removeVars,oldStateCount-removeVars);
	newA.copyRowsAndColsAt(0,addVars,A,0,removeVars,removeVars,oldStateCount-removeVars);
	newA.copyRowsAndColsAt(addVars,0,A,removeVars,0,oldStateCount-removeVars,removeVars);
	newA.copyRowsAndColsAt(0,0,A,0,0,removeVars,removeVars);
	newA.copyRowsAndColsAt(addVars,removeVars,B,removeVars,0,oldStateCount-removeVars,addVars-removeVars);
	newA.copyRowsAndColsAt(0,removeVars,B,0,0,removeVars,addVars-removeVars);

	newB = new FilterMatrix(newStateCount,popCount);
	newB.copyRowsAndColsAt(0,0,B,0,addVars-removeVars,removeVars,popCount-(addVars-removeVars));
	newB.copyRowsAndColsAt(addVars,0,B,removeVars,addVars-removeVars,oldStateCount-removeVars,popCount-(addVars-removeVars));

	for(int i=removeVars; i<addVars; i++) {
	    if(i+popCount<addVars)
		newA.setElement(i,i+popCount,ComplexNumber.ONE);
	    else
		newB.setElement(i,i+popCount-addVars,ComplexNumber.ONE);
	}


	newPreA = new FilterMatrix(newStateCount,newStateCount);
	newPreB = new FilterMatrix(newStateCount,newPeek-popCount);

	if(this.preworkNeeded()) {
	    preA = this.getPreWorkA();
	    preB = this.getPreWorkB();

	    newPreA.copyRowsAndColsAt(addVars,addVars,preA,removeVars,removeVars,oldStateCount-removeVars,oldStateCount-removeVars);
	    newPreA.copyRowsAndColsAt(0,addVars,preA,0,removeVars,removeVars,oldStateCount-removeVars);

	    newPreB.copyRowsAndColsAt(0,0,preB,0,0,removeVars,removeVars);
	    newPreB.copyRowsAndColsAt(addVars,0,preB,removeVars,0,oldStateCount-removeVars,removeVars);
	}
	else {
	    for(int i=0; i<newStateCount; i++)
		newPreA.setElement(i,i,ComplexNumber.ONE);
	}
	
	for(int i=removeVars; i<addVars; i++)
	    newPreB.setElement(i,i,ComplexNumber.ONE);
	
	newC = new FilterMatrix(pushCount,newStateCount);
	newC.copyColumnsAt(addVars,C,removeVars,oldStateCount-removeVars);
	newC.copyColumnsAt(0,C,0,removeVars);
	newC.copyColumnsAt(removeVars,D,0,addVars-removeVars);
			
	newD = new FilterMatrix(pushCount,popCount);

	int offset_temp = addVars-removeVars;
	LinearPrinter.println("offset,length,total: " + offset_temp + " " + removeVars + " " + D.getCols());
	newD.copyColumnsAt(0,D,addVars-removeVars,popCount-(addVars-removeVars));

	//	newD.copyColumnsAt(0,D,addVars-removeVars,removeVars);

	/*
	LinearPrinter.println("after changePeek A " + newA);
	LinearPrinter.println("after changePeek B" + newB);
	LinearPrinter.println("after changePeek C " + newC);
	LinearPrinter.println("after changePeek D" + newD);
	LinearPrinter.println("after changePeek preA " + newPreA);
	LinearPrinter.println("after changePeek preB " + newPreB);
	*/

	FilterVector init = this.getInit();
	FilterVector newInit = new FilterVector(newStateCount);
	newInit.copyAt(0,addVars-removeVars,init);
		

	LinearPrinter.println("after changePeek init " + newInit);


	
	LinearFilterRepresentation newRep; 
	newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreA,newPreB,newInit);

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

	// pull out old values for ease in understanding the code.
	int stateCount = this.getStateCount();
	int oldPush = this.getPushCount();
	int oldPop  = this.getPopCount();
	int oldPeek = this.getPeekCount();

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

	
	
	//System.err.println("--------");
	//System.err.println("new rows: " + newPeek);
	//System.err.println("new cols: " + newPush);
	//System.err.println("new pop: "  + newPop);
	//System.err.println("old rows: " + oldPeek);
	//System.err.println("old cols: " + oldPush);
	//System.err.println("old pop: "  + oldPop);
	//System.err.println("num copies: " + numCompleteCopies);
	

	LinearPrinter.println("Matrix oldA:");
	LinearPrinter.println(oldA.toString());
	LinearPrinter.println("Matrix oldB:");
	LinearPrinter.println(oldB.toString());
	LinearPrinter.println("Matrix oldC:");
	LinearPrinter.println(oldC.toString());
	LinearPrinter.println("Matrix oldD:");
	LinearPrinter.println(oldD.toString());

	LinearPrinter.println("Matrix A:");
	LinearPrinter.println(newA.toString());
	LinearPrinter.println("Matrix B:");
	LinearPrinter.println(newB.toString());
	LinearPrinter.println("Matrix C:");
	LinearPrinter.println(newC.toString());
	LinearPrinter.println("Matrix D:");
	LinearPrinter.println(newD.toString());


	// initial vector is the same
	FilterVector newInitVec = (FilterVector)this.getInit().copy();

	// create a new Linear rep for the expanded filter
	LinearFilterRepresentation newRep;

	if(this.preworkNeeded()) {
	    // prework matrices are the same
	    FilterMatrix newPreWorkA = this.getPreWorkA();
	    FilterMatrix newPreWorkB = this.getPreWorkB();

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreWorkA,newPreWorkB,newInitVec);
	
	}
	else {
	    // newpeek - newpop = oldpeek - oldpop
	    // newpop = factor*oldpop

	    int newPeek = oldPeek - oldPop + oldPop*factor;
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newInitVec,newPeek);
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
	int oldPeek = this.getPeekCount();

	// newA = oldA^(factor) * preA;
	// newB = [oldA^(factor * preB  oldA^(factor-1) * B  oldA^(factor-2) * B  ...  oldA * B  B]
	// newC = [C * preA  C * A * preA  C * A^2 * preA  ...  C * A^(factor-2) * preA  C * A^(factor-1) * preA] (transposed)

	/* 
	   newD = |C * preB           D                     0               0          0  ...  0      | 
                  |C * A * preB       C * B                 D               0          0  ...  0      |
                  |C * A^2 * preB     C * A * B             C * B           D          0  ...  0      |
                  |C * A^3 * preB     C * A^2 * B           C * A * B       C * B      D  ...  0      |
                  |       ...              ...                                                        | 
                  |C*A^(factor)*preB  C*A^(factor-2)*B   C*A^(factor-3)*B                 ...  C*B  D | 

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
	
	//System.err.println("--------");
	//System.err.println("new rows: " + newPeek);
	//System.err.println("new cols: " + newPush);
	//System.err.println("new pop: "  + newPop);
	//System.err.println("old rows: " + oldPeek);
	//System.err.println("old cols: " + oldPush);
	//System.err.println("old pop: "  + oldPop);
	//System.err.println("num copies: " + numCompleteCopies);
	

	LinearPrinter.println("Matrix A:");
	LinearPrinter.println(newA.toString());
	LinearPrinter.println("Matrix B:");
	LinearPrinter.println(newB.toString());
	LinearPrinter.println("Matrix C:");
	LinearPrinter.println(newC.toString());
	LinearPrinter.println("Matrix D:");
	LinearPrinter.println(newD.toString());


	// initial vector is the same
	FilterVector newInitVec = (FilterVector)this.getInit().copy();

	// create a new Linear rep for the expanded filter
	LinearFilterRepresentation newRep;

	if(this.preworkNeeded()) {

	    FilterMatrix newPreWorkA = this.getPreWorkA();
	    FilterMatrix newPreWorkB = this.getPreWorkB();

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreWorkA,newPreWorkB,newInitVec);
	
	}
	else
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newInitVec, newB.getCols());
		
	return newRep;
	
    }

	
    /**
     * Returns true if this filter is an FIR filter. A linear filter is FIR  
     * if push=pop=1 and no constant component.
     **/
    /*
    public boolean isFIR() {
	return ((this.getPopCount() == 1) &&
		(this.getPushCount() == 1) &&
		(this.getb().getElement(0,0).equals(ComplexNumber.ZERO)));
    }
    */

    /**
     * returns a LinearCost object that represents the number
     * of multiplies and adds that are necessary to implement this
     * linear filter representation.
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
	// add up multiplies and adds that are necessary for each column of the matrix. 
	int muls = 0;
	int adds = 0;

	int matRows = M.getRows();
	int matCols = M.getCols();
	
	for (int col=0; col<matCols; col++) {
	    // counters for the colums (# muls, adds)
	    int rowAdds = 0;
	    int rowMuls =  0;
	    for (int row=0; row<matRows; row++) {
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
}







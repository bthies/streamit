package at.dms.kjc.sir.statespace;

/* reduces states so that representation is observable */


public class LinearOptimizer {

    private static LinearFilterRepresentation transposeSystem(LinearFilterRepresentation l) {

	FilterMatrix transA, transB, transC, transD;
	LinearFilterRepresentation transRep;
	boolean preNeeded = l.preworkNeeded();

	transA = l.getA().transpose();
	transB = l.getB().transpose();
	transC = l.getC().transpose();
	transD = l.getD().transpose();

	if(preNeeded) {
	    FilterMatrix transPreA, transPreB;

	    transPreA = l.getPreWorkA().transpose();
	    transPreB = l.getPreWorkB().transpose();

	    transRep = new LinearFilterRepresentation(transA,transB,transC,transD,transPreA,transPreB,l.getStoredInputCount(),l.getInit());

	}
	else
	    transRep = new LinearFilterRepresentation(transA,transB,transC,transD,l.getStoredInputCount(),l.getInit());


	return transRep;

    }


    public static LinearFilterRepresentation getObservableRep(LinearFilterRepresentation orig) {

	LinearFilterRepresentation transOrig = transposeSystem(orig);

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


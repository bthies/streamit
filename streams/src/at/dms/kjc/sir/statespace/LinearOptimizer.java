package at.dms.kjc.sir.statespace;


public class LinearOptimizer {


    public static LinearFilterRepresentation getMinStateRep(LinearFilterRepresentation l) {

	int states = l.getStateCount();
	int outputs = l.getPushCount();
	int inputs = l.getPopCount();
	int totalRows = states + outputs;
	int totalCols = states + inputs;
	boolean preNeeded = l.preworkNeeded();

	FilterVector init = l.getInit();

	FilterMatrix totalMatrix = new FilterMatrix(totalRows,totalCols);
	totalMatrix.copyAt(0,0,l.getA());
	totalMatrix.copyAt(0,states,l.getB());
	totalMatrix.copyAt(states,0,l.getC());

	FilterMatrix totalPreMatrix = null;

	if(preNeeded) {
	    int totalPreCols = states + l.getPreWorkPopCount();
	    totalPreMatrix = new FilterMatrix(states,totalPreCols);

	    totalPreMatrix.copyAt(0,0,l.getPreWorkA());
	    totalPreMatrix.copyAt(0,states,l.getPreWorkB());

	    LinearPrinter.println("totalPreMatrix \n" + totalPreMatrix);

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
			    LinearPrinter.println("multiplying \n" + totalPreMatrix);
			}
			init.addCol(k,i,val);


			LinearPrinter.println("ADDED MULTIPLE " + i + " " + k + " " + val);
			
		    }
		}
	    }

	    i = i - 1; 
	    j = j - 1; 
	}

	if(i==j)
	    LinearPrinter.println("i = j = " + i);

	FilterMatrix newA,newB,newC,newD;
	FilterVector newInit;
	LinearFilterRepresentation newRep;

	newA = new FilterMatrix(states,states);
	newB = new FilterMatrix(states,inputs);
	newC = new FilterMatrix(outputs,states);
	newD = l.getD();

	newA.copyRowsAndColsAt(0,0,totalMatrix,0,0,states,states);
	newB.copyRowsAndColsAt(0,0,totalMatrix,0,states,states,inputs);
	newC.copyRowsAndColsAt(0,0,totalMatrix,states,0,outputs,states);

	if(preNeeded) {

	    int preInputs = l.getPreWorkPopCount();

	    FilterMatrix newPreA = new FilterMatrix(states,states);
	    FilterMatrix newPreB = new FilterMatrix(states,preInputs);

	    newPreA.copyRowsAndColsAt(0,0,totalPreMatrix,0,0,states,states);
	    newPreB.copyRowsAndColsAt(0,0,totalPreMatrix,0,states,states,preInputs);

	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,newPreA,newPreB,l.getStoredInputCount(),init);

	}
	else
	    newRep = new LinearFilterRepresentation(newA,newB,newC,newD,l.getStoredInputCount(),init);

	return newRep;
    }

}


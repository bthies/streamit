package at.dms.kjc.sir.statespace;


public class LinearOptimizer {


    public static LinearFilterRepresentation getMinStateRep(LinearFilterRepresentation l) {

	int states = l.getStateCount();
	int outputs = l.getPushCount();
	int inputs = l.getPopCount();
	int totalRows = states + outputs;
	int totalCols = states + inputs;

	FilterVector init = l.getInit();

	FilterMatrix totalMatrix = new FilterMatrix(totalRows,totalCols);
	totalMatrix.copyAt(0,0,l.getA());
	totalMatrix.copyAt(0,states,l.getB());
	totalMatrix.copyAt(states,0,l.getC());
	totalMatrix.copyAt(states,states,l.getD());

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
		    LinearPrinter.println("SWAPPED " + i + " " + r);
		}
	    }

	    double curr = totalMatrix.getElement(i,j).getReal();
	    
	    LinearPrinter.println("i,j,curr " + i + " " + j + " " + curr);

	    if(curr != 0.0) {
		for(int k=0; k<i; k++) {
		    if(!totalMatrix.getElement(k,j).equals(ComplexNumber.ZERO)) {
			double temp = totalMatrix.getElement(k,j).getReal();
			totalMatrix.addRowAndCol(i,k,-temp/curr);

		    }
		}
	    }

	    i = i - 1; 
	    j = j - 1; 
	}

	FilterMatrix newA,newB,newC,newD,newpreA,newpreB;
	FilterVector newInit;
	LinearFilterRepresentation newRep;

	newA = new FilterMatrix(states,states);
	newB = new FilterMatrix(states,inputs);
	newC = new FilterMatrix(outputs,states);
	newD = new FilterMatrix(outputs,inputs);

	newA.copyRowsAndColsAt(0,0,totalMatrix,0,0,states,states);
	newB.copyRowsAndColsAt(0,0,totalMatrix,0,states,states,inputs);
	newC.copyRowsAndColsAt(0,0,totalMatrix,states,0,outputs,states);
	newD.copyRowsAndColsAt(0,0,totalMatrix,states,states,outputs,inputs);

	newRep = new LinearFilterRepresentation(newA,newB,newC,newD,init,l.getPeekCount());

	return newRep;
    }

}

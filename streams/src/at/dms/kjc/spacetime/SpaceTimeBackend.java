package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;
import java.util.ListIterator;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
/**
 * The entry to the space time backend for raw.
 **/
public class SpaceTimeBackend 
{
    public static boolean FILTER_DEBUG_MODE = false;
    public static SIRStructure[] structures;
    
    public static void run(SIRStream str,
			   JInterfaceDeclaration[] 
			   interfaces,
			   SIRInterfaceTable[]
			   interfaceTables,
			   SIRStructure[]
			   structs) {
	structures = structs;
	
	int rawRows = -1;
	int rawColumns = -1;

	//set number of columns/rows
	rawRows = KjcOptions.raw;
	if(KjcOptions.rawcol>-1)
	    rawColumns = KjcOptions.rawcol;
	else
	    rawColumns = KjcOptions.raw;

	//create the RawChip
	RawChip rawChip = new RawChip(rawRows, rawColumns);

	//this must be run now, other pass rely on it...
	RenameAll.renameOverAllFilters(str);
	
	// move field initializations into init function
	FieldInitMover.moveStreamInitialAssignments(str);
		
	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println("Done Constant Prop and Unroll...");

	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);

	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(str);

	// do constant propagation on fields
        if (KjcOptions.nofieldprop) {
	} else {
	    System.out.println("Running Constant Field Propagation...");
	    FieldProp.doPropagate(str);
	    System.out.println("Done Constant Field Propagation...");
	}

	Lifter.liftAggressiveSync(str);
       	StreamItDot.printGraph(str, "before.dot");

	//jasperln's Stuff
	FlattenGraph.flattenGraph(str);
	UnflatFilter[] topNodes=FlattenGraph.getTopLevelNodes();
	System.out.println("Top Nodes:");
	for(int i=0;i<topNodes.length;i++)
	    System.out.println(topNodes[i]);
	//System.out.println(FlattenGraph.getFilterCount());
	
	//this is here just to test things!!
	Trace[] init = new Trace[2];
	Trace[] steady = new Trace[2];

	//mgordon's stuff
	ListIterator initTrav = TraceTraversal.getTraversal(init).listIterator();    
	ListIterator steadyTrav = TraceTraversal.getTraversal(steady).listIterator();

	//create the raw execution code and switch code for the initialization phase
	Rawify.run(initTrav, rawChip, true); 
	//create the raw execution code and switch for the steady-state
	Rawify.run(initTrav, rawChip, false);
	//generate the switch code assembly files...
	GenerateSwitchCode.run(rawChip);
	//generate the compute code from the SIR
	GenerateComputeCode.run(rawChip);
	Makefile.generate(rawChip);
    }
}

    



package at.dms.kjc.rstream;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;

/**
 * The main driver class for the StreamIt to RStream 
 * backend.  Used to house the run method that does all the 
 * work of translating the SIR into C code to be inputted to 
 * the RStream compiler.
 * 
 * @author Michael Gordon
 * @author Bill Thies
 * @author Jasper Lin
 * 
 */
public class StrToRStream {
    /** The execution counts from the scheduler: [0] init, [1] steady **/
    public static HashMap[] executionCounts;
    
    /** The structure defined in the application, see SIRStructure **/
    private static SIRStructure[] structures;
    
    /**
     * The entry point of the RStream "backend" for the StreamIt
     * Compiler. Given the SIR representation of the application, 
     * this call will create the c code that will be accepted by the 
     * RStream compiler.  The code will be placed in the current
     * working directory.
     *
     * @param str The stream graph
     * @param interfaces Not used 
     * @param interfaceTables Not used
     * @param structs The structures used in this StreamIt application
     * 
     *
     * @exception None
     * 
     */
    public static void run(SIRStream str,
			   JInterfaceDeclaration[] 
			   interfaces,
			   SIRInterfaceTable[]
			   interfaceTables,
			   SIRStructure[]
			   structs) {

	System.out.println("Entry to RStream Conversion");

	structures = structs;
	
	//rename all variables/functions in each filter to be
	//exclusive over all filters...
	//this must be run now, FlatIRToRS relies on it!!!
	RenameAll.renameAllFilters(str);
	
	// move field initializations into init function
	System.out.print("Moving initializers into init functions... ");
	FieldInitMover.moveStreamInitialAssignments(str);
	System.out.println("done.");
	
	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println("Done Constant Prop and Unroll.");

        // add initPath functions
        EnqueueToInitPath.doInitPath(str);

	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);

	//SIRPrinter printer1 = new SIRPrinter();
	//str.accept(printer1);
	//printer1.close();

	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(str);
	// do constant propagation on fields
	if (KjcOptions.nofieldprop) {
	} else {
	    System.out.println("Running Constant Field Propagation...");
	    FieldProp.doPropagate(str);
	    System.out.println("Done Constant Field Propagation...");
		//System.out.println("Analyzing Branches..");
		//new BlockFlattener().flattenBlocks(str);
		//new BranchAnalyzer().analyzeBranches(str);
	}

	//convert all file readers/writers to normal 
	//sirfilters, not predefined filters
	ConvertFileFilters.doit(str);

	
	Lifter.liftAggressiveSync(str);
	StreamItDot.printGraph(str, "before-partition.dot");

	//mgordon: I don't know, I could forsee the linear analysis 
	//and the statespace analysis being useful to Reservoir in the 
	//future...but don't run it now, there is no point.
	//	str = Flattener.doLinearAnalysis(str);
	//      str = Flattener.doStateSpaceAnalysis(str);
	

	//Partition the Stream Graph, fuse it down to one tile
	int count = new GraphFlattener(str).getNumTiles();
	int numTiles = 1;
	
	assert (KjcOptions.partition_dp) : 
	    "The RStream option must turn on Partitioning";

	System.err.println("Running Partitioning...");
	str = Partitioner.doit(str,      //stream graph
			       count,    //initial count of filters and collasped joiners
			       numTiles, //always 1
			       true);    //joiner's need tiles to true
	System.err.println("Done Partitioning...");
	
	//if Splitjoin to pipe is enabled, run it...
	if (KjcOptions.sjtopipe) {
	    SJToPipe.doit(str);
	}
	
	StreamItDot.printGraph(str, "after-partition.dot");

	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(str);
	
	
	//VarDecl Raise to move peek index up so
	//constant prop propagates the peek buffer index
	new VarDeclRaiser().raiseVars(str);
	
	// optionally print a version of the source code that we're
	// sending to the scheduler
	if (KjcOptions.print_partitioned_source) {
	    new streamit.scheduler2.print.PrintProgram().printProgram(IterFactory.createFactory().createIter(str));
	}
	
	//SIRPrinter printer1 = new SIRPrinter();
	//IterFactory.createFactory().createIter(str).accept(printer1);
	//printer1.close();
	
	System.out.println("Flattener Begin...");
	executionCounts = SIRScheduler.getExecutionCounts(str);
	
	//flatten the "graph", there should be only one filter, so this 
	//just wraps the filter inside a flat node
	GraphFlattener graphFlattener = new GraphFlattener(str);
	System.out.println("Flattener End.");
	
	//Generate the tile code
	ExecutionCode.doit(graphFlattener.top, executionCounts);

	if (KjcOptions.removeglobals) {
	    RemoveGlobals.doit(graphFlattener.top);
	}
	
	//VarDecl Raise to move array assignments down?
	new VarDeclRaiser().raiseVars(str);
	
	System.out.println("Tile Code begin...");
	FlatIRToRS.generateCode(graphFlattener.top);
	System.out.println("Tile Code End.");

	System.exit(0);
    }

    /**
     *  Helper function to add everything in a collection to the set
     *
     * @param set The Hashset we want to add <c> to
     * @param c   The collection to add
     *
     */
    public static void addAll(HashSet set, Collection c) 
    {
	Iterator it = c.iterator();
	while (it.hasNext()) {
	    set.add(it.next());
	}
    }
}


package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.reordering.*;
import at.dms.kjc.sir.linear.*; 
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.*; 

/**
 * This is the main class for decomposing the high SIR into
 * lower-level function calls.
 */
public class Flattener {
    /**
     * This variable is toggled once SIRInitStatements have been
     * eliminated in favor of a hierarchical stream represenation
     * within the SIRContainers.
     */
    public static boolean INIT_STATEMENTS_RESOLVED = false;

    /**
     * Flattens <str> into a low IR representation, given that <interfaces>
     * are all the top-level interfaces declared in the program and 
     * <interfaceTables> represents the mapping from interfaces to methods
     * that implement a given interface in a given class.
     */
    public static JClassDeclaration flatten(SIRStream str,
					    JInterfaceDeclaration[] 
					    interfaces,
					    SIRInterfaceTable[]
					    interfaceTables,
                                            SIRStructure[] structs) {
	// DEBUGGING PRINTING
        System.out.println("--------- ON ENTRY TO FLATTENER ----------------");
	SIRPrinter printer1 = new SIRPrinter();
	IterFactory.createIter(str).accept(printer1);
	printer1.close();

	// move field initializations into init function
	FieldInitMover.moveStreamInitialAssignments(str);

	// propagate constants and unroll loops
	ConstantProp.propagateAndUnroll(str);

	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);
	INIT_STATEMENTS_RESOLVED = true;

	// dump the original graph to a dot format
	StreamItDot.printGraph(str, "before.dot");

	AdjustGranularity.doit(str, -1);

	if (StreamItOptions.partition) {
	    Partitioner.doit(str, 
			     StreamItOptions.rawRows *
			     StreamItOptions.rawColumns);
	}

	/* Not general code: Just a test for sync-removal on TwoWeightedRR.java */ 
	/* StreamItDot.printGraph(str, "before-syncremov.dot");
	SIRPipeline parentPipe = (SIRPipeline)str; 
	SyncRemovalSJPair.diffuseSJPair((SIRSplitJoin)parentPipe.get(1), (SIRSplitJoin)parentPipe.get(2)); 
	StreamItDot.printGraph(str, "after-syncremov.dot"); */ 

	/*
	SIRFilter toDuplicate = ((SIRFilter)
				 ((SIRPipeline)
				  ((SIRPipeline)str).get(1)).get(0));
	System.err.println("Trying to duplicate " + toDuplicate);
	StatelessDuplicate.doit(toDuplicate, 2);
	*/

	if (StreamItOptions.fusion) {
	    System.out.println("Running Fusion");
	    FuseAll.fuse(str);
	    // DEBUGGING PRINTING
	    System.out.println("--------- AFTER FUSION ------------");
	    printer1 = new SIRPrinter();
	    IterFactory.createIter(str).accept(printer1);
	    printer1.close();
	    
	}

	// dump the original graph to a dot format
	StreamItDot.printGraph(str, "after.dot");

	//Raise NewArray's up to top
	new VarDeclRaiser().raiseVars(str);
	
        // do constant propagation on fields
        if (StreamItOptions.constprop) {
	    System.out.println("Running Constant Propagation of Fields");
	    FieldProp.doPropagate(str);
	}

	// move field initializations into init function
	FieldInitMover.moveStreamInitialAssignments(str);

	// DEBUGGING PRINTING
	System.out.println("--------- AFTER CONSTANT PROP / FUSION --------");
	printer1 = new SIRPrinter();
	IterFactory.createIter(str).accept(printer1);
	printer1.close();
	
	if (StreamItOptions.constprop) {
	    //Flatten Blocks
	    new BlockFlattener().flattenBlocks(str);
	    //Analyze Branches
	    new BranchAnalyzer().analyzeBranches(str);
	}
	//Destroys arrays into local variables if possible
	new ArrayDestroyer().destroyArrays(str);
	//Raise variables to the top of their block
	new VarDeclRaiser().raiseVars(str);

	// if someone wants to run linear analysis:
	if (StreamItOptions.linearanalysis) {
	    runLinearAnalysis(str);
	}

	
	// make single structure
	SIRIterator iter = IterFactory.createIter(str);
	JClassDeclaration flatClass = Structurer.structure(iter,
							   interfaces,
							   interfaceTables,
                                                           structs);
	// build schedule as set of higher-level work functions
	Schedule schedule = SIRScheduler.buildWorkFunctions(str, flatClass);
	// add LIR hooks to init and work functions
	LowerInitFunctions.lower(iter, schedule);
        LowerWorkFunctions.lower(iter);

	// DEBUGGING PRINTING
	System.out.println("----------- AFTER FLATTENER ------------------");
	IRPrinter printer = new IRPrinter();
	flatClass.accept(printer);
	printer.close();

	return flatClass;
    }


    /** Runs linear analysis (and associated optimizations) on the passed stream. **/
    static void runLinearAnalysis(SIRStream str) {
	System.out.println("Running Linear Analysis");
	//Destroys arrays into local variables if possible
 
	LinearFilterAnalyzer lfa = LinearFilterAnalyzer.findLinearFilters(str,
									  StreamItOptions.debug);
	
	// DEBUGGING PRINTING
	System.out.println("--------- AFTER Linear Analysis --------");
    }

    
}

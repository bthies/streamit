package at.dms.kjc.sir.lowering;

import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.partition.linear.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.sir.lowering.reordering.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.linear.frequency.*; 
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.*; 

/**
 * This is the main class for decomposing the high SIR into
 * lower-level function calls for the uniprocessor backend.
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
    public static void flatten(SIRStream str,
			       JInterfaceDeclaration[] 
			       interfaces,
			       SIRInterfaceTable[]
			       interfaceTables,
			       SIRStructure[] structs) {
	/* DEBUGGING PRINTING *
        System.out.println("--------- ON ENTRY TO FLATTENER ----------------");
	SIRPrinter printer1 = new SIRPrinter();
	IterFactory.createIter(str).accept(printer1);
	printer1.close();
	*/

	// move field initializations into init function
	FieldInitMover.moveStreamInitialAssignments(str);

	// propagate constants and unroll loops
	System.err.print("Running Constant Prop and Unroll... ");
	ConstantProp.propagateAndUnroll(str);
	System.err.println("done.");

        // Add initPath functions
        EnqueueToInitPath.doInitPath(str);

	// Convert Peeks to Pops
	if (KjcOptions.poptopeek) {
	    System.err.print("Converting pop to peek... ");
	    PopToPeek.removeAllPops(str);
	    ConstantProp.propagateAndUnroll(str);
	    System.err.println("done.");
	}
	
	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);
	INIT_STATEMENTS_RESOLVED = true;

	lowerFilterContents(str, true);

	Lifter.liftAggressiveSync(str);
	// dump the original graph to a dot format
	StreamItDot.printGraph(str, "before.dot");

	// gather application-characterization statistics
	if (KjcOptions.stats) {
	    StatisticsGathering.doit(str);
	}

	if (KjcOptions.fusion) {
	    System.err.print("Running FuseAll...");
	    str = FuseAll.fuse(str);
	    Lifter.lift(str);
	    System.err.println("done.");
	}

	if (KjcOptions.partition || KjcOptions.ilppartition || KjcOptions.dppartition) {
	    System.err.print("Partitioning...");
	    str = Partitioner.doit(str, 
				   KjcOptions.raw * KjcOptions.raw);
	    System.err.println("done.");
	}

	System.err.print("Raising variable declarations... ");
	new VarDeclRaiser().raiseVars(str);
	System.err.println("done.");

	// move field initializations into init function
	System.err.print("Moving initial assignments... ");
	FieldInitMover.moveStreamInitialAssignments(str);
	System.err.println("done.");

	if (KjcOptions.sjtopipe) {
	    SJToPipe.doit(str);
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

	str = doLinearAnalysis(str);

	// dump the partitioned graph to a dot format
	StreamItDot.printGraph(str, "after.dot");

	// if we have don't have a container, wrap it in a pipeline
	// for the sake of SIRScheduler.
	if (!(str instanceof SIRContainer)) {
	    str = SIRContainer.makeWrapper(str);
	}

	// make single structure
	SIRIterator iter = IterFactory.createIter(str);
	System.err.print("Structuring... ");
	JClassDeclaration flatClass = Structurer.structure(iter,
							   interfaces,
							   interfaceTables,
                                                           structs);
	System.err.println("done.");

	// optionally print a version of the source code that we're
	// sending to the scheduler
	if (KjcOptions.print_partitioned_source) {
	    new streamit.scheduler2.print.PrintProgram().printProgram(IterFactory.createIter(str));
	}

	// build schedule as set of higher-level work functions
	System.err.print("Scheduling... ");
	SIRSchedule schedule = SIRScheduler.buildWorkFunctions((SIRContainer)str, flatClass);
	System.err.println("done.");
	// add LIR hooks to init and work functions
	System.err.print("Annotating IR for uniprocessor... ");
	LowerInitFunctions.lower(iter, schedule);
        LowerWorkFunctions.lower(iter);
	System.err.println("done.");

	/* DEBUGGING PRINTING
	System.out.println("----------- AFTER FLATTENER ------------------");
	IRPrinter printer = new IRPrinter();
	flatClass.accept(printer);
	printer.close();
	*/

	System.err.println("Generating code...");
	LIRToC.generateCode(flatClass);
	//System.err.println("done.");
    }
    
    /**
     * Lowers the contents of filters <str> as is appropriate along
     * the main compilation path.  Also has the effect of doing
     * constant propagation and unrolling (with appropriate setup and
     * cleanup) for all filters in str.  Doesn't unroll any loops that
     * haven't already been unrolled.
     */
    public static void lowerFilterContents(SIRStream str, boolean printStatus) {
	//Raise NewArray's up to top
	if (printStatus) { System.err.print("Raising variable declarations... "); }
	new VarDeclRaiser().raiseVars(str);
	if (printStatus) { System.err.println("done."); }
	
	/* aal -- changed so that the default action is to do field prop.
	 * turn off field prop using --nofieldprop or -L */
	// do constant propagation on fields
        if (KjcOptions.nofieldprop) {
	} else {
	    if (printStatus) { System.err.print("Propagating constant fields... "); }
	    FieldProp.doPropagate(str);
	    if (printStatus) { System.err.println("done."); }
	}

	/* dzm -- note phase ordering issue here.  In particular, we
	 * probably want to form filter phases before fusing the world, but we need
	 * to run field prop before forming phases. */
	// resolve phases in phased filters
	FilterPhaser.resolvePhasedFilters(str);    
        
	/* DEBUGGING PRINTING
	System.out.println("--------- AFTER CONSTANT PROP / FUSION --------");
	printer1 = new SIRPrinter();
	IterFactory.createIter(str).accept(printer1);
	printer1.close();
	*/
	
	if (KjcOptions.nofieldprop) {
	} else {
	    //Flatten Blocks
	    if (printStatus) { System.err.print("Flattening blocks... "); }
	    new BlockFlattener().flattenBlocks(str);
	    if (printStatus) { System.err.println("done."); }
	    //Analyze Branches
	    //System.err.print("Analyzing branches... ");
	    //new BranchAnalyzer().analyzeBranches(str);
	    //System.err.println("done.");
	}
	//Destroys arrays into local variables if possible
	//System.err.print("Destroying arrays... ");
	//new ArrayDestroyer().destroyArrays(str);
	//System.err.println("done.");
	//Raise variables to the top of their block
	if (printStatus) { System.err.print("Raising variables... "); }
	new VarDeclRaiser().raiseVars(str);
	if (printStatus) { System.err.println("done."); }
    }

    /**
     * Returns new value of <str>.
     */
    public static SIRStream doLinearAnalysis(SIRStream str) {

	// if someone wants to run any of the linear tools/optimizations
	// we need to run linear analysis first to extract the information
	// we are working with.
	if (KjcOptions.linearanalysis ||
	    KjcOptions.linearreplacement ||
	    KjcOptions.linearreplacement2 ||
	    KjcOptions.linearreplacement3 ||
	    KjcOptions.atlas ||
	    KjcOptions.linearpartition ||
	    KjcOptions.frequencyreplacement ||
	    KjcOptions.redundantreplacement) {

	    // run the linear analysis and stores the information garnered in the lfa
	    System.err.println("Running linear analysis... ");
	    // only refactor linear children if we're NOT doing the linear partitioner
	    LinearAnalyzer lfa = LinearAnalyzer.findLinearFilters(str,
								  KjcOptions.debug,
								  !KjcOptions.linearpartition);
	    System.err.println("done with linear analysis.");

	    // now, print out the graph using the LinearPrinter which colors the graph
	    // nodes based on their linearity.
	    LinearDot.printGraph(str, "linear.dot", lfa);
	    LinearDotSimple.printGraph(str, "linear-simple.dot", lfa, null);

	    // if we are doing linear partitioning, it will take care
	    // of linear and frequency replacement automatically
	    if (KjcOptions.linearpartition) {
		str = new LinearPartitioner(str, lfa).toplevel();
	    } else { 
		// and finally, if we want to run frequency analysis
		// 0 means stupid implementation, 1 means nice implemenation
		if (KjcOptions.frequencyreplacement) {
		    System.err.print("Running frequency replacement...");
		    FrequencyReplacer.doReplace(lfa, str);
		    System.err.println("done.");
		    LinearDot.printGraph(str, ("linear-frequency.dot"), lfa);
		}

		// otherwise, test for linear and frequency
		// replacement separately...

		// if we are supposed to transform the graph
		// by replacing work functions with their linear forms, do so now 
		if (KjcOptions.linearreplacement) {
		    System.err.print("Running linear replacement... ");
		    LinearDirectReplacer.doReplace(lfa, str);
		    System.err.println("done.");
		    // print out the stream graph after linear replacement
		    LinearDot.printGraph(str, "linear-replace.dot", lfa);
		}

		// if we are supposed to transform the graph
		// by replacing work functions with their linear forms (using indirection)
		if (KjcOptions.linearreplacement2) {
		    System.err.print("Running indirect linear replacement... ");
		    LinearIndirectReplacer.doReplace(lfa, str);
		    System.err.println("done.");
		    // print out the stream graph after linear replacement
		    LinearDot.printGraph(str, "linear-indirect-replace.dot", lfa);
		}

		// if we are supposed to transform the graph
		// by replacing work functions with diagonal matrix multiplies
		if (KjcOptions.linearreplacement3) {
		    System.err.print("Running diagonal linear replacement... ");
		    LinearDiagonalReplacer.doReplace(lfa, str);
		    System.err.println("done.");
		    // print out the stream graph after linear replacement
		    LinearDot.printGraph(str, "linear-diagonal-replace.dot", lfa);
		}

		// if we are supposed to transform the graph
		// by replacing work functions with diagonal matrix multiplies
		if (KjcOptions.atlas) {
		    System.err.print("Running ATLAS linear replacement... ");
		    LinearAtlasReplacer.doReplace(lfa, str);
		    System.err.println("done.");
		    // print out the stream graph after linear replacement
		    LinearDot.printGraph(str, "linear-atlas-replace.dot", lfa);
		}

	    }

	    if (KjcOptions.redundantreplacement) {
		System.err.print("Running redundancy analysis... ");	    
		// now, run a redundancy analysis pass and print the results
		LinearRedundancyAnalyzer lra = new LinearRedundancyAnalyzer(lfa);
		System.err.println("done.");
		// print dot graph for redundancy information
		LinearDot.printGraph(str, "linear-redundant.dot", lfa, lra);
		
		// do the redundancy replacement
		System.err.print("Running anti-redundant replacement...");
		LinearRedundancyReplacer.doReplace(lfa, lra, str);
		System.err.println("done.");
		// print out the stream graph after linear redundant replacement
		LinearDot.printGraph(str, "linear-redundant-replace.dot", lfa);
	    }

	}
	Lifter.liftAggressiveSync(str);
	StreamItDot.printGraph(str, "linear-total-post-lift.dot");
	return str;
    }
    
}

package at.dms.kjc.spacedynamic;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatGraphToSIR;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.kjc.flatgraph.DumpGraph;
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

public class SpaceDynamicBackend {
    //the stream  graph object that represents the application...
    public static StreamGraph streamGraph;
    //the raw chip that we are executing on...
    public static RawChip rawChip;

    public static SIRStructure[] structures;

    //if true have each filter print out each value it is pushing
    //onto its output tape
    public static boolean FILTER_DEBUG_MODE = false;

    public static void run(SIRStream str,
			   JInterfaceDeclaration[] 
			   interfaces,
			   SIRInterfaceTable[]
			   interfaceTables,
			   SIRStructure[]
			   structs) {

	System.out.println("Entry to RAW Backend");
	
	//alt code gen has to be enabled for this pass to work
	KjcOptions.altcodegen = true;

	structures = structs;
	
	assert structures.length > 0 : "The Space Dynamic backend assumes that at least one struct is defined";

	int rawRows = -1;
	int rawColumns = -1;

	//set number of columns/rows
	rawRows = KjcOptions.raw;
	if(KjcOptions.rawcol>-1)
	    rawColumns = KjcOptions.rawcol;
	else
	    rawColumns = KjcOptions.raw;

	//create the RawChip
	rawChip = new RawChip(rawColumns, rawRows);

	//this must be run now, FlatIRToC relies on it!!!
	RenameAll.renameAllFilters(str);
	
	// move field initializations into init function
	System.out.print("Moving initializers into init functions... ");
	FieldInitMover.moveStreamInitialAssignments(str,
	      FieldInitMover.IGNORE_ARRAY_INITIALIZERS);
	System.out.println("done.");
	
	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println("Done Constant Prop and Unroll...");

        // add initPath functions
        EnqueueToInitPath.doInitPath(str);

	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);

	FieldProp.doPropagate(str);

	/*
	if (Flattener.hasDynamicRates(str)) {
	    System.err.println("Failure: Dynamic rates are not yet supported in the Raw backend.");
	    System.exit(1);
	}
	*/
	
	//first of all, flatten the graph to make it easier to deal with...
	GraphFlattener graphFlattener = new GraphFlattener(str);
	//	FlatGraphToSIR flatToSIR = new FlatGraphToSIR(graphFlattener.top);
	
	streamGraph = new StreamGraph(graphFlattener.top, rawChip);
	(new DumpGraph()).dumpGraph(graphFlattener.top, "pre-SSG-FG.dot", null, null);

	//create the static stream graphs cutting at dynamic rate boundaries
	streamGraph.createStaticStreamGraphs();

	//assign tiles to each static stream graph
	//	streamGraph.tileAssignment();
	streamGraph.handTileAssignment();
		
	//dump a dot representation of the graph
	streamGraph.dumpStaticStreamGraph();
	
	for (int k = 0; k < streamGraph.getStaticSubGraphs().length; k++) {
	    StaticStreamGraph ssg = streamGraph.getStaticSubGraphs()[k];
	    System.out.println(" ****** Static Sub-Graph = " + ssg.toString() + " ******");
	    
	    //SIRPrinter printer1 = new SIRPrinter();
	    //subStr.accept(printer1);
	    //printer1.close();
	    
	    //VarDecl Raise to move array assignments up
	    new VarDeclRaiser().raiseVars(ssg.getTopLevelSIR());


	    
	    // loop to decrease unroll factor until everything fits in IMEM
	    SIRStream strOrig = null;
	    // only need to make copy if there is some unrolling, since
	    // otherwise we won't roll back
	    boolean scaleUnrollFactor = KjcOptions.unroll>1 && !KjcOptions.forceunroll && !KjcOptions.standalone;
	    if (scaleUnrollFactor) {
		strOrig = (SIRStream)ObjectDeepCloner.deepCopy(ssg.getTopLevelSIR());
	    }
	    boolean fitsInIMEM;

	    do {
		
		// do constant propagation on fields
		if (KjcOptions.nofieldprop) {
		} else {
		    System.out.println("Running Constant Field Propagation...");
		    FieldProp.doPropagate(ssg.getTopLevelSIR());
		    System.out.println("Done Constant Field Propagation...");
		    //System.out.println("Analyzing Branches..");
		    //new BlockFlattener().flattenBlocks(ssg.getTopLevelSIR());
		    //new BranchAnalyzer().analyzeBranches(ssg.getTopLevelSIR());
		}
		
		Lifter.liftAggressiveSync(ssg.getTopLevelSIR());
		NumberDot.printGraph(ssg.getTopLevelSIR(), makeDotFileName("numbered", ssg.getTopLevelSIR()));
		StreamItDot.printGraph(ssg.getTopLevelSIR(), makeDotFileName("before-partition", ssg.getTopLevelSIR()));
		
		// gather application-characterization statistics
		if (KjcOptions.stats) {
		    StatisticsGathering.doit(ssg.getTopLevelSIR());
		}
		
		//ssg.setTopLevelSIR(Flattener.doLinearAnalysis(ssg.getTopLevelSIR()));
		//ssg.setTopLevelSIR(Flattener.doStateSpaceAnalysis(ssg.getTopLevelSIR()));
		
		if (KjcOptions.fusion) {
		    System.out.println("Running FuseAll...");
		    ssg.setTopLevelSIR(FuseAll.fuse(ssg.getTopLevelSIR()));
		    Lifter.lift(ssg.getTopLevelSIR());
		    System.out.println("Done FuseAll...");
		}
		
		if (KjcOptions.fission>1) {
		    System.out.println("Running Vertical Fission...");
		    FissionReplacer.doit(ssg.getTopLevelSIR(), KjcOptions.fission);
		    Lifter.lift(ssg.getTopLevelSIR());
		    System.out.println("Done Vertical Fission...");
		}
		
		// turn on partitioning if there aren't enough tiles for all
		// the filters
		int count = new GraphFlattener(ssg.getTopLevelSIR()).getNumTiles();
		//partition this sub graph based on the number of tiles it is assigned...
		int numTiles = ssg.getNumTiles();//SpaceDynamicBackend.rawRows * SpaceDynamicBackend.rawColumns;
		boolean manual = KjcOptions.manual != null;
		boolean partitioning = ((KjcOptions.standalone || !manual) // still fuse graph if both manual and standalone enabled
					&& (KjcOptions.partition_dp || KjcOptions.partition_greedy || KjcOptions.partition_greedier || KjcOptions.partition_ilp));
		// want to turn on partitioning for standalone; in this
		// case, manual is for manual optimizations, not manual
		// partitioning
		if (count>numTiles && !partitioning && !manual) { //
		    System.out.println("Need " + count + " tiles, so turning on partitioning...");
		    KjcOptions.partition_dp = true;
		    partitioning = true;
		}
		
		if (manual) {
		    System.err.println("Running Manual Partitioning...");
		    ssg.setTopLevelSIR(ManualPartition.doit(ssg.getTopLevelSIR()));
		    System.err.println("Done Manual Partitioning...");
		}
		
		if (partitioning) {
		    System.err.println("Running Partitioning...");
		    System.err.println("  Do not fuse: ");
		    HashSet doNotHorizFuse = ssg.getIOFilters();
		    Iterator it = doNotHorizFuse.iterator();
		    while (it.hasNext())
			System.out.println("   * " + it.next());
		    
		    ssg.setTopLevelSIR(Partitioner.doit(ssg.getTopLevelSIR(), count, 
							numTiles, true, false, doNotHorizFuse));
		    System.err.println("Done Partitioning...");
		}
		
		if (KjcOptions.sjtopipe) {
		    SJToPipe.doit(ssg.getTopLevelSIR());
		}
		
		StreamItDot.printGraph(ssg.getTopLevelSIR(), makeDotFileName("after-partition", ssg.getTopLevelSIR()));
		
		//VarDecl Raise to move array assignments up
		new VarDeclRaiser().raiseVars(ssg.getTopLevelSIR());
		
		
		//VarDecl Raise to move peek index up so
		//constant prop propagates the peek buffer index
		new VarDeclRaiser().raiseVars(ssg.getTopLevelSIR());
		
		// optionally print a version of the source code that we're
		// sending to the scheduler
		if (KjcOptions.print_partitioned_source) {
		    new streamit.scheduler2.print.PrintProgram().printProgram
			(IterFactory.createFactory().createIter(ssg.getTopLevelSIR()));
		}
		
		//SIRPrinter printer1 = new SIRPrinter();
		//IterFactory.createFactory().createIter(ssg.getTopLevelSIR()).accept(printer1);
		//printer1.close();
		
		/** Flatten the subgraph and create the flat node representation 
		 now we can use the flatgraph representation **/
		ssg.scheduleAndFlattenGraph();

		// see if we are going to overflow IMEM
		if (scaleUnrollFactor) {
		    System.out.println("Trying unroll factor " + KjcOptions.unroll);
		    fitsInIMEM = IMEMEstimation.testMe(ssg, ssg.getTopLevel());
		    if (fitsInIMEM) {
			// if we fit, clear backup copy of stream graph
			strOrig = null;
			System.gc();
		    } else if (KjcOptions.unroll<=1) {
			// if we have reached bottom of unrolling, print warning
			System.out.println("WARNING:  A filter overflows IMEM even though there is no unrolling.");
			// so that we exit the loop
			fitsInIMEM=true;
		    } else {
			// otherwise, cut unrolling in half and recurse
			System.out.println("Cutting unroll factor from " + KjcOptions.unroll + " to " + (KjcOptions.unroll/2) + " to try to fit in IMEM...");
			KjcOptions.unroll = KjcOptions.unroll / 2;
			ssg.setTopLevelSIR((SIRStream)ObjectDeepCloner.deepCopy(strOrig));
		    }
		} else {
		    // it might not fit in IMEM, but we can't decrease the
		    // unrolling any, so just go ahead
		    fitsInIMEM = true;
		}
		
	    } while (!fitsInIMEM);    
	}
	
	//see if we can remove any joiners, doesn't run in the old space backend...
	//JoinerRemoval.run(ssg.getTopLevel());
	
	// layout the components (assign filters to tiles)	
	streamGraph.layoutGraph();
	System.out.println("Assign End.");
	
	//if rate matching is requested, check if we can do it
	//if we can, then keep KjcOptions.rateMatch as true, 
	//otherwise set it to false
	
	
	if (KjcOptions.ratematch) {
	    assert false;
	    /*
	    if (RateMatch.doit(ssg.getTopLevel()))
		System.out.println("Rate Matching Test Successful.");
	    else {
		KjcOptions.ratematch = false;
		System.out.println("Cannot perform Rate Matching.");
	    }
	    */
	}
	
	if (KjcOptions.magic_net) {
	    assert false;
	    //MagicNetworkSchedule.generateSchedules(ssg.getTopLevel());
	}
	else {
	    System.out.println("Switch Code Begin...");
	    SwitchCode.generate(streamGraph);
	    System.out.println("Switch Code End.");
	}

	/*
	//remove print statements in the original app
	//if we are running with decoupled
	if (KjcOptions.decoupled)
	RemovePrintStatements.doIt(ssg.getTopLevel());
	*/

	//Generate the tile code
	RawExecutionCode.doit(streamGraph);

	//remove globals over all the SSGs if enabled
	if (KjcOptions.removeglobals) {
	    for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++)
		RemoveGlobals.doit(streamGraph.getStaticSubGraphs()[i].getTopLevel());
	}
	
	//VarDecl Raise to move array assignments down?
	for (int i = 0; i < streamGraph.getStaticSubGraphs().length; i++)
	    new VarDeclRaiser().raiseVars
		(streamGraph.getStaticSubGraphs()[i].getTopLevelSIR());
	
	//create the structure include file for the application
	StructureIncludeFile.doit(structures);

	
	System.out.println("Tile Code begin...");
	TileCode.generateCode(streamGraph);
	System.out.println("Tile Code End.");
	
	//generate the makefiles
	System.out.println("Creating Makefile.");
	MakefileGenerator.createMakefile(streamGraph);
		
	System.out.println("Exiting");
	System.exit(0);
    }

    //helper function to add everything in a collection to the set
    public static void addAll(HashSet set, Collection c) 
    {
	Iterator it = c.iterator();
	while (it.hasNext()) {
	    set.add(it.next());
	}
    }
   
    

    //simple helper function to find the topmost pipeline
    private static SIRStream getTopMostParent (FlatNode node) 
    {
	SIRContainer[] parents = node.contents.getParents();
	return parents[parents.length -1];
    }

    public static String makeDotFileName(String prefix, SIRStream strName) 
    {
	return prefix + (strName != null ? strName.getIdent() : "") + ".dot";
    }
    
}


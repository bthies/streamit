
package at.dms.kjc.cluster;

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
import at.dms.kjc.raw.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;

//import streamit.scheduler2.*;
//import streamit.scheduler2.constrained.*;

public class ClusterBackend {

    //given a flatnode map to the execution count
    //public static HashMap initExecutionCounts;
    //public static HashMap steadyExecutionCounts;
    //the simulator to be run

    //public static Simulator simulator;
    // get the execution counts from the scheduler

    public static HashMap[] executionCounts;
    
    public static SIRStructure[] structures;

    public static void run(SIRStream str,
			   JInterfaceDeclaration[] 
			   interfaces,
			   SIRInterfaceTable[]
			   interfaceTables,
			   SIRStructure[]
			   structs) {

	System.out.println("Entry to Cluster Backend");

	structures = structs;
	
	StructureIncludeFile.doit(structures);

	// set number of columns/rows
	//RawBackend.rawRows = KjcOptions.raw;
	//if(KjcOptions.rawcol>-1)
	//    RawBackend.rawColumns = KjcOptions.rawcol;
	//else
	//    RawBackend.rawColumns = KjcOptions.raw;

	//simulator = new FineGrainSimulator();

	//this must be run now, FlatIRToC relies on it!!!
	RenameAll.renameAllFilters(str);
	
	// move field initializations into init function
	System.out.print("Moving initializers into init functions... ");
	FieldInitMover.moveStreamInitialAssignments(str);
	System.out.println("done.");
	
	// propagate constants and unroll loop
	System.out.print("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println(" done.");

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
	    System.out.print("Running Constant Field Propagation...");
	    FieldProp.doPropagate(str);
	    System.out.println(" done.");
	    //System.out.println("Analyzing Branches..");
	    //new BlockFlattener().flattenBlocks(str);
	    //new BranchAnalyzer().analyzeBranches(str);
	}

	Lifter.liftAggressiveSync(str);
	StreamItDot.printGraph(str, "before.dot");

	// gather application-characterization statistics
	if (KjcOptions.stats) {
	    StatisticsGathering.doit(str);
	}

	Flattener.doLinearAnalysis(str);

	if (KjcOptions.fusion) {
	    System.out.println("Running FuseAll...");
	    str = FuseAll.fuse(str);
	    Lifter.lift(str);
	    System.out.println("Done FuseAll...");
	}

	if (KjcOptions.partition || KjcOptions.ilppartition || KjcOptions.dppartition) {
	    //System.err.println("Running Partitioning...");
	    //	    str = Partitioner.doit(str,
	    //		   RawBackend.rawRows *
	    //		   RawBackend.rawColumns);
	    //System.err.println("Done Partitioning...");
	}

	if (KjcOptions.sjtopipe) {
	    SJToPipe.doit(str);
	}

	StreamItDot.printGraph(str, "after.dot");

	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(str);

	
	//VarDecl Raise to move peek index up so
	//constant prop propagates the peek buffer index
	new VarDeclRaiser().raiseVars(str);

	// optionally print a version of the source code that we're
	// sending to the scheduler
	if (KjcOptions.print_partitioned_source) {
	    new streamit.scheduler2.print.PrintProgram().printProgram(IterFactory.createIter(str));
	}

	//run constrained scheduler

       	System.out.print("Constrained Scheduler Begin...");

	streamit.scheduler2.iriter.Iterator selfIter = 
	    IterFactory.createIter(str);

	streamit.scheduler2.constrained.Scheduler cscheduler =
	    new streamit.scheduler2.constrained.Scheduler(selfIter);

	//cscheduler.computeSchedule(); //"Not Implemented"

	int pipe_size = ((SIRPipeline)str).size();
	
	SIRFilter first = (SIRFilter)((SIRPipeline)str).get(0);
	SIRFilter last = (SIRFilter)((SIRPipeline)str).get(pipe_size-1);

	streamit.scheduler2.iriter.Iterator firstIter = 
	    IterFactory.createIter(first);
	streamit.scheduler2.iriter.Iterator lastIter = 
	    IterFactory.createIter(last);	

	streamit.scheduler2.SDEPData sdep = cscheduler.computeSDEP(firstIter, lastIter);

	for (int t = 0; t < 20; t++) {
	    int phase = sdep.getSrcPhase4DstPhase(t);
	    int phaserev = sdep.getDstPhase4SrcPhase(t);
	    System.out.println("sdep ["+t+"] = "+phase+
			       " reverse_sdep["+t+"] = "+phaserev);
	}

       	System.out.println(" done.");

	// end constrained scheduler

       	System.out.println("Flattener Begin...");
	executionCounts = SIRScheduler.getExecutionCounts(str);
	PartitionDot.printScheduleGraph(str, "schedule.dot", executionCounts);
	GraphFlattener graphFlattener = new GraphFlattener(str);
	graphFlattener.dumpGraph("flatgraph.dot");
	System.out.println("Flattener End.");


	////////////////////////////////////////////////
	// the cluster specific code begins here

	NodeEnumerator.reset();
	graphFlattener.top.accept(new NodeEnumerator(), new HashSet(), true);

	graphFlattener.top.accept(new RegisterStreams(), new HashSet(), true);


	if (KjcOptions.removeglobals) {
	    RemoveGlobals.doit(graphFlattener.top);
	}
	
	//VarDecl Raise to move array assignments down?
	new VarDeclRaiser().raiseVars(str);


	System.out.println("Cluster Code begin...");

	ClusterCode.generateCode(graphFlattener.top);
	ClusterCode.generateMasterFile();
	ClusterCode.generateMakeFile();
	ClusterCode.generateConfigFile();

	System.out.println("Cluster Code End.");	

	/*
	//generate the makefiles
	System.out.println("Creating Makefile.");
	MakefileGenerator.createMakefile();
	*/

	System.out.println("Exiting");
	System.exit(0);
    }
}

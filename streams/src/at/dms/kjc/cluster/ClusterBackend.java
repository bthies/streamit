 
package at.dms.kjc.cluster;

import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.flatgraph.GraphFlattener;
import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.partition.dynamicprog.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.raw.*;
import java.util.*;
import java.io.*;
import at.dms.util.Utils;

//import streamit.scheduler2.*;
//import streamit.scheduler2.constrained.*;

public class ClusterBackend implements FlatVisitor {

    //given a flatnode map to the execution count
    //public static HashMap initExecutionCounts;
    //public static HashMap steadyExecutionCounts;
    //the simulator to be run

    //public static Simulator simulator;
    // get the execution counts from the scheduler


    //given a flatnode map to the execution count
    public static HashMap initExecutionCounts;
    public static HashMap steadyExecutionCounts;

    public static HashMap filter2Node;

    public static HashMap[] executionCounts;
    
    public static SIRStructure[] structures;

    
    //if true have each filter print out each value it is pushing
    //onto its output tape
    public static boolean FILTER_DEBUG_MODE = false;

    public static streamit.scheduler2.iriter.Iterator topStreamIter; 
    
    public void visitNode(FlatNode node) 
    {
	filter2Node.put(node.contents, node);
    }

    public static void run(SIRStream str,
			   JInterfaceDeclaration[] 
			   interfaces,
			   SIRInterfaceTable[]
			   interfaceTables,
			   SIRStructure[]
			   structs) {

	System.out.println("Entry to Cluster Backend");
	System.out.println("  --cluster parameter is: "+KjcOptions.cluster);

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
	FieldInitMover.moveStreamInitialAssignments(str,
	      FieldInitMover.IGNORE_ARRAY_INITIALIZERS);
	System.out.println("done.");
	
	// propagate constants and unroll loop
	System.out.print("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println(" done.");

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
	System.out.print("Running Constant Field Propagation...");
	FieldProp.doPropagate(str);
	System.out.println(" done.");
	//System.out.println("Analyzing Branches..");
	//new BlockFlattener().flattenBlocks(str);
	//new BranchAnalyzer().analyzeBranches(str);

	SIRPortal.findMessageStatements(str);

	Lifter.liftAggressiveSync(str);
	StreamItDot.printGraph(str, "before-partition.dot");

	// gather application-characterization statistics
	if (KjcOptions.stats) {
	    StatisticsGathering.doit(str);
	}

	str = Flattener.doLinearAnalysis(str);
	str = Flattener.doStateSpaceAnalysis(str);

	/* for cluster backend, fusion means to fuse segments on same cluster
	if (KjcOptions.fusion) {
	    System.out.println("Running FuseAll...");
	    str = FuseAll.fuse(str);
	    Lifter.lift(str);
	    System.out.println("Done FuseAll...");
	}
	*/

	int threads = KjcOptions.cluster;
	System.err.println("Running Partitioning... target number of threads: "+threads);
	// actually fuse components if fusion flag is enabled
	if (KjcOptions.fusion) {
	    // turn on dynamic programming if no other partitioning is turned on
	    if (!KjcOptions.partition_greedy && !KjcOptions.partition_greedier) {
		KjcOptions.partition_dp = true;
	    }
	    str = Partitioner.doit(str, 0, threads, false);
	}
	HashMap partitionMap = new HashMap();
	str = new DynamicProgPartitioner(str, WorkEstimate.getWorkEstimate(str), threads, false).calcPartitions(partitionMap);
	System.err.println("Done Partitioning...");

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

	//run constrained scheduler

       	System.out.print("Constrained Scheduler Begin...");

	//topStreamIter = IterFactory.createFactory().createIter(str);
	topStreamIter = IterFactory.createFineGrainedFactory().createIter(str);
	//new streamit.scheduler2.print.PrintGraph().printProgram(topStreamIter);
	//new streamit.scheduler2.print.PrintProgram().printProgram(topStreamIter);

	if (KjcOptions.debug) {
	    debugOutput(str);
	}

       	System.out.println(" done.");

	// end constrained scheduler

       	System.out.println("Flattener Begin...");
	executionCounts = SIRScheduler.getExecutionCounts(str);
	PartitionDot.printScheduleGraph(str, "schedule.dot", executionCounts);
	GraphFlattener graphFlattener = new GraphFlattener(str);
	graphFlattener.dumpGraph("flatgraph.dot");
	System.out.println("Flattener End.");

	//create the execution counts for other passes
	createExecutionCounts(str, graphFlattener);


	////////////////////////////////////////////////
	// the cluster specific code begins here

	NodeEnumerator.reset();
	graphFlattener.top.accept(new NodeEnumerator(), new HashSet(), true);
	graphFlattener.top.accept(new RegisterStreams(), new HashSet(), true);


	if (KjcOptions.removeglobals) {
	    RemoveGlobals.doit(graphFlattener.top);
	}


	/// start output portals

	SIRPortal portals[] = SIRPortal.getPortals();

	LatencyConstraints.detectConstraints(topStreamIter, portals);
	
	/// end output portals

	
	//VarDecl Raise to move array assignments down?
	new VarDeclRaiser().raiseVars(str);

	// creating filter2Node
	filter2Node = new HashMap();
	graphFlattener.top.accept(new ClusterBackend(), null, true); 

	//generating code for partitioned nodes
	ClusterExecutionCode.doit(graphFlattener.top);

	System.out.println("Cluster Code begin...");

	ClusterFusion.setPartitionMap(partitionMap);
	graphFlattener.top.accept(new ClusterFusion(), new HashSet(), true);

	ClusterCode.setPartitionMap(partitionMap);
	ClusterCode.generateCode(graphFlattener.top);
	ClusterCode.generateMasterFile();
	ClusterCode.generateMakeFile();
	ClusterCode.generateConfigFile();
	ClusterCode.generateSetupFile();

	System.out.println("Cluster Code End.");	

	/*
	//generate the makefiles
	System.out.println("Creating Makefile.");
	MakefileGenerator.createMakefile();
	*/

	System.out.println("Exiting");
	System.exit(0);
    }

    /**
     * Just some debugging output.
     */
    private static void debugOutput(SIRStream str) {
	streamit.scheduler2.constrained.Scheduler cscheduler =
	    new streamit.scheduler2.constrained.Scheduler(topStreamIter);

	//cscheduler.computeSchedule(); //"Not Implemented"

	if (!(str instanceof SIRPipeline)) return;
	
	int pipe_size = ((SIRPipeline)str).size();
	
	SIRFilter first = (SIRFilter)((SIRPipeline)str).get(0);
	SIRFilter last = (SIRFilter)((SIRPipeline)str).get(pipe_size-1);

	streamit.scheduler2.iriter.Iterator firstIter = 
	    IterFactory.createFactory().createIter(first);
	streamit.scheduler2.iriter.Iterator lastIter = 
	    IterFactory.createFactory().createIter(last);	

	streamit.scheduler2.SDEPData sdep;

	try {
	    sdep = cscheduler.computeSDEP(firstIter, lastIter);

	    System.out.println("\n");
	    System.out.println("Source --> Sink Dependency:\n");

	    System.out.println("  Source Init Phases: "+sdep.getNumSrcInitPhases());
	    System.out.println("  Destn. Init Phases: "+sdep.getNumDstInitPhases());
	    System.out.println("  Source Steady Phases: "+sdep.getNumSrcSteadyPhases());
	    System.out.println("  Destn. Steady Phases: "+sdep.getNumDstSteadyPhases());
	    
	    
	    /*
	    for (int t = 0; t < 20; t++) {
		int phase = sdep.getSrcPhase4DstPhase(t);
		int phaserev = sdep.getDstPhase4SrcPhase(t);
		System.out.println("sdep ["+t+"] = "+phase+
				   " reverse_sdep["+t+"] = "+phaserev);
	    }
	    */

	} catch (streamit.scheduler2.constrained.NoPathException ex) {

	}
	DoSchedules.findSchedules(topStreamIter, firstIter, str);
    }
   
    private static void createExecutionCounts(SIRStream str,
					      GraphFlattener graphFlattener) {
	// make fresh hashmaps for results
	HashMap[] result = { initExecutionCounts = new HashMap(), 
			     steadyExecutionCounts = new HashMap()} ;

	// then filter the results to wrap every filter in a flatnode,
	// and ignore splitters
	for (int i=0; i<2; i++) {
	    for (Iterator it = executionCounts[i].keySet().iterator();
		 it.hasNext(); ){
		SIROperator obj = (SIROperator)it.next();
		int val = ((int[])executionCounts[i].get(obj))[0];
		//System.err.println("execution count for " + obj + ": " + val);
		/** This bug doesn't show up in the new version of
		 * FM Radio - but leaving the comment here in case
		 * we need to special case any other scheduler bugsx.
		 
		 if (val==25) { 
		 System.err.println("Warning: catching scheduler bug with special-value "
		 + "overwrite in RawBackend");
		 val=26;
		 }
	       	if ((i == 0) &&
		    (obj.getName().startsWith("Fused__StepSource") ||
		     obj.getName().startsWith("Fused_FilterBank")))
		    val++;
	       */
		if (graphFlattener.getFlatNode(obj) != null)
		    result[i].put(graphFlattener.getFlatNode(obj), 
				  new Integer(val));
	    }
	}
	
	//Schedule the new Identities and Splitters introduced by GraphFlattener
	for(int i=0;i<GraphFlattener.needsToBeSched.size();i++) {
	    FlatNode node=(FlatNode)GraphFlattener.needsToBeSched.get(i);
	    int initCount=-1;
	    if(node.incoming.length>0) {
		if(initExecutionCounts.get(node.incoming[0])!=null)
		    initCount=((Integer)initExecutionCounts.get(node.incoming[0])).intValue();
		if((initCount==-1)&&(executionCounts[0].get(node.incoming[0].contents)!=null))
		    initCount=((int[])executionCounts[0].get(node.incoming[0].contents))[0];
	    }
	    int steadyCount=-1;
	    if(node.incoming.length>0) {
		if(steadyExecutionCounts.get(node.incoming[0])!=null)
		    steadyCount=((Integer)steadyExecutionCounts.get(node.incoming[0])).intValue();
		if((steadyCount==-1)&&(executionCounts[1].get(node.incoming[0].contents)!=null))
		    steadyCount=((int[])executionCounts[1].get(node.incoming[0].contents))[0];
	    }
	    if(node.contents instanceof SIRIdentity) {
		if(initCount>=0)
		    initExecutionCounts.put(node,new Integer(initCount));
		if(steadyCount>=0)
		    steadyExecutionCounts.put(node,new Integer(steadyCount));
	    } else if(node.contents instanceof SIRSplitter) {
		//System.out.println("Splitter:"+node);
		int[] weights=node.weights;
		FlatNode[] edges=node.edges;
		int sum=0;
		for(int j=0;j<weights.length;j++)
		    sum+=weights[j];
		for(int j=0;j<edges.length;j++) {
		    if(initCount>=0)
			initExecutionCounts.put(edges[j],new Integer((initCount*weights[j])/sum));
		    if(steadyCount>=0)
			steadyExecutionCounts.put(edges[j],new Integer((steadyCount*weights[j])/sum));
		}
		if(initCount>=0)
		    result[0].put(node,new Integer(initCount));
		if(steadyCount>=0)
		    result[1].put(node,new Integer(steadyCount));
	    } else if(node.contents instanceof SIRJoiner) {
		FlatNode oldNode=graphFlattener.getFlatNode(node.contents);
		if(executionCounts[0].get(node.oldContents)!=null)
		    result[0].put(node,new Integer(((int[])executionCounts[0].get(node.oldContents))[0]));
		if(executionCounts[1].get(node.oldContents)!=null)
		    result[1].put(node,new Integer(((int[])executionCounts[1].get(node.oldContents))[0]));
	    }
	}
	
	/*

	//now, in the above calculation, an execution of a joiner node is 
	//considered one cycle of all of its inputs.  For the remainder of the
	//raw backend, I would like the execution of a joiner to be defined as
	//the joiner passing one data item down stream
	for (int i=0; i < 2; i++) {
	    Iterator it = result[i].keySet().iterator();
	    while(it.hasNext()){
		FlatNode node = (FlatNode)it.next();
		if (node.contents instanceof SIRJoiner) {
		    int oldVal = ((Integer)result[i].get(node)).intValue();
		    int cycles=oldVal*((SIRJoiner)node.contents).oldSumWeights;
		    if((node.schedMult!=0)&&(node.schedDivider!=0))
			cycles=(cycles*node.schedMult)/node.schedDivider;
		    result[i].put(node, new Integer(cycles));
		}
		if (node.contents instanceof SIRSplitter) {
		    int sum = 0;
		    for (int j = 0; j < node.ways; j++)
			sum += node.weights[j];
		    int oldVal = ((Integer)result[i].get(node)).intValue();
		    result[i].put(node, new Integer(sum*oldVal));
		    //System.out.println("SchedSplit:"+node+" "+i+" "+sum+" "+oldVal);
		}
	    }
	}
	*/

	
	//The following code fixes an implementation quirk of two-stage-filters
	//in the *FIRST* version of the scheduler.  It is no longer needed,
	//but I am keeping it around just in case we every need to go back to the old
	//scheduler.
	
	//increment the execution count for all two-stage filters that have 
	//initpop == initpush == 0, do this for the init schedule only
	//we must do this for all the two-stage filters, 
	//so iterate over the keyset from the steady state 
	/*	Iterator it = result[1].keySet().iterator();
	while(it.hasNext()){
	    FlatNode node = (FlatNode)it.next();
	    if (node.contents instanceof SIRTwoStageFilter) {
		SIRTwoStageFilter two = (SIRTwoStageFilter) node.contents;
		if (two.getInitPush() == 0 &&
		    two.getInitPop() == 0) {
		    Integer old = (Integer)result[0].get(node);
		    //if this 2-stage was not in the init sched
		    //set the oldval to 0
		    int oldVal = 0;
		    if (old != null)
			oldVal = old.intValue();
		    result[0].put(node, new Integer(1 + oldVal));   
		}
	    }
	    }*/
    }


}

package at.dms.kjc.raw;

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

public class RawBackend {
    // number of rows and columns that we're compiling for
    public static int rawRows = -1;
    public static int rawColumns = -1;

    //given a flatnode map to the execution count
    public static HashMap initExecutionCounts;
    public static HashMap steadyExecutionCounts;
    //the simulator to be run
    public static Simulator simulator;
    // get the execution counts from the scheduler
    public static HashMap[] executionCounts;
    
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

	structures = structs;
	
	StructureIncludeFile.doit(structures);

	// set number of columns/rows
	RawBackend.rawRows = KjcOptions.raw;
	if(KjcOptions.rawcol>-1)
	    RawBackend.rawColumns = KjcOptions.rawcol;
	else
	    RawBackend.rawColumns = KjcOptions.raw;

	//use the work based simulator to layout the communication instructions
	if (KjcOptions.wbs)
	    simulator = new WorkBasedSimulator();
	else 
	    simulator = new FineGrainSimulator();

	//this must be run now, FlatIRToC relies on it!!!
	RenameAll.renameAllFilters(str);
	
	// move field initializations into init function
	System.out.print("Moving initializers into init functions... ");
	FieldInitMover.moveStreamInitialAssignments(str);
	System.out.println("done.");
	
	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println("Done Constant Prop and Unroll...");

        // add initPath functions
        EnqueueToInitPath.doInitPath(str);

	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);

	//SIRPrinter printer1 = new SIRPrinter();
	//str.accept(printer1);
	//printer1.close();

	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(str);

	// loop to decrease unroll factor until everything fits in IMEM
	SIRStream strOrig = null;
	// only need to make copy if there is some unrolling, since
	// otherwise we won't roll back
	boolean scaleUnrollFactor = KjcOptions.unroll>1 && !KjcOptions.forceunroll;
	if (scaleUnrollFactor) {
	    strOrig = (SIRStream)ObjectDeepCloner.deepCopy(str);
	}
	boolean fitsInIMEM;
	do {

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
	    
	    Lifter.liftAggressiveSync(str);
	    StreamItDot.printGraph(str, "before-partition.dot");
	    
	    // gather application-characterization statistics
	    if (KjcOptions.stats) {
		StatisticsGathering.doit(str);
	    }

	    str = Flattener.doLinearAnalysis(str);
	    str = Flattener.doStateSpaceAnalysis(str);

	    if (KjcOptions.fusion) {
		System.out.println("Running FuseAll...");
		str = FuseAll.fuse(str);
		Lifter.lift(str);
		System.out.println("Done FuseAll...");
	    }

	    if (KjcOptions.fission>1) {
		System.out.println("Running Vertical Fission...");
		FissionReplacer.doit(str, KjcOptions.fission);
		Lifter.lift(str);
		System.out.println("Done Vertical Fission...");
	    }

	    // turn on partitioning if there aren't enough tiles for all
	    // the filters
	    int count = new GraphFlattener(str).getNumTiles();
	    int numTiles = RawBackend.rawRows * RawBackend.rawColumns;
	    boolean partitioning = KjcOptions.partition_dp || KjcOptions.partition_greedy || KjcOptions.partition_greedier || KjcOptions.partition_ilp;
	    if (count>numTiles && !partitioning) {
		System.out.println("Need " + count + " tiles, so turning on partitioning...");
		KjcOptions.partition_dp = true;
		partitioning = true;
	    }

	    if (partitioning) {
		System.err.println("Running Partitioning...");
		str = Partitioner.doit(str, count, numTiles, true);
		System.err.println("Done Partitioning...");
	    }

	    if (KjcOptions.sjtopipe) {
		SJToPipe.doit(str);
	    }

	    StreamItDot.printGraph(str, "after-partition.dot");

	    //VarDecl Raise to move array assignments up
	    new VarDeclRaiser().raiseVars(str);

	
	    //VarDecl Raise to move peek index up so
	    //constant prop propagates the peek buffer index
	    new VarDeclRaiser().raiseVars(str);

	    // see if we are going to overflow IMEM
	    if (scaleUnrollFactor) {
		System.out.println("Trying unroll factor " + KjcOptions.unroll);
		fitsInIMEM = IMEMEstimation.testMe(str);
		if (fitsInIMEM) {
		    // if we fit, clear backup copy of stream graph
		    strOrig = null;
		    System.gc();
		} else if (KjcOptions.unroll<=1) {
		    // if we have reached bottom of unrolling, fail
		    Utils.fail("A filter overflows IMEM even though there is no unrolling.");
		} else {
		    // otherwise, cut unrolling in half and recurse
		    System.out.println("Cutting unroll factor from " + KjcOptions.unroll + " to " + (KjcOptions.unroll/2) + " to try to fit in IMEM...");
		    KjcOptions.unroll = KjcOptions.unroll / 2;
		    str = (SIRStream)ObjectDeepCloner.deepCopy(strOrig);
		}
	    } else {
		// it might not fit in IMEM, but we can't decrease the
		// unrolling any, so just go ahead
		fitsInIMEM = true;
	    }
	    
	} while (!fitsInIMEM);
	
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
	PartitionDot.printScheduleGraph(str, "schedule.dot", executionCounts);
	GraphFlattener graphFlattener = new GraphFlattener(str);
	graphFlattener.dumpGraph("flatgraph.dot");
	System.out.println("Flattener End.");

	//create the execution counts for other passes
	createExecutionCounts(str, graphFlattener);

	//see if we can remove any joiners
	//JoinerRemoval.run(graphFlattener.top);

	// layout the components (assign filters to tiles)	
	Layout.simAnnealAssign(graphFlattener.top);

	//Layout.handAssign(graphFlattener.top);
	
	//Layout.handAssign(graphFlattener.top);
	System.out.println("Assign End.");

	//if rate matching is requested, check if we can do it
	//if we can, then keep KjcOptions.rateMatch as true, 
	//otherwise set it to false
	if (KjcOptions.ratematch) {
	    if (RateMatch.doit(graphFlattener.top))
		System.out.println("Rate Matching Test Successful.");
	    else {
		KjcOptions.ratematch = false;
		System.out.println("Cannot perform Rate Matching.");
	    }
	}
		
	if (KjcOptions.magic_net) {
	    MagicNetworkSchedule.generateSchedules(graphFlattener.top);
	}
	else {
	    System.out.println("Switch Code Begin...");
	    SwitchCode.generate(graphFlattener.top);
	    System.out.println("Switch Code End.");
	}
	
	//Generate number gathering simulator code
	if (KjcOptions.numbers > 0) {
	    // do this on demand from NumberGathering
	    //SinkUnroller.doit(graphFlattener.top);
	    if (!NumberGathering.doit(graphFlattener.top)) {
		System.err.println("Could not generate number gathering code.  Exiting...");
		System.exit(1);
	    }
	}
	
	//remove print statements in the original app
	//if we are running with decoupled
	if (KjcOptions.decoupled)
	    RemovePrintStatements.doIt(graphFlattener.top);
	
	//Generate the tile code
	RawExecutionCode.doit(graphFlattener.top);


	if (KjcOptions.removeglobals) {
	    RemoveGlobals.doit(graphFlattener.top);
	}
	
	//VarDecl Raise to move array assignments down?
	new VarDeclRaiser().raiseVars(str);
	
	System.out.println("Tile Code begin...");
	TileCode.generateCode(graphFlattener.top);
	System.out.println("Tile Code End.");



	//generate the makefiles
	System.out.println("Creating Makefile.");
	MakefileGenerator.createMakefile();
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
    
    //debug function
    //run me after layout please
    public static void printCounts(HashMap counts) {
	Iterator it = counts.keySet().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode)it.next();
	    //	if (Layout.joiners.contains(node)) 
	    System.out.println(node.contents.getName() + " " +
			       ((Integer)counts.get(node)).intValue());
	}
    }

    

    //simple helper function to find the topmost pipeline
    private static SIRStream getTopMostParent (FlatNode node) 
    {
	SIRContainer[] parents = node.contents.getParents();
	return parents[parents.length -1];
    }

}


package at.dms.kjc.spacetime;

import at.dms.kjc.sir.*;
import at.dms.kjc.*;
import at.dms.kjc.flatgraph2.*;
import java.util.LinkedList;
import java.util.ListIterator;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.stats.StatisticsGathering;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.partition.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import at.dms.util.SIRPrinter;

/**
 * The entry to the space time backend for raw.
 **/
public class SpaceTimeBackend 
{
    public static boolean FILTER_DEBUG_MODE = false;
    
    public static SIRStructure[] structures;
    final private static boolean TEST_SOFT_PIPE = false; //Test Software Pipelining
    final private static boolean TEST_BEAMFORMER = false; //Test SplitJoins
    final private static boolean REAL=true; //The Real Stuff
    
    
    public static void run(SIRStream str,
			   JInterfaceDeclaration[] 
			   interfaces,
			   SIRInterfaceTable[]
			   interfaceTables,
			   SIRStructure[]
			   structs) {
	structures = structs;
	
	//first of all enable altcodegen by default
	KjcOptions.altcodegen = true;

	int rawRows = -1;
	int rawColumns = -1;

	//set number of columns/rows
	rawRows = KjcOptions.raw;
	if(KjcOptions.rawcol>-1)
	    rawColumns = KjcOptions.rawcol;
	else
	    rawColumns = KjcOptions.raw;

	//create the RawChip
	RawChip rawChip = new RawChip(rawColumns, rawRows);

	// move field initializations into init function
	FieldInitMover.moveStreamInitialAssignments(str);
		
	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println("Done Constant Prop and Unroll...");

	// add initPath functions
        EnqueueToInitPath.doInitPath(str);

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
       	StreamItDot.printGraph(str, "before-partition.dot");

	//	str = Partitioner.doit(str, 32);

	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(str);
	
	//VarDecl Raise to move peek index up so
	//constant prop propagates the peek buffer index
	new VarDeclRaiser().raiseVars(str);

	//this must be run now, other pass rely on it...
	RenameAll.renameOverAllFilters(str);
	
	//SIRPrinter printer1 = new SIRPrinter();
	//IterFactory.createFactory().createIter(str).accept(printer1);
	//printer1.close();
	
	//Linear Analysis
	LinearAnalyzer lfa=null;
	if(KjcOptions.linearanalysis||KjcOptions.linearpartition) {
	    System.out.println("Running linear analysis... ");
	    lfa=LinearAnalyzer.findLinearFilters(str,KjcOptions.debug,false);
	    System.out.println("Done with linear analysis.");
	    LinearDot.printGraph(str,"linear.dot",lfa);
	    LinearDotSimple.printGraph(str,"linear-simple.dot",lfa,null);
	}
	
	//get the execution counts from the scheduler
	HashMap[] executionCounts=SIRScheduler.getExecutionCounts(str);
	//flatten the graph by running (super?) synch removal
	FlattenGraph.flattenGraph(str,lfa,executionCounts);
	UnflatFilter[] topNodes=FlattenGraph.getTopLevelNodes();
	System.out.println("Top Nodes:");
	for(int i=0;i<topNodes.length;i++)
	    System.out.println(topNodes[i]);

	Trace[] traces=null;
	Trace[] traceGraph=null; //used if REAL

	if (true) {
	    //get the work estimation
	    WorkEstimate work = WorkEstimate.getWorkEstimate(str);
	    SimplePartitioner partitioner = new SimplePartitioner(topNodes,executionCounts,lfa, work);
	    traceGraph = partitioner.partition();
	    System.out.println("UnPrunnedTraces: "+traceGraph.length);
	    partitioner.dumpGraph("traces.dot");
	}	
	else {
	    traceGraph = TraceExtractor.extractTraces(topNodes,executionCounts,lfa);
	    System.out.println("UnPrunnedTraces: "+traceGraph.length);
	    TraceExtractor.dumpGraph(traceGraph,"traces.dot");
	}
	
	System.exit(0);
	
	/*System.gc();
	  System.out.println("MEM: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));*/
	StreaMITMain.clearParams();
	FlattenGraph.clear();
	AutoCloner.clear();
	SIRContainer.destroy();
	UnflatEdge.clear();
	str=null;
	interfaces=null;
	interfaceTables=null;
	structs=null;
	structures=null;
	lfa=null;
	executionCounts=null;
	topNodes=null;
	System.gc();
	/*System.out.println("MEM: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
	  System.gc();
	  System.out.println("MEM: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
	  System.gc();
	  System.out.println("MEM: "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));*/
	//----------------------- This Is The Line -----------------------
	//No Structure, No SIRStreams, Old Stuff Restricted Past This Point
	//Violators Will Be Garbage Collected
	
	Trace[] traceForrest = new Trace[1];
	//traceForrest[0] = traces[0];
	/*if(false&&REAL) {
	  //System.out.println("TracesGraph: "+traceGraph.length);
	  //for(int i=0;i<traceGraph.length;i++)
	  //System.out.println(traceGraph[i]);
	  traces=traceGraph;
	  int index=0;
	  traceForrest[0]=traceGraph[0];
	  Trace realTrace=traceGraph[0];
	  while(((FilterTraceNode)realTrace.getHead().getNext()).isPredefined())
	  realTrace=traceGraph[++index];
	  TraceNode node=realTrace.getHead();
	  FilterTraceNode currentNode=null;
	  if(node instanceof InputTraceNode)
	  currentNode=(FilterTraceNode)node.getNext();
	  else
	  currentNode=(FilterTraceNode)node;
	  currentNode.setXY(0,0);
	  System.out.println("SETTING: "+currentNode+" (0,0)");
	  int curX=1;
	  int curY=0;
	  int forward=1;
	  int downward=1;
	  //ArrayList traceList=new ArrayList();
	  //traceList.add(new Trace(currentNode));
	  TraceNode nextNode=currentNode.getNext();
	  while(nextNode!=null&&nextNode instanceof FilterTraceNode) {
	  currentNode=(FilterTraceNode)nextNode;
	  System.out.println("SETTING: "+nextNode+" ("+curX+","+curY+")");
	  currentNode.setXY(curX,curY);
	  if(curX>=rawColumns-1&&forward>0) {
	  forward=-1;
	  curY+=downward;
	  } else if(curX<=0&&forward<0) {
	  forward=1;
	  if(curY==0)
	  downward=1;
	  if(curY==rawRows-1)
	  downward=-1;
	  if((curY==0)||(curY==rawRows-1)) {
	  } else
	  curY+=downward;
	  } else
	  curX+=forward;
	  nextNode=currentNode.getNext();
	  }
	  //traces=new Trace[traceList.size()];
	  //traceList.toArray(traces);
	  for(int i=1;i<traces.length;i++) {
	  traces[i-1].setEdges(new Trace[]{traces[i]});
	  traces[i].setDepends(new Trace[]{traces[i-1]});
	  }
	  //System.out.println(traceList);
	  } else */

	Trace[] io=null;

	if(true&&REAL) {
	    int len=traceGraph.length;
	    int newLen=len;
	    for(int i=0;i<len;i++)
		if(((FilterTraceNode)traceGraph[i].getHead().getNext()).isPredefined())
		    newLen--;
	    traces=new Trace[newLen];
	    io=new Trace[len-newLen];
	    int idx=0;
	    int idx2=0;
	    for(int i=0;i<len;i++) {
		Trace trace=traceGraph[i];
		if(!((FilterTraceNode)trace.getHead().getNext()).isPredefined())
		    traces[idx++]=trace;
		else
		    io[idx2++]=trace;
	    }
	    System.out.println("Traces: "+traces.length);
	    for(int i=0;i<traces.length;i++)
		System.out.println(traces[i]);
	    SpaceTimeSchedule sched=TestLayout.layout(traces,rawRows,rawColumns);
	    traceForrest=Schedule2Dependencies.findDependencies(sched,traces,rawRows,rawColumns);
	    SoftwarePipeline.pipeline(sched,traces,io);
	    for(int i=0;i<traces.length;i++)
		traces[i].doneDependencies();
	    System.err.println("TopNodes in Forest: "+traceForrest.length);
	    traceForrest=PruneTopTraces.prune(traceForrest);
	    System.err.println("TopNodes in Forest: "+traceForrest.length);
	}
	
	//traceList=null;
	//content=null;
	//executionCounts=null;
	if(true&&REAL) {
	    //mgordon's stuff
	    assert !KjcOptions.magicdram : 
		"Magic DRAM support is not working";
	    
	    System.out.println("Building Trace Traversal");
	    //LinkedList initList = TraceTraversal.getTraversal(traces);
	    List initList = Arrays.asList(traces);
	    List steadyList = TraceTraversal.getTraversal(traceForrest);

	    //

	    TraceDotGraph.dumpGraph(steadyList, io, "preDRAMsteady.dot", false);
	    //assign the buffers not assigned by Jasp to drams
	    BufferDRAMAssignment.run(steadyList, rawChip, io);
	    //communicate the addresses for the off-chip buffers
	    if (!KjcOptions.magicdram) {
		//so right now, this pass does not communicate addresses
		//but it generates the declarations of the buffers
		//on the corresponding tile.
		CommunicateAddrs.doit(rawChip);
	    }
	    TraceDotGraph.dumpGraph(initList, io, "inittraces.dot", true);
	    TraceDotGraph.dumpGraph(steadyList, io, "steadyforrest.dot", true);
	    //create the raw execution code and switch code for the initialization phase
	    System.out.println("Creating Initialization Stage");
	    Rawify.run(initList.iterator(), rawChip, true); 
	    //create the raw execution code and switch for the steady-state
	    System.out.println("Creating Steady-State Stage");
	    Rawify.run(steadyList.iterator(), rawChip, false);
	    //dump the layout
	    LayoutDot.dumpLayout(rawChip, "layout.dot");
	    //generate the switch code assembly files...
	    GenerateSwitchCode.run(rawChip);
	    //generate the compute code from the SIR
	    GenerateComputeCode.run(rawChip);
	    //generate the magic dram code if enabled
	    if (KjcOptions.magicdram) {
		MagicDram.GenerateCode(rawChip);
	    }
	    Makefile.generate(rawChip);
	    //generate the bc file depending on if we have number gathering enabled
	    if (KjcOptions.numbers > 0)
		BCFile.generate(rawChip, NumberGathering.doit(rawChip, io));
	    else 
		BCFile.generate(rawChip, null);
	}
    }

    public static void println(String s) 
    {
	if (KjcOptions.debug) 
	    System.out.println(s);
    }
}

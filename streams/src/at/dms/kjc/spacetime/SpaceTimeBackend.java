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
	
	//jasperln's Stuff
	HashMap[] executionCounts=SIRScheduler.getExecutionCounts(str);
	FlattenGraph.flattenGraph(str,lfa,executionCounts);
	UnflatFilter[] topNodes=FlattenGraph.getTopLevelNodes();
	System.out.println("Top Nodes:");
	for(int i=0;i<topNodes.length;i++)
	    System.out.println(topNodes[i]);
	Trace[] traces=null;
	Trace[] traceGraph=null; //used if REAL
	if(!REAL&&TEST_BEAMFORMER) { //Test for simple one join-split (beamformer)
	    traceGraph=TraceExtractor.extractTraces(topNodes,executionCounts,lfa);
	    System.out.println("Traces: "+traceGraph.length);
	    TraceExtractor.dumpGraph(traceGraph,"traces.dot");
	    Trace first=null;
	    Trace second=null;
	    Trace firstLast=null;
	    Trace secondLast=null;
	    for(int i=0;i<traceGraph.length;i++) {
		Trace trace=traceGraph[i];
		//System.err.println(trace+" "+trace.size()+" "+trace.getHead()+" "+trace.getTail());
		if(trace.getTail() instanceof OutputTraceNode) {
		    if(first==null) {
			first=trace;
			firstLast=trace;
		    } else {
			firstLast.connect(trace);
			firstLast=trace;
		    }
		    int x=0;
		    TraceNode node=trace.getTail().getPrevious();
		    ((FilterTraceNode)node).setXY(x++,0);
		    if(TEST_SOFT_PIPE)
			((FilterTraceNode)node).getFilter().setPrimePump(1);
		    node=node.getPrevious();
		    while(node!=null&&node instanceof FilterTraceNode) {
			((FilterTraceNode)node).setXY(x++,0);
			if(TEST_SOFT_PIPE)
			    ((FilterTraceNode)node).getFilter().setPrimePump(1);
			node=node.getPrevious();
		    }
		} else if(trace.getHead() instanceof InputTraceNode) {
		    if(second==null) {
			second=trace;
			secondLast=trace;
		    } else {
			secondLast.connect(trace);
			secondLast=trace;
		    }
		    int x=0;
		    TraceNode node=trace.getHead().getNext();
		    ((FilterTraceNode)node).setXY(x++,0);
		    node=node.getNext();
		    while(node!=null&&node instanceof FilterTraceNode) {
			((FilterTraceNode)node).setXY(x++,0);
			node=node.getNext();
		    }
		}
	    }
	    if(TEST_SOFT_PIPE)
		traces=new Trace[] {second};
	    else
		traces=new Trace[] {first};
	    //while(first.getEdges()[0]!=null)
	    //first=first.getEdges()[0];
	    if(TEST_SOFT_PIPE)
		secondLast.connect(first);
	    else
		firstLast.connect(second);
	    System.out.println("Traces Length: "+traces.length);
	    System.out.println("Traces:");
	    Trace trace=traces[0];
	    while(trace!=null) {
		TraceNode head=trace.getHead();
		System.err.println("START TRACE");
		while (head != null) {
		    if(head instanceof FilterTraceNode)
			System.out.println(((FilterTraceNode)head).getFilter()+" "+((FilterTraceNode)head).getX()+" "+((FilterTraceNode)head).getY());
		    else if (head.isInputTrace()) {
			//System.out.println(head);
			System.out.println("InputTraceNode "+((FilterTraceNode)head.getNext()).getX()+" "+((FilterTraceNode)head.getNext()).getY());
		    }
		    else if(head.isOutputTrace()) {
			//System.out.println(head);
			System.out.println("OutputTraceNode "+((FilterTraceNode)head.getPrevious()).getX()+" "+((FilterTraceNode)head.getPrevious()).getY());
		    } else
			System.out.println("Unknown TraceNode!: "+head);
		    
		    head = head.getNext();
		    
		}
		Trace[] edge=trace.getEdges();
		if(edge.length>0)
		    trace=trace.getEdges()[0];
		else
		    trace=null;
	    }
	} else if(!REAL) { //Test Code with traces (Just for pipelines on raw greater than 4x4)
	    ArrayList traceList=new ArrayList();
	    //HashMap[] executionCounts=SIRScheduler.getExecutionCounts(str);
	    UnflatFilter currentFilter=topNodes[0];
	    FilterContent content=new FilterContent(currentFilter);
	    if(TEST_SOFT_PIPE)
		content.setPrimePump(1);
	    TraceNode currentNode=new FilterTraceNode(content,0,0);
	    traceList.add(new Trace(currentNode));
	    int curX=1;
	    int curY=0;
	    int forward=1;
	    int downward=1;
	    while(currentFilter!=null&&currentFilter.out!=null&&currentFilter.outWeights.length>0) {
		currentFilter=currentFilter.out[0][0].dest;
		if(currentFilter!=null) {
		    content=new FilterContent(currentFilter);
		    if(TEST_SOFT_PIPE)
			content.setPrimePump(1);
		    TraceNode newNode=new FilterTraceNode(content,curX,curY);
		    currentNode.setNext(newNode);
		    newNode.setPrevious(currentNode);
		    currentNode=newNode;
		    if(curX>=rawColumns-1&&forward>0) {
			//System.err.println(currentFilter);
			//System.err.println(currentFilter.outWeights);
			//System.err.println(currentFilter.out);
			if(currentFilter.outWeights.length>0&&currentFilter.out!=null) {
			    forward=-1;
			    curY+=downward;
			}
		    } else if(curX<=0&&forward<0) {
			if(currentFilter.outWeights.length>0&&currentFilter.out!=null) {
			    forward=1;
			    if(curY==0)
				downward=1;
			    if(curY==rawRows-1)
				downward=-1;
			    if((curY==0)||(curY==rawRows-1)) {
				OutputTraceNode out=new OutputTraceNode(new int[]{1});
				InputTraceNode in=new InputTraceNode(new int[]{1});
				out.setDests(new InputTraceNode[][]{new InputTraceNode[]{in}});
				in.setSources(new OutputTraceNode[]{out});
				currentNode.setNext(out);
				out.setPrevious(currentNode);
				currentNode=in;
				traceList.add(new Trace(currentNode));
			    } else
				curY+=downward;
			}
		    } else
			curX+=forward;
		}
	    }
	    if(TEST_SOFT_PIPE) {
		Trace end=(Trace)traceList.remove(traceList.size()-1);
		TraceNode cur=end.getHead();
		while(cur!=null) {
		    if(cur instanceof FilterTraceNode)
			((FilterTraceNode)cur).getFilter().setPrimePump(0);
		    cur=cur.getNext();
		}
		traceList.add(0,end);
	    }
	    traces=new Trace[traceList.size()];
	    traceList.toArray(traces);
	    for(int i=1;i<traces.length;i++) {
		traces[i-1].setEdges(new Trace[]{traces[i]});
		traces[i].setDepends(new Trace[]{traces[i-1]});
	    }
	
	    System.out.println(traceList);
	    for(int i=0;i<traceList.size();i++) {
		TraceNode head=((Trace)traceList.get(i)).getHead();
		while (head != null) {
		    if(head instanceof FilterTraceNode)
			System.out.println(((FilterTraceNode)head).getFilter()+" "+((FilterTraceNode)head).getX()+" "+((FilterTraceNode)head).getY());
		    else if (head.isInputTrace()) {
			System.out.println(head);
			System.out.println("Input! "+((FilterTraceNode)head.getNext()).getX()+" "+((FilterTraceNode)head.getNext()).getY());
		    }
		    else {
			System.out.println(head);
			System.out.println("Output!");
		    }
		    
		    head = head.getNext();
		    
		}
		
		//System.out.println(((Trace)traceList.get(i)).getHead());
	    }
	} else if(REAL) {
	    traceGraph=TraceExtractor.extractTraces(topNodes,executionCounts,lfa);
	    System.out.println("Traces: "+traceGraph.length);
	    /*TraceExtractor.dumpGraph(traceGraph,"traces.dot");
	    //TEMP
	    ArrayList traceList=new ArrayList();
	    UnflatFilter currentFilter=topNodes[0];
	    FilterContent content=new FilterContent(currentFilter);
	    TraceNode currentNode=new FilterTraceNode(content,0,0);
	    traceList.add(new Trace(currentNode));
	    int curX=1;
	    int curY=0;
	    int forward=1;
	    int downward=1;
	    while(currentFilter!=null&&currentFilter.out!=null&&currentFilter.outWeights.length>0) {
		currentFilter=currentFilter.out[0][0].dest;
		if(currentFilter!=null) {
		    content=new FilterContent(currentFilter);
		    TraceNode newNode=new FilterTraceNode(content,curX,curY);
		    currentNode.setNext(newNode);
		    newNode.setPrevious(currentNode);
		    currentNode=newNode;
		    if(curX>=rawColumns-1&&forward>0) {
			if(currentFilter.outWeights.length>0&&currentFilter.out!=null) {
			    forward=-1;
			    curY+=downward;
			}
		    } else if(curX<=0&&forward<0) {
			if(currentFilter.outWeights.length>0&&currentFilter.out!=null) {
			    forward=1;
			    if(curY==0)
				downward=1;
			    if(curY==rawRows-1)
				downward=-1;
			    if((curY==0)||(curY==rawRows-1)) {
				OutputTraceNode out=new OutputTraceNode(new int[]{1});
				InputTraceNode in=new InputTraceNode(new int[]{1});
				out.setDests(new InputTraceNode[][]{new InputTraceNode[]{in}});
				in.setSources(new OutputTraceNode[]{out});
				currentNode.setNext(out);
				out.setPrevious(currentNode);
				currentNode=in;
				traceList.add(new Trace(currentNode));
			    } else
				curY+=downward;
			}
		    } else
			curX+=forward;
		}
	    }
	    traces=new Trace[traceList.size()];
	    traceList.toArray(traces);
	    for(int i=1;i<traces.length;i++) {
		traces[i-1].setEdges(new Trace[]{traces[i]});
		traces[i].setDepends(new Trace[]{traces[i-1]});
	    }
	    System.out.println(traceList);
	    for(int i=0;i<traceList.size();i++) {
		TraceNode head=((Trace)traceList.get(i)).getHead();
		while (head != null) {
		    if(head instanceof FilterTraceNode)
			System.out.println(((FilterTraceNode)head).getFilter()+" "+((FilterTraceNode)head).getX()+" "+((FilterTraceNode)head).getY());
		    else if (head.isInputTrace()) {
			System.out.println(head);
			System.out.println("Input! "+((FilterTraceNode)head.getNext()).getX()+" "+((FilterTraceNode)head.getNext()).getY());
		    }
		    else {
			System.out.println(head);
			System.out.println("Output!");
		    }
		    head = head.getNext();
		}
		}*/
	}
	

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
	if(REAL) {
	    TraceExtractor.dumpGraph(traceGraph,"traces.dot");
	    System.out.println("TracesGraph: "+traceGraph.length);
	    for(int i=0;i<traceGraph.length;i++)
		System.out.println(traceGraph[i]);
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
	}

	//traceList=null;
	//content=null;
	//executionCounts=null;
	if(true||REAL) {
	    //mgordon's stuff
	    System.out.println("Building Trace Traversal");
	    ListIterator initTrav = TraceTraversal.getTraversal(traceForrest).listIterator();    
	    ListIterator steadyTrav = TraceTraversal.getTraversal(traceForrest).listIterator();    

	    //assign the buffers not assigned by Jasp to drams
	    OIBufferAssignment.run(steadyTrav, rawChip);
	    //create the raw execution code and switch code for the initialization phase
	    System.out.println("Creating Initialization Stage");
	    Rawify.run(initTrav, rawChip, true); 
	    //create the raw execution code and switch for the steady-state
	    System.out.println("Creating Steady-State Stage");
	    Rawify.run(steadyTrav, rawChip, false);
	    //communicate the addresses for the off-chip buffers
	    if (!KjcOptions.magicdram) {
		//so right now, this pass does not communicate addresses
		//but it generates the declarations of the buffers
		//on the corresponding tile.
		CommunicateAddrs.doit(rawChip);
	    }
	    //generate the switch code assembly files...
	    GenerateSwitchCode.run(rawChip);
	    //generate the compute code from the SIR
	    GenerateComputeCode.run(rawChip);
	    //generate the magic dram code if enabled
	    if (KjcOptions.magicdram) {
		MagicDram.GenerateCode(rawChip);
	    }
	    Makefile.generate(rawChip);
	    BCFile.generate(rawChip);
	}
    }
}

    








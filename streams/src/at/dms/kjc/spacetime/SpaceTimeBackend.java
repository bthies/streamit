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
import java.util.*;
import at.dms.util.SIRPrinter;

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

	str = Partitioner.doit(str, 4);

	//VarDecl Raise to move array assignments up
	new VarDeclRaiser().raiseVars(str);
	
	//VarDecl Raise to move peek index up so
	//constant prop propagates the peek buffer index
	new VarDeclRaiser().raiseVars(str);

	//this must be run now, other pass rely on it...
	RenameAll.renameOverAllFilters(str);
	
	//SIRPrinter printer1 = new SIRPrinter();
	//IterFactory.createIter(str).accept(printer1);
	//printer1.close();
	
	//jasperln's Stuff
	FlattenGraph.flattenGraph(str);
	UnflatFilter[] topNodes=FlattenGraph.getTopLevelNodes();
	System.out.println("Top Nodes:");
	for(int i=0;i<topNodes.length;i++)
	    System.out.println(topNodes[i]);
	//System.out.println(FlattenGraph.getFilterCount());
	
	//this is here just to test things!!
	//Trace[] init = new Trace[2];
	//Trace[] steady = new Trace[2];
	
	//Test Code with traces (Just for pipelines on raw greater than 1x1)
	ArrayList initTraces=new ArrayList();
	ArrayList steadyTraces=new ArrayList();
	HashMap[] executionCounts=SIRScheduler.getExecutionCounts(str);
	UnflatFilter currentFilter=topNodes[0];
	TraceNode currentNode=new FilterTraceNode(new FilterContent(currentFilter.filter,executionCounts),0,0);
	initTraces.add(new Trace(currentNode));
	steadyTraces.add(new Trace(currentNode));
	int curX=1;
	int forward=1;
	while(currentFilter!=null&&currentFilter.outWeights.length>0) {
	    currentFilter=currentFilter.out[0][0].dest;
	    if(currentFilter!=null) {
		TraceNode newNode=new FilterTraceNode(new FilterContent(currentFilter.filter,executionCounts),curX,0);
		currentNode.setNext(newNode);
		newNode.setPrevious(currentNode);
		currentNode=newNode;
		if(curX>=rawColumns-1&&forward>0) {
		    if(currentFilter.outWeights.length>0&&currentFilter.out[0][0].dest!=null) {
			forward=-1;
			OutputTraceNode out=new OutputTraceNode(new int[]{1});
			InputTraceNode in=new InputTraceNode(new int[]{1});
			out.setDests(new InputTraceNode[][]{new InputTraceNode[]{in}});
			in.setSources(new OutputTraceNode[]{out});
			currentNode.setNext(out);
			out.setPrevious(currentNode);
			currentNode=in;
			initTraces.add(new Trace(currentNode));
			steadyTraces.add(new Trace(currentNode));
		    }
		} else if(curX<=0&&forward<0) {
		    if(currentFilter.outWeights.length>0&&currentFilter.out[0][0].dest!=null) {
			forward=1;
			OutputTraceNode out=new OutputTraceNode(new int[]{1});
			InputTraceNode in=new InputTraceNode(new int[]{1});
			out.setDests(new InputTraceNode[][]{new InputTraceNode[]{in}});
			in.setSources(new OutputTraceNode[]{out});
			currentNode.setNext(out);
			out.setPrevious(currentNode);
			currentNode=in;
			initTraces.add(new Trace(currentNode));
			steadyTraces.add(new Trace(currentNode));
		    }
		} else
		    curX+=forward;
	    }
	}
	
	Trace[] traces = new Trace[initTraces.size()];
	initTraces.toArray(traces);
	for(int i=1;i<traces.length;i++) {
	    traces[i-1].setEdges(new Trace[]{traces[i]});
	    traces[i].setDepends(new Trace[]{traces[i-1]});
	}
	

	/*System.out.println(initTraces);
	  for(int i=0;i<initTraces.size();i++) {
	  TraceNode head=((Trace)initTraces.get(i)).getHead();
	  if(head instanceof FilterTraceNode)
	  System.out.println(((FilterTraceNode)head).getFilter()+" "+((FilterTraceNode)head).getX()+" "+((FilterTraceNode)head).getY());
	  else
	  System.out.println("Input! "+((FilterTraceNode)head.getNext()).getX()+" "+((FilterTraceNode)head.getNext()).getY());
	  //System.out.println(((Trace)initTraces.get(i)).getHead());
	  }
	*/
	System.out.println(steadyTraces);
	for(int i=0;i<steadyTraces.size();i++) {
	    TraceNode head=((Trace)steadyTraces.get(i)).getHead();
	    while (head != null) {
		if(head instanceof FilterTraceNode)
		    System.out.println(((FilterTraceNode)head).getFilter()+" "+((FilterTraceNode)head).getX()+" "+((FilterTraceNode)head).getY());
		else {
		    System.out.println(head);
		    System.out.println("Input! "+((FilterTraceNode)head.getNext()).getX()+" "+((FilterTraceNode)head.getNext()).getY());
		}
		head = head.getNext();
		
	    }
	    
	    //System.out.println(((Trace)steadyTraces.get(i)).getHead());
	}
	    
	initTraces=null;
	steadyTraces=null;
	executionCounts=null;

	//mgordon's stuff
	System.out.println("Building Trace Traversal");
	ListIterator initTrav = TraceTraversal.getTraversal(traces).listIterator();    
	ListIterator steadyTrav = TraceTraversal.getTraversal(traces).listIterator();    


	//create the raw execution code and switch code for the initialization phase
	System.out.println("Creating Initialization Stage");
	Rawify.run(initTrav, rawChip, true); 
	//create the raw execution code and switch for the steady-state
	System.out.println("Creating Steady-State Stage");
	Rawify.run(steadyTrav, rawChip, false);
	//generate the switch code assembly files...
	GenerateSwitchCode.run(rawChip);
	//generate the compute code from the SIR
	GenerateComputeCode.run(rawChip);
	Makefile.generate(rawChip);
	BCFile.generate(rawChip);
    }
}

    



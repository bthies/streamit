package at.dms.kjc.raw;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.sir.lowering.fusion.*;
import at.dms.kjc.sir.lowering.fission.*;
import at.dms.kjc.lir.*;
import java.util.*;
import at.dms.util.Utils;

public class RawBackend {
    //given a flatnode map to the execution count
    public static HashMap initExecutionCounts;
    public static HashMap steadyExecutionCounts;

    public static void run(SIRStream str,
			JInterfaceDeclaration[] 
			interfaces,
			SIRInterfaceTable[]
			interfaceTables) {
	System.out.println("Entry to RAW Backend");

	// move field initializations into init function
	System.out.print("Moving initializers into init functions... ");
	FieldInitMover.moveStreamInitialAssignments(str);
	System.out.println("done.");
	
	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println("Done Constant Prop and Unroll...");

	// construct stream hierarchy from SIRInitStatements
	ConstructSIRTree.doit(str);

	//SIRPrinter printer1 = new SIRPrinter();
	//str.accept(printer1);
	//printer1.close();
	
	if (StreamItOptions.fusion) {
	    System.out.println("Running SJFusion...");
	    FuseAll.fuse(str);
	    System.out.println("Done SJFusion...");
	}

        // do constant propagation on fields
        if (StreamItOptions.constprop) {
	    System.out.println("Running Constant Propagation of Fields");
	    FieldProp.doPropagate(str);
	}
	
	AdjustGranularity.doit(str, 
			       StreamItOptions.rawRows * 
			       StreamItOptions.rawColumns);

	if (StreamItOptions.partition) {
	    Partitioner.doit(str, 
			     StreamItOptions.rawRows *
			     StreamItOptions.rawColumns);
	}

	//Destroys arrays into local variables if possible
	new ArrayDestroyer().destroyArrays(str);
	//Raise VarDecls to front of blocks
	new VarDeclRaiser().raiseVars(str);
       	System.out.println("Flattener Begin...");
	RawFlattener rawFlattener = new RawFlattener(str);
	rawFlattener.dumpGraph("flatgraph.dot");
	System.out.println("Flattener End.");
	//create the execution counts for other passes
	createExecutionCounts(str, rawFlattener);

	// layout the components (assign filters to tiles)
	
	Layout.simAnnealAssign(rawFlattener.top);
	
	//Layout.handAssign(rawFlattener.top);
	System.out.println("Assign End.");
	//Generate the switch code
	System.out.println("Switch Code Begin...");
	SwitchCode.generate(rawFlattener.top);
	//	SwitchCode.dumpCode();
	System.out.println("Switch Code End.");
	//Generate the tile code
	System.out.println("Tile Code begin...");
	TileCode.generateCode(rawFlattener.top);
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
					      RawFlattener rawFlattener) {
	// get the execution counts from the scheduler
	HashMap[] executionCounts = SIRScheduler.getExecutionCounts(str);

	// make fresh hashmaps for results
	HashMap[] result = { initExecutionCounts = new HashMap(), 
			     steadyExecutionCounts = new HashMap()} ;

	// then filter the results to wrap every filter in a flatnode,
	// and ignore splitters
	for (int i=0; i<2; i++) {
	    for (Iterator it = executionCounts[i].keySet().iterator();
		 it.hasNext(); ){
		SIROperator obj = (SIROperator)it.next();
		if (!(obj instanceof SIRSplitter)) {
		    int val = ((int[])executionCounts[i].get(obj))[0];
		    if (val==25) { 
			System.err.println("Warning: catching scheduler bug with special-value "
					   + "overwrite in RawBackend");
			val=26;
		    }
		    result[i].put(rawFlattener.getFlatNode(obj), 
				  new Integer(val));
		}
	    }
	}
    }

    //simple helper function to find the topmost pipeline
    private static SIRStream getTopMostParent (FlatNode node) 
    {
	SIRContainer[] parents = node.contents.getParents();
	return parents[parents.length -1];
    }
}

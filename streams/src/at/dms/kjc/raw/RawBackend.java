package at.dms.kjc.raw;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
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
	// DEBUGGING PRINTING
	System.out.println("Entry to RAW Backend");

	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	//FieldProp.doPropagate(str);
	//Renamer.renameAll(str);
	ConstantProp.propagateAndUnroll(str);
	
	System.out.println("Done Constant Prop and Unroll...");

	//SIRPrinter printer1 = new SIRPrinter();
	//str.accept(printer1);
	//printer1.close();
	
	//SIRPrinter printer1 = new SIRPrinter();
	//str.accept(printer1);
	//printer1.close();
		
	//if (StreamItOptions.fusion) {
	//   System.out.println("Running Fusion");
	//    Fusion.fuse((SIRPipeline)str, 
	//		(SIRFilter)((SIRPipeline)str).get(0), 
	//		(SIRFilter)((SIRPipeline)str).get(1));
	//	}
       
	/* DON'T KNOW IF THIS SHOULD BE DONE!!
        
	// flatten split/joins with duplicate splitters and RR joiners
	*/
	
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
	
	// name the components
	System.out.println("Namer Begin...");
	Namer.assignNames(str);
	System.out.println("Namer End.");

	//SIRPrinter printer1 = new SIRPrinter();
	//str.accept(printer1);
	//printer1.close();

	System.out.println("Flattener Begin...");
	RawFlattener.flatten(str);
	RawFlattener.dumpGraph("flatgraph.dot");
	System.out.println("Flattener End.");
	//create the execution counts for other passes
	createExecutionCounts(RawFlattener.top);

	// layout the components (assign filters to tiles)
	Layout.simAnnealAssign(RawFlattener.top);
	//Layout.handAssign(RawFlattener.top);
	System.out.println("Assign End.");
	//Generate the switch code
	System.out.println("Switch Code Begin...");
	SwitchCode.generate(RawFlattener.top);
	//	SwitchCode.dumpCode();
	System.out.println("Switch Code End.");
	//Generate the tile code
	System.out.println("Tile Code begin...");
	TileCode.generateCode(RawFlattener.top);
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
   
    private static void createExecutionCounts(FlatNode top) 
    {

	SIRScheduler scheduler = new SIRScheduler();
	Schedule schedule = scheduler.computeSchedule(getTopMostParent(top));

	initExecutionCounts = new HashMap();
	steadyExecutionCounts = new HashMap();

	fillExecutionCounts(schedule.getInitSchedule(),
			    initExecutionCounts,
			    scheduler);
	fillExecutionCounts(schedule.getSteadySchedule(), 
			    steadyExecutionCounts,
			    scheduler);
    }

    
    //simple helper function to find the topmost pipeline
    private static SIRStream getTopMostParent(FlatNode node) 
    {
	SIRContainer[] parents = node.contents.getParents();
	return parents[parents.length -1];
    }
    
    //creates execution counts of filters in graph (flatnode maps count)
    private static void fillExecutionCounts(Object schedObject, 
					    HashMap counts,
					    SIRScheduler scheduler) 
    {
	if (schedObject instanceof List) {
	    // first see if we have a two-stage filter
	    SIRTwoStageFilter twoStage = 
		scheduler.getTwoStageFilter((List)schedObject);
	    // if we found a two-stage filter, recurse on it instead
	    // of the list elements.  (This is a HACK that should be
	    // removed throughout the SIRScheduler, perhaps by
	    // supporting two-stage filters in Karczma's scheduler.) --bft
	    if (twoStage!=null) {
		fillExecutionCounts(twoStage, counts, scheduler);
	    } else {
		// otherwise, visit all of the elements of the list
		for (ListIterator it = ((List)schedObject).listIterator();
		     it.hasNext(); ) {
		    fillExecutionCounts(it.next(), counts, scheduler);
		}
	    }
	} else if (schedObject instanceof SchedRepSchedule) {
    	    // get the schedRep
	    SchedRepSchedule rep = (SchedRepSchedule)schedObject;
	    ///===========================================BIG INT?????
	    for(int i = 0; i < rep.getTotalExecutions().intValue(); i++)
		fillExecutionCounts(rep.getOriginalSchedule(), 
				    counts,
				    scheduler);
	} else {
	    //do not count splitter
	    if (schedObject instanceof SIRSplitter)
		return;
	    //add one to the count for this node
	    FlatNode fnode = FlatNode.getFlatNode((SIROperator)schedObject);
	    if (!counts.containsKey(fnode))
		counts.put(fnode, new Integer(1));
	    else {
		//add one to counter
		int old = ((Integer)counts.get(fnode)).intValue();
		counts.put(fnode, new Integer(old + 1));
	    }
	    
	}
    }
}

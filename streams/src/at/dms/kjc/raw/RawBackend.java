package at.dms.kjc.raw;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;
import java.util.*;


public class RawBackend {

    public static void run(SIRStream str,
			JInterfaceDeclaration[] 
			interfaces,
			SIRInterfaceTable[]
			interfaceTables) {
	// DEBUGGING PRINTING
	System.out.println("Entry to RAW Backend");
	//SIRPrinter printer1 = new SIRPrinter();
	//str.accept(printer1);
	//printer1.close();
	
	// propagate constants and unroll loop
	System.out.println("Running Constant Prop and Unroll...");
	ConstantProp.propagateAndUnroll(str);
	System.out.println("Done Constant Prop and Unroll...");
	/*	
		if (StreamItOptions.fusion) {
		System.out.println("Running Fusion");
		Fusion.fuse((SIRPipeline)str, 
		(SIRFilter)((SIRPipeline)str).get(0), 
		(SIRFilter)((SIRPipeline)str).get(1));
		}
	*/

	/* DON'T KNOW IF THIS SHOULD BE DONE!!
        
	// flatten split/joins with duplicate splitters and RR joiners
	
	if (StreamItOptions.fusion)
	str = SJFlatten.doFlatten(str);
	*/

        // do constant propagation on fields
        if (StreamItOptions.constprop) {
	    System.out.println("Running Constant Propagation of Fields");
	    FieldProp.doPropagate(str);
	}
	
	// name the components
	System.out.println("Namer Begin...");
	Namer.assignNames(str);
	System.out.println("Namer End.");
	//Flatten the graph
	System.out.println("Flattener Begin...");
	RawFlattener.flatten(str);
	RawFlattener.dumpGraph();
	System.out.println("Flattener End.");
	// layout the components (assign filters to tiles)
	System.out.println("Hand Assign Begin...");
	Layout.handAssign(RawFlattener.top);
	System.out.println("Hand Assign End.");
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
    
}

package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.SIRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

/**
 * This is the main class for decomposing the high SIR into
 * lower-level function calls.
 */
public class Flattener {

    /**
     * Flattens <str> into a low IR representation, given that <interfaces>
     * are all the top-level interfaces declared in the program and 
     * <interfaceTables> represents the mapping from interfaces to methods
     * that implement a given interface in a given class.
     */
    public static JClassDeclaration flatten(SIRStream str,
					    JInterfaceDeclaration[] 
					    interfaces,
					    SIRInterfaceTable[]
					    interfaceTables) {
	// DEBUGGING PRINTING
	System.out.println("--------- ON ENTRY TO FLATTENER ----------------");
	SIRPrinter printer1 = new SIRPrinter();
	str.accept(printer1);
	printer1.close();

	// propagate constants and unroll loops
	ConstantProp.propagateAndUnroll(str);

	/*
	Fusion.fuse((SIRPipeline)str, 
		    (SIRFilter)((SIRPipeline)str).get(0), 
		    (SIRFilter)((SIRPipeline)str).get(1));
	*/

        // flatten split/joins with duplicate splitters and RR joiners
        // str = DupRR.doFlatten(str);

	// DEBUGGING PRINTING
	System.out.println("--------- AFTER CONSTANT PROP / FUSION --------");
	printer1 = new SIRPrinter();
	str.accept(printer1);
	printer1.close();

	// name the components
	Namer.assignNames(str);
	// make single structure
	JClassDeclaration flatClass = Structurer.structure(str, 
							   interfaces,
							   interfaceTables);
	// build schedule as set of higher-level work functions
	Schedule schedule = SIRScheduler.schedule(str, flatClass);
	// add LIR hooks to init functions
	LowerInitFunctions.lower(str, schedule);

	// DEBUGGING PRINTING
	System.out.println("----------- AFTER FLATTENER ------------------");
	IRPrinter printer = new IRPrinter();
	flatClass.accept(printer);
	printer.close();

	return flatClass;
    }
   
}

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
     * Flattens <str> into a low IR representation.
     */
    public static JClassDeclaration flatten(SIRStream str) {
	// DEBUGGING PRINTING
	SIRPrinter printer1 = new SIRPrinter();
	str.accept(printer1);
	printer1.close();

	// propagate constants and unroll loops
	ConstantProp.propagateAndUnroll(str);
	// name the components
	Namer.assignNames(str);
	// make single structure
	JClassDeclaration flatClass = Structurer.structure(str);
	// build schedule as set of higher-level work functions
	Schedule schedule = SIRScheduler.schedule(str, flatClass);
	// add LIR hooks to init functions
	LowerInitFunctions.lower(str, schedule);

	// DEBUGGING PRINTING
	IRPrinter printer = new IRPrinter();
	flatClass.accept(printer);
	printer.close();

	return flatClass;
    }
   
}

package at.dms.kjc.sir.lowering;

import at.dms.util.IRPrinter;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/**
 * This is the main class for decomposing the high SIR into
 * lower-level function calls.
 */
public class Flattener {

    /**
     * Flattens <str> into a low IR representation.
     */
    public static JClassDeclaration flatten(SIRStream str) {
	// name the components
	Namer.assignNames(str);
	// make single structure
	JClassDeclaration flatClass = Structurer.structure(str);
	// build schedule as set of higher-level work functions
	IRPrinter printer = new IRPrinter();
	flatClass.accept(printer);
	printer.close();
	// add LIR hooks to init functions
	return null;
    }
    
}

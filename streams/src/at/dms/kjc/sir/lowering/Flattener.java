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

	// name the components
	Namer.assignNames(str);
	// make single structure
	JClassDeclaration flatClass = Structurer.structure(str);
	// build schedule as set of higher-level work functions
	Schedule schedule = SIRScheduler.schedule(str, flatClass);
	// add LIR hooks to init functions
	LowerInitFunctions.lower(str, schedule);
	// add main function
	addMainFunction(str, flatClass);

	// DEBUGGING PRINTING
	IRPrinter printer = new IRPrinter();
	flatClass.accept(printer);
	printer.close();

	return flatClass;
    }

    /**
     * Adds a main function to <flatClass>, with the information
     * necessary to call the toplevel init function in <toplevel>.
     */
    private static void addMainFunction(SIRStream toplevel, 
					JClassDeclaration flatClass) {
	// construct LIR node
	LIRMainFunction[] main 
	    = {new LIRMainFunction(toplevel.getName(),
				   new LIRFunctionPointer(toplevel.
							  getInit().
							  getName()),
				   null)};
	JBlock mainBlock = new JBlock(null, main, null);

	// add a method to <flatClass>
	flatClass.addMethod(
		new JMethodDeclaration( /* tokref     */ null,
				    /* modifiers  */ at.dms.kjc.
				                     Constants.ACC_PUBLIC,
				    /* returntype */ CStdType.Void,
				    /* identifier */ "main",
				    /* parameters */ JFormalParameter.EMPTY,
				    /* exceptions */ CClassType.EMPTY,
				    /* body       */ mainBlock,
				    /* javadoc    */ null,
				    /* comments   */ null));
    }
    
}

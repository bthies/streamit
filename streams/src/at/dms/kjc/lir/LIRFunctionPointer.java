package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a function pointer.  (Should we include the
 * signature of the function in here, too?
 */
public class LIRFunctionPointer {

    /**
     * The name of the function.
     */
    private String name;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    public LIRFunctionPointer(String name) {
	this.name = name;
    }
}

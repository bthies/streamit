package at.dms.kjc.sir.lowering;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/**
 * This is the main class for decomposing the high SIR into
 * lower-level function calls
 */
public class Flattener {
    
    /**
     * Flattens <str> into a low IR representation.
     */
    public static JClassDeclaration flatten(SIRStream str) {
	// name the components
	Namer.assignNames(str);
	// make single structure
	
	// scheduling!
	//   - arrange calls to work functions according to michal
	// add hooks to init functions
	//   - subcalls to init
	//   - peek/pop/push etc.
	return null;
    }
    
}

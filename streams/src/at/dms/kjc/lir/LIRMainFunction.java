package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents the contents of the main function in the low-level
 * program.  It calls the init function of the top-level stream, and
 * should look something like this:
 *
 * HelloWorld6_data *test = malloc(sizeof(HelloWorld6_data));
 * test->c = create_context(test);
 * HelloWorld6_init(test, NULL);
 *
 * streamit_run(test->c);
 */
public class LIRMainFunction extends LIRNode {

    /**
     * The name of the type of the struct required by the toplevel
     * init function.  
     */
    private String typeName;
    
    /**
     * The toplevel init function.
     */
    private LIRFunctionPointer init;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRMainFunction(String typeName,
			   LIRFunctionPointer init) {
	// stream context is null since we're at the toplevel
	super(null);
	this.init = init;
	this.typeName = typeName;
    }

    public void accept(SLIRVisitor v) {
        v.visitMainFunction(this, 
			    this.typeName, 
			    this.init);
    }
}

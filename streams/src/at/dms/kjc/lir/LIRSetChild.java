package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This registers a child stream with the runtime system.
 */
public class LIRSetChild extends LIRNode {

    /**
     * The encode function.
     */
    private JExpression childContext;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetChild(JExpression streamContext,
		       JExpression childContext) {
	super(streamContext);
	this.childContext = childContext;
    }
}

package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This registers the function in the stream that takes the canonical
 * representation from the runtime system and encodes it into a form
 * that a black box can utilize.
 */
public class LIRSetEncode extends LIRNode {

    /**
     * The encode function.
     */
    private LIRFunctionPointer encode;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetEncode(JExpression streamContext,
                        LIRFunctionPointer encode) {
        super(streamContext);
        this.encode = encode;
    }

    public void accept(SLIRVisitor v) {
        v.visitSetEncode(this, this.getStreamContext(), this.encode);
    }
}

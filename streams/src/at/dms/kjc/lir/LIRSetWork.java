package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This registers a stream's work function with the runtime system.
 */
public class LIRSetWork extends LIRNode {

    /**
     * The work function.
     */
    private LIRFunctionPointer work;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetWork(JExpression streamContext,
                      LIRFunctionPointer work) {
        super(streamContext);
        this.work = work;
    }

    public void accept(SLIRVisitor v) {
        v.visitSetWork(this, this.getStreamContext(), this.work);
    }
}

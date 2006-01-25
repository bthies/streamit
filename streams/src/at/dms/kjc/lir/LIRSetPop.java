package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This sets how many items are popped by this stream.
 */
public class LIRSetPop extends LIRNode {

    /**
     * The encode function.
     */
    private int pop;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetPop(JExpression streamContext,
                     int pop) {
        super(streamContext);
        this.pop = pop;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitSetPop(this, this.getStreamContext(), this.pop);
    }
}

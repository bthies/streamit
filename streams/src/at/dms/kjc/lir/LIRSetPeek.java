package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This sets how many items are peeked by this stream.
 */
public class LIRSetPeek extends LIRNode {

    /**
     * The encode function.
     */
    private int peek;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetPeek(JExpression streamContext,
                      int peek) {
        super(streamContext);
        this.peek = peek;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitSetPeek(this, this.getStreamContext(), this.peek);
    }
}

package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This sets the type of a stream.
 */
public class LIRSetStreamType extends LIRNode {

    /**
     * The encode function.
     */
    private LIRStreamType streamType;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRSetStreamType(JExpression streamContext,
                            LIRStreamType streamType) {
        super(streamContext);
        this.streamType = streamType;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitSetStreamType(this, 
                             this.getStreamContext(), 
                             this.streamType);
    }
}

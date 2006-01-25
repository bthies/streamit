package at.dms.kjc.lir;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This is a node in the low IR.  A low IR node appears in the init
 * function to communicate information to the runtime system.  All
 * calls are associated with a stream context, which is contained in this.
 */
public class LIRNode extends JStatement {

    /**
     * The name of the stream context for this node.
     */
    private JExpression streamContext;

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node in the parsing tree
     */
    protected LIRNode(JExpression streamContext) {
        // no token reference or comments
        super(null, null);
        this.streamContext = streamContext;
    }

    // ----------------------------------------------------------------------
    // SEMANTIC ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Analyses the statement (semantically) - NOT SUPPORTED YET.
     */
    public void analyse(CBodyContext context) throws PositionedError {
        at.dms.util.Utils.fail("Analysis of LIR nodes not supported yet.");
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor - NOT SUPPORTED YET.
     */
    public void accept(KjcVisitor p) {
        if (p instanceof SLIRVisitor)
            this.accept((SLIRVisitor) p);
        else
            at.dms.util.Utils.fail("Visitors to LIR nodes not supported yet.");
    }

    public void accept(SLIRVisitor p) {
        p.visitNode(this);
    }

    public Object accept(AttributeVisitor p) {
        // no action is defined for attribute visitors on lir nodes
        return this;
    }

    /**
     * Generates a sequence of bytescodes - NOT SUPPORTED YET.
     */
    public void genCode(CodeSequence code) {
        at.dms.util.Utils.fail("Codegen of LIR nodes not supported yet.");
    }

    /**
     * Returns the StreamContext associated with this node.
     */
    public JExpression getStreamContext() {
        return streamContext;
    }
}

package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents an operator in the stream graph.
 */
public class SIROperator extends at.dms.util.Utils {
    /**
     * The stream structure containing this, or NULL if this is the
     * toplevel stream.
     */
    protected SIRStream parent;
}

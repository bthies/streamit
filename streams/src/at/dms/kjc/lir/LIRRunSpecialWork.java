package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a call to a work function of a library-defined
 * stream unit.
 */
public class LIRRunSpecialWork extends LIRNode {

    /**
     * The type of work function to run
     */
    private LIRSpecialWorkType workType;
    
    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.  The streamContext should be the stream
     * context of the node that is being executed.
     */
    public LIRRunSpecialWork(JExpression streamContext,
			     LIRSpecialWorkType workType) {
	super(streamContext);
	this.workType = workType;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitRunSpecialWork(this);
    }

    public LIRSpecialWorkType getType() {
	return workType;
    }
}

package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This represents a call to a work function of a library-defined
 * stream unit.
 */
public class LIRRunSpecialWork extends LIRNode {

    /**
     * The stream context of the unit to run.
     */
    private JExpression childContext;
    /**
     * The type of work function to run
     */
    private LIRSpecialWorkType workType;
    
    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Construct a node.
     */
    public LIRRunSpecialWork(JExpression streamContext,
			     JExpression childContext,
			     LIRSpecialWorkType workType) {
	super(streamContext);
	this.childContext = childContext;
	this.workType = workType;
    }

    public JExpression getChildContext() {
	return childContext;
    }

    public void accept(SLIRVisitor v)
    {
        v.visitRunSpecialWork(this);
    }

    public LIRSpecialWorkType getType() {
	return workType;
    }
}

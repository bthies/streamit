package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * This appears at the entry to a work function.
 */
public class LIRWorkEntry extends LIRNode
{
    public LIRWorkEntry(JExpression streamContext)
    {
        super(streamContext);
    }
    public void accept(SLIRVisitor v)
    {
        v.visitWorkEntry(this);
    }
}

package at.dms.kjc.lir;

import at.dms.kjc.*;
import at.dms.compiler.*;

/**
 * Tell the run-time system how many items need to be pushed on
 * to the feedback input of a feedback loop's joiner, and what function
 * to call to get them.
 */
public class LIRSetDelay extends LIRNode 
{
    /**
     * The data structure corresponding to the current stream.
     */
    private JExpression data;
    
    /**
     * The number of items to push.
     */
    private int delay;

    /**
     * The type of item to push.
     */
    private CType type;
    
    /**
     * The function to call to get items.
     */
    private LIRFunctionPointer delayfn;
    
    /**
     * Construct a node.
     */
    public LIRSetDelay(JExpression data,
                       JExpression streamContext,
                       int delay,
                       CType type,
                       LIRFunctionPointer delayfn) 
    {
        super(streamContext);
        this.data = data;
        this.delay = delay;
        this.type = type;
        this.delayfn = delayfn;
    }
    
    public void accept(SLIRVisitor v) 
    {
        v.visitSetDelay(this, this.data, this.getStreamContext(),
                        this.delay, this.type, this.delayfn);
    }
}

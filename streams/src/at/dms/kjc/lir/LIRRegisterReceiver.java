package at.dms.kjc.lir;

import at.dms.compiler.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

/**
 * Call to the C library register_receiver() function.
 */
public class LIRRegisterReceiver extends LIRNode 
{
    /**
     * The (global) portal to register with.
     */
    private SIRPortal portal;

    /**
     * The name of the child (e.g. child1)
     */
    private String childName;

    /**
     * The interface table to register.
     */
    private SIRInterfaceTable itable;
    
    /**
     * Construct a node.
     */
    public LIRRegisterReceiver(JExpression streamContext,
                               SIRPortal portal,
                               String childName,
                               SIRInterfaceTable itable)
    {
        super(streamContext);
        this.portal = portal;
        this.childName = childName;
        this.itable = itable;
    }
    
    public void accept(SLIRVisitor v)
    {
        v.visitRegisterReceiver(this,
                                this.getStreamContext(),
                                this.portal,
                                this.childName,
                                this.itable);
    }
}

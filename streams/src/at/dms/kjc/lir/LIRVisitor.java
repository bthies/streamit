/*
 * LIRVisitor.java: visit StreaMIT Low IR nodes
 * $Id: LIRVisitor.java,v 1.1 2001-10-01 17:39:46 dmaze Exp $
 */

package at.dms.kjc.lir;

import at.dms.kjc.*;

public interface LIRVisitor 
{
    /**
     * Visits a function pointer.
     */
    public void visitFunctionPointer(LIRFunctionPointer self,
                                     String name);
    
    /**
     * Visits an LIR node.
     */
    public void visitNode(LIRNode self);

    /**
     * Visits a child registration node.
     */
    public void visitSetChild(LIRSetChild self,
                              JExpression streamContext,
                              JExpression childContext);
    
    /**
     * Visits a decoder registration node.
     */
    public void visitSetDecode(LIRSetDecode self,
                               JExpression streamContext,
                               LIRFunctionPointer fp);
    
    /**
     * Visits an encoder registration node.
     */
    public void visitSetEncode(LIRSetEncode self,
                               JExpression streamContext,
                               LIRFunctionPointer fp);
    
    /**
     * Visits a peek-rate-setting node.
     */
    public void visitSetPeek(LIRSetPeek self,
                             JExpression streamContext,
                             int peek);
    
    /**
     * Visits a pop-rate-setting node.
     */
    public void visitSetPop(LIRSetPop self,
                            JExpression streamContext,
                            int pop);
    
    /**
     * Visits a push-rate-setting node.
     */
    public void visitSetPush(LIRSetPush self,
                             JExpression streamContext,
                             int push);

    /**
     * Visits a stream-type-setting node.
     */
    public void visitSetStreamType(LIRSetStreamType self,
                                   JExpression streamContext,
                                   LIRStreamType streamType);
    
    /**
     * Visits a work-function-setting node.
     */
    public void visitSetWork(LIRSetWork self,
                             JExpression streamContext,
                             LIRFunctionPointer fn);
}


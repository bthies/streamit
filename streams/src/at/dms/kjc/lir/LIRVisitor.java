/*
 * LIRVisitor.java: visit StreaMIT Low IR nodes
 * $Id: LIRVisitor.java,v 1.2 2001-10-01 17:45:49 dmaze Exp $
 */

package at.dms.kjc.lir;

import at.dms.kjc.*;

public interface LIRVisitor 
{
    /**
     * Visits a function pointer.
     */
    void visitFunctionPointer(LIRFunctionPointer self,
                              String name);
    
    /**
     * Visits an LIR node.
     */
    void visitNode(LIRNode self);

    /**
     * Visits a child registration node.
     */
    void visitSetChild(LIRSetChild self,
                       JExpression streamContext,
                       JExpression childContext);
    
    /**
     * Visits a decoder registration node.
     */
    void visitSetDecode(LIRSetDecode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp);
    
    /**
     * Visits an encoder registration node.
     */
    void visitSetEncode(LIRSetEncode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp);
    
    /**
     * Visits a peek-rate-setting node.
     */
    void visitSetPeek(LIRSetPeek self,
                      JExpression streamContext,
                      int peek);
    
    /**
     * Visits a pop-rate-setting node.
     */
    void visitSetPop(LIRSetPop self,
                     JExpression streamContext,
                     int pop);
    
    /**
     * Visits a push-rate-setting node.
     */
    void visitSetPush(LIRSetPush self,
                      JExpression streamContext,
                      int push);

    /**
     * Visits a stream-type-setting node.
     */
    void visitSetStreamType(LIRSetStreamType self,
                            JExpression streamContext,
                            LIRStreamType streamType);
    
    /**
     * Visits a work-function-setting node.
     */
    void visitSetWork(LIRSetWork self,
                      JExpression streamContext,
                      LIRFunctionPointer fn);
}


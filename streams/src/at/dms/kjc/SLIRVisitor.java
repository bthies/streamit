/*
 * LIRVisitor.java: visit StreaMIT Low IR nodes
 * $Id: SLIRVisitor.java,v 1.27 2003-12-02 21:12:09 dmaze Exp $
 */

package at.dms.kjc;

import java.util.List;

import at.dms.kjc.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.*;

/**
 * This visitor is for visiting statement-level constructs in the
 * streamit IR.  It visits both high-level constructs like
 * SIRInitStatement that never appear in the LIR, as well as low-level
 * constructs like LIRSetPeek that never appear in the low IR.
 */
public interface SLIRVisitor extends KjcVisitor
{

    /**
     * SIR NODES.
     */

    /**
     * Visits an init statement.
     */
    void visitInitStatement(SIRInitStatement self,
			    SIRStream target);

    /* Visits an interface table.
     */
    void visitInterfaceTable(SIRInterfaceTable self);
    
    /**
     * Visits a latency.
     */
    void visitLatency(SIRLatency self);

    /**
     * Visits a max latency.
     */
    void visitLatencyMax(SIRLatencyMax self);

    /**
     * Visits a latency range.
     */
    void visitLatencyRange(SIRLatencyRange self);

    /**
     * Visits a latency set.
     */
    void visitLatencySet(SIRLatencySet self);

    void visitCreatePortalExpression(SIRCreatePortal self);

    /**
     * Visits a message statement.
     */
    void visitMessageStatement(SIRMessageStatement self,
			       JExpression portal,
                               String iname,
                               String ident,
			       JExpression[] args,
			       SIRLatency latency);

    /**
     * Visits a peek expression.
     */
    void visitPeekExpression(SIRPeekExpression self,
                             CType tapeType,
			     JExpression arg);

    /**
     * Visits a pop expression.
     */
    void visitPopExpression(SIRPopExpression self,
                            CType tapeType);

    /**
     * Visits a message-receiving portal.
     */
    void visitPortal(SIRPortal self);

    /**
     * Visits a print statement.
     */
    void visitPrintStatement(SIRPrintStatement self,
			     JExpression arg);

    /**
     * Visits a push expression.
     */
    void visitPushExpression(SIRPushExpression self,
                             CType tapeType,
			     JExpression arg);

    /**
     * Visits a phase-invocation statement.
     */
    void visitPhaseInvocation(SIRPhaseInvocation self,
                              JMethodCallExpression call,
                              JExpression peek,
                              JExpression pop,
                              JExpression push);

    /**
     * Visits a register-receiver statement.
     */
    void visitRegReceiverStatement(SIRRegReceiverStatement self,
				   JExpression portal,
				   SIRStream receiver,
				   JMethodDeclaration[] methods);

    /**
     * Visits a register-sender statement.
     */
    void visitRegSenderStatement(SIRRegSenderStatement self,
				 String portal,
				 SIRLatency latency);

    /**
     * LIR NODES.
     */

    /**
     * Visits a function pointer.
     */
    void visitFunctionPointer(LIRFunctionPointer self,
                              String name);

    /**
     * Visits a file reader.
     */
    void visitFileReader(LIRFileReader self);
    
    /**
     * Visits a file writer.
     */
    void visitFileWriter(LIRFileWriter self);

    /**
     * Visits an identity creator.
     */
    void visitIdentity(LIRIdentity self);
    
    /**
     * Visits an LIR node.
     */
    void visitNode(LIRNode self);

    /**
     * Visits a child registration node.
     */
    void visitSetChild(LIRSetChild self,
                       JExpression streamContext,
                       String childType,
		       String childName);
    
    /**
     * Visits a decoder registration node.
     */
    void visitSetDecode(LIRSetDecode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp);

    /**
     * Visits a feedback loop delay node.
     */
    void visitSetDelay(LIRSetDelay self,
                       JExpression data,
                       JExpression streamContext,
                       int delay,
                       CType type,
                       LIRFunctionPointer fp);
    
    /**
     * Visits an encoder registration node.
     */
    void visitSetEncode(LIRSetEncode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp);

    /**
     * Visits a joiner-setting node.
     */
    void visitSetJoiner(LIRSetJoiner self,
                        JExpression streamContext,
                        SIRJoinType type,
                        int ways,
                        int[] weights);
    
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
     * Visits a splitter-setting node.
     */
    void visitSetSplitter(LIRSetSplitter self,
                          JExpression streamContext,
                          SIRSplitType type,
                          int ways,
                          int[] weights);
    
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

    /**
     * Visits a tape registerer.
     */
    void visitSetTape(LIRSetTape self,
                      JExpression streamContext,
		      JExpression srcStruct,
		      JExpression dstStruct,
		      CType type,
		      int size);

    /**
     * Visits a main function contents.
     */
    void visitMainFunction(LIRMainFunction self,
			   String typeName,
			   LIRFunctionPointer init,
			   List initStatements);


    /**
     * Visits a set body of feedback loop.
     */
    void visitSetBodyOfFeedback(LIRSetBodyOfFeedback self,
				JExpression streamContext,
                                JExpression childContext,
				CType inputType,
				CType outputType,
				int inputSize,
				int outputSize);

    /**
     * Visits a set loop of feedback loop.
     */
    void visitSetLoopOfFeedback(LIRSetLoopOfFeedback self,
				JExpression streamContext,
                                JExpression childContext,
				CType inputType,
				CType outputType,
				int inputSize,
				int outputSize);

    /**
     * Visits a set a parallel stream.
     */
    void visitSetParallelStream(LIRSetParallelStream self,
				JExpression streamContext,
                                JExpression childContext,
				int position,
				CType inputType,
				CType outputType,
				int inputSize,
				int outputSize);

    /**
     * Visits a work function entry.
     */
    void visitWorkEntry(LIRWorkEntry self);

    /**
     * Visits a work function exit.
     */
    void visitWorkExit(LIRWorkExit self);
}


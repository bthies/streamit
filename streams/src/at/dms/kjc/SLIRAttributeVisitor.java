package at.dms.kjc;

import java.util.List;
import at.dms.kjc.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * Implementation of an Attributed Visitor Design Pattern for KJC.
 *
 */
public interface SLIRAttributeVisitor extends AttributeVisitor {

    /**
     * SIR NODES.
     */

    /**
     * Visits an init statement.
     */
    Object visitInitStatement(SIRInitStatement self,
			      SIRStream target);

    /**
     * Visits an interface table.
     */
    Object visitInterfaceTable(SIRInterfaceTable self);
    
    /**
     * Visits a latency.
     */
    Object visitLatency(SIRLatency self);

    /**
     * Visits a max latency.
     */
    Object visitLatencyMax(SIRLatencyMax self);

    /**
     * Visits a latency range.
     */
    Object visitLatencyRange(SIRLatencyRange self);

    /**
     * Visits a latency set.
     */
    Object visitLatencySet(SIRLatencySet self);

    Object visitCreatePortalExpression(SIRCreatePortal self);

    /**
     * Visits a message statement.
     */
    Object visitMessageStatement(SIRMessageStatement self,
			       JExpression portal,
                               String iname,
                               String ident,
			       JExpression[] args,
			       SIRLatency latency);

    /**
     * Visits a peek expression.
     */
    Object visitPeekExpression(SIRPeekExpression self,
                             CType tapeType,
			     JExpression arg);

    /**
     * Visits a pop expression.
     */
    Object visitPopExpression(SIRPopExpression self,
                            CType tapeType);

    /**
     * Visits a message-receiving portal.
     */
    Object visitPortal(SIRPortal self);

    /**
     * Visits a print statement.
     */
    Object visitPrintStatement(SIRPrintStatement self,
			     JExpression arg);

    /**
     * Visits a push expression.
     */
    Object visitPushExpression(SIRPushExpression self,
                             CType tapeType,
			     JExpression arg);

    /**
     * Visits a phase-invocation statement.
     */
    Object visitPhaseInvocation(SIRPhaseInvocation self,
                                JMethodCallExpression call,
                                JExpression peek,
                                JExpression pop,
                                JExpression push);

    /**
     * Visits a register-receiver statement.
     */
    Object visitRegReceiverStatement(SIRRegReceiverStatement self,
				   JExpression portal,
				   SIRStream receiver,
				   JMethodDeclaration[] methods);

    /**
     * Visits a register-sender statement.
     */
    Object visitRegSenderStatement(SIRRegSenderStatement self,
				 String portal,
				 SIRLatency latency);

    /**
     * LIR NODES.
     */

    /**
     * Visits a function pointer.
     */
    Object visitFunctionPointer(LIRFunctionPointer self,
                              String name);
    
    /**
     * Visits an LIR node.
     */
    Object visitNode(LIRNode self);

    /**
     * Visits a child registration node.
     */
    Object visitSetChild(LIRSetChild self,
                       JExpression streamContext,
                       String childType,
		       String childName);
    
    /**
     * Visits a decoder registration node.
     */
    Object visitSetDecode(LIRSetDecode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp);

    /**
     * Visits a feedback loop delay node.
     */
    Object visitSetDelay(LIRSetDelay self,
                       JExpression data,
                       JExpression streamContext,
                       int delay,
                       CType type,
                       LIRFunctionPointer fp);
    
    /**
     * Visits an encoder registration node.
     */
    Object visitSetEncode(LIRSetEncode self,
                        JExpression streamContext,
                        LIRFunctionPointer fp);

    /**
     * Visits a joiner-setting node.
     */
    Object visitSetJoiner(LIRSetJoiner self,
                        JExpression streamContext,
                        SIRJoinType type,
                        int ways,
                        int[] weights);
    
    /**
     * Visits a peek-rate-setting node.
     */
    Object visitSetPeek(LIRSetPeek self,
                      JExpression streamContext,
                      int peek);
    
    /**
     * Visits a pop-rate-setting node.
     */
    Object visitSetPop(LIRSetPop self,
                     JExpression streamContext,
                     int pop);
    
    /**
     * Visits a push-rate-setting node.
     */
    Object visitSetPush(LIRSetPush self,
                      JExpression streamContext,
                      int push);

    
    /**
     * Visits a file reader.
     */
    Object visitFileReader(LIRFileReader self);

    /**
     * Visits a file writer.
     */
    Object visitFileWriter(LIRFileWriter self);
    
    /**
     * Visits a splitter-setting node.
     */
    Object visitSetSplitter(LIRSetSplitter self,
                          JExpression streamContext,
                          SIRSplitType type,
                          int ways,
                          int[] weights);
    
    /**
     * Visits a stream-type-setting node.
     */
    Object visitSetStreamType(LIRSetStreamType self,
                            JExpression streamContext,
                            LIRStreamType streamType);
    
    /**
     * Visits a work-function-setting node.
     */
    Object visitSetWork(LIRSetWork self,
                      JExpression streamContext,
                      LIRFunctionPointer fn);

    /**
     * Visits a tape registerer.
     */
    Object visitSetTape(LIRSetTape self,
                      JExpression streamContext,
		      JExpression srcStruct,
		      JExpression dstStruct,
		      CType type,
		      int size);

    /**
     * Visits a main function contents.
     */
    Object visitMainFunction(LIRMainFunction self,
			   String typeName,
			   LIRFunctionPointer init,
			   List initStatements);


    /**
     * Visits a set body of feedback loop.
     */
    Object visitSetBodyOfFeedback(LIRSetBodyOfFeedback self,
				JExpression streamContext,
                                JExpression childContext,
				CType inputType,
				CType outputType,
				int inputSize,
				int outputSize);

    /**
     * Visits a set loop of feedback loop.
     */
    Object visitSetLoopOfFeedback(LIRSetLoopOfFeedback self,
				JExpression streamContext,
                                JExpression childContext,
				CType inputType,
				CType outputType,
				int inputSize,
				int outputSize);

    /**
     * Visits a set a parallel stream.
     */
    Object visitSetParallelStream(LIRSetParallelStream self,
				JExpression streamContext,
                                JExpression childContext,
				int position,
				CType inputType,
				CType outputType,
				int inputSize,
				int outputSize);
}

/*
 * LIRVisitor.java: visit StreaMIT Low IR nodes
 * $Id: SLIREmptyVisitor.java,v 1.11 2003-11-04 01:13:36 jasperln Exp $
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
public class SLIREmptyVisitor extends KjcEmptyVisitor 
    implements Constants, SLIRVisitor
{

    /**
     * SIR NODES.
     */

    /**
     * Visits an init statement.
     */
    public void visitInitStatement(SIRInitStatement self,
				   SIRStream target) {
	for (int i=0; i<self.getArgs().size(); i++) {
	    ((JExpression)self.getArgs().get(i)).accept(this);
	}
    }

    /**
     * Visits an interface table.
     */
    public void visitInterfaceTable(SIRInterfaceTable self) {}

    /**
     * Visits a latency.
     */
    public void visitLatency(SIRLatency self) {}

    /**
     * Visits a max latency.
     */
    public void visitLatencyMax(SIRLatencyMax self) {}

    /**
     * Visits a latency range.
     */
    public void visitLatencyRange(SIRLatencyRange self) {}

    /**
     * Visits a latency set.
     */
    public void visitLatencySet(SIRLatencySet self) {}

    public void visitCreatePortalExpression(SIRCreatePortal self) {}

    /**
     * Visits a message statement.
     */
    public void visitMessageStatement(SIRMessageStatement self,
				      JExpression portal,
				      String iname,
				      String ident,
				      JExpression[] args,
				      SIRLatency latency) {
	portal.accept(this);
	for (int i=0; i<args.length; i++) {
	    args[i].accept(this);
	}
	latency.accept(this);
    }

    /**
     * Visits a peek expression.
     */
    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	arg.accept(this);
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
    }

    /**
     * Visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
				    JExpression arg) {
	arg.accept(this);
    }

    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	arg.accept(this);
    }

    /**
     * Visits a phase-invocation statement.
     */
    public void visitPhaseInvocation(SIRPhaseInvocation self,
                                     JMethodCallExpression call,
                                     JExpression peek,
                                     JExpression pop,
                                     JExpression push)
    {
        call.accept(this);
        peek.accept(this);
        pop.accept(this);
        push.accept(this);
    }

    /**
     * Visits a register-receiver statement.
     */
    public void visitRegReceiverStatement(SIRRegReceiverStatement self,
					  JExpression portal,
					  SIRStream receiver,
					  JMethodDeclaration[] methods) {
	portal.accept(this);
    }

    /**
     * Visits a register-sender statement.
     */
    public void visitRegSenderStatement(SIRRegSenderStatement self,
					String portal,
					SIRLatency latency) {
	latency.accept(this);
    }

    /**
     * LIR NODES.
     */

    /**
     * Visits a function pointer.
     */
    public void visitFunctionPointer(LIRFunctionPointer self,
				     String name) {
    }
    
    /**
     * Visits an LIR node.
     */
    public void visitNode(LIRNode self) {}

    /**
     * Visits a child registration node.
     */
    public void visitSetChild(LIRSetChild self,
			      JExpression streamContext,
			      String childType,
			      String childName) {
	streamContext.accept(this);
    }
    
    /**
     * Visits a decoder registration node.
     */
    public void visitSetDecode(LIRSetDecode self,
			       JExpression streamContext,
			       LIRFunctionPointer fp) {
	streamContext.accept(this);
	fp.accept(this);
    }

    /**
     * Visits a feedback loop delay node.
     */
    public void visitSetDelay(LIRSetDelay self,
			      JExpression data,
			      JExpression streamContext,
			      int delay,
			      CType type,
			      LIRFunctionPointer fp) {
	data.accept(this);
	streamContext.accept(this);
	fp.accept(this);
    }
    
    /**
     * Visits a file reader.
     */
    public void visitFileReader(LIRFileReader self) {
	self.getStreamContext().accept(this);
    }
    
    /**
     * Visits a file writer.
     */
    public void visitFileWriter(LIRFileWriter self) {
	self.getStreamContext().accept(this);
    }

    /**
     * Visits an identity creator.
     */
    public void visitIdentity(LIRIdentity self) {
        self.getStreamContext().accept(this);
    }
    
    /**
     * Visits an encoder registration node.
     */
    public void visitSetEncode(LIRSetEncode self,
			       JExpression streamContext,
			       LIRFunctionPointer fp) {
	streamContext.accept(this);
	fp.accept(this);
    }

    /**
     * Visits a joiner-setting node.
     */
    public void visitSetJoiner(LIRSetJoiner self,
			       JExpression streamContext,
			       SIRJoinType type,
			       int ways,
			       int[] weights) {
	streamContext.accept(this);
    }
    
    /**
     * Visits a peek-rate-setting node.
     */
    public void visitSetPeek(LIRSetPeek self,
			     JExpression streamContext,
			     int peek) {
	streamContext.accept(this);
    }
    
    /**
     * Visits a pop-rate-setting node.
     */
    public void visitSetPop(LIRSetPop self,
			    JExpression streamContext,
			    int pop) {
	streamContext.accept(this);
    }
    
    /**
     * Visits a push-rate-setting node.
     */
    public void visitSetPush(LIRSetPush self,
			     JExpression streamContext,
			     int push) {
	streamContext.accept(this);
    }

    /**
     * Visits a splitter-setting node.
     */
    public void visitSetSplitter(LIRSetSplitter self,
				 JExpression streamContext,
				 SIRSplitType type,
				 int ways,
				 int[] weights) {
	streamContext.accept(this);
    }
    
    /**
     * Visits a stream-type-setting node.
     */
    public void visitSetStreamType(LIRSetStreamType self,
				   JExpression streamContext,
				   LIRStreamType streamType) {
	streamContext.accept(this);
    }
    
    /**
     * Visits a work-function-setting node.
     */
    public void visitSetWork(LIRSetWork self,
			     JExpression streamContext,
			     LIRFunctionPointer fn) {
	streamContext.accept(this);
	fn.accept(this);
    }

    /**
     * Visits a tape registerer.
     */
    public void visitSetTape(LIRSetTape self,
			     JExpression streamContext,
			     JExpression srcStruct,
			     JExpression dstStruct,
			     CType type,
			     int size) {
	streamContext.accept(this);
	srcStruct.accept(this);
	dstStruct.accept(this);
    }

    /**
     * Visits a main function contents.
     */
    public void visitMainFunction(LIRMainFunction self,
				  String typeName,
				  LIRFunctionPointer init,
				  List initStatements) {
	init.accept(this);
	for (int i=0; i<initStatements.size(); i++) {
	    ((JStatement)initStatements.get(i)).accept(this);
	}
    }


    /**
     * Visits a set body of feedback loop.
     */
    public void visitSetBodyOfFeedback(LIRSetBodyOfFeedback self,
				       JExpression streamContext,
				       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	streamContext.accept(this);
	childContext.accept(this);
    }


    /**
     * Visits a set loop of feedback loop.
     */
    public void visitSetLoopOfFeedback(LIRSetLoopOfFeedback self,
				       JExpression streamContext,
				       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	streamContext.accept(this);
	childContext.accept(this);
    }

    /**
     * Visits a set a parallel stream.
     */
    public void visitSetParallelStream(LIRSetParallelStream self,
				       JExpression streamContext,
				       JExpression childContext,
				       int position,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	streamContext.accept(this);
	childContext.accept(this);
    }

    /**
     * Visits a work function entry.
     */
    public void visitWorkEntry(LIRWorkEntry self)
    {
        self.getStreamContext().accept(this);
    }

    /**
     * Visits a work function exit.
     */
    public void visitWorkExit(LIRWorkExit self)
    {
        self.getStreamContext().accept(this);
    }

    /**
     * Visits InlineAssembly
     */
    public void visitInlineAssembly(InlineAssembly self,String[] asm) {}
}


/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package at.dms.kjc;

import java.util.List;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.util.Utils;
import java.util.ListIterator;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This is just like ReplacingVisitor except for SLIR nodes, too.
 * Right now it only replaces statements in children.
 */
public class SLIRReplacingVisitor extends ReplacingVisitor 
    implements SLIRAttributeVisitor {

    /**
     * SIR NODES.
     */

    /**
     * Visits an init statement.
     */
    public Object visitInitStatement(SIRInitStatement self,
				     SIRStream target) {
	List args = self.getArgs();
	for (int i=0; i<args.size(); i++) {
	    JExpression newExp = (JExpression)((JExpression)args.get(i)).accept(this);
	    if (newExp!=null && newExp!=args.get(i)) {
		args.set(i, newExp);
	    }
	}
	return self;
    }

    /**
     * Visits an interface table.
     */
    public Object visitInterfaceTable(SIRInterfaceTable self) {
        return self;
    }

    /**
     * Visits a latency.
     */
    public Object visitLatency(SIRLatency self) {
	return self;
    }

    /**
     * Visits a max latency.
     */
    public Object visitLatencyMax(SIRLatencyMax self) {
    	return self;
    }

    /**
     * Visits a latency range.
     */
    public Object visitLatencyRange(SIRLatencyRange self) {
	return self;
    }

    /**
     * Visits a latency set.
     */
    public Object visitLatencySet(SIRLatencySet self) {
    	return self;
    }

    public Object visitCreatePortalExpression(SIRCreatePortal self) {
	return self;
    }

    /**
     * Visits a message statement.
     */
    public Object visitMessageStatement(SIRMessageStatement self,
				      JExpression portal,
				      String iname,
				      String ident,
				      JExpression[] args,
				      SIRLatency latency) {
	JExpression newExp = (JExpression)portal.accept(this);
	if (newExp!=null && newExp!=portal) {
	    self.setPortal(newExp);
	}
	
	visitArgs(args);
	latency.accept(this);
	return self;
    }

    /**
     * Visits a peek expression.
     */
    public Object visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	JExpression newExp = (JExpression)arg.accept(this);
	if (newExp!=null && newExp!=arg) {
	    self.setArg(newExp);
	}
	
	return self;
    }

    /**
     * Visits a pop expression.
     */
    public Object visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	return self;
    }

    /**
     * Visits a message-receiving portal.
     */
    public Object visitPortal(SIRPortal self) {
        return self;
    }

    /**
     * Visits a phase-invocation statement.
     */
    public Object visitPhaseInvocation(SIRPhaseInvocation self,
                                       JMethodCallExpression call,
                                       JExpression peek,
                                       JExpression pop,
                                       JExpression push)
    {
        JMethodCallExpression newCall =
            (JMethodCallExpression)call.accept(this);
        JExpression newPeek = (JExpression)peek.accept(this);
        JExpression newPop = (JExpression)pop.accept(this);
        JExpression newPush = (JExpression)push.accept(this);
        if (newCall != call) self.setCall(newCall);
        if (newPeek != peek) self.setPeek(newPeek);
        if (newPop != pop) self.setPop(newPop);
        if (newPush != push) self.setPush(newPush);
        return self;
    }

    /**
     * Visits a print statement.
     */
    public Object visitPrintStatement(SIRPrintStatement self,
				    JExpression arg) {
	JExpression newExp = (JExpression)arg.accept(this);
	if (newExp!=null && newExp!=arg) {
	    self.setArg(newExp);
	}
	
	return self;
    }

    /**
     * Visits a push expression.
     */
    public Object visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	JExpression newExp = (JExpression)arg.accept(this);
	if (newExp!=null && newExp!=arg) {
	    self.setArg(newExp);
	}
	
	return self;
    }

    /**
     * Visits a register-receiver statement.
     */
    public Object visitRegReceiverStatement(SIRRegReceiverStatement self,
					  JExpression portal,
					  SIRStream receiver,
					  JMethodDeclaration[] methods) {
	JExpression newExp = (JExpression)portal.accept(this);
	if (newExp!=null && newExp!=portal) {
	    self.setPortal(newExp);
	}
	
	return self;
    }

    /**
     * Visits a register-sender statement.
     */
    public Object visitRegSenderStatement(SIRRegSenderStatement self,
					String portal,
					SIRLatency latency) {
	latency.accept(this);
	return self;
    }

    /**
     * LIR NODES.
     */

    /**
     * Visits a function pointer.
     */
    public Object visitFunctionPointer(LIRFunctionPointer self,
				     String name) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	return self;
    }
    
    /**
     * Visits an LIR node.
     */
    public Object visitNode(LIRNode self) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	return self;
    }

    /**
     * Visits a child registration node.
     */
    public Object visitSetChild(LIRSetChild self,
				JExpression streamContext,
				String childType,
				String childName) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	return self;
    }
    

    /**
     * Visits a file reader.
     */
    public Object visitFileReader(LIRFileReader self) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	self.getStreamContext().accept(this);
	return self;
    }

    /**
     * Visits a file writer.
     */
    public Object visitFileWriter(LIRFileWriter self) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	self.getStreamContext().accept(this);
	return self;
    }
    
    /**
     * Visits a decoder registration node.
     */
    public Object visitSetDecode(LIRSetDecode self,
			       JExpression streamContext,
			       LIRFunctionPointer fp) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	fp.accept(this);
	return self;
    }

    /**
     * Visits a feedback loop delay node.
     */
    public Object visitSetDelay(LIRSetDelay self,
			      JExpression data,
			      JExpression streamContext,
			      int delay,
			      CType type,
			      LIRFunctionPointer fp) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	data.accept(this);
	streamContext.accept(this);
	fp.accept(this);
	return self;
    }
    
    /**
     * Visits an encoder registration node.
     */
    public Object visitSetEncode(LIRSetEncode self,
			       JExpression streamContext,
			       LIRFunctionPointer fp) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	fp.accept(this);
	return self;
    }

    /**
     * Visits a joiner-setting node.
     */
    public Object visitSetJoiner(LIRSetJoiner self,
			       JExpression streamContext,
			       SIRJoinType type,
			       int ways,
			       int[] weights) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	return self;
    }
    
    /**
     * Visits a peek-rate-setting node.
     */
    public Object visitSetPeek(LIRSetPeek self,
			     JExpression streamContext,
			     int peek) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	return self;
    }
    
    /**
     * Visits a pop-rate-setting node.
     */
    public Object visitSetPop(LIRSetPop self,
			    JExpression streamContext,
			    int pop) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	return self;
    }
    
    /**
     * Visits a push-rate-setting node.
     */
    public Object visitSetPush(LIRSetPush self,
			     JExpression streamContext,
			     int push) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	return self;
    }

    /**
     * Visits a splitter-setting node.
     */
    public Object visitSetSplitter(LIRSetSplitter self,
				 JExpression streamContext,
				 SIRSplitType type,
				 int ways,
				 int[] weights) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	return self;
    }
    
    /**
     * Visits a stream-type-setting node.
     */
    public Object visitSetStreamType(LIRSetStreamType self,
				   JExpression streamContext,
				   LIRStreamType streamType) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	return self;
    }
    
    /**
     * Visits a work-function-setting node.
     */
    public Object visitSetWork(LIRSetWork self,
			     JExpression streamContext,
			     LIRFunctionPointer fn) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	fn.accept(this);
	return self;
    }

    /**
     * Visits a tape registerer.
     */
    public Object visitSetTape(LIRSetTape self,
			     JExpression streamContext,
			     JExpression srcStruct,
			     JExpression dstStruct,
			     CType type,
			     int size) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	srcStruct.accept(this);
	dstStruct.accept(this);
	return self;
    }

    /**
     * Visits a main function contents.
     */
    public Object visitMainFunction(LIRMainFunction self,
				  String typeName,
				  LIRFunctionPointer init,
				  List initStatements) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	init.accept(this);
	for (ListIterator it = initStatements.listIterator(); it.hasNext(); ) {
	    JStatement old = (JStatement)it.next();
	    JStatement newSt = (JStatement)old.accept(this);
	    if (newSt!=null && newSt!=old) {
		it.set(newSt);
	    }
	}
	return self;
    }


    /**
     * Visits a set body of feedback loop.
     */
    public Object visitSetBodyOfFeedback(LIRSetBodyOfFeedback self,
				       JExpression streamContext,
				       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	childContext.accept(this);
	return self;
    }


    /**
     * Visits a set loop of feedback loop.
     */
    public Object visitSetLoopOfFeedback(LIRSetLoopOfFeedback self,
				       JExpression streamContext,
				       JExpression childContext,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	childContext.accept(this);
	return self;
    }

    /**
     * Visits a set a parallel stream.
     */
    public Object visitSetParallelStream(LIRSetParallelStream self,
				       JExpression streamContext,
				       JExpression childContext,
				       int position,
				       CType inputType,
				       CType outputType,
				       int inputSize,
				       int outputSize) {
	Utils.fail("Replacing visitor doesn't deal with LIR nodes yet");
	streamContext.accept(this);
	childContext.accept(this);
	return self;
    }
}

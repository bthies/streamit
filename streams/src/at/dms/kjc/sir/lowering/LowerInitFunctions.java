package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This class adds LIR hooks to the init functions.
 */
public class LowerInitFunctions implements StreamVisitor {

    private final Schedule schedule;

    /**
     * Construct one of these with schedule <schedule>
     */
    private LowerInitFunctions(Schedule schedule) {
	this.schedule = schedule;
    }

    /**
     * Lowers the init functions in <str>.
     */
    public static void lower(SIRStream str, Schedule schedule) {
	str.accept(new LowerInitFunctions(schedule));
    }

    //
    // PRIVATE METHODS ------------------------------------------------------
    //
    
    /**
     * Registers children of <self> with init function <init> and
     * children <children>
     */
    private void registerChildren(JMethodDeclaration init, 
				  List children) {
	int childCount = children.size()-1;
	// iterate in reverse order so they come out right
	ListIterator it = children.listIterator(); 
	// find the end of the list (to get it from an index you need
	// a linked list... is there a better way to iterate backwards?)
	for (; it.hasNext(); it.next());
	for (; it.hasPrevious(); childCount--) {
	    Object o = it.previous();
	    SIRStream stream = (SIRStream)o;

	    // register the child
	    init.addStatementFirst(new LIRSetChild(LoweringConstants.
						   getStreamContext(),
						   /* child type */
						   stream.getName(),
						   /* child name */
						   LoweringConstants.
						   getChildName(childCount)));
	}
    }
				 
    /**
     * Registers the tapes that connect the children of <str> in <init>
     */
    private void registerTapes(SIRStream str, 
			       JMethodDeclaration init) {
	// assume <str> is a pipeline
	Utils.assert(str instanceof SIRPipeline, "Can only do pipes now.");
	SIRPipeline pipe = (SIRPipeline)str;
	// if empty pipeline, return
	if (pipe.size()==0) {
	    return;
	}
	// go through children of <pipe>, keeping track of stream1 and
	// stream2, which have a tape between them
	SIRStream stream1, stream2;
	// get stream1
	stream1 = (SIRStream)pipe.get(0);
	// for all the pairs of children...
	for (int i=1; i<pipe.size(); i++) {
	    // get stream2
	    stream2 = (SIRStream)pipe.get(i);
	    // declare a tape from stream1 to stream2
	    init.addStatement(new LIRSetTape(LoweringConstants.
					     getStreamContext(),
					     /* stream struct 1 */
					     LoweringConstants.
					     getChildStruct(i-1),
					     /* stream struct 2 */
					     LoweringConstants.
					     getChildStruct(i), 
					     /* type on tape */
					     stream1.getOutputType(), 
					     /* size of buffer */
	   schedule.getBufferSizeBetween(stream1, stream2).intValue()
						  ));
	    // re-assign stream1 for next step
	    stream1 = stream2;
	}
    }

    /**
     * Lowers all the SIRInitStatements in <init>, given that the
     * corresponding structure is <str>, into function calls that the
     * LIR can recognize.  
     */
    private void lowerInitStatements(SIRStream str,
				     JMethodDeclaration init) {
	// go through statements, looking for SIRInitStatement
	List statements = init.getStatementList();
	for (int i=0; i<statements.size(); i++) {
	    Object o = statements.get(i);
	    if (o instanceof SIRInitStatement) {
		statements.set(i,
			       lowerInitStatement(str, (SIRInitStatement)o));
	    }
	}
    }

    /**
     * Lowers an SIRInitStatement and returns the result.
     */
    private JExpressionStatement
	lowerInitStatement(SIRStream str, SIRInitStatement initStatement) {
	// cast str to pipe
	Utils.assert(str instanceof SIRPipeline,
		     "Only support lowering of pipelines at this point.");
	SIRPipeline pipe = (SIRPipeline)str;
	// get args from <initStatement>
	JExpression[] args = initStatement.getArgs();
	// get target of initialization
	SIRStream target = initStatement.getTarget();
	
	// create the new argument--the reference to the child's state
	JExpression childState 
	    = LoweringConstants.getChildStruct(pipe.indexOf(target));
	// create new argument list
	JExpression[] newArgs = new JExpression[args.length + 1];
	// set new arg
	newArgs[0] = childState;
	// set rest of args
	for (int i=0; i<args.length; i++) {
	    newArgs[i+1] = args[i];
	}
	// return method call statement
	return new JExpressionStatement(
	      null,
	      new JMethodCallExpression(/* tokref */
					null,
					/* prefix */
					null,
					/* ident */
					LoweringConstants.getInitName(target),
					/* args */
					newArgs),
	      null);
    }
    //
    // VISITOR STUFF ------------------------------------------------------
    //
    
    /**
     * visit a filter 
     */
    public void visitFilter(SIRFilter self,
			    SIRStream parent,
			    JFieldDeclaration[] fields,
			    JMethodDeclaration[] methods,
			    JMethodDeclaration init,
			    int peek, int pop, int push,
			    JMethodDeclaration work,
			    CType inputType, CType outputType) {
	// set stream type to filter
	init.addStatement(new LIRSetStreamType(LoweringConstants.
					       getStreamContext(),
					       LIRStreamType.LIR_FILTER));
	// set push count
	init.addStatement(new LIRSetPush(LoweringConstants.
					 getStreamContext(),
					 push));
	// set peek count
	init.addStatement(new LIRSetPeek(LoweringConstants.
					 getStreamContext(),
					 push));
	// set pop count
	init.addStatement(new LIRSetPop(LoweringConstants.
					getStreamContext(),
					push));

	// set work function
	init.addStatement(new LIRSetWork(LoweringConstants.
					 getStreamContext(),
					 new LIRFunctionPointer(
					 LoweringConstants.
					 getWorkName(self))));
    }
  
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {

	// translate init statements to function calls with context
	lowerInitStatements(self, init);

	// add some things to the init function... these things are
	// added to beginning, so they're in reverse order

	// register children
	registerChildren(init, elements);

	// set work function, if there is one
	if (self.hasMethod(LoweringConstants.getWorkName(self))) {
	    init.addStatementFirst(new LIRSetWork(LoweringConstants.
						  getStreamContext(),
						  new LIRFunctionPointer(
						 LoweringConstants.
						 getWorkName(self))));
	}

	// set stream type to pipeline (at very beginning)
	init.addStatementFirst(new LIRSetStreamType(LoweringConstants.
						    getStreamContext(),
						    LIRStreamType.
						    LIR_PIPELINE));

	// register tapes between children (at very end)
	registerTapes(self, init);
	
    }

    /* visit a splitter */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      int[] weights) {
	Utils.fail("Only lower filters and pipelines for now.");
    }
    
    /* visit a joiner */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    int[] weights) {
	Utils.fail("Only lower filters and pipelines for now.");
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	Utils.fail("Only lower filters and pipelines for now.");
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	Utils.fail("Only lower filters and pipelines for now.");
    }

    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
	Utils.fail("Only lower filters and pipelines for now.");
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
	Utils.fail("Only lower filters and pipelines for now.");
    }
}


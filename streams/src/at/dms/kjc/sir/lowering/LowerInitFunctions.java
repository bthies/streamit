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
     * Registers some <children> with prologue <prologue> to be
     * inserted at the beginning of an init function.  Adds statements
     * to <prologue> that do the registering.
     */
    private void registerChildren(List children, List prologue) {
	// iterate in reverse order so they come out right
	for (ListIterator it = children.listIterator(); it.hasNext(); ) {
	    // extract child
	    SIROperator child = (SIROperator)it.next();
	    if (child instanceof SIRStream) {
		// register the stream
		prologue.add(new LIRSetChild(LoweringConstants.
					     getStreamContext(),
					     /* child type */
					     child.getName(),
					     /* child name */
					     child.getRelativeName()));
	    } else if (child instanceof SIRJoiner) {
		// get joiner
		SIRJoiner joiner = (SIRJoiner)child;
		// register joiner
		prologue.add(new LIRSetJoiner(LoweringConstants.
					      getStreamContext(),
					      joiner.getType(),
					      joiner.getWays(),
					      joiner.getWeights()));
	    } else if (child instanceof SIRSplitter) {
		// get splitter
		SIRSplitter splitter = (SIRSplitter)child;
		// register splitter
		prologue.add(new LIRSetSplitter(LoweringConstants.
						getStreamContext(),
						splitter.getType(),
						splitter.getWays(),
						splitter.getWeights()));
	    } else {
		Utils.fail("Unexpected child type: " + child.getClass());
	    }
	}
    }
				 
    /**
     * Registers the tapes in <tapePairs> with new statements in <method>.
     */
    private void registerTapes(List tapePairs, JMethodDeclaration init) {
	// go through tape pairs, declaring tapes...
	for (ListIterator it = tapePairs.listIterator(); it.hasNext(); ) {
	    // get the next pair
	    SIROperator[] pair = (SIROperator[])it.next();
	    // determine type of tape by finding a SIRStream.  We need
	    // to do this in case we're connecting a splitter or
	    // joiner, since the splitters and joiners don't know
	    // about their types.
	    Utils.assert(pair[0] instanceof SIRStream ||
			 pair[1] instanceof SIRStream,
			 "Two many-to-1 or 1-to-many operators connected? " +
			 "Can't determine type of tape in between");
	    CType tapeType;
	    // assign type to either output of source or input of
	    // sink, depending on which we can determine
	    if (pair[0] instanceof SIRStream) {
		tapeType = ((SIRStream)pair[0]).getOutputType();
	    } else {
		tapeType = ((SIRStream)pair[1]).getInputType();
	    }
	    // declare a tape from pair[0] to pair[1]
	    init.addStatement(new LIRSetTape(LoweringConstants.
					     getStreamContext(),
					     /* stream struct 1 */
					     LoweringConstants.
					     getChildStruct(pair[0]),
					     /* stream struct 2 */
					     LoweringConstants.
					     getChildStruct(pair[1]), 
					     /* type on tape */
					     tapeType,
					     /* size of buffer */
	   schedule.getBufferSizeBetween(pair[0], pair[1]).intValue()
					     ));
	}
    }

    /**
     * Registers tapes in FeedbackLoop <str> into <init> function.
     */
    private void registerFeedbackLoopTapes(SIRFeedbackLoop str, 
					   JMethodDeclaration init) {
	// work on BODY...
	SIRStream body = str.getBody();
	// get child context
	JExpression bodyContext = 
	    LoweringConstants.getStreamContext(LoweringConstants.
					       getChildStruct(body));
	// get the input tape size
	int bodyInputSize = schedule.getBufferSizeBetween(str.getJoiner(), 
							  body).intValue();
	// get the output tape size
	int bodyOutputSize = schedule.getBufferSizeBetween(body,
							   str.getSplitter() 
							   ).intValue();
	// register tape
	init.addStatement(new LIRSetBodyOfFeedback(bodyContext,
						   body.getInputType(),
						   body.getOutputType(),
						   bodyInputSize,
						   bodyOutputSize));
	// work on LOOP...
	SIRStream loop = str.getLoop();
	// get child context
	JExpression loopContext = 
	    LoweringConstants.getStreamContext(LoweringConstants.
					       getChildStruct(loop));
	// get the input tape size
	int loopInputSize = schedule.getBufferSizeBetween(str.getSplitter(), 
							  loop).intValue();
	// get the output tape size
	int loopOutputSize = schedule.getBufferSizeBetween(loop,
							   str.getJoiner() 
							   ).intValue();
	// register tape
	init.addStatement(new LIRSetLoopOfFeedback(loopContext,
						   loop.getInputType(),
						   loop.getOutputType(),
						   loopInputSize,
						   loopOutputSize));
    }

    /**
     * Registers tapes in SplitJoin <str> into <init> function.
     */
    private void registerSplitJoinTapes(SIRSplitJoin str, 
					JMethodDeclaration init) {
	// go through elements
	for (int i=0; i<str.size(); i++) {
	    // get i'th child
	    SIRStream child = str.get(i);
	    // get context for child
	    JExpression childContext = 
		LoweringConstants.getStreamContext(LoweringConstants.
						   getChildStruct(child
								  ));
	    // get the input tape size
	    int inputSize = schedule.getBufferSizeBetween(str.getSplitter(), 
							  child).intValue();
	    // get the output tape size
	    int outputSize = schedule.getBufferSizeBetween(child,
							   str.getJoiner() 
							   ).intValue();
	    // register an LIR node to <init>
	    init.addStatement(new LIRSetParallelStream(childContext,
						       i,
						       child.getInputType(),
						       child.getOutputType(),
						       inputSize,
						       outputSize));
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
	// get args from <initStatement>
	JExpression[] args = initStatement.getArgs();
	// get target of initialization
	SIRStream target = initStatement.getTarget();
	
	// create the new argument--the reference to the child's state
	JExpression childState 
	    = LoweringConstants.getChildStruct(target);
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

    /* visit a splitter */
    public void visitSplitter(SIRSplitter self,
			      SIRStream parent,
			      SIRSplitType type,
			      int[] weights) {
	// do nothing at a splitter
    }
    
    /* visit a joiner */
    public void visitJoiner(SIRJoiner self,
			    SIRStream parent,
			    SIRJoinType type,
			    int[] weights) {
	// do nothing at a joiner
    }

    /**
     * Visits an SIRContainer to lower its init function.
     */
    private void visitContainer(SIRContainer self,
				JMethodDeclaration init) {
	// translate init statements to function calls with context.
	// this is modifying <init> without adding/removing extra
	// stuff.
	lowerInitStatements(self, init);

	// now add some things to the init function... 

	// first, a prologue that comes before the original statements
	// in the init function.  It is a list of statements.
	List prologue = new LinkedList();

	// start building up the prologoue, in order...
	// first, set stream type to pipeline
	prologue.add(new LIRSetStreamType(LoweringConstants.
					  getStreamContext(),
					  self.getStreamType()));

	// set work function, if there is one
	if (self.getWork()!=null) {
	    prologue.add(new LIRSetWork(LoweringConstants.
					getStreamContext(),
					new LIRFunctionPointer(
					   self.getWork().getName())));
	}

	// register children
	registerChildren(self.getChildren(), prologue);

	// add the prologue to the init function
	init.addAllStatements(0, prologue);
    }
	
    /* pre-visit a pipeline */
    public void preVisitPipeline(SIRPipeline self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements) {
	// do standard container stuff
	visitContainer(self, init);
	// register tapes between children in init function
	registerTapes(self.getTapePairs(), init);
    }

    /* pre-visit a splitjoin */
    public void preVisitSplitJoin(SIRSplitJoin self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init) {
	// do standard container stuff
	visitContainer(self, init);
	// register tapes
	registerSplitJoinTapes(self, init);
    }

    /* pre-visit a feedbackloop */
    public void preVisitFeedbackLoop(SIRFeedbackLoop self,
				     SIRStream parent,
				     JFieldDeclaration[] fields,
				     JMethodDeclaration[] methods,
				     JMethodDeclaration init,
				     int delay,
				     JMethodDeclaration initPath) {
	// do standard container stuff
	visitContainer(self, init);
	// register tapes
	registerFeedbackLoopTapes(self, init);
    }

    /* post-visit a pipeline */
    public void postVisitPipeline(SIRPipeline self,
				  SIRStream parent,
				  JFieldDeclaration[] fields,
				  JMethodDeclaration[] methods,
				  JMethodDeclaration init,
				  List elements) {
	// do nothing -- all work is in preVisit
    }

    /* post-visit a splitjoin */
    public void postVisitSplitJoin(SIRSplitJoin self,
				   SIRStream parent,
				   JFieldDeclaration[] fields,
				   JMethodDeclaration[] methods,
				   JMethodDeclaration init) {
	// do nothing -- all work is in preVisit
    }

    /* post-visit a feedbackloop */
    public void postVisitFeedbackLoop(SIRFeedbackLoop self,
				      SIRStream parent,
				      JFieldDeclaration[] fields,
				      JMethodDeclaration[] methods,
				      JMethodDeclaration init,
				      int delay,
				      JMethodDeclaration initPath) {
	// do nothing -- all work is in preVisit
    }
}


package at.dms.kjc.sir.lowering;

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

    private LowerInitFunctions() {}

    /**
     * Lowers the init functions in <str>.
     */
    public static void lower(SIRStream str) {
	str.accept(new LowerInitFunctions());
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
	int childCount = 0;
	for (ListIterator it = children.listIterator(); 
	     it.hasNext(); 
	     childCount++) {
	    Object next = it.next();
	    if (next instanceof List) {
		Utils.fail("No support for hierarchical streams yet");
	    }
	    Utils.assert(next instanceof SIRFilter, 
			 "Can only lower filters for now, but got a " + 
			 next.getClass() + " instead.");
	    SIRFilter filter = (SIRFilter)next;

	    // register the child
	    init.addStatementFirst(new LIRSetChild(LoweringConstants.
						   getStreamContext(),
						   /* child type */
						   filter.getName(),
						   /* child name */
						   LoweringConstants.
						   getChildName(childCount),
						   /* child init func */
						   new LIRFunctionPointer(
						   filter.getInit())));
	}
    }
				 
    /**
     * Registers the tapes that connect the children of <str> in <init>
     */
    private void registerTapes(SIRStream str, 
			       JMethodDeclaration init) {
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
	JExpression childState = 
	    new JFieldAccessExpression(
                      null,
		      /* prefix */
		      new JNameExpression(null, 
					  null, 
					  LoweringConstants.STATE_PARAM_NAME),
		      /* ident */
		      LoweringConstants.getChildName(pipe.indexOf(target)));
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
	// register children
	registerChildren(init, elements);

	// register tapes between children
	registerTapes(self, init);
	
	// translate init statements to function calls with context
	lowerInitStatements(self, init);

	// set stream type to pipeline
	init.addStatement(new LIRSetStreamType(LoweringConstants.
					       getStreamContext(),
					       LIRStreamType.LIR_PIPELINE));

	// set work function, if there is one
	if (self.hasWorkFunction()) {
	    init.addStatement(new LIRSetWork(LoweringConstants.
					     getStreamContext(),
					     new LIRFunctionPointer(
						 LoweringConstants.
						 getWorkName(self))));
	}
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


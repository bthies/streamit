package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This builds a schedule for the stream and instantiates the schedule
 * in a series of calls to filter's work functions.
 */
public class SIRScheduler {

    /**
     * Does the scheduling, adding a work function corresponding to
     * <toplevel> to <flatClass>.
     */
    public static Schedule schedule(SIRStream toplevel, 
				    JClassDeclaration flatClass) {
	// get a representation of the stream structure
	SchedStream schedStream 
	    = (SchedStream)toplevel.accept(new SIRSchedBuilder());
	// make a scheduler
	Scheduler scheduler = new SimpleHierarchicalScheduler(schedStream);
	// compute a schedule
	Schedule schedule = (Schedule)scheduler.computeSchedule();
	// make work function implementing the schedule
	JMethodDeclaration work = buildToplevelWork(schedule, toplevel);
	// add <work> to <flatClass>
	flatClass.addMethod(work);
	// return schedule for future reference
	return schedule;
    }

    /**
     * Translates <schedule> into a work function for toplevel class <toplevel>
     */
    private static JMethodDeclaration buildToplevelWork(Schedule schedule,
							SIRStream toplevel) {
	JStatement[] statementList = buildAllFilterWork(toplevel, schedule);
	// make block for statements
	JBlock statementBlock = new JBlock(null, 
					   statementList,
					   null);
	// build parameter list--the context to import
	JFormalParameter[] parameters = {
	    new JFormalParameter(/* tokref */ 
				 null,
				 /* desc */ 
				 JLocalVariable.DES_PARAMETER,
				 /* type */
				 CClassType.lookup(toplevel.getName()),
				 /* name */
				 LoweringConstants.STATE_PARAM_NAME, 
				 /* isFinal */
				 false)
	};
	// build the method declaration to return
	JMethodDeclaration result = 
	    new JMethodDeclaration( /* tokref     */ null,
				    /* modifiers  */ at.dms.kjc.
				                     Constants.ACC_PUBLIC,
				    /* returntype */ CStdType.Void,
				    /* identifier */ LoweringConstants.
				                     getWorkName(toplevel),
				    /* parameters */ parameters,
				    /* exceptions */ CClassType.EMPTY,
				    /* body       */ statementBlock,
				    /* javadoc    */ null,
				    /* comments   */ null);
	// return result
	return result;
    }
    
    /**
     * Returns a list of statements corresponding to a sequence of
     * calls to the filter work functions in <toplevel>.
     */
    private static JStatement[] buildAllFilterWork(SIRStream toplevel,
						   Schedule schedule)
    {
	// build the statements for <work> ...
	List statementList = new LinkedList();
	// get the list of filters to execute
	List list = schedule.getSchedule();
	// for each filter...
	for (ListIterator it = list.listIterator(); it.hasNext(); ) {
	    Object next = it.next();
	    if (next instanceof List) {
		Utils.fail("Hierarchical schedules not implemented yet--\n\t" +
			  "would like to associate SIRPipeline w/ SchedPipe");
	    }
	    Utils.assert(next instanceof SIRFilter, 
			 "Can only schedule filters for now, but got a" +
			 next.getClass() + " instead.");
	    SIRFilter filter = (SIRFilter)next;
	    // get method call to work function of <filter>
	    JMethodCallExpression filterWorkExpr = 
		buildFilterWork(toplevel, filter);
	    // make a statement from the call expression
	    JExpressionStatement filterWorkStatement =
		new JExpressionStatement(null, filterWorkExpr, null);
	    // add call to filter work to our statement list
	    statementList.add(filterWorkStatement);
	}
	// return list
	return (JStatement[])statementList.toArray(new JStatement[0]);
    }

    /**
     * Within class <toplevel>, adds a call to the work function of
     * <filter> to <statementList>.
     */
    private static JMethodCallExpression buildFilterWork(SIRStream toplevel,
							 SIRFilter filter) {
	// for now, assume <toplevel> is a pipeline
	Utils.assert((toplevel instanceof SIRPipeline),
		     "Can only schedule SIRPipeline's for now.");
	SIRPipeline pipe = (SIRPipeline)toplevel;
	// build parameter to call as the child's context...
	// get field name for <filter>'s context
	String childName = 
	    LoweringConstants.getChildName(pipe.indexOf(filter));
	// build a reference to <childName> in context of <toplevel>.
	// This assumes that the current function will have the
	// current context named <data>, as a parameter.
	JFieldAccessExpression[] childContext =
	    {new JFieldAccessExpression(/* tokref */
				       null,
				       /* prefix */
				       new JNameExpression(/* tokref */ 
							   null,
							   /* ident */
							   LoweringConstants.
							   STATE_PARAM_NAME),
				       /* ident */
				       childName)};
	// construct call to work function of <filter>, with
	// <childContext> as the argument
	JMethodCallExpression filterWork = 
	    new JMethodCallExpression(/* tokref */
				      null,
				      /* prefix -- ok to be null? */
				      null,
				      /* ident */
				      LoweringConstants.getWorkName(filter),
				      /* args */
				      childContext);
	return filterWork;
    }
}

/**
 * This class builds a representation of the stream structure for the
 * scheduler to use.
 */
class SIRSchedBuilder implements AttributeStreamVisitor {

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
			      SIRStream parent,
			      JFieldDeclaration[] fields,
			      JMethodDeclaration[] methods,
			      JMethodDeclaration init,
			      int peek, int pop, int push,
			      JMethodDeclaration work,
			      CType inputType, CType outputType) {
	// represent the filter
	return new SchedFilter(self, push, pop, peek);
    }
  
    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
				SIRStream parent,
				JFieldDeclaration[] fields,
				JMethodDeclaration[] methods,
				JMethodDeclaration init,
				List elements) {
	// represent the pipeline
	SchedPipeline sp = new SchedPipeline(self);
	// add children to pipeline
	for (ListIterator it = elements.listIterator(); it.hasNext(); ) {
	    // get child
	    SIRStream child = (SIRStream)it.next();
	    // build scheduler representation of child
	    SchedStream schedChild = (SchedStream)child.accept(this);
	    // add child to pipe
	    sp.addChild(schedChild);
	}
	// return result
	return sp;
    }

    /* pre-visit a splitjoin */
    public Object visitSplitJoin(SIRSplitJoin self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init) {
	Utils.fail("Not scheduling split-joins yet.");
	return null;
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
				    SIRStream parent,
				    JFieldDeclaration[] fields,
				    JMethodDeclaration[] methods,
				    JMethodDeclaration init,
				    int delay,
				    JMethodDeclaration initPath) {
	Utils.fail("Not scheduling feedback loops yet.");
	return null;
    }

    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
				SIRStream parent,
				SIRSplitType type,
				int[] weights) {
	Utils.fail("Not scheduling splitters yet.");
	return null;
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
			      SIRStream parent,
			      SIRJoinType type,
			      int[] weights)  {
	Utils.fail("Not scheduling joiners yet.");
	return null;
    }
}

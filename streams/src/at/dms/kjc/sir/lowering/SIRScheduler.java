package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;

import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This builds a schedule for the stream and instantiates the schedule
 * in a series of calls to filter's work functions.
 */
public class SIRScheduler {

    /**
     * Maps Lists returned by the scheduler to work functions that can
     * be called.
     */
    private HashMap listToWork;

    /**
     * The destination flatClass that we're working on.
     */
    private JClassDeclaration flatClass;

    /**
     * Creates one of these.
     */
    private SIRScheduler(JClassDeclaration flatClass) {
	this.listToWork = new HashMap();
	this.flatClass = flatClass;
    }

    /**
     * Does the scheduling, adding a work function corresponding to
     * <toplevel> to <flatClass>.
     */
    public static Schedule schedule(SIRStream toplevel, 
				    JClassDeclaration flatClass) {
	return new SIRScheduler(flatClass).schedule(toplevel);
    }

    /**
     * The private, instance-wise version of <schedule>, to do the
     * scheduling.
     */
    private Schedule schedule(SIRStream toplevel) {
	// make a scheduler
	Scheduler scheduler = new SimpleHierarchicalScheduler();
	// get a representation of the stream structure
	SchedStream schedStream 
	    = (SchedStream)toplevel.accept(new SIRSchedBuilder(scheduler));
	// set it to compute the schedule of <schedStream>
	scheduler.useStream(schedStream);
	// compute a schedule
	Schedule schedule = (Schedule)scheduler.computeSchedule();
	// make work function implementing the schedule
	JMethodDeclaration work = makeWork(schedule.getSchedule(), 
					   toplevel);
	// add <work> to <toplevel>
	toplevel.addMethod(work);
	// return schedule for future reference
	return schedule;
    }

    /**
     * Returns a work function for the scheduling object
     * <schedObject>, which must be either a List (corresponding to a
     * hierarchical scheduling unit) or an SIRFilter (corresponding to
     * the base case, in which a filter's work function should be executed.)
     */
    private JMethodDeclaration makeWork(Object schedObject,
					SIRStream toplevel) {
	// see what kind of schedObject we have
	if (schedObject instanceof List) {
	    // if we have a list, process as hierarhical unit...
	    List list = (List)schedObject;
	    // see if we've already built a work function for <list>
	    if (listToWork.containsKey(list)) {
		// if so, return the work function
		return (JMethodDeclaration)listToWork.get(list);
	    } else {
		// otherwise, compute the work function
		JMethodDeclaration work = makeWorkForList(list, toplevel);
		// store the new work function in a few places...
		// first, in listToWork so we can get it next time
		listToWork.put(list, work);
		// second, in the flatClass
		flatClass.addMethod(work);
		// return work
		return work;
	    }
	} else if (schedObject instanceof SIRFilter) {
	    // if we have a filter, just return filter's work function
	    return ((SIRFilter)schedObject).getWork();
	} else {
	    // othwerise, fail
	    Utils.fail("SIRScheduler expected List or SIRFilter, but found " + 
		       schedObject.getClass());
	    // return value doesn't matter
	    return null;
	}
    }
    
    /**
     * Given that <list> contains a set of scheduling objects under
     * toplevel stream <toplevel>, returns a work function
     * corresponding to <list>.  
     */
    private JMethodDeclaration makeWorkForList(List list, 
					       SIRStream toplevel) {
	JStatement[] statementList = makeWorkStatements(list, toplevel);
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
				                     getAnonWorkName(),
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
     * calls to the work functions corresponding to the elements of
     * <list> with top-level stream <toplevel>
     */
    private JStatement[] makeWorkStatements(List list, 
					    SIRStream toplevel) {
	// build the statements for <work> ...
	List statementList = new LinkedList();
	// for each filter...
	for (ListIterator it = list.listIterator(); it.hasNext(); ) {
	    Object next = it.next();
	    // get work function associated with <next>
	    JMethodDeclaration work = makeWork(next, toplevel);
	    // build the parameter to the work function, depending on
	    // whether we're calling a filter work function or a
	    // hierarchical list-work function
	    JExpression[] arguments
		= {makeWorkArgument(next, toplevel)} ;
	    // get method call to work function of <filter>
	    JMethodCallExpression workExpr = 
		new JMethodCallExpression(/* tokref */
					  null,
					  /* prefix -- ok to be null? */
					  null,
					  /* ident */
					  work.getName(),
					  /* args */
					  arguments);
	    // make a statement from the call expression
	    JExpressionStatement workStatement =
		new JExpressionStatement(null, workExpr, null);
	    // add call to filter work to our statement list
	    statementList.add(workStatement);
	}
	// return list
	return (JStatement[])statementList.toArray(new JStatement[0]);
    }

    /**
     * Returns the proper argument to the work function for <schedObject>
     * given toplevel stream <toplevel>.
     */
    private JExpression makeWorkArgument(Object schedObject,
					 SIRStream toplevel) {
	if (schedObject instanceof List) {
	    // make arg for list -- just the current context
	    return LoweringConstants.getDataField();
	} else if (schedObject instanceof SIRFilter) {
	    // make arg for filter
	    return makeFilterWorkArgument((SIRFilter)schedObject, 
					  toplevel);
	} else {
	    // otherwise, fail
	    Utils.fail("SIRScheduler expected List or SIRFilter but found" +
		       schedObject.getClass());
	    // return dummy value
	    return null;
	}
    }

    /**
     * Returns an expression that returns the data structure
     * corresponding to <filter>, tracing pointers from the data
     * structure for <toplevel>.
     */
    private JExpression makeFilterWorkArgument(SIRFilter filter,
					       SIRStream toplevel) {

	// get parents of <filter>
	SIRStream parents[] = filter.getParents();

	// construct result expression
	JExpression result = LoweringConstants.getDataField();

	// go through parents from top to bottom, building up the
	// field access expression.
	for (int i=parents.length-1; i>=0; i--) {
	  if (parents[i] instanceof SIRPipeline) {
	    SIRPipeline pipe = (SIRPipeline)parents[i];
	    // get the child of interest (either the next parent,
	    // or <filter>
	    SIRStream child = (i>0 ? parents[i-1] : filter);
	    // get field name for child context
	    String childName = 
	      LoweringConstants.getChildName(pipe.indexOf(child));
	    // build up cascaded field reference
	    result = new JFieldAccessExpression(/* tokref */
						null,
						/* prefix is previous ref*/
						result,
						/* ident */
						childName);
	  } else {
	    Utils.fail("Only pipelines supported now.");
	  }
	}

	return result;
    }
}

/**
 * This class builds a representation of the stream structure for the
 * scheduler to use.
 */
class SIRSchedBuilder implements AttributeStreamVisitor {

    /**
     * The scheduler that dictates construction of the representation
     * of the stream graph.
     */
    private Scheduler scheduler;

    /**
     * Construct one of these with scheduler <scheduler>
     */
    public SIRSchedBuilder(Scheduler scheduler) {
	this.scheduler = scheduler;
    }

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
	return scheduler.newSchedFilter(self, push, pop, peek);
    }
  
    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
				SIRStream parent,
				JFieldDeclaration[] fields,
				JMethodDeclaration[] methods,
				JMethodDeclaration init,
				List elements) {
	// represent the pipeline
	SchedPipeline sp = scheduler.newSchedPipeline(self);
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

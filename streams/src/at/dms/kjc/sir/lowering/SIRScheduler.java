package at.dms.kjc.sir.lowering;

import streamit.scheduler.*;
import streamit.scheduler.simple.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * This builds a schedule for the stream and instantiates the schedule
 * in a series of calls to filter's work functions.
 */
public class SIRScheduler implements Constants {

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
	Scheduler scheduler = new SimpleHierarchicalSchedulerPow2();
	// get a representation of the stream structure
	SchedStream schedStream 
	    = (SchedStream)toplevel.accept(new SIRSchedBuilder(scheduler));
	// set it to compute the schedule of <schedStream>
	scheduler.useStream(schedStream);
	// debugging output
	scheduler.print(System.out);
	// compute a schedule
	Schedule schedule = (Schedule)scheduler.computeSchedule();
	// debugging output
	//printSchedule(schedule.getSteadySchedule(), "steady state ");
	//printSchedule(schedule.getInitSchedule(), "initialization ");
	// get toplevel schedule
	Object schedObject = schedule.getSteadySchedule();
	// make work function implementing the steady-state schedule
	JMethodDeclaration steadyWork = makeWork(pruneRep(schedule.
							  getSteadySchedule()),
						 toplevel);
	// set <work> as the work function of <toplevel>
	toplevel.setWork(steadyWork);
	// make an initialization schedule
	schedObject = schedule.getInitSchedule(); 
	Utils.assert(schedObject!=null, 
		     "Got a null init. schedule from the scheduling library");
	JMethodDeclaration initWork = makeWork(pruneRep(schedObject), 
					       toplevel);
	// make the main function in <flatClass>, containing call to <initWork>
	addMainFunction(toplevel, initWork);
	// return schedule for future reference
	return schedule;
    }

    /**
     * If <schedObject> is a SchedRepSchedule, then just take its
     * contents until we don't have a SchedRepSchedule.
     */
    private Object pruneRep(Object schedObject) {
	while (schedObject instanceof SchedRepSchedule) {
	    schedObject =
		((SchedRepSchedule)schedObject).getOriginalSchedule();
	}
	return schedObject;
    }

    /**
     * Prints a schedule to the screen, with prefix label <label>
     */
    private void printSchedule(Object schedObject, String label) {
	// print top-level label
	System.out.println("---------------------------------");
	System.out.println(label + "schedule: ");
	// print the schedule
	printSchedule(schedObject, 1);
    }

    /**
     * Prints a sub-schedule with indentation <tabs>.
     */
    private void printSchedule(Object schedObject, int tabs) {
	// print indentation
	for (int i=0; i<tabs; i++) {
	    System.out.print("  ");
	}
	// 
	if (schedObject instanceof List) {
	    // print out a list
	    System.out.println("List " + schedObject.hashCode() + ":");
	    for (ListIterator it = ((List)schedObject).listIterator();
		 it.hasNext(); ) {
		printSchedule(it.next(), tabs+1);
	    }
	} else {
	    // print out an SIR component
	    System.out.println(((SIROperator)schedObject).getName());
	}
    }

    /**
     * Adds a main function to <flatClass>, with the information
     * necessary to call the toplevel init function in <toplevel> and
     * the init schedule that's executed in <initWork>
     */
    private void addMainFunction(SIRStream toplevel, 
				 JMethodDeclaration initWork) {
	// make a call to <initWork> from within the main function
	LinkedList statementList = new LinkedList();
	statementList.add(makeWorkCall(toplevel, 
				       initWork.getName(),
				       toplevel));

	// construct LIR node
	LIRMainFunction[] main 
	    = {new LIRMainFunction(toplevel.getName(),
				   new LIRFunctionPointer(toplevel.
							  getInit().
							  getName()),
				   statementList)};
	JBlock mainBlock = new JBlock(null, main, null);

	// add a method to <flatClass>
	flatClass.addMethod(
		new JMethodDeclaration( /* tokref     */ null,
				    /* modifiers  */ at.dms.kjc.
				                     Constants.ACC_PUBLIC,
				    /* returntype */ CStdType.Void,
				    /* identifier */ "main",
				    /* parameters */ JFormalParameter.EMPTY,
				    /* exceptions */ CClassType.EMPTY,
				    /* body       */ mainBlock,
				    /* javadoc    */ null,
				    /* comments   */ null));
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
	} else if (schedObject==null) {
	    // fail
	    Utils.fail("SIRScheduler expected List or SIRFilter but found " +
		       schedObject);
	    // return value doesn't matter
	    return null;
	} else {
	    // otherwise, fail
	    Utils.fail("SIRScheduler expected List or SIRFilter but found " +
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
     * Returns a list of JStatements corresponding to a sequence of
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
	    // make a statement that does some work (could be for loop
	    // or function call)
	    JStatement workStatement = makeWorkStatement(next, toplevel);
	    // add work statement to list
	    statementList.add(workStatement);
	}
	// return list
	return (JStatement[])statementList.toArray(new JStatement[0]);
    }

    /**
     * Makes a statement that does some work as a component of a list
     * in the schedule.  This statement is either a for loop or a call
     * to another work function.
     */
    private JStatement makeWorkStatement(Object schedObject, 
					 SIRStream toplevel) {
	// see if we have a repeated scheduled object
	if (schedObject instanceof SchedRepSchedule) {
	    // get the repeated schedule
	    SchedRepSchedule repSched = (SchedRepSchedule)schedObject;
	    // get body of the for loop
	    JStatement body = makeWorkStatement(repSched.
						getOriginalSchedule(),
						toplevel);
	    // get the count for the for loop
	    int count = repSched.getTotalExecutions().intValue();
	    // make a for loop
	    return makeForLoop(body, count);
	} else {
	    // otherwise, get name of work function associated with
	    // <schedObject>
	    String workName = getWorkName(schedObject, toplevel);
	    // make the work statement
	    return makeWorkCall(schedObject, workName, toplevel);
	}
    }

    /**
     * Returns a block with a loop counter declaration and a for loop
     * that executes <contents> for <count> number of times.  If the
     * count is just one, then return the body instead of a loop.
     */
    private JStatement makeForLoop(JStatement body, int count) {
	// if the count is one, then just return the body
	if (count==1) {
	    return body;
	}
	// define a variable to be our loop counter
	JVariableDefinition var = 
	    new JVariableDefinition(/* where */ null,
				    /* modifiers */ 0,
				    /* type */ CStdType.Integer,
				    /* ident */ 
				    LoweringConstants.getUniqueVarName(),
				    /* initializer */
				    new JIntLiteral(0));
	// make a declaration statement for our new variable
	JVariableDeclarationStatement varDecl =
	    new JVariableDeclarationStatement(null, var, null);
	// make a test if our variable is less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      OPE_LT,
				      new JLocalVariableExpression(null, var),
				      new JIntLiteral(count));
	// make an increment for <var>
	JStatement incr = 
	    new JExpressionStatement(null,
				     new JPostfixExpression(null,
							    OPE_POSTINC,
			       new JLocalVariableExpression(null, var)),
				     null);
	// make the for statement
	JStatement forStatement = 
	    new JForStatement(/* tokref */ null,
			      /* init */ new JEmptyStatement(null, null),
			      cond,
			      incr,
			      body,
			      /* comments */ null);
	// return the block
	JStatement[] statements = {varDecl, forStatement};
	return new JBlock(null, statements, null);
    }

    /**
     * Generates a statement that calls the work function for <schedObject>.
     */
    private JStatement makeWorkCall(Object schedObject, 
				    String workName,
				    SIRStream toplevel) {
	// build the parameter to the work function, depending on
	// whether we're calling a filter work function or a
	// hierarchical list-work function
	JExpression[] arguments
	    = {makeWorkArgument(schedObject, toplevel)} ;
	// get method call to work function of <filter>
	JMethodCallExpression workExpr = 
	    new JMethodCallExpression(/* tokref */
				      null,
				      /* prefix -- ok to be null? */
				      null,
				      /* ident */
				      workName,
				      /* args */
				      arguments);
	// make a statement from the call expression
	return new JExpressionStatement(null, workExpr, null);
    }

    /**
     * Given a <schedObject> and its toplevel container <toplevel>,
     * return the name of the work function associated with
     * <schedObject>.  If <schedObject> is a filter or list, this will
     * involve making a work function; otherwise we can refer to
     * constant names for splitters/joiners.
     */
    private String getWorkName(Object schedObject, SIRStream toplevel) {
	// if <next> is splitter or joiner, then work is constant
	if (schedObject instanceof SIRSplitter) {
	    // if splitter, take splitter name
	    return LoweringConstants.SPLITTER_WORK_NAME;
	} else if (schedObject instanceof SIRJoiner) {
	    // if joiner, take joiner name
	    return LoweringConstants.JOINER_WORK_NAME;
	} else if (schedObject instanceof SIRFileReader) {
	    // if file reader, take file reader name
	    return LoweringConstants.FILE_READER_WORK_NAME;
	} else if (schedObject instanceof SIRFileWriter) {
	    // if file reader, take file reader name
	    return LoweringConstants.FILE_WRITER_WORK_NAME;
	} else {
	    // otherwise, have a list or filter--need to make a work function
	    JMethodDeclaration work = makeWork(schedObject, toplevel);
	    // return name of work function
	    return work.getName();
	}
    }


    /**
     * Returns the proper argument to the work function for <schedObject>
     * given toplevel stream <toplevel>.
     */
    private JExpression makeWorkArgument(Object schedObject,
					 SIRStream toplevel) {
	if (schedObject instanceof List || schedObject instanceof SIRPipeline) {
	    // make arg for list -- just the current context.  the SIRPipeline
	    // case is when we're scheduling the toplevel init function; an
	    // SIRPipeline shouldn't appear in the schedule from the scheduler
	    return LoweringConstants.getDataField();
	} else if (schedObject instanceof SIRFilter) {
	    // make arg for filter node
	    return makeFilterWorkArgument((SIRFilter)schedObject);
	} else if (schedObject instanceof SIRSplitter ||
		   schedObject instanceof SIRJoiner ) {
	    // make arg for splitter/joiner node 
	    return makeSplitJoinWorkArgument((SIROperator)schedObject);
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
     * */
    private JExpression makeFilterWorkArgument(SIRFilter str) {
	// get access to structure of <str>'s parent
	JExpression parent = str.getParentStructureAccess();

	// return reference to <str> off of parent
	return new JFieldAccessExpression(/* tokref */
					  null,
					  /* prefix is previous ref*/
					  parent,
					  /* ident */
					  str.getRelativeName());
    }

    /**
     * Returns an expression that returns the data structure
     * corresponding to <str>, tracing pointers from the data
     * structure for <toplevel>.  <str> is either an SIRSplitter or
     * and SIRJoiner.
     *  
     */
    private JExpression makeSplitJoinWorkArgument(SIROperator str) {
	// get access to structure of <str>'s parent
	JExpression parent = str.getParentStructureAccess();

	// return reference to context of parent
	return LoweringConstants.getStreamContext(parent);
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
			      JMethodDeclaration work,
			      CType inputType, CType outputType) {
	// represent the filter
	return scheduler.newSchedFilter(self, 
					self.getPushInt(), 
					self.getPopInt(),
					self.getPeekInt());
    }
  
    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
				SIRStream parent,
				JFieldDeclaration[] fields,
				JMethodDeclaration[] methods,
				JMethodDeclaration init,
				List elements) {
	// represent the pipeline
	SchedPipeline result = scheduler.newSchedPipeline(self);
	// add children to pipeline
	for (ListIterator it = elements.listIterator(); it.hasNext(); ) {
	    // get child
	    SIRStream child = (SIRStream)it.next();
	    // build scheduler representation of child
	    SchedStream schedChild = (SchedStream)child.accept(this);
	    // add child to pipe
	    result.addChild(schedChild);
	}
	// return result
	return result;
    }

    /* pre-visit a splitjoin */
    public Object visitSplitJoin(SIRSplitJoin self,
				 SIRStream parent,
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 List elements,
				 SIRSplitter splitter,
				 SIRJoiner joiner) {
	// represent the pipeline
	SchedSplitJoin result = scheduler.newSchedSplitJoin(self);
	// add children to pipeline
	for (ListIterator it = elements.listIterator(); it.hasNext(); ) {
	    // get child
	    SIRStream child = (SIRStream)it.next();
	    // build scheduler representation of child
	    SchedStream schedChild = (SchedStream)child.accept(this);
	    // add child to pipe
	    result.addChild(schedChild);
	}
	// set split type
	result.setSplitType((SchedSplitType)splitter.accept(this));
	// set join type
	result.setJoinType((SchedJoinType)joiner.accept(this));
	// return result
	return result;
    }

    /* pre-visit a feedbackloop */
    public Object visitFeedbackLoop(SIRFeedbackLoop self,
				    SIRStream parent,
				    JFieldDeclaration[] fields,
				    JMethodDeclaration[] methods,
				    JMethodDeclaration init,
				    int delay,
				    JMethodDeclaration initPath) {
	// get joiner
	SchedJoinType joiner = (SchedJoinType)self.getJoiner().accept(this);
	// get body
	SchedStream body = (SchedStream)self.getBody().accept(this);
	// get splitter
	SchedSplitType splitter 
	    = (SchedSplitType)self.getSplitter().accept(this);
	// get loop
	SchedStream loop = (SchedStream)self.getLoop().accept(this);

	// represent the whole feedback loop
	SchedLoop result = scheduler.newSchedLoop(self,
						  joiner,
						  body,
						  splitter,
						  loop,
						  delay);
	// return result
	return result;
    }

    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
				SIRStream parent,
				SIRSplitType type,
				JExpression[] weights) {
	// represent the splitter
	return scheduler.newSchedSplitType(type.toSchedType(), 
					   Utils.intArrayToList(self.
								getWeights()), 
					   self);
    }
    
    /* visit a joiner */
    public Object visitJoiner(SIRJoiner self,
			      SIRStream parent,
			      SIRJoinType type,
			      JExpression[] weights)  {
	// represent the joiner
	return scheduler.newSchedJoinType(type.toSchedType(), 
					  Utils.intArrayToList(self.
							       getWeights()),
					  self);
    }
}

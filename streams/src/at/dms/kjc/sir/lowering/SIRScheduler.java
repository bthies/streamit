package at.dms.kjc.sir.lowering;

import streamit.scheduler2.iriter.Iterator;
import streamit.scheduler2.Scheduler;
import streamit.scheduler2.Schedule;
import streamit.library.StreamIt;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.kjc.sir.lowering.partition.PartitionDot;

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
     * The toplevel stream we're dealing with.
     */
    private SIRContainer toplevel;

    /**
     * The destination flatClass that we're working on.
     */
    private JClassDeclaration flatClass;

    /**
     * Whether or not we're working on the initial schedule.  This is
     * true when we're building the initial work function, false when
     * we're building the steady-state work function.
     */
    private boolean initMode;    

    /**
     * Maps schedules (phases) to work functions.  A given phase could
     * appear multiple times (as sub-schedules of other schedules), so
     * we want to only have one work function generated.
     */
    private HashMap scheduleToWork;

    /**
     * Creates one of these.
     */
    private SIRScheduler(SIRContainer toplevel, JClassDeclaration flatClass) {
	this.toplevel = toplevel;
	this.flatClass = flatClass;
	this.scheduleToWork = new HashMap();
    }

    /**
     * Does the scheduling, adding a work function corresponding to
     * <toplevel> to <flatClass>.  Returns the schedule built as part
     * of this.  
     *
     * For now, requires a container as input to simplify
     * implementation - if there's a pre-defined filter at toplevel,
     * then would have to make wrapper to call it here, which is not
     * entirely obvious how to do (considering the data structures
     * being passed around.)
     */
    public static SIRSchedule buildWorkFunctions(SIRContainer toplevel, 
						 JClassDeclaration flatClass) {
	Scheduler scheduler = new SIRScheduler(toplevel, flatClass).buildWorkFunctions();
	return new SIRSchedule(toplevel, scheduler);
    }

    /**
     * Returns a two-dimensional array HashMap's that map each
     * splitter, joiner, & filter in <str> to a 1-dimensional int
     * array containing the count for how many times that siroperator
     * executes:
     *
     *  result[0] = map for initializaiton schedule
     *  result[1] = map for steady-state schedule
     */ 
    public static HashMap[] getExecutionCounts(SIRStream str) {
	return getExecutionCounts(str, computeSchedule(str));
    }
    /**
     * Internal routine for getting execution counts given a scheduler.
     */
    private static HashMap[] getExecutionCounts(SIRStream str, Scheduler scheduler) {
	// make the result
	HashMap[] result = { new HashMap(), new HashMap() } ;

	// fill in the init schedule
	fillExecutionCounts(scheduler.getOptimizedInitSchedule(), result[0], 1);
	// fill in the steady-state schedule
	fillExecutionCounts(scheduler.getOptimizedSteadySchedule(), result[1], 1);

	checkSchedule(str, scheduler, result);

	// debug
	//printSchedules(schedule);
	//printSchedulesViaLibrary(schedule);
	//printExecutionCounts(result);

	return result;
    }

    /*
     * Try making buffer sizes just to see if there's an exception.
     * This is to detect an invalid schedule without depending on the
     * compiler.
     */
    private static int numSchedErrors = 0;
    private static void checkSchedule(SIRStream str, Scheduler scheduler, HashMap[] execCounts) {
	// this only works if <str> is stand-alone -- that is, it
	// starts with a source and ends with a sink
	if (str.getInputType()!=CStdType.Void || str.getOutputType()!=CStdType.Void) {
	    return;
	}
	try {
	    scheduler.computeBufferUse();
	} catch (Exception e) {
	    String filename = "bad-schedule-" + (++numSchedErrors) + ".dot";
	    PartitionDot.printScheduleGraph(str, filename, execCounts);
	    System.err.println("\n" + 
			       "WARNING:  Scheduler throws exception trying to compute buffer sizes.\n" +
			       "  We don't need the buffer sizes, but this could indicate an invalid schedule.\n" +
			       "  The bad schedule is in " + filename + ".  The stack trace is as follows:");
	    e.printStackTrace();
	    System.err.println();
	}
    }

    private static void printExecutionCounts(HashMap[] result) {
	for (int i=0; i<2; i++) {
	    System.err.println(i==0 ? "Initial counts:" : "Steady counts:");
	    for (java.util.Iterator it=result[i].keySet().iterator(); it.hasNext(); ) {
		SIROperator op = (SIROperator)it.next();
		int[] count = (int[])result[i].get(op);
		System.err.println("  " + op.getName() + ": " + (count==null ? "empty" : "" + count[0]));
	    }
	}
    }

    // Creates execution counts of filters in graph.  Requires that
    // <schedule> results from a schedule built with this instance of
    // the scheduler.
    private static void fillExecutionCounts(Schedule schedule, HashMap counts, int numReps) {
	if (schedule.isBottomSchedule()) {
	    // tally up for this node.
	    SIROperator str = getTarget(schedule);
	    if (!counts.containsKey(str)) {
		// initialize counter
		int[] wrapper = { numReps };
		counts.put(str, wrapper);
	    } else {
		// add to counter
		int[] wrapper = (int[])counts.get(str);
		wrapper[0] += numReps;
	    }	    
	} else {
	    // otherwise we have a container, so simulate execution of
	    // children
	    for (int i=0; i<schedule.getNumPhases(); i++) {
		fillExecutionCounts(schedule.getSubSched(i), counts, numReps * schedule.getSubSchedNumExecs(i));
	    }
	}
    }

    /**
     * The private, instance-wise version of <schedule>, to do the
     * scheduling.
     */
    private Scheduler buildWorkFunctions() {
	// get the schedule
	Scheduler scheduler = computeSchedule(this.toplevel);
	// debugging printing
	System.err.print("got schedule, interpreting... ");
	// print schedule to dot graph
	PartitionDot.printScheduleGraph(this.toplevel, "schedule.dot", getExecutionCounts(this.toplevel, scheduler));
	// do steady-state scheduling
	scheduleSteady(scheduler);
	// do initial scheduling
	scheduleInit(scheduler);
	// return schedule
	return scheduler;
    }
    
    /**
     * Interface with the scheduler to get a schedule for <str>.
     */
    private static Scheduler computeSchedule(SIRStream str) {
	streamit.scheduler2.Scheduler scheduler = new streamit.scheduler2.minlatency.Scheduler(IterFactory.createFactory().createIter(str));
	scheduler.computeSchedule();
	//System.err.println("Back from scheduler package.");
	return scheduler;
    }

    /**
     * Given the schedule <schedule>, do the steady-state scheduling
     * for <this.toplevel>.
     */
    private void scheduleSteady(Scheduler scheduler) {
	// indicate we're working on steady schedule
	this.initMode = false;

	// get steady schedule
	Schedule schedule = scheduler.getOptimizedSteadySchedule();
	// make the steady work
	JMethodDeclaration steadyWork = makeHierWork(schedule);
	// set <steadyWork> as the work function of <toplevel>
	this.toplevel.setWork(steadyWork);
    }
    
    /**
     * Given the schedule <schedule>, do the initial scheduling for
     * <this.toplevel>.
     */
    private void scheduleInit(Scheduler scheduler) {
	// indicate we're working on init schedule
	this.initMode = true;

	// get init schedule
	Schedule schedule = scheduler.getOptimizedInitSchedule();

	// make the init work
	JMethodDeclaration initWork = makeHierWork(schedule);
	// make the main function in <flatClass>, containing call to <initWork>
	addMainFunction(initWork);
    }

    /**
     * Adds a main function to <flatClass>, with the information
     * necessary to call the toplevel init function in <this.toplevel>
     * and the init schedule that's executed in <initWork>
     */
    private void addMainFunction(JMethodDeclaration initWork) {
	// make a call to <initWork> from within the main function
	LinkedList statementList = new LinkedList();
	statementList.add(makeWorkCall(this.toplevel, 
				       initWork.getName(),
				       false));

	// construct LIR node
	LIRMainFunction[] main 
	    = {new LIRMainFunction(this.toplevel.getName(),
				   new LIRFunctionPointer(this.toplevel.
							  getInit().getName()),
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
     * Calls the library's mechanism for printing a schedule.  Should
     * give the same results as the other printSchedules if everything
     * is sane.
     */
    private static void printSchedulesViaLibrary(Scheduler scheduler) {
	StreamIt s = new StreamIt ();
	System.out.println ("\nInit schedule:");
	s.computeSize(scheduler.getOptimizedInitSchedule(), true);
	System.out.println("\nSteady schedule:");
	s.computeSize(scheduler.getOptimizedSteadySchedule(), true);
	System.out.println();
    }

    /**
     * Prints initial and steady-state schedules of <schedule>
     */
    private static void printSchedules(Scheduler scheduler) {
	System.err.println();
	System.err.println("Init schedule:");
	printSchedule(scheduler.getOptimizedInitSchedule(), 1, 1);
	System.err.println("Steady schedule:");
	printSchedule(scheduler.getOptimizedSteadySchedule(), 1, 1);
    }

    /**
     * Prints a <schedule> with indentation <tabs>.
     */
    private static void printSchedule(Schedule schedule, int tabs, int numExecs) {
	// print indentation
	for (int i=0; i<tabs; i++) {
	    System.err.print("  ");
	}
	if (schedule.isBottomSchedule()) {
	    /* This is true, except if we're printing schedules during fusion's mangling.
	    if (getTarget(schedule) instanceof SIRFilter) {
		Utils.assert(((SIRStream)getTarget(schedule)).getWork()==schedule.getWorkFunc());
	    }
	    */
	    System.err.println("Repeat " + numExecs + ": " + getTarget(schedule).getName() + " (BOTTOM)");
	} else {
	    System.err.println("Repeat " + numExecs +
			       ":  (" + schedule.getNumPhases() + " child" + (schedule.getNumPhases()!=1 ? "ren)" : ")"));
	    for (int i=0; i<schedule.getNumPhases(); i++) {
		printSchedule(schedule.getSubSched(i), tabs+1, schedule.getSubSchedNumExecs(i));
	    }
	}
    }

    /**
     * Returns name of work function for <schedule>.
     */
    private String getWorkName(Schedule schedule) {
	if (schedule.isBottomSchedule()) {
	    // special case some kinds of filters... 
	    SIROperator str = getTarget(schedule);
	    if (str instanceof SIRFileReader) {
		return LoweringConstants.FILE_READER_WORK_NAME;
	    } else if (str instanceof SIRFileWriter) {
		return LoweringConstants.FILE_WRITER_WORK_NAME;
	    } else if (str instanceof SIRIdentity) {
		return LoweringConstants.IDENTITY_WORK_NAME;
	    }

	    // otherwise, return the work function name
	    return ((JMethodDeclaration)schedule.getWorkFunc()).getName();
	} else {
	    // otherwise, we have hierarchical schedule; call components
	    JMethodDeclaration work = makeHierWork(schedule);
	    // return work
	    return work.getName();
	}
    }

    /**
     * Makes work function for hierarchical <schedule>.
     */
    private JMethodDeclaration makeHierWork(Schedule schedule) {
	// see if we've already made a work function for this schedule
	if (scheduleToWork.containsKey(schedule)) {
	    return (JMethodDeclaration)scheduleToWork.get(schedule);
	}
	// otherwise, build a work function from scratch...
	JStatement[] statementList = makeWorkStatements(schedule);
	// make block for statements
	JBlock statementBlock = new JBlock(null, statementList, null);
	// build parameter list--the context to import
	JFormalParameter[] parameters = {
	    new JFormalParameter(/* tokref */ 
				 null,
				 /* desc */ 
				 JLocalVariable.DES_PARAMETER,
				 /* type */
				 CClassType.lookup(this.toplevel.getName()),
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
	// store work function in flat class
	flatClass.addMethod(result);
	// remember that this schedule is represented by this work function
	scheduleToWork.put(schedule, result);
	// return result
	return result;
    }

    /**
     * Returns a list of JStatements corresponding to a sequence of
     * calls to the work functions in <schedule>.
     */
    private JStatement[] makeWorkStatements(Schedule schedule) {
	// build the statements for <work> ...
	List statementList = new LinkedList();
	// for each phase of <schedule>
	for (int i=0; i<schedule.getNumPhases(); i++) {
	    // make work statement for sub-schedule
	    JStatement workStatement = makeWorkStatement(schedule.getSubSched(i), schedule.getSubSchedNumExecs(i));
	    // add work statement to list
	    statementList.add(workStatement);
	}
	// return list
	return (JStatement[])statementList.toArray(new JStatement[0]);
    }

    /**
     * Makes a statement that does some work as a phase in the
     * schedule.  This statement is either a for loop or a call to
     * another work function.
     */
    private JStatement makeWorkStatement(Schedule schedule, int numExecs) {
	/*
	System.err.print("making work name for call to " + getTarget(schedule).getName());
	if (schedule.isBottomSchedule()) {
	    System.err.println(" (BOTTOM) ");
	} else {
	    System.err.println(" (" + schedule.getNumPhases() + " children) ");
	}
	*/
	String workName = getWorkName(schedule);
	/*
	System.err.println("got work name " + workName);
	*/

	JStatement workCall = makeWorkCall(getTarget(schedule), 
					   workName,
					   schedule.isBottomSchedule());
	
	if (numExecs>1) {
	    // if we execute multiple times, return a loop
	    return Utils.makeForLoop(workCall, numExecs);
	} else {
	    // otherwise, return single call
	    return workCall;
	}
    }

    /**
     * Returns the stream operator corresponding to <schedule>.
     */
    private static SIROperator getTarget(Schedule schedule) {
	// make function call for sub-schedule
	SIRStream str = ((SIRIterator)schedule.getStream()).getStream();
	// if it's a filter or not bottom schedule, can return
	if (str instanceof SIRFilter ||
            str instanceof SIRPhasedFilter ||
	    !schedule.isBottomSchedule()) {
	    return str;
	}
	// otherwise, we have a bottom schedule that isn't a filter.
	// This must be a splitter or joiner with <str> as its
	// container.
	Utils.assert(str instanceof SIRSplitJoin || str instanceof SIRFeedbackLoop);
	// test for splitter
	if (schedule.getWorkFunc()==SIRSplitter.WORK_FUNCTION) {
	    if (str instanceof SIRSplitJoin) {
		return ((SIRSplitJoin)str).getSplitter();
	    } else {
		return ((SIRFeedbackLoop)str).getSplitter();
	    }
	}
	// test for joiner
	if (schedule.getWorkFunc()==SIRJoiner.WORK_FUNCTION) {
	    if (str instanceof SIRSplitJoin) {
		return ((SIRSplitJoin)str).getJoiner();
	    } else {
		return ((SIRFeedbackLoop)str).getJoiner();
	    }
	}
	Utils.fail("Didn't find target of schedule.");
	return null;
    }

    /**
     * Generates a statement that calls the work function for <schedule>.
     */
    private JStatement makeWorkCall(SIROperator workOp,
				    String workName, 
				    boolean isBottom) {
	// build the parameter to the work function, depending on
	// whether we're calling a filter work function or a
	// hierarchical list-work function
	JExpression[] arguments
	    = {makeWorkArgument(workOp, isBottom)} ;
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
     * Returns the proper argument to the work function for <op> given
     * toplevel stream <this.toplevel>.
     */
    private JExpression makeWorkArgument(SIROperator op, boolean isBottom) {
	if (isBottom) {
	    // if we're at the bottom, then dereference into specific
	    // type of receiver
	    if (op instanceof SIRFilter || op instanceof SIRPhasedFilter) {
		// make arg for filter node
		return makeFilterWorkArgument(op);
	    } else if (op instanceof SIRJoiner || op instanceof SIRSplitter) {
		return makeSplitJoinWorkArgument(op);
	    } else {
		Utils.fail("Unexpected operator type: " + op.getClass());
		return null;
	    }
	} else {
	    // otherwise, just pass on the data structure
	    return LoweringConstants.getDataField();
	}
    }

    /**
     * Returns an expression that returns the data opucture
     * corresponding to <filter>.
     * */
    private JExpression makeFilterWorkArgument(SIROperator str) {
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
     * corresponding to <str>.  <str> is either an SIRSplitter or and
     * SIRJoiner.
     *  
     */
    private JExpression makeSplitJoinWorkArgument(SIROperator str) {
	// get access to structure of <str>'s parent
	JExpression parent = str.getParentStructureAccess();

	// return reference to context of parent
	return LoweringConstants.getStreamContext(parent);
    }
}

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
     * SIRFilter -> SIRTwoStageFilter.
     *
     * Each SIRTwoStage filter is represented as a pipeline to two
     * dummy filters for the sake of the scheduler.  twoStageFilters
     * maps the first dummy filter to the TwoStageFilter that it is
     * representing.  This is used to interpret the results from the
     * scheduler--any list starting with something as a key in this
     * map can be replaced with the corresponding value in the map.
     */
    protected HashMap twoStageFilters;

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
     * Creates one of these.
     */
    private SIRScheduler(JClassDeclaration flatClass) {
	this.flatClass = flatClass;
	this.twoStageFilters = new HashMap();
    }

    /**
     * Does the scheduling, adding a work function corresponding to
     * <toplevel> to <flatClass>.  Returns the schedule built as part
     * of this.
     */
    public static Schedule buildWorkFunctions(SIRStream toplevel, 
					      JClassDeclaration flatClass) {
	return new SIRScheduler(flatClass).buildWorkFunctions(toplevel);
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
	// make the result
	HashMap[] result = { new HashMap(), new HashMap() } ;

	// get the schedule
	SIRScheduler scheduler = new SIRScheduler(null);
	Schedule schedule = scheduler.computeSchedule(str);
	
	// fill in the init schedule
	scheduler.fillExecutionCounts(schedule.getInitSchedule(),
				      result[0]);

	// fill in the steady-state schedule
	scheduler.fillExecutionCounts(schedule.getSteadySchedule(), 
				      result[1]);

	return result;
    }

    // Creates execution counts of filters in graph.  Requires that
    // <schedObject> results from a schedule built with this instance
    // of the scheduler.
    private void fillExecutionCounts(Object schedObject, HashMap counts) {
	if (schedObject instanceof List) {
	    // first see if we have a two-stage filter
	    SIRTwoStageFilter twoStage = 
		getTwoStageFilter((List)schedObject);
	    // if we found a two-stage filter, recurse on it instead
	    // of the list elements.
	    if (twoStage!=null) {
		fillExecutionCounts(twoStage, counts);
	    } else {
		// otherwise, visit all of the elements of the list
		for (ListIterator it = ((List)schedObject).listIterator();
		     it.hasNext(); ) {
		    fillExecutionCounts(it.next(), counts);
		}
	    }
	} else if (schedObject instanceof SchedRepSchedule) {
    	    // get the schedRep
	    SchedRepSchedule rep = (SchedRepSchedule)schedObject;
	    for(int i = 0; i < rep.getTotalExecutions().intValue(); i++)
		fillExecutionCounts(rep.getOriginalSchedule(), 
				    counts);
	} else {
	    //add one to the count for this node
	    if (!counts.containsKey(schedObject)) {
		int[] wrapper = { 1 };
		counts.put(schedObject, wrapper);
	    } else {
		//add one to counter
		int[] wrapper = (int[])counts.get(schedObject);
		wrapper[0]++;
	    }
	}
    }

    /**
     * The private, instance-wise version of <schedule>, to do the
     * scheduling.
     */
    private Schedule buildWorkFunctions(SIRStream toplevel) {
	// get the schedule
	Schedule schedule = computeSchedule(toplevel);
	// do steady-state scheduling
	scheduleSteady(toplevel, schedule);
	// do initial scheduling
	scheduleInit(toplevel, schedule);
	// return schedule
	return schedule;
    }
    
    /**
     * Interface with the scheduler to get a schedule for <toplevel>.
     * Records all associations of two-stage filters in <twoStageFilters>.
     */
    private Schedule computeSchedule(SIRStream toplevel) {
	// make a scheduler
	Scheduler scheduler = new SimpleHierarchicalSchedulerPow2();
	// get a representation of the stream structure
	SchedStream schedStream 
	    = (SchedStream)toplevel.accept(new SIRSchedBuilder(scheduler,
							       this));
	// set it to compute the schedule of <schedStream>
	scheduler.useStream(schedStream);
	// debugging output
	//scheduler.print(System.out);
	// compute a schedule
	Schedule result = (Schedule)scheduler.computeSchedule();
	// debugging output
	//printSchedule(result.getSteadySchedule(), "sirscheduler steady state");
	//printSchedule(result.getInitSchedule(), "sirscheduler initialization");
	// return schedule
	return result;
    }

    /**
     * Given the schedule <schedule>, do the steady-state scheduling
     * for <toplevel>.
     */
    private void scheduleSteady(SIRStream toplevel, Schedule schedule) {
	// clear listToWork
	listToWork = new HashMap();
	// indicate we're working on steady schedule
	this.initMode = false;

	// get steady schedule
	Object schedObject = schedule.getSteadySchedule();
	// make the steady work
	JMethodDeclaration steadyWork = makeWork(pruneRep(schedObject),
						 toplevel);	
	// set <steadyWork> as the work function of <toplevel>
	toplevel.setWork(steadyWork);
    }
    
    /**
     * Given the schedule <schedule>, do the initial scheduling for
     * <toplevel>.
     */
    private void scheduleInit(SIRStream toplevel, Schedule schedule) {
	// clear listToWork
	listToWork = new HashMap();
	// indicate we're working on init schedule
	this.initMode = true;

	List schedObject = new LinkedList();
	schedObject.add(schedule.getInitSchedule());
	// make the init work
	JMethodDeclaration initWork = makeWork(pruneRep(schedObject),
					       toplevel);
	// make the main function in <flatClass>, containing call to <initWork>
	addMainFunction(toplevel, initWork);
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
    public static void printSchedule(Object schedObject, String label) {
	// print top-level label
	System.out.println("---------------------------------");
	System.out.println(label + " schedule: ");
	// print the schedule
	printSchedule(schedObject, 1);
    }

    /**
     * Prints a sub-schedule with indentation <tabs>.
     */
    private static void printSchedule(Object schedObject, int tabs) {
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
	} else if (schedObject instanceof SchedRepSchedule) {
	    // get the schedRep
	    SchedRepSchedule rep = (SchedRepSchedule)schedObject;
	    // print stuff
	    System.out.print("Rep x " + rep.getTotalExecutions() + ":");
	    printSchedule(rep.getOriginalSchedule(), tabs+1);
	} else if (schedObject instanceof SIRFilter) {
	    // print name and ident
	    System.out.println(((SIRFilter)schedObject).getName() + " / " +
			       ((SIRFilter)schedObject).getIdent() + 
			       " pop=" + ((SIRFilter)schedObject).getPopInt() + 
			       " peek=" + ((SIRFilter)schedObject).getPeekInt() +
			       " push=" + ((SIRFilter)schedObject).getPushInt());
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
     * Returns the two-stage filter that is represented by
     * <schedObject>, or <null> if <schedObject> does not represent a
     * two-stage filter.
     */
    protected SIRTwoStageFilter getTwoStageFilter(List schedObject) {
	// see if the first element in the list corresponds to a
	// two-stage filter.
	if (schedObject.size()>0 && 
	    twoStageFilters.containsKey(pruneRep(schedObject.get(0)))) {
	    // if so, return it
	    return (SIRTwoStageFilter)
		twoStageFilters.get(pruneRep(schedObject.get(0)));
	} else {
	    // otherwise, return null
	    return null;
	}
    }

    /**
     * If <schedObject> contains two dummy filters representing a
     * single TwoStageFilter, then use the hashmap <twoStageFilters>
     * to retrieve the two-staged filter associated with this list,
     * and return the work function that should be used for the next
     * invocation of the two-staged filter.  Note that this method is
     * stateful--the first time it finds a given two-stage filter, it
     * will return the initWork function.  On subsequent calls, it
     * will return the work function.
     *
     * If <schedObject> does not represent a two-stage filter, this
     * returns null.
     */
    private JMethodDeclaration getTwoStageWork(List schedObject) {
	// see if we have a list corresponding to a two-stage filter
	SIRTwoStageFilter filter = getTwoStageFilter(schedObject);
	if (filter!=null) {
	    // return the init or steady-state work function depending
	    // on how long the list is.  If the list is just one
	    // element, then we must be doing init; otherwise steady.
	    if (schedObject.size()==1) {
		return filter.getInitWork();
	    } else {
		Utils.assert(schedObject.size()==2,
			     "Unexpected size of " + schedObject.size() + 
			     " for size of list simulating two-stage filter");
		return filter.getWork();
	    }
	}
	// otherwise, not a two-stage filter; return null
	return null;
    }

    /**
     * Returns a work function for the scheduling object
     * <schedObject>, which must be either a List (corresponding to a
     * hierarchical scheduling unit) or an SIRFilter (corresponding to
     * the base case, in which a filter's work function should be
     * executed.)
     */
    private JMethodDeclaration makeWork(Object schedObject,
					SIRStream toplevel) {
	// see what kind of schedObject we have
	if (schedObject instanceof List) {
	    // get the list
	    List list = (List)schedObject;
	    // first see if this list represents a two-stage filter.
	    JMethodDeclaration work = getTwoStageWork(list);
	    if (work != null) {
		// if so, use its work function.
		return work;
	    }
	    // othwerwise, process <list> as a hierarchical unit...
	    // see if we've already built a work function for <list>
	    /* LEAVE THIS OUT FOR NOW -- REUSE BREAKS INITWORK. */
	    if (listToWork.containsKey(list)) {
		// if so, return the work function
		return (JMethodDeclaration)listToWork.get(list);
	    } else {
		// otherwise, compute the work function
		work = makeWorkForList(list, toplevel);
		// store the new work function in a few places...
		// first, in listToWork so we can get it next time
		listToWork.put(list, work);
		// second, in the flatClass
		flatClass.addMethod(work);
		// return work
		return work;
	    }
	} else if (schedObject instanceof SIRFilter) {
	    // if we have a filter, return the filter's work function
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
	    // return a for loop
	    return Utils.makeForLoop(body, count);
	} else {
	    // otherwise, get name of work function associated with
	    // <schedObject>
	    String workName = getWorkName(schedObject, toplevel);
	    // make the work statement
	    return makeWorkCall(schedObject, workName, toplevel);
	}
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
        } else if (schedObject instanceof SIRIdentity) {
            // if identity, take identity name
            return LoweringConstants.IDENTITY_WORK_NAME;
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
	if (schedObject instanceof SIRPipeline) {
	    // the SIRPipeline case is when we're scheduling the
	    // toplevel init function; an SIRPipeline shouldn't appear
	    // in the schedule from the scheduler
	    return LoweringConstants.getDataField();
	} else if (schedObject instanceof List) {
	    // arg for list is just the current context, unless we
	    // have a two-stage filter disguised as a list, in which
	    // case we treat it as a filter.
	    SIRTwoStageFilter filter = getTwoStageFilter((List)schedObject);
	    if (filter!=null) {
		return makeFilterWorkArgument(filter);
	    } else {
		return LoweringConstants.getDataField();
	    }
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
     * The parent SIRScheduler calling this.
     */
    private SIRScheduler sirScheduler;

    /**
     * Construct one of these with scheduler <scheduler>, and keep
     * track of associations of two-stage filters in twoStageFilters.
     */
    public SIRSchedBuilder(Scheduler scheduler, SIRScheduler sirScheduler) {
	this.scheduler = scheduler;
	this.sirScheduler = sirScheduler;
    }

    /**
     * Builds a representation of a two-stage filter that the
     * scheduler can understand: a composition of two normal filters
     * in a pipeline.
     */
    private Object visitTwoStageFilter(SIRTwoStageFilter twoStage) {
	// make two dummy filters to represent <filter>
	SIRFilter filter1 = new SIRFilter("dummy1 for " + twoStage.getName());
	SIRFilter filter2 = new SIRFilter("dummy2 for " + twoStage.getName());
	// associate <twoStage> with <filter1> in <twoStageFilters>
	sirScheduler.twoStageFilters.put(filter1, twoStage);
	// set the peek, pop, and push rates for filter1 and filter2
	// according to derived formulas.
	filter1.setPeek(twoStage.getInitPeek() - twoStage.getInitPop() + 1);
	filter1.setPop(1);
	filter1.setPush(1);
	// now for filter2
	filter2.setPeek(twoStage.getInitPop() + twoStage.getPopInt());
	filter2.setPop(twoStage.getPopInt());
	filter2.setPush(twoStage.getPushInt());
	// if we have a source, munge the filters so that it outputs
	// the source granularity
	if (twoStage.getInitPeek()== 0 &&
	    twoStage.getPeekInt() == 0) {
	    /*
	    System.err.println("found twostage source with: " + 
			       "\n  initpeek=" + twoStage.getInitPeek() +
			       "\n  initpop=" + twoStage.getInitPop() +
			       "\n  initpush=" + twoStage.getInitPush() +
			       "\n  peek=" + twoStage.getPeekInt() +
			       "\n  pop=" + twoStage.getPopInt() +
			       "\n  push=" + twoStage.getPushInt());
	    */
	    filter1.setPop(0);
	    filter1.setPeek(0);
	    filter1.setPush(1);
	    filter2.setPop(1);
	    filter2.setPeek(2);
	}
	// make a pipeline containing <filter1> and <filter2>
	SIRPipeline pipe = new SIRPipeline(twoStage.getParent(), 
					   null, null, null);
	pipe.add(filter1);
	pipe.add(filter2);
	// visit the pipeline that's simulating <twoStage>
	return pipe.accept(this);
    }
    
    /* visit a structure */
    public Object visitStructure(SIRStructure self,
                                 JFieldDeclaration[] fields)
    {
        // Do nothing; the scheduler shouldn't care about structures
        return null;
    }

    /* visit a filter */
    public Object visitFilter(SIRFilter self,
			      JFieldDeclaration[] fields,
			      JMethodDeclaration[] methods,
			      JMethodDeclaration init,
			      JMethodDeclaration work,
			      CType inputType, CType outputType) {
	// represent the filter
	if (self instanceof SIRTwoStageFilter) {
	    return visitTwoStageFilter((SIRTwoStageFilter)self);
	} else {
	    return scheduler.newSchedFilter(self, 
					    self.getPushInt(), 
					    self.getPopInt(),
					    self.getPeekInt());
	}
    }
  
    /* pre-visit a pipeline */
    public Object visitPipeline(SIRPipeline self,
				JFieldDeclaration[] fields,
				JMethodDeclaration[] methods,
				JMethodDeclaration init) {
	// this isn't pretty, but the sched handle should be the fused
	// filter if this pipeline is representing a fused filter.
	// Othwerwise, the handle can just be <self>.
	SIRStream fusedFilter = 
	    sirScheduler.getTwoStageFilter(self.getChildren());
	SchedPipeline result = 
	    scheduler.newSchedPipeline(fusedFilter!=null ? fusedFilter : self);
	// add children to pipeline
	for (ListIterator it = self.getChildren().listIterator(); it.hasNext(); ) {
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
				 JFieldDeclaration[] fields,
				 JMethodDeclaration[] methods,
				 JMethodDeclaration init,
				 SIRSplitter splitter,
				 SIRJoiner joiner) {
	// represent the pipeline
	SchedSplitJoin result = scheduler.newSchedSplitJoin(self);
	// add children to pipeline
	for (ListIterator it = self.getParallelStreams().listIterator(); it.hasNext(); ) {
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
				    JFieldDeclaration[] fields,
				    JMethodDeclaration[] methods,
				    JMethodDeclaration init,
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
						  self.getDelayInt());
	// return result
	return result;
    }

    /* visit a splitter */
    public Object visitSplitter(SIRSplitter self,
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
			      SIRJoinType type,
			      JExpression[] weights)  {
	// represent the joiner
	return scheduler.newSchedJoinType(type.toSchedType(), 
					  Utils.intArrayToList(self.
							       getWeights()),
					  self);
    }
}

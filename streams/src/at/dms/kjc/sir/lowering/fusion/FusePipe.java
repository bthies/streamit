package at.dms.kjc.sir.lowering.fusion;

import streamit.scheduler.*;
import streamit.scheduler.simple.*;

import at.dms.util.IRPrinter;
import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.kjc.lir.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

public class FusePipe {

    /**
     * This is the name of the field that is used to hold the items
     * that are peeked.
     */
    private static final String PEEK_BUFFER_NAME = "___PEEK_BUFFER";

    /**
     * This is the name of the field that is used to hold the items
     * that are popped / looked at.
     */
    private static final String POP_BUFFER_NAME = "___POP_BUFFER";

    /**
     * The name of the counter that is used to write into buffers.
     */
    private static final String PUSH_INDEX_NAME = "___PUSH_INDEX";

    /**
     * The name of the counter that is used to read from buffers.
     */
    private static final String POP_INDEX_NAME = "___POP_INDEX";

    /**
     * The name of the counter that is used to count executions of a phase.
     */
    private static final String COUNTER_NAME = "___COUNTER";

    /**
     * The name of the initial work function.
     */
    public static final String INIT_WORK_NAME = "___initWork";

    /**
     * Prefix for name of parameters in fused init function.
     */
    protected static final String INIT_PARAM_NAME = "___param";

    /**
     * Fuses all candidate portions of <pipe>.  Candidates for fusion
     * are sequences of filters that do not have special,
     * compiler-defined work functions.
     */
    public static void fuse(SIRPipeline pipe) {
	int start = 0;
	do {
	    // find start of candidate stretch for fusion
	    while (start < pipe.size()-1 && !isFusable(pipe.get(start))) {
		start++;
	    }
	    // find end of candidate stretch for fusion
	    int end = start;
	    while ((end+1) < pipe.size() && isFusable(pipe.get(end+1))) {
		end++;
	    }
	    // if we found anything to fuse
	    if (end > start) {
		fuse((SIRFilter)pipe.get(start),
		     (SIRFilter)pipe.get(end));
		System.err.println("Fusing " + (end-start+1) + " Pipeline filters!");
	    }
	    start = end + 1;
	} while (start < pipe.size()-1);
    }

    /**
     * Returns whether or note <str> is a candidate component for
     * fusion.  For now, <str> must be a filter with a work function
     * in order for us to fuse it.
     */
    private static boolean isFusable(SIRStream str) {
	return (str instanceof SIRFilter) && ((SIRFilter)str).needsWork();
    }
	
    /**
     * Fuses filters <first> ... <last>.  For now, assumes: 
     *
     * 1. all of <first> ... <last> are consecutive filters in their
     *     parent, which must be an SIRPipeline
     *
     */
    public static void fuse(SIRFilter first,
			    SIRFilter last) {
	SIRPipeline parent = (SIRPipeline)first.getParent();
	// make a list of the filters to be fused
	List filterList = parent.getChildrenBetween(first, last);
	// fuse the filters
	SIRFilter fused = fuse(filterList);
	// insert the fused filter in the parent
	replace(parent, fused, filterList);
    }

    /**
     * In <parent>, replace <filterList> with <fused>
     */
    private static void replace(SIRPipeline parent, 
				final SIRFilter fused,
				final List filterList) {
	// have to get the first and last list items this way since we
	// only know it's a list. 
	final SIRStream first = (SIRStream)filterList.get(0);
	SIRStream last = (SIRStream)filterList.get(filterList.size()-1);
	// replace <filterList> with <fused>
	parent.replace(first, last, fused);

	// replace the SIRInitStatements in the parent
	parent.getInit().accept(new SLIRReplacingVisitor() {
		public Object visitInitStatement(SIRInitStatement oldSelf,
						 JExpression[] oldArgs,
						 SIRStream oldTarget) {
		    // do the super
		    SIRInitStatement self = 
			(SIRInitStatement)
			super.visitInitStatement(oldSelf, oldArgs, oldTarget);
		    
		    // if we're the first filter, change target to be
		    // <fused>.  Otherwise, if we're in one of the
		    // fused filters, replace with an empty statement
		    if (self.getTarget()==first) {
			self.setTarget(fused);
			return self;
		    } else if (filterList.contains(self.getTarget())) {
			// if we're another filter, remove the init statement
			return new JEmptyStatement(null, null);
		    } else {
			// otherwise, return self
			return self;
		    }
		}
	    });
    }
				
    /*
     * Returns a fused filter that has same behavior as all of
     * <filters>.
     */
    private static SIRFilter fuse(List filters) {
	// construct a dummy result to be filled in later.  This is
	// necessary because in the process of patching the parent
	// init function, we need to know about the new target.  Not a
	// perfect solution.
	SIRFilter result = new SIRTwoStageFilter();

	// rename the components of the filters
	RenameAll renamer = new RenameAll();
	for (ListIterator it=filters.listIterator(); it.hasNext(); ) {
	    renamer.renameFilterContents((SIRFilter)it.next());
	}

	// construct set of filter info
	List filterInfo = makeFilterInfo(filters);

	// make the initial work function
	JMethodDeclaration initWork =  makeWork(filterInfo, true);

	// make the steady-state work function
	JMethodDeclaration steadyWork =  makeWork(filterInfo, false);

	// make the fused init functions
	JMethodDeclaration init = makeInitFunction(filterInfo, 
						   initWork, 
						   result);

	// fuse all other fields and methods
	makeFused(filterInfo, init, initWork, steadyWork, result);
	
	// return result
	return result;
    }

    /**
     * Tabulates info on <filterList> that is needed for fusion.
     */
    private static List makeFilterInfo(List filterList) {
	// make the result
	List result = new LinkedList();
	// construct a schedule for <filterList>
	Schedule schedule = getSchedule(filterList);
	// get the schedules
	List initSched = schedule.getInitSchedule();
	List steadySched = schedule.getSteadySchedule();

	// DEBUGGING OUTPUT
	SIRScheduler.printSchedule(initSched, "initialization");
	SIRScheduler.printSchedule(steadySched, "steady state");

	// for each filter...
	ListIterator it = filterList.listIterator();
	for (int i=0; it.hasNext(); i++) {
	    // the filter
	    SIRFilter filter = (SIRFilter)it.next();

	    // the peek buffer
	    JVariableDefinition peekBufferVar = 
		new JVariableDefinition(null,
					at.dms.kjc.Constants.ACC_FINAL,
					new CArrayType(voidToInt(filter.
						       getInputType()), 
						       1 /* dimension */ ),
					PEEK_BUFFER_NAME + "_" + i,
					null);
	    JFieldDeclaration peekBuffer = new JFieldDeclaration(null,
								 peekBufferVar,
								 null,
								 null);
	    
	    // number of executions (num[0] is init, num[1] is steady)
	    int[] num = new int[2];
	    // for now, guard against empty/incomplete schedules
	    // by assuming a count of zero.  Talk to Michal about
	    // have entries of zero-weight in schedule?  FIXME.
	    if (initSched.size()>i) {
		num[0] = ((SchedRepSchedule)initSched.get(i)).
		    getTotalExecutions().intValue();
	    } else {
		num[0] = 0;
	    }
	    // do the same for the steady schedule
	    if (steadySched.size()>i) {
		num[1] = ((SchedRepSchedule)steadySched.get(i)).
		    getTotalExecutions().intValue();
	    } else {
		num[1] = 0;
	    }
	    
	    // get ready to make rest of phase-specific info
	    JVariableDefinition popBuffer[] = new JVariableDefinition[2];
	    JVariableDefinition popCounter[] = new JVariableDefinition[2];
	    JVariableDefinition pushCounter[] = new JVariableDefinition[2];
	    JVariableDefinition loopCounter[] = new JVariableDefinition[2];
	    
	    for (int j=0; j<2; j++) {
		// the pop buffer
		popBuffer[j] = makePopBuffer(filter, num[j], i);

		// the pop counter.
		popCounter[j] = 
		    new JVariableDefinition(null, 0, CStdType.Integer,
					    POP_INDEX_NAME + "_" + j + "_" + i,
					    new
					    JIntLiteral(filter.getPeekInt() - filter.getPopInt()
							//							- 1
							- 1 /* this is since we're starting
							       at -1 and doing pre-inc 
							       instead of post-inc */ ));
	    
		// the push counter.  In the steady state, the initial
		// value of the push counter is the first slot after
		// the peek values are restored, which is peek-pop-1
		// (-1 since we're pre-incing, not post-incing).  In
		// the inital work function, the push counter starts
		// at -1 (since we're pre-incing, not post-incing).
		int pushInit = 
		    j==0 ? -1 : filter.getPeekInt() - filter.getPopInt() - 1;
		pushCounter[j] = 
		    new JVariableDefinition(null, 0, CStdType.Integer,
					    PUSH_INDEX_NAME + "_" + j + "_" +i,
					    new JIntLiteral(pushInit));

		// the exec counter
		loopCounter[j] = 
		    new JVariableDefinition(null, 0, CStdType.Integer,
					    COUNTER_NAME + "_" + j + "_" +i,
					    null);
	    }

	    // add a filter info to <result>
	    result.add(new 
		       FilterInfo(filter, peekBuffer, 
				  new PhaseInfo(num[0], 
						popBuffer[0], 
						popCounter[0], 
						pushCounter[0],
						loopCounter[0]),
				  new PhaseInfo(num[1], 
						popBuffer[1],
						popCounter[1], 
						pushCounter[1],
						loopCounter[1])
				  ));
	}
	// return result
	return result;
    }
	
    /**
     * Interfaces with the scheduler to return a schedule for
     * <filterList> in <parent>.
     */
    private static Schedule getSchedule(List filterList) {
	// make a scheduler
	Scheduler scheduler = new SimpleHierarchicalScheduler();
	// make a dummy parent object as a hook for the scheduler
	Object parent = new Object();
	// ask the scheduler to schedule the list
	SchedPipeline sp = scheduler.newSchedPipeline(parent);
	// add the filters to the parent
	for (ListIterator it = filterList.listIterator(); it.hasNext(); ) {
	    SIRFilter filter = (SIRFilter)it.next();
	    sp.addChild(scheduler.newSchedFilter(filter, 
						 filter.getPushInt(),
						 filter.getPopInt(),
						 filter.getPeekInt()));
	}
	// tell the scheduler we're interested in <parent>
	scheduler.useStream(sp);
	// return the schedule
	return scheduler.computeSchedule();
    }

    /**
     * Returns a JVariableDefinition for a pop buffer for <filter>
     * that executes <num> times in its schedule and appears in the
     * <pos>'th position of its pipeline.
     */
    private static JVariableDefinition makePopBuffer(SIRFilter filter, 
						     int num,
						     int pos) {
	// get the number of items looked at in an execution round
	int lookedAt = num * filter.getPopInt() + 
	    filter.getPeekInt() - filter.getPopInt();
	// make an initializer to make a buffer of extent <lookedAt>
	JExpression[] dims = { new JIntLiteral(null, lookedAt) };
	JExpression initializer = 
	    new JNewArrayExpression(null,
				    voidToInt(filter.getInputType()),
				    dims,
				    null);
	// make a buffer for all the items looked at in a round
	return new JVariableDefinition(null,
				       at.dms.kjc.Constants.ACC_FINAL,
				       new CArrayType(voidToInt(filter.
						      getInputType()), 
						      1 /* dimension */ ),
				       POP_BUFFER_NAME + "_" + pos,
				       initializer);
    }

    /**
     * Builds the initial work function for <filterList>, where <init>
     * indicates whether or not we're doing the initialization work
     * function.
     */
    private static JMethodDeclaration makeWork(List filterInfo, boolean init) {
	// make a statement list for the init function
	JBlock statements = new JBlock(null, new JStatement[0], null);

	// add the variable declarations
	makeWorkDecls(filterInfo, statements, init);

	// add the work statements
	makeWorkBody(filterInfo, statements, init);

	// return result
	return new JMethodDeclaration(null,
				      at.dms.kjc.Constants.ACC_PUBLIC,
				      CStdType.Void,
				      init ? INIT_WORK_NAME : "work",
				      JFormalParameter.EMPTY,
				      CClassType.EMPTY,
				      statements,
				      null,
				      null);
    }

    /**
     * Adds local variable declarations to <statements> that are
     * needed by <filterInfo>.  If <init> is true, it does it for init
     * phase; otherwise for steady phase.
     */
    private static void makeWorkDecls(List filterInfo,
				      JBlock statements,
				      boolean init) {
	// add declarations for each filter
	for (ListIterator it = filterInfo.listIterator(); it.hasNext(); ) {
	    FilterInfo info = (FilterInfo)it.next();
	    // get list of local variable definitions from <filterInfo>
	    List locals = 
		init ? info.init.getVariables() : info.steady.getVariables();
	    // go through locals, adding variable declaration
	    for (ListIterator loc = locals.listIterator(); loc.hasNext(); ) {
		// get local
		JVariableDefinition local = 
		    (JVariableDefinition)loc.next();
		// add variable declaration for local
		statements.
		    addStatement(new JVariableDeclarationStatement(null, 
								   local, 
								   null));
	    }
	}
    }

    /**
     * Adds the body of the initial work function.  <init> indicates
     * whether or not this is the initial run of the work function
     * instead of the steady-state version.
     */
    private static void makeWorkBody(List filterInfo, 
				     JBlock statements,
				     boolean init) {
	// for all the filters...
	for (int i=0; i<filterInfo.size(); i++) {
	    FilterInfo cur = (FilterInfo)filterInfo.get(i);
	    PhaseInfo curPhase = init ? cur.init : cur.steady;
	    // we'll only need the "next" fields if we're not at the
	    // end of a pipe.
	    FilterInfo next = null;
	    PhaseInfo nextPhase = null;
	    // get the next fields
	    if (i<filterInfo.size()-1) {
		next = (FilterInfo)filterInfo.get(i+1);
		nextPhase = init ? next.init : next.steady;
	    }

	    // if the current filter doesn't execute at all, continue
	    // (FIXME this is part of some kind of special case for 
	    // filters that don't execute at all in a schedule, I think.)
	    if (curPhase.num!=0) {

		// if in the steady-state phase, restore the peek values
		if (!init) {
		    statements.addStatement(makePeekRestore(cur, curPhase));
		}
		// get the filter's work function
		JMethodDeclaration work = cur.filter.getWork();
		// take a deep breath and clone the body of the work function
		JBlock oldBody = new JBlock(null, work.getStatements(), null);
		JBlock body = (JBlock)ObjectDeepCloner.deepCopy(oldBody);
		// move variable declarations from front of <body> to
		// front of <statements>
		moveVarDecls(body, statements);
		// mutate <statements> to make them fit for fusion
		FusingVisitor fuser = 
		    new FusingVisitor(curPhase, nextPhase, i!=0,
				      i!=filterInfo.size()-1);
		for (ListIterator it = body.getStatementIterator(); 
		     it.hasNext() ; ) {
		    ((JStatement)it.next()).accept(fuser);
		}
		// get <body> into a loop in <statements>
		statements.addStatement(makeForLoop(body,
						    curPhase.loopCounter,
						    new 
						    JIntLiteral(curPhase.num))
					);
	    }
	    // get the postlude--copying state to the peek buffer
	    statements.addStatement(makePeekBackup(cur, curPhase));
	}
    }

    /**
     * Moves all variable declaration statements at front of <source>
     * to front of <dest>.
     */
    private static void moveVarDecls(JBlock source, JBlock dest) {
	JStatement decl;
	int index = 0;
	while (true) {
	    // get statement at <index> of source
	    decl = source.getStatement(index);
	    // if it's a var decl...
	    if (decl instanceof JVariableDeclarationStatement) {
		// remove the initializers from <decl> ...
		JVariableDefinition[] vars = ((JVariableDeclarationStatement)
					      decl).getVars();
		JExpressionListStatement assigns = stripInitializers(vars);
		// add to front of dest
		dest.addStatementFirst(decl);
		// remove from source
		source.removeStatement(index);
		// add assignment to source
		source.addStatement(index, assigns);
		index++;
	    } else {
		// quit looping when we run out of decl's
		break;
	    }
	}
    }

    /**
     * Strips all initializers out of <vars> and returns a statement that
     * assigns the initial value to each variable in <vars>.
     */
    private static JExpressionListStatement 
	stripInitializers(JVariableDefinition[] vars) {
	// make list to hold assignments
	LinkedList assign = new LinkedList();
	// go through vars 
	for (int i=0; i<vars.length; i++) {
	    // see if there's an initializer
	    if (vars[i].hasInitializer()) {
		// if so, clear it...
		JExpression init = vars[i].getValue();
		vars[i].setValue(null);
		// and make assignment
		JLocalVariableExpression lhs =
		    new JLocalVariableExpression(null, vars[i]);
		assign.add(new JAssignmentExpression(null,
						     lhs,
						     init));
	    }
	}
	return new JExpressionListStatement(null,
					    (JExpression[])
					    assign.toArray(new JExpression[0]),
					    null);
    }

    /**
     * Given that a phase is about to be executed, restores the peek
     * information to the front of the pop buffer.
     */
    private static JStatement makePeekRestore(FilterInfo filterInfo,
					      PhaseInfo phaseInfo) {
	// make a statement that will copy peeked items into the pop
	// buffer, assuming the counter will count from 0 to
	// (peek-pop) [exclusive upper bound]

	// the lhs of the source of the assignment
	JExpression sourceLhs = 
	    new JFieldAccessExpression(null,
				       new JThisExpression(null),
				       filterInfo.peekBuffer.
				       getVariable().getIdent());

	// the rhs of the source of the assignment
	JExpression sourceRhs = 
	    new JLocalVariableExpression(null, 
					 phaseInfo.loopCounter);

	// the lhs of the dest of the assignment
	JExpression destLhs = 
	    new JLocalVariableExpression(null,
					 phaseInfo.popBuffer);
	    
	// the rhs of the dest of the assignment
	JExpression destRhs = 
	    new JLocalVariableExpression(null,
					 phaseInfo.loopCounter);

	// the expression that copies items from the pop buffer to the
	// peek buffer
	JExpression copyExp = 
	    new JAssignmentExpression(null,
				      new JArrayAccessExpression(null,
								 destLhs,
								 destRhs),
				      new JArrayAccessExpression(null,
								 sourceLhs,
								 sourceRhs));

	// finally we have the body of the loop
	JStatement body = new JExpressionStatement(null, copyExp, null);

	// return a for loop that executes (peek-pop) times.
	return makeForLoop(body,
			   phaseInfo.loopCounter, 
			   new JIntLiteral(filterInfo.filter.
					   getPeekInt() -
					   filterInfo.filter.
					   getPopInt()));
    }

    /**
     * Given that a phase has already executed, backs up the state of
     * unpopped items into the peek buffer.
     */
    private static JStatement makePeekBackup(FilterInfo filterInfo,
					     PhaseInfo phaseInfo) {
	// make a statement that will copy unpopped items into the
	// peek buffer, assuming the counter will count from 0 to
	// (peek-pop) [exclusive upper bound]

	// the lhs of the destination of the assignment
	JExpression destLhs = 
	    new JFieldAccessExpression(null,
				       new JThisExpression(null),
				       filterInfo.peekBuffer.
				       getVariable().getIdent());

	// the rhs of the destination of the assignment
	JExpression destRhs = 
	    new JLocalVariableExpression(null, 
					 phaseInfo.loopCounter);

	// the lhs of the source of the assignment
	JExpression sourceLhs = 
	    new JLocalVariableExpression(null,
					 phaseInfo.popBuffer);
	    
	// the rhs of the source of the assignment... (add one to the
	// push index because of our pre-inc convention.)
	JExpression sourceRhs1 = 
	    new
	    JAddExpression(null, 
			   new JLocalVariableExpression(null, 
							phaseInfo.loopCounter),
			   new JAddExpression(null, new JIntLiteral(1),
					      new JLocalVariableExpression(null,
									   phaseInfo.pushCounter)));
	// need to subtract the difference in peek and pop counts to
	// see what we have to backup
	JExpression sourceRhs =
	    new JMinusExpression(null,
				 sourceRhs1,
				 new JIntLiteral(filterInfo.filter.getPeekInt()
						 -filterInfo.filter.getPopInt()
						 ));

	// the expression that copies items from the pop buffer to the
	// peek buffer
	JExpression copyExp = 
	    new JAssignmentExpression(null,
				      new JArrayAccessExpression(null,
								 destLhs,
								 destRhs),
				      new JArrayAccessExpression(null,
								 sourceLhs,
								 sourceRhs));

	// finally we have the body of the loop
	JStatement body = new JExpressionStatement(null, copyExp, null);

	// return a for loop that executes (peek-pop) times.
	return makeForLoop(body,
			   phaseInfo.loopCounter, 
			   new JIntLiteral(filterInfo.filter.getPeekInt() -
					   filterInfo.filter.getPopInt()));
    }

    /**
     * Returns a for loop that uses local variable <var> to count
     * <count> times with the body of the loop being <body>.  If count
     * is non-positive, just returns the initial assignment statement.
     */
    private static JStatement makeForLoop(JStatement body,
					  JLocalVariable var,
					  JExpression count) {
	// make init statement - assign zero to <var>.  We need to use
	// an expression list statement to follow the convention of
	// other for loops and to get the codegen right.
	JExpression initExpr[] = {
	    new JAssignmentExpression(null,
				      new JLocalVariableExpression(null, var),
				      new JIntLiteral(0)) };
	JStatement init = new JExpressionListStatement(null, initExpr, null);
	// if count==0, just return init statement
	if (count instanceof JIntLiteral) {
	    int intCount = ((JIntLiteral)count).intValue();
	    if (intCount<=0) {
		// return assignment statement
		return init;
	    }
	}
	// make conditional - test if <var> less than <count>
	JExpression cond = 
	    new JRelationalExpression(null,
				      Constants.OPE_LT,
				      new JLocalVariableExpression(null, var),
				      count);
	JExpression incrExpr = 
	    new JPostfixExpression(null, 
				   Constants.OPE_POSTINC, 
				   new JLocalVariableExpression(null, var));
	JStatement incr = 
	    new JExpressionStatement(null, incrExpr, null);

	return new JForStatement(null, init, cond, incr, body, null);
    }

    /**
     * Returns an init function that is the combinatio of those in
     * <filterInfo> and includes a call to <initWork>.  Also patches
     * the parent's init function to call the new one, given that
     * <result> will be the resulting fused filter.
     */
    private static 
	JMethodDeclaration makeInitFunction(List filterInfo,
					    JMethodDeclaration initWork,
					    SIRFilter result) {
	// get init function of parent
	JMethodDeclaration parentInit = 
	    ((FilterInfo)filterInfo.get(0)).filter.getParent().getInit();
	
	// make an init function builder out of <filterList>
	InitFuser initFuser = new InitFuser(filterInfo, initWork, result);
	
	// traverse <parentInit> with initFuser
	parentInit.accept(initFuser);

	// make the actual function
	return initFuser.getInitFunction();
    }

    /**
     * Returns an array of the fields that should appear in filter
     * fusing all in <filterInfo>.
     */
    private static JFieldDeclaration[] getFields(List filterInfo) {
	// make result
	List result = new LinkedList();
	// add the peek buffer's and the list of fields from each filter
	for (ListIterator it = filterInfo.listIterator(); it.hasNext(); ) {
	    FilterInfo info = (FilterInfo)it.next();
	    result.add(info.peekBuffer);
	    result.addAll(Arrays.asList(info.filter.getFields()));
	}
	// return result
	return (JFieldDeclaration[])result.toArray(new JFieldDeclaration[0]);
    }

    /**
     * Returns an array of the methods fields that should appear in
     * filter fusing all in <filterInfo>, with extra <init>, <initWork>, 
     * and <steadyWork> appearing in the fused filter.
     */
    private static 
	JMethodDeclaration[] getMethods(List filterInfo,
					JMethodDeclaration init,
					JMethodDeclaration initWork,
					JMethodDeclaration steadyWork) {
	// make result
	List result = new LinkedList();
	// start with the methods that we were passed
	result.add(init);
	result.add(initWork);
	result.add(steadyWork);
	// add methods from each filter that aren't work methods
	for (ListIterator it = filterInfo.listIterator(); it.hasNext(); ) {
	    FilterInfo info = (FilterInfo)it.next();
	    List methods = Arrays.asList(info.filter.getMethods());
	    for (ListIterator meth = methods.listIterator(); meth.hasNext(); ){
		JMethodDeclaration m = (JMethodDeclaration)meth.next();
		// add methods that aren't work
		if (m!=info.filter.getWork()) {
		    result.add(m);
		}
	    }
	}
	// return result
	return (JMethodDeclaration[])result.toArray(new JMethodDeclaration[0]);
    }

    /**
     * Return a name for the fused filter that consists of those
     * filters in <filterInfo>
     */
    protected static String getFusedName(List filterInfo) {
	StringBuffer name = new StringBuffer("Fused_");
	for (ListIterator it = filterInfo.listIterator(); it.hasNext(); ) {
	    name.append("_");
	    name.append(((FilterInfo)it.next()).filter.getIdent());
	}
	return name.toString();
    }

    /**
     * Mutates <result> to be the final, fused filter.
     */
    private static void makeFused(List filterInfo, 
				  JMethodDeclaration init, 
				  JMethodDeclaration initWork, 
				  JMethodDeclaration steadyWork,
				  SIRFilter result) {
	// get the first and last filters' info
	FilterInfo first = (FilterInfo)filterInfo.get(0);
	FilterInfo last = (FilterInfo)filterInfo.get(filterInfo.size()-1);

	// calculate the peek, pop, and push count for the fused
	// filter in the INITIAL state
	int initPop = first.init.num * first.filter.getPopInt();
	int initPeek =
	    (first.filter.getPeekInt() - first.filter.getPopInt()) + initPop;
	int initPush = last.init.num * last.filter.getPushInt();

	// calculate the peek, pop, and push count for the fused
	// filter in the STEADY state
	int steadyPop = first.steady.num * first.filter.getPopInt();
	int steadyPeek = 
	    (first.filter.getPeekInt() - first.filter.getPopInt()) + steadyPop;
	int steadyPush = last.steady.num * last.filter.getPushInt();

	System.out.println(" Fused filter init peek   = " + initPeek);
	System.out.println("              init pop    = " + initPop);
	System.out.println("              init push   = " + initPush);
	System.out.println("              steady peek = " + steadyPeek);
	System.out.println("              steady pop  = " + steadyPop);
	System.out.println("              steady push = " + steadyPush);

	// make a new filter to represent the fused combo
	result.copyState(new SIRTwoStageFilter(first.filter.getParent(),
					       getFusedName(filterInfo),
					       getFields(filterInfo),
					       getMethods(filterInfo, 
							  init, 
							  initWork, 
							  steadyWork),
					       new JIntLiteral(steadyPeek), 
					       new JIntLiteral(steadyPop),
					       new JIntLiteral(steadyPush),
					       steadyWork,
					       initPeek,
					       initPop,
					       initPush,
					       initWork,
					       voidToInt(first.filter.
					       getInputType()),
					       voidToInt(last.filter.
					       getOutputType())));
	
	// set init functions and work functions of fused filter
	result.setInit(init);
    }

    /**
     * If <type> is void, then return <int> type; otherwise return
     * <type>.  This is a hack to get around the disallowance of void
     * arrays in C--should fix this better post-asplos.
     */
    protected static CType voidToInt(CType type) {
	return type==CStdType.Void ? CStdType.Integer : type;
    }

}
    
/**
 * Contains information that is relevant to a given filter's
 * inclusion in a fused pipeline.
 */
class FilterInfo {
    /**
     * The filter itself.
     */
    public final SIRFilter filter;

    /**
     * The persistent buffer for holding peeked items
     */
    public final JFieldDeclaration peekBuffer;

    /**
     * The info on the initial execution.
     */
    public final PhaseInfo init;
	
    /**
     * The info on the steady-state execution.
     */
    public final PhaseInfo steady;

    public FilterInfo(SIRFilter filter, JFieldDeclaration peekBuffer,
		      PhaseInfo init, PhaseInfo steady) {
	this.filter = filter;
	this.peekBuffer = peekBuffer;
	this.init = init;
	this.steady = steady;
    }
}

class PhaseInfo {
    /**
     * The number of times this filter is executed in the parent.
     */ 
    public final int num;

    /**
     * The buffer for holding popped items.
     */
    public final JVariableDefinition popBuffer;

    /**
     * The counter for popped items.
     */
    public final JVariableDefinition popCounter;

    /*
     * The counter for pushed items (of the CURRENT phase)
     */
    public final JVariableDefinition pushCounter;

    /**
     * The counter for keeping track of executions of the whole block.
     */
    public final JVariableDefinition loopCounter;
    
    public PhaseInfo(int num, 
		     JVariableDefinition popBuffer,
		     JVariableDefinition popCounter,
		     JVariableDefinition pushCounter,
		     JVariableDefinition loopCounter) {
	this.num = num;
	this.popBuffer = popBuffer;
	this.popCounter = popCounter;
	this.pushCounter = pushCounter;
	this.loopCounter = loopCounter;
    }

    /**
     * Returns list of JVariableDefinitions of all var defs in here.
     */
    public List getVariables() {
	List result = new LinkedList();
	result.add(popBuffer);
	result.add(popCounter);
	result.add(pushCounter);
	result.add(loopCounter);
	return result;
    }
}

class FusingVisitor extends SLIRReplacingVisitor {
    /**
     * The info for the current filter.
     */
    private final PhaseInfo curInfo;

    /**
     * The info for the next filter in the pipeline.
     */
    private final PhaseInfo nextInfo;

    /**
     * Whether or not peek and pop expressions should be fused.
     */
    private final boolean fuseReads;

    /**
     * Whether or not push expressions should be fused.
     */
    private final boolean fuseWrites;

    public FusingVisitor(PhaseInfo curInfo, PhaseInfo nextInfo,
			 boolean fuseReads, boolean fuseWrites) {
	this.curInfo = curInfo;
	this.nextInfo = nextInfo;
	this.fuseReads = fuseReads;
	this.fuseWrites = fuseWrites;
    }

    public Object visitPopExpression(SIRPopExpression self,
				     CType tapeType) {
	// leave it alone not fusing reads
	if (!fuseReads) {
	    return super.visitPopExpression(self, tapeType);
	}

	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, curInfo.popBuffer);

	// build increment of index to array
	JExpression rhs =
	    new JPrefixExpression(null, 
				  Constants.OPE_PREINC, 
				  new JLocalVariableExpression(null,
							       curInfo.
							       popCounter));
	// return a new array access expression
	return new JArrayAccessExpression(null, lhs, rhs);
    }

    public Object visitPeekExpression(SIRPeekExpression oldSelf,
				      CType oldTapeType,
				      JExpression oldArg) {
	// leave it alone not fusing reads
	if (!fuseReads) {
	    return super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
	}

	// do the super
	SIRPeekExpression self = 
	    (SIRPeekExpression)
	    super.visitPeekExpression(oldSelf, oldTapeType, oldArg);
	
	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, curInfo.popBuffer);

	// build subtraction of peek index from current pop index (add
	// one to the pop index because of our pre-inc convention)
	JExpression rhs =
	    new JAddExpression(null,
			      new JAddExpression(null,
						 new JIntLiteral(1),
						 new JLocalVariableExpression(null,
									      curInfo.
									      popCounter)),
			      self.getArg());

	// return a new array access expression
	return new JArrayAccessExpression(null, lhs, rhs);
    }

    public Object visitPushExpression(SIRPushExpression oldSelf,
				      CType oldTapeType,
				      JExpression oldArg) {
	// leave it alone not fusing writes
	if (!fuseWrites) {
	    return super.visitPushExpression(oldSelf, oldTapeType, oldArg);
	}

	// do the super
	SIRPushExpression self = 
	    (SIRPushExpression)
	    super.visitPushExpression(oldSelf, oldTapeType, oldArg);
	
	// build ref to push array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, nextInfo.popBuffer);

	// build increment of index to array
	JExpression rhs =
	    new JPrefixExpression(null,
				  Constants.OPE_PREINC, 
				  new JLocalVariableExpression(null,
							       nextInfo.
							       pushCounter));
	// return a new array assignment to the right spot
	return new JAssignmentExpression(
		  null,
		  new JArrayAccessExpression(null, lhs, rhs),
		  self.getArg());
    }
}

/**
 * This builds up the init function of the fused class by traversing
 * the init function of the parent.
 */
class InitFuser extends SLIRReplacingVisitor {
    /**
     * The info on the filters we're trying to fuse.
     */
    private final List filterInfo;

    /**
     * The block of the resulting fused init function.
     */
    private JBlock fusedBlock;
    
    /**
     * A list of the parameters of the fused block, all of type
     * JFormalParameter.
     */
    private List fusedParam;
    
    /**
     * A list of the arguments to the init function of the fused
     * block, all of type JExpression.
     */
    private List fusedArgs;

    /**
     * What will be filled in to be the final, fused filter.
     */
    private SIRFilter fusedFilter;

    /**
     * Cached copy of the method decl for the init function.
     */
    private JMethodDeclaration initFunction;

    /**
     * The initWork function of the filter we're fusing.
     */
    private JMethodDeclaration initWork;

    /**
     * The number of filter's we've fused.
     */
    private int numFused;

    /**
     * <fusedFilter> represents what -will- be the result of the
     * fusion.  It has been allocated, but is not filled in with
     * correct values yet.
     */
    public InitFuser(List filterInfo, 
		     JMethodDeclaration initWork,
		     SIRFilter fusedFilter) {
	this.filterInfo = filterInfo;
	this.initWork = initWork;
	this.fusedFilter = fusedFilter;
	this.fusedBlock = new JBlock(null, new JStatement[0], null);
	this.fusedParam = new LinkedList();
	this.fusedArgs = new LinkedList();
	this.numFused = 0;
    }

    /**
     * Visits an init statement.
     */
    public Object visitInitStatement(SIRInitStatement self,
				     JExpression[] args,
				     SIRStream target) {
	// if <target> is a filter that we're fusing, then incorporate
	// this init statement into the fused work function, and
	// replace it with an empty statement in the parent.
	for (ListIterator it = filterInfo.listIterator(); it.hasNext(); ) {
	    FilterInfo info = (FilterInfo)it.next();
	    if (info.filter == target) {
		processArgs(info, args);
		// if this is the last one, replace it with call to
		// fused init function
		if (!it.hasNext()) {
		    makeInitFunction();
		    return makeInitCall(filterInfo);
		} else {
		    // otherwise, just return an empty statement
		    return new JEmptyStatement(null, null);
		}
	    }
	}

	// otherwise, just leave it the way it was
	return self;
    }

    /**
     * Given that we found <args> in an init call to <info>,
     * incorporate this info into the init function of the fused
     * filter.
     */
    private void processArgs(FilterInfo info, 
			     JExpression[] args) {
	// make parameters for <args>, and build <newArgs> to pass
	// to new init function call
	JExpression[] newArgs = new JExpression[args.length];
	for (int i=0; i<args.length; i++) {
	    JFormalParameter param = 
		new JFormalParameter(null,
				     0,
				     args[i].getType(),
				     FusePipe.INIT_PARAM_NAME + 
				     "_" + i + "_" + numFused,
				     false);
	    // add to list
	    fusedParam.add(param);
	    // make a new arg
	    newArgs[i] = new JLocalVariableExpression(null, param);
	    // increment fused count
	    numFused++;
	}

	// add the arguments to the list
	fusedArgs.addAll(Arrays.asList(args));

	// make a call to the init function of <info> with <params>
	fusedBlock.addStatement(new JExpressionStatement(
              null,
	      new JMethodCallExpression(null,
					new JThisExpression(null),
					info.filter.getInit().getName(),
					newArgs), null));
    }

    /**
     * Fabricates a call to this init function.
     */
    private JStatement makeInitCall(List filterInfo) {
	return new SIRInitStatement(null, 
				    null, 
				    /* args */
				    (JExpression[])
				    fusedArgs.toArray(new JExpression[0]),
				    /* target */
				    fusedFilter);
    }

    /**
     * Prepares the init function for the fused block once the
     * traversal of the parent's init function is complete.
     */
    private void makeInitFunction() {
	// add allocations for peek buffers
	for (ListIterator it = filterInfo.listIterator(); it.hasNext(); ) {
	    // get the next info
	    FilterInfo info = (FilterInfo)it.next();
	    // calculate dimensions of the buffer
	    JExpression[] dims = { new JIntLiteral(null, 
						   info.filter.getPeekInt() -
						   info.filter.getPopInt())};
	    // add a statement initializeing the peek buffer
	    fusedBlock.addStatementFirst(new JExpressionStatement(null,
	      new JAssignmentExpression(
		  null,
		  new JFieldAccessExpression(null,
					     new JThisExpression(null),
					     info.peekBuffer.
					     getVariable().getIdent()),
		  new JNewArrayExpression(null,
					  FusePipe.voidToInt(info.filter.
					  getInputType()),
					  dims,
					  null)), null));
	}
	// now we can make the init function
	this.initFunction = new JMethodDeclaration(null,
				      at.dms.kjc.Constants.ACC_PUBLIC,
				      CStdType.Void,
				      "init",
				      (JFormalParameter[])
				      fusedParam.toArray(new 
							 JFormalParameter[0]),
				      CClassType.EMPTY,
				      fusedBlock,
				      null,
				      null);
    }
    
    /**
     * Returns fused init function of this.
     */
    public JMethodDeclaration getInitFunction() {
	Utils.assert(initFunction!=null);
	return initFunction;
    }
}

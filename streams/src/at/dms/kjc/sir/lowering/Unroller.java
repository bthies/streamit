
package at.dms.kjc.sir.lowering;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This class unrolls loops where it can.
 */
public class Unroller extends SLIRReplacingVisitor {
    /**
     * Map allowing the current block to access the modified
     * list of the current for loop
     */
    private Hashtable currentModified;
    /**
     * Map of known constants (JLocalVariable -> JLiteral)
     */
    private Hashtable constants;
    /**
     * Holds compile time values
     */
    private Hashtable values;
    /**
     * Whether or not anything has been unrolled.
     */
    private boolean hasUnrolled;
    /**
     * Whether or not we're in the init function of a container.
     * (Should unroll maximally here because might contain stream
     * children.)
     */
    private boolean inContainerInit;
    /**
     * Whether or not outer loops should be unrolled.  For performance
     * reasons, the defauls is no, but for some applications (e.g.,
     * linear analysis) full unrolling is required.
     */
    private final boolean unrollOuterLoops;
    
    /**
     * Creates one of these given that <constants> maps
     * JLocalVariables to JLiterals for the scope that we'll be
     * visiting.
     */
    public Unroller(Hashtable constants) {
	this(constants, false);
    }
    public Unroller(Hashtable constants, boolean unrollOuterLoops) {
	super();
	this.constants = constants;
	this.unrollOuterLoops = unrollOuterLoops;
	this.hasUnrolled = false;
	currentModified=new Hashtable();
	values=new Hashtable();
	inContainerInit=false;
    }
    
    /**
     * Unrolls <filter> up to a factor of 100000.
     */
    public static void unrollFilter(SIRFilter filter) {
	// set all loops to be unrolled again
	IterFactory.createFactory().createIter(filter).accept(new EmptyStreamVisitor() {
		public void preVisitStream(SIRStream filter, SIRIterator iter) {
		    for (int i=0; i<filter.getMethods().length; i++) {
			filter.getMethods()[i].accept(new SLIREmptyVisitor() {
				public void visitForStatement(JForStatement self, JStatement init, JExpression cond,
							      JStatement incr, JStatement body) {
				    self.setUnrolled(false);
				}
			    });
		    }
		}
	    });
	// now do unrolling
	int origUnroll = KjcOptions.unroll;
	KjcOptions.unroll = 100000;
	FieldProp.doPropagate(filter, true);
	KjcOptions.unroll = origUnroll;
    }

    public void setContainerInit(boolean init) {
	inContainerInit=init;
    }

    public boolean getContainerInit() {
	return inContainerInit;
    }
    
    public static void unroll(SIRStream str) {
	if (str instanceof SIRFeedbackLoop)
	    {
		SIRFeedbackLoop fl = (SIRFeedbackLoop)str;
		unroll(fl.getBody());
		unroll(fl.getLoop());
	    }
        if (str instanceof SIRPipeline)
	    {
		SIRPipeline pl = (SIRPipeline)str;
		Iterator iter = pl.getChildren().iterator();
		while (iter.hasNext())
		    {
			SIRStream child = (SIRStream)iter.next();
			unroll(child);
		    }
	    }
        if (str instanceof SIRSplitJoin)
	    {
		SIRSplitJoin sj = (SIRSplitJoin)str;
		Iterator iter = sj.getParallelStreams().iterator();
		while (iter.hasNext())
		    {
			SIRStream child = (SIRStream)iter.next();
			unroll(child);
		    }
	    }
	if (str instanceof SIRFilter)
	    for (int i = 0; i < str.getMethods().length; i++) {
		Unroller unroller;
		//Very aggressive
		//Intended as a last and final unroll pass
		//do {
		//do { //Unroll as much as possible
		//unroller=new Unroller(new Hashtable());
		//str.getMethods()[i].accept(unroller);
		str.getMethods()[i].accept(new Propagator(new Hashtable()));
		//  } while(unroller.hasUnrolled());
		//Constant Prop then check to see if any new unrolling can be done
		//str.getMethods()[i].accept(new Propagator(new Hashtable()));
		//unroller=new Unroller(new Hashtable());
		//str.getMethods()[i].accept(unroller);
		//} while(unroller.hasUnrolled());
	    }
    }

    /**
     * checks prefix
     */
    public Object visitPrefixExpression(JPrefixExpression self,
					int oper,
					JExpression expr) {
	if(expr instanceof JLocalVariableExpression) {
	    currentModified.put(((JLocalVariableExpression)expr).getVariable(),Boolean.TRUE);
	    values.remove(((JLocalVariableExpression)expr).getVariable());
	}
	return super.visitPrefixExpression(self,oper,expr);
    }
    
    /**
     * checks postfix
     */
    public Object visitPostfixExpression(JPostfixExpression self,
					 int oper,
					 JExpression expr) {
	if(expr instanceof JLocalVariableExpression){
	    currentModified.put(((JLocalVariableExpression)expr).getVariable(),Boolean.TRUE);
	    values.remove(((JLocalVariableExpression)expr).getVariable());
	}
	return super.visitPostfixExpression(self,oper,expr);
    }

    /**
     * checks var def
     */
    public Object visitVariableDefinition(JVariableDefinition self,
					  int modifiers,
					  CType type,
					  String ident,
					  JExpression expr) {
	currentModified.put(self,Boolean.TRUE);
	if(expr instanceof JLiteral) {
	    values.put(self,expr);
	}
    	return super.visitVariableDefinition(self,modifiers,type,ident,expr);
    }

    /**
     * checks assignment
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
					    JExpression left,
					    JExpression right) {
	if(left instanceof JLocalVariableExpression) {
	    currentModified.put(((JLocalVariableExpression)left).getVariable(),Boolean.TRUE);
	    if(right instanceof JLiteral) {
		values.put(((JLocalVariableExpression)left).getVariable(),right);
	    }
	}
	return super.visitAssignmentExpression(self,left,right);
    }

    /**
     * Overload the for-statement visit.
     */
    public Object visitForStatement(JForStatement self,
				    JStatement init,
				    JExpression cond,
				    JStatement incr,
				    JStatement body) {

	// to do the right thing if someone set an unroll factor of 0
	// (or 1, which means to do nothing)
	if((KjcOptions.unroll>1 || inContainerInit) && !self.getUnrolled()) { //Ignore if already unrolled
	    // first recurse into body...
	    Hashtable saveModified=currentModified;
	    currentModified=new Hashtable();
	    // we're going to see if any child unrolls, to avoid
	    // unrolling doubly-nested loops
	    boolean saveHasUnrolled = hasUnrolled;
	    hasUnrolled = false;

	    JStatement newStmt = (JStatement)body.accept(this);
	    if (newStmt!=null && newStmt!=body) {
		self.setBody(newStmt);
	    }
	    
	    boolean childHasUnrolled = hasUnrolled;
	    // restore this way because you want to propagate child
	    // unrollings up, but don't want to eliminate record of
	    // previous unrolling
	    hasUnrolled = saveHasUnrolled || childHasUnrolled;

	    // only unroll if we're set to unroll outer loops, or if
	    // child hasn't unrolled, or if we're doing the init
	    // function
	    if (unrollOuterLoops || !childHasUnrolled || inContainerInit) {
		// check for loop induction variable
		UnrollInfo info = getUnrollInfo(init, cond, incr, body,values,constants);
		// see if we can unroll...
		if(shouldUnroll(info, body, currentModified)) {
		    // Set modified
		    saveModified.putAll(currentModified);
		    currentModified=saveModified;
		    currentModified.put(info.var,Boolean.TRUE);
		    // do unrolling
		    return doUnroll(info, self);
		} else if(canUnroll(info,currentModified)) {
		    // Set modified
		    saveModified.putAll(currentModified);
		    currentModified=saveModified;
		    currentModified.put(info.var,Boolean.TRUE);
		    // do unrolling
		    return doPartialUnroll(info, self);
		}
	    } else {
		// otherwise, still mark the loop as having unrolled,
		// because we don't want to consider it again and
		// unroll it when children were unrolled by a
		// different unroller
		if (!childHasUnrolled)
		    self.setUnrolled(true);
	    }
	    saveModified.putAll(currentModified);
	    currentModified=saveModified;
	}
	return self;
    }

    /**
     * Returns whether or not we should unroll a loop with unrollinfo
     * <info>, body <body> and <currentModified> as in
     * visitForStatement.
     */
    private boolean shouldUnroll(UnrollInfo info, JStatement body, Hashtable currentModified) {
	// if no unroll info or variable is modified in loop, fail
	if (info==null || currentModified.containsKey(info.var)) {
	    return false;
	}

	//Unroll if in init
	if(inContainerInit)
	    return true;
	
	/*
	  // otherwise if there is an SIRInitStatement in the loop, then
	  // definately unroll for the sake of graph expansion
	  final boolean[] hasInit = { false };
	  body.accept(new SLIREmptyVisitor() {
	  public void visitInitStatement(SIRInitStatement self,
	  SIRStream target) {
	  super.visitInitStatement(self, target);
	  hasInit[0] = true;
	  }
	  });
	  if (hasInit[0]) {
	  return true;
	  }
	*/

	// Unroll maximally for number gathering
	if(KjcOptions.numbers>0) {
	    final boolean[] hasPrint = { false };
	    body.accept(new SLIREmptyVisitor() {
		    public void visitPrintStatement(SIRPrintStatement self,
						    JExpression arg) {
			hasPrint[0]=true;
			super.visitPrintStatement(self,arg);
		    }
		});
	    if (hasPrint[0]) {
		return true;
	    }
	}

	// otherwise calculate how many times the loop will execute,
	// and only unroll if it is within our max unroll range
	int count = getNumExecutions(info);

	return count <= KjcOptions.unroll;
    }

    /**
     * Failing shouldUnroll (completely) this determines if the loop can
     * be unrolled partially
     */
    private boolean canUnroll(UnrollInfo info, Hashtable currentModified) {
	if (info==null || currentModified.containsKey(info.var)) {
	    return false;
	}
	return true;
    }

    /**
     * Returns the number of times a for-loop with the given
     * characteristics will execute, or -1 if the count cannot be
     * determined.
     */
    public static int getNumExecutions(JStatement init,
				       JExpression cond,
				       JStatement incr,
				       JStatement body) {
	UnrollInfo info = getUnrollInfo(init, cond, incr, body,new Hashtable(),new Hashtable());
	return getNumExecutions(info);
    }

    /**
     * Returns how many times a for loop with unroll info <info> will
     * execute.
     */
    private static int getNumExecutions(UnrollInfo info) {
	// if we didn't get any unroll info, return -1
	if (info==null) { return -1; }
	// get the initial value of the counter
	int counter = info.initVal;
	// track number of executions
	int result = 0;
	// simulate execution of the loop...
	while (done(counter, info)) {
	    // increment counters
	    counter = incrementCounter(counter, info);
	    result++;
	}
	return result;
    }

    /**
     * Given the loop <self> and original unroll info <info>, perform
     * the unrolling and return a statement block of the new
     * statements.
     */
    private JBlock doUnroll(UnrollInfo info, JForStatement self) {
	// make a list of statements
	List statementList = new LinkedList();
	statementList.add(self.getInit());
	// get the initial value of the counter
	int counter = info.initVal;
	// simulate execution of the loop...
	Propagator prop=new Propagator(new Hashtable());
	while (done(counter,info)) {
	    // replace induction variable with its value current value
	    prop.getConstants().put(info.var, new JIntLiteral(counter));
	    // do the replacement
            JStatement newBody =
                (JStatement)ObjectDeepCloner.deepCopy(self.getBody());
	    newBody.accept(prop);
	    // add to statement list
	    statementList.add(newBody);
	    // increment counter
	    counter = incrementCounter(counter, info);
	}
	statementList.add(new JExpressionStatement(self.getTokenReference(),new JAssignmentExpression(self.getTokenReference(),new JLocalVariableExpression(self.getTokenReference(),info.var),new JIntLiteral(counter)),null));
	/*	Hashtable cons=prop.getConstants();
		Enumeration enum=prop.getChanged().keys();
		while(enum.hasMoreElements()) {
		JLocalVariable var=(JLocalVariable)enum.nextElement();
		Object val=cons.get(var);
		if(val instanceof JLiteral)
		statementList.add(new JExpressionStatement(null,new JAssignmentExpression(null,new JLocalVariableExpression(null,var),(JLiteral)val),null));
		System.err.println(var+"="+val);
		}*/
	// mark that we've unrolled
	this.hasUnrolled = true;
	// return new block instead of the for loop
	constants.remove(info.var);
	return new JBlock(null, 
			  (JStatement[])statementList.
			  toArray(new JStatement[0]),
			  null);
    }
    
    /**
     * Repeats body KjcOptions.unroll times and adds post loop guard
     */
    private JBlock doPartialUnroll(final UnrollInfo info, JForStatement self) {
	int numExec=getNumExecutions(info);
	//int numLoops=numExec/KjcOptions.unroll;
	int remain=numExec%KjcOptions.unroll;
	JStatement[] newBody=new JStatement[KjcOptions.unroll];
	//if(newBody.length>=2) {
	//newBody[0]=self.getBody();
	//newBody[1]=self.getIncrement();
	//}
	if(KjcOptions.unroll>=1) {
	    JStatement cloneBody=(JStatement)ObjectDeepCloner.deepCopy(self.getBody());
	    newBody[0]=cloneBody;
	}
	{
	    final JLocalVariable inductVar=info.var;
	    final int incrVal=info.incrVal;
	    for(int i=1;i<KjcOptions.unroll;i++) {
		JStatement cloneBody=(JStatement)ObjectDeepCloner.deepCopy(self.getBody());
		//JStatement cloneIncr=(JStatement)ObjectDeepCloner.deepCopy(makeIncr(info,info.incrVal));
		final int incremented=i;
		cloneBody.accept(new SLIRReplacingVisitor() {
			public Object visitLocalVariableExpression(JLocalVariableExpression self2,
								   String ident) {
			    if(inductVar.equals(self2.getVariable())) {
				return makeIncreased(info,incremented*incrVal);
			    } else
				return self2;
			}  
		    });
		newBody[i]=cloneBody;
	    }
	}
	JBlock body=new JBlock(null,newBody,null);
	JStatement[] newStatements=new JStatement[2*remain+2];
	newStatements[0]=self.getInit();
	int result=info.initVal;
	for(int i=1;i<2*remain+1;i++) {
	    JStatement cloneBody=(JStatement)ObjectDeepCloner.deepCopy(self.getBody());
	    JStatement cloneIncr=(JStatement)ObjectDeepCloner.deepCopy(makeIncr(info,info.incrVal));
	    newStatements[i]=cloneBody;
	    i++;
	    newStatements[i]=cloneIncr;
	    result=incrementCounter(result,info);
	}
	JForStatement newFor=new JForStatement(null,
					       new JExpressionStatement(null,
									new JAssignmentExpression(null,
												  new JLocalVariableExpression(null,
															       info.var),
												  new JIntLiteral(result)),
									null),
					       
					       self.getCondition(),
					       makeIncr(info,KjcOptions.unroll*info.incrVal),
					       body,
					       null);
	newFor.setUnrolled(true);
	newStatements[newStatements.length-1]=newFor;
	// mark that we've unrolled
	this.hasUnrolled = true;
	return new JBlock(null,
			  newStatements,
			  null);
    }
    
    private static boolean done(int counter, UnrollInfo info) {
	switch(info.oper) {
	case OPE_PLUS: 
	case OPE_POSTINC:
	case OPE_PREINC:
	case OPE_STAR: 
	    return counter < info.finalVal;
	case OPE_MINUS: 
        case OPE_POSTDEC:
        case OPE_PREDEC:   
	case OPE_SLASH:
	    return counter > info.finalVal;
	default:
	    Utils.fail("Can only unroll add/sub/mul/div increments for now.");
	    // dummy value
	    return false;
	}
    }
    
    /**
     * Given the UnrollInfo <info> and that <counter> was the old
     * value of the count, returns the new value of the count.
     */
    private static int incrementCounter(int counter, UnrollInfo info) {
	switch(info.oper) {
	case OPE_PLUS: 
        case OPE_POSTINC:
        case OPE_PREINC:
	    return counter + info.incrVal;
	case OPE_MINUS: 
        case OPE_POSTDEC:
        case OPE_PREDEC:
	    return counter - info.incrVal;
	case OPE_STAR: 
	    return counter * info.incrVal;
	case OPE_SLASH:
	    return counter / info.incrVal;
	default: 
	    Utils.fail("Can only unroll add/sub/mul/div increments for now.");
	    // dummy value
	    return 0;
	}
    }

    private static JStatement makeIncr(UnrollInfo info,int num) {
	JLocalVariableExpression var=new JLocalVariableExpression(null,info.var);
	JIntLiteral numLit=new JIntLiteral(null,num);
	JAssignmentExpression incr=new JAssignmentExpression(null,var,null);
	switch(info.oper) {
	case OPE_PLUS: 
        case OPE_POSTINC:
        case OPE_PREINC:
	    incr.setRight(new JAddExpression(null,var,numLit));
	    return new JExpressionStatement(null,incr,null);
	case OPE_MINUS:
        case OPE_POSTDEC:
        case OPE_PREDEC:
	    incr.setRight(new JMinusExpression(null,var,numLit));
	    return new JExpressionStatement(null,incr,null);
	case OPE_STAR: 
	    incr.setRight(new JMultExpression(null,var,numLit));
	    return new JExpressionStatement(null,incr,null);
	case OPE_SLASH:
	    incr.setRight(new JDivideExpression(null,var,numLit));
	    return new JExpressionStatement(null,incr,null);
	default: 
	    Utils.fail("Can only unroll add/sub/mul/div increments for now.");
	    return new JExpressionStatement(null,incr,null);
	}
    }
    
    private static JExpression makeIncreased(UnrollInfo info,int num) {
	JLocalVariableExpression var=new JLocalVariableExpression(null,info.var);
	JIntLiteral numLit=new JIntLiteral(null,num);
	switch(info.oper) {
	case OPE_PLUS: 
        case OPE_POSTINC:
        case OPE_PREINC:
	    return new JAddExpression(null,var,numLit);
	case OPE_MINUS:
        case OPE_POSTDEC:
        case OPE_PREDEC:
	    return new JMinusExpression(null,var,numLit);
	case OPE_STAR: 
	    return new JMultExpression(null,var,numLit);
	case OPE_SLASH:
	    return new JDivideExpression(null,var,numLit);
	default: 
	    Utils.fail("Can only unroll add/sub/mul/div increments for now.");
	    return null;
	}
    }
    
    /**
     * Return whether or not this has unrolled any loops.
     */
    public boolean hasUnrolled() {
	return hasUnrolled;
    }

    /**
     * Gets unroll info for this loop.  Right now, we check that:
     *
     *  1. the initialization is an assignemnt of a constant to a variable
     *      - further, the variable is not declared in the initialization;
     *        it is only assigned to there (no "for (int i=...)")
     *  Above should be fixed --jasperln
     *  2. the condition is a relational less-than test of the var and a const
     *  3. the incr is addition or multiplication or div by a const. (use +=1, not ++)
     *  4. the variable is an integer
     *
     *  We do not check that the induction variable is unmodified in
     *  the loop.  Should be fixed for prefix/suffix expr and assign --jasperln
     *
     * This will return <null> if the loop can not be unrolled. 
     */
    private static UnrollInfo getUnrollInfo(JStatement init,
					    JExpression cond,
					    JStatement incr,
					    JStatement body,
					    Hashtable values,
					    Hashtable constants) {
	try {
	    JLocalVariable var;
	    int initVal=0;
	    int finalVal=0;
	    boolean incrementing;
	    // inspect condition...
	    JRelationalExpression condExpr = (JRelationalExpression)cond;
	    int relation=condExpr.getOper();
	    var=((JLocalVariableExpression)condExpr.getLeft()).getVariable();
	    if(init instanceof JExpressionListStatement || init instanceof JExpressionStatement) {
		JAssignmentExpression initExpr;
		if (init instanceof JExpressionListStatement) {
		    initExpr = (JAssignmentExpression)
			((JExpressionListStatement)init).getExpression(0);

		} else {
		    initExpr = (JAssignmentExpression)((JExpressionStatement)init).getExpression();
		}
		if(((JLocalVariableExpression)initExpr.getLeft()).getVariable()==var)
		    initVal 
			= ((JIntLiteral)initExpr.getRight()).intValue();
		else if(values.containsKey(var))
		    initVal=((JIntLiteral)values.get(var)).intValue();
		else if(constants.containsKey(var))
		    initVal=((JIntLiteral)constants.get(var)).intValue();
		else {
		    throw new Exception("Not Constant!");
		}
	    } else if(values.containsKey(var))
		initVal=((JIntLiteral)values.get(var)).intValue();
	    else {
		throw new Exception("Not Constant!");
	    }
	    // get the upper limit
	    if(condExpr.getRight() instanceof JIntLiteral) {
		finalVal = ((JIntLiteral)condExpr.getRight()).intValue();
		if(relation==OPE_LE)
		    finalVal++;
		else if(relation==OPE_GE)
		    finalVal--;
	    } else {
		// if we can't get the upper limit, then we don't know
		// how much to unroll, so fail
		return null;
	    }
	    //else
	    //System.err.println("Cond: "+((JFieldAccessExpression)condExpr.getRight()).isConstant());
	    // inspect increment...
	    int incrVal, oper;
	    JLocalVariableExpression incrVar;
	    JExpression incrExpr;
	    if(incr instanceof JExpressionListStatement)
		incrExpr =
		    ((JExpressionListStatement)incr).getExpression(0);
	    else {
		incrExpr =
		    ((JExpressionStatement)incr).getExpression();
	    }
	    if (incrExpr instanceof JCompoundAssignmentExpression)
		{
		    JCompoundAssignmentExpression cae =
			(JCompoundAssignmentExpression)incrExpr;
		    oper = cae.getOperation();
		    incrVal = ((JIntLiteral)cae.getRight()).intValue();
		    incrVar =
			(JLocalVariableExpression)cae.getLeft();
		}
	    else if(incrExpr instanceof JAssignmentExpression){
		JAssignmentExpression ae=(JAssignmentExpression)incrExpr;
		//oper = ae.getOperation();
		//incrVal=((JIntLiteral)ae.getRight()).intValue();
		incrVar=(JLocalVariableExpression)ae.getLeft();
		JBinaryExpression expr=(JBinaryExpression)ae.getRight();
		if(expr instanceof JDivideExpression) {
		    if(!((JLocalVariableExpression)expr.getLeft()).equals(incrVar)) {
			//System.err.println("Vars don't match!");
			return null;
		    }
		    incrVal=((JIntLiteral)expr.getRight()).intValue();
		    oper=OPE_SLASH;
		} else if(expr instanceof JMultExpression) {
		    oper=OPE_STAR;
		    if(expr.getLeft() instanceof JLocalVariableExpression) {
			if(!((JLocalVariableExpression)expr.getLeft()).equals(incrVar)) {
			    return null;
			}
			incrVal=((JIntLiteral)expr.getRight()).intValue();
		    } else if(expr.getRight() instanceof JLocalVariableExpression) {
			if(!((JLocalVariableExpression)expr.getRight()).equals(incrVar)) {
			    return null;
			}
			incrVal=((JIntLiteral)expr.getLeft()).intValue();
		    } else {
			return null;
		    }
		} else if(expr instanceof JAddExpression) {
		    oper=OPE_PLUS;
		    if(expr.getLeft() instanceof JLocalVariableExpression) {
			if(!((JLocalVariableExpression)expr.getLeft()).equals(incrVar)) {
			    return null;
			}
			incrVal=((JIntLiteral)expr.getRight()).intValue();
		    } else if(expr.getRight() instanceof JLocalVariableExpression) {
			if(!((JLocalVariableExpression)expr.getRight()).equals(incrVar)) {
			    return null;
			}
			incrVal=((JIntLiteral)expr.getLeft()).intValue();
		    } else 
			return null;
		} else if(expr instanceof JMinusExpression) {
		    if(!((JLocalVariableExpression)expr.getLeft()).equals(incrVar)) {
			//System.err.println("Vars don't match!");
			return null;
		    }
		    incrVal=((JIntLiteral)expr.getRight()).intValue();
		    oper=OPE_MINUS;
		} else {
		    return null;
		}
	    } else if (incrExpr instanceof JPrefixExpression)
		{
		    JPrefixExpression pfx = (JPrefixExpression)incrExpr;
		    oper = pfx.getOper();
		    incrVal = 1;
		    incrVar = (JLocalVariableExpression)pfx.getExpr();
		}
	    else if (incrExpr instanceof JPostfixExpression)
		{
		    JPostfixExpression pfx = (JPostfixExpression)incrExpr;
		    oper = pfx.getOper();
		    incrVal = 1;
		    incrVar = (JLocalVariableExpression)pfx.getExpr();
		}
	    else {
		return null;
	    }
	    
	    // make sure the variable is the same
	    if (var != incrVar.getVariable()) {
		return null;
	    }

	    //Not have to worry about weird cases
	    if(incrVal==0) {
		return null;
	    } else if(((oper==OPE_STAR)||(oper==OPE_SLASH))&&incrVal<2) {
		return null;
	    }
	    
	    //Normalize + and -
	    if((oper==OPE_PLUS)&&(incrVal<0)) {
		oper=OPE_MINUS;
		incrVal*=-1;
	    } else if((oper==OPE_MINUS)&&(incrVal<0)) {
		oper=OPE_PLUS;
		incrVal*=-1;
	    }

	    //Check to make sure we are incrementing to a ceiling
	    //or decrementing to a floor
	    if((((oper==OPE_PLUS)||(oper==OPE_STAR)||(oper==OPE_POSTINC)||(oper==OPE_PREINC))&&((relation==OPE_GE)||(relation==OPE_GT)))||
	       (((oper==OPE_MINUS)||(oper==OPE_SLASH)||(oper==OPE_POSTDEC)||(oper==OPE_PREDEC))&&((relation==OPE_LE)||(relation==OPE_LT)))) {
		return null;
	    }

	    // return result
	    return new UnrollInfo(var, initVal, finalVal, oper, incrVal);
	} catch (Exception e) {
	    // uncommment these lines if you want to trace a case of something
	    // not unrolling ---
	    
	    //System.err.println("Didn't unroll because:");
	    //e.printStackTrace();
	    
	    // assume we failed 'cause assumptions violated -- return null
	    return null;
	}
    }

    /**
     * Returns the number of times that a loop with unroll info <info>
     * can be unrolled.
     */
    private int calcUnrollFactor(UnrollInfo info) {
	switch(info.oper) {
	case OPE_PLUS: 
	case OPE_POSTINC:
	case OPE_PREINC:
	case OPE_MINUS: 
	case OPE_POSTDEC:
	case OPE_PREDEC:
	    return (info.finalVal-info.initVal)/info.incrVal;
	case OPE_STAR: 
	    // simulate execution of multiplication
	    int count = 0;
	    int val = info.initVal;
	    while (val < info.finalVal) {
		val *= info.incrVal;
		count++;
	    }
	    return count;
	case OPE_SLASH:
	    // simulate execution of division
	    int count2 = 0;
	    int val2 = info.initVal;
	    while (val2 > info.finalVal) {
		val2 /= info.incrVal;
		count2++;
	    }
	    return count2;
	default: 
	    Utils.fail("Can only unroll add/sub/mul/div increments for now.");
	    // dummy value
	    return 0;
	}
    }

    static class UnrollInfo {
	/**
	 * The induction variable in the loop.
	 */
	public final JLocalVariable var;
	/**
	 * The initial value of the induction variable.
	 */
	public final int initVal;
	/**
	 * The final value of the induction variable.
	 */
	public final int finalVal;
	/**
	 * The operation that is being used to change the induction variable.
	 */
	public final int oper;
	/**
	 * The increment.
	 */
	public final int incrVal;
	
	public UnrollInfo(JLocalVariable var,
			  int initVal, 
			  int finalVal, 
			  int oper, 
			  int incrVal) {
	    this.var = var;
	    this.initVal = initVal;
	    this.finalVal = finalVal;
	    this.oper = oper;
	    this.incrVal = incrVal;
	}
    }
}


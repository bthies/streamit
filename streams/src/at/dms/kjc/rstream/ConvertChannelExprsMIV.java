package at.dms.kjc.rstream;

import at.dms.kjc.common.*;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.flatgraph.FlatVisitor;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.Utils;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.HashMap;
import java.io.*;
import at.dms.compiler.*;
import at.dms.kjc.sir.lowering.*;
import java.util.Hashtable;
import at.dms.util.SIRPrinter;

/**
 * This class will try to convert the communication expressions of a filter
 * (push, pop, and peek) into buffer accesses that are indexed by a linear function
 * of the expression's enclosing loop indcution variables (MIVs).  It performs the conversion
 * inplace. 
 *
 * It handles pop/peek separately.  It will succeed for pops/peeks if all pops are 
 * enclosed only in analyzable control flow, which is incrementing doloops with static bounds or
 * if/then/else statements and the rate on each branch of an if are equal.
 *
 * Push statements are complementary.
 * 
 *
 * @author Michael Gordon
*/
public class ConvertChannelExprsMIV {

    private JLocalVariable popBuffer;
    private JLocalVariable pushBuffer;
    private int pushOffset;

    FilterFusionState fusionState;

    /** Create a new conversion object that will try to convert the communication
	expression in *current* to MIVs for the current stage (init if *isInit* == true).
	Don't forget to call tryMIV();
    **/
    public ConvertChannelExprsMIV(FilterFusionState current, boolean isInit)
    {
	//calculate push offset!!
	
	this.pushBuffer = null;

	//set the push buffer and the push counter if this filter pushes
	if (current.getNode().ways > 0) {
	    assert current.getNode().ways == 1;
	    assert current.getNode().edges[0] != null;
	    
	    FusionState next = FusionState.getFusionState(current.getNode().edges[0]);

	    this.pushBuffer = current.getPushBufferVar(isInit);
	    //if this is not the init, then remember to push after the items saved,
	    //by the downstream filter (these items were not pop'ed and they were copied to 
	    //the beginning of the downstream buffer)
	    this.pushOffset = isInit ? 0 :  next.getRemaining(current.getNode(), isInit);
	}
	
	this.popBuffer = current.getBufferVar(null /*this is a filter, so only one previous */,
					      isInit);
	fusionState = current;
    }
    
    /**
     * Try convert the communication expressions of the given FilterFusionState to 
     * buffer access with MIV index expression, starting at the top level do loop *top*.
     * Return true if the conversion was successful and false otherwise, if false, nothing
     * was accomplished and one must use another conversion scheme.
     **/
    public boolean tryMIV(JDoLoopStatement top) 
    {
	Phase1 phase1 = new Phase1(top);
	//run phase 1 that will check to see if the conversion can be correctly applied
	// and saves some state in the process.
	boolean passed = phase1.run();
	if (passed) {
	    //assert that the calculated rates match the declared rates...
	    assert fusionState.getFilter().getPushInt() == phase1.getTopLevelPushCount() :
		"Declared push rate does not match calculated rated for ConvertChannelExprMIV";
	    
	    assert fusionState.getFilter().getPopInt() == phase1.getTopLevelPopCount() :
		"Declared pop rate does not match calculated rated for ConvertChannelExprMIV";

	    Phase2 phase2 = new Phase2(top, phase1, 
				       popBuffer, pushBuffer, pushOffset);
	    //run the second phase
	    phase2.run();
	}
	//	else 
	//    System.out.println("** Could not generate MIVs for " + fusionState.getNode().contents);
	
	return passed;
    }    
}

/**
 * Phase 2 of the MIV conversion algorithm.  This class assumes that it is 
 * legal to perform the conversion (tested in phase 1) and that all
 * necessary state for the conversion has been calculated by the first
 * stage.
 * 
 * So, each statement is converted into a buffer access that takes 
 * into account the previous iterations of the outside loop, anything that
 * came before it in for the current iteration of the outside loop, and
 * any iterations of its enclosing loops.
 *
 * @author Michael Gordon
 * 
 */
class Phase2 extends SLIRReplacingVisitor 
{
    private Phase1 phase1;
    private JDoLoopStatement topLevel;
    private JLocalVariable popBuffer;
    private JLocalVariable pushBuffer;
    private int pushOffset;

    public Phase2(JDoLoopStatement topLevel, Phase1 phase1,
		  JLocalVariable popBuffer, 
		  JLocalVariable pushBuffer, int pushOffset) 
    {
	this.topLevel = topLevel;
	this.phase1 = phase1;
	this.pushBuffer = pushBuffer;
	this.popBuffer = popBuffer;
	this.pushOffset = pushOffset;
    }
    /**
     * Run Phase2 of the MIV conversion pass.
     **/
    public void run() 
    {
	topLevel.accept(this);
    }
    
    /**
     * convert a push expression into a MIV access of the push buffer
     */
    public Object visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	JExpression newExp = (JExpression)arg.accept(this);
	
	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, pushBuffer);
	
	//build the MIV index expression
	JExpression rhs = Util.newIntAddExpr(buildAccessExpr(self),
					new JIntLiteral(pushOffset));

	// return a new array access expression
	JArrayAccessExpression pushBufAccess = 
	    new JArrayAccessExpression(null, lhs,
				       rhs);

	return new JAssignmentExpression(null,
					 pushBufAccess,
					 newExp);
    }

    /**
     * make sure we replace expressions of expression statements
     */
    public Object visitExpressionStatement(JExpressionStatement self,
					   JExpression expr) {
	
	//if we have just a pop expression, not nested in anything, then 
	//just remove it, we don't need it anymore!!!!!
	//	if (expr instanceof SIRPopExpression)
	//   return new JEmptyStatement(null, null);
		
	JExpression newExp = (JExpression)expr.accept(this);
	if (newExp!=null && newExp!=expr) {
	    self.setExpression(newExp);
	}
	return self;
    }

    
    /**
     * convert a peek expression into a MIV access of the pop buffer..
     */
    public Object visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	JExpression newExp = (JExpression)arg.accept(this);
	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, popBuffer);
	
	//add the peek index to the index calculated for this peek
	JExpression rhs = 
	    Util.newIntAddExpr(newExp,
			       buildAccessExpr(self));
	
	// return a new array access expression
	return new JArrayAccessExpression(null, lhs,
					  rhs);
    }


    /** build the expression for a buffer access and return the expression **/
    private JExpression buildAccessExpr(JExpression self) 
    {
	HashMap loopExpr = null;
	HashMap topLevelExpr = null;

	//depending on the expression, use the correct state (push or peek/pop)
	if (self instanceof SIRPopExpression ||
	    self instanceof SIRPeekExpression) {
	    loopExpr = phase1.loopPop;
	    topLevelExpr = phase1.topLevelPop;
	} else if (self instanceof SIRPushExpression) {
	    loopExpr = phase1.loopPush;
	    topLevelExpr = phase1.topLevelPush;
	}
	else {
	    assert false : "Expression must be a push, pop, or peek";
	}
	
	//init the expression to 0
	JExpression access = new JIntLiteral(0);
	//find the expression for the inner-most loop containing this expression
	assert phase1.enclosingLoop.containsKey(self);
	JDoLoopStatement enclosing = 
	    (JDoLoopStatement)phase1.enclosingLoop.get(self);

	//add the terms for all the enclosing loops of this expression
	while (enclosing != null) {	    
	    assert loopExpr.containsKey(enclosing);
	    //this term in the access (induction - initValue)*(number_of_buffer_increment)
	    //so tripcount so farm * expression for the loop
	    access = 
		Util.newIntAddExpr(access,
				   Util.newIntMultExpr(Util.newIntSubExpr(new JLocalVariableExpression
									    (null,
									     enclosing.getInduction()),
									    enclosing.getInitValue()),
						       new JIntLiteral(((Integer)loopExpr.get(enclosing)).intValue())));
	    assert phase1.enclosingLoop.containsKey(enclosing);
	    //get the next enclosing loop
	    enclosing = (JDoLoopStatement)phase1.enclosingLoop.get(enclosing);
	}
	

	assert topLevelExpr.containsKey(self);
	//now add the expression that represents everything that executed so far in the top level loop
	access =
	    Util.newIntAddExpr(access,
			       new JIntLiteral(((Integer)topLevelExpr.get(self)).intValue()));
	
	return access;
    }
    

    /**
     * convert a pop expression to a buffer access with MIV expression
     */
    public Object visitPopExpression(SIRPopExpression self,
				   CType tapeType) {

	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, popBuffer);
	
	// return a new array access expression
	return new JArrayAccessExpression(null, lhs,
					  buildAccessExpr(self));
    }
}

/**
 * Phase 1 of the MIV conversion algorithm.  This class calculates
 * the state needed to perform the conversion, which calculating it
 * checks for illegal conditions.
 *
 * The state includes, for each do loop, the number of push/pops in the loop
 * for each expression, the number of pops/push's that come before the outermost
 * loop that it is nested it.
 *
 * @author Michael Gordon
 * 
 */

class Phase1 extends SLIREmptyVisitor 
{
    //the top level do loop
    private JDoLoopStatement topLevel;

    //pop/peek information
    /** the number of pops we have seen so for for the current loop we are 
	in the process of analyzing **/
    private int currentLoopPop;
    /**the number of pops we have seen so far on *this* (one) iteration fo the 
       outer loop**/
    private int currentTopLevelPop;
    /** maps pop/peek to the number of pops that have occured on the current iteration
     * of the outer loop for loops that have already completed execution **/
    public HashMap topLevelPop;
    /** maps do loop to the number of pops that occur on one iteration of the 
	loop **/
    public HashMap loopPop;
    
    //push information
    /** the number of push's we have seen so for for the current loop we are 
	in the process of analyzing **/
    private int currentLoopPush;
    /**the number of push's we have seen so far on *this* (one) iteration fo the 
       outer loop**/
    private int currentTopLevelPush;
    /** maps push to the number of push' that have occured on the current iteration
     * of the outer loop for loops that have already completed execution **/
    public HashMap topLevelPush;
    /** maps do loop to the number of pushs that occur on one iteration of the 
	loop **/
    public HashMap loopPush;

    // > 0 if we are visiting the header of a do loop
    private int doHeader = 0;

    //if > 0 we are nested inside a do loop
    private int doLoopLevel = 0;
    //if > 0 we are inside some control where we cannot see a channel expr
    private int insideControlFlow = 0;
    

    //pop, peek, push -> JDoLoopStatment, the inner most enclosing loop
    public HashMap enclosingLoop;
    //the current loop we are analyzing
    public JDoLoopStatement currentLoop;

    /** create a new Phase 1 with *top* as the outermost loop **/
    public Phase1(JDoLoopStatement top) 
    {
	topLevel = top;
	//init pop/peek state
	currentLoopPop = 0;
	currentTopLevelPop = 0;
	topLevelPop = new HashMap();
	loopPop = new HashMap();
	
	//init push state
	currentLoopPush = 0;
	currentTopLevelPush = 0;	
	topLevelPush = new HashMap();
	loopPush = new HashMap();

	enclosingLoop = new HashMap();

	currentLoop = null;
	
	enclosingLoop.put(currentLoop, null);
	
    }
    /** give that this phase has completed, return the number of pops that
	occur in one iteration of the top level loop */
    public int getTopLevelPopCount() 
    {
	return ((Integer)loopPop.get(topLevel)).intValue();
    }
    
    /** give that this phase has completed, return the number of push's that
	occur in one iteration of the top level loop */
    public int getTopLevelPushCount() 
    {
	return ((Integer)loopPush.get(topLevel)).intValue();
    }
    
    /** Run the 1st phase of the MIV conversion pass **/
    public boolean run()
    {
	if (!StrToRStream.GENERATE_MIVS)
	    return false;
	
	try {
	    topLevel.accept(this);
	    assert insideControlFlow == 0;
	    assert doLoopLevel == 0;
	}
	catch (MIVException e) {
	    //e.printStackTrace();
	    return false;
	}
	return true;
    }

    /**
     * visit a for statement
     */
    public void visitForStatement(JForStatement self,
				  JStatement init,
				  JExpression cond,
				  JStatement incr,
				  JStatement body) {
	//if this is a do loop try to visit it with visitdoloopstatement
	//this method will return true if the doloop is analyzable and it was
	//visited with that method, otherwise assume that it is a normal for loop
	//and visit it
	if (self instanceof JDoLoopStatement && visitDoLoopStatement((JDoLoopStatement)self)) {
	    return;
	}
	//we cannot see a channel expression inside 
	//non-analyzable control flow
	insideControlFlow++;
	if (init != null) {
	    init.accept(this);
	}
	if (cond != null) {
	    cond.accept(this);
	}
	if (incr != null) {
	    incr.accept(this);
	}
	body.accept(this);
	insideControlFlow--;
    }


    /** return true if we passed the tests for an analyzable do loop
	other wise visitforloop will visit this loop **/
    public boolean visitDoLoopStatement(JDoLoopStatement doloop) 
    {
	//the old values for the state, the values before entrance to this loop
	int oldCurrentPop, oldTopLevelPop;
	int oldCurrentPush, oldTopLevelPush;
	//the enlosing loop
	JDoLoopStatement oldCurrentLoop;
	
	//make sure we have static bounds
	if (!doloop.staticBounds()) {
	    return false;
	    //throw new MIVException();
	}
	
	
	if (doloop.getIncrInt() != 1) {
	    //System.out.println("**** Can't generate MIV because loop does not have 1 increment");
	    return false;
	    //throw new MIVException();
	}
	//remember this loops enclosing loop
	enclosingLoop.put(doloop, currentLoop);
	//remember the old enclosing loop
	oldCurrentLoop = currentLoop;
	//set this loop to be the current loop
	currentLoop = doloop;

	//visit the init
	if (doloop.getInitValue() != null) {
	    doloop.getInitValue().accept(this);
	}
	//record that we are in the header of a do loop
	//we don't want to see channel expression in the header
	doHeader ++;
	//visit the cond
	if (doloop.getCondValue() != null) {
	    doloop.getCondValue().accept(this);
	}
	//visit the incr
	if (doloop.getIncrValue() != null) {
	    doloop.getIncrValue().accept(this);
	}
	//not in the head any more
	doHeader--;
	//in a do loop loop
	doLoopLevel++;
	//remember the old state
	oldCurrentPop = currentLoopPop;
	oldTopLevelPop = currentTopLevelPop;
	oldCurrentPush = currentLoopPush;
	oldTopLevelPush = currentTopLevelPush;
	
	//reset the current expression
	currentLoopPop = 0;
	currentLoopPush = 0;

	//visit the body
	doloop.getBody().accept(this);

	//remember this loop's number of pops 
	loopPop.put(doloop, new Integer(currentLoopPop));

	//pass on up the new value that describes the number of pops we have
	//seen so far on *this* iteration of all enclosing loops
	currentTopLevelPop = oldTopLevelPop  + (doloop.getTripCount() * currentLoopPop);
	
	//pass on up this loops number of pops * the trip count
	//this is the number of total pops in this loop, this used so the 
	//outer loop can get a count of how many pops it has
	currentLoopPop = oldCurrentPop + (doloop.getTripCount() * currentLoopPop);


	//remember this loop's number of pushes 
	loopPush.put(doloop, new Integer(currentLoopPush));

	//pass on up the new expr that describes the number of pushes we have
	//seen so far on *this* iteration of all enclosing loops
	currentTopLevelPush = oldTopLevelPush + (doloop.getTripCount() * currentLoopPush);

	//pass on up this loops number of pushes * the trip count
	//this is the number of total pushes in this loop, this used so the 
	//outer loop can get a count of how many pushes it has
	currentLoopPush = oldCurrentPush + (doloop.getTripCount() * currentLoopPush);
		
	doLoopLevel--;
	currentLoop = oldCurrentLoop;
	return true;
    }

    /**
     * peek expression
     */
    public void visitPeekExpression(SIRPeekExpression self,
				    CType tapeType,
				    JExpression arg) {
	//don't want this is a header to for!!
	if (doHeader > 0)
	    throw new MIVException();


	//if (insideControlFlow > 0)
	//    throw new MIVException();

	arg.accept(this);
	//remember the enclosing loop
	enclosingLoop.put(self, currentLoop);
	//remember the number of pops that we have seen so far in loop that have
	//completed...
	topLevelPop.put(self, new Integer(currentTopLevelPop));
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
				   CType tapeType) {
	//don't want this is a header to for!!
	if (doHeader > 0)
	    throw new MIVException();
	//can't see a channel expr inside non-doloop control flow
	if (insideControlFlow > 0)
	    throw new MIVException();

	//remember the enclosing loop
	enclosingLoop.put(self, currentLoop);
	//remember the number of pops that we have seen so far in loop that have
	//completed...
	topLevelPop.put(self, new Integer(currentTopLevelPop));
	//add 1 to the pop count for both the loop and the top level
	currentTopLevelPop += 1;
	currentLoopPop += 1;
    }
    
    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	//can't see a channel expr inside non-doloop control flow
	if (insideControlFlow > 0)
	    throw new MIVException();
	//don't want this is a header to for!!
	if (doHeader > 0)
	    throw new MIVException();

	arg.accept(this);
	//remember the enclosing loop
	enclosingLoop.put(self, currentLoop);
	//remember the number of push's that we have seen so far in loop that have
	//completed...
	topLevelPush.put(self, new Integer(currentTopLevelPush));
	//add 1 to the push count for the current loop and the top level expr
	currentTopLevelPush += 1;
	currentLoopPush += 1;
    }

    

    /**
     ** visit an if statement, the rate on each branch must be equal
     */
    public void visitIfStatement(JIfStatement self,
				 JExpression cond,
				 JStatement thenClause,
				 JStatement elseClause) {
	//visit the conditional
	cond.accept(this);
	//set the old values and the "then" values of the state
	int thenCurrentTopLevelPush = currentTopLevelPush;
	int oldCurrentTopLevelPush = currentTopLevelPush;
	int thenCurrentLoopPush = currentLoopPush;
	int oldCurrentLoopPush = currentLoopPush;
	int thenCurrentTopLevelPop = currentTopLevelPop;
	int oldCurrentTopLevelPop = currentTopLevelPop;
	int thenCurrentLoopPop = currentLoopPop;
	int oldCurrentLoopPop = currentLoopPop;
	
	//visit the then clause
	thenClause.accept(this);
	
	//remember the state after the then clause//
	thenCurrentTopLevelPush = currentTopLevelPush; 
	thenCurrentLoopPush = currentLoopPush;
	thenCurrentTopLevelPop = currentTopLevelPop;
	thenCurrentLoopPop = currentLoopPop;
	
	//reset the state to be before the then clause
	//for the else clause
	currentTopLevelPush = oldCurrentTopLevelPush; 
	currentLoopPush = oldCurrentLoopPush;
	currentTopLevelPop = oldCurrentTopLevelPop;
	currentLoopPop = oldCurrentLoopPop;
	

	if (elseClause != null) {
	    elseClause.accept(this);
	}
	//make sure the rates of the then clause match the current rate
	//which is the rate of the else or the rate before the then...
	if (thenCurrentTopLevelPush != currentTopLevelPush || 
	    thenCurrentLoopPush != currentLoopPush ||
	    thenCurrentTopLevelPop != currentTopLevelPop ||
	    thenCurrentLoopPop != currentLoopPop) {
	    throw new MIVException();
	}
    }

    
    /**
     * visit a while statement, remember that we are in unanalyzable control flow
     */
    public void visitWhileStatement(JWhileStatement self,
				    JExpression cond,
				    JStatement body) {
	insideControlFlow++;
	cond.accept(this);
	body.accept(this);
	insideControlFlow--;
    }

    
    /**
     * visit a label statement, remember that we are in unanalyzable control flow
     */
    public void visitLabeledStatement(JLabeledStatement self,
				      String label,
				      JStatement stmt) {
	insideControlFlow++;
	stmt.accept(this);
	insideControlFlow--;
    }

    
    /**
     * visit a label statement, remember that we are in unanalyzable control flow
     */
    public void visitDoStatement(JDoStatement self,
				 JExpression cond,
				 JStatement body) {
	insideControlFlow++;
	 body.accept(this);
	 cond.accept(this);
	insideControlFlow--;
    }

    /**
     * visit a continue statement, we shouldn't see any breaks inside a do loop
     */
    public void visitContinueStatement(JContinueStatement self,
				       String label) {
	//can't see this inside a doloop
	if (doLoopLevel > 0)
	    throw new MIVException();
    }

    /**
     * we should see any breaks inside of do loops
     */
    public void visitBreakStatement(JBreakStatement self,
				    String label) {
	//can't see this inside a doloop
	if (doLoopLevel > 0)
	    throw new MIVException();
    }
}
/** a class for exceptions throw by phase 1 of the MIV pass **/
class MIVException extends RuntimeException 
{
    public MIVException() 
    {
	super();
    }
}


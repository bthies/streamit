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

*/
public class ConvertChannelExprsMIV {

    private JLocalVariable popBuffer;
    private JLocalVariable pushBuffer;
    private int pushOffset;

    FilterFusionState fusionState;

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
	    this.pushOffset = isInit ? 0 :  next.getPeekBufferSize();
	}
	
	this.popBuffer = current.getBufferVar(null /*this is a filter, so only one previous */,
					      isInit);
	fusionState = current;
    }
    

    public boolean tryMIV(JDoLoopStatement top) 
    {
	Phase1 phase1 = new Phase1(top);
	boolean passed = phase1.run();
	if (passed) {
	    //assert that the calculated rates match the declared rates...
	    assert fusionState.getFilter().getPushInt() == phase1.getTopLevelPushCount() :
		"Declared push rate does not match calculated rated for ConvertChannelExprMIV";
	    
	    assert fusionState.getFilter().getPopInt() == phase1.getTopLevelPopCount() :
		"Declared pop rate does not match calculated rated for ConvertChannelExprMIV";

	    Phase2 phase2 = new Phase2(top, phase1, 
				       popBuffer, pushBuffer, pushOffset);
	    phase2.run();
	}
	else 
	    System.out.println("** Could not generate MIVs for " + fusionState.getNode().contents);
	
	return passed;
    }    
}


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
    
    public void run() 
    {
	topLevel.accept(this);
    }
    
    /**
     * Visits a push expression.
     */
    public Object visitPushExpression(SIRPushExpression self,
				    CType tapeType,
				    JExpression arg) {
	JExpression newExp = (JExpression)arg.accept(this);
	
	// build ref to pop array
	JLocalVariableExpression lhs = 
	    new JLocalVariableExpression(null, pushBuffer);
	
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
     * prints an expression statement
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
     * Visits a peek expression.
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

    private JExpression buildAccessExpr(JExpression self) 
    {
	HashMap loopExpr = null;
	HashMap topLevelExpr = null;

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
	
	
	JExpression access = new JIntLiteral(0);
	
	assert phase1.enclosingLoop.containsKey(self);
	JDoLoopStatement enclosing = 
	    (JDoLoopStatement)phase1.enclosingLoop.get(self);

	while (enclosing != null) {	    
	    assert loopExpr.containsKey(enclosing);
	    //this term in the access (induction - initValue)*(number_of_buffer_increment)
	    access = 
		Util.newIntAddExpr(access,
				   Util.newIntMultExpr(Util.newIntSubExpr(new JLocalVariableExpression
									    (null,
									     enclosing.getInduction()),
									    enclosing.getInitValue()),
						       new JIntLiteral(((Integer)loopExpr.get(enclosing)).intValue())));
	    assert phase1.enclosingLoop.containsKey(enclosing);
	    enclosing = (JDoLoopStatement)phase1.enclosingLoop.get(enclosing);
	}
	

	assert topLevelExpr.containsKey(self);
	access =
	    Util.newIntAddExpr(access,
			       new JIntLiteral(((Integer)topLevelExpr.get(self)).intValue()));
	
	return access;
    }
    

    /**
     * Visits a pop expression.
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


class Phase1 extends SLIREmptyVisitor 
{
    private JDoLoopStatement topLevel;

    //pop/peek information
    private int currentLoopPop;
    private int currentTopLevelPop;
    public HashMap topLevelPop;
    public HashMap loopPop;
    
    //push information
    private int currentLoopPush;
    private int currentTopLevelPush;
    public HashMap topLevelPush;
    public HashMap loopPush;

    // > 0 if we are visiting the header of a do loop
    private int doHeader = 0;

    //if > 0 we are nested inside a do loop
    private int doLoopLevel = 0;
    //if > 0 we are inside some control where we cannot see a channel expr
    private int insideControlFlow = 0;
    

    //pop, peek, JDoLoopStatment
    public HashMap enclosingLoop;
    public JDoLoopStatement currentLoop;

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
    
    public int getTopLevelPopCount() 
    {
	return ((Integer)loopPop.get(topLevel)).intValue();
    }
    
    public int getTopLevelPushCount() 
    {
	return ((Integer)loopPush.get(topLevel)).intValue();
    }
    
    
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
     * prints a for statement
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


    //return true if we passed the tests for an analyzable do loop
    //other wise visit for loop will visit this loop
    public boolean visitDoLoopStatement(JDoLoopStatement doloop) 
    {
	int oldCurrentPop, oldTopLevelPop;
	int oldCurrentPush, oldTopLevelPush;
	
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
	
	enclosingLoop.put(doloop, currentLoop);
	oldCurrentLoop = currentLoop;
	currentLoop = doloop;

	if (doloop.getInitValue() != null) {
	    doloop.getInitValue().accept(this);
	}
	doHeader ++;
	if (doloop.getCondValue() != null) {
	    doloop.getCondValue().accept(this);
	}
	if (doloop.getIncrValue() != null) {
	    doloop.getIncrValue().accept(this);
	}
	doHeader--;
	doLoopLevel++;
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

	//pass on up the new  that describes the number of pops we have
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
     * Visits a peek expression.
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
	
	enclosingLoop.put(self, currentLoop);
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

	
	enclosingLoop.put(self, currentLoop);
	topLevelPop.put(self, new Integer(currentTopLevelPop));
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
	enclosingLoop.put(self, currentLoop);
	topLevelPush.put(self, new Integer(currentTopLevelPush));
	
	currentTopLevelPush += 1;
	currentLoopPush += 1;
    }

    

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
				 JExpression cond,
				 JStatement thenClause,
				 JStatement elseClause) {
	cond.accept(this);
	
	int thenCurrentTopLevelPush = currentTopLevelPush;
	int oldCurrentTopLevelPush = currentTopLevelPush;
	int thenCurrentLoopPush = currentLoopPush;
	int oldCurrentLoopPush = currentLoopPush;
	int thenCurrentTopLevelPop = currentTopLevelPop;
	int oldCurrentTopLevelPop = currentTopLevelPop;
	int thenCurrentLoopPop = currentLoopPop;
	int oldCurrentLoopPop = currentLoopPop;
	
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
	
	if (thenCurrentTopLevelPush != currentTopLevelPush || 
	    thenCurrentLoopPush != currentLoopPush ||
	    thenCurrentTopLevelPop != currentTopLevelPop ||
	    thenCurrentLoopPop != currentLoopPop) {
	    throw new MIVException();
	}
    }

    
    /**
     * prints a while statement
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
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
				      String label,
				      JStatement stmt) {
	insideControlFlow++;
	stmt.accept(this);
	insideControlFlow--;
    }

    
    /**
     * prints a do statement
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
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
				       String label) {
	//can't see this inside a doloop
	if (doLoopLevel > 0)
	    throw new MIVException();
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
				    String label) {
	//can't see this inside a doloop
	if (doLoopLevel > 0)
	    throw new MIVException();
    }
}

class MIVException extends RuntimeException 
{
    public MIVException() 
    {
	super();
    }
}


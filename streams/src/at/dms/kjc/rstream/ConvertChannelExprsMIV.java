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
	    //change me!!!
	    Phase2 phase2 = new Phase2(top, phase1, 
				       popBuffer, pushBuffer, pushOffset);
	    phase2.run();
	}
	else 
	    System.out.println("failed");
	
	//NOTE:   change !!!!!!!!
	return false;
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
	    loopExpr = phase1.loopPopExpr;
	    topLevelExpr = phase1.topLevelPopExpr;
	} else if (self instanceof SIRPushExpression) {
	    loopExpr = phase1.loopPushExpr;
	    topLevelExpr = phase1.topLevelPushExpr;
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
	    access = 
		Util.newIntAddExpr(access,
				   Util.newIntMultExpr(new JLocalVariableExpression(null,
									   enclosing.getInduction()),
						    (JExpression)loopExpr.get(enclosing)));
	    assert phase1.enclosingLoop.containsKey(enclosing);
	    enclosing = (JDoLoopStatement)phase1.enclosingLoop.get(enclosing);
	}
	

	assert topLevelExpr.containsKey(self);
	access =
	    Util.newIntAddExpr(access,
			       (JExpression)topLevelExpr.get(self));
	
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
    private JExpression currentLoopPopExpr;
    private JExpression currentTopLevelPopExpr;
    public HashMap topLevelPopExpr;
    public HashMap loopPopExpr;
    
    //push information
    private JExpression currentLoopPushExpr;
    private JExpression currentTopLevelPushExpr;
    public HashMap topLevelPushExpr;
    public HashMap loopPushExpr;

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
	currentLoopPopExpr = new JIntLiteral(0);
	currentTopLevelPopExpr = new JIntLiteral(0);
	topLevelPopExpr = new HashMap();
	loopPopExpr = new HashMap();
	
	//init push state
	currentLoopPushExpr = new JIntLiteral(0);
	currentTopLevelPushExpr = new JIntLiteral(0);
	topLevelPushExpr = new HashMap();
	loopPushExpr = new HashMap();

	enclosingLoop = new HashMap();

	currentLoop = null;
	
	enclosingLoop.put(currentLoop, null);
	
    }
    

    public boolean run()
    {
	try {
	    topLevel.accept(this);
	}
	catch (MIVException e) {
	    e.printStackTrace();
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
	JExpression oldCurrentPopExpr, oldTopLevelPopExpr;
	JExpression oldCurrentPushExpr, oldTopLevelPushExpr;
	
	JDoLoopStatement oldCurrentLoop;


	//if not a do loop, then crap out
	if (!(self instanceof JDoLoopStatement))
	    throw new MIVException();
	//make sure we have static bounds
	if (!((JDoLoopStatement)self).staticBounds())
	    throw new MIVException();
	
	JDoLoopStatement doloop = (JDoLoopStatement)self;

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
	oldCurrentPopExpr = currentLoopPopExpr;
	oldTopLevelPopExpr = currentTopLevelPopExpr;
	oldCurrentPushExpr = currentLoopPushExpr;
	oldTopLevelPushExpr = currentTopLevelPushExpr;

	
	//reset the current expression
	currentLoopPopExpr = new JIntLiteral(0);
	currentLoopPushExpr = new JIntLiteral(0);


	//visit the body
	body.accept(this);

	//remember this loop's number of pops 
	loopPopExpr.put(doloop, currentLoopPopExpr);

	//pass on up the new expr that describes the number of pops we have
	//seen so far on *this* iteration of all enclosing loops
	currentTopLevelPopExpr = 
	    Util.newIntAddExpr(oldTopLevelPopExpr,
			       Util.newIntMultExpr(new JIntLiteral(doloop.getTripCount()),
						   currentLoopPopExpr));

	//pass on up this loops number of pops * the trip count
	//this is the number of total pops in this loop, this used so the 
	//outer loop can get a count of how many pops it has
	currentLoopPopExpr = 
	    Util.newIntAddExpr(oldCurrentPopExpr,
			       Util.newIntMultExpr(new JIntLiteral(doloop.getTripCount()),
						   currentLoopPopExpr));


	//remember this loop's number of pushes 
	loopPushExpr.put(doloop, currentLoopPushExpr);

	//pass on up the new expr that describes the number of pushes we have
	//seen so far on *this* iteration of all enclosing loops
	currentTopLevelPushExpr = 
	    Util.newIntAddExpr(oldTopLevelPushExpr,
			       Util.newIntMultExpr(new JIntLiteral(doloop.getTripCount()),
						   currentLoopPushExpr));

	//pass on up this loops number of pushes * the trip count
	//this is the number of total pushes in this loop, this used so the 
	//outer loop can get a count of how many pushes it has
	currentLoopPushExpr = 
	    Util.newIntAddExpr(oldCurrentPushExpr,
			       Util.newIntMultExpr(new JIntLiteral(doloop.getTripCount()),
						   currentLoopPushExpr));

	
	doLoopLevel--;
	currentLoop = oldCurrentLoop;
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
	//can't see a channel expr inside non-doloop control flow
	if (insideControlFlow > 0)
	    throw new MIVException();

	arg.accept(this);
	
	enclosingLoop.put(self, currentLoop);
	topLevelPopExpr.put(self, currentTopLevelPopExpr);
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
	topLevelPopExpr.put(self, currentTopLevelPopExpr);
	currentTopLevelPopExpr =
	    Util.newIntAddExpr(currentTopLevelPopExpr,
			       new JIntLiteral(1));
	currentLoopPopExpr = 
	    Util.newIntAddExpr(currentLoopPopExpr,
			       new JIntLiteral(1));
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
	topLevelPushExpr.put(self, currentTopLevelPushExpr);
	currentTopLevelPushExpr =
	    Util.newIntAddExpr(currentTopLevelPushExpr,
			       new JIntLiteral(1));
	currentLoopPushExpr = 
	    Util.newIntAddExpr(currentLoopPushExpr,
			       new JIntLiteral(1));
	
    }

    

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
				 JExpression cond,
				 JStatement thenClause,
				 JStatement elseClause) {
	assert false;
	
	cond.accept(this);
	
	thenClause.accept(this);
	
	if (elseClause != null) {
	    elseClause.accept(this);
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


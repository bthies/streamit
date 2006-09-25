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

class ConvertChannelExprsPhase1 extends SLIREmptyVisitor 
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
    public HashMap<JExpression, Integer> topLevelPop;
    /** maps do loop to the number of pops that occur on one iteration of the 
        loop **/
    public HashMap<JDoLoopStatement, Integer> loopPop;
    
    //push information
    /** the number of push's we have seen so for for the current loop we are 
        in the process of analyzing **/
    private int currentLoopPush;
    /**the number of push's we have seen so far on *this* (one) iteration fo the 
       outer loop**/
    private int currentTopLevelPush;
    /** maps push to the number of push' that have occured on the current iteration
     * of the outer loop for loops that have already completed execution **/
    public HashMap<JExpression, Integer> topLevelPush;
    /** maps do loop to the number of pushs that occur on one iteration of the 
        loop **/
    public HashMap<JDoLoopStatement, Integer> loopPush;

    // > 0 if we are visiting the header of a do loop
    private int doHeader = 0;

    //if > 0 we are nested inside a do loop
    private int doLoopLevel = 0;
    //if > 0 we are inside some control where we cannot see a channel expr
    private int insideControlFlow = 0;
    

    //pop, peek, push -> JDoLoopStatment, the inner most enclosing loop
    public HashMap<JPhylum, JDoLoopStatement> enclosingLoop;
    //the current loop we are analyzing
    public JDoLoopStatement currentLoop;

    /** create a new Phase 1 with *top* as the outermost loop **/
    public ConvertChannelExprsPhase1(JDoLoopStatement top) 
    {
        topLevel = top;
        //init pop/peek state
        currentLoopPop = 0;
        currentTopLevelPop = 0;
        topLevelPop = new HashMap<JExpression, Integer>();
        loopPop = new HashMap<JDoLoopStatement, Integer>();
    
        //init push state
        currentLoopPush = 0;
        currentTopLevelPush = 0;    
        topLevelPush = new HashMap<JExpression, Integer>();
        loopPush = new HashMap<JDoLoopStatement, Integer>();

        enclosingLoop = new HashMap<JPhylum, JDoLoopStatement>();

        currentLoop = null;
    
        enclosingLoop.put(currentLoop, null);
    
    }
    /** give that this phase has completed, return the number of pops that
        occur in one iteration of the top level loop */
    public int getTopLevelPopCount() 
    {
        return loopPop.get(topLevel).intValue();
    }
    
    /** give that this phase has completed, return the number of push's that
        occur in one iteration of the top level loop */
    public int getTopLevelPushCount() 
    {
        return loopPush.get(topLevel).intValue();
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
        currentTopLevelPop += self.getNumPop();
        currentLoopPop += self.getNumPop();
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

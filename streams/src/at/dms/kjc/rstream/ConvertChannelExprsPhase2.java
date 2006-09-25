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
class ConvertChannelExprsPhase2 extends SLIRReplacingVisitor 
{
    private ConvertChannelExprsPhase1 phase1;
    private JDoLoopStatement topLevel;
    private JLocalVariable popBuffer;
    private JLocalVariable pushBuffer;
    private int pushOffset;

    public ConvertChannelExprsPhase2(JDoLoopStatement topLevel, 
                                     ConvertChannelExprsPhase1 phase1,
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
        // RMR { not sure why the following was commented out before
        // (it generated statements with no effect)
        if (expr instanceof SIRPopExpression)
            return new JEmptyStatement(null, null);
        // } RMR
        
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
        HashMap<JDoLoopStatement, Integer> loopExpr = null;
        HashMap<JExpression, Integer> topLevelExpr = null;

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
            phase1.enclosingLoop.get(self);

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
                                                       new JIntLiteral(loopExpr.get(enclosing).intValue())));
            assert phase1.enclosingLoop.containsKey(enclosing);
            //get the next enclosing loop
            enclosing = phase1.enclosingLoop.get(enclosing);
        }
    

        assert topLevelExpr.containsKey(self);
        //now add the expression that represents everything that executed so far in the top level loop
        access =
            Util.newIntAddExpr(access,
                               new JIntLiteral(topLevelExpr.get(self).intValue()));
    
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

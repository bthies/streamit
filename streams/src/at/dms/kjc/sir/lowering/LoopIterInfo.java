package at.dms.kjc.sir.lowering;
import at.dms.kjc.*;

import java.util.*;

/**
 * Information about loops with fixed number of iterations.
 * 
 * <p>This class was originally part of Unroller but is now used by other classes.
 * It deals with loops that are run for a fixed number of iterations.</p>
 *
 * <p>For our purposes, so far, we identifu a loop with a fixed number of iterations
 * by looking for a <code>for</code> loop where a local variable is initialized to a constant,
 * that same local variable is added to / subtracted from / multiplied by / divided by a constant,
 * and the termination condition is given by comparison to a constant.</p>
 * 
 * <p>Ths code in this class does <b>not</b> check whether the induction variable is modified
 * in the loop body.  That check is left for the user to perform.</p>
 * 
 * @author Jasper, minorly munged by Allyn
 *
 */
public class LoopIterInfo  implements Constants {
    /**
     * The induction variable in the loop.
     * @return 
     */
    public JLocalVariable getVar() {
        return var;
    }
    /**
     * The induction variable in the loop.
     */
    private JLocalVariable var;

    /**
     * The initial value of the induction variable.
     * @return 
     */
    public int getInitVal() {
        return initVal;
    }
    /**
     * The initial value of the induction variable.
     */
    private int initVal;

    /**
     * The one past final value of the induction variable that would cause looping.
     * @return 
     */
    public int getFinalVal() {
        return finalVal;
    }
    /**
     * The one past final value of the induction variable that would cause looping.
     */
    private int finalVal;

    /**
     * The operation that is being used to change the induction variable.
     * @return 
     */
    public int getOper() {
        return oper;
    }
    /**
     * The operation that is being used to change the induction variable.
     */
    private int oper;

    /**
     * The increment.
     * @return 
     */
    public int getIncrVal() {
        return incrVal;
    }
    /**
     * The increment.
     */
    private int incrVal;
    
    /**
     * Whether the induction variable is declared in the initialization statement
     * @return 
     */
    public boolean getIsDeclaredInInit() {
        return isDeclaredInInit;
    }
    /**
     * Whether the induction variable is declared in the initialization statement
     */
    private boolean isDeclaredInInit;
    
    /**
     * Let getLoopInfo deal with creating this structure
     * @param var          The induction variable
     * @param initVal      Initial value
     * @param finalVal     Final value
     * @param oper         Operation used to change the value
     * @param incrVal      value of the increment
     * @param isDeclaredInInit true if the induction variable if declared in the 
     *                     init postion of the loop, false otherwise.
     */
    private LoopIterInfo(JLocalVariable var,
                      int initVal, 
                      int finalVal, 
                      int oper, 
                      int incrVal,
                      boolean isDeclaredInInit) {
        this.var = var;
        this.initVal = initVal;
        this.finalVal = finalVal;
        this.oper = oper;
        this.incrVal = incrVal;
        this.isDeclaredInInit = isDeclaredInInit;
    }

    /**
     * Determines if integer value <code>counter</code> is within the loop bounds
     * given by <code>info</code>.
     * 
     * @param counter
     * @param info
     * @return            true if in range else false.
     */    
    public static boolean inRange(int counter, LoopIterInfo info) {
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
            assert false: "Can only deal with add/sub/mul/div increments for now.";
            // dummy value
            return false;
        }
    }
    
    /**
     * Given the UnrollInfo <code>info</code> and that <code>counter</code> was the old
     * value of the count, returns the new value of the count for one loop iteration.
     * 
     * <p>Does not check whether new value is in bounds.</p>
     * @param counter 
     * @param info 
     * @return 
     */
    public static int incrementCounter(int counter, LoopIterInfo info) {
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
            assert false: "Can only deal with add/sub/mul/div increments for now.";
            // dummy value
            return 0;
        }
    }

    /**
     * Return an assignment statement (ExpressionStatement(AssignmentExpression)) to 
     * increment (+,-,*,/) the induction variable by <code>num</code>.
     * @param info
     * @param num
     * @return        statement updating the induction variable.
     */
    public static JStatement makeIncrAssignment(LoopIterInfo info,int num) {
        JLocalVariableExpression var=new JLocalVariableExpression(null,info.var);
        JAssignmentExpression incr=new JAssignmentExpression(null,var,null);
        incr.setRight(makeIncreased(info,num));
        return new JExpressionStatement(null,incr,null);
    }

    
    /**
     * Return a JExpression that corresponds performing the loop increment operation
     * <code>num</code> times on the loop induction variable.
     * 
     * For some reason the caller had to deal with the stride.
     * 
     * Used in unrolling loops.
     * 
     * @param info      the LoopIterInfo
     * @param num
     * @return          code for inductionVariable + num   (or -, *, / num).          
     */
    public static JExpression makeIncreased(LoopIterInfo info,int num) {
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
            assert false: "Can only deal with add/sub/mul/div increments for now.";
            return null;
        }
    }
 
    /**
     * Get unroll info for this loop.  
     * 
     * <p>Right now, we check that:</p>
     *
     *  <br/>1. the initialization is an assignemnt of a constant to a variable
     *          or the declaration of a variable with a constant initial value.
     *  <br/>2. the condition is a relational less-than test of the var and a const
     *  <br/>3. the incr is addition or multiplication or div by a const. (use +=1, not ++)
     *  <br/>4. the variable is an integer
     *
     *  <p>We do not check that the induction variable is unmodified in
     *  the loop.</p>  
     *
     * <p>This will return <code>null</code> if the loop can not be unrolled.
     * </p> 
     *
     * @param init   the initialization statement of the <code>for</code> loop
     * @param cond   the termination condition expression
     * @param incr   the statement updating the induction variable of the loop.
     * @param body   the loop body (not currently used)
     * @param values  can specify the initial value of the induction variable 
     *                if it is not set up in the init statement (but setting up
     *                in the init statement will override)
     * @param constants can specify the initial value of the induction variable 
     *                if the init statement assigns to a variable but that variable
     *                is not the induction variable (ask Jasper I don't know --- AD)
     *                
     * @return a LoopIterInfo if the loop has a constant trip count, else null.
     */
    public static LoopIterInfo getLoopInfo(JStatement init,
                                           JExpression cond,
                                           JStatement incr,
                                           JStatement body,
                                           Map <JLocalVariable, JExpression> values,
                                           Map <JLocalVariable,JLiteral> constants) {
        try {
            JLocalVariable var;
            int initVal=0;
            int finalVal=0;
            boolean isDeclaredInInit = false;
            //boolean incrementing;
            // inspect condition...
            JRelationalExpression condExpr = (JRelationalExpression)cond;
            int relation=condExpr.getOper();
            if (! (condExpr.getLeft() instanceof JLocalVariableExpression)) {
                throw new Exception("Left hand side of conditional is not a local variable");
            }
            var=((JLocalVariableExpression)condExpr.getLeft()).getVariable();
            if(init instanceof JExpressionListStatement || init instanceof JExpressionStatement) {
                JAssignmentExpression initExpr;
                if (init instanceof JExpressionListStatement) {
                    if (((JExpressionListStatement)init).getExpressions().length != 1) {
                        throw new Exception("Can't decipher initialization");
                    }
                    initExpr = (JAssignmentExpression)
                        ((JExpressionListStatement)init).getExpression(0);
                } else {
                    initExpr = (JAssignmentExpression)((JExpressionStatement)init).getExpression();
                }
                JExpression initialRExpr = null;
                if(((JLocalVariableExpression)initExpr.getLeft()).getVariable()==var)
                    initialRExpr = initExpr.getRight();
                else if(values.containsKey(var))
                    initialRExpr = values.get(var);
                else if(constants.containsKey(var))
                    initialRExpr = constants.get(var);
                else {
                    throw new Exception("Not Constant!");
                }
                if (! (initialRExpr instanceof JIntLiteral)) {
                    throw new Exception("Not Constant!");
                } else {
                    initVal = ((JIntLiteral)initialRExpr).intValue();
                }
            } else if (init instanceof JVariableDeclarationStatement &&
                    ((JVariableDeclarationStatement)init).getVars()[0].getIdent().equals(var.getIdent())
                    && ((JVariableDeclarationStatement)init).getVars()[0].getValue() instanceof JIntLiteral) {
                initVal = ((JIntLiteral)((JVariableDeclarationStatement)init).getVars()[0].getValue()).intValue();
                isDeclaredInInit = true;
            } else if(values.containsKey(var)) {
                initVal=((JIntLiteral)values.get(var)).intValue();
            } else {
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
            return new LoopIterInfo(var, initVal, finalVal, oper, incrVal, isDeclaredInInit);
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
     * Returns how many times a for loop with unroll info <code>info</code> will
     * execute.  
     * 
     * Returns -1 if the input is null.
     */
    public static int getNumIterations(LoopIterInfo info) {
        // if we didn't get any unroll info, return -1
        if (info==null) { return -1; }
        // get the initial value of the counter
        int counter = info.initVal;
        // track number of executions
        int result = 0;
        // simulate execution of the loop...
        while (inRange(counter, info)) {
            // increment counters
            counter = incrementCounter(counter, info);
            result++;
        }
        return result;
    }
}

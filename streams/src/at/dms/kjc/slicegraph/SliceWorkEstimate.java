package at.dms.kjc.slicegraph;

import java.util.HashSet;
import java.util.Set;

import at.dms.kjc.CClassType;
import at.dms.kjc.CStdType;
import at.dms.kjc.CType;
import at.dms.kjc.JArrayAccessExpression;
import at.dms.kjc.JAssignmentExpression;
import at.dms.kjc.JBinaryExpression;
import at.dms.kjc.JBitwiseExpression;
import at.dms.kjc.JBlock;
import at.dms.kjc.JBreakStatement;
import at.dms.kjc.JCompoundAssignmentExpression;
import at.dms.kjc.JConditionalExpression;
import at.dms.kjc.JContinueStatement;
import at.dms.kjc.JDivideExpression;
import at.dms.kjc.JDoStatement;
import at.dms.kjc.JEqualityExpression;
import at.dms.kjc.JExpression;
import at.dms.kjc.JFieldAccessExpression;
import at.dms.kjc.JForStatement;
import at.dms.kjc.JFormalParameter;
import at.dms.kjc.JIfStatement;
import at.dms.kjc.JLocalVariableExpression;
import at.dms.kjc.JMethodCallExpression;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JNameExpression;
import at.dms.kjc.JPhylum;
import at.dms.kjc.JPostfixExpression;
import at.dms.kjc.JPrefixExpression;
import at.dms.kjc.JRelationalExpression;
import at.dms.kjc.JReturnStatement;
import at.dms.kjc.JShiftExpression;
import at.dms.kjc.JStatement;
import at.dms.kjc.JSwitchGroup;
import at.dms.kjc.JSwitchStatement;
import at.dms.kjc.JUnaryExpression;
import at.dms.kjc.JWhileStatement;
import at.dms.kjc.SLIREmptyVisitor;
import at.dms.kjc.sir.SIRFilter;
import at.dms.kjc.sir.SIRIdentity;
import at.dms.kjc.sir.SIRPeekExpression;
import at.dms.kjc.sir.SIRPopExpression;
import at.dms.kjc.sir.SIRPrintStatement;
import at.dms.kjc.sir.SIRPushExpression;
import at.dms.kjc.sir.lowering.Propagator;
import at.dms.kjc.sir.lowering.Unroller;
import at.dms.kjc.sir.lowering.partition.WorkConstants;
import at.dms.kjc.sir.lowering.partition.WorkEstimate.WorkVisitor;

public class SliceWorkEstimate extends SLIREmptyVisitor implements
        WorkConstants {
    private Set<JMethodDeclaration> methodsBeingProcessed;
    
    /**
     * An estimate of the amount of work found by this filter.
     */
    private long work;
    
    private Slice slice;
    
    private SliceWorkEstimate(Slice slice) {
        this.work = 0;
        this.slice = slice;
        methodsBeingProcessed = new HashSet<JMethodDeclaration>();
    }
    
    private SliceWorkEstimate(Slice slice, Set<JMethodDeclaration> methodsBeingProcessed) {
        this.work = 0;
        this.slice = slice;
        this.methodsBeingProcessed = methodsBeingProcessed;
    }

    
    /**
     * Returns estimate of work function in <pre>filter</pre> multiplied by the steady-state
     * multiplicity.
     */
    public static long getWork(Slice slice) {
        // if no work function (e.g., identity filters?) return 0
        if (slice.getFirstFilter().isPredefined()) {
            return 0;
        } else if (slice.getFirstFilter().getFilter().getWork()==null) {
            //System.err.println("this filter has null work function: " + filter);
            return 0;
        } else {
                long work = getWork(slice, slice.getFirstFilter().getFilter().getWork()) *                
                slice.getFirstFilter().getFilter().getSteadyMult();
                System.out.println(slice + ": " + work);    
                return work;
        }
        
        //TODO: Don't forget to multiply by steady mult
    }

    /**
     * Returns estimate of work in <pre>node</pre> of <pre>filter</pre>, use on first call only.
     */
    private static long getWork(Slice slice, JPhylum node) {
        SliceWorkEstimate visitor = new SliceWorkEstimate(slice);
        node.accept(visitor);
        return visitor.work;
    }
    
    /**
     * Returns estimate of work in <pre>node</pre> of <pre>filter</pre>, use on internal calls.
     */
    private static long getWork(Slice slice, JPhylum node, Set<JMethodDeclaration> methodsBeingProcessed ) {
        SliceWorkEstimate visitor = new SliceWorkEstimate(slice, methodsBeingProcessed);
        node.accept(visitor);
        return visitor.work;
    }
    
    private JMethodDeclaration findMethod(String name) 
    {
        JMethodDeclaration[] methods = slice.getFirstFilter().getFilter().getMethods();
        for (int i = 0; i < methods.length; i++)
            {
                JMethodDeclaration method = methods[i];
                if (method.getName().equals(name))
                    return method;
            }
        return null;
    }

    /* still need to handle:
       public void visitCastExpression(JCastExpression self,
       JExpression expr,
       CType type) {}

       public void visitUnaryPromoteExpression(JUnaryPromote self,
       JExpression expr,
       CType type) {}
    */

    /**
     * SIR NODES.
     */

    /**
     * Visits a peek expression.
     */
    public void visitPeekExpression(SIRPeekExpression self,
                                    CType tapeType,
                                    JExpression arg) {
        super.visitPeekExpression(self, tapeType, arg);
        work += PEEK;
    }

    /**
     * Visits a pop expression.
     */
    public void visitPopExpression(SIRPopExpression self,
                                   CType tapeType) {
        super.visitPopExpression(self, tapeType);
        work += POP;
    }

    /**
     * Visits a print statement.
     */
    public void visitPrintStatement(SIRPrintStatement self,
                                    JExpression arg) {
        super.visitPrintStatement(self, arg);
        work += PRINT;
    }

    /**
     * Visits a push expression.
     */
    public void visitPushExpression(SIRPushExpression self,
                                    CType tapeType,
                                    JExpression arg) {
        super.visitPushExpression(self, tapeType, arg);
        work += PUSH;
    }

    /*

    /**
    * KJC NODES.
    */

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        //System.err.println("WARNING:  Estimating work in loop, assume N=" +
        //LOOP_COUNT);
        long oldWork = work;
        super.visitWhileStatement(self, cond, body);
        long newWork = work;
        work = oldWork + LOOP_COUNT * (newWork - oldWork);
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        super.visitSwitchStatement(self, expr, body);
        work += SWITCH;
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        super.visitReturnStatement(self, expr);
        // overhead of returns is folded into method call overhead
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {

        // always count the work in the conditional
        cond.accept(this);

        // get the work in the then and else clauses and average
        // them...
        long thenWork = SliceWorkEstimate.getWork(slice, thenClause, methodsBeingProcessed);
        long elseWork;
        if (elseClause != null) {
            elseWork = SliceWorkEstimate.getWork(slice, elseClause, methodsBeingProcessed);
        } else {
            elseWork = 0;
        }

        work += IF + (thenWork + elseWork) / 2;
    }

    /**
     * prints a for statement
     */
    public void visitForStatement(JForStatement self,
                                  JStatement init,
                                  JExpression cond,
                                  JStatement incr,
                                  JStatement body) {
        if (init != null) {
            init.accept(this);
        }
        // try to determine how many times the loop executes
        int loopCount = Unroller.getNumExecutions(init, cond, incr, body);
        if (loopCount==-1) {
            //System.err.println("WARNING:  Estimating work in loop, assume N=" +
            //LOOP_COUNT);
            loopCount = LOOP_COUNT;
        }
        long oldWork = work;
        if (cond != null) {
            cond.accept(this);
        }
        if (incr != null) {
            incr.accept(this);
        }
        body.accept(this);
        long newWork = work;
        work = oldWork + loopCount * (newWork - oldWork);
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        //System.err.println("WARNING:  Estimating work in loop, assume N=" +
        //LOOP_COUNT);
        long oldWork = work;
        super.visitDoStatement(self, cond, body);
        long newWork = work;
        work = oldWork + LOOP_COUNT * (newWork - oldWork);
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
        super.visitContinueStatement(self, label);
        work += CONTINUE;
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
        super.visitBreakStatement(self, label);
        work += BREAK;
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * Adds to work estimate an amount for an arithmetic op of type
     * expr.  Assumes <pre>expr</pre> is integral unless the type is explicitly
     * float or double.
     */
    private void countArithOp(JExpression expr) {
        if (expr.getType()==CStdType.Float ||
            expr.getType()==CStdType.Double) {
            int add=FLOAT_ARITH_OP;
            if(expr instanceof JDivideExpression)
                add*=16;
            work += add;
        } else {
            work += INT_ARITH_OP;
        }
    }

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr) {
        super.visitUnaryPlusExpression(self, expr);
        countArithOp(self);
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr) {
        super.visitUnaryMinusExpression(self, expr);
        countArithOp(self);
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        super.visitBitwiseComplementExpression(self, expr);
        countArithOp(self);
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        super.visitLogicalComplementExpression(self, expr);
        countArithOp(self);
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
        super.visitShiftExpression(self, oper, left, right);
        countArithOp(self);
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
        super.visitRelationalExpression(self, oper, left, right);
        countArithOp(self);
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
        super.visitPrefixExpression(self, oper, expr);
        countArithOp(self);
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
        super.visitPostfixExpression(self, oper, expr);
        countArithOp(self);
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) {
        super.visitNameExpression(self, prefix, ident);
        work += MEMORY_OP;
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
        super.visitBinaryExpression(self, oper, left, right);
        countArithOp(self);
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        super.visitMethodCallExpression(self, prefix, ident, args);
        // Known values from profiling RAW code:
        if (ident.equals("acos")) work += 515;
        else if (ident.equals("acosh")) work += 665;
        else if (ident.equals("acosh")) work += 665;
        else if (ident.equals("asin")) work += 536;
        else if (ident.equals("asinh")) work += 578;
        else if (ident.equals("atan")) work += 195;
        else if (ident.equals("atan2")) work += 272;
        else if (ident.equals("atanh")) work += 304;
        else if (ident.equals("ceil")) work += 47;
        else if (ident.equals("cos")) work += 120;
        else if (ident.equals("cosh")) work += 368;
        else if (ident.equals("exp")) work += 162;
        else if (ident.equals("expm1")) work += 220;
        else if (ident.equals("floor")) work += 58;
        else if (ident.equals("fmod")) work += 147;
        else if (ident.equals("frexp")) work += 60;
        else if (ident.equals("log")) work += 146;
        else if (ident.equals("log10")) work += 212;
        else if (ident.equals("log1p")) work += 233;
        else if (ident.equals("modf")) work += 41;
        else if (ident.equals("pow")) work += 554;
        else if (ident.equals("sin")) work += 97;
        else if (ident.equals("sinh")) work += 303;
        else if (ident.equals("sqrt")) work += 297;
        else if (ident.equals("tan")) work += 224;
        else if (ident.equals("tanh")) work += 288;
        // not from profiling: round(x) is currently macro for floor((x)+0.5)
        else if (ident.equals("round")) work += (58 + FLOAT_ARITH_OP);
        // not from profiling: just stuck in here to keep compilation of gmti 
        // from spewing warnings.
        else if (ident.equals("abs")) work += 60;
        else if (ident.equals("max")) work += 60;
        else if (ident.equals("min")) work += 60;
        else
            {
                JMethodDeclaration target = findMethod(ident);
                if (target != null) {
                    target.accept(this);
                } else {
                    System.err.println("Warning:  Work estimator couldn't find target method \"" + ident + "\"" + "\n" + 
                                       "   Will assume constant work overhead of " + WorkConstants.UNKNOWN_METHOD_CALL);
                    work += UNKNOWN_METHOD_CALL;
                }
            }
        work += METHOD_CALL_OVERHEAD;
    }

    /**
     * only exists here to abort recursive calls.
     */
    @Override
    public void visitMethodDeclaration(JMethodDeclaration self,
            int modifiers,
            CType returnType,
            String ident,
            JFormalParameter[] parameters,
            CClassType[] exceptions,
            JBlock body) {
        if (methodsBeingProcessed.contains(self)) {
            System.err.println("Work estimator may underestimate for recursive call to " + self);
        } else {
            methodsBeingProcessed.add(self);
            super.visitMethodDeclaration(self, modifiers, returnType, ident, parameters, exceptions, body);
            methodsBeingProcessed.remove(self);
        }
    }
    
    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
        super.visitEqualityExpression(self, equal, left, right);
        countArithOp(self);
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
        super.visitConditionalExpression(self, cond, left, right);
        work += IF;
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
        super.visitCompoundAssignmentExpression(self, oper, left, right);
        // no work count for assignments, as most arith ops, memory
        // ops, etc., have a destination that takes care of the assign
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident) {
        super.visitFieldExpression(self, left, ident);
        work += MEMORY_OP;
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        super.visitBitwiseExpression(self, oper, left, right);
        countArithOp(self);
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {
        // try to leave out const prop remnants
        if (!(left instanceof JLocalVariableExpression &&
              ((JLocalVariableExpression)left).getVariable().getIdent().indexOf(Propagator.TEMP_VARIABLE_BASE)!=-1)) {
            super.visitAssignmentExpression(self, left, right);
        }
        // no work count for assignments, as most arith ops, memory
        // ops, etc., have a destination that takes care of the assign
    }

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        super.visitArrayAccessExpression(self, prefix, accessor);
        // the work estimate gets worse (e.g. for beamformer 4x4) if
        // we include array expressions, oddly enough.
        work += 0;
    }

}


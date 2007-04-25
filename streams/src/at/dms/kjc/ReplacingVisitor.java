package at.dms.kjc;

import java.util.*;
import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.lir.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This class descends through the tree, and tests if any of the
 * returned STATEMENTS or EXPRESSIONS are different from old ones in
 * the tree.  If a difference is detected, then the new statement is
 * substituted for the original.
 *
 * It would be desirable to extend this class so that it's a complete
 * replacing visitor--i.e., it also replaces everything else that is
 * modified in the tree.  However, this would be kind of tedious and
 * we haven't had need for it yet--but it you end up needing that
 * code, let's put it in here instead of in a class that's specific to
 * some compiler pass.
 *
 */
public class ReplacingVisitor extends EmptyAttributeVisitor {
    
    /**
     * Creates a new one of these.
     */
    public ReplacingVisitor() {}

    /**
     * prints a labeled statement
     */
    public Object visitLabeledStatement(JLabeledStatement self,
                                        String label,
                                        JStatement stmt) {
        JStatement newStmt = (JStatement)stmt.accept(this);
        if (newStmt!=null && newStmt!=stmt) {
            self.setBody(newStmt);
        }
        return self;
    }

    /**
     * prints a if statement
     */
    public Object visitIfStatement(JIfStatement self,
                                   JExpression cond,
                                   JStatement thenClause,
                                   JStatement elseClause) {
        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCondition(newExp);
        }

        JStatement newThen = (JStatement)thenClause.accept(this);
        if (newThen!=null && newThen!=thenClause) {
            self.setThenClause(newThen);
        }
        if (elseClause != null) {
            JStatement newElse = (JStatement)elseClause.accept(this);
            if (newElse!=null && newElse!=elseClause) {
                self.setElseClause(newElse);
            }
        }
        return self;
    }

    /**
     * prints a compound statement
     */
    public Object visitCompoundStatement(JCompoundStatement self,
                                         JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            JStatement newBody = (JStatement)body[i].accept(this);
            if (newBody!=null && newBody!=body[i]) {
                body[i] = newBody;
            }
        }
        return self;
    }

    /**
     * prints a do statement
     */
    public Object visitDoStatement(JDoStatement self,
                                   JExpression cond,
                                   JStatement body) {
        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCondition(newExp);
        }

        JStatement newBody = (JStatement)body.accept(this);
        if (newBody!=null && newBody!=body) {
            self.setBody(newBody);
        }

        return self;
    }

    /**
     * prints an expression statement
     */
    public Object visitBlockStatement(JBlock self,
                                      JavaStyleComment[] comments) {
        for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
            JStatement oldBody = (JStatement)it.next();
            Object newBody = oldBody.accept(this);
            if (!(newBody instanceof JStatement))
                continue;
            if (newBody!=null && newBody!=oldBody) {
                it.set((JStatement)newBody);
            }
        }
        visitComments(comments);
        return self;
    }

    /**
     * prints an array length expression
     */
    public Object visitSwitchGroup(JSwitchGroup self,
                                   JSwitchLabel[] labels,
                                   JStatement[] stmts) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].accept(this);
        }
        for (int i = 0; i < stmts.length; i++) {
            JStatement newStmt = (JStatement)stmts[i].accept(this);
            if (newStmt!=null && newStmt!=stmts[i]) {
                stmts[i] = newStmt;
            }
        }
        return self;
    }

    /**
     * visits a for statement
     */
    public Object visitForStatement(JForStatement self,
                                    JStatement init,
                                    JExpression cond,
                                    JStatement incr,
                                    JStatement body) {
        // recurse into init
        JStatement newInit = (JStatement)init.accept(this);
        if (newInit!=null && newInit!=init) {
            self.setInit(newInit);
        }
    
        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCond(newExp);
        }
    
        // recurse into incr
        JStatement newIncr = (JStatement)incr.accept(this);
        if (newIncr!=null && newIncr!=incr) {
            self.setIncr(newIncr);
        }
    
        // recurse into body
        JStatement newBody = (JStatement)body.accept(this);
        if (newBody!=null && newBody!=body) {
            self.setBody(newBody);
        }
        return self;
    }

    /**
     * prints a field declaration
     */
    public Object visitFieldDeclaration(JFieldDeclaration self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        if (expr != null) {
            JExpression newExp = (JExpression)expr.accept(this);
            if (newExp!=null && newExp!=expr) {
                self.setValue(newExp);
            }
        
            expr.accept(this);
        }
        // visit static array dimensions
        if (type.isArrayType()) {
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i=0; i<dims.length; i++) {
                JExpression newExp = (JExpression)dims[i].accept(this);
                if (newExp !=null && newExp!=dims[i]) {
                    dims[i] = newExp;
                }
            }
        }
        return self;
    }

    /**
     * prints a while statement
     */
    public Object visitWhileStatement(JWhileStatement self,
                                      JExpression cond,
                                      JStatement body) {
        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCondition(newExp);
        }
    
        JStatement newSt = (JStatement)body.accept(this);
        if (newSt!=null && newSt!=body) {
            self.setBody(newSt);
        }
    
        return self;
    }

    /**
     * prints a variable declaration statement
     */
    public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                    JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            JVariableDefinition result = 
                (JVariableDefinition)vars[i].accept(this);
            if (result != null && result!=vars[i]) {
                vars[i] = result;
            }
        }
        return self;
    }

    /**
     * prints a variable declaration statement
     */
    public Object visitVariableDefinition(JVariableDefinition self,
                                          int modifiers,
                                          CType type,
                                          String ident,
                                          JExpression expr) {
        if (expr != null) {
            JExpression newExp = (JExpression)expr.accept(this);
            if (newExp!=null && newExp!=expr) {
                self.setValue(newExp);
            }
        
        }
        // visit static array dimensions
        if (type.isArrayType()) {
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i=0; i<dims.length; i++) {
                JExpression newExp = (JExpression)dims[i].accept(this);
                if (newExp !=null && newExp!=dims[i]) {
                    dims[i] = newExp;
                }
            }
        }
        return self;
    }

    /**
     * prints a throw statement
     */
    public Object visitThrowStatement(JThrowStatement self,
                                      JExpression expr) {
        Utils.fail("Replacing visitor doesn't support throw statements yet");
        return super.visitThrowStatement(self, expr);
    }

    /**
     * prints a synchronized statement
     */
    public Object visitSynchronizedStatement(JSynchronizedStatement self,
                                             JExpression cond,
                                             JStatement body) {
        Utils.fail("Replacing visitor doesn't support synchronized " +
                   "statements yet");
        return super.visitSynchronizedStatement(self, cond, body);
    }

    /**
     * prints a switch statement
     */
    public Object visitSwitchStatement(JSwitchStatement self,
                                       JExpression expr,
                                       JSwitchGroup[] body) {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpression(newExp);
        }

        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        return self;
    }

    /**
     * prints a return statement
     */
    public Object visitReturnStatement(JReturnStatement self,
                                       JExpression expr) {
        if (expr != null) {
            JExpression newExp = (JExpression)expr.accept(this);
            if (newExp!=null && newExp!=expr) {
                self.setExpression(newExp);
            }
        }
        return self;
    }

    /**
     * prints an expression statement
     */
    public Object visitExpressionStatement(JExpressionStatement self,
                                           JExpression expr) {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpression(newExp);
        }
        return self;
    }

    /**
     * prints an expression list statement
     */
    public Object visitExpressionListStatement(JExpressionListStatement self,
                                               JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            JExpression newExp = (JExpression)expr[i].accept(this);
            if (newExp!=null && newExp!=expr[i]) {
                expr[i] = newExp;
            }
        }
        return self;
    }

    /**
     * prints an unary plus expression
     */
    public Object visitUnaryPlusExpression(JUnaryExpression self,
                                           JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }
        return self;
    }

    /**
     * prints an unary minus expression
     */
    public Object visitUnaryMinusExpression(JUnaryExpression self,
                                            JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }
        return self;
    }

    /**
     * prints a bitwise complement expression
     */
    public Object visitBitwiseComplementExpression(JUnaryExpression self,
                                                   JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }
        return self;
    }

    /**
     * prints a logical complement expression
     */
    public Object visitLogicalComplementExpression(JUnaryExpression self,
                                                   JExpression expr)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }
        return self;
    }

    /**
     * prints a this expression
     */
    public Object visitThisExpression(JThisExpression self,
                                      JExpression prefix) {
        if (prefix != null) {
            JExpression newExp = (JExpression)prefix.accept(this);
            if (newExp!=null && newExp!=prefix) {
                self.setPrefix(newExp);
            }
        }
        return self;
    }

    /**
     * prints a shift expression
     */
    public Object visitShiftExpression(JShiftExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * prints a shift expressiona
     */
    public Object visitRelationalExpression(JRelationalExpression self,
                                            int oper,
                                            JExpression left,
                                            JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * prints a prefix expression
     */
    public Object visitPrefixExpression(JPrefixExpression self,
                                        int oper,
                                        JExpression expr) {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }

        return self;
    }

    /**
     * prints a postfix expression
     */
    public Object visitPostfixExpression(JPostfixExpression self,
                                         int oper,
                                         JExpression expr) {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }

        return self;
    }

    /**
     * prints a parenthesed expression
     */
    public Object visitParenthesedExpression(JParenthesedExpression self,
                                             JExpression expr) {
        // parenthesized expressions are evil, they tend to cause
        // problems for passes looking for literals in specific
        // locations.  So remove them whenever we can.
        while (self.getExpr() instanceof JParenthesedExpression) {
            self = (JParenthesedExpression)self.getExpr();
        }
        return (JExpression)expr.accept(this);
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public Object visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
                                                  JExpression prefix,
                                                  String ident,
                                                  JExpression[] params,
                                                  JClassDeclaration decl)
    {
        JExpression newExp = (JExpression)prefix.accept(this);
        if (newExp!=null && newExp!=prefix) {
            self.setPrefix(newExp);
        }

        visitArgs(params);
        return self;
    }


    /**
     * Prints an unqualified instance creation expression.
     */
    public Object visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                                 JExpression prefix,
                                                 String ident,
                                                 JExpression[] params)
    {
        JExpression newExp = (JExpression)prefix.accept(this);
        if (newExp!=null && newExp!=prefix) {
            self.setPrefix(newExp);
        }

        visitArgs(params);
        return self;
    }

    /**
     * prints an array length expression
     */
    public Object visitFormalParameters(JFormalParameter self,
                                        boolean isFinal,
                                        CType type,
                                        String ident) {

        // visit static array dimensions
        if (type.isArrayType()) {
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i=0; i<dims.length; i++) {
                JExpression newExp = (JExpression)dims[i].accept(this);
                if (newExp !=null && newExp!=dims[i]) {
                    dims[i] = newExp;
                }
            }
        }

        return self;
    }

    /**
     * prints an array length expression
     */
    public Object visitArgs(JExpression[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                JExpression newExp = (JExpression)args[i].accept(this);
                if (newExp!=null && newExp!=args[i]) {
                    args[i] = newExp;
                }
            }
        }
        return args;
    }

    /**
     * prints an array allocator expression
     */
    public Object visitNewArrayExpression(JNewArrayExpression self,
                                          CType type,
                                          JExpression[] dims,
                                          JArrayInitializer init)
    {
        for (int i = 0; i < dims.length; i++) {
            if (dims[i] != null) {
                JExpression newExp = (JExpression)dims[i].accept(this);
                if (newExp!=null && newExp!=dims[i]) {
                    dims[i] = newExp;
                }
            }
        }
        if (init != null) {
            JArrayInitializer newExp = (JArrayInitializer)init.accept(this);
            if (newExp!=null && newExp!=init) {
                self.setInit(newExp);
            }       
        }
        return self;
    }

    /**
     * prints a name expression
     */
    public Object visitNameExpression(JNameExpression self,
                                      JExpression prefix,
                                      String ident) {
        if (prefix != null) {
            JExpression newExp = (JExpression)prefix.accept(this);
            if (newExp!=null && newExp!=prefix) {
                self.setPrefix(newExp);
            }
        }
        return self;
    }

    /**
     * prints an array allocator expression
     */
    public Object visitBinaryExpression(JBinaryExpression self,
                                        String oper,
                                        JExpression left,
                                        JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * prints a method call expression
     */
    public Object visitMethodCallExpression(JMethodCallExpression self,
                                            JExpression prefix,
                                            String ident,
                                            JExpression[] args) {
        if (prefix != null) {
            JExpression newExp = (JExpression)prefix.accept(this);
            if (newExp!=null && newExp!=prefix) {
                self.setPrefix(newExp);
            }
        }
        visitArgs(args);
        return self;
    }

    /**
     * prints an instanceof expression
     */
    public Object visitInstanceofExpression(JInstanceofExpression self,
                                            JExpression expr,
                                            CType dest) {
        Utils.fail("Replacing visitor doesn't support instanceof yet.");
        return super.visitInstanceofExpression(self, expr, dest);
    }

    /**
     * prints an equality expression
     */
    public Object visitEqualityExpression(JEqualityExpression self,
                                          boolean equal,
                                          JExpression left,
                                          JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * prints a conditional expression
     */
    public Object visitConditionalExpression(JConditionalExpression self,
                                             JExpression cond,
                                             JExpression left,
                                             JExpression right) {
        JExpression newExp = (JExpression)cond.accept(this);
        if (newExp!=null && newExp!=cond) {
            self.setCondition(newExp);
        }
    
        newExp = (JExpression)left.accept(this);
        if (newExp!=null && newExp!=left) {
            self.setLeft(newExp);
        }

        newExp = (JExpression)right.accept(this);
        if (newExp!=null && newExp!=right) {
            self.setRight(newExp);
        }

        return self;
    }

    /**
     * prints a compound expression
     */
    public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                    int oper,
                                                    JExpression left,
                                                    JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * this is a private method for visiting binary expressions
     */
    private Object doBinaryExpression(JBinaryExpression self, 
                                      JExpression left, 
                                      JExpression right) {
        JExpression newExp = (JExpression)left.accept(this);
        if (newExp!=null && newExp!=left) {
            self.setLeft(newExp);
        }
    
        newExp = (JExpression)right.accept(this);
        if (newExp!=null && newExp!=right) {
            self.setRight(newExp);
        }

        return self;
    }

    /**
     * prints a field expression
     */
    public Object visitFieldExpression(JFieldAccessExpression self,
                                       JExpression left,
                                       String ident)
    {
        JExpression newExp = (JExpression)left.accept(this);
        if (newExp!=null && newExp!=left) {
            self.setPrefix(newExp);
        }
    
        return self;
    }

    /**
     * prints a cast expression
     */
    public Object visitCastExpression(JCastExpression self,
                                      JExpression expr,
                                      CType type)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }
    
        return self;
    }

    /**
     * prints a cast expression
     */
    public Object visitUnaryPromoteExpression(JUnaryPromote self,
                                              JExpression expr,
                                              CType type)
    {
        JExpression newExp = (JExpression)expr.accept(this);
        if (newExp!=null && newExp!=expr) {
            self.setExpr(newExp);
        }
    
        return self;
    }

    /**
     * prints a compound assignment expression
     */
    public Object visitBitwiseExpression(JBitwiseExpression self,
                                         int oper,
                                         JExpression left,
                                         JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * prints an assignment expression
     */
    public Object visitAssignmentExpression(JAssignmentExpression self,
                                            JExpression left,
                                            JExpression right) {
        return doBinaryExpression(self, left, right);
    }

    /**
     * prints an array length expression
     */
    public Object visitArrayLengthExpression(JArrayLengthExpression self,
                                             JExpression prefix) {
        JExpression newExp = (JExpression)prefix.accept(this);
        if (newExp!=null && newExp!=prefix) {
            self.setPrefix(newExp);
        }
    
        return self;
    }

    /**
     * prints an array length expression
     */
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
                                             JExpression prefix,
                                             JExpression accessor) {
        JExpression newExp = (JExpression)prefix.accept(this);
        if (newExp!=null && newExp!=prefix) {
            self.setPrefix(newExp);
        }
    
        newExp = (JExpression)accessor.accept(this);
        if (newExp!=null && newExp!=accessor) {
            self.setAccessor(newExp);
        }
    
        return self;
    }

    /**
     * Replaces all replacable subexpressions of expression contaiing uninterpretable text
     */
    @Override
    public Object visitEmittedTextExpression(JEmittedTextExpression self, Object[] parts) {
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] instanceof JExpression) {
                JExpression newExp = (JExpression)((JExpression)parts[i]).accept(this);
                if (newExp!=null && newExp!=parts[i]) {
                    parts[i] = newExp;
                } else {
                    assert parts[i] instanceof String || parts[i] instanceof CType || parts[i] instanceof JExpression;
                }
            }
        }
        return self;
    }

    /**
     * prints an array length expression
     */
    public Object visitSwitchLabel(JSwitchLabel self,
                                   JExpression expr) {
        if (expr != null) {
            JExpression newExp = (JExpression)expr.accept(this);
            if (newExp!=null && newExp!=expr) {
                self.setExpression(newExp);
            }
        }

        return self;
    }


}

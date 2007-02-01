/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: KjcEmptyVisitor.java,v 1.10 2007-02-01 21:11:31 dimock Exp $
 */

package at.dms.kjc;

import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import java.util.ListIterator;

/**
 * This is a visitor that just recurses into children at every node.
 * It can be extended to add some functionality at a given node.
 *
 * Suggested from: Max R. Andersen(max@cs.auc.dk) */
public class KjcEmptyVisitor implements Constants, KjcVisitor {

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * prints a compilation unit
     */
    public void visitCompilationUnit(JCompilationUnit self,
                                     JPackageName packageName,
                                     JPackageImport[] importedPackages,
                                     JClassImport[] importedClasses,
                                     JTypeDeclaration[] typeDeclarations) {
        if (packageName.getName().length() > 0) {
            packageName.accept(this);
        }

        for (int i = 0; i < importedPackages.length ; i++) {
            if (!importedPackages[i].getName().equals("java/lang")) {
                importedPackages[i].accept(this);
            }
        }

        for (int i = 0; i < importedClasses.length ; i++) {
            importedClasses[i].accept(this);
        }

        for (int i = 0; i < typeDeclarations.length ; i++) {
            typeDeclarations[i].accept(this);
        }
    }

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * prints a class declaration
     */
    public void visitClassDeclaration(JClassDeclaration self,
                                      int modifiers,
                                      String ident,
                                      String superName,
                                      CClassType[] interfaces,
                                      JPhylum[] body,
                                      JFieldDeclaration[] fields,
                                      JMethodDeclaration[] methods,
                                      JTypeDeclaration[] decls) {
        visitClassBody(decls, fields, methods, body);
    }

    /**
     *
     */
    public void visitClassBody(JTypeDeclaration[] decls,
                               JFieldDeclaration[] fields,
                               JMethodDeclaration[] methods,
                               JPhylum[] body) {
        for (int i = 0; i < decls.length ; i++) {
            decls[i].accept(this);
        }
        for (int i = 0; i < fields.length ; i++) {
            fields[i].accept(this);
        }
        for (int i = 0; i < methods.length ; i++) {
            methods[i].accept(this);
        }
        for (int i = 0; i < body.length ; i++) {
            if (!(body[i] instanceof JFieldDeclaration)) {
                body[i].accept(this);
            }
        }
        for (int i = 0; i < body.length ; i++) {
            if (body[i] instanceof JFieldDeclaration) {
                body[i].accept(this);
            }
        }
    }

    /**
     * prints a class declaration
     */
    public void visitInnerClassDeclaration(JClassDeclaration self,
                                           int modifiers,
                                           String ident,
                                           String superName,
                                           CClassType[] interfaces,
                                           JTypeDeclaration[] decls,
                                           JPhylum[] body,
                                           JFieldDeclaration[] fields,
                                           JMethodDeclaration[] methods) {
        for (int i = 0; i < decls.length ; i++) {
            decls[i].accept(this);
        }
        for (int i = 0; i < fields.length ; i++) {
            fields[i].accept(this);
        }
        for (int i = 0; i < methods.length ; i++) {
            methods[i].accept(this);
        }
        for (int i = 0; i < body.length ; i++) {
            body[i].accept(this);
        }
    }

    /**
     * prints an interface declaration
     */
    public void visitInterfaceDeclaration(JInterfaceDeclaration self,
                                          int modifiers,
                                          String ident,
                                          CClassType[] interfaces,
                                          JPhylum[] body,
                                          JMethodDeclaration[] methods) {
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
        for (int i = 0; i < methods.length; i++) {
            methods[i].accept(this);
        }
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * prints a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
                                      int modifiers,
                                      CType type,
                                      String ident,
                                      JExpression expr) {
        if (expr != null) {
            expr.accept(this);
        }
        // also descend into the vardef
        self.getVariable().accept(this);
    }

    /**
     * prints a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
                                       int modifiers,
                                       CType returnType,
                                       String ident,
                                       JFormalParameter[] parameters,
                                       CClassType[] exceptions,
                                       JBlock body) {
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
            }
        }
        if (body != null) {
            body.accept(this);
        }
    }

    /**
     * prints a method declaration
     */
    public void visitConstructorDeclaration(JConstructorDeclaration self,
                                            int modifiers,
                                            String ident,
                                            JFormalParameter[] parameters,
                                            CClassType[] exceptions,
                                            JConstructorBlock body)
    {
        for (int i = 0; i < parameters.length; i++) {
            if (!parameters[i].isGenerated()) {
                parameters[i].accept(this);
            }
        }
        body.accept(this);
    }

    // ----------------------------------------------------------------------
    // STATEMENT
    // ----------------------------------------------------------------------

    /**
     * prints a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
                                    JExpression cond,
                                    JStatement body) {
        cond.accept(this);
        body.accept(this);
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                                  JVariableDefinition[] vars) {
        for (int i = 0; i < vars.length; i++) {
            vars[i].accept(this);
        }
    }

    /**
     * prints a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
                                        int modifiers,
                                        CType type,
                                        String ident,
                                        JExpression expr) {
        if (expr != null) {
            expr.accept(this);
        }
        // visit static array dimensions
        if (type.isArrayType()) {
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i=0; i<dims.length; i++) {
                dims[i].accept(this);
            }
        }
    }

    /**
     * prints a try-catch statement
     */
    public void visitTryCatchStatement(JTryCatchStatement self,
                                       JBlock tryClause,
                                       JCatchClause[] catchClauses) {
        tryClause.accept(this);
        for (int i = 0; i < catchClauses.length; i++) {
            catchClauses[i].accept(this);
        }
    }

    /**
     * prints a try-finally statement
     */
    public void visitTryFinallyStatement(JTryFinallyStatement self,
                                         JBlock tryClause,
                                         JBlock finallyClause) {
        tryClause.accept(this);
        if (finallyClause != null) {
            finallyClause.accept(this);
        }
    }

    /**
     * prints a throw statement
     */
    public void visitThrowStatement(JThrowStatement self,
                                    JExpression expr) {
        expr.accept(this);
    }

    /**
     * prints a synchronized statement
     */
    public void visitSynchronizedStatement(JSynchronizedStatement self,
                                           JExpression cond,
                                           JStatement body) {
        cond.accept(this);
        body.accept(this);
    }

    /**
     * prints a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
                                     JExpression expr,
                                     JSwitchGroup[] body) {
        expr.accept(this);
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
    }

    /**
     * prints a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
                                     JExpression expr) {
        if (expr != null) {
            expr.accept(this);
        }
    }

    /**
     * prints a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
                                      String label,
                                      JStatement stmt) {
        stmt.accept(this);
    }

    /**
     * prints a if statement
     */
    public void visitIfStatement(JIfStatement self,
                                 JExpression cond,
                                 JStatement thenClause,
                                 JStatement elseClause) {
        cond.accept(this);
        thenClause.accept(this);
        if (elseClause != null) {
            elseClause.accept(this);
        }
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
        if (cond != null) {
            cond.accept(this);
        }
        if (incr != null) {
            incr.accept(this);
        }
        body.accept(this);
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JCompoundStatement self,
                                       JStatement[] body) {
        visitCompoundStatement(body);
    }

    /**
     * prints a compound statement
     */
    public void visitCompoundStatement(JStatement[] body) {
        for (int i = 0; i < body.length; i++) {
            body[i].accept(this);
        }
    }

    /**
     * prints an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
                                         JExpression expr) {
        expr.accept(this);
    }

    /**
     * prints an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
                                             JExpression[] expr) {
        for (int i = 0; i < expr.length; i++) {
            expr[i].accept(this);
        }
    }

    /**
     * prints a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
    }

    /**
     * prints a do statement
     */
    public void visitDoStatement(JDoStatement self,
                                 JExpression cond,
                                 JStatement body) {
        body.accept(this);
        cond.accept(this);
    }

    /**
     * prints a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
                                       String label) {
    }

    /**
     * prints a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
                                    String label) {
    }

    /**
     * prints an expression statement
     */
    public void visitBlockStatement(JBlock self,
                                    JavaStyleComment[] comments) {
        for (ListIterator it = self.getStatementIterator(); it.hasNext(); ) {
            ((JStatement)it.next()).accept(this);
        }
    }

    /**
     * prints a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                              JTypeDeclaration decl) {
        decl.accept(this);
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * prints an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
                                         JExpression expr)
    {
        expr.accept(this);
    }

    /**
     * prints an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
                                          JExpression expr)
    {
        expr.accept(this);
    }

    /**
     * prints a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        expr.accept(this);
    }

    /**
     * prints a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
                                                 JExpression expr)
    {
        expr.accept(this);
    }

    /**
     * prints a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
                                        CType type) {
    }

    /**
     * prints a this expression
     */
    public void visitThisExpression(JThisExpression self,
                                    JExpression prefix) {
        if (prefix != null) {
            prefix.accept(this);
        }
    }

    /**
     * prints a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
    }

    /**
     * prints a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right) {
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
                                          int oper,
                                          JExpression left,
                                          JExpression right) {
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
                                      int oper,
                                      JExpression expr) {
        expr.accept(this);
    }

    /**
     * prints a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
                                       int oper,
                                       JExpression expr) {
        expr.accept(this);
    }

    /**
     * prints a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
                                           JExpression expr) {
        expr.accept(this);
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
                                                JExpression prefix,
                                                String ident,
                                                JExpression[] params,
                                                JClassDeclaration decl)
    {
        prefix.accept(this);
        visitArgs(params);
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                               JExpression prefix,
                                               String ident,
                                               JExpression[] params)
    {
        prefix.accept(this);
        visitArgs(params);
    }

    /**
     * Prints an unqualified anonymous class instance creation expression.
     */
    public void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                                  CClassType type,
                                                  JExpression[] params,
                                                  JClassDeclaration decl)
    {
        visitArgs(params);
    }

    /**
     * Prints an unqualified instance creation expression.
     */
    public void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                                 CClassType type,
                                                 JExpression[] params)
    {
        visitArgs(params);
    }

    /**
     * prints an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
                                        CType type,
                                        JExpression[] dims,
                                        JArrayInitializer init)
    {
        for (int i = 0; i < dims.length; i++) {
            if (dims[i] != null) {
                dims[i].accept(this);
            }
        }
        if (init != null) {
            init.accept(this);
        }
    }

    /**
     * prints a name expression
     */
    public void visitNameExpression(JNameExpression self,
                                    JExpression prefix,
                                    String ident) {
        if (prefix != null) {
            prefix.accept(this);
        }
    }

    /**
     * prints an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
                                      String oper,
                                      JExpression left,
                                      JExpression right) {
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] args) {
        if (prefix != null) {
            prefix.accept(this);
        }
        visitArgs(args);
    }

    /**
     * prints a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
                                             String ident) {
    }

    /**
     * prints an instanceof expression
     */
    public void visitInstanceofExpression(JInstanceofExpression self,
                                          JExpression expr,
                                          CType dest) {
        expr.accept(this);
    }

    /**
     * prints an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
                                        boolean equal,
                                        JExpression left,
                                        JExpression right) {
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
                                           JExpression cond,
                                           JExpression left,
                                           JExpression right) {
        cond.accept(this);
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                                  int oper,
                                                  JExpression left,
                                                  JExpression right) {
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
                                     JExpression left,
                                     String ident)
    {
        left.accept(this);
    }

    /**
     * prints a class expression
     */
    public void visitClassExpression(JClassExpression self, CType type) {
    }

    /**
     * prints a cast expression
     */
    public void visitCastExpression(JCastExpression self,
                                    JExpression expr,
                                    CType type)
    {
        expr.accept(this);
    }

    /**
     * prints a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
                                            JExpression expr,
                                            CType type)
    {
        expr.accept(this);
    }

    /**
     * prints a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
                                       int oper,
                                       JExpression left,
                                       JExpression right) {
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
                                          JExpression left,
                                          JExpression right) {
        left.accept(this);
        right.accept(this);
    }

    /**
     * prints an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
                                           JExpression prefix) {
        prefix.accept(this);
    }

    /**
     * prints an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
                                           JExpression prefix,
                                           JExpression accessor) {
        prefix.accept(this);
        accessor.accept(this);
    }

    /** visiting emitted text with possible embedded expressions. */
    public void visitEmittedTextExpression(JEmittedTextExpression self, Object[] parts) {
        for (Object part : parts) {
            if (part instanceof JExpression) {
                ((JExpression)part).accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitComments(JavaStyleComment[] comments) {
    }

    /**
     * prints an array length expression
     */
    public void visitComment(JavaStyleComment comment) {
    }

    /**
     * prints an array length expression
     */
    public void visitJavadoc(JavadocComment comment) {
    }

    // ----------------------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------------------

    /**
     * prints an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
                                 JExpression expr) {
        if (expr != null) {
            expr.accept(this);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
                                 JSwitchLabel[] labels,
                                 JStatement[] stmts) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].accept(this);
        }
        for (int i = 0; i < stmts.length; i++) {
            stmts[i].accept(this);
        }
    }

    /**
     * prints an array length expression
     */
    public void visitCatchClause(JCatchClause self,
                                 JFormalParameter exception,
                                 JBlock body) {
        exception.accept(this);
        body.accept(this);
    }

    /**
     * prints a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
    }

    /**
     * prints a byte literal
     */
    public void visitByteLiteral(byte value) {
    }

    /**
     * prints a character literal
     */
    public void visitCharLiteral(char value) {
    }

    /**
     * prints a double literal
     */
    public void visitDoubleLiteral(double value) {
    }

    /**
     * prints a float literal
     */
    public void visitFloatLiteral(float value) {
    }

    /**
     * prints a int literal
     */
    public void visitIntLiteral(int value) {
    }

    /**
     * prints a long literal
     */
    public void visitLongLiteral(long value) {
    }

    /**
     * prints a short literal
     */
    public void visitShortLiteral(short value) {
    }

    /**
     * prints a string literal
     */
    public void visitStringLiteral(String value) {
    }

    /**
     * prints a null literal
     */
    public void visitNullLiteral() {
    }

    /**
     * prints an array length expression
     */
    public void visitPackageName(String name) {
    }

    /**
     * prints an array length expression
     */
    public void visitPackageImport(String name) {
    }

    /**
     * prints an array length expression
     */
    public void visitClassImport(String name) {
    }

    /**
     * prints an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
                                      boolean isFinal,
                                      CType type,
                                      String ident) {
        // visit static array dimensions
        if (type.isArrayType()) {
            JExpression[] dims = ((CArrayType)type).getDims();
            for (int i=0; i<dims.length; i++) {
                dims[i].accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitArgs(JExpression[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                args[i].accept(this);
            }
        }
    }

    /**
     * prints an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
                                     boolean functorIsThis,
                                     JExpression[] params)
    {
        visitArgs(params);
    }

    /**
     * prints an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
                                      JExpression[] elems)
    {
        for (int i = 0; i < elems.length; i++) {
            elems[i].accept(this);
        }
    }
}

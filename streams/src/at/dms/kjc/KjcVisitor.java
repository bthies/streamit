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
 * $Id: KjcVisitor.java,v 1.5 2007-02-01 21:11:31 dimock Exp $
 */

package at.dms.kjc;

import java.util.List;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * Implementation of Visitor Design Pattern for KJC.
 *
 * Suggested from: Max R. Andersen(max@cs.auc.dk)
 */
public interface KjcVisitor {

    // ----------------------------------------------------------------------
    // COMPILATION UNIT
    // ----------------------------------------------------------------------

    /**
     * visits a compilation unit
     */
    void visitCompilationUnit(JCompilationUnit self,
                              JPackageName packageName,
                              JPackageImport[] importedPackages,
                              JClassImport[] importedClasses,
                              JTypeDeclaration[] typeDeclarations);

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * visits a class declaration
     */
    void visitClassDeclaration(JClassDeclaration self,
                               int modifiers,
                               String ident,
                               String superName,
                               CClassType[] interfaces,
                               JPhylum[] body,
                               JFieldDeclaration[] fields,
                               JMethodDeclaration[] methods,
                               JTypeDeclaration[] decls);
    /**
     * visits a class body
     */
    void visitClassBody(JTypeDeclaration[] decls,
                        JFieldDeclaration[] fields,
                        JMethodDeclaration[] methods,
                        JPhylum[] body);

    /**
     * visits a class declaration
     */
    void visitInnerClassDeclaration(JClassDeclaration self,
                                    int modifiers,
                                    String ident,
                                    String superName,
                                    CClassType[] interfaces,
                                    JTypeDeclaration[] decls,
                                    JPhylum[] body,
                                    JFieldDeclaration[] fields,
                                    JMethodDeclaration[] methods);

    /**
     * visits an interface declaration
     */
    void visitInterfaceDeclaration(JInterfaceDeclaration self,
                                   int modifiers,
                                   String ident,
                                   CClassType[] interfaces,
                                   JPhylum[] body,
                                   JMethodDeclaration[] methods);

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * visits a field declaration
     */
    void visitFieldDeclaration(JFieldDeclaration self,
                               int modifiers,
                               CType type,
                               String ident,
                               JExpression expr);

    /**
     * visits a method declaration
     */
    void visitMethodDeclaration(JMethodDeclaration self,
                                int modifiers,
                                CType returnType,
                                String ident,
                                JFormalParameter[] parameters,
                                CClassType[] exceptions,
                                JBlock body);
    /**
     * visits a method declaration
     */
    void visitConstructorDeclaration(JConstructorDeclaration self,
                                     int modifiers,
                                     String ident,
                                     JFormalParameter[] parameters,
                                     CClassType[] exceptions,
                                     JConstructorBlock body);

    // ----------------------------------------------------------------------
    // STATEMENTS
    // ----------------------------------------------------------------------

    /**
     * visits a while statement
     */
    void visitWhileStatement(JWhileStatement self,
                             JExpression cond,
                             JStatement body);

    /**
     * visits a variable declaration statement
     */
    void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                           JVariableDefinition[] vars);

    /**
     * visits a variable declaration statement
     */
    void visitVariableDefinition(JVariableDefinition self,
                                 int modifiers,
                                 CType type,
                                 String ident,
                                 JExpression expr);
    /**
     * visits a try-catch statement
     */
    void visitTryCatchStatement(JTryCatchStatement self,
                                JBlock tryClause,
                                JCatchClause[] catchClauses);

    /**
     * visits a try-finally statement
     */
    void visitTryFinallyStatement(JTryFinallyStatement self,
                                  JBlock tryClause,
                                  JBlock finallyClause);

    /**
     * visits a throw statement
     */
    void visitThrowStatement(JThrowStatement self,
                             JExpression expr);

    /**
     * visits a synchronized statement
     */
    void visitSynchronizedStatement(JSynchronizedStatement self,
                                    JExpression cond,
                                    JStatement body);

    /**
     * visits a switch statement
     */
    void visitSwitchStatement(JSwitchStatement self,
                              JExpression expr,
                              JSwitchGroup[] body);

    /**
     * visits a return statement
     */
    void visitReturnStatement(JReturnStatement self,
                              JExpression expr);

    /**
     * visits a labeled statement
     */
    void visitLabeledStatement(JLabeledStatement self,
                               String label,
                               JStatement stmt);

    /**
     * visits a if statement
     */
    void visitIfStatement(JIfStatement self,
                          JExpression cond,
                          JStatement thenClause,
                          JStatement elseClause);

    /**
     * visits a for statement
     */
    void visitForStatement(JForStatement self,
                           JStatement init,
                           JExpression cond,
                           JStatement incr,
                           JStatement body);

    /**
     * visits a compound statement
     */
    void visitCompoundStatement(JCompoundStatement self,
                                JStatement[] body);

    /**
     * visits an expression statement
     */
    void visitExpressionStatement(JExpressionStatement self,
                                  JExpression expr);

    /**
     * visits an expression list statement
     */
    void visitExpressionListStatement(JExpressionListStatement self,
                                      JExpression[] expr);

    /**
     * visits a empty statement
     */
    void visitEmptyStatement(JEmptyStatement self);

    /**
     * visits a do statement
     */
    void visitDoStatement(JDoStatement self,
                          JExpression cond,
                          JStatement body);

    /**
     * visits a continue statement
     */
    void visitContinueStatement(JContinueStatement self,
                                String label);

    /**
     * visits a break statement
     */
    void visitBreakStatement(JBreakStatement self,
                             String label);

    /**
     * visits an expression statement
     */
    void visitBlockStatement(JBlock self,
                             JavaStyleComment[] comments);

    /**
     * visits a type declaration statement
     */
    void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                       JTypeDeclaration decl);

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * visits an unary plus expression
     */
    void visitUnaryPlusExpression(JUnaryExpression self,
                                  JExpression expr);

    /**
     * visits an unary minus expression
     */
    void visitUnaryMinusExpression(JUnaryExpression self,
                                   JExpression expr);

    /**
     * visits a bitwise complement expression
     */
    void visitBitwiseComplementExpression(JUnaryExpression self,
                                          JExpression expr);

    /**
     * visits a logical complement expression
     */
    void visitLogicalComplementExpression(JUnaryExpression self,
                                          JExpression expr);

    /**
     * visits a type name expression
     */
    void visitTypeNameExpression(JTypeNameExpression self,
                                 CType type);

    /**
     * visits a this expression
     */
    void visitThisExpression(JThisExpression self,
                             JExpression prefix);

    /**
     * visits a super expression
     */
    void visitSuperExpression(JSuperExpression self);

    /**
     * visits a shift expression
     */
    void visitShiftExpression(JShiftExpression self,
                              int oper,
                              JExpression left,
                              JExpression right);

    /**
     * visits a shift expressiona
     */
    void visitRelationalExpression(JRelationalExpression self,
                                   int oper,
                                   JExpression left,
                                   JExpression right);

    /**
     * visits a prefix expression
     */
    void visitPrefixExpression(JPrefixExpression self,
                               int oper,
                               JExpression expr);

    /**
     * visits a postfix expression
     */
    void visitPostfixExpression(JPostfixExpression self,
                                int oper,
                                JExpression expr);

    /**
     * visits a parenthesed expression
     */
    void visitParenthesedExpression(JParenthesedExpression self,
                                    JExpression expr);

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    void visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
                                         JExpression prefix,
                                         String ident,
                                         JExpression[] params,
                                         JClassDeclaration decl);
    /**
     * Visits an unqualified instance creation expression.
     */
    void visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                        JExpression prefix,
                                        String ident,
                                        JExpression[] params);

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                           CClassType type,
                                           JExpression[] params,
                                           JClassDeclaration decl);

    /**
     * Visits an unqualified instance creation expression.
     */
    void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                          CClassType type,
                                          JExpression[] params);

    /**
     * visits an array allocator expression
     */
    void visitNewArrayExpression(JNewArrayExpression self,
                                 CType type,
                                 JExpression[] dims,
                                 JArrayInitializer init);

    /**
     * visits a name expression
     */
    void visitNameExpression(JNameExpression self,
                             JExpression prefix,
                             String ident);

    /**
     * visits an array allocator expression
     */
    void visitBinaryExpression(JBinaryExpression self,
                               String oper,
                               JExpression left,
                               JExpression right);
    /**
     * visits a method call expression
     */
    void visitMethodCallExpression(JMethodCallExpression self,
                                   JExpression prefix,
                                   String ident,
                                   JExpression[] args);
    /**
     * visits a local variable expression
     */
    void visitLocalVariableExpression(JLocalVariableExpression self,
                                      String ident);

    /**
     * visits an instanceof expression
     */
    void visitInstanceofExpression(JInstanceofExpression self,
                                   JExpression expr,
                                   CType dest);

    /**
     * visits an equality expression
     */
    void visitEqualityExpression(JEqualityExpression self,
                                 boolean equal,
                                 JExpression left,
                                 JExpression right);

    /**
     * visits a conditional expression
     */
    void visitConditionalExpression(JConditionalExpression self,
                                    JExpression cond,
                                    JExpression left,
                                    JExpression right);

    /**
     * visits a compound expression
     */
    void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                           int oper,
                                           JExpression left,
                                           JExpression right);
    /**
     * visits a field expression
     */
    void visitFieldExpression(JFieldAccessExpression self,
                              JExpression left,
                              String ident);

    /**
     * visits a class expression
     */
    void visitClassExpression(JClassExpression self,
                              CType type);

    /**
     * visits a cast expression
     */
    void visitCastExpression(JCastExpression self,
                             JExpression expr,
                             CType type);

    /**
     * visits a cast expression
     */
    void visitUnaryPromoteExpression(JUnaryPromote self,
                                     JExpression expr,
                                     CType type);

    /**
     * visits a compound assignment expression
     */
    void visitBitwiseExpression(JBitwiseExpression self,
                                int oper,
                                JExpression left,
                                JExpression right);
    /**
     * visits an assignment expression
     */
    void visitAssignmentExpression(JAssignmentExpression self,
                                   JExpression left,
                                   JExpression right);

    /**
     * visits an array length expression
     */
    void visitArrayLengthExpression(JArrayLengthExpression self,
                                    JExpression prefix);

    /**
     * visits an array length expression
     */
    void visitArrayAccessExpression(JArrayAccessExpression self,
                                    JExpression prefix,
                                    JExpression accessor);

    /**
     * 
     */
    void visitEmittedTextExpression(JEmittedTextExpression self, Object[] parts);
    
    /**
     * visits an array length expression
     */
    void visitComments(JavaStyleComment[] comments);

    /**
     * visits an array length expression
     */
    void visitComment(JavaStyleComment comment);

    /**
     * visits an array length expression
     */
    void visitJavadoc(JavadocComment comment);

    // ----------------------------------------------------------------------
    // OTHERS
    // ----------------------------------------------------------------------

    /**
     * visits an array length expression
     */
    void visitSwitchLabel(JSwitchLabel self,
                          JExpression expr);

    /**
     * visits an array length expression
     */
    void visitSwitchGroup(JSwitchGroup self,
                          JSwitchLabel[] labels,
                          JStatement[] stmts);

    /**
     * visits an array length expression
     */
    void visitCatchClause(JCatchClause self,
                          JFormalParameter exception,
                          JBlock body);

    /**
     * visits an array length expression
     */
    void visitFormalParameters(JFormalParameter self,
                               boolean isFinal,
                               CType type,
                               String ident);

    /**
     * visits an array length expression
     */
    void visitConstructorCall(JConstructorCall self,
                              boolean functorIsThis,
                              JExpression[] params);

    /**
     * visits an array initializer expression
     */
    void visitArrayInitializer(JArrayInitializer self,
                               JExpression[] elems);

    /**
     * visits a boolean literal
     */
    void visitBooleanLiteral(boolean value);

    /**
     * visits a byte literal
     */
    void visitByteLiteral(byte value);

    /**
     * visits a character literal
     */
    void visitCharLiteral(char value);

    /**
     * visits a double literal
     */
    void visitDoubleLiteral(double value);

    /**
     * visits a float literal
     */
    void visitFloatLiteral(float value);

    /**
     * visits a int literal
     */
    void visitIntLiteral(int value);

    /**
     * visits a long literal
     */
    void visitLongLiteral(long value);

    /**
     * visits a short literal
     */
    void visitShortLiteral(short value);

    /**
     * visits a string literal
     */
    void visitStringLiteral(String value);

    /**
     * visits a null literal
     */
    void visitNullLiteral();

    /**
     * visits a package name declaration
     */
    void visitPackageName(String name);

    /**
     * visits a package import declaration
     */
    void visitPackageImport(String name);

    /**
     * visits a class import declaration
     */
    void visitClassImport(String name);
}

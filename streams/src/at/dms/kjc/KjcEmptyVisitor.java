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
 * $Id: KjcEmptyVisitor.java,v 1.2 2001-10-02 21:19:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * This is a visitor that does nothing at every node.  It can be
 * extended to add some functionality at a given node.
 *
 * Suggested from: Max R. Andersen(max@cs.auc.dk) */
public abstract class KjcEmptyVisitor implements KjcVisitor {

    // ----------------------------------------------------------------------
    // COMPILATION UNIT
    // ----------------------------------------------------------------------

    /**
     * visits a compilation unit
     */
    public void visitCompilationUnit(JCompilationUnit self,
				     JPackageName packageName,
				     JPackageImport[] importedPackages,
				     JClassImport[] importedClasses,
				     JTypeDeclaration[] typeDeclarations) {
    }

    // ----------------------------------------------------------------------
    // TYPE DECLARATION
    // ----------------------------------------------------------------------

    /**
     * visits a class declaration
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
    }
    /**
     * visits a class body
     */
    public void visitClassBody(JTypeDeclaration[] decls,
			       JFieldDeclaration[] fields,
			       JMethodDeclaration[] methods,
			       JPhylum[] body) {
    }

    /**
     * visits a class declaration
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
    }

    /**
     * visits an interface declaration
     */
    public void visitInterfaceDeclaration(JInterfaceDeclaration self,
					  int modifiers,
					  String ident,
					  CClassType[] interfaces,
					  JPhylum[] body,
					  JMethodDeclaration[] methods) {
    }

    // ----------------------------------------------------------------------
    // METHODS AND FIELDS
    // ----------------------------------------------------------------------

    /**
     * visits a field declaration
     */
    public void visitFieldDeclaration(JFieldDeclaration self,
				      int modifiers,
				      CType type,
				      String ident,
				      JExpression expr) {
    }

    /**
     * visits a method declaration
     */
    public void visitMethodDeclaration(JMethodDeclaration self,
				       int modifiers,
				       CType returnType,
				       String ident,
				       JFormalParameter[] parameters,
				       CClassType[] exceptions,
				       JBlock body) {
    }
    /**
     * visits a method declaration
     */
    public void visitConstructorDeclaration(JConstructorDeclaration self,
					    int modifiers,
					    String ident,
					    JFormalParameter[] parameters,
					    CClassType[] exceptions,
					    JConstructorBlock body) {
    }

    // ----------------------------------------------------------------------
    // STATEMENTS
    // ----------------------------------------------------------------------

    /**
     * visits a while statement
     */
    public void visitWhileStatement(JWhileStatement self,
				    JExpression cond,
				    JStatement body) {
    }

    /**
     * visits a variable declaration statement
     */
    public void visitVariableDeclarationStatement(JVariableDeclarationStatement self,
						  JVariableDefinition[] vars) {
    }

    /**
     * visits a variable declaration statement
     */
    public void visitVariableDefinition(JVariableDefinition self,
					int modifiers,
					CType type,
					String ident,
					JExpression expr) {
    }
    /**
     * visits a try-catch statement
     */
    public void visitTryCatchStatement(JTryCatchStatement self,
				       JBlock tryClause,
				       JCatchClause[] catchClauses) {
    }

    /**
     * visits a try-finally statement
     */
    public void visitTryFinallyStatement(JTryFinallyStatement self,
					 JBlock tryClause,
					 JBlock finallyClause) {
    }

    /**
     * visits a throw statement
     */
    public void visitThrowStatement(JThrowStatement self,
				    JExpression expr) {
    }

    /**
     * visits a synchronized statement
     */
    public void visitSynchronizedStatement(JSynchronizedStatement self,
					   JExpression cond,
					   JStatement body) {
    }

    /**
     * visits a switch statement
     */
    public void visitSwitchStatement(JSwitchStatement self,
				     JExpression expr,
				     JSwitchGroup[] body) {
    }

    /**
     * visits a return statement
     */
    public void visitReturnStatement(JReturnStatement self,
				     JExpression expr) {
    }

    /**
     * visits a labeled statement
     */
    public void visitLabeledStatement(JLabeledStatement self,
				      String label,
				      JStatement stmt) {
    }

    /**
     * visits a if statement
     */
    public void visitIfStatement(JIfStatement self,
				 JExpression cond,
				 JStatement thenClause,
				 JStatement elseClause) {
    }

    /**
     * visits a for statement
     */
    public void visitForStatement(JForStatement self,
				  JStatement init,
				  JExpression cond,
				  JStatement incr,
				  JStatement body) {
    }

    /**
     * visits a compound statement
     */
    public void visitCompoundStatement(JCompoundStatement self,
				       JStatement[] body) {
    }

    /**
     * visits an expression statement
     */
    public void visitExpressionStatement(JExpressionStatement self,
					 JExpression expr) {
    }

    /**
     * visits an expression list statement
     */
    public void visitExpressionListStatement(JExpressionListStatement self,
					     JExpression[] expr) {
    }

    /**
     * visits a empty statement
     */
    public void visitEmptyStatement(JEmptyStatement self) {
    }

    /**
     * visits a do statement
     */
    public void visitDoStatement(JDoStatement self,
				 JExpression cond,
				 JStatement body) {
    }

    /**
     * visits a continue statement
     */
    public void visitContinueStatement(JContinueStatement self,
				       String label) {
    }

    /**
     * visits a break statement
     */
    public void visitBreakStatement(JBreakStatement self,
				    String label) {
    }

    /**
     * visits an expression statement
     */
    public void visitBlockStatement(JBlock self,
				    JStatement[] body,
				    JavaStyleComment[] comments) {
    }

    /**
     * visits a type declaration statement
     */
    public void visitTypeDeclarationStatement(JTypeDeclarationStatement self,
					      JTypeDeclaration decl) {
    }

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * visits an unary plus expression
     */
    public void visitUnaryPlusExpression(JUnaryExpression self,
					 JExpression expr) {
    }

    /**
     * visits an unary minus expression
     */
    public void visitUnaryMinusExpression(JUnaryExpression self,
					  JExpression expr) {
    }

    /**
     * visits a bitwise complement expression
     */
    public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr) {
    }

    /**
     * visits a logical complement expression
     */
    public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr) {
    }

    /**
     * visits a type name expression
     */
    public void visitTypeNameExpression(JTypeNameExpression self,
					CType type) {
    }

    /**
     * visits a this expression
     */
    public void visitThisExpression(JThisExpression self,
				    JExpression prefix) {
    }

    /**
     * visits a super expression
     */
    public void visitSuperExpression(JSuperExpression self) {
    }

    /**
     * visits a shift expression
     */
    public void visitShiftExpression(JShiftExpression self,
				     int oper,
				     JExpression left,
				     JExpression right) {
    }

    /**
     * visits a shift expressiona
     */
    public void visitRelationalExpression(JRelationalExpression self,
					  int oper,
					  JExpression left,
					  JExpression right) {
    }

    /**
     * visits a prefix expression
     */
    public void visitPrefixExpression(JPrefixExpression self,
				      int oper,
				      JExpression expr) {
    }

    /**
     * visits a postfix expression
     */
    public void visitPostfixExpression(JPostfixExpression self,
				       int oper,
				       JExpression expr) {
    }

    /**
     * visits a parenthesed expression
     */
    public void visitParenthesedExpression(JParenthesedExpression self,
					   JExpression expr) {
    }

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public void visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
						JExpression prefix,
						String ident,
						JExpression[] params,
						JClassDeclaration decl) {
    }
    /**
     * Visits an unqualified instance creation expression.
     */
    public void visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
					       JExpression prefix,
					       String ident,
					       JExpression[] params) {
    }

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    public void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
						  CClassType type,
						  JExpression[] params,
						  JClassDeclaration decl) {
    }

    /**
     * Visits an unqualified instance creation expression.
     */
    public void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
						 CClassType type,
						 JExpression[] params) {
    }

    /**
     * visits an array allocator expression
     */
    public void visitNewArrayExpression(JNewArrayExpression self,
					CType type,
					JExpression[] dims,
					JArrayInitializer init) {
    }

    /**
     * visits a name expression
     */
    public void visitNameExpression(JNameExpression self,
				    JExpression prefix,
				    String ident) {
    }

    /**
     * visits an array allocator expression
     */
    public void visitBinaryExpression(JBinaryExpression self,
				      String oper,
				      JExpression left,
				      JExpression right) {
    }
    /**
     * visits a method call expression
     */
    public void visitMethodCallExpression(JMethodCallExpression self,
					  JExpression prefix,
					  String ident,
					  JExpression[] args) {
    }
    /**
     * visits a local variable expression
     */
    public void visitLocalVariableExpression(JLocalVariableExpression self,
					     String ident) {
    }

    /**
     * visits an instanceof expression
     */
    public void visitInstanceofExpression(JInstanceofExpression self,
					  JExpression expr,
					  CType dest) {
    }

    /**
     * visits an equality expression
     */
    public void visitEqualityExpression(JEqualityExpression self,
					boolean equal,
					JExpression left,
					JExpression right) {
    }

    /**
     * visits a conditional expression
     */
    public void visitConditionalExpression(JConditionalExpression self,
					   JExpression cond,
					   JExpression left,
					   JExpression right) {
    }

    /**
     * visits a compound expression
     */
    public void visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						  int oper,
						  JExpression left,
						  JExpression right) {
    }
    /**
     * visits a field expression
     */
    public void visitFieldExpression(JFieldAccessExpression self,
				     JExpression left,
				     String ident) {
    }

    /**
     * visits a class expression
     */
    public void visitClassExpression(JClassExpression self,
				     CType type) {
    }

    /**
     * visits a cast expression
     */
    public void visitCastExpression(JCastExpression self,
				    JExpression expr,
				    CType type) {
    }

    /**
     * visits a cast expression
     */
    public void visitUnaryPromoteExpression(JUnaryPromote self,
					    JExpression expr,
					    CType type) {
    }

    /**
     * visits a compound assignment expression
     */
    public void visitBitwiseExpression(JBitwiseExpression self,
				       int oper,
				       JExpression left,
				       JExpression right) {
    }
    /**
     * visits an assignment expression
     */
    public void visitAssignmentExpression(JAssignmentExpression self,
					  JExpression left,
					  JExpression right) {
    }

    /**
     * visits an array length expression
     */
    public void visitArrayLengthExpression(JArrayLengthExpression self,
					   JExpression prefix) {
    }

    /**
     * visits an array length expression
     */
    public void visitArrayAccessExpression(JArrayAccessExpression self,
					   JExpression prefix,
					   JExpression accessor) {
    }

    /**
     * visits an array length expression
     */
    public void visitComments(JavaStyleComment[] comments) {
    }

    /**
     * visits an array length expression
     */
    public void visitComment(JavaStyleComment comment) {
    }

    /**
     * visits an array length expression
     */
    public void visitJavadoc(JavadocComment comment) {
    }

    // ----------------------------------------------------------------------
    // OTHERS
    // ----------------------------------------------------------------------

    /**
     * visits an array length expression
     */
    public void visitSwitchLabel(JSwitchLabel self,
				 JExpression expr) {
    }

    /**
     * visits an array length expression
     */
    public void visitSwitchGroup(JSwitchGroup self,
				 JSwitchLabel[] labels,
				 JStatement[] stmts) {
    }

    /**
     * visits an array length expression
     */
    public void visitCatchClause(JCatchClause self,
				 JFormalParameter exception,
				 JBlock body) {
    }

    /**
     * visits an array length expression
     */
    public void visitFormalParameters(JFormalParameter self,
				      boolean isFinal,
				      CType type,
				      String ident) {
    }

    /**
     * visits an array length expression
     */
    public void visitConstructorCall(JConstructorCall self,
				     boolean functorIsThis,
				     JExpression[] params) {
    }

    /**
     * visits an array initializer expression
     */
    public void visitArrayInitializer(JArrayInitializer self,
				      JExpression[] elems) {
    }

    /**
     * visits a boolean literal
     */
    public void visitBooleanLiteral(boolean value) {
    }

    /**
     * visits a byte literal
     */
    public void visitByteLiteral(byte value) {
    }

    /**
     * visits a character literal
     */
    public void visitCharLiteral(char value) {
    }

    /**
     * visits a double literal
     */
    public void visitDoubleLiteral(double value) {
    }

    /**
     * visits a float literal
     */
    public void visitFloatLiteral(float value) {
    }

    /**
     * visits a int literal
     */
    public void visitIntLiteral(int value) {
    }

    /**
     * visits a long literal
     */
    public void visitLongLiteral(long value) {
    }

    /**
     * visits a short literal
     */
    public void visitShortLiteral(short value) {
    }

    /**
     * visits a string literal
     */
    public void visitStringLiteral(String value) {
    }

    /**
     * visits a null literal
     */
    public void visitNullLiteral() {
    }

    /**
     * visits a package name declaration
     */
    public void visitPackageName(String name) {
    }

    /**
     * visits a package import declaration
     */
    public void visitPackageImport(String name) {
    }

    /**
     * visits a class import declaration
     */
    public void visitClassImport(String name) {
    }
}

//===========================================================================
//
//   FILE: AttributeVisitor.java:
//   
//   Author: Michael Gordon
//   Date: Mon Oct  1 22:36:08 2001
//
//   Function:  Implements an interface for a visit which returns an 
//              attribute
//
//===========================================================================


package at.dms.kjc;

import java.util.List;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;

/**
 * Implementation of an Attributed Visitor Design Pattern for KJC.
 *
 */
public interface AttributeVisitor {

    // ----------------------------------------------------------------------
    // COMPILATION UNIT
    // ----------------------------------------------------------------------

    /**
     * visits a compilation unit
     */
    Object visitCompilationUnit(JCompilationUnit self,
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
    Object visitClassDeclaration(JClassDeclaration self,
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
    Object visitClassBody(JTypeDeclaration[] decls,
                          JFieldDeclaration[] fields,
                          JMethodDeclaration[] methods,
                          JPhylum[] body);

    /**
     * visits a class declaration
     */
    Object visitInnerClassDeclaration(JClassDeclaration self,
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
    Object visitInterfaceDeclaration(JInterfaceDeclaration self,
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
    Object visitFieldDeclaration(JFieldDeclaration self,
                                 int modifiers,
                                 CType type,
                                 String ident,
                                 JExpression expr);

    /**
     * visits a method declaration
     */
    Object visitMethodDeclaration(JMethodDeclaration self,
                                  int modifiers,
                                  CType returnType,
                                  String ident,
                                  JFormalParameter[] parameters,
                                  CClassType[] exceptions,
                                  JBlock body);
    /**
     * visits a method declaration
     */
    Object visitConstructorDeclaration(JConstructorDeclaration self,
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
    Object visitWhileStatement(JWhileStatement self,
                               JExpression cond,
                               JStatement body);

    /**
     * visits a variable declaration statement
     */
    Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                             JVariableDefinition[] vars);

    /**
     * visits a variable declaration statement
     */
    Object visitVariableDefinition(JVariableDefinition self,
                                   int modifiers,
                                   CType type,
                                   String ident,
                                   JExpression expr);
    /**
     * visits a try-catch statement
     */
    Object visitTryCatchStatement(JTryCatchStatement self,
                                  JBlock tryClause,
                                  JCatchClause[] catchClauses);

    /**
     * visits a try-finally statement
     */
    Object visitTryFinallyStatement(JTryFinallyStatement self,
                                    JBlock tryClause,
                                    JBlock finallyClause);

    /**
     * visits a throw statement
     */
    Object visitThrowStatement(JThrowStatement self,
                               JExpression expr);

    /**
     * visits a synchronized statement
     */
    Object visitSynchronizedStatement(JSynchronizedStatement self,
                                      JExpression cond,
                                      JStatement body);

    /**
     * visits a switch statement
     */
    Object visitSwitchStatement(JSwitchStatement self,
                                JExpression expr,
                                JSwitchGroup[] body);

    /**
     * visits a return statement
     */
    Object visitReturnStatement(JReturnStatement self,
                                JExpression expr);

    /**
     * visits a labeled statement
     */
    Object visitLabeledStatement(JLabeledStatement self,
                                 String label,
                                 JStatement stmt);

    /**
     * visits a if statement
     */
    Object visitIfStatement(JIfStatement self,
                            JExpression cond,
                            JStatement thenClause,
                            JStatement elseClause);

    /**
     * visits a for statement
     */
    Object visitForStatement(JForStatement self,
                             JStatement init,
                             JExpression cond,
                             JStatement incr,
                             JStatement body);

    /**
     * visits a compound statement
     */
    Object visitCompoundStatement(JCompoundStatement self,
                                  JStatement[] body);

    /**
     * visits an expression statement
     */
    Object visitExpressionStatement(JExpressionStatement self,
                                    JExpression expr);

    /**
     * visits an expression list statement
     */
    Object visitExpressionListStatement(JExpressionListStatement self,
                                        JExpression[] expr);

    /**
     * visits a empty statement
     */
    Object visitEmptyStatement(JEmptyStatement self);

    /**
     * visits a do statement
     */
    Object visitDoStatement(JDoStatement self,
                            JExpression cond,
                            JStatement body);

    /**
     * visits a continue statement
     */
    Object visitContinueStatement(JContinueStatement self,
                                  String label);

    /**
     * visits a break statement
     */
    Object visitBreakStatement(JBreakStatement self,
                               String label);

    /**
     * visits an expression statement
     */
    Object visitBlockStatement(JBlock self,
                               JavaStyleComment[] comments);

    /**
     * visits a type declaration statement
     */
    Object visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                         JTypeDeclaration decl);
    
    /** Visit text to be emitted unchanged in a compiler backend. */
    Object visitEmittedText(JEmittedText self);

    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * visits an unary plus expression
     */
    Object visitUnaryPlusExpression(JUnaryExpression self,
                                    JExpression expr);

    /**
     * visits an unary minus expression
     */
    Object visitUnaryMinusExpression(JUnaryExpression self,
                                     JExpression expr);

    /**
     * visits a bitwise complement expression
     */
    Object visitBitwiseComplementExpression(JUnaryExpression self,
                                            JExpression expr);

    /**
     * visits a logical complement expression
     */
    Object visitLogicalComplementExpression(JUnaryExpression self,
                                            JExpression expr);

    /**
     * visits a type name expression
     */
    Object visitTypeNameExpression(JTypeNameExpression self,
                                   CType type);

    /**
     * visits a this expression
     */
    Object visitThisExpression(JThisExpression self,
                               JExpression prefix);

    /**
     * visits a super expression
     */
    Object visitSuperExpression(JSuperExpression self);

    /**
     * visits a shift expression
     */
    Object visitShiftExpression(JShiftExpression self,
                                int oper,
                                JExpression left,
                                JExpression right);

    /**
     * visits a shift expressiona
     */
    Object visitRelationalExpression(JRelationalExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right);

    /**
     * visits a prefix expression
     */
    Object visitPrefixExpression(JPrefixExpression self,
                                 int oper,
                                 JExpression expr);

    /**
     * visits a postfix expression
     */
    Object visitPostfixExpression(JPostfixExpression self,
                                  int oper,
                                  JExpression expr);

    /**
     * visits a parenthesed expression
     */
    Object visitParenthesedExpression(JParenthesedExpression self,
                                      JExpression expr);

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    Object visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
                                           JExpression prefix,
                                           String ident,
                                           JExpression[] params,
                                           JClassDeclaration decl);
    /**
     * Visits an unqualified instance creation expression.
     */
    Object visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] params);

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    Object visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                             CClassType type,
                                             JExpression[] params,
                                             JClassDeclaration decl);

    /**
     * Visits an unqualified instance creation expression.
     */
    Object visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                            CClassType type,
                                            JExpression[] params);

    /**
     * visits an array allocator expression
     */
    Object visitNewArrayExpression(JNewArrayExpression self,
                                   CType type,
                                   JExpression[] dims,
                                   JArrayInitializer init);

    /**
     * visits a name expression
     */
    Object visitNameExpression(JNameExpression self,
                               JExpression prefix,
                               String ident);

    /**
     * visits an array allocator expression
     */
    Object visitBinaryExpression(JBinaryExpression self,
                                 String oper,
                                 JExpression left,
                                 JExpression right);
    /**
     * visits a method call expression
     */
    Object visitMethodCallExpression(JMethodCallExpression self,
                                     JExpression prefix,
                                     String ident,
                                     JExpression[] args);
    /**
     * visits a local variable expression
     */
    Object visitLocalVariableExpression(JLocalVariableExpression self,
                                        String ident);

    /**
     * visits an instanceof expression
     */
    Object visitInstanceofExpression(JInstanceofExpression self,
                                     JExpression expr,
                                     CType dest);

    /**
     * visits an equality expression
     */
    Object visitEqualityExpression(JEqualityExpression self,
                                   boolean equal,
                                   JExpression left,
                                   JExpression right);

    /**
     * visits a conditional expression
     */
    Object visitConditionalExpression(JConditionalExpression self,
                                      JExpression cond,
                                      JExpression left,
                                      JExpression right);

    /**
     * visits a compound expression
     */
    Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                             int oper,
                                             JExpression left,
                                             JExpression right);
    /**
     * visits a field expression
     */
    Object visitFieldExpression(JFieldAccessExpression self,
                                JExpression left,
                                String ident);

    /**
     * visits a class expression
     */
    Object visitClassExpression(JClassExpression self,
                                CType type);

    /**
     * visits a cast expression
     */
    Object visitCastExpression(JCastExpression self,
                               JExpression expr,
                               CType type);

    /**
     * visits a cast expression
     */
    Object visitUnaryPromoteExpression(JUnaryPromote self,
                                       JExpression expr,
                                       CType type);

    /**
     * visits a compound assignment expression
     */
    Object visitBitwiseExpression(JBitwiseExpression self,
                                  int oper,
                                  JExpression left,
                                  JExpression right);
    /**
     * visits an assignment expression
     */
    Object visitAssignmentExpression(JAssignmentExpression self,
                                     JExpression left,
                                     JExpression right);

    /**
     * visits an array length expression
     */
    Object visitArrayLengthExpression(JArrayLengthExpression self,
                                      JExpression prefix);

    /**
     * visits an array length expression
     */
    Object visitArrayAccessExpression(JArrayAccessExpression self,
                                      JExpression prefix,
                                      JExpression accessor);

    /**
     * visits an array length expression
     */
    Object visitComments(JavaStyleComment[] comments);

    /**
     * visits an array length expression
     */
    Object visitComment(JavaStyleComment comment);

    /**
     * visits an array length expression
     */
    Object visitJavadoc(JavadocComment comment);

    // ----------------------------------------------------------------------
    // OTHERS
    // ----------------------------------------------------------------------

    /**
     * visits an array length expression
     */
    Object visitSwitchLabel(JSwitchLabel self,
                            JExpression expr);

    /**
     * visits an array length expression
     */
    Object visitSwitchGroup(JSwitchGroup self,
                            JSwitchLabel[] labels,
                            JStatement[] stmts);

    /**
     * visits an array length expression
     */
    Object visitCatchClause(JCatchClause self,
                            JFormalParameter exception,
                            JBlock body);

    /**
     * visits an array length expression
     */
    Object visitFormalParameters(JFormalParameter self,
                                 boolean isFinal,
                                 CType type,
                                 String ident);

    /**
     * visits an array length expression
     */
    Object visitConstructorCall(JConstructorCall self,
                                boolean functorIsThis,
                                JExpression[] params);

    /**
     * visits an array initializer expression
     */
    Object visitArrayInitializer(JArrayInitializer self,
                                 JExpression[] elems);

    /**
     * visits a boolean literal
     */
    Object visitBooleanLiteral(JBooleanLiteral self,
                               boolean value);

    /**
     * visits a byte literal
     */
    Object visitByteLiteral(JByteLiteral self,
                            byte value);

    /**
     * visits a character literal
     */
    Object visitCharLiteral(JCharLiteral self,
                            char value);

    /**
     * visits a double literal
     */
    Object visitDoubleLiteral(JDoubleLiteral self,
                              double value);

    /**
     * visits a float literal
     */
    Object visitFloatLiteral(JFloatLiteral self,
                             float value);

    /**
     * visits a int literal
     */
    Object visitIntLiteral(JIntLiteral self,
                           int value);

    /**
     * visits a long literal
     */
    Object visitLongLiteral(JLongLiteral self,
                            long value);

    /**
     * visits a short literal
     */
    Object visitShortLiteral(JShortLiteral self,
                             short value);

    /**
     * visits a string literal
     */
    Object visitStringLiteral(JStringLiteral self,
                              String value);

    /**
     * visits a null literal
     */
    Object visitNullLiteral(JNullLiteral self);

    /**
     * visits a package name declaration
     */
    Object visitPackageName(String name);

    /**
     * visits a package import declaration
     */
    Object visitPackageImport(String name);

    /**
     * visits a class import declaration
     */
    Object visitClassImport(String name);
}

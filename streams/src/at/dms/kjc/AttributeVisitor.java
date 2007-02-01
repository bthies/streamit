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
public interface AttributeVisitor<T> {

    // ----------------------------------------------------------------------
    // COMPILATION UNIT
    // ----------------------------------------------------------------------

    /**
     * visits a compilation unit
     */
    T visitCompilationUnit(JCompilationUnit self,
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
    T visitClassDeclaration(JClassDeclaration self,
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
    T visitClassBody(JTypeDeclaration[] decls,
                          JFieldDeclaration[] fields,
                          JMethodDeclaration[] methods,
                          JPhylum[] body);

    /**
     * visits a class declaration
     */
    T visitInnerClassDeclaration(JClassDeclaration self,
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
    T visitInterfaceDeclaration(JInterfaceDeclaration self,
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
    T visitFieldDeclaration(JFieldDeclaration self,
                                 int modifiers,
                                 CType type,
                                 String ident,
                                 JExpression expr);

    /**
     * visits a method declaration
     */
    T visitMethodDeclaration(JMethodDeclaration self,
                                  int modifiers,
                                  CType returnType,
                                  String ident,
                                  JFormalParameter[] parameters,
                                  CClassType[] exceptions,
                                  JBlock body);
    /**
     * visits a method declaration
     */
    T visitConstructorDeclaration(JConstructorDeclaration self,
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
    T visitWhileStatement(JWhileStatement self,
                               JExpression cond,
                               JStatement body);

    /**
     * visits a variable declaration statement
     */
    T visitVariableDeclarationStatement(JVariableDeclarationStatement self,
                                             JVariableDefinition[] vars);

    /**
     * visits a variable declaration statement
     */
    T visitVariableDefinition(JVariableDefinition self,
                                   int modifiers,
                                   CType type,
                                   String ident,
                                   JExpression expr);
    /**
     * visits a try-catch statement
     */
    T visitTryCatchStatement(JTryCatchStatement self,
                                  JBlock tryClause,
                                  JCatchClause[] catchClauses);

    /**
     * visits a try-finally statement
     */
    T visitTryFinallyStatement(JTryFinallyStatement self,
                                    JBlock tryClause,
                                    JBlock finallyClause);

    /**
     * visits a throw statement
     */
    T visitThrowStatement(JThrowStatement self,
                               JExpression expr);

    /**
     * visits a synchronized statement
     */
    T visitSynchronizedStatement(JSynchronizedStatement self,
                                      JExpression cond,
                                      JStatement body);

    /**
     * visits a switch statement
     */
    T visitSwitchStatement(JSwitchStatement self,
                                JExpression expr,
                                JSwitchGroup[] body);

    /**
     * visits a return statement
     */
    T visitReturnStatement(JReturnStatement self,
                                JExpression expr);

    /**
     * visits a labeled statement
     */
    T visitLabeledStatement(JLabeledStatement self,
                                 String label,
                                 JStatement stmt);

    /**
     * visits a if statement
     */
    T visitIfStatement(JIfStatement self,
                            JExpression cond,
                            JStatement thenClause,
                            JStatement elseClause);

    /**
     * visits a for statement
     */
    T visitForStatement(JForStatement self,
                             JStatement init,
                             JExpression cond,
                             JStatement incr,
                             JStatement body);

    /**
     * visits a compound statement
     */
    T visitCompoundStatement(JCompoundStatement self,
                                  JStatement[] body);

    /**
     * visits an expression statement
     */
    T visitExpressionStatement(JExpressionStatement self,
                                    JExpression expr);

    /**
     * visits an expression list statement
     */
    T visitExpressionListStatement(JExpressionListStatement self,
                                        JExpression[] expr);

    /**
     * visits a empty statement
     */
    T visitEmptyStatement(JEmptyStatement self);

    /**
     * visits a do statement
     */
    T visitDoStatement(JDoStatement self,
                            JExpression cond,
                            JStatement body);

    /**
     * visits a continue statement
     */
    T visitContinueStatement(JContinueStatement self,
                                  String label);

    /**
     * visits a break statement
     */
    T visitBreakStatement(JBreakStatement self,
                               String label);

    /**
     * visits an expression statement
     */
    T visitBlockStatement(JBlock self,
                               JavaStyleComment[] comments);

    /**
     * visits a type declaration statement
     */
    T visitTypeDeclarationStatement(JTypeDeclarationStatement self,
                                         JTypeDeclaration decl);
    
    // ----------------------------------------------------------------------
    // EXPRESSION
    // ----------------------------------------------------------------------

    /**
     * visits an unary plus expression
     */
    T visitUnaryPlusExpression(JUnaryExpression self,
                                    JExpression expr);

    /**
     * visits an unary minus expression
     */
    T visitUnaryMinusExpression(JUnaryExpression self,
                                     JExpression expr);

    /**
     * visits a bitwise complement expression
     */
    T visitBitwiseComplementExpression(JUnaryExpression self,
                                            JExpression expr);

    /**
     * visits a logical complement expression
     */
    T visitLogicalComplementExpression(JUnaryExpression self,
                                            JExpression expr);

    /**
     * visits a type name expression
     */
    T visitTypeNameExpression(JTypeNameExpression self,
                                   CType type);

    /**
     * visits a this expression
     */
    T visitThisExpression(JThisExpression self,
                               JExpression prefix);

    /**
     * visits a super expression
     */
    T visitSuperExpression(JSuperExpression self);

    /**
     * visits a shift expression
     */
    T visitShiftExpression(JShiftExpression self,
                                int oper,
                                JExpression left,
                                JExpression right);

    /**
     * visits a shift expressiona
     */
    T visitRelationalExpression(JRelationalExpression self,
                                     int oper,
                                     JExpression left,
                                     JExpression right);

    /**
     * visits a prefix expression
     */
    T visitPrefixExpression(JPrefixExpression self,
                                 int oper,
                                 JExpression expr);

    /**
     * visits a postfix expression
     */
    T visitPostfixExpression(JPostfixExpression self,
                                  int oper,
                                  JExpression expr);

    /**
     * visits a parenthesed expression
     */
    T visitParenthesedExpression(JParenthesedExpression self,
                                      JExpression expr);

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    T visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
                                           JExpression prefix,
                                           String ident,
                                           JExpression[] params,
                                           JClassDeclaration decl);
    /**
     * Visits an unqualified instance creation expression.
     */
    T visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
                                          JExpression prefix,
                                          String ident,
                                          JExpression[] params);

    /**
     * Visits an unqualified anonymous class instance creation expression.
     */
    T visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
                                             CClassType type,
                                             JExpression[] params,
                                             JClassDeclaration decl);

    /**
     * Visits an unqualified instance creation expression.
     */
    T visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
                                            CClassType type,
                                            JExpression[] params);

    /**
     * visits an array allocator expression
     */
    T visitNewArrayExpression(JNewArrayExpression self,
                                   CType type,
                                   JExpression[] dims,
                                   JArrayInitializer init);

    /**
     * visits a name expression
     */
    T visitNameExpression(JNameExpression self,
                               JExpression prefix,
                               String ident);

    /**
     * visits an array allocator expression
     */
    T visitBinaryExpression(JBinaryExpression self,
                                 String oper,
                                 JExpression left,
                                 JExpression right);
    /**
     * visits a method call expression
     */
    T visitMethodCallExpression(JMethodCallExpression self,
                                     JExpression prefix,
                                     String ident,
                                     JExpression[] args);
    /**
     * visits a local variable expression
     */
    T visitLocalVariableExpression(JLocalVariableExpression self,
                                        String ident);

    /**
     * visits an instanceof expression
     */
    T visitInstanceofExpression(JInstanceofExpression self,
                                     JExpression expr,
                                     CType dest);

    /**
     * visits an equality expression
     */
    T visitEqualityExpression(JEqualityExpression self,
                                   boolean equal,
                                   JExpression left,
                                   JExpression right);

    /**
     * visits a conditional expression
     */
    T visitConditionalExpression(JConditionalExpression self,
                                      JExpression cond,
                                      JExpression left,
                                      JExpression right);

    /**
     * visits a compound expression
     */
    T visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
                                             int oper,
                                             JExpression left,
                                             JExpression right);
    /**
     * visits a field expression
     */
    T visitFieldExpression(JFieldAccessExpression self,
                                JExpression left,
                                String ident);

    /**
     * visits a class expression
     */
    T visitClassExpression(JClassExpression self,
                                CType type);

    /**
     * visits a cast expression
     */
    T visitCastExpression(JCastExpression self,
                               JExpression expr,
                               CType type);

    /**
     * visits a cast expression
     */
    T visitUnaryPromoteExpression(JUnaryPromote self,
                                       JExpression expr,
                                       CType type);

    /**
     * visits a compound assignment expression
     */
    T visitBitwiseExpression(JBitwiseExpression self,
                                  int oper,
                                  JExpression left,
                                  JExpression right);
    /**
     * visits an assignment expression
     */
    T visitAssignmentExpression(JAssignmentExpression self,
                                     JExpression left,
                                     JExpression right);

    /**
     * visits an array length expression
     */
    T visitArrayLengthExpression(JArrayLengthExpression self,
                                      JExpression prefix);

    /**
     * visits an array length expression
     */
    T visitArrayAccessExpression(JArrayAccessExpression self,
                                      JExpression prefix,
                                      JExpression accessor);


    /** Visit text to be emitted unchanged in a compiler backend. 
     * @param parts a list of embedded objects which might include JExpressions.
     */
    T visitEmittedTextExpression(JEmittedTextExpression self, Object[] parts);


    /**
     * visits an array length expression
     */
    T visitComments(JavaStyleComment[] comments);

    /**
     * visits an array length expression
     */
    T visitComment(JavaStyleComment comment);

    /**
     * visits an array length expression
     */
    T visitJavadoc(JavadocComment comment);

    // ----------------------------------------------------------------------
    // OTHERS
    // ----------------------------------------------------------------------

    /**
     * visits an array length expression
     */
    T visitSwitchLabel(JSwitchLabel self,
                            JExpression expr);

    /**
     * visits an array length expression
     */
    T visitSwitchGroup(JSwitchGroup self,
                            JSwitchLabel[] labels,
                            JStatement[] stmts);

    /**
     * visits an array length expression
     */
    T visitCatchClause(JCatchClause self,
                            JFormalParameter exception,
                            JBlock body);

    /**
     * visits an array length expression
     */
    T visitFormalParameters(JFormalParameter self,
                                 boolean isFinal,
                                 CType type,
                                 String ident);

    /**
     * visits an array length expression
     */
    T visitConstructorCall(JConstructorCall self,
                                boolean functorIsThis,
                                JExpression[] params);

    /**
     * visits an array initializer expression
     */
    T visitArrayInitializer(JArrayInitializer self,
                                 JExpression[] elems);

    /**
     * visits a boolean literal
     */
    T visitBooleanLiteral(JBooleanLiteral self,
                               boolean value);

    /**
     * visits a byte literal
     */
    T visitByteLiteral(JByteLiteral self,
                            byte value);

    /**
     * visits a character literal
     */
    T visitCharLiteral(JCharLiteral self,
                            char value);

    /**
     * visits a double literal
     */
    T visitDoubleLiteral(JDoubleLiteral self,
                              double value);

    /**
     * visits a float literal
     */
    T visitFloatLiteral(JFloatLiteral self,
                             float value);

    /**
     * visits a int literal
     */
    T visitIntLiteral(JIntLiteral self,
                           int value);

    /**
     * visits a long literal
     */
    T visitLongLiteral(JLongLiteral self,
                            long value);

    /**
     * visits a short literal
     */
    T visitShortLiteral(JShortLiteral self,
                             short value);

    /**
     * visits a string literal
     */
    T visitStringLiteral(JStringLiteral self,
                              String value);

    /**
     * visits a null literal
     */
    T visitNullLiteral(JNullLiteral self);

    /**
     * visits a package name declaration
     */
    T visitPackageName(String name);

    /**
     * visits a package import declaration
     */
    T visitPackageImport(String name);

    /**
     * visits a class import declaration
     */
    T visitClassImport(String name);
}

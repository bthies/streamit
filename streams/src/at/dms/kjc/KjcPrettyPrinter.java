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
 * $Id: KjcPrettyPrinter.java,v 1.4 2003-05-28 05:58:45 thies Exp $
 */

package at.dms.kjc;

import java.util.StringTokenizer;
import java.util.List;

import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.TabbedPrintWriter;
import at.dms.util.InconsistencyException;

/**
 * This class implements a Java pretty printer
 */
public class KjcPrettyPrinter extends at.dms.util.Utils implements Constants, KjcVisitor {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * construct a pretty printer object for java code
   */
  public KjcPrettyPrinter() {
  }

  /**
   * construct a pretty printer object for java code
   * @param	fileName		the file into the code is generated
   */
  public KjcPrettyPrinter(String fileName) {
    try {
      this.p = new TabbedPrintWriter(
	 new java.io.BufferedWriter(new java.io.FileWriter(fileName)));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("cant write: " + fileName + e);
    }
  }

  /**
   * construct a pretty printer object for java code
   * @param	fileName		the file into the code is generated
   */
  public KjcPrettyPrinter(TabbedPrintWriter p) {
    this.p = p;
    this.pos = 0;
  }

  /**
   * Close the stream at the end
   */
  public void close() {
    p.close();
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

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
      if (importedPackages.length + importedClasses.length > 0) {
	newLine();
      }
    }

    for (int i = 0; i < importedPackages.length ; i++) {
      if (!importedPackages[i].getName().equals("java/lang")) {
	importedPackages[i].accept(this);
	newLine();
      }
    }

    for (int i = 0; i < importedClasses.length ; i++) {
      importedClasses[i].accept(this);
      newLine();
    }

    for (int i = 0; i < typeDeclarations.length ; i++) {
      newLine();
      typeDeclarations[i].accept(this);
      newLine();
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
    newLine();
    print(CModifier.toString(modifiers));
    print("class " + ident);

    if (superName != null) {
      print(" extends " + superName.replace('/', '.'));
    }

    if (interfaces.length != 0) {
      print(" implements ");
      for (int i = 0; i < interfaces.length; i++) {
	if (i != 0) {
	  print(", ");
	}
	print(interfaces[i]);
      }
    }

    print(" ");
    visitClassBody(decls, fields, methods, body);
  }

  /**
   *
   */
  public void visitClassBody(JTypeDeclaration[] decls,
			     JFieldDeclaration[] fields,
			     JMethodDeclaration[] methods,
			     JPhylum[] body) {
    print("{");
    pos += TAB_SIZE;
    for (int i = 0; i < decls.length ; i++) {
      decls[i].accept(this);
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

    pos -= TAB_SIZE;
    newLine();
    print("}");
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
    print(" {");
    pos += TAB_SIZE;
    for (int i = 0; i < decls.length ; i++) {
      decls[i].accept(this);
    }
    for (int i = 0; i < methods.length ; i++) {
      methods[i].accept(this);
    }
    for (int i = 0; i < body.length ; i++) {
      body[i].accept(this);
    }
    pos -= TAB_SIZE;
    newLine();
    print("}");
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
    newLine();
    print(CModifier.toString(modifiers));
    print("interface " + ident);

    if (interfaces.length != 0) {
      print(" extends ");
      for (int i = interfaces.length - 1; i >= 0; i--) {
	print((i == 0 ? "" : ",") + interfaces[i]);
      }
    }

    print(" {");
    pos += TAB_SIZE;
    for (int i = 0; i < body.length; i++) {
      body[i].accept(this);
    }
    for (int i = 0; i < methods.length; i++) {
      methods[i].accept(this);
    }
    pos -= TAB_SIZE;
    newLine();
    print("}");
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
    if (ident.indexOf("$") != -1) {
      return; // dont print generated elements
    }

    newLine();
    print(CModifier.toString(modifiers));
    print(type);
    print("\t");
    print(ident);
    if (expr != null) {
      print("\t= ");
      expr.accept(this);
    }
    print(";");
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
    if (ident.equals(JAV_STATIC_INIT) || ident.equals(JAV_INIT)) {
      return; // we do not want to generate this methods in source code
    }

    newLine();
    print(CModifier.toString(modifiers));
    print(returnType);
    print(" ");
    print(ident);
    print("(");
    int count = 0;

    for (int i = 0; i < parameters.length; i++) {
      if (count != 0) {
	print(", ");
      }

      if (!parameters[i].isGenerated()) {
	parameters[i].accept(this);
	count++;
      }
    }
    print(")");

    for (int i = 0; i < exceptions.length; i++) {
      if (i != 0) {
	print(", ");
      } else {
	print(" throws ");
      }
      print(exceptions[i].toString());
    }

    print(" ");
    if (body != null) {
      body.accept(this);
    } else {
      print(";");
    }
    newLine();
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
    newLine();
    print(CModifier.toString(modifiers));
    print(ident);
    print("(");
    int count = 0;
    for (int i = 0; i < parameters.length; i++) {
      if (count != 0) {
	print(", ");
      }
      if (!parameters[i].isGenerated()) {
	parameters[i].accept(this);
	count++;
      }
    }
    print(")");
    for (int i = 0; i < exceptions.length; i++) {
      if (i != 0) {
	print(", ");
      } else {
	print(" throws ");
      }
      print(exceptions[i].toString());
    }
    print(" ");
    body.accept(this);
    newLine();
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
    print("while (");
    cond.accept(this);
    print(") ");

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
    print(CModifier.toString(modifiers));
    print(type);
    print("\t");
    print(ident);
    if (expr != null) {
      print("\t= ");
      expr.accept(this);
    }
    print(";");
  }

  /**
   * prints a try-catch statement
   */
  public void visitTryCatchStatement(JTryCatchStatement self,
				     JBlock tryClause,
				     JCatchClause[] catchClauses) {
    print("try ");
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
    print("try ");
    tryClause.accept(this);
    if (finallyClause != null) {
      print(" finally ");
      finallyClause.accept(this);
    }
  }

  /**
   * prints a throw statement
   */
  public void visitThrowStatement(JThrowStatement self,
				  JExpression expr) {
    print("throw ");
    expr.accept(this);
    print(";");
  }

  /**
   * prints a synchronized statement
   */
  public void visitSynchronizedStatement(JSynchronizedStatement self,
					 JExpression cond,
					 JStatement body) {
    print("synchronized (");
    cond.accept(this);
    print(") ");
    body.accept(this);
  }

  /**
   * prints a switch statement
   */
  public void visitSwitchStatement(JSwitchStatement self,
				   JExpression expr,
				   JSwitchGroup[] body) {
    print("switch (");
    expr.accept(this);
    print(") {");
    for (int i = 0; i < body.length; i++) {
      body[i].accept(this);
    }
    newLine();
    print("}");
  }

  /**
   * prints a return statement
   */
  public void visitReturnStatement(JReturnStatement self,
				   JExpression expr) {
    print("return");
    if (expr != null) {
      print(" ");
      expr.accept(this);
    }
    print(";");
  }

  /**
   * prints a labeled statement
   */
  public void visitLabeledStatement(JLabeledStatement self,
				    String label,
				    JStatement stmt) {
    print(label + ":");
    stmt.accept(this);
  }

  /**
   * prints a if statement
   */
  public void visitIfStatement(JIfStatement self,
			       JExpression cond,
			       JStatement thenClause,
			       JStatement elseClause) {
    print("if (");
    cond.accept(this);
    print(") ");
    pos += thenClause instanceof JBlock ? 0 : TAB_SIZE;
    thenClause.accept(this);
    pos -= thenClause instanceof JBlock ? 0 : TAB_SIZE;
    if (elseClause != null) {
      if ((elseClause instanceof JBlock) || (elseClause instanceof JIfStatement)) {
	print(" ");
      } else {
	newLine();
      }
      print("else ");
      pos += elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
      elseClause.accept(this);
      pos -= elseClause instanceof JBlock || elseClause instanceof JIfStatement ? 0 : TAB_SIZE;
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
    print("for (");
    forInit = true;
    if (init != null) {
      init.accept(this);
    } else {
      print(";");
    }
    forInit = false;

    print(" ");
    if (cond != null) {
      cond.accept(this);
    }
    print("; ");

    if (incr != null) {
      incr.accept(this);
    }
    print(") ");

    print("{");
    pos += TAB_SIZE;
    body.accept(this);
    pos -= TAB_SIZE;
    newLine();
    print("}");
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
      if (body[i] instanceof JIfStatement &&
	  i < body.length - 1 &&
	  !(body[i + 1] instanceof JReturnStatement)) {
	newLine();
      }
      if (body[i] instanceof JReturnStatement && i > 0) {
	newLine();
      }

      newLine();
      body[i].accept(this);

      if (body[i] instanceof JVariableDeclarationStatement &&
	  i < body.length - 1 &&
	  !(body[i + 1] instanceof JVariableDeclarationStatement)) {
	newLine();
      }
    }
  }

  /**
   * prints an expression statement
   */
  public void visitExpressionStatement(JExpressionStatement self,
				       JExpression expr) {
    expr.accept(this);
    if (!forInit) {
      print(";");
    }
  }

  /**
   * prints an expression list statement
   */
  public void visitExpressionListStatement(JExpressionListStatement self,
					   JExpression[] expr) {
    for (int i = 0; i < expr.length; i++) {
      if (i != 0) {
	print(", ");
      }
      expr[i].accept(this);
    }
    if (forInit) {
      print(";");
    }
  }

  /**
   * prints a empty statement
   */
  public void visitEmptyStatement(JEmptyStatement self) {
    newLine();
    print(";");
  }

  /**
   * prints a do statement
   */
  public void visitDoStatement(JDoStatement self,
			       JExpression cond,
			       JStatement body) {
    newLine();
    print("do ");
    body.accept(this);
    print("");
    print("while (");
    cond.accept(this);
    print(");");
  }

  /**
   * prints a continue statement
   */
  public void visitContinueStatement(JContinueStatement self,
				     String label) {
    newLine();
    print("continue");
    if (label != null) {
      print(" " + label);
    }
    print(";");
  }

  /**
   * prints a break statement
   */
  public void visitBreakStatement(JBreakStatement self,
				  String label) {
    newLine();
    print("break");
    if (label != null) {
      print(" " + label);
    }
    print(";");
  }

  /**
   * prints an expression statement
   */
  public void visitBlockStatement(JBlock self,
				  JavaStyleComment[] comments) {
    print("{");
    pos += TAB_SIZE;
    visitCompoundStatement(self.getStatementArray());
    if (comments != null) {
      visitComments(comments);
    }
    pos -= TAB_SIZE;
    newLine();
    print("}");
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
    print("+");
    expr.accept(this);
  }

  /**
   * prints an unary minus expression
   */
  public void visitUnaryMinusExpression(JUnaryExpression self,
					JExpression expr)
  {
    print("-");
    expr.accept(this);
  }

  /**
   * prints a bitwise complement expression
   */
  public void visitBitwiseComplementExpression(JUnaryExpression self,
						 JExpression expr)
  {
    print("~");
    expr.accept(this);
  }

  /**
   * prints a logical complement expression
   */
  public void visitLogicalComplementExpression(JUnaryExpression self,
						 JExpression expr)
  {
    print("!");
    expr.accept(this);
  }

  /**
   * prints a type name expression
   */
  public void visitTypeNameExpression(JTypeNameExpression self,
				      CType type) {
    print(type);
  }

  /**
   * prints a this expression
   */
  public void visitThisExpression(JThisExpression self,
				  JExpression prefix) {
    if (prefix != null) {
      prefix.accept(this);
      print(".this");
    } else {
      print("this");
    }
  }

  /**
   * prints a super expression
   */
  public void visitSuperExpression(JSuperExpression self) {
    print("super");
  }

  /**
   * prints a shift expression
   */
  public void visitShiftExpression(JShiftExpression self,
				   int oper,
				   JExpression left,
				   JExpression right) {
    left.accept(this);
    if (oper == OPE_SL) {
      print(" << ");
    } else if (oper == OPE_SR) {
      print(" >> ");
    } else {
      print(" >>> ");
    }
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
    switch (oper) {
    case OPE_LT:
      print(" < ");
      break;
    case OPE_LE:
      print(" <= ");
      break;
    case OPE_GT:
      print(" > ");
      break;
    case OPE_GE:
      print(" >= ");
      break;
    default:
      throw new InconsistencyException();
    }
    right.accept(this);
  }

  /**
   * prints a prefix expression
   */
  public void visitPrefixExpression(JPrefixExpression self,
				    int oper,
				    JExpression expr) {
    if (oper == OPE_PREINC) {
      print("++");
    } else {
      print("--");
    }
    expr.accept(this);
  }

  /**
   * prints a postfix expression
   */
  public void visitPostfixExpression(JPostfixExpression self,
				     int oper,
				     JExpression expr) {
    expr.accept(this);
    if (oper == OPE_POSTINC) {
      print("++");
    } else {
      print("--");
    }
  }

  /**
   * prints a parenthesed expression
   */
  public void visitParenthesedExpression(JParenthesedExpression self,
					 JExpression expr) {
    print("(");
    expr.accept(this);
    print(")");
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
    print(".new " + ident + "(");
    visitArgs(params);
    print(")");
    decl.genInnerJavaCode(this);
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
    print(".new " + ident + "(");
    visitArgs(params);
    print(")");
  }

  /**
   * Prints an unqualified anonymous class instance creation expression.
   */
  public void visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
						CClassType type,
						JExpression[] params,
						JClassDeclaration decl)
  {
    print("new " + type + "(");
    visitArgs(params);
    print(")");
    decl.genInnerJavaCode(this);
  }

  /**
   * Prints an unqualified instance creation expression.
   */
  public void visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self,
					       CClassType type,
					       JExpression[] params)
  {
    print("new " + type + "(");
    visitArgs(params);
    print(")");
  }

  /**
   * prints an array allocator expression
   */
  public void visitNewArrayExpression(JNewArrayExpression self,
				      CType type,
				      JExpression[] dims,
				      JArrayInitializer init)
  {
    print("new ");
    print(type);
    for (int i = 0; i < dims.length; i++) {
      print("[");
      if (dims[i] != null) {
	dims[i].accept(this);
      }
      print("]");
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
      print('.');
    }
    print(ident);
  }

  /**
   * prints an array allocator expression
   */
  public void visitBinaryExpression(JBinaryExpression self,
				    String oper,
				    JExpression left,
				    JExpression right) {
    left.accept(this);
    print(" ");
    print(oper);
    print(" ");
    right.accept(this);
  }

  /**
   * prints a method call expression
   */
  public void visitMethodCallExpression(JMethodCallExpression self,
					JExpression prefix,
					String ident,
					JExpression[] args) {
    if (ident != null && ident.equals(JAV_INIT)) {
      return; // we do not want generated methods in source code
    }

    if (prefix != null) {
      prefix.accept(this);
      print('.');
    }
    print(ident);
    print("(");
    visitArgs(args);
    print(")");
  }

  /**
   * prints a local variable expression
   */
  public void visitLocalVariableExpression(JLocalVariableExpression self,
					   String ident) {
    print(ident);
  }

  /**
   * prints an instanceof expression
   */
  public void visitInstanceofExpression(JInstanceofExpression self,
					JExpression expr,
					CType dest) {
    expr.accept(this);
    print(" instanceof ");
    print(dest);
  }

  /**
   * prints an equality expression
   */
  public void visitEqualityExpression(JEqualityExpression self,
				      boolean equal,
				      JExpression left,
				      JExpression right) {
    left.accept(this);
    print(equal ? " == " : " != ");
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
    print(" ? ");
    left.accept(this);
    print(" : ");
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
    switch (oper) {
    case OPE_STAR:
      print(" *= ");
      break;
    case OPE_SLASH:
      print(" /= ");
      break;
    case OPE_PERCENT:
      print(" %= ");
      break;
    case OPE_PLUS:
      print(" += ");
      break;
    case OPE_MINUS:
      print(" -= ");
      break;
    case OPE_SL:
      print(" <<= ");
      break;
    case OPE_SR:
      print(" >>= ");
      break;
    case OPE_BSR:
      print(" >>>= ");
      break;
    case OPE_BAND:
      print(" &= ");
      break;
    case OPE_BXOR:
      print(" ^= ");
      break;
    case OPE_BOR:
      print(" |= ");
      break;
    }
    right.accept(this);
  }

 /**
   * prints a field expression
   */
  public void visitFieldExpression(JFieldAccessExpression self,
				   JExpression left,
				   String ident)
  {
    if (ident.equals(JAV_OUTER_THIS)) {// don't generate generated fields
      print(left.getType().getCClass().getOwner().getType() + ".this");
      return;
    }
    int		index = ident.indexOf("_$");
    if (index != -1) {
      print(ident.substring(0, index));      // local var
    } else {
      left.accept(this);
      print("." + ident);
    }
  }

  /**
   * prints a class expression
   */
  public void visitClassExpression(JClassExpression self, CType type) {
    print(type);
    print(".class");
  }

  /**
   * prints a cast expression
   */
  public void visitCastExpression(JCastExpression self,
				  JExpression expr,
				  CType type)
  {
    print("(");
    print(type);
    print(")");
    expr.accept(this);
  }

  /**
   * prints a cast expression
   */
  public void visitUnaryPromoteExpression(JUnaryPromote self,
					  JExpression expr,
					  CType type)
  {
    print("(");
    print(type);
    print(")");
    print("(");
    expr.accept(this);
    print(")");
  }

  /**
   * prints a compound assignment expression
   */
  public void visitBitwiseExpression(JBitwiseExpression self,
				     int oper,
				     JExpression left,
				     JExpression right) {
    left.accept(this);
    switch (oper) {
    case OPE_BAND:
      print(" & ");
      break;
    case OPE_BOR:
      print(" | ");
      break;
    case OPE_BXOR:
      print(" ^ ");
      break;
    default:
      throw new InconsistencyException();
    }
    right.accept(this);
  }

  /**
   * prints an assignment expression
   */
  public void visitAssignmentExpression(JAssignmentExpression self,
					JExpression left,
					JExpression right) {
    if ((left instanceof JFieldAccessExpression) &&
	((JFieldAccessExpression)left).getField().getIdent().equals(Constants.JAV_OUTER_THIS)) {
      return;
    }

    left.accept(this);
    print(" = ");
    right.accept(this);
  }

  /**
   * prints an array length expression
   */
  public void visitArrayLengthExpression(JArrayLengthExpression self,
					 JExpression prefix) {
    prefix.accept(this);
    print(".length");
  }

  /**
   * prints an array length expression
   */
  public void visitArrayAccessExpression(JArrayAccessExpression self,
					 JExpression prefix,
					 JExpression accessor) {
    prefix.accept(this);
    print("[");
    accessor.accept(this);
    print("]");
  }

  /**
   * prints an array length expression
   */
  public void visitComments(JavaStyleComment[] comments) {
    for (int i = 0; i < comments.length; i++) {
      if (comments[i] != null) {
	visitComment(comments[i]);
      }
    }
  }

  /**
   * prints an array length expression
   */
  public void visitComment(JavaStyleComment comment) {
    StringTokenizer	tok = new StringTokenizer(comment.getText(), "\n");

    if (comment.hadSpaceBefore()) {
      newLine();
    }

    if (comment.isLineComment()) {
      print("// " + tok.nextToken().trim());
      p.println();
    } else {
      if (p.getLine() > 0) {
	if (!nl) {
	  newLine();
	}
	newLine();
      }
      print("/*");
      while (tok.hasMoreTokens()){
	String comm = tok.nextToken().trim();
	if (comm.startsWith("*")) {
	  comm = comm.substring(1).trim();
	}
	if (tok.hasMoreTokens() || comm.length() > 0) {
	  newLine();
	  print(" * " + comm);
	}
      }
      newLine();
      print(" */");
      newLine();
    }

    if (comment.hadSpaceAfter()) {
      newLine();
    }
  }

  /**
   * prints an array length expression
   */
  public void visitJavadoc(JavadocComment comment) {
    StringTokenizer	tok = new StringTokenizer(comment.getText(), "\n");
    boolean		isFirst = true;

    if (!nl) {
      newLine();
    }
    newLine();
    print("/**");
    while (tok.hasMoreTokens()) {
      String	text = tok.nextToken().trim();
      String	type = null;
      boolean	param = false;
      int	idx = text.indexOf("@param");
      if (idx >= 0) {
	type = "@param";
	param = true;
      }
      if (idx < 0) {
	idx = text.indexOf("@exception");
	if (idx >= 0) {
	  type = "@exception";
	  param = true;
	}
      }
      if (idx < 0) {
	idx = text.indexOf("@exception");
	if (idx >= 0) {
	  type = "@exception";
	  param = true;
	}
      }
      if (idx < 0) {
	idx = text.indexOf("@author");
	if (idx >= 0) {
	  type = "@author";
	}
      }
      if (idx < 0) {
	idx = text.indexOf("@see");
	if (idx >= 0) {
	  type = "@see";
	}
      }
      if (idx < 0) {
	idx = text.indexOf("@version");
	if (idx >= 0) {
	  type = "@version";
	}
      }
      if (idx < 0) {
	idx = text.indexOf("@return");
	if (idx >= 0) {
	  type = "@return";
	}
      }
      if (idx < 0) {
	idx = text.indexOf("@deprecated");
	if (idx >= 0) {
	  type = "@deprecated";
	}
      }
      if (idx >= 0) {
	newLine();
	isFirst = false;
	if (param) {
	  text = text.substring(idx + type.length()).trim();
	  idx = Math.min(text.indexOf(" ") == -1 ? Integer.MAX_VALUE : text.indexOf(" "),
			 text.indexOf("\t") == -1 ? Integer.MAX_VALUE : text.indexOf("\t"));
	  if (idx == Integer.MAX_VALUE) {
	    idx = 0;
	  }
	  String	before = text.substring(0, idx);
	  print(" * " + type);
	  pos += 12;
	  print(before);
	  pos += 20;
	  print(text.substring(idx).trim());
	  pos -= 20;
	  pos -= 12;
	} else {
	  text = text.substring(idx + type.length()).trim();
	  print(" * " + type);
	  pos += 12;
	  print(text);
	  pos -= 12;
	}
      } else {
	text = text.substring(text.indexOf("*") + 1);
	if (tok.hasMoreTokens() || text.length() > 0) {
	  newLine();
	  print(" * ");
	  pos += isFirst ? 0 : 32;
	  print(text.trim());
	  pos -= isFirst ? 0 : 32;
	}
      }
    }
    newLine();
    print(" */");
  }

  // ----------------------------------------------------------------------
  // UTILS
  // ----------------------------------------------------------------------

  /**
   * prints an array length expression
   */
  public void visitSwitchLabel(JSwitchLabel self,
			       JExpression expr) {
    newLine();
    if (expr != null) {
      print("case ");
      expr.accept(this);
      print(": ");
    } else {
      print("default: ");
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
    pos += TAB_SIZE;
    for (int i = 0; i < stmts.length; i++) {
      newLine();
      stmts[i].accept(this);
    }
    pos -= TAB_SIZE;
  }

  /**
   * prints an array length expression
   */
  public void visitCatchClause(JCatchClause self,
			       JFormalParameter exception,
			       JBlock body) {
    print(" catch (");
    exception.accept(this);
    print(") ");
    body.accept(this);
  }

  /**
   * prints a boolean literal
   */
  public void visitBooleanLiteral(boolean value) {
    print(value);
  }

  /**
   * prints a byte literal
   */
  public void visitByteLiteral(byte value) {
    print("(byte)" + value);
  }

  /**
   * prints a character literal
   */
  public void visitCharLiteral(char value) {
    switch (value) {
    case '\b':
      print("'\\b'");
      break;
    case '\r':
      print("'\\r'");
      break;
    case '\t':
      print("'\\t'");
      break;
    case '\n':
      print("'\\n'");
      break;
    case '\f':
      print("'\\f'");
      break;
    case '\\':
      print("'\\\\'");
      break;
    case '\'':
      print("'\\''");
      break;
    case '\"':
      print("'\\\"'");
      break;
    default:
      print("'" + value + "'");
    }
  }

  /**
   * prints a double literal
   */
  public void visitDoubleLiteral(double value) {
    print("(double)" + value);
  }

  /**
   * prints a float literal
   */
  public void visitFloatLiteral(float value) {
    print(value + "F");
  }

  /**
   * prints a int literal
   */
  public void visitIntLiteral(int value) {
    print(value);
  }

  /**
   * prints a long literal
   */
  public void visitLongLiteral(long value) {
    print(value + "L");
  }

  /**
   * prints a short literal
   */
  public void visitShortLiteral(short value) {
    print("(short)" + value);
  }

  /**
   * prints a string literal
   */
  public void visitStringLiteral(String value) {
    print('"' + value + '"');
  }

  /**
   * prints a null literal
   */
  public void visitNullLiteral() {
    print("null");
  }

  /**
   * prints an array length expression
   */
  public void visitPackageName(String name) {
    print("package " + name + ";");
    newLine();
  }

  /**
   * prints an array length expression
   */
  public void visitPackageImport(String name) {
    print("import " + name.replace('/', '.') + ".*;");
  }

  /**
   * prints an array length expression
   */
  public void visitClassImport(String name) {
    print("import " + name.replace('/', '.') + ";");
  }

  /**
   * prints an array length expression
   */
  public void visitFormalParameters(JFormalParameter self,
				    boolean isFinal,
				    CType type,
				    String ident) {
    if (isFinal) {
      print("final ");
    }
    print(type.toString());
    if (ident.indexOf("$") == -1) {
      print(" ");
      print(ident);
    }
  }

  /**
   * prints an array length expression
   */
  public void visitArgs(JExpression[] args) {
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
	if (i != 0) {
	  print(", ");
	}
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
    newLine();
    print(functorIsThis ? "this" : "super");
    print("(");
    visitArgs(params);
    print(");");
  }

  /**
   * prints an array initializer expression
   */
  public void visitArrayInitializer(JArrayInitializer self,
				    JExpression[] elems)
  {
    newLine();
    print("{");
    for (int i = 0; i < elems.length; i++) {
      if (i != 0) {
	print(", ");
      }
      elems[i].accept(this);
    }
    print("}");
  }

  // ----------------------------------------------------------------------
  // PROTECTED METHODS
  // ----------------------------------------------------------------------

  protected void newLine() {
    p.println();
  }

  protected void print(Object s) {
    print(s.toString());
  }

  protected void print(String s) {
    p.setPos(pos);
    p.print(s);
  }

  protected void print(boolean s) {
    print("" + s);
  }

  protected void print(int s) {
    print("" + s);
  }

  protected void print(char s) {
    print("" + s);
  }

  protected void print(double s) {
    print("" + s);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected boolean			forInit;	// is on a for init
  protected int				TAB_SIZE = 2;
  protected int				WIDTH = 80;
  protected int				pos;

  protected TabbedPrintWriter		p;
  protected boolean			nl = true;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.KjcPrettyPrinter other = new at.dms.kjc.KjcPrettyPrinter();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.KjcPrettyPrinter other) {
  super.deepCloneInto(other);
  other.forInit = this.forInit;
  other.TAB_SIZE = this.TAB_SIZE;
  other.WIDTH = this.WIDTH;
  other.pos = this.pos;
  other.p = (at.dms.compiler.TabbedPrintWriter)at.dms.kjc.AutoCloner.cloneToplevel(this.p);
  other.nl = this.nl;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

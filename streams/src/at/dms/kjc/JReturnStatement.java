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
 * $Id: JReturnStatement.java,v 1.9 2003-11-13 10:46:11 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.16: Return Statement
 *
 * A return statement returns control to the invoker of a method or constructor.
 */
public class JReturnStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JReturnStatement() {} // for cloner only

 /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	expr		the expression to return.
   */
  public JReturnStatement(TokenReference where, JExpression expr, JavaStyleComment[] comments) {
    super(where, comments);
    this.expr = expr;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------


  /**
   * Returns the type of this return statement
   */
  public CType getType() {
    return expr != null ? expr.getType() : CStdType.Void;
  }


  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the statement (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CBodyContext context) throws PositionedError {
    CType	returnType = context.getMethodContext().getCMethod().getReturnType();

    if (expr != null) {
      check(context, returnType != CStdType.Void, KjcMessages.RETURN_NONEMPTY_VOID);

      CExpressionContext	expressionContext = new CExpressionContext(context);

      expr = expr.analyse(expressionContext);
      check(context,
	    expr.isAssignableTo(returnType),
	    KjcMessages.RETURN_BADTYPE, expr.getType(), returnType);
      expr = expr.convertType(returnType, expressionContext);
    } else {
      check(context, returnType == CStdType.Void, KjcMessages.RETURN_EMPTY_NONVOID);
    }

    context.setReachable(false);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    super.accept(p);
    p.visitReturnStatement(this, expr);
  }
 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return p.visitReturnStatement(this, expr);
  }
    
    /**
     * Sets expression of this.
     */
    public void setExpression(JExpression expr) {
	this.expr = expr;
    }


  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    if (expr != null) {
      expr.genCode(code, false);

      code.plantReturn(this);
      code.plantNoArgInstruction(expr.getType().getReturnOpcode());
    } else {
      code.plantReturn(this);
      code.plantNoArgInstruction(opc_return);
    }
  }

  /**
   * Load the value from a local var (after finally)
   */
  public void load(CodeSequence code, JLocalVariable var) {
    code.plantLocalVar(expr.getType().getLoadOpcode(), var);
  }

  /**
   * Load the value from a local var (after finally)
   */
  public void store(CodeSequence code, JLocalVariable var) {
    code.plantLocalVar(expr.getType().getStoreOpcode(), var);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected JExpression		expr;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JReturnStatement other = new at.dms.kjc.JReturnStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JReturnStatement other) {
  super.deepCloneInto(other);
  other.expr = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.expr);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

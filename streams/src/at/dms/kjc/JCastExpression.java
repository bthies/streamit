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
 * $Id: JCastExpression.java,v 1.7 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class represents a cast expression '((byte)2)'
 */
public class JCastExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JCastExpression() {} // for cloner only

  /**
   * Constructs a node in the parsing tree.
   * This method is directly called by the parser.
   * @param	where	the line of this node in the source code
   * @param	expr	the expression to be casted
   * @param	dest	the type of this expression after cast
   */
  public JCastExpression(TokenReference where, JExpression expr, CType dest) {
    super(where);
    this.expr = expr;
    this.dest = dest;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression.
   *
   * @return the type of this expression
   */
  public CType getType() {
    return dest;
  }

    public JExpression getExpr() {
	return expr;
    }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the expression (semantically).
   * @param	context		the analysis context
   * @return	an equivalent, analysed expression
   * @exception	PositionedError	the analysis detected an error
   */
  public JExpression analyse(CExpressionContext context) throws PositionedError {
    expr = expr.analyse(context);

    try {
      dest.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }

    check(context, expr.getType().isCastableTo(dest), KjcMessages.CAST_CANT, expr.getType(), dest);

    if (!expr.getType().isPrimitive() && expr.getType().isAssignableTo(dest) && expr.getType() != CStdType.Null) {
      context.reportTrouble(new CWarning(getTokenReference(), KjcMessages.UNNECESSARY_CAST, expr.getType(), dest));
    }

    if (expr.isConstant() /*&& expr.getType().isPrimitive() */) {
      return expr.convertType(dest, context);
    }

    if (!dest.isAssignableTo(expr.getType())) {
      return expr.convertType(dest, context);
    }

    return this;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitCastExpression(this, expr, dest);
  }

  public String toString() {
    return "JCastExpression: dest: " + dest + " expr: " + expr;
  }
 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitCastExpression(this, expr, dest);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    expr.genCode(code, false);

    if (dest.isNumeric()) {
      ((CNumericType)expr.getType()).genCastTo((CNumericType)dest, code);
    } else if (dest instanceof CClassType) {
      code.plantClassRefInstruction(opc_checkcast, ((CClassType)dest).getQualifiedName());
    }

    if (discardValue) {
      code.plantPopInstruction(dest);
    }
  }
    
    public void setExpr(JExpression e) {
	expr = e;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected JExpression		expr;
  protected CType		dest;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JCastExpression other = new at.dms.kjc.JCastExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JCastExpression other) {
  super.deepCloneInto(other);
  other.expr = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.expr);
  other.dest = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.dest);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JLogicalComplementExpression.java,v 1.4 2003-05-28 05:58:44 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * JLS 15.15.6 Logical Complement Operator !
 */
public class JLogicalComplementExpression extends JUnaryExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JLogicalComplementExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	expr		the operand
   */
  public JLogicalComplementExpression(TokenReference where, JExpression expr) {
    super(where, expr);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

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
    check(context, expr.getType() == CStdType.Boolean, KjcMessages.UNARY_BADTYPE_LNOT, expr.getType());
    type = CStdType.Boolean;
    if (expr.isConstant()) {
      return new JBooleanLiteral(getTokenReference(), !expr.booleanValue());
    } else {
      return this;
    }
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitLogicalComplementExpression(this, expr);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitLogicalComplementExpression(this, expr);
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
    code.plantInstruction(new PushLiteralInstruction(1));
    code.plantNoArgInstruction(opc_ixor);

    if (discardValue) {
      code.plantPopInstruction(type);
    }
  }

  /**
   * Generates a sequence of bytescodes to branch on a label
   * This method helps to handle heavy optimizables conditions
   * @param	code		the code list
   */
  public void genBranch(boolean cond, CodeSequence code, CodeLabel label) {
    expr.genBranch(!cond, code, label);
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JLogicalComplementExpression other = new at.dms.kjc.JLogicalComplementExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JLogicalComplementExpression other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JConditionalAndExpression.java,v 1.4 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class implements the conditional and operation
 */
public class JConditionalAndExpression extends JBinaryExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JConditionalAndExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	left		the left operand
   * @param	right		the right operand
   */
  public JConditionalAndExpression(TokenReference where,
				   JExpression left,
				   JExpression right)
  {
    super(where, left, right);
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
    left = left.analyse(context);
    right = right.analyse(context);

    CType	leftType = left.getType();
    CType	rightType = right.getType();

    check(context,
	  leftType == CStdType.Boolean && rightType == CStdType.Boolean,
	  KjcMessages.AND_BADTYPE, leftType, rightType);

    type = CStdType.Boolean;

    // JLS 15.28: Constant Expression ?
    if (left.isConstant() && right.isConstant()) {
      return new JBooleanLiteral(getTokenReference(), left.booleanValue() && right.booleanValue());
    } else {
      return this;
    }
  }

  public JExpression constantFolding() {
    if (left instanceof JBooleanLiteral && right instanceof JBooleanLiteral)
      return new JBooleanLiteral(null, 
				 ((JBooleanLiteral) left).booleanValue() && 
				 ((JBooleanLiteral) right).booleanValue());
    else 
      return super.constantFolding();
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitBinaryExpression(this, "&&", left, right);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitBinaryExpression(this, "&&", left, right);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    genBooleanResultCode(code, discardValue);
  }

  /**
   * Optimize a bi-conditional expression
   */
  protected void genBranch(JExpression left,
			   JExpression right,
			   boolean cond,
			   CodeSequence code,
			   CodeLabel label)
  {
    if (cond) {
      CodeLabel		skip = new CodeLabel();
      left.genBranch(false, code, skip);
      right.genBranch(true, code, label);
      code.plantLabel(skip);
    } else {
      left.genBranch(false, code, label);
      right.genBranch(false, code, label);
    }
  }
}

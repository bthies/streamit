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
 * $Id: JBinaryArithmeticExpression.java,v 1.1 2001-08-30 16:32:51 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

/**
 * This class is an abstract root class for binary expressions
 */
public abstract class JBinaryArithmeticExpression extends JBinaryExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	p1		left operand
   * @param	p2		right operand
   */
  public JBinaryArithmeticExpression(TokenReference where,
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
  public abstract JExpression analyse(CExpressionContext context) throws PositionedError;

  /**
   * compute the type of this expression according to operands
   * @param	operator	the binary arithmetic operator
   * @param	left		the type of left operand
   * @param	right		the type of right operand
   * @return	the type computed for this binary operation
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public static CType computeType(String operator,
				  CType	left,
				  CType right)
    throws UnpositionedError
  {
    if (!left.isNumeric() || !right.isNumeric()) {
      throw new UnpositionedError(KjcMessages.BINARY_NUMERIC_BAD_TYPES,
				  new Object[]{ operator, left, right });
    }
    return CNumericType.binaryPromote(left, right);
  }

  // ----------------------------------------------------------------------
  // CONSTANT FOLDING
  // ----------------------------------------------------------------------

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the left value
   * @param	right		the right value
   * @return	the literal holding the result of the operation
   */
  public JExpression constantFolding() {
    switch (left.getType().getTypeID()) {
    case TID_INT:
      return new JIntLiteral(getTokenReference(), compute(left.intValue(), right.intValue()));
    case TID_LONG:
      return new JLongLiteral(getTokenReference(), compute(left.longValue(), right.longValue()));
    case TID_FLOAT:
      return new JFloatLiteral(getTokenReference(), compute(left.floatValue(), right.floatValue()));
    case TID_DOUBLE:
      return new JDoubleLiteral(getTokenReference(), compute(left.doubleValue(), right.doubleValue()));
    default:
      throw new InconsistencyException("unexpected type " + left.getType());
    }
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public int compute(int left, int right) {
    throw new InconsistencyException("not available");
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public long compute(long left, long right) {
    throw new InconsistencyException("not available");
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public float compute(float left, float right) {
    throw new InconsistencyException("not available");
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public double compute(double left, double right) {
    throw new InconsistencyException("not available");
  }
}

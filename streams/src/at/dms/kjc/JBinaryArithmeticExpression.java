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
 * $Id: JBinaryArithmeticExpression.java,v 1.4 2003-05-16 21:58:35 thies Exp $
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

    protected JBinaryArithmeticExpression() {} // for cloner only

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
      CType myType = getType();
      try {
	  if(myType==null)
	      myType=computeType("unknown",left.getType(),right.getType());
      } catch(UnpositionedError e) {
	  System.err.println("UnpositionedError!");
      }
      if(myType!=null) {
	  switch (myType.getTypeID()) {
	  case TID_INT: {
	      int val1 = ((JIntLiteral)left.convertType(myType)).intValue();
	      int val2 = ((JIntLiteral)right.convertType(myType)).intValue();
	      return new JIntLiteral(getTokenReference(), compute(val1, val2));
	  }
	  case TID_LONG: {
	      long val1 = ((JLongLiteral)left.convertType(myType)).longValue();
	      long val2 = ((JLongLiteral)right.convertType(myType)).longValue();
	      return new JLongLiteral(getTokenReference(), compute(val1, val2));
	  }
	  case TID_FLOAT: {
	      float val1 = ((JFloatLiteral)left.convertType(myType)).floatValue();
	      float val2 = ((JFloatLiteral)right.convertType(myType)).floatValue();
	      return new JFloatLiteral(getTokenReference(), compute(val1, val2));
	  }
	  case TID_DOUBLE: {
	      double val1 = ((JDoubleLiteral)left.convertType(myType)).doubleValue(); 
	      double val2 = ((JDoubleLiteral)right.convertType(myType)).doubleValue();
	      return new JDoubleLiteral(getTokenReference(), compute(val1, val2));
	  }
	  default:
	      throw new InconsistencyException("unexpected type " + myType);
	  }
      } else
	  throw new InconsistencyException("unexpected type " + myType);
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

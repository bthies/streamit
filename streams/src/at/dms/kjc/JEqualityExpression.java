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
 * $Id: JEqualityExpression.java,v 1.4 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * JLS 15.21: Equality Operators ('==' and '!=')
 */
public class JEqualityExpression extends JBinaryExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JEqualityExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	equal		is the operator '==' ?
   * @param	left		the left operand
   * @param	right		the right operand
   */
  public JEqualityExpression(TokenReference where,
			     boolean equal,
			     JExpression left,
			     JExpression right)
  {
    super(where, left, right);
    this.equal = equal;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns a string representation of this literal.
   */
  public String toString() {
    StringBuffer	buffer = new StringBuffer();

    buffer.append("JEqualityExpression[");
    buffer.append(left.toString());
    buffer.append(equal ? " == " : " != ");
    buffer.append(right.toString());
    buffer.append("]");
    return buffer.toString();
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

    if (leftType.isNumeric()) {
      // JLS 15.21.1: Numerical Equality Operators
      check(context, rightType.isNumeric(), KjcMessages.EQUALITY_TYPE, leftType, rightType);

      CType	promoted = CNumericType.binaryPromote(leftType, rightType);

      left = left.convertType(promoted, context);
      right = right.convertType(promoted, context);
    } else if (leftType == CStdType.Boolean) {
      // JLS 15.21.2: Boolean Equality Operators
      check(context, rightType == CStdType.Boolean, KjcMessages.EQUALITY_TYPE, leftType, rightType);
      if (left instanceof JBooleanLiteral || right instanceof JBooleanLiteral) {
	context.reportTrouble(new CWarning(getTokenReference(), KjcMessages.COMPARING_BOOLEAN_CONSTANT));
      }
    } else {
      // JLS 15.21.3: Reference Equality Operators
      check(context,
	    leftType.isReference() && rightType.isReference(),
	    KjcMessages.EQUALITY_TYPE, leftType, rightType);
      check(context,
	    rightType.isCastableTo(leftType) || leftType.isCastableTo(rightType),
	    KjcMessages.EQUALITY_TYPE, leftType, rightType);
      if (left.getType().equals(CStdType.String) && right.getType().equals(CStdType.String) &&
	  (left.isConstant() || right.isConstant())) {
	context.reportTrouble(new CWarning(getTokenReference(), KjcMessages.STRING_COMPARISON));
      }
    }

    type = CStdType.Boolean;

    if (left.isConstant() && right.isConstant()) {
      return constantFolding();
    } else {
      return this;
    }
  }

  /**
   * @return	a literal resulting of an operation over two literals
   */
  public JExpression constantFolding() {
    boolean	result;

    switch (left.getType().getTypeID()) {
    case TID_INT:
      result = left.intValue() == right.intValue();
      break;
    case TID_LONG:
      result = left.longValue() == right.longValue();
      break;
    case TID_FLOAT:
      result = left.floatValue() == right.floatValue();
      break;
    case TID_DOUBLE:
      result = left.doubleValue() == right.doubleValue();
      break;
    case TID_BOOLEAN:
      result = left.booleanValue() == right.booleanValue();
      break;
    case TID_CLASS:
      if (left.getType() != CStdType.String) {
	throw new InconsistencyException("unexpected type " + left.getType());
      }
      result = left.stringValue().equals(right.stringValue());
      break;
    default:
      throw new InconsistencyException("unexpected type " + left.getType());
    }

    return new JBooleanLiteral(getTokenReference(), equal ? result : !result);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitEqualityExpression(this, equal, left, right);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitEqualityExpression(this, equal, left, right);
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
    setLineNumber(code);

    if (left.getType() == CStdType.Null) {
      // use specific instruction to compare to null
      right.genCode(code, false);
      code.plantJumpInstruction(cond == equal ? opc_ifnull : opc_ifnonnull, label);
    } else if (right.getType() == CStdType.Null) {
      // use specific instruction to compare to null
      left.genCode(code, false);
      code.plantJumpInstruction(cond == equal ? opc_ifnull : opc_ifnonnull, label);
    } else if (left.isConstant()
	       && (left.getType() == CStdType.Integer || left.getType() == CStdType.Boolean)
	       && ((JLiteral)left).isDefault()) {
      // use specific instruction to compare to 0
      right.genCode(code, false);
      code.plantJumpInstruction(cond == equal ? opc_ifeq : opc_ifne, label);
    } else if (right.isConstant()
	       && (right.getType() == CStdType.Integer || right.getType() == CStdType.Boolean)
	       && ((JLiteral)right).isDefault()) {
      // use specific instruction to compare to 0
      left.genCode(code, false);
      code.plantJumpInstruction(cond == equal ? opc_ifeq : opc_ifne, label);
    } else {
      left.genCode(code, false);
      right.genCode(code, false);

      switch (left.getType().getTypeID()) {
      case TID_ARRAY:
      case TID_CLASS:
	code.plantJumpInstruction(cond  == equal ? opc_if_acmpeq : opc_if_acmpne, label);
	break;
      case TID_FLOAT:
	code.plantNoArgInstruction(opc_fcmpl);
	code.plantJumpInstruction(cond == equal ? opc_ifeq : opc_ifne, label);
	break;
      case TID_LONG:
	code.plantNoArgInstruction(opc_lcmp);
	code.plantJumpInstruction(cond == equal ? opc_ifeq : opc_ifne, label);
	break;
      case TID_DOUBLE:
	code.plantNoArgInstruction(opc_dcmpl);
	code.plantJumpInstruction(cond == equal ? opc_ifeq : opc_ifne, label);
	break;
      default:
	code.plantJumpInstruction(cond == equal ? opc_if_icmpeq : opc_if_icmpne, label);
      }
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    protected /* final */ boolean		equal; // removed final for cloner
}

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
 * $Id: JConditionalExpression.java,v 1.9 2003-08-29 19:25:36 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * JLS 15.25 Conditional Operator ? :
 */
public class JConditionalExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JConditionalExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	cond		the condition operand
   * @param	left		the left operand
   * @param	right		the right operand
   */
  public JConditionalExpression(TokenReference where,
				JExpression cond,
				JExpression left,
				JExpression right)
  {
    super(where);
    this.cond = cond;
    this.left = left;
    this.right = right;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
    return type;
  }

    public void setLeft(JExpression left) {
	this.left = left;
    }

    public void setRight(JExpression right) {
	this.right = right;
    }

  /**
   * Returns a string representation of this literal.
   */
  public String toString() {
    StringBuffer	buffer = new StringBuffer();

    buffer.append("JConditionalExpression[");
    buffer.append(cond.toString());
    buffer.append(", ");
    buffer.append(left.toString());
    buffer.append(", ");
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
    cond = cond.analyse(context);
    left = left.analyse(context);
    right = right.analyse(context);

    check(context, cond.getType() == CStdType.Boolean, KjcMessages.TRINARY_BADCOND);

    CType leftType = left.getType();
    CType rightType = right.getType();

    // JLS 15.25 :
    // The type of a conditional expression is determined as follows:
    if (leftType.equals(rightType)) {
      // - If the second and third operands have the same type (which may
      //   be the null type), then that is the type of the conditional
      //   expression.
      type = leftType;
    } else if (leftType.isNumeric() && rightType.isNumeric()) {
      // - Otherwise, if the second and third operands have numeric type,
      //   then there are several cases:
      //   * If one of the operands is of type byte and the other is of
      //     type short, then the type of the conditional expression is short.
      //   * If one of the operands is of type T where T is byte, short,
      //     or char, and the other operand is a constant expression of type
      //     int whose value is representable in type T, then the type of
      //     the conditional expression is T.
      //   * Otherwise, binary numeric promotion is applied to the operand
      //     types, and the type of the conditional expression is the promoted
      //     type of the second and third operands. Note that binary numeric
      //     promotion performs value set conversion.
      if ((leftType == CStdType.Byte && rightType == CStdType.Short)
	  || (rightType == CStdType.Byte && leftType == CStdType.Short)) {
	type = CStdType.Short;
      } else if ((leftType == CStdType.Byte
		  || leftType == CStdType.Short
		  || leftType == CStdType.Char)
		 && rightType == CStdType.Integer
		 && right.isConstant()
		 && right.isAssignableTo(leftType)) {
	type = leftType;
      } else if ((rightType == CStdType.Byte
		  || rightType == CStdType.Short
		  || rightType == CStdType.Char)
		 && leftType == CStdType.Integer
		 && left.isConstant()
		 && left.isAssignableTo(rightType)) {
	type = rightType;
      } else {
	type = CNumericType.binaryPromote(leftType, rightType);
	check(context,
	      type != null,
	      KjcMessages.TRINARY_INCOMP, leftType, rightType);
      }
      left = left.convertType(type, context);
      right = right.convertType(type, context);
    } else if (leftType.isReference() && rightType.isReference()) {
      // - If one of the second and third operands is of the null type and the
      //   type of the other is a reference type, then the type of the
      //   conditional expression is that reference type.
      // - If the second and third operands are of different reference types,
      //   then it must be possible to convert one of the types to the other type
      //   (call this latter type T) by assignment conversion ; the type of the
      //   conditional expression is T.
      //   It is a compile-time error if neither type is assignment
      //   compatible with the other type.
      if (leftType == CStdType.Null) {
	type = rightType;
      } else if (rightType == CStdType.Null) {
	type = leftType;
      } else if (leftType.isAssignableTo(rightType)) {
	type = rightType;
      } else if (rightType.isAssignableTo(leftType)) {
	type = leftType;
      } else {
	check(context, false, KjcMessages.TRINARY_INCOMP, leftType, rightType);
      }
    } else {
      check(context, false, KjcMessages.TRINARY_INCOMP, leftType, rightType);
    }

    // JLS 15.28: Constant Expression ?
    if (cond.isConstant() && left.isConstant() && right.isConstant()) {
      return cond.booleanValue() ? left : right;
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
    p.visitConditionalExpression(this, cond, left, right);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitConditionalExpression(this, cond, left, right);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    CodeLabel		rightLabel = new CodeLabel();
    CodeLabel		nextLabel = new CodeLabel();

    cond.genBranch(false, code, rightLabel);		//		COND IFEQ right
    left.genCode(code, discardValue);			//		LEFT CODE
    code.plantJumpInstruction(opc_goto, nextLabel);	//		GOTO next
    code.plantLabel(rightLabel);			//	right:
    right.genCode(code, discardValue);			//		RIGHT CODE
    code.plantLabel(nextLabel);				//	next:	...
  }

    public void setCond(JExpression c) {
	cond = c;
    }

    public void setCondition(JExpression condition) {
	this.cond = condition;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CType		type;
  private JExpression   cond;
  private JExpression   left;
  private JExpression   right;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JConditionalExpression other = new at.dms.kjc.JConditionalExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JConditionalExpression other) {
  super.deepCloneInto(other);
  other.type = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.type, other);
  other.cond = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.cond, other);
  other.left = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.left, other);
  other.right = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.right, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

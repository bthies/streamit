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
 * $Id: JCompoundAssignmentExpression.java,v 1.5 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

/**
 * JLS 15.26.2 : Compound Assignment Operator.
 */
public class JCompoundAssignmentExpression extends JAssignmentExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JCompoundAssignmentExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	oper		the assignment operator
   * @param	left		the left operand
   * @param	right		the right operand
   */
  public JCompoundAssignmentExpression(TokenReference where,
				       int oper,
				       JExpression left,
				       JExpression right)
  {
    super(where, left, right);
    this.oper = oper;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns true iff this expression can be used as a statement (JLS 14.8)
   */
  public boolean isStatementExpression() {
    return true;
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
    // discardValue = false: check if initialized
    left = left.analyse(new CExpressionContext(context, true, false));
    check(context, left.isLValue(context), KjcMessages.ASSIGNMENT_NOTLVALUE);

    // try to assign: check lhs is not final
    try {
      left.setInitialized(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }

    right = right.analyse(new CExpressionContext(context, false, false));
    if (right instanceof JTypeNameExpression) {
      check(context, false, KjcMessages.VAR_UNKNOWN, ((JTypeNameExpression)right).getQualifiedName());
    }

    // JLS 15.26.2 Compound Assignment Operators :
    // All compound assignment operators require both operands to be of
    // primitive type, except for +=, which allows the right-hand operand to be
    // of any type if the left-hand operand is of type String.

    boolean convertRight = true;

    try {
      switch (oper) {
      case OPE_STAR:
	type = JMultExpression.computeType(left.getType(), right.getType());
	break;
      case OPE_SLASH:
	type = JDivideExpression.computeType(left.getType(), right.getType());
	break;
      case OPE_PERCENT:
	type = JModuloExpression.computeType(left.getType(), right.getType());
	break;
      case OPE_PLUS:
	if (left.getType().equals(CStdType.String)) {
	  if (right.getType() == CStdType.Void) {
	    throw new UnpositionedError(KjcMessages.ADD_BADTYPE,
					CStdType.String,
					CStdType.Void);
	  }
	  type = CStdType.String;
	  convertRight = false;
	} else {
	  type = JBinaryArithmeticExpression.computeType("+",
							 left.getType(),
							 right.getType());
	}
	break;
      case OPE_MINUS:
	type = JMinusExpression.computeType(left.getType(), right.getType());
	break;
      case OPE_SL:
      case OPE_SR:
      case OPE_BSR:
	type = JShiftExpression.computeType(left.getType(), right.getType());
	convertRight = false;
	break;
      case OPE_BAND:
      case OPE_BXOR:
      case OPE_BOR:
	type = JBitwiseExpression.computeType(left.getType(), right.getType());
	break;
      default:
	throw new InconsistencyException("unexpected operator " + oper);
      }
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
    check(context,
	  type.isCastableTo(left.getType()),
	  KjcMessages.ASSIGNMENT_BADTYPE, right.getType(), left.getType());
    if (convertRight && !right.isAssignableTo(left.getType())) {
      context.reportTrouble(new CWarning(getTokenReference(),
					 KjcMessages.NARROWING_COMPOUND_ASSIGNMENT,
					 right.getType(), left.getType()));
    }

    type = left.getType();

    if (convertRight) {
      right = right.convertType(type, context);
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
    p.visitCompoundAssignmentExpression(this, oper, left, right);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitCompoundAssignmentExpression(this, oper, left, right);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    left.genStartStoreCode(code);

    if (oper == OPE_PLUS && type.equals(CStdType.String)) {
      left.genCode(code, false);
      right.genCode(code, false);
      if (!right.getType().isReference()) {
	code.plantMethodRefInstruction(opc_invokestatic,
				       "java/lang/String",
				       "valueOf",
				       "(" + right.getType().getSignature() + ")Ljava/lang/String;");
      } else if (!right.getType().equals(CStdType.String)) {
	code.plantMethodRefInstruction(opc_invokestatic,
				       "java/lang/String",
				       "valueOf",
				       "(Ljava/lang/Object;)Ljava/lang/String;");
      }

      code.plantMethodRefInstruction(opc_invokevirtual,
				     "java/lang/String",
				     "concat",
				     "(Ljava/lang/String;)Ljava/lang/String;");
    } else {
      left.genCode(code, false);
      right.genCode(code, false);

      int	opcode = -1;

      switch (oper) {
      case OPE_STAR:
	opcode = JMultExpression.getOpcode(getType());
	break;
      case OPE_SLASH:
	opcode = JDivideExpression.getOpcode(getType());
	break;
      case OPE_PERCENT:
	opcode = JModuloExpression.getOpcode(getType());
	break;
      case OPE_PLUS:
	opcode = JAddExpression.getOpcode(getType());
	break;
      case OPE_MINUS:
	opcode = JMinusExpression.getOpcode(getType());
	break;
      case OPE_SL:
      case OPE_SR:
      case OPE_BSR:
	opcode = JShiftExpression.getOpcode(oper, getType());
	break;
      case OPE_BAND:
      case OPE_BXOR:
      case OPE_BOR:
	opcode = JBitwiseExpression.getOpcode(oper, getType());
	break;
      }

      code.plantNoArgInstruction(opcode);
    }

    left.genEndStoreCode(code, discardValue);
  }
    
  /**
   * Returns a string representation of this object.
   */
  public String toString() {
    StringBuffer	buffer = new StringBuffer();

    buffer.append("JCompoundAssignmentExpression[");
    buffer.append(oper);
    buffer.append(", ");
    buffer.append(left.toString());
    buffer.append(", ");
    buffer.append(right.toString());
    buffer.append("]");
    return buffer.toString();
  }

    /**
     * Get operation of this.
     */
    public int getOperation() {
	return oper;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected  int		oper;
}

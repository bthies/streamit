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
 * $Id: JRelationalExpression.java,v 1.8 2003-05-28 05:58:44 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * This class implements '< > <= >=' specific operations
 * Plus operand may be String, numbers
 */
public class JRelationalExpression extends JBinaryExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JRelationalExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	oper		the operator
   * @param	left		the left operand
   * @param	right		the right operand
   */
  public JRelationalExpression(TokenReference where,
			       int oper,
			       JExpression left,
			       JExpression right)
  {
    super(where, left, right);
    this.oper = oper;
  }

    public int getOper() {
	return oper;
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

    check(context,
	  left.getType().isNumeric() && right.getType().isNumeric(),
	  KjcMessages.RELATIONAL_TYPE, left.getType(), right.getType());

    type = CStdType.Boolean;

    CType	promoted = CNumericType.binaryPromote(left.getType(), right.getType());
    left = left.convertType(promoted, context);
    right = right.convertType(promoted, context);

    if (left.isConstant() && right.isConstant()) {
      return constantFolding();
    }

    return this;
  }

  // ----------------------------------------------------------------------
  // CONSTANT FOLDING
  // ----------------------------------------------------------------------

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the left value
   * @param	right		the right value
   * @return	a literal resulting of an operation over two literals
   */
  public JExpression constantFolding() {
    boolean	result;

    switch (left.getType().getTypeID()) {
    case TID_INT:
	result = compute(left.intValue(), right.intValue());
	break;
    case TID_LONG:
	result = compute(left.longValue(), right.longValue());
	break;
    case TID_FLOAT:
	result = compute(left.floatValue(), right.floatValue());
	break;
    case TID_DOUBLE:
	result = compute(left.doubleValue(), right.doubleValue());
	break;
	//case OPE_BAND:
	//result = left.booleanValue()&&right.booleanValue();
    default:
      throw new InconsistencyException("unexpected type " + left.getType());
    }

    return new JBooleanLiteral(getTokenReference(), result);
  }
    
    /**
     * Tries to fold even if both left an right aren't constant
     
     public JExpression partialFold() {
     return this;
     }*/
    
    /**
     * Computes the result of the operation at compile-time (JLS 15.28).
     * @param	left		the first operand
     * @param	right		the seconds operand
     * @return	the result of the operation
     */
    public boolean compute(int left, int right) {
	switch (oper) {
	case OPE_LT:
	    return left < right;
	case OPE_LE:
	    return left <= right;
	case OPE_GT:
	    return left > right;
	case OPE_GE:
	    return left >= right;
	case OPE_EQ:
	    return left == right;
	case OPE_NE:
	    return left != right;
	default:
	    throw new InconsistencyException();
	}
    }
    
    /**
     * Computes the result of the operation at compile-time (JLS 15.28).
     * @param	left		the first operand
     * @param	right		the seconds operand
     * @return	the result of the operation
     */
    public boolean compute(long left, long right) {
	switch (oper) {
	case OPE_LT:
	    return left < right;
	case OPE_LE:
	    return left <= right;
	case OPE_GT:
	    return left > right;
	case OPE_GE:
	    return left >= right;
	case OPE_EQ:
	    return left == right;
	case OPE_NE:
	    return left != right;
	default:
	    throw new InconsistencyException();
	}
    }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public boolean compute(float left, float right) {
    switch (oper) {
    case OPE_LT:
      return left < right;
    case OPE_LE:
      return left <= right;
    case OPE_GT:
      return left > right;
    case OPE_GE:
      return left >= right;
    case OPE_EQ:
	return left == right;
    case OPE_NE:
	return left != right;
    default:
      throw new InconsistencyException();
    }
  }

  /**
   * Computes the result of the operation at compile-time (JLS 15.28).
   * @param	left		the first operand
   * @param	right		the seconds operand
   * @return	the result of the operation
   */
  public boolean compute(double left, double right) {
      switch (oper) {
      case OPE_LT:
	  return left < right;
      case OPE_LE:
	  return left <= right;
      case OPE_GT:
	  return left > right;
      case OPE_GE:
	  return left >= right;
      case OPE_EQ:
	  return left == right;
      case OPE_NE:
	  return left != right;
      default:
	  throw new InconsistencyException();
      }
  }

    /**
     * Changes to the complement of this statement
     */
    public JRelationalExpression complement() {
	switch (oper) {
	case OPE_LT:
	    return new JRelationalExpression(getTokenReference(),OPE_GE,left,right);
	case OPE_LE:
	    return new JRelationalExpression(getTokenReference(),OPE_GT,left,right);
	case OPE_GT:
	    return new JRelationalExpression(getTokenReference(),OPE_LE,left,right);
	case OPE_GE:
	    return new JRelationalExpression(getTokenReference(),OPE_LT,left,right);
	case OPE_EQ:
	    return new JRelationalExpression(getTokenReference(),OPE_NE,left,right);
	case OPE_NE:
	    return new JRelationalExpression(getTokenReference(),OPE_EQ,left,right);
	default:
	    return null;
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
    p.visitRelationalExpression(this, oper, left, right);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitRelationalExpression(this, oper, left, right);
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

    if (left.getType() == CStdType.Integer && left.isConstant()	&& ((JLiteral)left).isDefault()) {
      int	opcode;

      right.genCode(code, false);
      switch (oper) {
      case OPE_LT:
	opcode = cond ? opc_ifgt : opc_ifle;
	break;
      case OPE_LE:
	opcode = cond ? opc_ifge : opc_iflt;
	break;
      case OPE_GT:
	opcode = cond ? opc_iflt : opc_ifge;
	break;
      case OPE_GE:
	opcode = cond ? opc_ifle : opc_ifgt;
	break;
      default:
	throw new InconsistencyException("bad operator " + oper);
      }
      code.plantJumpInstruction(opcode, label);
    } else if (left.getType() == CStdType.Integer && right.isConstant() && ((JLiteral)right).isDefault()) {
      int	opcode;

      left.genCode(code, false);
      switch (oper) {
      case OPE_LT:
	opcode = cond ? opc_iflt : opc_ifge;
	break;
      case OPE_LE:
	opcode = cond ? opc_ifle : opc_ifgt;
	break;
      case OPE_GT:
	opcode = cond ? opc_ifgt : opc_ifle;
	break;
      case OPE_GE:
	opcode = cond ? opc_ifge : opc_iflt;
	break;
      default:
	throw new InconsistencyException("bad operator " + oper);
      }
      code.plantJumpInstruction(opcode, label);
    } else {
      left.genCode(code, false);
      right.genCode(code, false);

      if (left.getType() == CStdType.Integer) {
	int		opcode;

	switch (oper) {
	case OPE_LT:
	  opcode = cond ? opc_if_icmplt : opc_if_icmpge;
	  break;
	case OPE_LE:
	  opcode = cond ? opc_if_icmple : opc_if_icmpgt;
	  break;
	case OPE_GT:
	  opcode = cond ? opc_if_icmpgt : opc_if_icmple;
	  break;
	case OPE_GE:
	  opcode = cond ? opc_if_icmpge : opc_if_icmplt;
	  break;
	default:
	  throw new InconsistencyException("bad operator " + oper);
	}
	code.plantJumpInstruction(opcode, label);
      } else {
	int		opcode;

	if (left.getType() == CStdType.Long) {
	  opcode = opc_lcmp;
	} else if (left.getType() == CStdType.Float) {
	  opcode = opc_fcmpl;
	} else if (left.getType() == CStdType.Double) {
	  opcode = opc_dcmpl;
	} else {
	  throw new InconsistencyException("bad type " + left.getType());
	}
	code.plantNoArgInstruction(opcode);

	switch (oper) {
	case OPE_LT:
	  opcode = cond ? opc_iflt : opc_ifge;
	  break;
	case OPE_LE:
	  opcode = cond ? opc_ifle : opc_ifgt;
	  break;
	case OPE_GT:
	  opcode = cond ? opc_ifgt : opc_ifle;
	  break;
	case OPE_GE:
	  opcode = cond ? opc_ifge : opc_iflt;
	  break;
	default:
	  throw new InconsistencyException("bad operator " + oper);
	}
	code.plantJumpInstruction(opcode, label);
      }
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    protected /* final */  int		oper; // removed final for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JRelationalExpression other = new at.dms.kjc.JRelationalExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JRelationalExpression other) {
  super.deepCloneInto(other);
  other.oper = this.oper;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

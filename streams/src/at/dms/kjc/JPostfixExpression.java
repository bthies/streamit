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
 * $Id: JPostfixExpression.java,v 1.9 2003-11-13 10:46:11 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.IincInstruction;
import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class represents postfix increment and decrement expressions.
 * 15.13.2 Postfix Increment Operator ++
 * 15.13.3 Postfix Decrement Operator --
 */
public class JPostfixExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JPostfixExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	oper		the operator
   * @param	expr		the operand
   */
  public JPostfixExpression(TokenReference where, int oper, JExpression expr) {
    super(where);
    this.oper = oper;
    this.expr = expr;
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

  /**
   * Return the operation for this.
   */
  public int getOper() {
    return oper;
  }
    
  /**
   * Return the expression.
   */
  public JExpression getExpr() {
    return expr;
  }

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
    expr = expr.analyse(new CExpressionContext(context, true, context.discardValue()));
    check(context, expr.getType().isNumeric(), KjcMessages.POSTFIX_BADTYPE, expr.getType());
    check(context, expr.isLValue(context), KjcMessages.POSTFIX_NOTLVALUE);
    check(context, expr.isInitialized(context), KjcMessages.POSTFIX_NOTINITIALIZED);
    type = expr.getType();

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
    p.visitPostfixExpression(this, oper, expr);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitPostfixExpression(this, oper, expr);
  }


  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    int			val = oper == OPE_POSTINC ? 1 : -1;

    if ((expr.getType() == CStdType.Integer) &&
	(expr instanceof JLocalVariableExpression)) {
      expr.genCode(code, discardValue);
      code.plantInstruction(new IincInstruction(((JLocalVariableExpression)expr).getPosition(),
						val));
    } else {
      if (discardValue) {
	expr.genStartStoreCode(code);
	expr.genCode(code, false);
      } else {
	expr.genStartStoreCode(code);
	expr.genCode(code, discardValue);
	if (getType().getSize() == 2) {
	  if ((expr instanceof JLocalVariableExpression) ||
	      ((expr instanceof JFieldAccessExpression) &&
	       ((JFieldAccessExpression)expr).getField().isStatic())) {
	    code.plantNoArgInstruction(opc_dup2);
	  } else if (expr instanceof JArrayAccessExpression) {
	    code.plantNoArgInstruction(opc_dup2_x2);
	  } else {
	    code.plantNoArgInstruction(opc_dup2_x1);
	  }
	} else if (getType().getSize() == 1) {
	  if ((expr instanceof JLocalVariableExpression) ||
	      ((expr instanceof JFieldAccessExpression) &&
	       ((JFieldAccessExpression)expr).getField().isStatic())) {
	    code.plantNoArgInstruction(opc_dup);
	  } else if (expr instanceof JArrayAccessExpression) {
	    code.plantNoArgInstruction(opc_dup_x2);
	  } else {
	    code.plantNoArgInstruction(opc_dup_x1);
	  }
	}
      }

      switch (expr.getType().getTypeID()) {
      case TID_FLOAT:
	code.plantInstruction(new PushLiteralInstruction((float)val));
	code.plantNoArgInstruction(opc_fadd);
	break;
      case TID_LONG:
	code.plantInstruction(new PushLiteralInstruction((long)val));
	code.plantNoArgInstruction(opc_ladd);
	break;
      case TID_DOUBLE:
	code.plantInstruction(new PushLiteralInstruction((double)val));
	code.plantNoArgInstruction(opc_dadd);
	break;

      case TID_BYTE:
	code.plantInstruction(new PushLiteralInstruction(val));
	code.plantNoArgInstruction(opc_iadd);
	code.plantNoArgInstruction(opc_i2b);
	break;
      case TID_CHAR:
	code.plantInstruction(new PushLiteralInstruction(val));
	code.plantNoArgInstruction(opc_iadd);
	code.plantNoArgInstruction(opc_i2c);
	break;
      case TID_SHORT:
	code.plantInstruction(new PushLiteralInstruction(val));
	code.plantNoArgInstruction(opc_iadd);
	code.plantNoArgInstruction(opc_i2s);
	break;
      case TID_INT:
	code.plantInstruction(new PushLiteralInstruction(val));
	code.plantNoArgInstruction(opc_iadd);
	break;
      }

      expr.genEndStoreCode(code, true);
    }
  }

    
    public void setExpr(JExpression e)
    {
	expr = e;
    }



  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected int			oper;
  protected JExpression		expr;
  protected CType		type;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JPostfixExpression other = new at.dms.kjc.JPostfixExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JPostfixExpression other) {
  super.deepCloneInto(other);
  other.oper = this.oper;
  other.expr = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.expr);
  other.type = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.type);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

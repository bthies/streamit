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
 * $Id: JUnaryPromote.java,v 1.7 2003-08-21 09:44:21 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;

/**
 * This class convert arithmetics expression from types to types
 */
public class JUnaryPromote extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JUnaryPromote() {} // for cloner only

 /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   */
  public JUnaryPromote(JExpression expr, CType type) {
    super(expr.getTokenReference());

    this.expr = expr;
    this.type = type;
    
    if (!expr.isAssignableTo(type)) {
	needCheck = true;
    }
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
   * Return the expression being promoted.
   */
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
    if (type.equals(CStdType.String)
	&& expr.getType().isReference()
	&& expr.getType() != CStdType.Null) {
      return new JMethodCallExpression(getTokenReference(),
				       new JCheckedExpression(getTokenReference(), expr),
				       "toString",
				       JExpression.EMPTY).analyse(context);
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
    if (needCheck) {
      p.visitUnaryPromoteExpression(this, expr, getType());
    } else {
      expr.accept(p);
    }
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      if (needCheck) {
	  return p.visitUnaryPromoteExpression(this, expr, getType());
      } else {
	  return expr.accept(p);
    }
  }
    

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    expr.genCode(code, false);

    if (type.isNumeric()) {
      ((CNumericType)expr.getType()).genCastTo((CNumericType)type, code);
    } else if (needCheck) {
      code.plantClassRefInstruction(opc_checkcast, ((CClassType)type).getQualifiedName());
    }

    if (discardValue) {
      code.plantPopInstruction(type);
    }
  }

    public void setExpr(JExpression e) {
	expr = e;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		expr;
  private CType			type;
  private boolean		needCheck;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JUnaryPromote other = new at.dms.kjc.JUnaryPromote();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JUnaryPromote other) {
  super.deepCloneInto(other);
  other.expr = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.expr, this);
  other.type = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.type, this);
  other.needCheck = this.needCheck;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JNullLiteral.java,v 1.7 2003-05-28 05:58:44 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * A simple character constant
 */
public class JNullLiteral extends JLiteral {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JNullLiteral() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param where  the line of this node in the source code
   */
  public JNullLiteral(TokenReference where) {
    super(where);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
    return CStdType.Null;
  }

  /**
   * Returns true iff the value of this literal is the
   * default value for this type (JLS 4.5.5).
   */
  public boolean isDefault() {
    return true;
  }

  /**
   * Tests whether this expression denotes a compile-time constant (JLS 15.28).
   * The null literal is not a compile-time constant.
   *
   * @return	true iff this expression is constant
   */
  public boolean isConstant() {
    return false;
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
  public JExpression analyse(CExpressionContext context) {
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
    p.visitNullLiteral();
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitNullLiteral(this);
  }

    /**
     * Returns whether or <o> this represents a literal with the same
     * value as this.
     */
    public boolean equals(Object o) {
	return (o!=null && 
		(o instanceof JNullLiteral));
    }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    if (! discardValue) {
      setLineNumber(code);
      code.plantNoArgInstruction(opc_aconst_null);
    }
  }

    public JExpression convertType(CType dest, CExpressionContext context) {
	throw new InconsistencyException("cannot convert NullType");
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JNullLiteral other = new at.dms.kjc.JNullLiteral();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JNullLiteral other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JBooleanLiteral.java,v 1.7 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * Root class for all expressions
 */
public class JBooleanLiteral extends JLiteral {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JBooleanLiteral() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	image		the string representation of this literal
   */
  public JBooleanLiteral(TokenReference where, boolean value) {
    super(where);
    this.value = value;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
    return CStdType.Boolean;
  }

  /**
   * Returns a string representation of this literal.
   */
  public String toString() {
    StringBuffer	buffer = new StringBuffer();

    buffer.append("JBooleanLiteral[");
    buffer.append(value);
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Returns the constant value of the expression.
   */
  public boolean booleanValue() {
    return value;
  }

  /**
   * Returns true iff the value of this literal is the
   * default value for this type (JLS 4.5.5).
   */
  public boolean isDefault() {
    return !value;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * convertType
   * changes the type of this expression to an other
   * @param  dest the destination type
   */
  public JExpression convertType(CType dest, CExpressionContext context) {
    switch (dest.getTypeID()) {
    case TID_BOOLEAN:
      return this;
    case TID_CLASS:
      if (dest != CStdType.String) {
	throw new InconsistencyException("cannot convert from boolean to " + dest);
      }
      return new JStringLiteral(getTokenReference(), "" + value);
    default:
      throw new InconsistencyException("cannot convert from boolean to " + dest);
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
    p.visitBooleanLiteral(value);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return p.visitBooleanLiteral(this, value);
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
      code.plantInstruction(new PushLiteralInstruction(value ? 1 : 0));
    }
  }

    /**
     * Returns whether or <o> this represents a literal with the same
     * value as this.
     */
    public boolean equals(Object o) {
	return (o!=null && 
		(o instanceof JBooleanLiteral) &&
		((JBooleanLiteral)o).value==this.value);
    }

  /**
   * Generates a sequence of bytescodes to branch on a label
   * This method helps to handle heavy optimizables conditions
   * @param	code		the code list
   */
  public void genBranch(boolean cond, CodeSequence code, CodeLabel label) {
    setLineNumber(code);

    if (value == cond) {
      code.plantJumpInstruction(opc_goto, label);
    }
  }


  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final */ boolean		value; // removed final for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JBooleanLiteral other = new at.dms.kjc.JBooleanLiteral();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JBooleanLiteral other) {
  super.deepCloneInto(other);
  other.value = this.value;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JDoubleLiteral.java,v 1.8 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * JLS 3.10.2 Floating-Point Literals. This class represents double literals.
 */
public class JDoubleLiteral extends JLiteral {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JDoubleLiteral() {} // for cloner only

  /**
   * Constructs a literal expression from a textual representation.
   * @param	where		the line of this node in the source code
   * @param	image		the textual representation of this literal
   */
  public JDoubleLiteral(TokenReference where, String image)
    throws PositionedError
  {
    super(where);
    try {
      this.value = Double.valueOf(image).doubleValue();
    } catch (NumberFormatException e) {
      throw new PositionedError(where, KjcMessages.INVALID_DOUBLE_LITERAL, image);
    }
    if (Double.isInfinite(this.value)) {
      throw new PositionedError(where, KjcMessages.DOUBLE_LITERAL_OVERFLOW, image);
    }
    // cannot be negative since - is an operator :
    if (this.value == 0 && !isZeroLiteral(image)) {
      throw new PositionedError(where, KjcMessages.DOUBLE_LITERAL_UNDERFLOW, image);
    }
  }

  /*
   * Is this a zero literal, i.e. 0.0, 0e22, 0d ... ?
   */
  private boolean isZeroLiteral(String image) {
    char[]	chars = image.toCharArray();

    for (int i = 0; i < chars.length; i++) {
      switch (chars[i]) {
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
	return false;
      case 'd':
      case 'D':
      case 'e':
      case 'E':
	return true;
      }
    }

    return true;
  }

  /**
   * Constructs a literal expression from a constant value.
   * @param	where		the line of this node in the source code
   * @param	value		the constant value
   */
  public JDoubleLiteral(TokenReference where, double value) {
    super(where);
    this.value = value;
  }

    public String toString() {
	return "Double["+value+"]";
    }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the type of this expression.
   */
  public CType getType() {
    return CStdType.Double;
  }

  /**
   * Returns the constant value of the expression.
   */
  public double doubleValue() {
    return value;
  }

  /**
   * Returns true iff the value of this literal is the
   * default value for this type (JLS 4.5.5).
   */
  public boolean isDefault() {
    return Double.doubleToLongBits(value) == ZERO_BITS;
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
    case TID_BYTE:
      return new JByteLiteral(getTokenReference(), (byte)value);
    case TID_SHORT:
      return new JShortLiteral(getTokenReference(), (short)value);
    case TID_CHAR:
      return new JCharLiteral(getTokenReference(), (char)value);
    case TID_INT:
      return new JIntLiteral(getTokenReference(), (int)value);
    case TID_LONG:
      return new JLongLiteral(getTokenReference(), (long)value);
    case TID_FLOAT:
      return new JFloatLiteral(getTokenReference(), (float)value);
    case TID_DOUBLE:
      return this;
    case TID_CLASS:
      if (dest != CStdType.String) {
	throw new InconsistencyException("cannot convert from double to " + dest);
      }
      return new JStringLiteral(getTokenReference(), ""+value);
    default:
      throw new InconsistencyException("cannot convert from double to " + dest);
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
    p.visitDoubleLiteral(value);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitDoubleLiteral(this, value);
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
      code.plantInstruction(new PushLiteralInstruction(value));
    }
  }

    /**
     * Returns whether or <o> this represents a literal with the same
     * value as this.
     */
    public boolean equals(Object o) {
	return (o!=null && 
		(o instanceof JDoubleLiteral) &&
		((JDoubleLiteral)o).value==this.value);
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private static final long	ZERO_BITS = Double.doubleToLongBits(0d);
    private /* final */ double		value;  // removed final for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JDoubleLiteral other = new at.dms.kjc.JDoubleLiteral();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JDoubleLiteral other) {
  super.deepCloneInto(other);
  other.value = this.value;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

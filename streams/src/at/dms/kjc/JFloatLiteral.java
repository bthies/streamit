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
 * $Id: JFloatLiteral.java,v 1.8 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * JLS 3.10.2 Floating-Point Literals. This class represents float literals.
 */
public class JFloatLiteral extends JLiteral {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JFloatLiteral() {} // for cloner only

  /**
   * Constructs a literal expression from a textual representation.
   * @param	where		the line of this node in the source code
   * @param	image		the textual representation of this literal
   */
  public JFloatLiteral(TokenReference where, String image)
    throws PositionedError
  {
    super(where);
    try {
      this.value = Float.valueOf(image).floatValue();
    } catch (NumberFormatException e) {
      throw new PositionedError(where, KjcMessages.INVALID_FLOAT_LITERAL, image);
    }
    if (Float.isInfinite(this.value)) {
      throw new PositionedError(where, KjcMessages.FLOAT_LITERAL_OVERFLOW, image);
    }
    // cannot be negative since - is an operator :
    if (this.value == 0 && !isZeroLiteral(image)) {
      throw new PositionedError(where, KjcMessages.FLOAT_LITERAL_UNDERFLOW, image);
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
      case 'e':
      case 'E':
	return true;
      case 'f':
      case 'F':
      }
    }

    return true;
  }

  /**
   * Constructs a literal expression from a constant value.
   * @param	where		the line of this node in the source code
   * @param	value		the constant value
   */
  public JFloatLiteral(TokenReference where, float value) {
    super(where);
    this.value = value;
  }

  public JFloatLiteral(float value) {
      this(null, value);
  }

    public String toString() {
	return "JFloatLiteral["+value+"]";
    }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the type of this expression.
   */
  public CType getType() {
    return CStdType.Float;
  }

  /**
   * Returns the constant value of the expression.
   */
  public float floatValue() {
    return value;
  }

  /**
   * Returns true iff the value of this literal is the
   * default value for this type (JLS 4.5.5).
   */
  public boolean isDefault() {
    return Float.floatToIntBits(value) == ZERO_BITS;
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
      return this;
    case TID_DOUBLE:
      return new JDoubleLiteral(getTokenReference(), (double)value);
    case TID_CLASS:
      if (dest != CStdType.String) {
	throw new InconsistencyException("cannot convert from float to " + dest);
      }
      return new JStringLiteral(getTokenReference(), "" + value);
    default:
      throw new InconsistencyException("cannot convert from float to " + dest);
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
    p.visitFloatLiteral(value);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitFloatLiteral(this, value);
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
		(o instanceof JFloatLiteral) &&
		((JFloatLiteral)o).value==this.value);
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private static final int	ZERO_BITS = Float.floatToIntBits(0f);
    private /* final */ float		value;  // removed final for cloner
}

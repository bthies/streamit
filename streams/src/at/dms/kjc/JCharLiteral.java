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
 * $Id: JCharLiteral.java,v 1.7 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.NumberParser;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

/**
 * A simple character constant
 */
public class JCharLiteral extends JLiteral {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JCharLiteral() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	image		the string representation of this literal
   */
  public JCharLiteral(TokenReference where, String image)
    throws PositionedError
  {
    super(where);
    try {
      value = decodeChar(image);
    } catch (UnpositionedError e) {
      throw e.addPosition(where);
    }
  }

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	image		the string representation of this literal
   */
  public JCharLiteral(TokenReference where, char value) {
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
    return CStdType.Char;
  }

  /**
   * Returns the constant value of the expression.
   */
  public char charValue() {
    return value;
  }

  /**
   * Returns true iff the value of this literal is the
   * default value for this type (JLS 4.5.5).
   */
  public boolean isDefault() {
    return value == '\u0000';
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Returns the character represented by the specified image.
   * @exception	UnpositionedError	if the image is invalid.
   */
  private static char decodeChar(String image) throws UnpositionedError {
    char	value;

    if (image.startsWith("\\u")) {
      value = (char)NumberParser.decodeHexInt(false, image.substring(2));
    } else if (image.startsWith("\\") && image.length() > 1) {
      if ((image.charAt(1) >= '0') && (image.charAt(1) <= '9')) {
	value = (char)NumberParser.decodeOctInt(false, image.substring(1));
	if (value > 377) {
	  throw new UnpositionedError(KjcMessages.INVALID_OCTAL_CHAR, image);
	}
      } else if (image.equals("\\b")) {
	value = '\b';
      } else if (image.equals("\\r")) {
	value = '\r';
      } else if (image.equals("\\t")) {
	value = '\t';
      } else if (image.equals("\\n")) {
	value = '\n';
      } else if (image.equals("\\f")) {
	value = '\f';
      } else if (image.equals("\\\"")) {
	value = '\"';
      } else if (image.equals("\\\'")) {
	value = '\'';
      } else if (image.equals("\\\\")) {
	value = '\\';
      } else {
	throw new UnpositionedError(KjcMessages.INVALID_ESCAPE_SEQUENCE, image);
      }
    } else {
      value = image.charAt(0);
    }

    return value;
  }

  /**
   * Can this expression be converted to the specified type by assignment conversion (JLS 5.2) ?
   * @param	dest		the destination type
   * @return	true iff the conversion is valid
   */
  public boolean isAssignableTo(CType dest) {
    switch (dest.getTypeID()) {
    case TID_BYTE:
      return (byte)value == value;
    case TID_SHORT:
      return (short)value == value;
    case TID_CHAR:
      return true;
    default:
      return CStdType.Char.isAssignableTo(dest);
    }
  }

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
      return this;
    case TID_INT:
      return new JIntLiteral(getTokenReference(), (int)value);
    case TID_LONG:
      return new JLongLiteral(getTokenReference(), (long)value);
    case TID_FLOAT:
      return new JFloatLiteral(getTokenReference(), (float)value);
    case TID_DOUBLE:
      return new JDoubleLiteral(getTokenReference(), (double)value);
    case TID_CLASS:
      if (dest != CStdType.String) {
	throw new InconsistencyException("cannot convert from char to " + dest);
      }
      return new JStringLiteral(getTokenReference(), "" + value);
    default:
      throw new InconsistencyException("cannot convert from char to " + dest);
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
    p.visitCharLiteral(value);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitCharLiteral(this, value);
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
		(o instanceof JCharLiteral) &&
		((JCharLiteral)o).value==this.value);
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private char		value;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JCharLiteral other = new at.dms.kjc.JCharLiteral();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JCharLiteral other) {
  super.deepCloneInto(other);
  other.value = this.value;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

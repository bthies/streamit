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
 * $Id: JStringLiteral.java,v 1.6 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.PushLiteralInstruction;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * A simple character constant
 */
public class JStringLiteral extends JLiteral {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    private JStringLiteral() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	image		the string representation of this literal
   */
  public JStringLiteral(TokenReference where, String image) {
    this(where, image, false);
  }

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	image		the string representation of this literal
   * @param	quoted		there is quote around image
   */
  public JStringLiteral(TokenReference where, String image, boolean quoted) {
    super(where);

    if (image == null) {
      throw new InconsistencyException();
    }

    if (quoted) {
      StringBuffer s = new StringBuffer();
      for (int i = 0; i < image.length(); i++) {
	char c = image.charAt(i);
	if (c == '\\') {
	  if (i + 1 < image.length() - 1) {
	    i++;
	    c = image.charAt(i);
	    switch (c) {
	    case 'n' : c = '\n'; break;
	    case 'r' : c = '\r'; break;
	    case 't' : c = '\t'; break;
	    case 'b' : c = '\b'; break;
	    case 'f' : c = '\f'; break;
	    case '"' : c = '\"'; break;
	    case '\'' : c = '\''; break;
	    case '\\' : c = '\\'; break;
	    }
	  }
	}
	s.append(c);
      }
      value = s.toString();
      value = value.substring(1, value.length() - 1);
    } else {
      value = image;
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
    return CStdType.String;
  }

  /**
   * Returns the constant value of the expression.
   */
  public String stringValue() {
    return value;
  }

  /**
   * Returns true iff the value of this literal is the
   * default value for this type (JLS 4.5.5).
   */
  public boolean isDefault() {
    return false;
  }

  /**
   * Returns a string representation of this literal.
   */
  public String toString() {
    StringBuffer	buffer = new StringBuffer();

    buffer.append("JStringLiteral[");
    buffer.append(value);
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
    StringBuffer s = new StringBuffer();
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
      case '\n' : s.append("\\n"); break;
      case '\r' : s.append("\\r"); break;
      case '\t' : s.append("\\t"); break;
      case '\b' : s.append("\\b"); break;
      case '\f' : s.append("\\f"); break;
      case '\"' : s.append("\\\""); break;
      case '\'' : s.append("\\\'"); break;
      case '\\' : s.append("\\\\"); break;
      default:
	s.append(c);
      }
    }
    value = s.toString();
    p.visitStringLiteral(value);
  }
 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
    public Object accept(AttributeVisitor p) {
	StringBuffer s = new StringBuffer();
	for (int i = 0; i < value.length(); i++) {
	    char c = value.charAt(i);
	    switch (c) {
	    case '\n' : s.append("\\n"); break;
	    case '\r' : s.append("\\r"); break;
	    case '\t' : s.append("\\t"); break;
	    case '\b' : s.append("\\b"); break;
	    case '\f' : s.append("\\f"); break;
	    case '\"' : s.append("\\\""); break;
	    case '\'' : s.append("\\\'"); break;
	    case '\\' : s.append("\\\\"); break;
	    default:
		s.append(c);
	    }
	}
	value = s.toString();
	return p.visitStringLiteral(this, value);
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
		(o instanceof JStringLiteral) &&
		((JStringLiteral)o).value.equals(this.value));
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private	String		value;
    
    public JExpression convertType(CType dest, CExpressionContext context) {
	if(dest.getTypeID()!=TID_CLASS)
	    throw new InconsistencyException("cannot convert StringType");
	return this;
    }
}

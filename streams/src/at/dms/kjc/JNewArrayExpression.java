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
 * $Id: JNewArrayExpression.java,v 1.9 2003-10-09 13:49:49 jasperln Exp $
 */

package at.dms.kjc;

import at.dms.classfile.MultiarrayInstruction;
import at.dms.classfile.ClassRefInstruction;
import at.dms.classfile.NewarrayInstruction;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * JLS 15.10 Array Creation Expressions.
 *
 * An array instance creation expression is used to create new arrays.
 */
public class JNewArrayExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JNewArrayExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	type		the type of elements of this array
   * @param	dims		the dimensions of the array
   * @param	init		an initializer for the array
   */
  public JNewArrayExpression(TokenReference where,
			     CType type,
			     JExpression[] dims,
			     JArrayInitializer init)
  {
    super(where);

    this.type = new CArrayType(type, dims.length);
    this.dims = dims;
    this.init = init;
  }

    public String toString() {
	StringBuffer out=new StringBuffer("JNewArrayExpr[");
	if(dims!=null&&dims.length>0) {
	    out.append(dims[0].toString());
	    for(int i=1;i<dims.length;i++) {
		out.append(",");
		if(dims[i].toString()!=null)
		    out.append(dims[i].toString());
	    }
	}
	return out.append("]").toString();
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
   * Find the dimensions of this expression
   */
  public JExpression[] getDims() {
    return dims;
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
    boolean	hasBounds;

    hasBounds = analyseDimensions(context);

    try {
      type.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }

    if (init == null) {
      check(context, hasBounds, KjcMessages.MULTIARRAY_BOUND_MISSING);
    } else {
      check(context, !hasBounds, KjcMessages.ARRAY_BOUND_AND_INITIALIZER);
      init.setType(type);
      /*!!! init =*/ init.analyse(context);
    }

    return this;
  }

  /**
   * Analyses the dimension array.
   *
   * @return	true iff there are expressions for some sizes
   */
  private boolean analyseDimensions(CExpressionContext context)
    throws PositionedError
  {
    boolean	lastEmpty = false;	// last dimension empty ?
    boolean	hasBounds = false;	// expressions for some sizes ?

    for (int i = 0; i < dims.length; i++) {
      if (dims[i] == null) {
	lastEmpty = true;
      } else {
	check(context, !lastEmpty, KjcMessages.MULTIARRAY_BOUND_MISSING);
	dims[i] = dims[i].analyse(context);
	check(context,
	      dims[i].getType().isAssignableTo(CStdType.Integer),
	      KjcMessages.ARRAY_BADTYPE, dims[i].getType());
	hasBounds = true;
      }
    }
    return hasBounds;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitNewArrayExpression(this, type.getBaseType(), dims, init);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitNewArrayExpression(this, type.getBaseType(), dims, init);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    if (init == null) {
      allocArray(code, type, dims);
      if (discardValue) {
	code.plantPopInstruction(type);
      }
    } else {
      init.genCode(code, discardValue);
    }
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public static void allocArray(CodeSequence code, CArrayType type, JExpression[] dims) {
    for (int i = 0; i < dims.length; i++) {
      if (dims[i] != null) {
	dims[i].genCode(code, false);
      }
    }

    if (type.getArrayBound() > 1) {
      int		filled = 0;

      for (int i = 0; i < dims.length && dims[i] != null; i++) {
	filled++;
      }
      code.plantInstruction(new MultiarrayInstruction(type.getSignature(), filled));
    } else {
      code.plantNewArrayInstruction(type.getElementType());
    }
  }

    /**
     * Sets init expression of this.
     */
    public void setInit(JArrayInitializer init) {
	this.init = init;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CArrayType			type;
  private JExpression[]			dims;
  private JArrayInitializer		init;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JNewArrayExpression other = new at.dms.kjc.JNewArrayExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JNewArrayExpression other) {
  super.deepCloneInto(other);
  other.type = (at.dms.kjc.CArrayType)at.dms.kjc.AutoCloner.cloneToplevel(this.type, other);
  other.dims = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.dims, other);
  other.init = (at.dms.kjc.JArrayInitializer)at.dms.kjc.AutoCloner.cloneToplevel(this.init, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

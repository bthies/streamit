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
 * $Id: JSwitchLabel.java,v 1.5 2003-05-28 05:58:44 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;
import at.dms.util.MessageDescription;

/**
 * This class represents a parameter declaration in the syntax tree
 */
public class JSwitchLabel extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------
    protected JSwitchLabel() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	expr		the expression (null if default label)
   */
  public JSwitchLabel(TokenReference where, JExpression expr) {
    super(where);

    this.expr = expr;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return	true if this label is a "default:" label
   */
  public boolean isDefault() {
    return expr == null;
  }

  /**
   * @return	the value of this label
   */
  public Integer getLabel() {
    return new Integer(getLabelValue());
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the node (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CSwitchGroupContext context)
    throws PositionedError
  {
    if (expr != null) {
      expr = expr.analyse(new CExpressionContext(context));
      check(context, expr.isConstant(), KjcMessages.SWITCH_LABEL_EXPR_NOTCONST);
      check(context,
	    expr.isAssignableTo(context.getType()),
	    KjcMessages.SWITCH_LABEL_OVERFLOW, expr.getType());

      try {
	context.addLabel(new Integer(getLabelValue()));
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }
    } else {
      try {
	context.addDefault();
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }
    }
  }

  /**
   * Adds a compiler error.
   * @param	context		the context in which the error occurred
   * @param	key		the message ident to be displayed
   * @param	params		the array of parameters
   *
   */
  protected void fail(CContext context, MessageDescription key, Object[] params)
    throws PositionedError
  {
    throw new CLineError(getTokenReference(), key, params);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitSwitchLabel(this, expr);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitSwitchLabel(this, expr);
  }

    /**
     * Sets the expression of this.
     */
    public void setExpression(JExpression expr) {
	this.expr = expr;
    }

  // ----------------------------------------------------------------------
  // IMPLEMENTATION
  // ----------------------------------------------------------------------

  private int getLabelValue() {
    CType	type = expr.getType();

    if (type == CStdType.Byte) {
      return expr.byteValue();
    } else if (type == CStdType.Char) {
      return expr.charValue();
    } else if (type == CStdType.Short) {
      return expr.shortValue();
    } else if (type == CStdType.Integer) {
      return expr.intValue();
    } else {
      throw new InconsistencyException("unexpected type " + type);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		expr;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JSwitchLabel other = new at.dms.kjc.JSwitchLabel();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JSwitchLabel other) {
  super.deepCloneInto(other);
  other.expr = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.expr);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

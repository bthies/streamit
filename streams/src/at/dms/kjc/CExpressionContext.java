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
 * $Id: CExpressionContext.java,v 1.4 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

/**
 * This class provides the contextual information for the semantic
 * analysis of an expression.
 */
public class CExpressionContext extends CContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CExpressionContext() {} // for cloner only

  /**
   * Constructs the context to analyse an expression semantically.
   *
   * @param	parent		the analysis context of the entity
   *				containing the expression
   * @param	isLeftSide	is the expression the left hand side
   *				of an assignment ?
   * @param	discardValue	will the result of the evaluation of
   *				the expression be discarded ?
   */
  public CExpressionContext(CBodyContext parent,
			    boolean isLeftSide,
			    boolean discardValue)
  {
    super(parent);
    this.isLeftSide = isLeftSide;
    this.discardValue = discardValue;
  }

  /**
   * Constructs the context to analyse an expression semantically.
   *
   * @param	parent		the analysis context of the entity
   *				containing the expression
   */
  public CExpressionContext(CBodyContext parent) {
    this(parent, false, false);
  }

  /**
   * Constructs the context to analyse an expression semantically.
   *
   * @param	parent		the analysis context of the entity
   *				containing the expression
   * @param	isLeftSide	is the expression the left hand side
   *				of an assignment ?
   * @param	discardValue	will the result of the evaluation of
   *				the expression be discarded ?
   */
  public CExpressionContext(CExpressionContext parent,
			    boolean isLeftSide,
			    boolean discardValue)
  {
    super(parent.getBodyContext());
    this.isLeftSide = isLeftSide;
    this.discardValue = discardValue;
  }

  /**
   * Constructs the context to analyse an expression semantically.
   *
   * @param	parent		the analysis context of the entity
   *				containing the expression
   */
  public CExpressionContext(CExpressionContext parent) {
    this(parent, false, false);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the parent context.
   */
  public CBodyContext getBodyContext() {
    return (CBodyContext)parent;
  }

  /**
   * Returns true iff the expression is the left hand side of an assignment.
   */
  public boolean isLeftSide() {
    return isLeftSide;
  }

  /**
   * Returns true iff the result of the evaluation of the expression
   * will be discarded.
   */
  public boolean discardValue() {
    return discardValue;
  }

  // ----------------------------------------------------------------------
  // PRIVATE UTILITIES
  // ----------------------------------------------------------------------

  /**
   * Is the expression the left hand side of an assignment ?
   */
    private /* final */ boolean		isLeftSide; // removed final for cloner

  /**
   * Will the result of the evaluation of the expression be discarded ?
   */
      private /* final */ boolean		discardValue; // removed final for cloner

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CExpressionContext other = new at.dms.kjc.CExpressionContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CExpressionContext other) {
  super.deepCloneInto(other);
  other.isLeftSide = this.isLeftSide;
  other.discardValue = this.discardValue;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

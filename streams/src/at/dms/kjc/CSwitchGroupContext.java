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
 * $Id: CSwitchGroupContext.java,v 1.3 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;

import at.dms.compiler.UnpositionedError;

/**
 * This class represents a local context during checkBody
 * It follows the control flow and maintain informations about
 * variable (initialised, used, allocated), exceptions (thrown, catched)
 * It also verify that context is still reachable
 *
 * There is a set of utilities method to access fields, methods and class
 * with the name by clamping the parsing tree
 * @see CCompilationUnitContext
 * @see CClassContext
 * @see CMethodContext
 * @see CContext
 */
public class CSwitchGroupContext extends CBodyContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CSwitchGroupContext() {} // for cloner only

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * @param	parent		the parent context
   */
  public CSwitchGroupContext(CSwitchBodyContext parent) {
    // clone
    super(parent, parent);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * add a default label to this switch
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void addDefault() throws UnpositionedError {
    ((CSwitchBodyContext)getParentContext()).addDefault();
  }

  /**
   * add a label to this switch and check it is a new one
   * @param	lit		the literal value of this label
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void addLabel(Integer value) throws UnpositionedError {
    ((CSwitchBodyContext)getParentContext()).addLabel(value);
  }

  /**
   * Returns the type of the switch expression.
   */
  public CType getType() {
    return ((CSwitchBodyContext)getParentContext()).getType();
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CSwitchGroupContext other = new at.dms.kjc.CSwitchGroupContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CSwitchGroupContext other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: CTryFinallyContext.java,v 1.6 2003-11-13 10:46:10 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;

import at.dms.compiler.TokenReference;

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
public class CTryFinallyContext extends CBlockContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CTryFinallyContext() {} // for cloner only

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   */
  CTryFinallyContext(CBodyContext parent) {
    super(parent);
    clearThrowables();
  }

  public void close(TokenReference ref) {
  }

  /**
   *
   */
  void forwardBreaksAndContinues() {
    CBodyContext	parent = (CBodyContext)getParentContext();

    if (breaks != null) {
      for (int i = 0; i < breaks.size(); i++) {
	Object[]	elem = (Object[])breaks.elementAt(i);

	parent.addBreak((JStatement)elem[0], (CBodyContext)elem[1]);
      }
    }
    if (continues != null) {
      for (int i = 0; i < continues.size(); i++) {
	Object[]	elem = (Object[])continues.elementAt(i);

	parent.addContinue((JStatement)elem[0], (CBodyContext)elem[1]);
      }
    }
  }

  /**
   *
   * @param	target		the target of the break statement
   * @param	context		the context at the break statement
   */
  protected void addBreak(JStatement target,
			  CBodyContext context)
  {
    if (breaks == null) {
      breaks = new Vector();
    }
    //!!! FIXME clone context ???
    breaks.addElement(new Object[]{ target, context });
  }

  /**
   *
   * @param	target		the target of the continue statement
   * @param	context		the context at the continue statement
   */
  protected void addContinue(JStatement target,
			     CBodyContext context)
  {
    if (continues == null) {
      continues = new Vector();
    }
    //!!! FIXME clone context ???
    continues.addElement(new Object[]{ target, context });
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private Vector	breaks;
  private Vector	continues;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CTryFinallyContext other = new at.dms.kjc.CTryFinallyContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CTryFinallyContext other) {
  super.deepCloneInto(other);
  other.breaks = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.breaks);
  other.continues = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.continues);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

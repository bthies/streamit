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
 * $Id: CSwitchBodyContext.java,v 1.4 2003-05-28 05:58:42 thies Exp $
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
public class CSwitchBodyContext extends CBodyContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CSwitchBodyContext() {} // for cloner only

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   * @param	switchType	the size of switch (byte, short, char...)
   */
  public CSwitchBodyContext(CBodyContext parent,
			    JSwitchStatement stmt)
  {
    super(parent);
    this.stmt = stmt;
    clearFlowState();
    setInLoop(parent.isInLoop());
  }

  /**
   * close
   */
  public void close() {
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (SWITCHES)
  // ----------------------------------------------------------------------

  /**
   * add a default label to this switch
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void addDefault() throws UnpositionedError {
    if (defaultExist) {
      throw new UnpositionedError(KjcMessages.SWITCH_DEFAULT_DOUBLE);
    }
    defaultExist = true;
  }

  /**
   * add a label to this switch and check it is a new one
   * @param	value		the literal value of this label
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void addLabel(Integer value) throws UnpositionedError {
    if (labels.put(value, "*") != null) {
      throw new UnpositionedError(KjcMessages.SWITCH_LABEL_EXIST, value);
    }
  }

  /**
   * Returns the type of the switch expression.
   */
  public CType getType() {
    return stmt.getType();
  }

  /**
   * @return	true is a default label has already happen
   */
  public boolean defaultExists() {
    return defaultExist;
  }

  /**
   * Returns the innermost statement which can be target of a break
   * statement without label.
   */
  public JStatement getNearestBreakableStatement() {
    return stmt;
  }

  /**
   *
   */
  protected void addBreak(JStatement target,
			  CBodyContext context)
  {
    if (stmt == target) {
      stmt.addBreak(context);
    } else {
      ((CBodyContext)getParentContext()).addBreak(target, context);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final */ JSwitchStatement	stmt; // removed final for cloner
    private /* final */ Hashtable		labels = new Hashtable(); // removed final for cloner
    private boolean			defaultExist;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CSwitchBodyContext other = new at.dms.kjc.CSwitchBodyContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CSwitchBodyContext other) {
  super.deepCloneInto(other);
  other.stmt = (at.dms.kjc.JSwitchStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.stmt);
  other.labels = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.labels);
  other.defaultExist = this.defaultExist;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

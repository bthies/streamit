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
 * $Id: CMethodContext.java,v 1.3 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;
import java.util.Enumeration;

import at.dms.compiler.CWarning;
import at.dms.compiler.TokenReference;
import at.dms.compiler.PositionedError;

/**
 * This class represents a method context during check
 * @see CContext
 * @see CCompilationUnitContext
 * @see CClassContext
 * @see CMethodContext
 * @see CConstructorContext
 * @see CInitializerContext
 * @see CBodyContext
 * @see CBlockContext
 */
public class CMethodContext extends CContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CMethodContext() {} // for cloner only

  /**
   * CMethodContext
   * @param	parent		the parent context
   * @param	self		the corresponding method interface
   */
  CMethodContext(CClassContext parent, CMethod self) {
    super(parent);
    this.self = self;
  }

  /**
   * Verify that all checked exceptions are defined in the throw list
   * and return types are valid
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void close(TokenReference ref) throws PositionedError {
    Enumeration		enum = throwables.elements();
    CClassType[]	checked = self.getThrowables();
    boolean[]		used = new boolean[checked.length];

  loop:
    while (enum.hasMoreElements()) {
      CThrowableInfo	thrown = (CThrowableInfo)enum.nextElement();
      CClassType	type = thrown.getThrowable();

      // only checked exceptions need to be checked
      if (! type.isCheckedException()) {
	continue loop;
      }

      for (int j = 0; j < checked.length; j++) {
	if (type.isAssignableTo(checked[j])) {
	  used[j] = true;
	  continue loop;
	}
      }
      throw new PositionedError(thrown.getLocation().getTokenReference(),
				KjcMessages.METHOD_UNCATCHED_EXCEPTION,
				type,
				null);
    }

    for (int i = 0; i < checked.length; i++) {
      if (!checked[i].isCheckedException()) {
	reportTrouble(new CWarning(ref,
				   KjcMessages.METHOD_UNCHECKED_EXCEPTION,
				   checked[i],
				   null));
      } else if (!used[i]) {
	reportTrouble(new CWarning(ref,
				   KjcMessages.METHOD_UNTHROWN_EXCEPTION,
				   checked[i],
				   null));
      }
    }
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------
  /** 
   * JLS 8.1.2: A statement or expression occurs in a static context if and 
   * only if the innermost method, constructor, instance initializer, static 
   * initializer, field initializer, or explicit constructor statement 
   * enclosing the statement or expression is a static method, a static 
   * initializer, the variable initializer of a static variable, or an 
   * explicit constructor invocation statement 
   *
   * @return true iff the context is static
   */
  public boolean isStaticContext() {
    if (self.isStatic()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * getClassContext
   * @return	the near parent of type CClassContext
   */
  public CClassContext getClassContext() {
    return getParentContext().getClassContext();
  }

  /**
   * getCMethod
   * @return	the near parent of type CMethodContext
   */
  public CMethod getCMethod() {
    return self;
  }

  /**
   * getMethod
   * @return	the near parent of type CMethodContext
   */
  public CMethodContext getMethodContext() {
    return this;
  }

  public int localsPosition() {
    return 0;
  }

  // ----------------------------------------------------------------------
  // THROWABLES
  // ----------------------------------------------------------------------

  /**
   * @param	throwable	the type of the new throwable
   */
  public void addThrowable(CThrowableInfo throwable) {
    throwables.put(throwable.getThrowable().toString(), throwable);
  }

  /**
   * @return the list of exception that may be thrown
   */
  public Hashtable getThrowables() {
    return throwables;
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected	Hashtable	throwables = new Hashtable();
  protected	Hashtable	labels;		// Hashtable<String, String>
  private	CMethod		self;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CMethodContext other = new at.dms.kjc.CMethodContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CMethodContext other) {
  super.deepCloneInto(other);
  other.throwables = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.throwables);
  other.labels = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.labels);
  other.self = (at.dms.kjc.CMethod)at.dms.kjc.AutoCloner.cloneToplevel(this.self);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: CConstructorContext.java,v 1.3 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import java.util.Enumeration;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class represents a method context during check
 * @see CCompilationUnitContext
 * @see CClassContext
 * @see CMethodContext
 * @see CContext
 */
public class CConstructorContext extends CMethodContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CConstructorContext() {} // for cloner only
  /**
   * CConstructorContext
   * @param	parent		the parent context
   * @param	self		the corresponding method interface
   */
  public CConstructorContext(CClassContext parent, CMethod self) {
    super(parent, self);

    // we create a local copy of field info
    this.fieldInfo = new CVariableInfo();

    isSuperConstructorCalled = true;
  }

  /**
   * Verify that all checked exceptions are defined in the throw list
   * @exception UnpositionedError	this error will be positioned soon
   */
  public void close(TokenReference ref) throws PositionedError {
    if (getClassContext().getCClass().isAnonymous()) {
      getCMethod().setThrowables(getThrowables());
    }

    super.close(ref);
  }

  /**
   *
   */
  public void setSuperConstructorCalled(boolean b) {
    isSuperConstructorCalled = b;
  }

  /**
   *
   */
  public boolean isSuperConstructorCalled() {
    return isSuperConstructorCalled;
  }

  // ----------------------------------------------------------------------
  // FIELD STATE
  // ----------------------------------------------------------------------
 
  /**
   * Returns the field definition state.
   */
  public CVariableInfo getFieldInfo() {
    return fieldInfo;
  }

  /**
   * @param	var		the definition of a field
   * @return	all informations we have about this field
   */
  public int getFieldInfo(int index) {
    return fieldInfo.getInfo(index);
  }

  /**
   * @param	index		The field position in method array of local vars
   * @param	info		The information to add
   *
   * We make it a local copy of this information and at the end of this context
   * we will transfert it to the parent context according to controlFlow
   */
  public void setFieldInfo(int index, int info) {
    fieldInfo.setInfo(index, info);
  }

  /**
   * Marks all instance fields of this class initialized.
   */
  public void markAllFieldToInitialized() {
    for (int i = parent.getClassContext().getCClass().getFieldCount() - 1; i >= 0; i--) {
      setFieldInfo(i, CVariableInfo.INITIALIZED);
    }
  }

  /**
   * Adopts field state from instance initializer.
   */
  public void adoptInitializerInfo() {
    CClassContext	classContext = parent.getClassContext();
    CVariableInfo	initializerInfo = classContext.getInitializerInfo();
    int			fieldCount = classContext.getCClass().getFieldCount();

    if (initializerInfo != null) {
      for (int i = 0; i < fieldCount; i++) {
	int	info = initializerInfo.getInfo(i);

	if (info != 0) {
	  setFieldInfo(i, info);
	}
      }
    }
  }

  // ----------------------------------------------------------------------
  // DEBUG
  // ----------------------------------------------------------------------

  /**
   * Dumps this context to standard error stream.
   */
  public void dumpContext(String text) {
    System.err.println(text + " " + this + " parent: " + parent);
    System.err.print("    flds: ");
    if (fieldInfo == null) {
      System.err.print("---");
    } else {
      for (int i = 0; i < 8; i++) {
	System.err.print(" " + i + ":" + getFieldInfo(i));
      }
    }
    System.err.println("");
    System.err.print("    excp: ");
    for (Enumeration e = throwables.elements(); e.hasMoreElements(); ) {
      System.err.print(" " + ((CThrowableInfo)e.nextElement()).getThrowable());
    }
    System.err.println("");
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private boolean		isSuperConstructorCalled;

  private CVariableInfo		fieldInfo;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CConstructorContext other = new at.dms.kjc.CConstructorContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CConstructorContext other) {
  super.deepCloneInto(other);
  other.isSuperConstructorCalled = this.isSuperConstructorCalled;
  other.fieldInfo = (at.dms.kjc.CVariableInfo)at.dms.kjc.AutoCloner.cloneToplevel(this.fieldInfo);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

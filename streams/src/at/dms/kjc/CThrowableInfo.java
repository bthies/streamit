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
 * $Id: CThrowableInfo.java,v 1.7 2003-11-13 10:46:10 thies Exp $
 */

package at.dms.kjc;

/**
 * This class represents a throw <throwable> information during check
 */
public class CThrowableInfo extends at.dms.util.Utils {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    private CThrowableInfo() {} // for cloner only

  /**
   * Constructs an informztion handler
   * @param	throwable		the type of exception
   * @param	location		the throw statement
   */
  public CThrowableInfo(CClassType throwable, JPhylum location) {
    this.throwable = throwable;
    this.location = location;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * getVar
   * @return	the variable definition
   */
  public CClassType getThrowable() {
    return throwable;
  }

  /**
   * Return the location of this throwable
   */
  public JPhylum getLocation() {
    return location;
  }

  /**
   * Sets this throwable to be cached or not
   */
  public void setCatched(boolean catched) {
    this.catched = catched;
  }

  /**
   * Return true if this throwable is catched
   */
  public boolean isCatched() {
    return catched;
  }

  // ----------------------------------------------------------------------
  // OPTIMIZATION
  // ----------------------------------------------------------------------

  public boolean equals(Object o) {
    return ((CThrowableInfo)o).throwable.getCClass() == throwable.getCClass();
  }

  public int hashCode() {
    return throwable.getCClass().hashCode();
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final*/ CClassType	throwable; // removed final for cloner
    private /* final*/ JPhylum		location; // removed final for cloner
    private boolean		catched;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CThrowableInfo other = new at.dms.kjc.CThrowableInfo();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CThrowableInfo other) {
  super.deepCloneInto(other);
  other.throwable = (at.dms.kjc.CClassType)at.dms.kjc.AutoCloner.cloneToplevel(this.throwable);
  other.location = (at.dms.kjc.JPhylum)at.dms.kjc.AutoCloner.cloneToplevel(this.location);
  other.catched = this.catched;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

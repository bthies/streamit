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
 * $Id: CBooleanType.java,v 1.3 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.UnpositionedError;
import at.dms.util.SimpleStringBuffer;

/**
 * This class represents java and kopi Numericals types
 * Such as byte, short, int, long, float, double
 */
public class CBooleanType extends CType {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Constructor
   */
  protected CBooleanType() {
    super(TID_BOOLEAN);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

    private Object readResolve() throws Exception {
	return CStdType.Boolean;
    }
    

  /**
   * Transforms this type to a string
   */
  public String toString() {
    return "boolean";
  }

  /**
   * Returns the VM signature of this type.
   */
  public String getSignature() {
    return "Z";
  }

  /**
   * Appends the VM signature of this type to the specified buffer.
   */
  protected void appendSignature(SimpleStringBuffer buffer) {
    buffer.append('Z');
  }

  /**
   * Returns the stack size used by a value of this type.
   */
  public int getSize() {
    return 1;
  }

  // ----------------------------------------------------------------------
  // BODY CHECKING
  // ----------------------------------------------------------------------

  /**
   * check that type is valid
   * necessary to resolve String into java/lang/String
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void checkType(CContext context) throws UnpositionedError {
  }

  /**
   * Can this type be converted to the specified type by assignment conversion (JLS 5.2) ?
   * @param	dest		the destination type
   * @return	true iff the conversion is valid
   */
  public boolean isAssignableTo(CType dest) {
    return dest == CStdType.Boolean;
  }

  /**
   * Can this type be converted to the specified type by casting conversion (JLS 5.5) ?
   * @param	dest		the destination type
   * @return	true iff the conversion is valid
   */
  public boolean isCastableTo(CType dest) {
    return dest == CStdType.Boolean;
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CBooleanType other = new at.dms.kjc.CBooleanType();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CBooleanType other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

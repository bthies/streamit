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
 * $Id: CClassType.java,v 1.9 2004-01-28 16:55:35 dmaze Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;
import at.dms.compiler.Compiler;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;
import at.dms.util.SimpleStringBuffer;

/**
 * This class represents class type in the type structure
 */
public class CClassType extends CType {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Construct a class type
   * @param	clazz		the class that will represent this type
   */
  protected CClassType() {
    super(TID_CLASS);

    this.clazz = BAC_CLASS;
  }

  /**
   * Construct a class type
   * @param	clazz		the class that will represent this type
   */
  public CClassType(CClass clazz) {
    super(TID_CLASS);

    this.clazz = clazz;

    if (!(this instanceof CArrayType)) {
      allCClassType.put(clazz.getQualifiedName(), this);
    }
  }

    private Object readResolve() {
	if (this.getQualifiedName().equals(Constants.JAV_STRING))
	    return CStdType.String;
	return this;
    }

  public static CClassType lookup(String qualifiedName) {
    if (qualifiedName.indexOf('/') >= 0) {
      CClassType	type = (CClassType)allCClassType.get(qualifiedName);

      if (type == null) {
	type = new CClassNameType(qualifiedName);
	allCClassType.put(qualifiedName, type);
      }

      return type;
    } else {
      return new CClassNameType(qualifiedName);
    }
  }

    protected boolean isChecked() {
    return clazz != BAC_CLASS;
  }

  public void setClass(CClass clazz) {
    this.clazz = clazz;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * equals
   */
  public boolean equals(CType other) {
    return (!other.isClassType() || other.isArrayType()) ?
      false :
      ((CClassType)other).getCClass() == getCClass();
  }

  /**
   * Transforms this type to a string
   */
  public String toString() {
    if (clazz.isNested()) {
      return clazz.getIdent();
    } else {
      return getQualifiedName().replace('/', '.');
    }
  }

  /**
   * Appends the VM signature of this type to the specified buffer.
   */
  protected void appendSignature(SimpleStringBuffer buffer) {
    buffer.append('L');
    buffer.append(getQualifiedName());
    buffer.append(';');
  }

  /**
   * @return the short name of this class
   */
  public String getIdent() {
    return getCClass().getIdent();
  }

  /**
   *
   */
  public String getQualifiedName() {
    return getCClass().getQualifiedName();
  }

  /**
   * Returns the stack size used by a value of this type.
   */
  public int getSize() {
    return 1;
  }

  /**
   * Check if a type is a reference
   * @return	is it a type that accept null value
   */
  public boolean isReference() {
    return true;
  }

  /**
   * Check if a type is a class type
   * @return	is it a subtype of ClassType ?
   */
  public boolean isClassType() {
    return true;
  }

  /**
   * Returns the class object associated with this type
   *
   * If this type was never checked (read from class files)
   * check it!
   *
   * @return the class object associated with this type
   */
  public CClass getCClass() {
    // !!! graf 000213
    // !!! should have been checked (see JFieldAccessExpression)
    assert clazz != BAC_CLASS;
    // !!! graf 000213
    if (clazz == null) {
      if (this == CStdType.Object) {
	throw new InconsistencyException("java.lang.Object is not in the classpath !!!");
      }
      clazz = CStdType.Object.getCClass();
    }

    return clazz;
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * check that type is valid
   * necessary to resolve String into java/lang/String
   * @param	context		the context (may be be null)
   * @exception UnpositionedError	this error will be positioned soon
   */
  public void checkType(CContext context) throws UnpositionedError {
  }

  // ----------------------------------------------------------------------
  // BODY CHECKING
  // ----------------------------------------------------------------------

  /**
   * Can this type be converted to the specified type by assignment conversion (JLS 5.2) ?
   * @param	dest		the destination type
   * @return	true iff the conversion is valid
   */
  public boolean isAssignableTo(CType dest) {
    return dest.isClassType() && !dest.isArrayType() &&
      getCClass().descendsFrom(dest.getCClass());
  }

  /**
   * Can this type be converted to the specified type by casting conversion (JLS 5.5) ?
   * @param	dest		the destination type
   * @return	true iff the conversion is valid
   */
  public boolean isCastableTo(CType dest) {
    // test for array first because array types are classes

    if (getCClass().isInterface()) {
      if (! dest.isClassType()) {
	return false;
      } else if (dest.getCClass().isInterface()) {
	// if T is an interface type and if T and S contain methods
	// with the same signature but different return types,
	// then a compile-time error occurs.
	//!!! graf 000512: FIXME: implement this test
	return true;
      } else if (! dest.getCClass().isFinal()) {
	return true;
      } else {
	return dest.getCClass().descendsFrom(getCClass());
      }
    } else {
      // this is a class type
      if (dest.isArrayType()) {
	return equals(CStdType.Object);
      } else if (! dest.isClassType()) {
	return false;
      } else if (dest.getCClass().isInterface()) {
	if (! getCClass().isFinal()) {
	  return true;
	} else {
	  return getCClass().descendsFrom(dest.getCClass());
	}
      } else {
	return getCClass().descendsFrom(dest.getCClass())
	  || dest.getCClass().descendsFrom(getCClass());
      }
    }
  }

  /**
   *
   */
  public boolean isCheckedException() {
   return !isAssignableTo(CStdType.RuntimeException) &&
      !isAssignableTo(CStdType.Error);
  }

  // ----------------------------------------------------------------------
  // INITIALIZERS METHODS
  // ----------------------------------------------------------------------

  public static void init(Compiler compiler) {
      // bft: removed 9/25 since there was a static initializer, too
      // allCClassType = new Hashtable(2000);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  public static final CClassType[]		EMPTY = new CClassType[0];

  private static Hashtable	allCClassType = new Hashtable(2000);
  private static final CClass	BAC_CLASS = new CBadClass("<NOT YET DEFINED>");

  private CClass		clazz;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CClassType other = new at.dms.kjc.CClassType();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CClassType other) {
  super.deepCloneInto(other);
  other.clazz = (at.dms.kjc.CClass)at.dms.kjc.AutoCloner.cloneToplevel(this.clazz);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

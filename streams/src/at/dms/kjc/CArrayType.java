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
 * $Id: CArrayType.java,v 1.9 2004-01-28 16:55:35 dmaze Exp $
 */

package at.dms.kjc;

import at.dms.compiler.UnpositionedError;
import at.dms.util.SimpleStringBuffer;
import at.dms.util.Utils;

/**
 * This class represents class type in the type structure
 */
public class CArrayType extends CClassType {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    private CArrayType() {} // for cloner only

  /**
   * Constructs an array type
   * @param	baseType	the base type of the array type
   * @param	arrayBound	the dimension of the array type
   */
  public CArrayType(CType baseType, int arrayBound) {
    super();

    type = TID_ARRAY;
    assert baseType != null;
    assert arrayBound > 0;

    if (baseType.isArrayType()) {
      this.arrayBound = arrayBound + ((CArrayType)baseType).arrayBound;
      this.baseType = ((CArrayType)baseType).baseType;
    } else {
      this.arrayBound = arrayBound;
      this.baseType = baseType;
    }
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Transforms this type to a string
   * @return	the ksm form of this type
   */
  public String toString() {
    String	res = baseType.toString();
    for (int i = 0; i < arrayBound; i++) {
      res += "*";
    }
    return res;
  }

  /**
   *
   */
  public String getQualifiedName() {
    return getSignature();
  }

  /**
   * Appends the VM signature of this type to the specified buffer.
   */
  public void appendSignature(SimpleStringBuffer buffer) {
    for (int i = 0; i < arrayBound; i++) {
      buffer.append('[');
    }
    baseType.appendSignature(buffer);
  }

  /**
   * @return	the size used in stack by value of this type
   */
  public int getSize() {
    return 1;
  }

  /**
   * Checks if a type is a reference type
   * @return	is it a type that accept null value ?
   */
  public boolean isReference() {
    return true;
  }

  /**
   * @return	is this type an array ?
   */
  public boolean isArrayType() {
    return true;
  }

  /**
   * @return	the base class of this type
   */
  public CType getBaseType() {
    assert baseType != null;
    assert !(baseType instanceof CArrayType);
    return baseType;
  }

  /**
   * Returns the type of the elements of an array of this type.
   */
  public CType getElementType() {
    assert baseType != null;
    assert !(baseType instanceof CArrayType);
    if (arrayBound == 1) {
      return baseType;
    } else {
      return new CArrayType(baseType, arrayBound - 1);
    }
  }

  /**
   * @return	the number of array bracket of this type
   */
  public int getArrayBound() {
    return arrayBound;
  }

  /**
   * @return	true if this type is valid
   */
  public boolean checked() {
    return baseType.checked();
  }

  /**
   * equals
   */
  public boolean equals(CType other) {
    if (!other.isArrayType()) {
      return false;
    } else {
      CArrayType arr = (CArrayType)other;

      return baseType.equals(arr.baseType) && arrayBound == arr.arrayBound;
    }
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * check that type is valid
   * necessary to resolve String into java/lang/String
   * @exception UnpositionedError	this error will be positioned soon
   */
  public void checkType(CContext context) throws UnpositionedError {
    if (!isChecked()) {
      setClass(CStdType.Object.getCClass());
      baseType.checkType(context);
    }
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
    if (dest.isArrayType()) {
      if (arrayBound == ((CArrayType)dest).arrayBound &&
	  baseType.isAssignableTo(((CArrayType)dest).baseType)) {
	// normal folks
	return true;
      } else {
	if (arrayBound < ((CArrayType)dest).arrayBound) {
	  // int[][] i = new int[]; ???
	  return false;
	}
	// May be the unusal case Object[] o = new String[][]...;
	return (((CArrayType)dest).baseType).equals(CStdType.Object);
      }
    } else {
      return dest.equals(CStdType.Object);
    }
  }

  /**
   * Can this type be converted to the specified type by casting conversion (JLS 5.5) ?
   * @param	dest		the destination type
   * @return	true iff the conversion is valid
   */
  public boolean isCastableTo(CType dest) {
    // test for array first because array types are classes

    if (dest.isArrayType()) {
      // optimizes recursive call with base types (indexed types)

      CType		destBase = ((CArrayType)dest).baseType;
      int		destBound = ((CArrayType)dest).arrayBound;

      if (arrayBound == destBound) {
	if (baseType.isPrimitive()) {
	  return baseType == destBase;
	} else {
	  return baseType.isCastableTo(destBase);
	}
      } else if (arrayBound < destBound) {
	return baseType.isCastableTo(new CArrayType(destBase, destBound - arrayBound));
      } else {
	// arrayBound > destBound
	return new CArrayType(baseType, arrayBound - destBound).isCastableTo(destBase);
      }
    } else if (dest.isClassType()) {
      if (dest.equals(CStdType.Object)) {
	// if T is a class type, then if T is not Object,
	// then a compile-time error occurs
	return true;
      } else if (dest.getCClass().getQualifiedName().equals(JAV_CLONEABLE)) {
	// if T is an interface type, then a compile-time error
	// occurs unless T is the interface type Cloneable
	return true;
      } else {
	return false;
      }
    } else {
      return false;
    }
  }


    /**
     *dims is only set as part of a channel declaration
     * so the input output type of a filter has dims set
     * but all other CArrayType's do not
     **/
    public JExpression[] getDims() {
	return dims;
    }

    public void setDims(JExpression[] d){
	dims = d;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CType		baseType;
  private int		arrayBound;
    private JExpression  dims[];

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CArrayType other = new at.dms.kjc.CArrayType();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CArrayType other) {
  super.deepCloneInto(other);
  other.baseType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.baseType);
  other.arrayBound = this.arrayBound;
  other.dims = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.dims);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

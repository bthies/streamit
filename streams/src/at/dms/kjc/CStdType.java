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
 * $Id: CStdType.java,v 1.5 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.Compiler;

/**
 * Root for type hierarchy
 */
public class CStdType extends at.dms.util.Utils implements Constants {

  // ----------------------------------------------------------------------
  // PRIMITIVE TYPES
  // ----------------------------------------------------------------------

  public static final CVoidType	Void = new CVoidType();
  public static final CNullType	Null = new CNullType();

  public static final CBooleanType Boolean = new CBooleanType();
  public static final CByteType Byte = new CByteType();
  public static final CCharType Char = new CCharType();
  public static final CDoubleType Double = new CDoubleType();
  public static final CFloatType Float = new CFloatType();
  public static final CIntType Integer = new CIntType();
  public static final CLongType	Long = new CLongType();
  public static final CShortType Short = new CShortType();
    public static final CBitType Bit = new CBitType();

  public static CClassType Object = CClassType.lookup(Constants.JAV_OBJECT);
  public static CClassType Class = CClassType.lookup(Constants.JAV_CLASS);
  public static CClassType String = CClassType.lookup(Constants.JAV_STRING);
  public static CClassType Throwable 
      = CClassType.lookup(Constants.JAV_THROWABLE);
  public static CClassType Exception
      = CClassType.lookup(Constants.JAV_EXCEPTION);
  public static CClassType Error = CClassType.lookup(Constants.JAV_ERROR);
  public static CClassType RuntimeException
       = CClassType.lookup(Constants.JAV_RUNTIME_EXCEPTION);

  // ----------------------------------------------------------------------
  // INITIALIZERS
  // ----------------------------------------------------------------------

  /**
   * Initialize all constants
   */
  public static void init(Compiler compiler) {
    CClassType.init(compiler);
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CStdType other = new at.dms.kjc.CStdType();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CStdType other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

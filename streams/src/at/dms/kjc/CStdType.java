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
 * $Id: CStdType.java,v 1.1 2001-08-30 16:32:51 thies Exp $
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

  public static CVoidType	Void;
  public static CNullType	Null;

  public static CBooleanType	Boolean;
  public static CByteType	Byte;
  public static CCharType	Char;
  public static CDoubleType	Double;
  public static CFloatType	Float;
  public static CIntType	Integer;
  public static CLongType	Long;
  public static CShortType	Short;

  public static CClassType	Object;
  public static CClassType	Class;
  public static CClassType	String;
  public static CClassType	Throwable;
  public static CClassType	Exception;
  public static CClassType	Error;
  public static CClassType	RuntimeException;

  // ----------------------------------------------------------------------
  // INITIALIZERS
  // ----------------------------------------------------------------------

  /**
   * Initialize all constants
   */
  public static void init(Compiler compiler) {
    // one time is enough
    if (Void == null) {
      Void = new CVoidType();
      Null = new CNullType();

      Boolean = new CBooleanType();
      Byte = new CByteType();
      Char = new CCharType();
      Double = new CDoubleType();
      Float = new CFloatType();
      Integer = new CIntType();
      Long = new CLongType();
      Short = new CShortType();
    }

    CClassType.init(compiler);

    Object = CClassType.lookup(Constants.JAV_OBJECT);
    Class = CClassType.lookup(Constants.JAV_CLASS);
    String = CClassType.lookup(Constants.JAV_STRING);
    Throwable = CClassType.lookup(Constants.JAV_THROWABLE);
    Exception = CClassType.lookup(Constants.JAV_EXCEPTION);
    Error = CClassType.lookup(Constants.JAV_ERROR);
    RuntimeException = CClassType.lookup(Constants.JAV_RUNTIME_EXCEPTION);
  }
}

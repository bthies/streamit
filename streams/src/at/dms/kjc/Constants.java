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
 * $Id: Constants.java,v 1.1 2001-08-30 16:32:51 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;

/**
 * Defines all constants shared by compiler
 */
public interface Constants extends at.dms.classfile.Constants {

  // ----------------------------------------------------------------------
  // TYPE IDENTIFIER
  // ----------------------------------------------------------------------

  int TID_VOID			= 1;
  int TID_BYTE			= 2;
  int TID_SHORT			= 3;
  int TID_CHAR			= 4;
  int TID_INT			= 5;
  int TID_LONG			= 6;
  int TID_FLOAT			= 7;
  int TID_DOUBLE		= 8;
  int TID_CLASS			= 9;
  int TID_ARRAY			= 10;
  int TID_BOOLEAN		= 11;


  // ----------------------------------------------------------------------
  // COMPILER FLAGS
  // ----------------------------------------------------------------------

  int CMP_VERSION		= 0xC0DE01;

  // ----------------------------------------------------------------------
  // JAVA CONSTANTS
  // ----------------------------------------------------------------------

  String JAV_CLASS		= "java/lang/Class";
  String JAV_CLONEABLE		= "java/lang/Cloneable";
  String JAV_ERROR		= "java/lang/Error";
  String JAV_EXCEPTION		= "java/lang/Exception";
  String JAV_OBJECT		= "java/lang/Object";
  String JAV_RUNTIME_EXCEPTION	= "java/lang/RuntimeException";
  String JAV_STRING		= "java/lang/String";
  String JAV_STRINGBUFFER	= "java/lang/StringBuffer";
  String JAV_THROWABLE		= "java/lang/Throwable";

  String JAV_CONSTRUCTOR	= "<init>";
  String JAV_INIT		= "Block$";
  String JAV_STATIC_INIT	= "<clinit>";

  String JAV_THIS		= "this";
  String JAV_OUTER_THIS		= "this$0";

  String JAV_NAME_SEPARATOR	= "/";
  String JAV_RUNTIME		= "java/lang";
  String JAV_CLONE		= "clone";
  String JAV_LENGTH		= "length";

  // ----------------------------------------------------------------------
  // BINARY OPERATORS
  // ----------------------------------------------------------------------

  int OPE_SIMPLE		= 0;
  int OPE_PLUS			= 1;
  int OPE_MINUS			= 2;
  int OPE_STAR			= 3;
  int OPE_SLASH			= 4;
  int OPE_PERCENT		= 5;
  int OPE_SR			= 6;
  int OPE_BSR			= 7;
  int OPE_SL			= 8;
  int OPE_BAND			= 9;
  int OPE_BXOR			= 10;
  int OPE_BOR			= 11;
  int OPE_BNOT			= 12;
  int OPE_LNOT			= 13;
  int OPE_LT			= 14;
  int OPE_LE			= 15;
  int OPE_GT			= 16;
  int OPE_GE			= 17;
  int OPE_EQ			= 18;
  int OPE_NE			= 19;

  // ----------------------------------------------------------------------
  // UNARY OPERATORS
  // ----------------------------------------------------------------------

  int OPE_PREINC		= 20;
  int OPE_PREDEC		= 21;
  int OPE_POSTINC		= 22;
  int OPE_POSTDEC		= 23;

  // ----------------------------------------------------------------------
  // UTILITIES EMPTY COLLECTION
  // ----------------------------------------------------------------------

  Vector	VECTOR_EMPTY	= new Vector();
}

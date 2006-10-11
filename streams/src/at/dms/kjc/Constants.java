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
 * $Id: Constants.java,v 1.5 2006-10-11 17:49:42 dimock Exp $
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

    /** void X; for {@link CType}. See also {@link CStdType#Void}, {@link CVoidType} */
    int TID_VOID            = 1;
    /** byte X; for {@link CType}. See also {@link CStdType#Byte}, {@link CByteType} */
    int TID_BYTE            = 2;
    /** short X; for {@link CType}. See also {@link CStdType#Short}, {@link CShortType} */
    int TID_SHORT           = 3;
    /** char X; for {@link CType}. See also {@link CStdType#Char}, {@link CCharType} */
    int TID_CHAR            = 4;
    /** int X; for {@link CType}. See also {@link CStdType#Int}, {@link CIntType} */
    int TID_INT             = 5;
    /** long int X; for {@link CType}. See also {@link CStdType#Long}, {@link CLongType} */
    int TID_LONG            = 6;
    /** float X; for {@link CType}. See also {@link CStdType#Float}, {@link CFloatType} */
    int TID_FLOAT           = 7;
    /** double X; for {@link CType}. See also {@link CStdType#Double}, {@link CDoubleType} */
    int TID_DOUBLE          = 8;
    /** Java classes for {@link CType}. @see CClassType */
    int TID_CLASS           = 9;
    /** array types for {@link CType}. @see CArrayType */
    int TID_ARRAY           = 10;
    /** boolean X; for {@link CType}. See also {@link CStdType#Boolean}, {@link CBooleanType} */
    int TID_BOOLEAN         = 11;
    /** bit X; for {@link CType}. See also {@link CStdType#Bit}, {@link CBitType} */
    int TID_BIT             = 12;
    /** vector types for {@link CType}. @see CVectorType */
    int TID_VECTOR          = 13;

    // ----------------------------------------------------------------------
    // COMPILER FLAGS
    // ----------------------------------------------------------------------

    int CMP_VERSION     = 0xC0DE01;

    // ----------------------------------------------------------------------
    // JAVA CONSTANTS
    // ----------------------------------------------------------------------

    String JAV_CLASS        = "java/lang/Class";
    String JAV_CLONEABLE    = "java/lang/Cloneable";
    String JAV_ERROR        = "java/lang/Error";
    String JAV_EXCEPTION    = "java/lang/Exception";
    String JAV_OBJECT       = "java/lang/Object";
    String JAV_RUNTIME_EXCEPTION    = "java/lang/RuntimeException";
    String JAV_STRING       = "java/lang/String";
    String JAV_STRINGBUFFER = "java/lang/StringBuffer";
    String JAV_THROWABLE    = "java/lang/Throwable";

    String JAV_CONSTRUCTOR  = "<init>";
    String JAV_INIT         = "Block$";
    String JAV_STATIC_INIT  = "<clinit>";

    String JAV_THIS         = "this";
    String JAV_OUTER_THIS   = "this$0";

    String JAV_NAME_SEPARATOR   = "/";
    String JAV_RUNTIME      = "java/lang";
    String JAV_CLONE        = "clone";
    String JAV_LENGTH       = "length";

    // ----------------------------------------------------------------------
    // BINARY OPERATORS
    // ----------------------------------------------------------------------
    /** ?? not used in code */
    int OPE_SIMPLE      = 0;
    /** X += ... for {@link JCompoundAssignmentExpression} */
    int OPE_PLUS        = 1;
    /** X -= ... for {@link JCompoundAssignmentExpression} */
    int OPE_MINUS       = 2;
    /** X *= ... for {@link JCompoundAssignmentExpression} */
    int OPE_STAR        = 3;
    /** X /= ... for {@link JCompoundAssignmentExpression} */
    int OPE_SLASH       = 4;
    /** X %= ... for {@link JCompoundAssignmentExpression} */
    int OPE_PERCENT     = 5;
    /** X >>= ... for {@link JCompoundAssignmentExpression} */
    int OPE_SR          = 6;
    /** X >>>= ... for {@link JCompoundAssignmentExpression} */
    int OPE_BSR         = 7;
    /** X <<= ... for {@link JCompoundAssignmentExpression} */
    int OPE_SL          = 8;
    /** X &amp;= ... for {@link JCompoundAssignmentExpression} */
    int OPE_BAND        = 9;
    /** X ^= ... for {@link JCompoundAssignmentExpression} */
    int OPE_BXOR        = 10;
    /** X |= ... for {@link JCompoundAssignmentExpression} */
    int OPE_BOR         = 11;
    /** ?? KJC legacy not used in code */
    int OPE_BNOT        = 12;
    /** ?? KJC legacy not used in code */
    int OPE_LNOT        = 13;
    /** X < Y for {@link JRelationalExpression} */
    int OPE_LT          = 14;
    /** X <= Y  for {@link JRelationalExpression} */
    int OPE_LE          = 15;
    /** X > Y  for {@link JRelationalExpression} */
    int OPE_GT          = 16;
    /** X >= Y  for {@link JRelationalExpression} */
    int OPE_GE          = 17;
    /** X == Y for {@link JRelationalExpression} obsolete? @see JEqualityExpression */
    int OPE_EQ          = 18;
    /** X != Y for {@link JRelationalExpression} obsolete? @see JEqualityExpression */
    int OPE_NE          = 19;

    // ----------------------------------------------------------------------
    // UNARY OPERATORS
    // ----------------------------------------------------------------------

    /** Unary operator ++X for {@link JPreFixExpression} */
    int OPE_PREINC      = 20;
    /** Unary operator --X for {@link JPreFixExpression} */
    int OPE_PREDEC      = 21;
    /** Unary operator X++ for {@link JPostFixExpression} */
    int OPE_POSTINC     = 22;
    /** Unary operator X-- for {@link JPostFixExpression} */
    int OPE_POSTDEC     = 23;

    // ----------------------------------------------------------------------
    // UTILITIES EMPTY COLLECTION
    // ----------------------------------------------------------------------

    Vector  VECTOR_EMPTY    = new Vector();
}

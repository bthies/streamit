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
 * $Id: CBinaryField.java,v 1.3 2003-05-28 05:58:41 thies Exp $
 */

package at.dms.kjc;

import at.dms.classfile.FieldInfo;
import at.dms.compiler.TokenReference;
import at.dms.util.InconsistencyException;

/**
 * This class represents loaded (compiled) class fields.
 */
public class CBinaryField extends CField {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CBinaryField() {} // for cloner only

  /**
   * Constructs a field export
   * @param	owner		the owner of this field
   * @param	fieldInfo	a field info from a class file
   */
  public CBinaryField(CClass owner, FieldInfo fieldInfo) {
    super(owner,
	  fieldInfo.getModifiers(),
	  fieldInfo.getName().intern(),
	  CType.parseSignature(fieldInfo.getSignature()),
	  fieldInfo.isDeprecated());

    if (isFinal() && isStatic()) {
      Object		value = fieldInfo.getConstantValue();

      if (value != null) {
	setValue(createLiteral(getType(), value));
      }
    }
  }

  /*
   * Returns a literal representing the constant value.
   */
  private static JLiteral createLiteral(CType type, Object value) {
    switch (type.getTypeID()) {
    case TID_BYTE:
      return new JByteLiteral(TokenReference.NO_REF, (byte)((Integer)value).intValue());
    case TID_SHORT:
      return new JShortLiteral(TokenReference.NO_REF, (short)((Integer)value).intValue());
    case TID_CHAR:
      return new JCharLiteral(TokenReference.NO_REF, (char)((Integer)value).intValue());
    case TID_INT:
      return new JIntLiteral(TokenReference.NO_REF, ((Integer)value).intValue());
    case TID_LONG:
      return new JLongLiteral(TokenReference.NO_REF, ((Long)value).longValue());
    case TID_FLOAT:
      return new JFloatLiteral(TokenReference.NO_REF, ((Float)value).floatValue());
    case TID_DOUBLE:
      return new JDoubleLiteral(TokenReference.NO_REF, ((Double)value).doubleValue());
    case TID_CLASS:
      if (type != CStdType.String) {
	throw new InconsistencyException("bad type " + type + "(literal: " + value.getClass() + ")");
      }
      return new JStringLiteral(TokenReference.NO_REF, (String)value);
    case TID_BOOLEAN:
      return new JBooleanLiteral(TokenReference.NO_REF, ((Integer)value).intValue() != 0);
    default:
      throw new InconsistencyException("bad type " + type + "(literal: " + value.getClass() + ")");
    }
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CBinaryField other = new at.dms.kjc.CBinaryField();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CBinaryField other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

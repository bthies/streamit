/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: UnresolvedConstant.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;

import at.dms.util.InconsistencyException;

/**
 * Wrap an Unresolved constant reference with this CPE.
 */
public class UnresolvedConstant extends PooledConstant {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new unresolved pooled constant.
   *
   * @param	tag		the constant type
   * @param	index1		the first index
   * @param	index2		the second index
   */
  public UnresolvedConstant(byte tag, int index1, int index2) {
    this.tag = tag;
    this.index1 = index1;
    this.index2 = index2;
  }

  // --------------------------------------------------------------------
  // RESOLVE
  // --------------------------------------------------------------------

  /**
   * Insert or check location of constant value on constant pool
   *
   * @param	cp		the constant pool for this class
   *
   * @exception	ClassFileFormatException	attempt to
   *					write a bad classfile info
   */
  /*package*/ PooledConstant resolveConstant(PooledConstant[] constants)
    throws ClassFileFormatException
  {
    switch (tag) {
    case CST_CLASS:
      return new ClassConstant((AsciiConstant)constants[index1]);

    case CST_STRING:
      return new StringConstant((AsciiConstant)constants[index1]);

    case CST_FIELD:
      return new FieldRefConstant((ClassConstant)constants[index1],
				  (NameAndTypeConstant)constants[index2]);
    case CST_METHOD:
      return new MethodRefConstant((ClassConstant)constants[index1],
				   (NameAndTypeConstant)constants[index2]);

    case CST_INTERFACEMETHOD:
      return new InterfaceConstant((ClassConstant)constants[index1],
				   (NameAndTypeConstant)constants[index2]);

    case CST_NAMEANDTYPE:
      return new NameAndTypeConstant((AsciiConstant)constants[index1],
					(AsciiConstant)constants[index2]);

    default:
      throw new ClassFileFormatException("Bad constant tag: " + tag);
    }
  }

  // --------------------------------------------------------------------
  // DUMMY METHODS
  // --------------------------------------------------------------------

  /*package*/ Object getLiteral() {
    throw new InconsistencyException("method should not be called");
  }

  /**
   * hashCode (a fast comparison)
   * CONVENTION: return XXXXXXXXXXXX << 4 + Y
   * with Y = ident of the type of the pooled constant
   */
  public final int hashCode() {
    throw new InconsistencyException("method should not be called");
  }

  /**
   * equals (an exact comparison)
   * ASSERT: this.hashCode == o.hashCode ===> cast
   */
  public final boolean equals(Object o) {
    throw new InconsistencyException("method should not be called");
  }

  /**
   * Check location of constant value on constant pool
   *
   * @param	pc		the already in pooled constant
   * ASSERT pc.getClass() == this.getClass()
   */
  /*package*/ final void resolveConstants(PooledConstant pc) {
    throw new InconsistencyException("method should not be called");
  }

  /**
   * Insert or check location of constant value on constant pool
   *
   * @param	cp		the constant pool for this class
   */
  /*package*/ void resolveConstants(ConstantPool cp)  {
    throw new InconsistencyException("method should not be called");
  }

  /**
   * Write this class into the the file (out) getting data position from
   * the constant pool
   *
   * @param	cp		the constant pool that contain all data
   * @param	out		the file where to write this object info
   */
  /*package*/ void write(ConstantPool cp, DataOutput out) {
    throw new InconsistencyException("method should not be called");
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private byte			tag;
  private int			index1;
  private int			index2;
}

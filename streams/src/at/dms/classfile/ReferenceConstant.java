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
 * $Id: ReferenceConstant.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.Utils;

/**
 * VMS 4.4.2 : Reference Constants.
 *
 * This class implements field/method/interface method reference constants.
 */
public abstract class ReferenceConstant extends PooledConstant {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new reference constant.
   *
   * @param	tag		the constant type tag
   * @param	name		the qualified name of the referenced object
   * @param	type		the signature of the referenced object
   */
  public ReferenceConstant(byte tag, String name, String type) {
    String[]	split = Utils.splitQualifiedName(name);

    this.tag = tag;
    this.clazz = new ClassConstant(split[0]);
    this.nametype = new NameAndTypeConstant(split[1], type);
  }

  /**
   * Constructs a new reference constant.
   *
   * @param	tag		the constant type tag
   * @param	owner		the qualified name of the class containing the reference
   * @param	name		the simple name of the referenced object
   * @param	type		the signature of the referenced object
   */
  public ReferenceConstant(byte tag, String owner, String name, String type) {
    this.tag = tag;
    this.clazz = new ClassConstant(owner);
    this.nametype = new NameAndTypeConstant(name, type);
  }

  /**
   * Constructs a reference constant.
   *
   * @param	tag		the constant type tag
   * @param	clazz		the class that defines the referenced object
   * @param	nametype	the simple name and signature of the referenced object
   */
  public ReferenceConstant(byte tag, ClassConstant clazz, NameAndTypeConstant nametype) {
    this.tag = tag;
    this.clazz = clazz;
    this.nametype = nametype;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns the associated literal: this constant type has none
   */
  /*package*/ Object getLiteral() {
    return null;
  }

  /**
   * Returns the name of this constant
   */
  public String getName() {
    return clazz.getName() + "/" + nametype.getName();
  }

  /**
   * Returns the name of this constant
   */
  public String getClassName() {
    return clazz.getName();
  }

  /**
   * Returns the name of this constant
   */
  public String getTypeName() {
    return nametype.getName();
  }

  /**
   * Returns the name of this constant
   */
  public void setTypeName(String name) {
    nametype.setName(name);
  }

  /**
   * Returns the name of this constant
   */
  public String getType() {
    return nametype.getType();
  }

  // --------------------------------------------------------------------
  // POOLING
  // --------------------------------------------------------------------

  /**
   * hashCode (a fast comparison)
   * CONVENTION: return XXXXXXXXXXXX << 4 + Y
   * with Y = ident of the type of the pooled constant
   */
  public final int hashCode() {
    // ADD is not perfect, but...
    // we know already that name is an ascii: &
    return ((clazz.hashCode() + nametype.hashCode()) * tag & 0xFFFFFFF0) + POO_REF_CONSTANT;
  }

  /**
   * equals (an exact comparison)
   * ASSERT: this.hashCode == o.hashCode ===> cast
   */
  public final boolean equals(Object o) {
    return (o instanceof ReferenceConstant) &&
      ((ReferenceConstant)o).clazz.equals(clazz) &&
      ((ReferenceConstant)o).nametype.equals(nametype);
  }

  // --------------------------------------------------------------------
  // WRITE
  // --------------------------------------------------------------------

  /**
   * Insert or check location of constant value on constant pool
   *
   * @param	cp		the constant pool for this class
   */
  /*package*/ void resolveConstants(ConstantPool cp) {
    cp.addItem(clazz);
    cp.addItem(nametype);
  }

  /**
   * Check location of constant value on constant pool
   *
   * @param	pc		the already in pooled constant
   * ASSERT pc.getClass() == this.getClass()
   */
  /*package*/ final void resolveConstants(PooledConstant pc) {
    setIndex(pc.getIndex());
    clazz.setIndex(((ReferenceConstant)pc).clazz.getIndex());
    nametype.setIndex(((ReferenceConstant)pc).nametype.getIndex());
  }

  /**
   * Write this class into the the file (out) getting data position from
   * the constant pool
   *
   * @param	cp		the constant pool that contain all data
   * @param	out		the file where to write this object info
   *
   * @exception	java.io.IOException	an io problem has occured
   */
  /*package*/ void write(ConstantPool cp, DataOutput out) throws IOException {
    out.writeByte(tag);
    out.writeShort(clazz.getIndex());
    out.writeShort(nametype.getIndex());
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private byte				tag;
  private ClassConstant			clazz;
  private NameAndTypeConstant		nametype;
}

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
 * $Id: ClassConstant.java,v 1.1 2001-08-30 16:32:26 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * This is used to create a Class constant pool item
 *
 */
public class ClassConstant extends PooledConstant {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new class constant.
   *
   * @param	name		the qualified name of the class
   */
  public ClassConstant(String name) {
    this(new AsciiConstant(name));
  }

  /**
   * Constructs a new class constant.
   *
   * @param	name		the qualified name of the class
   */
  public ClassConstant(AsciiConstant name) {
    this.name = name;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  public String getName() {
    return name.getValue();
  }

  /**
   * Returns the associated literal: this constant type has none
   */
  /*package*/ Object getLiteral() {
    throw new InconsistencyException("attempt to read literal in a class constant");
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
    // we know already that name is an ascii: &
    return (name.hashCode() & 0xFFFFFFF0) + POO_CLASS_CONSTANT;
  }

  /**
   * equals (an exact comparison)
   * ASSERT: this.hashCode == o.hashCode ===> cast
   */
  public final boolean equals(Object o) {
    return (o instanceof ClassConstant) &&
      ((ClassConstant)o).name.equals(name);
  }

  // --------------------------------------------------------------------
  // WRITE
  // --------------------------------------------------------------------

  /**
   * Check location of constant value on constant pool
   *
   * @param	pc		the already in pooled constant
   * ASSERT pc.getClass() == this.getClass()
   */
  /*package*/ final void resolveConstants(PooledConstant pc) {
    setIndex(pc.getIndex());
    name.setIndex(((ClassConstant)pc).name.getIndex());
  }

  /**
   * Insert or check location of constant value on constant pool
   *
   * @param	cp		the constant pool for this class
   */
  /*package*/ void resolveConstants(ConstantPool cp) {
    cp.addItem(name);
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
    out.writeByte(CST_CLASS);
    out.writeShort(name.getIndex());
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private AsciiConstant		name;
}

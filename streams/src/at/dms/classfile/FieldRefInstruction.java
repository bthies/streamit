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
 * $Id: FieldRefInstruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * Instructions thar refers to class fields
 * opc_getstatic, opc_putstatic, opc_getfield, opc_putfield
 */
public class FieldRefInstruction extends Instruction {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new field reference instruction
   *
   * @param	opcode		the opcode of the instruction
   * @param	name		the qualified name of the referenced field
   * @param	type		the signature of the referenced field
   */
  public FieldRefInstruction(int opcode, String name, String type) {
    super(opcode);

    this.field = new FieldRefConstant(name, type);
  }

  /**
   * Constructs a new field reference instruction
   *
   * @param	opcode		the opcode of the instruction
   * @param	owner		the qualified name of the class containing the field
   * @param	name		the simple name of the referenced field
   * @param	type		the signature of the referenced field
   */
  public FieldRefInstruction(int opcode, String owner, String name, String type) {
    super(opcode);

    this.field = new FieldRefConstant(owner, name, type);
  }

  /**
   * Constructs a new field reference instruction from a class file
   *
   * @param	opcode		the opcode of the instruction
   * @param	field		the field reference (as pooled constant)
   */
  public FieldRefInstruction(int opcode, FieldRefConstant field) {
    super(opcode);

    this.field = field;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns true iff control flow can reach the next instruction
   * in textual order.
   */
  public boolean canComplete() {
    return true;
  }

  /**
   * Insert or check location of constant value on constant pool
   *
   * @param	cp		the constant pool for this class
   */
  /*package*/ void resolveConstants(ConstantPool cp) {
    cp.addItem(field);
  }

  /**
   * Returns the number of bytes used by the the instruction in the code array.
   */
  /*package*/ int getSize() {
    return 1 + 2;
  }

  /**
   * Returns the field constant on which this expression refers itself
   */
  public FieldRefConstant getFieldRefConstant() {
    return field;
  }

  // --------------------------------------------------------------------
  // CHECK CONTROL FLOW
  // --------------------------------------------------------------------

  /**
   * Returns the type pushed on the stack
   */
  public byte getReturnType() {
    switch (field.getType().charAt(0)) {
    case 'Z':
    case 'B':
    case 'C':
    case 'S':
    case 'I':
      return TYP_INT;
    case 'F':
      return TYP_FLOAT;
    case 'L':
    case '[':
      return TYP_REFERENCE;
    case 'D':
      return TYP_DOUBLE;
    case 'J':
      return TYP_LONG;

    default:
      throw new InconsistencyException("invalid signature " + field.getType());
    }
  }

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public int getPushedOnStack() {
    int		stack = getStack();

    switch (getOpcode()) {
    case opc_getstatic:
      return stack;
    case opc_putstatic:
      return 0;
    case opc_getfield:
      return stack + 1;
    case opc_putfield:
      return 0;
    default:
      return 0;
    }
  }

  /**
   * Return the amount of stack (positive or negative) used by this instruction
   */
  public int getStack() {
    int		used;

    switch (field.getType().charAt(0)) {
    case 'Z':
    case 'B':
    case 'C':
    case 'S':
    case 'F':
    case 'I':
    case 'L':
    case '[':
      used = 1;
      break;

    case 'D':
    case 'J':
      used = 2;
      break;

    default:
      throw new InconsistencyException("invalid signature " + field.getType());
    }

    switch (getOpcode()) {
    case opc_getstatic:
      return used;
    case opc_putstatic:
      return -used;
    case opc_getfield:
      return used - 1;
    case opc_putfield:
      return -used - 1;
    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  // --------------------------------------------------------------------
  // WRITE
  // --------------------------------------------------------------------

  /**
   * Write this instruction into a file
   *
   * @param	cp		the constant pool that contain all data
   * @param	out		the file where to write this field info
   *
   * @exception	java.io.IOException	an io problem has occured
   */
  /*package*/ void write(ConstantPool cp, DataOutput out) throws IOException {
    out.writeByte((byte)getOpcode());
    out.writeShort(field.getIndex());
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private FieldRefConstant	field;
}

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
 * $Id: InvokeinterfaceInstruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * Some instructions are perniticky enough that its simpler
 * to write them separately instead of smushing them with
 * all the rest. the invokeinterface instruction is one of them.
 */
public class InvokeinterfaceInstruction extends Instruction {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new invokeinterface instruction
   *
   * @param	name		the qualified name of the method
   * @param	type		the method signature
   * @param	nargs		number of arguments
   */
  public InvokeinterfaceInstruction(String name, String type, int nargs) {
    super(opc_invokeinterface);

    this.method = new InterfaceConstant(name, type);
    this.nargs = nargs;
  }

  /**
   * Constructs a new invokeinterface instruction
   *
   * @param	name		the qualified name of the method
   * @param	type		the method signature
   * @param	nargs		number of arguments
   */
  public InvokeinterfaceInstruction(String owner, String name, String type, int nargs) {
    super(opc_invokeinterface);

    this.method = new InterfaceConstant(owner, name, type);
    this.nargs = nargs;
  }

  /**
   * Constructs a new invokeinterface instruction from a class file
   *
   * @param	method		the method reference (as pooled constant)
   * @param	nargs		number of arguments
   */
  public InvokeinterfaceInstruction(InterfaceConstant method, int nargs) {
    super(opc_invokeinterface);

    this.method = method;
    this.nargs = nargs;
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
   * Returns the number of bytes used by the the instruction in the code array.
   */
  /*package*/ int getSize() {
    return 1 + 4;
  }

  /**
   * Returns the interface constant value
   */
  public InterfaceConstant getInterfaceConstant() {
    return method;
  }

  /**
   * Returns the number of arguments
   */
  public int getNbArgs() {
    return nargs;
  }

  // --------------------------------------------------------------------
  // CHECK CONTROL FLOW
  // --------------------------------------------------------------------

  /**
   * Returns the type pushed on the stack
   */
  public byte getReturnType() {
    String	type = method.getType();

    switch (type.charAt(type.indexOf(")") + 1)) {
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
      throw new InconsistencyException("invalid signature " + type);
    }
  }

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public int getPushedOnStack() {
    String	type = method.getType();

    switch (type.charAt(type.indexOf(")") + 1)) {
    case 'V':
      return 0;
    case 'Z':
    case 'B':
    case 'C':
    case 'S':
    case 'F':
    case 'I':
    case 'L':
    case '[':
      return 1;
    case 'D':
    case 'J':
      return 2;
    default:
      throw new InconsistencyException("invalid signature x" + type);
    }
  }

  /**
   * Return the amount of stack (positive or negative) used by this instruction
   */
  public int getStack() {
    String	type = method.getType();
    int		used = 0;

    if (type.charAt(0) != '(') {
      throw new InconsistencyException("invalid signature " + type);
    }

    int		pos = 1;

  _method_parameters_:
    for (;;) {
      switch (type.charAt(pos++)) {
      case ')':
	break _method_parameters_;

      case '[':
	while (type.charAt(pos) == '[') {
	  pos += 1;
	}
	if (type.charAt(pos) == 'L') {
	  while (type.charAt(pos) != ';') {
	    pos += 1;
	  }
	}
	pos += 1;

	used -= 1;
	break;

      case 'L':
	while (type.charAt(pos) != ';') {
	  pos += 1;
	}
	pos += 1;

	used -= 1;
	break;

      case 'Z':
      case 'B':
      case 'C':
      case 'S':
      case 'F':
      case 'I':
	used -= 1;
	break;

      case 'D':
      case 'J':
	used -= 2;
	break;

      default:
	throw new InconsistencyException("invalid signature " + type);
      }
    }

    switch (type.charAt(pos)) {
    case 'V':
      break;

    case 'Z':
    case 'B':
    case 'C':
    case 'S':
    case 'F':
    case 'I':
    case 'L':
    case '[':
      used += 1;
      break;

    case 'D':
    case 'J':
      used += 2;
      break;

    default:
      throw new InconsistencyException("invalid signature " + type);
    }

    // pop object reference
    return used - 1;
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
    cp.addItem(method);
  }

  /**
   * Write this instruction into a file
   *
   * @param	cp		the constant pool that contain all data
   * @param	out		the file where to write this object info
   *
   * @exception	java.io.IOException	an io problem has occured
   */
  /*package*/ void write(ConstantPool cp, DataOutput out) throws IOException {
    out.writeByte((byte)getOpcode());

    out.writeShort(method.getIndex());
    out.writeByte((byte)(nargs & 0xFF));
    out.writeByte(0);
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private InterfaceConstant		method;
  private int				nargs;
}

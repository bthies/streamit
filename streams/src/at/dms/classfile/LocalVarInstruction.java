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
 * $Id: LocalVarInstruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * This class represents instructions that take a local variable as argument.
 *
 * Shortcuts (single byte instructions) are created for the opcodes:
 * iload, fload, aload, lload, dload, istore, fstore, astore, lstore, dstore
 * if the variable index is 0, 1, 2 or 3.
 *
 * An extra wide prefix is automatically added for these
 * instructions if the numeric argument is larger than 255.
 */
public class LocalVarInstruction extends Instruction {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new instruction that takes a local variable as argument.
   *
   * @param	opcode		the opcode of the instruction
   * @param     index		the index of the local variable
   */
  public LocalVarInstruction(int opcode, int index) {
    super(opcode);

    this.index = index;

    if (index > 255) {
      width = 4;		// wide + opcode + index(short)
    } else if (index > 3) {
      width = 2;		// opcode + index(byte)
    } else {
      // for each of the following instructions <inst> with opcode <c>, the
      // corresponding shortcut instructions inst_<n> have opcode <c> + 1 + <n>.
      switch (opcode) {
      case opc_iload:
	setOpcode(opc_iload_0 + index);
	width = 1;		// opcode
	break;

      case opc_fload:
	setOpcode(opc_fload_0 + index);
	width = 1;		// opcode
	break;

      case opc_aload:
	setOpcode(opc_aload_0 + index);
	width = 1;		// opcode
	break;

      case opc_lload:
	setOpcode(opc_lload_0 + index);
	width = 1;		// opcode
	break;

      case opc_dload:
	setOpcode(opc_dload_0 + index);
	width = 1;		// opcode
	break;

      case opc_istore:
	setOpcode(opc_istore_0 + index);
	width = 1;		// opcode
	break;

      case opc_fstore:
	setOpcode(opc_fstore_0 + index);
	width = 1;		// opcode
	break;

      case opc_astore:
	setOpcode(opc_astore_0 + index);
	width = 1;		// opcode
	break;

      case opc_lstore:
	setOpcode(opc_lstore_0 + index);
	width = 1;		// opcode
	break;

      case opc_dstore:
	setOpcode(opc_dstore_0 + index);
	width = 1;		// opcode
	break;

      default:
	width = 2;		// opcode + index(byte)
      }
    }
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns true iff control flow can reach the next instruction
   * in textual order.
   */
  public boolean canComplete() {
    return getOpcode() != opc_ret;
  }

  /**
   * Returns the number of bytes used by the the instruction in the code array.
   */
  /*package*/ int getSize() {
    return width;
  }

  /**
   * Returns the position of the variable in the local var set
   */
  public int getIndex() {
    return index;
  }

  /**
   * Returns the type pushed on the stack
   */
  public byte getReturnType() {
    return getOperandType();
  }

  /**
   * Returns the type of the operand.
   */
  public byte getOperandType() {
    switch (getOpcode()) {
    case opc_aload:
    case opc_aload_0:
    case opc_aload_1:
    case opc_aload_2:
    case opc_aload_3:
    case opc_astore:
    case opc_astore_0:
    case opc_astore_1:
    case opc_astore_2:
    case opc_astore_3:
      return TYP_REFERENCE;

    case opc_dload:
    case opc_dload_0:
    case opc_dload_1:
    case opc_dload_2:
    case opc_dload_3:
    case opc_dstore:
    case opc_dstore_0:
    case opc_dstore_1:
    case opc_dstore_2:
    case opc_dstore_3:
      return TYP_DOUBLE;

    case opc_fload:
    case opc_fload_0:
    case opc_fload_1:
    case opc_fload_2:
    case opc_fload_3:
    case opc_fstore:
    case opc_fstore_0:
    case opc_fstore_1:
    case opc_fstore_2:
    case opc_fstore_3:
      return TYP_FLOAT;

    case opc_iload:
    case opc_iload_0:
    case opc_iload_1:
    case opc_iload_2:
    case opc_iload_3:
    case opc_istore:
    case opc_istore_0:
    case opc_istore_1:
    case opc_istore_2:
    case opc_istore_3:
      return TYP_INT;

    case opc_lload:
    case opc_lload_0:
    case opc_lload_1:
    case opc_lload_2:
    case opc_lload_3:
    case opc_lstore:
    case opc_lstore_0:
    case opc_lstore_1:
    case opc_lstore_2:
    case opc_lstore_3:
      return TYP_LONG;

    case opc_ret:
      return TYP_ADDRESS;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  /**
   * Returns the operation kind.
   */
  public byte getOperationKind() {
    switch (getOpcode()) {
    case opc_aload:
    case opc_aload_0:
    case opc_aload_1:
    case opc_aload_2:
    case opc_aload_3:
    case opc_dload:
    case opc_dload_0:
    case opc_dload_1:
    case opc_dload_2:
    case opc_dload_3:
    case opc_fload:
    case opc_fload_0:
    case opc_fload_1:
    case opc_fload_2:
    case opc_fload_3:
    case opc_iload:
    case opc_iload_0:
    case opc_iload_1:
    case opc_iload_2:
    case opc_iload_3:
    case opc_lload:
    case opc_lload_0:
    case opc_lload_1:
    case opc_lload_2:
    case opc_lload_3:
      return KND_LOAD;

    case opc_astore:
    case opc_astore_0:
    case opc_astore_1:
    case opc_astore_2:
    case opc_astore_3:
    case opc_dstore:
    case opc_dstore_0:
    case opc_dstore_1:
    case opc_dstore_2:
    case opc_dstore_3:
    case opc_fstore:
    case opc_fstore_0:
    case opc_fstore_1:
    case opc_fstore_2:
    case opc_fstore_3:
    case opc_istore:
    case opc_istore_0:
    case opc_istore_1:
    case opc_istore_2:
    case opc_istore_3:
    case opc_lstore:
    case opc_lstore_0:
    case opc_lstore_1:
    case opc_lstore_2:
    case opc_lstore_3:
      return KND_STORE;

    case opc_ret:
      return KND_RET;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  /**
   * Returns the maximum index of local vars used by this instruction.
   * A variable of type
   */
  /*package*/ int getLocalVar() {
    switch (getOperandType()) {
    case TYP_ADDRESS:
    case TYP_FLOAT:
    case TYP_INT:
    case TYP_REFERENCE:
      return index;

    case TYP_DOUBLE:
    case TYP_LONG:
      return index + 1;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  // --------------------------------------------------------------------
  // CHECK CONTROL FLOW
  // --------------------------------------------------------------------

  public boolean isLoad() {
    return getStack() > 0;
  }

  public boolean isStore() {
    return getStack() < 0;
  }

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public int getPushedOnStack() {
    return isLoad() ? getStack() : 0;
  }

  /**
   * Return the amount of stack (positive or negative) used by this instruction.
   */
  public int getStack() {
    switch (getOpcode()) {
    case opc_lstore:
    case opc_lstore_0:
    case opc_lstore_1:
    case opc_lstore_2:
    case opc_lstore_3:
    case opc_dstore:
    case opc_dstore_0:
    case opc_dstore_1:
    case opc_dstore_2:
    case opc_dstore_3:
	return -2;

    case opc_istore:
    case opc_istore_0:
    case opc_istore_1:
    case opc_istore_2:
    case opc_istore_3:
    case opc_fstore:
    case opc_fstore_0:
    case opc_fstore_1:
    case opc_fstore_2:
    case opc_fstore_3:
    case opc_astore:
    case opc_astore_0:
    case opc_astore_1:
    case opc_astore_2:
    case opc_astore_3:
      return -1;

    case opc_ret:
      return 0;

    case opc_iload:
    case opc_iload_0:
    case opc_iload_1:
    case opc_iload_2:
    case opc_iload_3:
    case opc_fload:
    case opc_fload_0:
    case opc_fload_1:
    case opc_fload_2:
    case opc_fload_3:
    case opc_aload:
    case opc_aload_0:
    case opc_aload_1:
    case opc_aload_2:
    case opc_aload_3:
      return 1;

    case opc_lload:
    case opc_lload_0:
    case opc_lload_1:
    case opc_lload_2:
    case opc_lload_3:
    case opc_dload:
    case opc_dload_0:
    case opc_dload_1:
    case opc_dload_2:
    case opc_dload_3:
      return 2;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  // --------------------------------------------------------------------
  // WRITE
  // --------------------------------------------------------------------

  /**
   * Insert or check location of constant value on constant pool
   *
   * @param	cp		the constant pool for this class
   */
  /*package*/ void resolveConstants(ConstantPool cp) {}

  /**
   * Write this instruction into a file
   *
   * @param	cp		the constant pool that contain all data
   * @param	out		the file where to write this object info
   *
   * @exception	java.io.IOException	an io problem has occured
   */
  /*package*/ void write(ConstantPool cp, DataOutput out) throws IOException {
    if (width == 4) {
      out.writeByte((byte)opc_wide);
    }
    out.writeByte((byte)getOpcode());

    if (width == 2) {
      out.writeByte((byte)(index & 0xFF));
    } else if (width == 4) {
      out.writeShort((short)(index & 0xFFFF));
    }
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  public static final byte	KND_LOAD = 1;
  public static final byte	KND_STORE = 2;
  public static final byte	KND_RET = 3;

  private int			index;
  private int			width;
}

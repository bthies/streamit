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
 * $Id: Instruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * Root class for instructions.
 *
 * An instruction is defined by its opcode and its arguments.
 */
public abstract class Instruction
  extends AbstractInstructionAccessor
  implements Constants
{

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new instruction
   *
   * @param	opcode		the opcode of the instruction
   */
  public Instruction(int opcode) {
    this.opcode = opcode;
    this.address = -1;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns the opcode of the instruction
   */
  public final int getOpcode() {
    return opcode;
  }

  /**
   * Returns the opcode of the instruction
   * Needed by PushLiteralInstruction
   */
  /*package*/ final void setOpcode(int opcode) {
    this.opcode = opcode;
  }

  /**
   * Returns the offset in bytes of the instruction from the beginning
   * of the method code (ie classfile).
   */
  /*package*/ final int getAddress() {
    if (address == -1) {
      throw new InconsistencyException("instruction not reached");
    }
    return address;
  }

  /**
   * Sets the the offset in bytes of the instruction from the beginning
   * of the method code
   */
  /*package*/ final void setAddress(int address) {
    this.address = address;
  }

  /**
   * Returns the number of bytes used by the the instruction in the code array.
   */
  /*package*/ abstract int getSize();

  /**
   * Returns the maximum index of local vars used by this instruction.
   */
  /*package*/ int getLocalVar() {
    return 0;
  }

  // --------------------------------------------------------------------
  // CHECK CONTROL FLOW
  // --------------------------------------------------------------------

  /**
   * Verifies the enclosed instruction and computes the stack height.
   *
   * @param	env			the check environment
   * @param	curStack		the stack height at the end
   *					of the execution of the instruction
   * @return	true iff the next instruction in textual order needs to be
   *		checked, i.e. this instruction has not been checked before
   *		and it can complete normally
   * @exception	ClassFileFormatException	a problem was detected
   */
  /*package*/ void check(CodeEnv env, int curStack)
    throws ClassFileFormatException
  {}

  /**
   * Computes the address of the end of the instruction.
   *
   * @param	position	the minimum and maximum address of the
   *				begin of this instruction. This parameter
   *				is changed to the minimum and maximum
   *				address of the end of this instruction.
   */
  /*package*/ void computeEndAddress(CodePosition position) {
    position.addOffset(getSize());
  }

  /**
   * Returns the type pushed on the stack
   */
  public abstract byte getReturnType();

  /**
   * Returns the amount of stack (positive or negative) used by this instruction.
   */
  public abstract int getStack();

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public abstract int getPushedOnStack();

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public final int getPoppedFromStack() {
    return Math.abs(getStack() - getPushedOnStack());
  }

  // --------------------------------------------------------------------
  // INFOS ABOUT SUBCLASSES
  // --------------------------------------------------------------------

  /**
   * Returns true iff this instruction is a literal.
   */
  public boolean isLiteral() {
    return false;
  }

  /**
   * Returns true iff control flow can reach the next instruction
   * in textual order after executing this instruction.
   */
  public abstract boolean canComplete();

  // --------------------------------------------------------------------
  // WRITE
  // --------------------------------------------------------------------

  /**
   * Inserts or checks location of constant value in constant pool
   *
   * @param	cp		the constant pool for this class
   */
  /*package*/ abstract void resolveConstants(ConstantPool cp);

  /**
   * Write this class into the the file (out) getting data position from
   * the constant pool
   *
   * @param	cp		the constant pool that contain all data
   * @param	out		the file where to write this object info
   *
   * @exception	java.io.IOException	an io problem has occured
   */
  /*package*/ abstract void write(ConstantPool cp, DataOutput out)
    throws IOException;

  // --------------------------------------------------------------------
  // DEBUG
  // --------------------------------------------------------------------

  public void dump() {
    System.err.println("" + OpcodeNames.getName(getOpcode()) + " [" + this + "]");
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private int			opcode;

  // the offset in bytes of the instruction from the beginning of the method code
  private int			address;
}

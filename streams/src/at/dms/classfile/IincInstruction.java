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
 * $Id: IincInstruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Some instructions are perniticky enough that its simpler
 * to write them separately instead of smushing them with
 * all the rest. the iinc instruction is one of them.
 *
 * A wide prefix is automatically added if either the
 * variable index exceeds 256, or the increment value lies
 * outside the range [-128, 127]
 *
 * The VM spec is unclear on how the wide instruction is implemented,
 * but the implementation makes <em>both</em> the constant and the
 * variable index 16 bit values for the wide version of this instruction.
 */
public class IincInstruction extends Instruction {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new iinc instruction
   *
   * @param	var		the index of the variable to be incremented.
   * @param	inc		value to be added to the variable.
   */
  public IincInstruction(int var, int inc) {
    super(opc_iinc);

    this.var = var;
    this.inc = inc;
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
  /*package*/ void resolveConstants(ConstantPool cp) {}

  /**
   * Returns the number of bytes used by the the instruction in the code array.
   */
  /*package*/ int getSize() {
    return 1 + (isWide() ? 5 : 2);
  }

  /**
   * Return the value that is added to this variable
   */
  public int getIncrement() {
    return inc;
  }

  /**
   * Return the position of this variable in the local var set
   */
  public int getVariable() {
    return var;
  }

  // --------------------------------------------------------------------
  // CHECK CONTROL FLOW
  // --------------------------------------------------------------------

  /**
   * Returns the type pushed on the stack
   */
  public byte getReturnType() {
    return TYP_INT;
  }

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public int getPushedOnStack() {
    return 0;
  }

  /**
   * Return the amount of stack (positive or negative) used by this instruction
   */
  public int getStack() {
    return 0;
  }

  // --------------------------------------------------------------------
  // WRITE
  // --------------------------------------------------------------------

  /**
   * Write this instruction into a file
   *
   * @param	cp		the constant pool that contain all data
   * @param	out		the file where to write this object info
   *
   * @exception	java.io.IOException	an io problem has occured
   */
  /*package*/ void write(ConstantPool cp, DataOutput out) throws IOException {
    if (isWide()) {
      out.writeByte((byte)opc_wide);
    }

    out.writeByte((byte)getOpcode());

    if (isWide()) {
      out.writeShort((short)(var & 0xFFFF));
      out.writeShort((short)(inc & 0xFFFF));
    } else {
      out.writeByte((byte)(var & 0xFF));
      out.writeByte((byte)(inc & 0xFF));
    }
  }

  // --------------------------------------------------------------------
  // PRIVATE METHODS
  // --------------------------------------------------------------------

  /**
   * Return true if this instruction is implemented with a wide increment
   */
  private boolean isWide() {
    return (var > 255) || (inc > 127) || (inc < -128);
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private int			var;
  private int			inc;
}

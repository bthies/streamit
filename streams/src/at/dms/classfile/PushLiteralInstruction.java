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
 * $Id: PushLiteralInstruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * This class encapsulates the instructions pushing a literal on the stack:
 * bipush, sipush, ldc, ldc_w, ldc2_w, dconst_<d>, fconst_<f>, iconst_<i>,
 * lconst_<l>
 */
public class PushLiteralInstruction extends Instruction {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs an instruction that pushes a double literal on the stack.
   *
   * @param	value		a double literal
   */
  public PushLiteralInstruction(double value) {
    super(opc_ldc2_w);	// except for special cases

    if (value == 0D &&
	Double.doubleToLongBits(value) == Double.doubleToLongBits(0D)) {
      setOpcode(opc_dconst_0);
    } else if (value == 1D) {
      setOpcode(opc_dconst_1);
    } else {
      operand = new ConstantOperand(new DoubleConstant(value), true);
    }

    this.value = new Double(value);
  }

  /**
   * Constructs an instruction that pushes a float literal on the stack.
   *
   * @param	value		a float literal
   */
  public PushLiteralInstruction(float value) {
    super(opc_ldc);	// except for special cases

    if (value == 0F &&
	Float.floatToIntBits(value) == Float.floatToIntBits(0F)) {
      setOpcode(opc_fconst_0);
    } else if (value == 1F) {
      setOpcode(opc_fconst_1);
    } else if (value == 2F) {
      setOpcode(opc_fconst_2);
    } else {
      operand = new ConstantOperand(new FloatConstant(value), false);
    }

    this.value = new Float(value);
  }

  /**
   * Constructs an instruction that pushes a int literal on the stack.
   *
   * @param	value		a int literal
   */
  public PushLiteralInstruction(int value) {
    super(opc_ldc);	// except for special cases

    if (value >= -1 && value <= 5) {
      switch (value) {
      case -1:
	setOpcode(opc_iconst_m1); break;
      case 0:
	setOpcode(opc_iconst_0); break;
      case 1:
	setOpcode(opc_iconst_1); break;
      case 2:
	setOpcode(opc_iconst_2); break;
      case 3:
	setOpcode(opc_iconst_3); break;
      case 4:
	setOpcode(opc_iconst_4); break;
      case 5:
	setOpcode(opc_iconst_5); break;
      default:
	// no default
      }
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      setOpcode(opc_bipush);
      operand = new ByteOperand((byte)value);
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      setOpcode(opc_sipush);
      operand = new ShortOperand((short)value);
    } else {
      operand = new ConstantOperand(new IntegerConstant(value), false);
    }

    this.value = new Integer(value);
  }

  /**
   * Constructs an instruction that pushes a long literal on the stack.
   *
   * @param	value		a long literal
   */
  public PushLiteralInstruction(long value) {
    super(opc_ldc2_w);	// except for special cases

    if (value == 0L) {
      setOpcode(opc_lconst_0);
    } else if (value == 1L) {
      setOpcode(opc_lconst_1);
    } else {
      operand = new ConstantOperand(new LongConstant(value), true);
    }

    this.value = new Long(value);
  }

  /**
   * Constructs an instruction that pushes a string literal on the stack.
   *
   * @param	value		a string literal
   */
  public PushLiteralInstruction(String value) {
    super(opc_ldc);

    operand = new ConstantOperand(new StringConstant(value), false);

    this.value = value;
  }

  /**
   * Constructs an instruction that pushes a string literal on the stack
   * from a class file stream.
   *
   * @param	cst		a pooled constant
   * @param	wide		does this constant use 2 slots in the pool ?
   */
  public PushLiteralInstruction(PooledConstant cst, boolean wide) {
    super(opc_ldc);
    operand = new ConstantOperand(cst, wide);

    this.value = cst.getLiteral();
    if (this.value instanceof Long || this.value instanceof Double) {
      setOpcode(opc_ldc2_w);
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
    return true;
  }

  /**
   * Return true if this instruction is a literal
   */
  public boolean isLiteral() {
    return true;
  }

  /**
   * Returns the number of bytes used by the the instruction in the code array.
   */
  /*package*/ int getSize() {
    return 1 + (operand == null ? 0 : operand.getSize());
  }

  /**
   * Returns the value of this literal
   */
  public Object getLiteral() {
    return value;
  }

  // --------------------------------------------------------------------
  // CHECK CONTROL FLOW
  // --------------------------------------------------------------------

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public int getPushedOnStack() {
    return getStack();
  }

  /**
   * Returns the type pushed on the stack
   */
  public byte getReturnType() {
    switch (getOpcode()) {
    case opc_iconst_m1:
    case opc_iconst_0:
    case opc_iconst_1:
    case opc_iconst_2:
    case opc_iconst_3:
    case opc_iconst_4:
    case opc_iconst_5:
    case opc_sipush:
    case opc_bipush:
    case opc_ldc:
    case opc_ldc_w:
      return TYP_INT;
    case opc_fconst_0:
    case opc_fconst_1:
    case opc_fconst_2:
      return TYP_FLOAT;
    case opc_ldc2_w:
    case opc_lconst_0:
    case opc_lconst_1:
      return TYP_LONG;
    case opc_dconst_0:
    case opc_dconst_1:
      return TYP_DOUBLE;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  /**
   * Return the amount of stack (positive or negative) used by this instruction
   */
  public int getStack() {
    switch (getOpcode()) {
    case opc_iconst_m1:
    case opc_iconst_0:
    case opc_iconst_1:
    case opc_iconst_2:
    case opc_iconst_3:
    case opc_iconst_4:
    case opc_iconst_5:
    case opc_fconst_0:
    case opc_fconst_1:
    case opc_fconst_2:
    case opc_sipush:
    case opc_bipush:
    case opc_ldc:
    case opc_ldc_w:
      return 1;

    case opc_lconst_0:
    case opc_lconst_1:
    case opc_dconst_0:
    case opc_dconst_1:
    case opc_ldc2_w:
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
  /*package*/ void resolveConstants(ConstantPool cp) {
    if (operand != null) {
      operand.resolveConstants(cp);
    }
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

    if (operand != null) {
      operand.write(cp, out);
    }
  }


  // --------------------------------------------------------------------
  // INNER CLASSES
  // --------------------------------------------------------------------

  interface Operand {

    /**
     * Insert or check location of constant value on constant pool
     *
     * @param	cp		the constant pool for this class
     */
    void resolveConstants(ConstantPool cp);

    /**
     * Returns the number of bytes used by the the operand in the code array.
     */
    int getSize();

    /**
     * Write this class into the the file (out) getting data position from
     * the constant pool
     *
     * @param	cp		the constant pool that contain all data
     * @param	out		the file where to write this object info
     *
     * @exception	java.io.IOException	an io problem has occured
     */
    void write(ConstantPool cp, DataOutput out) throws IOException;
  }

  // --------------------------------------------------------------------

  private class ByteOperand implements Operand {
    ByteOperand(byte value) {
      this.value = value;
    }

    public void resolveConstants(ConstantPool cp) {}

    public int getSize() {
      return 1;
    }

    public void write(ConstantPool cp, DataOutput out) throws IOException {
      out.writeByte(value);
    }

    private final byte		value;
  }

  // --------------------------------------------------------------------

  private class ShortOperand implements Operand {
    ShortOperand(short value) {
      this.value = value;
    }

    public void resolveConstants(ConstantPool cp) {}

    public int getSize() {
      return 2;
    }

    public void write(ConstantPool cp, DataOutput out) throws IOException {
      out.writeShort(value);
    }

    private final short		value;
  }

  // --------------------------------------------------------------------

  private class ConstantOperand implements Operand {
    ConstantOperand(PooledConstant value, boolean wide) {
      this.value = value;
      this.wide = wide;
    }

    public void resolveConstants(ConstantPool cp) {
      cp.addItem(value);
    }

    public int getSize() {
      wide = value.getIndex() > 255;

      if (getOpcode() != opc_ldc2_w) {
	if (wide) {
	  setOpcode(opc_ldc_w);
	} else {
	  setOpcode(opc_ldc);
	}
      }

      return (getOpcode() == opc_ldc2_w) || wide ? 2 : 1;
    }

    public void write(ConstantPool cp, DataOutput out) throws IOException {
      int	idx = value.getIndex();

      if (wide || getOpcode() == opc_ldc2_w) {
  	out.writeShort((short)idx);
      } else {
  	out.writeByte((byte)(idx & 0xFF));
      }
    }

    private final PooledConstant	value;
    private boolean			wide;
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private Operand		operand;
  private Object		value;
}

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
 * $Id: NoArgInstruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * This class represents instructions that take no arguments.
 * opc_dastore, opc_lastore, opc_aastore, opc_bastore, opc_castore, opc_dcmpg, opc_dcmpl, opc_fastore,
 * opc_iastore, opc_lcmp, opc_sastore, opc_dadd, opc_ddiv, opc_dmul, opc_drem, opc_dreturn, opc_dsub,
 * opc_ladd, opc_land, opc_ldiv, opc_lmul, opc_lor, opc_lrem, opc_lreturn, opc_lsub, opc_lxor,
 * opc_pop2, opc_aaload, opc_areturn, opc_athrow, opc_baload, opc_caload, opc_d2f, opc_d2i, opc_fadd,
 * opc_faload, opc_fcmpg, opc_fcmpl, opc_fdiv, opc_fmul, opc_frem, opc_freturn, opc_fsub, opc_iadd,
 * opc_iaload, opc_iand, opc_idiv, opc_imul, opc_isub, opc_irem, opc_ishl, opc_ishr, opc_iushr, opc_ior,
 * opc_ixor, opc_l2f, opc_l2i, opc_lshl, opc_lshr, opc_lushr, opc_monitorenter, opc_monitorexit, opc_pop,
 * opc_saload, opc_ireturn, opc_nop, opc_arraylength, opc_d2l, opc_daload, opc_dneg, opc_f2i, opc_fneg,
 * opc_i2b, opc_i2c, opc_i2f, opc_i2s, opc_ineg, opc_l2d, opc_laload, opc_lneg, opc_return, opc_swap,
 * opc_aconst_null, opc_dup, opc_dup_x1, opc_dup_x2, opc_f2d, opc_f2l, opc_i2d, opc_i2l, opc_dup2,
 * opc_dup2_x1, opc_dup2_x2
 */
public class NoArgInstruction extends Instruction {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new instruction that takes no arguments
   *
   * @param	opcode		the opcode of the instruction
   */
  public NoArgInstruction(int opcode) {
    super(opcode);
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns true iff control flow can reach the next instruction
   * in textual order.
   */
  public boolean canComplete() {
    switch (getOpcode()) {
    case opc_dreturn:
    case opc_lreturn:
    case opc_areturn:
    case opc_freturn:
    case opc_return:
    case opc_ireturn:
    case opc_athrow:
      return false;

    default:
      return true;
    }
  }

  /**
   * Returns the number of bytes used by the the instruction in the code array.
   */
  /*package*/ int getSize() {
    return 1;
  }

  // --------------------------------------------------------------------
  // CHECK CONTROL FLOW
  // --------------------------------------------------------------------

  /**
   * Returns the type pushed on the stack
   */
  public byte getReturnType() {
    switch (getOpcode()) {
    case opc_dastore:
    case opc_lastore:
    case opc_aastore:
    case opc_bastore:
    case opc_castore:
    case opc_fastore:
    case opc_iastore:
    case opc_sastore:
    case opc_dreturn:
    case opc_lreturn:
    case opc_pop2:
    case opc_areturn:
    case opc_athrow:
    case opc_monitorenter:
    case opc_monitorexit:
    case opc_pop:
    case opc_ireturn:
    case opc_nop:
    case opc_return:
    case opc_freturn:
      return TYP_VOID;

    case opc_dcmpg:
    case opc_dcmpl:
    case opc_lcmp:
    case opc_baload:
    case opc_caload:
    case opc_d2i:
    case opc_fcmpg:
    case opc_fcmpl:
    case opc_iadd:
    case opc_iaload:
    case opc_iand:
    case opc_idiv:
    case opc_imul:
    case opc_isub:
    case opc_irem:
    case opc_ishl:
    case opc_ishr:
    case opc_iushr:
    case opc_ior:
    case opc_ixor:
    case opc_l2i:
    case opc_saload:
    case opc_arraylength:
    case opc_f2i:
    case opc_i2b:
    case opc_i2c:
    case opc_i2s:
    case opc_ineg:
      return TYP_INT;
    case opc_aconst_null:
    case opc_aaload:
      return TYP_REFERENCE;
    case opc_dadd:
    case opc_ddiv:
    case opc_dmul:
    case opc_drem:
    case opc_dsub:
    case opc_dneg:
    case opc_daload:
    case opc_l2d:
    case opc_f2d:
    case opc_i2d:
      return TYP_DOUBLE;
    case opc_ladd:
    case opc_land:
    case opc_ldiv:
    case opc_lmul:
    case opc_lor:
    case opc_lrem:
    case opc_lsub:
    case opc_lxor:
    case opc_lshl:
    case opc_lshr:
    case opc_lushr:
    case opc_d2l:
    case opc_laload:
    case opc_lneg:
    case opc_f2l:
    case opc_i2l:
      return TYP_LONG;
    case opc_d2f:
    case opc_fadd:
    case opc_faload:
    case opc_fdiv:
    case opc_fmul:
    case opc_frem:
    case opc_fsub:
    case opc_l2f:
    case opc_fneg:
    case opc_i2f:
      return TYP_FLOAT;

    case opc_swap:
    case opc_dup:
    case opc_dup_x1:
    case opc_dup_x2:
    case opc_dup2:
    case opc_dup2_x1:
    case opc_dup2_x2:
      return TYP_VOID;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public int getPushedOnStack() {
    switch (getOpcode()) {

    case opc_dastore:
    case opc_lastore:
    case opc_aastore:
    case opc_bastore:
    case opc_castore:
    case opc_fastore:
    case opc_iastore:
    case opc_sastore:
    case opc_dreturn:
    case opc_lreturn:
    case opc_pop2:
    case opc_areturn:
    case opc_athrow:
    case opc_monitorenter:
    case opc_monitorexit:
    case opc_pop:
    case opc_ireturn:
    case opc_nop:
    case opc_return:
      return 0;

    case opc_dcmpg:
    case opc_dcmpl:
    case opc_lcmp:
    case opc_aaload:
    case opc_baload:
    case opc_caload:
    case opc_d2f:
    case opc_d2i:
    case opc_fadd:
    case opc_faload:
    case opc_fcmpg:
    case opc_fcmpl:
    case opc_fdiv:
    case opc_fmul:
    case opc_frem:
    case opc_freturn:
    case opc_fsub:
    case opc_iadd:
    case opc_iaload:
    case opc_iand:
    case opc_idiv:
    case opc_imul:
    case opc_isub:
    case opc_irem:
    case opc_ishl:
    case opc_ishr:
    case opc_iushr:
    case opc_ior:
    case opc_ixor:
    case opc_l2f:
    case opc_l2i:
    case opc_saload:
    case opc_arraylength:
    case opc_f2i:
    case opc_fneg:
    case opc_i2b:
    case opc_i2c:
    case opc_i2f:
    case opc_i2s:
    case opc_ineg:
    case opc_swap:
    case opc_aconst_null:
    case opc_dup:
    case opc_dup_x1:
    case opc_dup_x2:
      return 1;

    case opc_dadd:
    case opc_ddiv:
    case opc_dmul:
    case opc_drem:
    case opc_dsub:
    case opc_ladd:
    case opc_land:
    case opc_ldiv:
    case opc_lmul:
    case opc_lor:
    case opc_lrem:
    case opc_lsub:
    case opc_lxor:
    case opc_lshl:
    case opc_lshr:
    case opc_lushr:
    case opc_d2l:
    case opc_dneg:
    case opc_daload:
    case opc_l2d:
    case opc_laload:
    case opc_lneg:
    case opc_dup2:
    case opc_dup2_x1:
    case opc_dup2_x2:
    case opc_f2d:
    case opc_f2l:
    case opc_i2d:
    case opc_i2l:
      return 2;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  /**
   * Return the amount of stack (positive or negative) used by this instruction
   */
  public int getStack() {
    switch (getOpcode()) {

    case opc_dastore:
    case opc_lastore:
      return -4;

    case opc_aastore:
    case opc_bastore:
    case opc_castore:
    case opc_dcmpg:
    case opc_dcmpl:
    case opc_fastore:
    case opc_iastore:
    case opc_lcmp:
    case opc_sastore:
      return -3;

    case opc_dadd:
    case opc_ddiv:
    case opc_dmul:
    case opc_drem:
    case opc_dreturn:
    case opc_dsub:
    case opc_ladd:
    case opc_land:
    case opc_ldiv:
    case opc_lmul:
    case opc_lor:
    case opc_lrem:
    case opc_lreturn:
    case opc_lsub:
    case opc_lxor:
    case opc_pop2:
      return -2;

    case opc_aaload:
    case opc_areturn:
    case opc_athrow:
    case opc_baload:
    case opc_caload:
    case opc_d2f:
    case opc_d2i:
    case opc_fadd:
    case opc_faload:
    case opc_fcmpg:
    case opc_fcmpl:
    case opc_fdiv:
    case opc_fmul:
    case opc_frem:
    case opc_freturn:
    case opc_fsub:
    case opc_iadd:
    case opc_iaload:
    case opc_iand:
    case opc_idiv:
    case opc_imul:
    case opc_isub:
    case opc_irem:
    case opc_ishl:
    case opc_ishr:
    case opc_iushr:
    case opc_ior:
    case opc_ixor:
    case opc_l2f:
    case opc_l2i:
    case opc_lshl:
    case opc_lshr:
    case opc_lushr:
    case opc_monitorenter:
    case opc_monitorexit:
    case opc_pop:
    case opc_saload:
    case opc_ireturn:
      return -1;

    case opc_nop:
    case opc_arraylength:
    case opc_d2l:
    case opc_daload:
    case opc_dneg:
    case opc_f2i:
    case opc_fneg:
    case opc_i2b:
    case opc_i2c:
    case opc_i2f:
    case opc_i2s:
    case opc_ineg:
    case opc_l2d:
    case opc_laload:
    case opc_lneg:
    case opc_return:
    case opc_swap:
      return 0;

    case opc_aconst_null:
    case opc_dup:
    case opc_dup_x1:
    case opc_dup_x2:
    case opc_f2d:
    case opc_f2l:
    case opc_i2d:
    case opc_i2l:
      return 1;

    case opc_dup2:
    case opc_dup2_x1:
    case opc_dup2_x2:
      return 2;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  // --------------------------------------------------------------------
  // INFOS ABOUT THIS
  // --------------------------------------------------------------------

  /**
   * Return true if this instruction is a literal
   */
  public boolean isLiteral() {
    return getOpcode() == opc_aconst_null;
  }

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
  {
    switch (getOpcode()) {
    case opc_dreturn:
    case opc_lreturn:
    case opc_areturn:
    case opc_freturn:
    case opc_return:
    case opc_ireturn:
      if (curStack != 0) {
	throw new ClassFileFormatException("stack not empty after return");
      }
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
    out.writeByte((byte)getOpcode());
  }
}

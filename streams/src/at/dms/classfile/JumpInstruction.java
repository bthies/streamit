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
 * $Id: JumpInstruction.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;

import at.dms.util.InconsistencyException;

/**
 * This class represents instructions that take a label as argument.
 *
 * Only goto and jsr instructions have wide variants for targets that
 * are too far away to have their relative address stored in a short.
 * For jump instructions except goto and jsr with a target that is too
 * far away, an additional goto_w has to be generated. For example :
 *         ifne A
 *     B:  ...
 * will be transformed into :
 *         ifeq B
 *         goto_w A
 *     B:  ...
 * Note that the condition has been inverted in the transformed code.
 */
public class JumpInstruction extends Instruction implements AccessorContainer {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new instruction that takes a label as argument.
   *
   * @param	opcode		the opcode of the instruction
   * @param	target		the referenced instruction
   */
  public JumpInstruction(int opcode, InstructionAccessor target) {
    super(opcode);

    this.target = target;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns true iff control flow can reach the next instruction
   * in textual order.
   */
  public boolean canComplete() {
    return getOpcode() != opc_goto && getOpcode() != opc_goto_w;
  }

  /**
   * Transforms targets (deferences to actual instructions).
   */
  public void transformAccessors(AccessorTransformer transformer) throws BadAccessorException {
    this.target = this.target.transform(transformer, this);
  }

  /**
   * Sets the target for this instruction
   */
  public void setTarget(InstructionAccessor target) {
    this.target = target;
  }

  /**
   * Return the target of this instruction
   */
  public InstructionAccessor getTarget() {
    return target;
  }

  /**
   * Returns the number of bytes used by the the instruction
   * in the code array.
   */
  /*package*/ int getSize() {
    return getSize(wide);
  }

  /**
   * Returns the number of bytes used by the the instruction
   * in the code array, depending on the distance of the target.
   *
   * @param	wide		is a wide jump necessary ?
   */
  private int getSize(boolean wide) {
    switch (getOpcode()) {
    case opc_goto:
    case opc_goto_w:
    case opc_jsr:
    case opc_jsr_w:
      return wide ? (1 + 4) : (1 + 2);

    default:
      return wide ? (1 + 2 + 1 + 4) : (1 + 2);
    }
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
  {
    if (getOpcode() == opc_jsr_w || getOpcode() == opc_jsr) {
      // the return address is pushed on the stack, but not
      // counted here because:
      // - it is popped on return before control flows to the
      //   next instruction in textual order
      // - it is counted in the traget
      env.checkExecutionPath((InstructionHandle)target, curStack + 1);
    } else {
      env.checkExecutionPath((InstructionHandle)target, curStack);
    }
  }

  /**
   * Computes the address of the end of the instruction.
   *
   * @param	position	the minimum and maximum address of the
   *				begin of this instruction. This parameter
   *				is changed to the minimum and maximum
   *				address of the end of this instruction.
   */
  /*package*/ void computeEndAddress(CodePosition position) {
    CodePosition	target = ((InstructionHandle)this.target).getPosition();

    boolean		minWide;
    boolean		maxWide;

    if (target.min == -1) {
      // target not yet known
      minWide = false;
      maxWide = true;
    } else if (target.min < position.max) {
      // target before this instruction
      minWide = (target.max - position.min) < Short.MIN_VALUE;
      maxWide = (target.min - position.max) < Short.MIN_VALUE;
    } else {
      // target after this instruction
      minWide = (target.min - position.max) > Short.MAX_VALUE;
      maxWide = (target.max - position.min) > Short.MAX_VALUE;
    }

    position.min += getSize(minWide);
    position.max += getSize(maxWide);

    if (minWide == maxWide) {
      wide = minWide;

      switch (getOpcode()) {
      case opc_goto_w:
      case opc_goto:
	setOpcode(wide ? opc_goto_w : opc_goto);
	break;

      case opc_jsr:
      case opc_jsr_w:
	setOpcode(wide ? opc_jsr_w : opc_jsr);
	break;

      default:
	// the actual translation will be done during writing
      }
    }
  }

  /**
   * Returns the size of data pushed on the stack by this instruction
   */
  public int getPushedOnStack() {
    switch (getOpcode()) {
    case opc_jsr_w:
      return 2;
    case opc_jsr:
      return 1;
    default:
      return 0;
    }
  }

  /**
   * Returns the type pushed on the stack
   */
  public byte getReturnType() {
    return TYP_VOID;
  }

  /**
   * Return the amount of stack (positive or negative) used by this instruction
   */
  public int getStack() {
    switch (getOpcode()) {
      case opc_ifeq:
      case opc_ifne:
      case opc_iflt:
      case opc_ifge:
      case opc_ifgt:
      case opc_ifle:
      case opc_ifnull:
      case opc_ifnonnull:
	return -1;

      case opc_if_icmpeq:
      case opc_if_icmpne:
      case opc_if_icmplt:
      case opc_if_icmpge:
      case opc_if_icmpgt:
      case opc_if_icmple:
      case opc_if_acmpeq:
      case opc_if_acmpne:
	return -2;

      case opc_goto_w:
      case opc_goto:
      case opc_jsr:
      case opc_jsr_w:
	return 0;

    default:
      throw new InconsistencyException("invalid opcode: " + getOpcode());
    }
  }

  // --------------------------------------------------------------------
  // INFO
  // --------------------------------------------------------------------

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
    Instruction		target = (Instruction)this.target;

    if (!wide) {
      out.writeByte((byte)getOpcode());
      out.writeShort((short)(target.getAddress() - getAddress()));
    } else {
      switch (getOpcode()) {
      case opc_goto_w:
      case opc_jsr_w:
	out.writeByte((byte)getOpcode());
	out.writeInt(target.getAddress() - getAddress());
	break;

      default:
	// conditionnally jump to address after goto_w (count own size)
	out.writeByte((byte)getReverseOpcode(getOpcode()));
	out.writeShort((short)(1 + 2 + 1 + 4));

	// goto original target, compute address of goto wrt to address
	// of conditional jump
	out.writeByte((byte)opc_goto_w);
	out.writeInt(target.getAddress() - (getAddress() + 1 + 2));
      }
    }
  }

  /**
   * Returns the inverted conditional jump opcode.
   */
  private static int getReverseOpcode(int opcode) {
    switch (opcode) {
    case opc_if_acmpeq:
      return opc_if_acmpne;
    case opc_if_acmpne:
      return opc_if_acmpeq;
    case opc_if_icmpeq:
      return opc_if_icmpne;
    case opc_if_icmpne:
      return opc_if_icmpeq;
    case opc_if_icmplt:
      return opc_if_icmpge;
    case opc_if_icmple:
      return opc_if_icmpgt;
    case opc_if_icmpgt:
      return opc_if_icmple;
    case opc_if_icmpge:
      return opc_if_icmplt;
    case opc_ifeq:
      return opc_ifne;
    case opc_ifne:
      return opc_ifeq;
    case opc_iflt:
      return opc_ifge;
    case opc_ifgt:
      return opc_ifle;
    case opc_ifle:
      return opc_ifgt;
    case opc_ifge:
      return opc_iflt;
    case opc_ifnull:
      return opc_ifnonnull;
    case opc_ifnonnull:
      return opc_ifnull;
    default:
      throw new InconsistencyException("no reverse opcode for " + OpcodeNames.getName(opcode));
    }
  }

  // --------------------------------------------------------------------
  // DEBUG
  // --------------------------------------------------------------------

  public void dump() {
    System.err.println("" + OpcodeNames.getName(getOpcode()) + " [" + this + "] ===> " + target);
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private InstructionAccessor	target;
  private boolean		wide;
}

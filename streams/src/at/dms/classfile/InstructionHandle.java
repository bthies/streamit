/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: InstructionHandle.java,v 1.1 2001-08-30 16:32:27 thies Exp $
 */

package at.dms.classfile;

/**
 *
 */
class InstructionHandle extends AbstractInstructionAccessor {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Creates a new instruction handle.
   *
   * @param	insn		the instruction
   * @param	prev		the handle of the next instruction
   *				in textual order
   */
  public InstructionHandle(Instruction insn, InstructionHandle prev) {
    this.insn = insn;

    this.next = null;
    if (prev == null) {
      this.index = 0;
    } else {
      this.index = prev.index + 1;
      prev.next = this;
    }

    this.position = new CodePosition(-1, -1);
    this.stackHeight = Integer.MIN_VALUE;
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Returns the enclosed instruction.
   */
  public Instruction getInstruction() {
    return insn;
  }

  /**
   * Returns the handle of the next instruction in textual order.
   */
  public InstructionHandle getNext() {
    return next;
  }

  /**
   * Notifies this handle that is has been attached to the specified container.
   */
  public void attachTo(AccessorContainer container) {
    // nothing to do for now
  }

  /**
   * Gets the position of current instruction
   */
  /*package*/ CodePosition getPosition() {
    return position;
  }

  /**
   * Returns the stack height after execution of this instruction.
   */
  /*package*/ final int getStackHeight() {
    return stackHeight;
  }

  /**
   * Returns the maximum index of local vars used by this instruction.
   */
  /*package*/ final int getLocalVar() {
    return insn.getLocalVar();
  }

  // --------------------------------------------------------------------
  // CHECKING OF THE EXECUTION PATHS
  // --------------------------------------------------------------------

  /**
   * Verifies the enclosed instruction and computes the stack height.
   *
   * @param	env			the check environment
   * @param	curStack		the stack height at the beginning
   *					of the execution of the instruction
   * @return	true iff the next instruction in textual order needs to be
   *		checked, i.e. this instruction has not been checked before
   *		and it can complete normally
   * @exception	ClassFileFormatException	a problem was detected
   */
  /*package*/ final boolean checkInstruction(CodeEnv env, int curStack)
    throws ClassFileFormatException
  {
    curStack += insn.getStack();
    if (curStack < 0) {
      System.err.println(">>>>>>>>" + curStack + " / " + insn.getStack());
      dump();
      throw new ClassFileFormatException("stack underflow");
    }

    if (stackHeight != Integer.MIN_VALUE) {
      if (stackHeight != curStack) {
	env.dumpCode();
	throw new ClassFileFormatException("@" + index + "(" + OpcodeNames.getName(insn.getOpcode()) + "): stack height different: " + stackHeight + "/" + curStack);
      }
      return false;
    } else {
      stackHeight = curStack;
      insn.check(env, curStack);
      return insn.canComplete();
    }
  }

  /**
   * Sets the position of this instruction and return true if address is final
   */
  /*package*/ boolean setAddressAndAdvancePC(CodePosition position) {
    this.position.setPosition(position);
    this.insn.computeEndAddress(position);
    return position.isFix();
  }

  /**
   * Sets the final position of this instruction in the code array.
   */
  /*package*/ void setAddress() {
    insn.setAddress(position.min);
  }

  // --------------------------------------------------------------------
  // DEBUG
  // --------------------------------------------------------------------

  /*package*/ void dump() {
    System.err.println(index
		       + ":\t" + (stackHeight == Integer.MIN_VALUE ? "N/A" : "" + stackHeight)
		       + "\t" + OpcodeNames.getName(insn.getOpcode()));
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private final Instruction		insn;

  private InstructionHandle		next;

  // the index of the instruction
  private int				index;

  // the position in classfile (instruction size may vary depending on
  // there position  ==>  need to find a fix point).
  private CodePosition			position;

  // The stack height when this instruction is reached by some execution path,
  // after execution of this instruction (Integer.MIN_VALUE: unreachable)
  private int				stackHeight;
}

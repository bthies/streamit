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
 * $Id: CodeEnv.java,v 1.1 2001-08-30 16:32:26 thies Exp $
 */

package at.dms.classfile;

import at.dms.util.InconsistencyException;

/**
 * This class represent the code environement during control flow
 *
 * This allow to compute the amount of stack consumed, to check
 * if instructions are reached and so on
 */
final class CodeEnv {

  // --------------------------------------------------------------------
  // ENTRY POINT
  // --------------------------------------------------------------------

  /**
   * Checks the specified CodeInfo structure and computes maxStack and
   * maxLocals.
   */
  public static void check(CodeInfo codeInfo) throws ClassFileFormatException {
    CodeEnv	env;

    env = new CodeEnv(codeInfo);
    env.installInstructionHandles();

    env.checkExecutionPaths();
    env.computeCodeLength();
    env.computeStackAndLocals();

    env.removeInstructionHandles();
  }

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a new CodeEnv structure.
   */
  private CodeEnv(CodeInfo codeInfo) {
    this.codeInfo = codeInfo;
  }

  // --------------------------------------------------------------------
  // CHECK EXECUTION PATHS, COMPUTE CODE SIZE
  // --------------------------------------------------------------------

  /**
   * Verifies all possible execution path(s).
   */
  private final void checkExecutionPaths() throws ClassFileFormatException {
    checkExecutionPath(methodStart, 0);

    HandlerInfo[]	handlers = codeInfo.getHandlers();
    for (int i = 0; i < handlers.length; i++) {
      // the exception parameter is on top of stack
      checkExecutionPath((InstructionHandle)handlers[i].getHandler(), 1);
    }
  }

  /**
   * Verifies execution path(s) starting at specified instruction.
   *
   * @param	handle			the handle of the first instruction
   * @param	curStack		the stack height at the beginning
   *					of the execution of the instruction
   * @exception	ClassFileFormatException	a problem was detected
   */
  /*package*/ final void checkExecutionPath(InstructionHandle handle,
					    int curStack)
    throws ClassFileFormatException
  {
    try {
      while (handle != null && handle.checkInstruction(this, curStack)) {
	curStack = handle.getStackHeight();
	handle = handle.getNext();
      }
    } catch (ClassFileFormatException e) {
      dumpCode();
      throw e;
    }
  }

  /**
   * Computes size and sets address of each instruction in the code array.
   */
  private final void computeCodeLength() {
    boolean		fixPoint = false;
    CodePosition	position;

    // compute size for each instruction
    do {
      fixPoint = true;
      position = new CodePosition(0, 0);

      for (InstructionHandle handle = methodStart;
	   handle != null;
	   handle = handle.getNext()) {
	fixPoint &= handle.setAddressAndAdvancePC(position);
      }
    } while (!fixPoint);

    // sets address of each instruction
    for (InstructionHandle handle = methodStart;
	 handle != null;
	 handle = handle.getNext()) {
      handle.setAddress();
    }

    codeInfo.setCodeLength(position.min);
  }

  /**
   * Computes max stack and max locals.
   */
  private final void computeStackAndLocals() {
    int		maxStack = 0;
    int		maxLocals = 0;

    for (InstructionHandle handle = methodStart;
	 handle != null;
	 handle = handle.getNext()) {
      maxStack = Math.max(maxStack, handle.getStackHeight());
      maxLocals = Math.max(maxLocals, handle.getLocalVar());
    }

    codeInfo.setMaxStack(maxStack);
    codeInfo.setMaxLocals(maxLocals + 1);
  }

  // --------------------------------------------------------------------
  // INSTALLING AND REMOVING INSTRUCTION HANDLES
  // --------------------------------------------------------------------

  /**
   * Install handles around instructions.
   */
  private void installInstructionHandles() {
    Instruction[]		insns = codeInfo.getInstructions();

    InstructionHandle[]		handles = new InstructionHandle[insns.length];
    for (int i = 0; i < handles.length; i++) {
      // this also sets the field next in handles
      handles[i] = new InstructionHandle(insns[i], i == 0 ? null : handles[i-1]);
    }

    try {
      codeInfo.transformAccessors(new HandleCreator(insns, handles));
    } catch (BadAccessorException e) {
      throw new InconsistencyException(e.getMessage());
    }

    this.methodStart = handles[0];
  }

  /**
   * Replaces handles by associated instructions.
   */
  private void removeInstructionHandles() {
    // replace instruction handles by actual instructions
    try {
      AccessorTransformer	transformer = new AccessorTransformer() {
	  public InstructionAccessor transform(InstructionAccessor accessor,
					       AccessorContainer container)
	  {
	    // the only accessors to resolve are instruction handles
	    return ((InstructionHandle)accessor).getInstruction();
	  }
	};

      codeInfo.transformAccessors(transformer);
    } catch (BadAccessorException e) {
      throw new InconsistencyException(e.getMessage());
    }
  }

  // --------------------------------------------------------------------
  // DEBUG
  // --------------------------------------------------------------------

  /*package*/ void dumpCode() {
    for (InstructionHandle handle = methodStart;
	 handle != null;
	 handle = handle.getNext()) {
      handle.dump();
    }
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private final CodeInfo	codeInfo;

  // the first instruction in textual order
  private InstructionHandle	methodStart;
}

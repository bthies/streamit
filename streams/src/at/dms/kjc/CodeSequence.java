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
 * $Id: CodeSequence.java,v 1.3 2003-08-21 09:44:20 thies Exp $
 */

/**
 * Code sequence is used as a bag to hold lists of instructions
 * until it is time to put them into a class file.
 *
 * Note: Local variables are currently not used
 */
package at.dms.kjc;

import java.util.Stack;
import java.util.Vector;

import at.dms.classfile.*;
import at.dms.util.InconsistencyException;
import at.dms.util.Utils;

public final class CodeSequence extends at.dms.util.Utils implements Constants {

  // --------------------------------------------------------------------
  // CONSTRUCTORS
  // --------------------------------------------------------------------

  /**
   * Constructs a code attribute.
   */
  private CodeSequence() {
    instructions = new Instruction[at.dms.classfile.Constants.MAX_CODE_PER_METHOD];
    handlers = new Vector();
    lines = new Vector();
    /* LOCAL VARIABLES NOT USED
    locals = new Hashtable();
    LOCAL VARIABLES NOT USED */
  }

  /**
   * Constructs a code sequence.
   */
  public static CodeSequence getCodeSequence() {
    CodeSequence	seq;
    if (!Constants.ENV_USE_CACHE || stack.empty()) {
      seq = new CodeSequence();
    } else {
      seq = (CodeSequence)stack.pop();
    }
    seq.pc = 0;
    seq.labelAtEnd = false;
    seq.lineNumber = 0;
    seq.lastLine = -1;
    return seq;
  }

  /**
   * Release a code sequence
   */
  public void release() {
    if (Constants.ENV_USE_CACHE) {
      stack.push(this);
      handlers.setSize(0);
      lines.setSize(0);
      /* LOCAL VARIABLES NOT USED
      seq.locals.clear();
      LOCAL VARIABLES NOT USED */
    }
  }

  public static void endSession() {
    while (!stack.empty()) {
      stack.pop();
    }
  }

  // --------------------------------------------------------------------
  // ACCESSORS
  // --------------------------------------------------------------------

  /**
   * Adds an instruction to the code of the current method.
   *
   * @param	insn		the instruction to append
   */
  public final void plantInstruction(Instruction insn) {
    instructions[pc++] = insn;

    labelAtEnd = false;
    if (lineNumber != lastLine) {
      lastLine = lineNumber;
      lines.addElement(new LineNumberInfo((short)lineNumber, insn));
    }
  }

  /**
   * Appends an instruction without arguments to the code
   * of the current method.
   *
   * @param	opcode		the instruction opcode
   */
  public final void plantNoArgInstruction(int opcode) {
    plantInstruction(new NoArgInstruction(opcode));
  }

  /**
   * Appends an instruction to the code of the current method
   * which pops the top-most element from the stack.
   *
   * @param	type		the type of the top-most element
   */
  public final void plantPopInstruction(CType type) {
    switch (type.getSize()) {
    case 0:
      assert(type == CStdType.Void);
      break;
    case 1:
      plantNoArgInstruction(opc_pop);
      break;
    case 2:
      plantNoArgInstruction(opc_pop2);
      break;
    default:
      assert(false);
    }
  }

  /**
   * Adds a local var instruction to the code of the current method.
   *
   * @param	opcode		the instruction opcode
   * @param	var		the referenced variable
   */
  public final void plantLocalVar(int opcode, JLocalVariable var) {
    LocalVarInstruction	insn = new LocalVarInstruction(opcode,
						       var.getPosition());
    plantInstruction(insn);
    addLocalVarInfo(insn, var);
  }

  /**
   * Adds a load of this (local var 0) to the code of the current method.
   */
  public final void plantLoadThis() {
    plantInstruction(new LocalVarInstruction(opc_aload, 0));
  }

  /**
   * Adds a field reference instruction to the code of the current method.
   *
   * @param	opcode		the instruction opcode
   * @param	owner		the qualified name of the class containing the field
   * @param	name		the simple name of the referenced field
   * @param	type		the signature of the referenced field
   */
  public final void plantFieldRefInstruction(int opcode,
					     String owner,
					     String name,
					     String type)
  {
    plantInstruction(new FieldRefInstruction(opcode, owner, name, type));
  }

  /**
   * Adds a method reference instruction to the code of the current method.
   *
   * @param	opcode		the instruction opcode
   * @param	owner		the qualified name of the class containing the method
   * @param	name		the simple name of the referenced method
   * @param	type		the signature of the referenced method
   */
  public final void plantMethodRefInstruction(int opcode,
					      String owner,
					      String name,
					      String type)
  {
    plantInstruction(new MethodRefInstruction(opcode, owner, name, type));
  }

  /**
   * Adds a class reference instruction to the code of the current method.
   *
   * @param	opcode		the instruction opcode
   * @param	name		the qualified name of the referenced object
   */
  public final void plantClassRefInstruction(int opcode, String name) {
    plantInstruction(new ClassRefInstruction(opcode, name));
  }

  /**
   * Adds an jump instruction to the code of the current method.
   *
   * @param	opcode		the instruction opcode
   * @param	target		the jump target
   */
  public final void plantJumpInstruction(int opcode, CodeLabel target) {
    plantInstruction(new JumpInstruction(opcode, target));
  }

  /**
   * Appends an array creation instruction to the code of the current method.
   *
   * @param	type		the element type
   */
  public final void plantNewArrayInstruction(CType type) {
    if (type.isReference()) {
      plantClassRefInstruction(opc_anewarray, ((CClassType)type).getQualifiedName());
    } else {
      byte	num;

      switch (type.getTypeID()) {
      case TID_BYTE:
	num = 8;
	break;
      case TID_BOOLEAN:
	num = 4;
	break;
      case TID_CHAR:
	num = 5;
	break;
      case TID_SHORT:
	num = 9;
	break;
      case TID_INT:
	num = 10;
	break;
      case TID_LONG:
	num = 11;
	break;
      case TID_FLOAT:
	num = 6;
	break;
      case TID_DOUBLE:
	num = 7;
	break;
      default:
	throw new InconsistencyException();
      }
      plantInstruction(new NewarrayInstruction(num));
    }
  }

  /**
   * Adds an instruction to the code of the current method.
   *
   * @param	insn		the instruction to append
   */
  public final void plantLabel(CodeLabel label) {
    label.setAddress(pc);
    labelAtEnd = true;
  }

  // --------------------------------------------------------------------
  // ENVIRONEMENT
  // --------------------------------------------------------------------

  /**
   * @param	lineNumber		the current line number in source code
   */
  public final void setLineNumber(int lineNumber) {
    if (lineNumber != 0) {
      this.lineNumber = lineNumber;
    }
  }

  /**
   * @return	an array of line number information
   */
  public final LineNumberInfo[] getLineNumbers() {
    return (LineNumberInfo[])Utils.toArray(lines, LineNumberInfo.class);
  }

  /**
   * @return	an array of local vars information
   */
  public final LocalVariableInfo[] getLocalVariableInfos() {
    return null;
    /* LOCAL VARIABLES NOT USED
    if (locals.size() == 0) {
      return null;
    }

    Vector	compress = new Vector();

    Enumeration stacks = locals.elements();
    while (stacks.hasMoreElements()) {
      Stack current = (Stack)stacks.nextElement();
      while (!current.empty()) {
	compress.addElement(((JLocalVariableEntry)current.pop()).getInfo());
      }
    }

    return (LocalVariableInfo[])Utils.toArray(compress, LocalVariableInfo.class);
    LOCAL VARIABLES NOT USED */
  }

  /**
   * Ask the code handler to generate the necessary code to call every
   * finally clause of all try statements
   */
  public final void plantReturn(JReturnStatement ret) {
    for (int i = contexts.size() - 1; i >= 0; i--) {
      JStatement stmt = (JStatement)contexts.elementAt(i);

      if (stmt instanceof JTryFinallyStatement) {
	((JTryFinallyStatement)stmt).genFinallyCall(this, ret);
      } else if (stmt instanceof JSynchronizedStatement) {
	((JSynchronizedStatement)stmt).genMonitorExit(this);
      }
    }
  }

  /**
   * Ask the code handler to generate the necessary code to call every
   * finally and monitorexit
   */
  public final void plantBreak(JStatement top) {
    for (int i = contexts.size() - 1; i >= 0 && contexts.elementAt(i) != top; i--) {
      JStatement stmt = (JStatement)contexts.elementAt(i);

      if (stmt instanceof JTryFinallyStatement) {
	((JTryFinallyStatement)stmt).genFinallyCall(this, null);
      } else if (stmt instanceof JSynchronizedStatement) {
	((JSynchronizedStatement)stmt).genMonitorExit(this);
      }
    }
  }

  // --------------------------------------------------------------------
  // PUSH CONTEXT
  // --------------------------------------------------------------------

  /**
   * Informs the code handlers that we begin a portion of breakable code.
   */
  public final void pushContext(JStatement stmt) {
    contexts.push(stmt);
  }

  /**
   * Informs the code handlers that we exit a breakable code.
   * Checks that contexts match.
   */
  public final void popContext(JStatement stmt) {
    assert((JStatement)contexts.pop() == stmt);
  }


  // --------------------------------------------------------------------
  // EXCEPTIONS
  // --------------------------------------------------------------------

  /*
   * Adds an exception handler to the code of this method.
   *
   * @param	start		the beginning of the checked area (inclusive)
   * @param	end		the end of the checked area (exclusive !)
   * @param	handler		the entrypoint into the exception handling routine.
   * @param	thrown		the exceptions handled by this routine
   */
  public final void addExceptionHandler(int start,
					int end,
					int handler,
					String thrown)
  {
    // no handler if checked area is empty
    if (start != end) {
      handlers.addElement(new HandlerInfo(getInstructionAt(start),
					  getInstructionAt(end - 1),	// inclusive !
					  getInstructionAt(handler),
					  thrown));
    }
  }

  /**
   * Returns an array of all exception handler
   */
  public final HandlerInfo[] getHandlers() {
    return (HandlerInfo[])Utils.toArray(handlers, HandlerInfo.class);
  }

  // --------------------------------------------------------------------
  // PC
  // --------------------------------------------------------------------

  /**
   * Gets the location in code sequence
   */
  public final int getPC() {
    return pc;
  }

  /**
   * Returns the actual size of code (number of instruction)
   */
  public final int size() {
    return pc;
  }

  /**
   * Returns the instruction at a given position
   */
  public final Instruction getInstructionAt(int pc) {
    return instructions[pc];
  }

  /**
   * Return the instruction as a list
   *
   * @param	insn		the instruction to append
   * WARNING: AFTER a call to release() this array will be reused
   */
  public Instruction[] getInstructionArray() {
    // we resolve the labels here, since the array will be reused
    resolveLabels();

    Instruction[]	code = new Instruction[pc];
    System.arraycopy(instructions, 0, code, 0, pc);
    return code;
  }

  private void resolveLabels() {
    // if there is a label planted as last instruction, add a dummy
    // instruction at the end: it will never be reached
    if (labelAtEnd) {
      plantNoArgInstruction(Constants.opc_nop);
    }

    try {
      AccessorTransformer	transformer = new AccessorTransformer() {
	  public InstructionAccessor transform(InstructionAccessor accessor,
					       AccessorContainer container)
	  {
	    // the only accessors to resolve are labels
	    return getInstructionAt(((CodeLabel)accessor).getAddress());
	  }
	};

      for (int i = 0; i < pc; i++) {
	if (instructions[i] instanceof AccessorContainer) {
	  ((AccessorContainer)instructions[i]).transformAccessors(transformer);
	}
      }
    } catch (BadAccessorException e) {
      throw new InconsistencyException();
    }
  }

  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  /**
   * Add a local variable name information
   */
  private final void addLocalVarInfo(LocalVarInstruction insn,
				     JLocalVariable var) {
/* //  DISABLED, SLOW AND NOT USED AFTER
    Integer		pos = new Integer(var.getLocalIndex());
    Stack		elemsAtPos;
    JLocalVariableEntry	entry = new JLocalVariableEntry(insn, var);

    elemsAtPos  = (Stack)locals.get(pos);
    if (elemsAtPos == null) {
      locals.put(pos, elemsAtPos = new Stack());
      elemsAtPos.push(entry);
    } else if (!((JLocalVariableEntry)elemsAtPos.peek()).merge(entry)) {
      elemsAtPos.push(entry);
    }
*/
  }


  // --------------------------------------------------------------------
  // DATA MEMBERS
  // --------------------------------------------------------------------

  private Instruction[]			instructions;
  private Vector			handlers;
  private Vector			lines;
    /* LOCAL VARIABLES NOT USED
  private Hashtable			locals;
    LOCAL VARIABLES NOT USED */
  private int				pc;
  // is a label after the last instruction ?
  private boolean			labelAtEnd;
  private int				lineNumber;
  private int				lastLine;
  private Stack				contexts = new Stack();

  private static final Stack		stack = new Stack();

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CodeSequence other = new at.dms.kjc.CodeSequence();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CodeSequence other) {
  super.deepCloneInto(other);
  other.instructions = (at.dms.classfile.Instruction[])at.dms.kjc.AutoCloner.cloneToplevel(this.instructions, this);
  other.handlers = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.handlers, this);
  other.lines = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.lines, this);
  other.pc = this.pc;
  other.labelAtEnd = this.labelAtEnd;
  other.lineNumber = this.lineNumber;
  other.lastLine = this.lastLine;
  other.contexts = (java.util.Stack)at.dms.kjc.AutoCloner.cloneToplevel(this.contexts, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

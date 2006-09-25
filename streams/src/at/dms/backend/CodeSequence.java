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
 * $Id: CodeSequence.java,v 1.3 2006-09-25 13:54:30 dimock Exp $
 */

package at.dms.backend;

import at.dms.classfile.JumpInstruction;
import at.dms.classfile.Instruction;
import java.util.Stack;

/**
 * This class reconstructs the code sequence
 */
public class CodeSequence {

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the first instruction of the sequence
     */
    public InstructionHandle getCodeStart() {
        return codeStart;
    }

    /**
     * Returns the (current) last instruction of the sequence
     */
    public InstructionHandle getCurrent() {
        return current;
    }

    // --------------------------------------------------------------------
    // CODE GENERATION
    // --------------------------------------------------------------------

    /**
     * Adds an instruction at the end of the sequence
     * @param   handle      an instruction handle
     */
    public void plantInstruction(InstructionHandle handle) {
        if (current != null) {
            current.setNext(handle);
        }
        current = handle;
        handle.getInstruction().dump();
        if (codeStart == null) {
            codeStart = current;
        }
    }

    /**
     * Adds an instruction at the end of the sequence
     * @param   insn        the instruction
     */
    public void plantInstruction(Instruction insn) {
        plantInstruction(new InstructionHandle(insn, current));
    }

    /**
     * Adds a quadruple at the end of the sequence
     * @param   node        the node of the quadruple
     */
    public void plantQuadruple(QNode node) {
        node.generate(this);
    }

    /**
     * Adds a basic block and a jump instruction as needed
     * @param   block       the destination (BasicBlock)
     */
    public void jumpToBasicBlock(BasicBlock block) {
        if (block.isMarked() || block.getPosition() >= max) {
            plantInstruction(new InstructionHandle(new JumpInstruction(at.dms.classfile.Constants.opc_goto, block), current));
        }
        plantBasicBlock(block);
    }

    /**
     * Adds a basic block at the end of the sequence
     * @param   block       the destination (BasicBlock)
     */
    public void plantBasicBlock(BasicBlock block) {
        if (!block.isMarked()) {
            if (block.getPosition() >= max) {
                stack.push(block);
            } else {
                block.generateQuadruple(this);
            }
        }
    }

    /**
     * Adds a basic block at the end of the sequence
     * @param   block       the destination (BasicBlock)
     * @param   jump        the jump instruction to this block
     * @param   transition  the quadruple MOVE instructions
     */
    public void plantBasicBlock(BasicBlock block, JumpInstruction jump, QQuadruple[] transition) {
        if (transition != null && transition.length != 0) {
            BasicBlock  trans = new BasicBlock(-1);

            trans.setBody(transition);
            jump.setTarget(trans);
            plantBasicBlock(trans);
            jumpToBasicBlock(block);
        } else {
            plantBasicBlock(block);
        }
    }

    /**
     * Adds a basic block at the end of the sequence
     * @param   block       the destination (BasicBlock)
     * @param   max     the maximum position of this block on the original source
     *              this is made to avoid locality problems
     */
    public void plantBasicBlock(BasicBlock block, int max) {
        int     oldMax = this.max;

        this.max = max;

        plantBasicBlock(block);

        this.max = oldMax;
    }

    /**
     * Closes the code sequence
     * Generates pending basic blocks code
     */
    public void close() {
        if (stack.size() > 0) {
            Stack<BasicBlock>   todo = stack;

            stack = new Stack<BasicBlock>();

            while (todo.size() > 0) {
                BasicBlock  block = todo.pop();

                if (!block.isMarked()) {
                    plantBasicBlock(block);
                }
            }

            close();
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private InstructionHandle       codeStart;
    private InstructionHandle       current;
    private int             max = Integer.MAX_VALUE;
    private Stack<BasicBlock>               stack = new Stack<BasicBlock>();
}

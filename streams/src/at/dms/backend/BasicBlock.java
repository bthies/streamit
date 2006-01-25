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
 * $Id: BasicBlock.java,v 1.2 2006-01-25 17:00:34 thies Exp $
 */

package at.dms.backend;

import java.util.Vector;
import at.dms.classfile.AccessorTransformer;
import at.dms.classfile.BadAccessorException;
import at.dms.classfile.AccessorContainer;
import at.dms.classfile.AbstractInstructionAccessor;
import at.dms.classfile.SwitchInstruction;
import at.dms.backend.InstructionHandle;

import at.dms.util.InconsistencyException;
import at.dms.util.Utils;

/**
 * This class replaces all references to instructions by their associated handle.
 * At the same time, it adds line numbers to the handles and checks if they
 * are references by an instruction, handler or local variable info.
 */
class BasicBlock extends AbstractInstructionAccessor implements AccessorContainer {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new basic block
     */
    public BasicBlock(int pos) {
        this.pos = pos;
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the position of the BasicBlock in the list
     */
    public int getPosition() {
        return pos;
    }

    /**
     * Returns the quadruples of this block
     */
    public QNode[] getQuadruples() {
        return quads;
    }

    /**
     * Returns the successors of this block
     */
    public BasicBlock[] getSuccessors() {
        return successors;
    }

    /**
     * Returns the successors of this block
     */
    public QNode[][] getSuccessorAccess() {
        return successorAccess;
    }

    /**
     * Returns the successors of this block
     */
    public QNode[] getSuccessorNodes(int pos, int branch) {
        if (pos < quads.length - 1 && branch == -1) {
            return new QNode[]{quads[pos + 1]};
        } else if (branch == -1) {
            QNode[] temp = new QNode[successors.length];

            for (int i = 0; i < temp.length; i++) {
                temp[i] = successorAccess[i].length != 0 ?
                    successorAccess[i][0] :
                    successors[i].getSuccessorNodes(0, -1)[0];
            }

            return temp;
        } else {
            if (pos != successorAccess[branch].length - 1) {
                return new QNode[] {successorAccess[branch][pos + 1]};
            } else {
                return successors[branch].getSuccessorNodes(0, -1);
            }
        }
    }

    /**
     * Sets the next basic block in textual order
     */
    public void setNext(BasicBlock next) {
        this.next = next;
    }

    /**
     * Sets the body of this basic block
     * @param   insns           the array of instructions
     */
    public void setBody(InstructionHandle[] insns) {
        this.insns = insns;
    }

    /**
     * Sets the body of this basic block
     * @param   quads           the array of quadruples
     */
    public void setBody(QQuadruple[] quads) {
        this.quads = quads;
    }

    /**
     * Returns the first instruction of the basic block
     */
    public final InstructionHandle getFirstInstruction() {
        return firstInstruction;
    }

    /**
     * Returns the last instruction of the basic block
     */
    public final QNode getLastNode() {
        return quads.length == 0 ? null : quads[quads.length - 1];
    }

    // --------------------------------------------------------------------
    // CONTROL FLOW
    // --------------------------------------------------------------------

    /**
     * Builds quadruples
     */
    public void buildQuadruples(MethodEnv env) {
        if (isMarked()) {
            return;
        }
        setMarked(true);

        Vector  vquads = new Vector();
        remainingStack = QQuadruple.buildQuadruples(env, this, insns, entryStack, vquads);
        quads = (QNode[])Utils.toArray(vquads, QNode.class);
        insns = null;

        // Build edge with temporary assignment
        BasicBlock[]    successors = buildSuccessorList();

        successorAccess = new QQuadruple[successors.length][];

        for (int i = 0; i < successors.length; i++) {
            if (successors[i].entryStack != null) {
                successorAccess[i] = new QQuadruple[remainingStack.length];
                for (int j = remainingStack.length - 1; j >= 0; j--) {
                    successorAccess[i][j] = new QQuadruple(successors[i].entryStack[j], remainingStack[j]);
                }
            } else {
                successors[i].entryStack = new QTemporary[remainingStack.length];
                for (int j = remainingStack.length - 1; j >= 0; j--) {
                    successors[i].entryStack[j] = new QTemporary(remainingStack[j].getType());
                }
                successorAccess[i] = new QQuadruple[remainingStack.length];
                for (int j = remainingStack.length - 1; j >= 0; j--) {
                    successorAccess[i][j] = new QQuadruple(successors[i].entryStack[j], remainingStack[j]);
                }
            }

            successors[i].buildQuadruples(env);
        }
    }

    /**
     * Generates the flow of instructions
     */
    public void generateQuadruple(CodeSequence seq) {
        InstructionHandle   current = seq.getCurrent();
        for (int i = 0; i < quads.length; i++) {
            seq.plantQuadruple(quads[i]);
        }
        setMarked(true);

        if (next != null) {
            QQuadruple[]    transition = getTransition(next);

            if (transition != null && transition.length != 0) {
                BasicBlock  trans = new BasicBlock(-1);

                trans.setBody(transition);
                seq.plantBasicBlock(trans);
            }
            seq.jumpToBasicBlock(next);
        }

        firstInstruction = current == null ? seq.getCodeStart() : current.getNext();

        QNode   last = getLastNode();

        if (last != null && last.isJump()) {
            BasicBlock  target = (BasicBlock)last.getJump().getTarget();

            seq.plantBasicBlock(target, last.getInstruction().getJump(), getTransition(target));
        } else if (last != null && last.isSwitch()) {
            BasicBlock[]    targets = last.getSwitch().getTargets();

            optimizeOrder(targets, seq);
        }
    }

    /**
     * Resolves the jumps
     * Make jumps point to basic block instead of instructions
     */
    public void resolveJump() {
        InstructionHandle   last = insns[insns.length - 1];

        if (last.isJump()) {
            InstructionHandle   target = (InstructionHandle)last.getTarget();
            BasicBlock  block = ControlFlow.findBasicBlock(target);

            last.getJump().setTarget(block);
            if (last.getJump().canComplete()) {
                next = ControlFlow.findBasicBlock(last.getNext());
            }
        } else if (last.getInstruction() instanceof SwitchInstruction) {
            SwitchInstruction   insn = (SwitchInstruction)last.getInstruction();

            for (int i = -1; i < insn.getSwitchCount(); i++) {
                insn.setTarget(ControlFlow.findBasicBlock((InstructionHandle)insn.getTarget(i)), i);
            }
        } else if (last.getInstruction().canComplete() && last.getNext() != null) {
            next = ControlFlow.findBasicBlock(last.getNext());
        }
    }

    // ----------------------------------------------------------------------
    // COLORING
    // ----------------------------------------------------------------------

    /**
     * Sets this block to be marked
     */
    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    /**
     * Sets this block to be marked
     */
    public boolean isMarked() {
        return marked;
    }

    /**
     * Human readable form
     */
    public String toString() {
        return "BasicBlock " + pos;
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    private void optimizeOrder(BasicBlock[] bblocks, CodeSequence seq) {
        int     max = Integer.MIN_VALUE;
        int     end = Integer.MIN_VALUE;

        for (int j = 0; j < bblocks.length; j++) {
            if (bblocks[j].getPosition() > end) {
                end = bblocks[j].getPosition();
            }
        }
        for (int i = 0; i < bblocks.length; i++) {
            int min = Integer.MAX_VALUE;
            int index = 0;

            for (int j = 0; j < bblocks.length; j++) {
                if (bblocks[j].getPosition() < min &&
                    bblocks[j].getPosition() > max) {
                    min = bblocks[j].getPosition();
                    index = j;
                }
            }

            max = min;
            seq.plantBasicBlock(bblocks[index], i == bblocks.length - 1 ? Integer.MAX_VALUE : end);
        }
    }

    private BasicBlock[] buildSuccessorList() {
        if (successors != null) {
            return successors;
        }

        int     count = next == null ? 0 : 1;
        QNode   last = getLastNode();

        if (last != null && last.isJump()) {
            count++;
        } else if (last != null && last.isSwitch()) {
            count += ((SwitchInstruction)last.getInstruction().getInstruction()).getSwitchCount();
        }

        successors = new BasicBlock[count];

        if (next != null) {
            successors[--count] = next;
        }
        if (last != null && last.isJump()) {
            successors[--count] = (BasicBlock)last.getJump().getTarget();
        } else if (last != null && last.isSwitch()) {
            SwitchInstruction   insn = (SwitchInstruction)last.getInstruction().getInstruction();

            for (int i = -1; i < insn.getSwitchCount() - 1; i++) {
                successors[--count] = (BasicBlock)((SwitchInstruction)last.getInstruction().getInstruction()).getTarget(i);
            }
        }

        return successors;
    }

    private QQuadruple[] getTransition(BasicBlock target) {
        for (int i = 0; i < successors.length; i++) {
            if (successors[i] == target) {
                return successorAccess[i];
            }
        }
        throw new InconsistencyException();
    }

    // --------------------------------------------------------------------
    // IMPLEMENTATION OF INSTRUCTION HANDLER
    // --------------------------------------------------------------------

    /**
     * Transforms the accessors contained in this class.
     * @param   transformer     the transformer used to transform accessors
     */
    public void transformAccessors(AccessorTransformer transformer)
        throws BadAccessorException
    {
    }

    /**
     * Notifies this handle that is has been attached to the specified container.
     */
    public void attachTo(AccessorContainer container) {
        //!!! nothing to do for now
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private InstructionHandle[] insns;
    private InstructionHandle   firstInstruction;
    private QNode[]     quads;
    private QTemporary[]        remainingStack;
    private QTemporary[]        entryStack;
    private boolean     marked;
    private BasicBlock      next;
    private BasicBlock[]        successors;
    private QQuadruple[][]  successorAccess;
    private int         pos;
}

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
 * $Id: InstructionHandle.java,v 1.4 2006-09-25 13:54:51 dimock Exp $
 */

package at.dms.optimize;

import java.util.Enumeration;
import java.util.Vector;

import at.dms.classfile.AccessorContainer;
import at.dms.classfile.AbstractInstructionAccessor;
import at.dms.classfile.Instruction;
import at.dms.classfile.JumpInstruction;
import at.dms.classfile.LineNumberInfo;
import at.dms.classfile.NoArgInstruction;
import at.dms.classfile.LocalVarInstruction;
import at.dms.classfile.SwitchInstruction;
import at.dms.classfile.HandlerInfo;

import at.dms.util.InconsistencyException;

/**
 *
 */
public class InstructionHandle extends AbstractInstructionAccessor implements at.dms.classfile.Constants {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Creates a new instruction handle.
     *
     * @param   insn        the instruction
     * @param   prev        the handle of the next instruction
     *              in textual order
     */
    public InstructionHandle(Instruction insn, InstructionHandle prev) {
        this.insn = insn;
        this.prev = prev;
        this.next = null;

        if (prev != null) {
            prev.setNext(this);
        }
    }

    // --------------------------------------------------------------------
    // IMPLEMENTATION OF INSTRUCTION HANDLER
    // --------------------------------------------------------------------

    /**
     * Notifies this handle that is has been attached to the specified container.
     */
    public void attachTo(AccessorContainer container) {
        if (container instanceof LineNumberInfo) {
            if (lineNumbers == null) {
                lineNumbers = new Vector<Integer>();
            }
            lineNumbers.addElement(new Integer(((LineNumberInfo)container).getLine()));
        }
    }

    /**
     * Adds line number info stored for the instruction to the specified vector.
     */
    public void addLineNumberInfo(Vector lineNumberInfo) {
        if (lineNumbers != null) {
            for (Enumeration<Integer> eNum = lineNumbers.elements(); eNum.hasMoreElements(); ) {
                int line = eNum.nextElement().intValue();

                lineNumberInfo.addElement(new LineNumberInfo((short)line, this));
            }
        }
    }

    // --------------------------------------------------------------------
    // ACCESSORS (LIST)
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
     * Returns the handle of the next instruction in textual order.
     */
    public InstructionHandle getPrevious() {
        return prev;
    }

    // --------------------------------------------------------------------
    // ACCESSORS (INSTRUCTION)
    // --------------------------------------------------------------------

    /**
     * Returns true if this instruction is a jump instruction
     */
    public boolean isJump() {
        return insn instanceof JumpInstruction;
    }

    /**
     * Returns the instruction as a Jump instruction
     */
    public JumpInstruction getJump() {
        return (JumpInstruction)insn;
    }

    /**
     * Returns the instruction as a Jump instruction
     */
    public LocalVarInstruction getLocal() {
        return (LocalVarInstruction)insn;
    }

    /**
     * Returns the target of this jump instruction
     */
    public InstructionHandle getTarget() {
        return (InstructionHandle)((JumpInstruction)insn).getTarget();
    }

    /**
     * Returns the target of this jump instruction
     */
    public void setTarget(InstructionHandle target) {
        ((JumpInstruction)insn).setTarget(target);
    }

    /**
     * Returns the target of this jump instruction
     */
    public int getOpcode() {
        return insn.getOpcode();
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Sets the handle of the next instruction in textual order.
     */
    public void setNext(InstructionHandle next) {
        this.next = next;
        if (next != null) {
            next.prev = this;
        }
    }

    /**
     * isReached
     */
    public boolean isReached() {
        return reached;
    }

    /**
     * isTarget
     */
    public boolean isTarget() {
        return accessors != null;
    }

    /**
     * isReached
     */
    public void set() {
        reached = prev == null ||
            accessors != null ||
            (prev.isReached() && prev.getInstruction().canComplete());
    }

    /**
     * destroy
     */
    public void destroy() {
        if (accessors != null && getNext() == null) {
            insn = new NoArgInstruction(opc_nop);
        } else {
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }
            for (int i = 0; accessors != null && i < accessors.size(); i++) {
                changeTarget(accessors.elementAt(i), next);
            }
            notifyTargetOnRemove();
        }
    }

    /**
     * destroy
     */
    public boolean remove() {
        if (accessors != null && getNext() == null) {
            return false;
        } else {
            destroy();

            return true;
        }
    }

    /**
     * reset
     */
    public void reset() {
        if (insn instanceof AccessorContainer) {
            if (insn instanceof JumpInstruction) {
                ((InstructionHandle)((JumpInstruction)insn).getTarget()).addAccessor((JumpInstruction)insn); // !!!
            } else if (insn instanceof SwitchInstruction) {
                SwitchInstruction   current = (SwitchInstruction)insn;

                for (int i = -1; i < current.getSwitchCount(); i++) {
                    ((InstructionHandle)current.getTarget(i)).addAccessor(current);
                }
            } else {
                throw new InconsistencyException(">>>>>>>" + insn);
            }
        }
    }

    /**
     * reset
     */
    public void clean() {
        reached = false;

        accessors = null;
    }

    /**
     * replaceBy
     */
    public void replaceBy(Instruction insn) {
        notifyTargetOnRemove();
        this.insn = insn;
        notifyTargetOnAdd();
        set();
    }

    /**
     * replaceBy
     */
    public void replaceBy(Instruction[] insns) {
        replaceBy(insns[0]);

        InstructionHandle   current = this;
        InstructionHandle   next = this.next;

        for (int i = 1; i < insns.length; i++) {
            current = new InstructionHandle(insns[i], current);

            current.notifyTargetOnAdd();
        }

        current.next = next;
        if (current.next != null) {
            current.next.prev = current;
        }
    }

    public void addAccessor(AccessorContainer accessor) {
        if (accessors == null) {
            accessors = new Vector<AccessorContainer>();
        }

        accessors.addElement(accessor);
    }

    public AccessorContainer getAccessor(int i) {
        return accessors.elementAt(i);
    }

    public void removeAccessor(AccessorContainer accessor) {
        accessors.removeElement(accessor);
        if (accessors.size() == 0) {
            accessors = null;
        }
    }

    public void removeAccessors() {
        accessors = null;
    }

    // --------------------------------------------------------------------
    // PRIVATE UTILITIES
    // --------------------------------------------------------------------

    private void notifyTargetOnRemove() {
        if (insn instanceof JumpInstruction) {
            ((InstructionHandle)((JumpInstruction)insn).getTarget()).removeAccessor((AccessorContainer)insn);
        } else if (insn instanceof SwitchInstruction) {
            SwitchInstruction   current = (SwitchInstruction)insn;

            for (int i = -1; i < current.getSwitchCount(); i++) {
                ((InstructionHandle)(current.getTarget(i))).removeAccessor((AccessorContainer)insn);
            }
        }
    }

    private void notifyTargetOnAdd() {
        if (insn instanceof JumpInstruction) {
            ((InstructionHandle)((JumpInstruction)insn).getTarget()).addAccessor((AccessorContainer)insn);
        } else if (insn instanceof SwitchInstruction) {
            SwitchInstruction   current = (SwitchInstruction)insn;

            for (int i = -1; i < current.getSwitchCount(); i++) {
                ((InstructionHandle)(current.getTarget(i))).addAccessor((AccessorContainer)insn);
            }
        }
    }

    private void changeTarget(AccessorContainer container, InstructionHandle target) {
        if (container instanceof JumpInstruction) {
            ((JumpInstruction)container).setTarget(target);
            target.addAccessor(container);
        } else if (container instanceof SwitchInstruction) {
            SwitchInstruction   current = (SwitchInstruction)container;

            for (int i = -1; i < current.getSwitchCount(); i++) {
                if (current.getTarget(i) == this) {
                    current.setTarget(i, target);
                }
            }
            target.addAccessor(container);
        } else if (container instanceof HandlerInfo) {
            HandlerInfo handler = (HandlerInfo)container;

            if (handler.getStart() == this) {
                handler.setStart(target);
            }
            if (handler.getEnd() == this) {
                handler.setEnd(target);
            }
            if (handler.getHandler() == this) {
                handler.setHandler(target);
            }
        } else if (container instanceof Optimizer) {
            ((Optimizer)container).setCodeStart(target);
        } else {
            throw new InconsistencyException("" + container);
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private Instruction     insn;
    private boolean     reached;

    private InstructionHandle   prev;
    private InstructionHandle   next;

    private Vector<Integer>      lineNumbers;
    private Vector<AccessorContainer>      accessors;
}

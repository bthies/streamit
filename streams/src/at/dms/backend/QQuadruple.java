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
 * $Id: QQuadruple.java,v 1.3 2006-09-25 13:54:30 dimock Exp $
 */

package at.dms.backend;

import java.util.Vector;
import java.util.Stack;
import at.dms.classfile.IincInstruction;
import at.dms.classfile.LocalVarInstruction;
import at.dms.classfile.PushLiteralInstruction;
import at.dms.util.InconsistencyException;

/**
 * This class represents a quadruple: a source and a destination
 */
class QQuadruple extends QNode implements QOrigin {

    public QQuadruple(QDestination dest, QOrigin origin) {
        this.dest = dest;
        this.origin = origin;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * isStore
     */
    public boolean isStore() {
        return true;
    }

    /**
     * Returns this node a a jump
     */
    public QQuadruple getStore() {
        return this;
    }

    /**
     * Human readable form
     */
    public String toString() {
        return dest + " <- " + origin;
    }

    /**
     * The type of this instruction
     */
    public int getType() {
        return at.dms.classfile.Constants.TYP_VOID;
    }

    /**
     * Duplicate this node
     */
    public QOrigin duplicate() {
        throw new InconsistencyException("NYI");
    }

    /**
     * Uses the stack instead of a local var
     */
    public void useStack() {
        dest = new QStack(((QTemporary)dest).getType());
    }

    /**
     * returns the parameters of this instruction
     */
    public QOrigin[] getOrigins() {
        return origin.getOrigins();
    }

    /**
     * Sets the parameters of this instruction
     */
    public void setOrigin(QOrigin origin, int i) {
        if (this.origin instanceof QOperator) {
            this.origin.setOrigin(origin, i);
        } else if (i == 0) {
            this.origin = origin;
        } else {
            throw new InconsistencyException();
        }
    }

    // ----------------------------------------------------------------------
    // ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Returns the defined temporary.
     */
    public QTemporary getDef() {
        return (QTemporary)dest;
    }

    /**
     * Returns the used temporaries.
     */
    public QTemporary[] getUses() {
        return origin.getUses();
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Generates instructions for this quadruple
     * @param   seq     The code sequence of instruction
     */
    public void generate(CodeSequence seq) {
        origin.generate(seq);
        if (dest instanceof QTemporary) {
            dest.store(seq, isLive((QTemporary)dest));
        } else {
            dest.store(seq, true);
        }
    }

    // ----------------------------------------------------------------------
    // UTILITIES
    // ----------------------------------------------------------------------

    /**
     * Builds the quadruple list
     * @param   insns       the list of instructionx
     * @param   entryStack  the stack in entry
     * @param   vect        a place holder for quadruples
     * @return  the stack at the end of this basic block
     */
    public static QTemporary[] buildQuadruples(MethodEnv env,
                                               BasicBlock block,
                                               InstructionHandle[] insns,
                                               QTemporary[] entryStack,
                                               Vector vect) {
        Stack<Object>   stack = new Stack<Object>();

        for (int i = 0; entryStack != null && i < entryStack.length; i++) {
            stack.push(entryStack[i]);
        }

        for (int i = 0; i < insns.length; i++) {
            InstructionHandle       insn = insns[i];
            //insn.getInstruction().dump();

            if (insn.getInstruction() instanceof LocalVarInstruction) {
                if (insn.getLocal().isLoad()) {
                    QTemporary  var = env.getLocalVar(insn);

                    stack.push(var);
                } else {
                    QOrigin value = (QOrigin)stack.pop();
                    QTemporary  var = env.getLocalVar(insn);

                    vect.addElement(new QQuadruple(var, value));
                }
            } else if (insn.getInstruction() instanceof PushLiteralInstruction) {
                stack.push(new QLiteral(insn));
            } else if (insn.isPop()) {
                vect.addElement(new QQuadruple(new QTemporary(1), (QOrigin)stack.pop()));
            } else if (insn.isDup()) {
                Object  temp1 = stack.pop();

                stack.push(temp1);
                stack.push(((QOrigin)temp1).duplicate());
            } else if (insn.isDupX2()) {
                Object  temp1 = stack.pop();
                Object  temp2 = stack.pop();

                stack.push(temp2);
                stack.push(temp1);
                stack.push(((QOrigin)temp2).duplicate());
            } else if (insn.isSwap()) {
                Object  temp1 = stack.pop();
                Object  temp2 = stack.pop();

                stack.push(temp1);
                stack.push(temp2);
            } else if (insn.getInstruction() instanceof IincInstruction) {
                vect.addElement(new QIinc(insn.getInstruction(),
                                          env.getLocalVar(((IincInstruction)insn.getInstruction()).getVariable())));
            } else {
                // OPER
                QOperand[]  operands = readOperands(stack, insn.countOperands());

                if (insn.isJump()) {
                    vect.addElement(new QJump(new QOperator(insn, operands)));
                } else if (!insn.hasReturnValue()) {
                    vect.addElement(new QVoid(new QOperator(insn, operands)));
                } else {
                    if (i != insns.length - 1 &&
                        insns[i + 1].getInstruction() instanceof LocalVarInstruction &&
                        insns[i + 1].getLocal().isStore()) {
                        QOperator   oper = new QOperator(insn, operands);
                        QTemporary  temp = env.getLocalVar(insns[i + 1]);

                        vect.addElement(new QQuadruple(temp, oper));
                        i++;
                    } else {
                        QOperator   oper = new QOperator(insn, operands);
                        QTemporary  temp = new QTemporary(oper.getType());

                        vect.addElement(new QQuadruple(temp, oper));
                        stack.push(temp);
                    }
                }
            }
        }

        int     pos = 0;
        QNode   last = vect.size() == 0 ?
            null
            : (QNode)vect.elementAt(vect.size() - 1);
        if (last instanceof QJump) {
            vect.setSize(vect.size() - 1);
        }

        QTemporary[] remainingStack = new QTemporary[stack.size()];
        while (stack.size() > 0) {
            QOrigin value = (QOrigin)stack.pop();

            if (value instanceof QTemporary) {
                remainingStack[pos++] = (QTemporary)value;
            } else {
                remainingStack[pos] = new QTemporary(value.getType());
                vect.addElement(new QQuadruple(remainingStack[pos++], value));
            }
        }
        if (last instanceof QJump) {
            vect.addElement(last);
        }

        return remainingStack;
    }

    private static QOperand[] readOperands(Stack<Object> stack, int size) {
        Vector  vect = new Vector();

        for (int i = 0; size > 0; i++) {
            QOperand    oper = (QOperand)stack.pop();

            size -= InstructionHandle.getTypeSize(oper.getType());
            vect.addElement(oper);
        }

        return (QOperand[])at.dms.util.Utils.toArray(vect, QOperand.class);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private QDestination        dest;
    private QOrigin     origin;
}

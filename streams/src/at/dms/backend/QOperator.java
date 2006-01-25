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
 * $Id: QOperator.java,v 1.2 2006-01-25 17:00:34 thies Exp $
 */

package at.dms.backend;

import java.util.Vector;

import at.dms.util.InconsistencyException;

/**
 * This class represents the origin of a quadruple
 */
class QOperator implements QOrigin {

    public QOperator(InstructionHandle operator, QOrigin[] operands) {
        this.operator = operator;
        this.operands = operands;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Human readable form
     */
    public String toString() {
        String  s = at.dms.classfile.OpcodeNames.getName(operator.getOpcode()) + " {";

        for (int i = operands.length - 1; i >= 0; i--) {
            s += operands[i] + (i != 0 ? ", " : "");
        }

        return s + "}";
    }

    /**
     * The type of this instruction
     */
    public int getType() {
        return operator.getInstruction().getReturnType();
    }

    /**
     * Returns the primitive instruction
     */
    public InstructionHandle getInstruction() {
        return operator;
    }

    /**
     * Duplicate this node
     */
    public QOrigin duplicate() {
        throw new InconsistencyException("NYI");
    }

    // ----------------------------------------------------------------------
    // ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Returns the used temporaries.
     */
    public QTemporary[] getUses() {
        Vector  vect = new Vector();

        for (int i = 0; i < operands.length; i++) {
            if ((operands[i] instanceof QTemporary) && (!vect.contains(operands[i]))) {
                vect.addElement(operands[i]);
            }
        }

        return (QTemporary[])at.dms.util.Utils.toArray(vect, QTemporary.class);
    }

    /**
     * returns the parameters of this instruction
     */
    public QOrigin[] getOrigins() {
        return operands;
    }

    /**
     * Sets the parameters of this instruction
     */
    public void setOrigin(QOrigin origin, int i) {
        operands[i] = origin;
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Generates instructions for this quadruple
     * @param   seq     The code sequence of instruction
     */
    public void generate(CodeSequence seq) {
        for (int i = operands.length - 1; i >= 0; i--) {
            operands[i].generate(seq);
        }
        seq.plantInstruction(operator);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private InstructionHandle   operator;
    private QOrigin[]       operands;
}

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
 * $Id: QIinc.java,v 1.2 2006-01-25 17:00:34 thies Exp $
 */

package at.dms.backend;

import at.dms.classfile.Instruction;
import at.dms.classfile.IincInstruction;
import at.dms.util.InconsistencyException;

/**
 * This class represents a quadruple: a source and a destination
 */
class QIinc extends QNode {

    public QIinc(Instruction insn, QTemporary temp) {
        this.iinc = (IincInstruction)insn;
        this.temp = temp;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Human readable form
     */
    public String toString() {
        return "iinc " + temp;
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

    // ----------------------------------------------------------------------
    // ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Returns the defined temporary.
     */
    public QTemporary getDef() {
        return temp;
    }

    /**
     * Returns the used temporaries.
     */
    public QTemporary[] getUses() {
        return new QTemporary[]{temp};
    }

    /**
     * returns the parameters of this instruction
     */
    public QOrigin[] getOrigins() {
        return new QOrigin[0]; // !!! share
    }

    /**
     * Sets the parameters of this instruction
     */
    public void setOrigin(QOrigin origin, int i) {
        throw new InconsistencyException();
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Generates instructions for this quadruple
     * @param   seq     The code sequence of instruction
     */
    public void generate(CodeSequence seq) {
        seq.plantInstruction(new IincInstruction(temp.getRegister(), iinc.getIncrement()));
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private IincInstruction iinc;
    private QTemporary      temp;
}

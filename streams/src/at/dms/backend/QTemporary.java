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
 * $Id: QTemporary.java,v 1.2 2006-01-25 17:00:34 thies Exp $
 */

package at.dms.backend;

import at.dms.classfile.LocalVarInstruction;
import at.dms.classfile.NoArgInstruction;
import at.dms.classfile.Constants;
import at.dms.util.InconsistencyException;

/**
 * This class represents a temporary
 */
class QTemporary extends QOperand implements QDestination {

    QTemporary(int type) {
        this.type = type;
        this.pos = _count++; //  for trace
    }

    QTemporary(int type, int precolor) {
        this(type);
        this.precolor = precolor; // is it a parameter, we will see after...
    }

    // ----------------------------------------------------------------------
    // ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Returns the defined temporary.
     */
    public QTemporary getDef() {
        return null;
    }

    /**
     * Returns the used temporaries.
     */
    public QTemporary[] getUses() {
        return new QTemporary[]{this};
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Returns the size (the number of slot) of this temporary
     */
    public int getSize() {
        return InstructionHandle.getTypeSize(type);
    }

    /**
     * Returns the register number of this temporary
     */
    public int getRegister() {
        return register;
    }

    /**
     * Returns the register number of this temporary
     */
    public int getPrecolor() {
        return usePrecolor && Integer.MAX_VALUE != precolor ? precolor : UNINITIALIZED;
    }

    /**
     * Enforces the precolor
     */
    public void enforceColor() {
        usePrecolor = true;
    }

    /**
     * The size (in instruction) of the subtree
     */
    public int getType() {
        return type;
    }

    /**
     * Human readable form
     */
    public String toString() {
        int     name = register == UNINITIALIZED ? pos : register;

        if (precolor != Integer.MAX_VALUE) {
            if (usePrecolor) {
                return "P_" + precolor;
            } else {
                return "L_" + precolor;
            }
        } else {
            return "t_" + name;
        }
    }

    /**
     * Duplicate this node
     */
    public QOrigin duplicate() {
        return this;
    }

    // ----------------------------------------------------------------------
    // LIVENESS ANALYSIS
    // ----------------------------------------------------------------------

    /**
     * Uses a temporary
     */
    public void use(BasicBlock block, int pos) {
        useBlock = block;
        usePos = pos;
        countUses++;
    }

    /**
     * Uses a temporary
     */
    public void def(BasicBlock block, int pos) {
        defBlock = block;
        defPos = pos;
    }

    /**
     * Returns the number of uses
     */
    public int getUseCount() {
        return countUses;
    }

    /**
     * Sets position
     */
    public void setPosition(int pos) {
        this.pos = pos;
    }

    /**
     * Gets position
     */
    public int getPosition() {
        return pos;
    }

    // ----------------------------------------------------------------------
    // REGISTER ALLOCATION
    // ----------------------------------------------------------------------

    public void setRegister(int reg) {
        if (countUses == 0) {
            register = UNUSED;
        } else {
            register = reg;
        }
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Generates instructions for this quadruple
     * @param   seq     The code sequence of instruction
     */
    public void generate(CodeSequence seq) {
        int     opcode;

        if (register != UNUSED) {
            switch (getType()) {
            case Constants.TYP_REFERENCE:
                opcode = Constants.opc_aload;
                break;
            case Constants.TYP_DOUBLE:
                opcode = Constants.opc_dload;
                break;
            case Constants.TYP_FLOAT:
                opcode = Constants.opc_fload;
                break;
            case Constants.TYP_INT:
                opcode = Constants.opc_iload;
                break;
            case Constants.TYP_LONG:
                opcode = Constants.opc_lload;
                break;
            default:
                throw new InconsistencyException();
            }

            seq.plantInstruction(new LocalVarInstruction(opcode, register));
        } else if (countUses == 0) {
            throw new InconsistencyException();
        }
    }

    /**
     * Generates instructions for destination
     * @param   seq     The code sequence of instruction
     */
    public void store(CodeSequence seq, boolean isLive) {
        int     opcode;

        if (register != UNUSED && isLive) {
            switch (getType()) {
            case Constants.TYP_REFERENCE:
                opcode = Constants.opc_astore;
                break;
            case Constants.TYP_DOUBLE:
                opcode = Constants.opc_dstore;
                break;
            case Constants.TYP_FLOAT:
                opcode = Constants.opc_fstore;
                break;
            case Constants.TYP_INT:
                opcode = Constants.opc_istore;
                break;
            case Constants.TYP_LONG:
                opcode = Constants.opc_lstore;
                break;
            default:
                throw new InconsistencyException();
            }

            seq.plantInstruction(new LocalVarInstruction(opcode, register));
        } else if (countUses == 0 || !isLive) {
            int size = InstructionHandle.getTypeSize(type);

            seq.plantInstruction(new NoArgInstruction(size == 1 ? Constants.opc_pop : Constants.opc_pop2));
        }
    }

    // ----------------------------------------------------------------------
    // PRIVATE DATA TYPE
    // ----------------------------------------------------------------------

    private int     type;
    private BasicBlock  defBlock;
    private int     defPos;
    private BasicBlock  useBlock;
    private int     usePos;
    private int     countUses;

    private int     register = UNINITIALIZED;

    private int     precolor = Integer.MAX_VALUE;
    private boolean usePrecolor;

    private int     pos;
    private static int  _count;

    public static final int UNINITIALIZED = -1;
    public static final int UNUSED = -2;

    public static final QTemporary[]    EMPTY = new QTemporary[0];
}

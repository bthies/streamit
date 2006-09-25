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
 * $Id: SwitchInstruction.java,v 1.3 2006-09-25 13:54:31 dimock Exp $
 */

package at.dms.classfile;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Vector;

import at.dms.util.InconsistencyException;
import at.dms.util.Utils;

/**
 * Some instructions are perniticky enough that its simpler
 * to write them separately instead of smushing them with
 * all the rest. the multiarray instruction is one of them.
 */
public class SwitchInstruction extends Instruction implements AccessorContainer {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs a switch instruction
     *
     * @param   opcode      the opcode of the instruction
     * @param   deflab      default target for switch
     * @param   matches     array of match values to compare the key to
     * @param   targets     array of target instructions for each match value
     */
    public SwitchInstruction(int opcode,
                             InstructionAccessor deflab,
                             int[] matches,
                             InstructionAccessor[] targets)
    {
        super(opcode);

        this.selected = false;  // no switch type selection done
        this.deflab = deflab;
        this.matches = matches;
        this.targets = targets;
    }

    /**
     * Constructs a switch instruction
     *
     * @param   deflab      default target for switch
     * @param   matches     array of match values to compare the key to
     * @param   targets     array of target instructions for each match value
     */
    public SwitchInstruction(InstructionAccessor deflab,
                             int[] matches,
                             InstructionAccessor[] targets)
    {
        // actual instruction not known here, will be determined later
        this(opc_tableswitch, deflab, matches, targets);
    }

    /**
     * Constructs a switch instruction
     *
     * @param   deflab      default target for switch
     * @param   matches     vector of match values to compare the key to
     * @param   targets     vector of target instructions for each match value
     */
    public SwitchInstruction(InstructionAccessor deflab,
                             Vector<Integer> matches,
                             Vector targets)
    {
        this(deflab,
             Utils.toIntArray(matches),
             (InstructionAccessor[])Utils.toArray(targets, InstructionAccessor.class));
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns true iff control flow can reach the next instruction
     * in textual order.
     */
    public boolean canComplete() {
        return false;
    }

    /**
     * Transforms targets (deferences to actual instructions).
     */
    public void transformAccessors(AccessorTransformer transformer) throws BadAccessorException {
        this.deflab = this.deflab.transform(transformer, this);
        for (int i = 0; i < targets.length; i++) {
            this.targets[i] = this.targets[i].transform(transformer, this);
        }
    }

    /**
     * Sets the target for this instruction
     */
    public void setTarget(InstructionAccessor target, int position) {
        if (selected) {
            throw new InconsistencyException("switch type already determined");
        }

        if (position == -1) {
            this.deflab = target;
        } else {
            this.targets[position] = target;
        }
    }

    /**
     * Gets the number of 'case'
     */
    public int getSwitchCount() {
        return matches.length;
    }

    /**
     * Returns the case's value at a position
     */
    public int getMatch(int position) {
        return matches[position];
    }

    /**
     * Returns the target at a position
     */
    public InstructionAccessor getTarget(int position) {
        return (position == -1) ? deflab : targets[position];
    }

    /**
     * Returns the target at a position
     */
    public void setTarget(int position, InstructionAccessor accessor) {
        if (position == -1) {
            deflab = accessor;
        } else {
            targets[position] = accessor;
        }
    }

    /**
     * Select the appropriate switch type (tableswitch or lookupswitch)
     */
    public void selectSwitchType() {
        int[]           matches = this.matches;
        InstructionAccessor[]   targets = this.targets;
        int             count  = 0;
        int             minval = 0;
        int             maxval = 0;

        for (int i = 0; i < matches.length; i++) {
            // ignore matches with default target
            if (targets[i] == deflab) {
                continue;
            }

            if (count == 0) {
                minval = matches[i];
                maxval = matches[i];
            } else {
                if (matches[i] < minval) {
                    minval = matches[i];
                }
                if (matches[i] > maxval) {
                    maxval = matches[i];
                }
            }

            count += 1;
        }

        if (count == 0) {
            this.matches = new int[0];
            this.targets = new InstructionAccessor[0];

            setOpcode(opc_lookupswitch);    // it uses less size
            selected = true;
        } else {
            // use long operations to avoid overflows
            long    distance = (long)0 + maxval - minval + 1;

            // choose the switch type: tableswitch if density >= 0.5
            if (distance <= 2 * count) {
                int length = (int)distance;

                // build arrays and initialise them
                // Note: the matches array is not really needed (only the first value)
                this.matches = new int[length];
                this.targets = new InstructionAccessor[length];

                for (int i = 0; i < this.matches.length; i++) {
                    this.matches[i] = minval + i;
                    this.targets[i] = deflab;
                }

                for (int i = 0; i < matches.length; i++) {
                    // ignore matches with default target
                    if (targets[i] == deflab) {
                        continue;
                    }

                    this.targets[matches[i] - minval] = targets[i];
                }

                setOpcode(opc_tableswitch);
                selected = true;
            } else {
                // build arrays
                this.matches = new int[count];
                this.targets = new InstructionAccessor[count];

                for (int i = 0, j = 0; i < matches.length; i++) {
                    // ignore matches with default target
                    if (targets[i] == deflab) {
                        continue;
                    }

                    this.matches[j] = matches[i];
                    this.targets[j] = targets[i];
                    j += 1;
                }

                setOpcode(opc_lookupswitch);
                selected = true;
            }
        }
    }

    /**
     * Returns the number of bytes used by the the instruction in the code array.
     */
    /*package*/ int getSize() {
        return computeSize(getAddress());
    }

    // --------------------------------------------------------------------
    // CHECK CONTROL FLOW
    // --------------------------------------------------------------------

    /**
     * Returns the type pushed on the stack
     */
    public byte getReturnType() {
        return TYP_REFERENCE;
    }

    /**
     * Verifies the enclosed instruction and computes the stack height.
     *
     * @param   env         the check environment
     * @param   curStack        the stack height at the end
     *                  of the execution of the instruction
     * @return  true iff the next instruction in textual order needs to be
     *      checked, i.e. this instruction has not been checked before
     *      and it can complete normally
     * @exception   ClassFileFormatException    a problem was detected
     */
    /*package*/ void check(CodeEnv env, int curStack)
        throws ClassFileFormatException
    {
        env.checkExecutionPath((InstructionHandle)deflab, curStack);
        for (int i = 0; i < targets.length; i++) {
            env.checkExecutionPath((InstructionHandle)targets[i], curStack);
        }
    }

    /**
     * Computes the address of the end of the instruction.
     *
     * @param   position    the minimum and maximum address of the
     *              begin of this instruction. This parameter
     *              is changed to the minimum and maximum
     *              address of the end of this instruction.
     */
    /*package*/ void computeEndAddress(CodePosition position) {
        int     size = computeSize(position.max);
        position.min += size;
        position.max += size;
    }

    /**
     * Returns the size of data pushed on the stack by this instruction
     */
    public int getPushedOnStack() {
        return 0;
    }

    /**
     * Return the amount of stack (positive or negative) used by this instruction
     */
    public int getStack() {
        return -1;
    }

    // --------------------------------------------------------------------
    // WRITE
    // --------------------------------------------------------------------

    /**
     * Insert or check location of constant value on constant pool
     *
     * @param   cp      the constant pool for this class
     */
    /*package*/ void resolveConstants(ConstantPool cp) {}

    /**
     * Write this instruction into a file
     *
     * @param   cp      the constant pool that contain all data
     * @param   out     the file where to write this object info
     *
     * @exception   java.io.IOException an io problem has occured
     */
    /*package*/ void write(ConstantPool cp, DataOutput out) throws IOException {
        out.writeByte((byte)getOpcode());

        // pad to next 4-byte boundary
        for (int i = 0; i < pad; i++) {
            out.writeByte(0);
        }

        // default
        out.writeInt(((Instruction)deflab).getAddress() - getAddress());

        if (!selected) {
            throw new InconsistencyException("switch type not yet determined");
        }

        if (getOpcode() == opc_tableswitch) {
            out.writeInt(matches[0]);
            out.writeInt(matches[0] + targets.length - 1);

            for (int i = 0; i < targets.length; i++) {
                out.writeInt(((Instruction)targets[i]).getAddress() - getAddress());
            }
        } else if (getOpcode() == opc_lookupswitch) {
            out.writeInt(targets.length);
            sort(matches, targets);
            for (int i = 0; i < targets.length; i++) {
                out.writeInt(matches[i]);
                out.writeInt(((Instruction)targets[i]).getAddress() - getAddress());
            }
        } else {
            throw new InconsistencyException("unexpected opcode: " + getOpcode());
        }
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    /**
     * Compute the size of this instruction
     */
    private int computeSize(int start) {
        if (! selected) {
            selectSwitchType();
        }

        if (getOpcode() == opc_tableswitch) {
            int     size = 1 + 12;                // 1+padding+4+4+4+jmptable...

            if (((start + 1) % 4) == 0) {
                pad = 0;
            } else {
                pad = 4 - ((start + 1) % 4);
            }

            size += pad;

            size += 4*targets.length;

            return size;
        } else if (getOpcode() == opc_lookupswitch) {
            int     size = 1 + 8;           // 1+padding+4+4+jumptable

            if (((start + 1) % 4) == 0) {
                pad = 0;
            } else {
                pad = 4 - ((start + 1) % 4);
            }

            size += pad;

            if (targets != null) {
                size += 8*targets.length;
            }

            return size;
        } else {
            throw new InconsistencyException("unexpected opcode: " + getOpcode());
        }
    }

    /**
     * Bubble sort an array
     */
    private static final void sort(int[] matches, InstructionAccessor[] instrs) {
        for (int i = matches.length; --i >= 0;) {
            for (int j = 0; j < i; j++) {
                if (matches[j] > matches[j + 1]) {
                    int         tmpMatch = matches[j];
                    InstructionAccessor tmpInstr = instrs[j];

                    instrs[j] = instrs[j+1];
                    matches[j] = matches[j+1];
                    instrs[j+1] = tmpInstr;
                    matches[j+1] = tmpMatch;
                }
            }
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    /* switch type selected ? */
    private boolean         selected;
    private int             pad;
    private InstructionAccessor     deflab;
    private int[]               matches;
    private InstructionAccessor[]       targets;
}

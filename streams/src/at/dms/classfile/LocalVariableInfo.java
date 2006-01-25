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
 * $Id: LocalVariableInfo.java,v 1.2 2006-01-25 17:00:39 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * VMS 4.7.9: Local Variable Table Attribute.
 *
 * This attribute represents a mapping between he Java Virtual Machine code
 * array and the line number in the original Java source file
 */
public class LocalVariableInfo implements AccessorContainer {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Create an entry in the line number table
     *
     * @param   start       the beginning of the area (inclusive) where the name is valid
     * @param   end     the end of the area (inclusive) where the name is valid
     * @param   name        the name of the variable
     * @param   type        the type signature of the variable
     * @param   slot        the index in the method's local variables
     */
    public LocalVariableInfo(InstructionAccessor start,
                             InstructionAccessor end,
                             String name,
                             String type,
                             short slot) {
        this.start = start;
        this.end = end;
        this.name = new AsciiConstant(name);
        this.type = new AsciiConstant(type);
        this.slot = slot;
    }

    /**
     * Create an entry in the line number table from a class file stream
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     * @param   insns       (sparse) array of instructions
     *
     * @exception   java.io.IOException an io problem has occured
     */
    public LocalVariableInfo(DataInput in, ConstantPool cp, Instruction[] insns)
        throws IOException
    {
        int     startPC = in.readUnsignedShort();
        int     length = in.readUnsignedShort();

        this.start = insns[startPC];

        // find beginning last instruction where variable is in scope
        int     endPC = startPC + length - 1;
        while (insns[endPC] == null) {
            endPC -= 1;
        }
        this.end = insns[endPC];

        this.name = (AsciiConstant)cp.getEntryAt(in.readUnsignedShort());
        this.type = (AsciiConstant)cp.getEntryAt(in.readUnsignedShort());
        this.slot = (short)in.readUnsignedShort();
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Transforms targets (deferences to actual instructions).
     */
    public void transformAccessors(AccessorTransformer transformer) throws BadAccessorException {
        this.start = this.start.transform(transformer, this);
        this.end = this.end.transform(transformer, this);
    }

    /**
     * Returns the name of the local variable
     */
    public String getName() {
        return name.getValue();
    }

    /**
     * Returns the type of the local variable
     */
    public String getType() {
        return type.getValue();
    }

    /**
     * Sets the start of the protected area
     */
    public void setStart(InstructionAccessor start) {
        this.start = start;
    }

    /**
     * Returns the start of the protected area
     */
    public InstructionAccessor getStart() {
        return start;
    }

    /**
     * Sets the end of the protected area
     */
    public void setEnd(InstructionAccessor end) {
        this.end = end;
    }

    /**
     * Returns the end of the protected area
     */
    public InstructionAccessor getEnd() {
        return end;
    }

    /**
     * Returns the index in the method's local variables
     */
    public short getSlot() {
        return slot;
    }

    // --------------------------------------------------------------------
    // WRITE
    // --------------------------------------------------------------------

    /**
     * Insert or check location of constant value on constant pool
     *
     * @param   cp      the constant pool for this class
     */
    /*package*/ void resolveConstants(ConstantPool cp) {
        cp.addItem(name);
        cp.addItem(type);
    }

    /**
     * Write this class into the the file (out) getting data position from
     * the constant pool
     *
     * @param   cp      the constant pool that contain all data
     * @param   out     the file where to write this object info
     *
     * @exception   java.io.IOException an io problem has occured
     */
    /*package*/ void write(ConstantPool cp, DataOutput out) throws IOException {
        out.writeShort((short)((Instruction)start).getAddress());
        out.writeShort((short)((((Instruction)end).getAddress() + ((Instruction)end).getSize())
                               - ((Instruction)start).getAddress()));
        out.writeShort(name.getIndex());
        out.writeShort(type.getIndex());
        out.writeShort(slot);
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private InstructionAccessor     start;
    private InstructionAccessor     end;
    private AsciiConstant           name;
    private AsciiConstant           type;
    private short               slot;
}

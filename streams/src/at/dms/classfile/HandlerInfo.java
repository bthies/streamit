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
 * $Id: HandlerInfo.java,v 1.3 2006-03-23 18:18:41 dimock Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * This class represents a protected (try) portion of block with
 * an handler (catch) for a specified type of exception
 */
public class HandlerInfo implements AccessorContainer {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs a new exception handler info entry
     *
     * @param   start       the beginning of the checked area (inclusive)
     * @param   end     the end of the checked area (inclusive !)
     * @param   handler     the entrypoint into the exception handling routine.
     * @param   thrown      the exceptions handled by this routine
     */
    public HandlerInfo(InstructionAccessor start,
                       InstructionAccessor end,
                       InstructionAccessor handler,
                       String thrown)
    {
        this.start = start;
        this.end = end;
        this.handler = handler;
        this.thrown = thrown == null ? null : new ClassConstant(thrown);
    }

    /**
     * Constructs a new exception handler info entry from a class file
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     * @param   insns       (sparse) array of instructions
     *
     * @exception   java.io.IOException an io problem has occured
     */
    public HandlerInfo(DataInput in, ConstantPool cp, Instruction[] insns)
        throws IOException
    {
        this.start = insns[in.readUnsignedShort()];

        // find last instruction of checked area
        int     end = in.readUnsignedShort() - 1;
        while (insns[end] == null) {
            end -= 1;
        }
        this.end = insns[end];

        this.handler = insns[in.readUnsignedShort()];

        int     thrown = in.readUnsignedShort();
        this.thrown = thrown == 0 ? null : (ClassConstant)cp.getEntryAt(thrown);
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
        this.handler = this.handler.transform(transformer, this);
    }


    /*
     * Sets the start of the protected area
     */
    public void setStart(InstructionAccessor start) {
        this.start = start;
    }

    /*
     * Returns the start of the protected area
     */
    public InstructionAccessor getStart() {
        return start;
    }

    /*
     * Sets the end of the protected area
     */
    public void setEnd(InstructionAccessor end) {
        this.end = end;
    }

    /*
     * Returns the end of the protected area
     */
    public InstructionAccessor getEnd() {
        return end;
    }

    /*
     * Sets the handler of the protected area
     */
    public void setHandler(InstructionAccessor handler) {
        this.handler = handler;
    }

    /*
     * Returns the handler of the protected area
     */
    public InstructionAccessor getHandler() {
        return handler;
    }

    /*
     * Returns the thrown of the protected area
     */
    public String getThrown() {
        return thrown == null ? null : thrown.getName();
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
        if (thrown != null) {
            cp.addItem(thrown);
        }
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
        out.writeShort((short)(((Instruction)end).getAddress() + ((Instruction)end).getSize()));
        out.writeShort((short)((Instruction)handler).getAddress());
        out.writeShort((short)(thrown == null ? 0 : thrown.getIndex()));
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private InstructionAccessor     start;
    private InstructionAccessor     end;
    private InstructionAccessor     handler;
    private ClassConstant           thrown;
}

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
 * $Id: LineNumberTable.java,v 1.2 2006-01-25 17:00:39 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * VMS 4.7.8: Line Number Table Attribute.
 *
 * This attribute represents a mapping between he Java Virtual Machine code
 * array and the line number in the original Java source file
 */
public class LineNumberTable extends Attribute {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Create a line number table attribute.
     */
    public LineNumberTable(LineNumberInfo[] entries) {
        this.entries = entries;
    }

    /**
     * Constructs a line number table attribute from a class file stream.
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     * @param   insns       (sparse) array of instructions
     *
     * @exception   java.io.IOException an io problem has occured
     */
    public LineNumberTable(DataInput in, ConstantPool cp, Instruction[] insns)
        throws IOException
    {
        in.readInt();   // ignore

        this.entries = new LineNumberInfo[in.readUnsignedShort()];
        for (int i = 0; i < this.entries.length; i++) {
            this.entries[i] = new LineNumberInfo(in, cp, insns);
        }
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the attribute's tag
     */
    /*package*/ int getTag() {
        return Constants.ATT_LINENUMBERTABLE;
    }

    /**
     * Returns the space in bytes used by this attribute in the classfile
     */
    /*package*/ int getSize() {
        return 2 + 4 + 2 + 4*entries.length;
    }

    /**
     * Returns line number information
     */
    /*package*/ LineNumberInfo[] getLineNumbers() {
        return entries;
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
        cp.addItem(attr);
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
        out.writeShort(attr.getIndex());
        out.writeInt(2 + 4 * entries.length);
        out.writeShort(entries.length);
        for (int i = 0; i < entries.length; i++) {
            entries[i].write(out);
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private static AsciiConstant        attr = new AsciiConstant("LineNumberTable");
    private LineNumberInfo[]        entries;
}

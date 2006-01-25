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
 * $Id: SyntheticAttribute.java,v 1.2 2006-01-25 17:00:39 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * VMS 4.7.6 : Synthetic Attribute.
 *
 * This attribute marks fields and methods synthesized by the compiler
 * in order to implement the scoping of names.
 *
 * At present,the length of this must be zero.
 */
public class SyntheticAttribute extends Attribute {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Create a synthetic attribute.
     */
    public SyntheticAttribute() {}

    /**
     * Constructs a synthetic attribute from a class file stream.
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   java.io.IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to
     *                  write a bad classfile info
     */
    public SyntheticAttribute(DataInput in, ConstantPool cp)
        throws IOException, ClassFileFormatException
    {
        if (in.readInt() != 0) {
            throw new ClassFileFormatException("bad attribute length");
        }
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the attribute's tag
     */
    /*package*/ int getTag() {
        return Constants.ATT_SYNTHETIC;
    }

    /**
     * Returns the space in bytes used by this attribute in the classfile
     */
    /*package*/ int getSize() {
        return 2 + 4;
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
        out.writeInt(0);
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private static AsciiConstant        attr = new AsciiConstant("Synthetic");
}

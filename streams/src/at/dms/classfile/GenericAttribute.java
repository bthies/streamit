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
 * $Id: GenericAttribute.java,v 1.2 2006-01-25 17:00:39 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * This is an opaque attribute that lets you add an uninterpreted
 * stream of bytes into an attribute in a class file. This can be
 * used (for instance) to embed versioning or signatures into the
 * class file or method.
 */
public class GenericAttribute extends Attribute {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Make up a new attribute
     *
     * @param   name        Name to be associated with the attribute
     * @param   data        stream of bytes to be placed with the attribute
     */
    public GenericAttribute(String name, byte[] data) {
        this.name = new AsciiConstant(name);
        this.data = data;
    }

    /**
     * Make up a new attribute
     *
     * @param   name        the attribute's name
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   java.io.IOException an io problem has occured
     */
    public GenericAttribute(AsciiConstant name, DataInput in, ConstantPool cp)
        throws IOException
    {
        this.name = name;

        this.data = new byte[in.readInt()];
        in.readFully(this.data);
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the attribute's tag
     */
    /*package*/ int getTag() {
        return Constants.ATT_GENERIC;
    }

    /**
     * Returns the space in bytes used by this attribute in the classfile
     */
    /*package*/ int getSize() {
        return 2 + 4 + data.length;
    }

    /**
     * Returns the attribute's name
     */
    /*package*/ String getName() {
        return name.getValue();
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
        out.writeShort(name.getIndex());
        out.writeInt(data.length);
        out.write(data);
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private AsciiConstant       name;
    private byte[]      data;
}

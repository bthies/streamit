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
 * $Id: ExceptionsAttribute.java,v 1.2 2006-01-25 17:00:39 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * VMS 4.7.4: Exceptions Attribute.
 *
 * This attribute indicates which checked exceptions a method may throw.
 */
public class ExceptionsAttribute extends Attribute {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Create an exceptions attribute.
     */
    public ExceptionsAttribute(String[] names) {
        exceptions = new ClassConstant[names.length];

        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i] = new ClassConstant(names[i]);
        }
    }

    /**
     * Constructs a exceptions attribute from a class file stream.
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   java.io.IOException an io problem has occured
     */
    public ExceptionsAttribute(DataInput in, ConstantPool cp)
        throws IOException
    {
        in.readInt();   // ignore the length

        exceptions = new ClassConstant[in.readUnsignedShort()];
        for (int i = 0; i < exceptions.length; i++) {
            exceptions[i] = (ClassConstant)cp.getEntryAt(in.readUnsignedShort());
        }
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the attribute's tag
     */
    /*package*/ int getTag() {
        return Constants.ATT_EXCEPTIONS;
    }

    /**
     * Returns the space in bytes used by this attribute in the classfile
     */
    /*package*/ int getSize() {
        return 2 + 4 + 2 + 2*exceptions.length;
    }

    /**
     * Returns the exceptions
     *
     */
    /*package*/ String[] getExceptions() {
        String[]    names = new String[exceptions.length];

        for (int i = 0; i < exceptions.length; i++) {
            names[i] = exceptions[i].getName();
        }

        return names;
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

        for (int i = 0; i < exceptions.length; i++) {
            cp.addItem(exceptions[i]);
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
        out.writeShort(attr.getIndex());
        out.writeInt(exceptions.length*2 + 2);
        out.writeShort(exceptions.length);
        for (int i = 0; i < exceptions.length; i++) {
            out.writeShort(exceptions[i].getIndex());
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private static AsciiConstant        attr = new AsciiConstant("Exceptions");
    private ClassConstant[]     exceptions;
}

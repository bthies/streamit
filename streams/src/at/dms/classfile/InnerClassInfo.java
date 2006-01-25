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
 * $Id: InnerClassInfo.java,v 1.2 2006-01-25 17:00:39 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * VMS 4.7.5 : Inner Classes Attribute.
 *
 * This attribute declares the encoding of bytecode names of classes
 * or interfaces which are not package members. It contains an array of
 * records, one for each encoded name.
 */
public class InnerClassInfo implements Constants {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Create an entry in the inner classes table.
     *
     * @param   innerClass  the encoded name of the (inner) class
     * @param   outerClass  the defining scope  of the (inner) class
     * @param   simpleName  the simple name of the (inner) class
     * @param   modifiers   access permission to and properties of the class
     */
    public InnerClassInfo(String innerClass,
                          String outerClass,
                          String simpleName,
                          short modifiers)
    {
        this.innerClass = new ClassConstant(innerClass);
        this.outerClass = outerClass == null ? null : new ClassConstant(outerClass);
        this.simpleName = simpleName == null ? null : new AsciiConstant(simpleName);
        this.modifiers = modifiers;
    }

    /**
     * Creates an entry in the inner classes table from a class file stream.
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to read a bad classfile
     */
    public InnerClassInfo(DataInput in, ConstantPool cp) throws IOException {
        this.innerClass = (ClassConstant)cp.getEntryAt(in.readUnsignedShort());

        int     outerClass = in.readUnsignedShort();
        this.outerClass = outerClass == 0 ? null : (ClassConstant)cp.getEntryAt(outerClass);

        int     simpleName = in.readUnsignedShort();
        this.simpleName = simpleName == 0 ? null : (AsciiConstant)cp.getEntryAt(simpleName);

        this.modifiers = (short)in.readUnsignedShort();
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Return the qualified name of this class
     */
    public String getQualifiedName() {
        return innerClass.getName();
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
        cp.addItem(innerClass);
        if (outerClass != null) {
            cp.addItem(outerClass);
        }
        if (simpleName != null) {
            cp.addItem(simpleName);
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
        out.writeShort(innerClass.getIndex());
        out.writeShort(outerClass == null ? (short)0 : outerClass.getIndex());
        out.writeShort(simpleName == null ? (short)0 : simpleName.getIndex());
        out.writeShort(modifiers & MODIFIER_MASK);
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    /**
     * Valid modifiers for inner class infos.
     *
     * VMS 4.6 : All bits of the inner_class_access_flags item not assigned in
     * Table 4.7 are reserved for future use. They should be set to zero
     * in generated class files.
     */
    private static final int        MODIFIER_MASK =
        ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC
        | ACC_FINAL | ACC_INTERFACE | ACC_ABSTRACT;

    private ClassConstant           innerClass;
    private ClassConstant           outerClass;
    private AsciiConstant           simpleName;
    private short               modifiers;
}

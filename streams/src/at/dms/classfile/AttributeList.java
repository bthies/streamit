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
 * $Id: AttributeList.java,v 1.2 2006-01-25 17:00:38 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;
import java.util.Vector;

import at.dms.util.Utils;

/**
 * VMS 4.7: Attributes.
 *
 */
/*package*/ class AttributeList {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs an empty attribute list
     */
    AttributeList() {
        attributes = Attribute.EMPTY;
    }

    /**
     * Constructs a one element attribute list
     */
    AttributeList(Attribute att1) {
        attributes = att1 == null ?
            Attribute.EMPTY :
            new Attribute[]{att1};
    }

    /**
     * Constructs a two elements attribute list
     */
    AttributeList(Attribute att1, Attribute att2) {
        attributes = att1 == null && att2 == null ?
            Attribute.EMPTY : att1 != null && att2 != null ?
            new Attribute[] {att1, att2} :
            new Attribute[] {att1 == null ? att2 : att1};
    }

    /**
     * Constructs a three elements attribute list
     */
    AttributeList(Attribute att1, Attribute att2, Attribute att3) {
        this(att1, att2, att3, null);
    }

    /**
     * Constructs a four elements attribute list
     */
    AttributeList(Attribute att1, Attribute att2, Attribute att3, Attribute att4) {
        int     count = (att1 == null ? 0 : 1) + (att2 == null ? 0 : 1) + (att3 == null ? 0 : 1) +
            (att4 == null ? 0 : 1);

        attributes = new Attribute[count--];

        if (att4 != null) {
            attributes[count--] = att4;
        }
        if (att3 != null) {
            attributes[count--] = att3;
        }
        if (att2 != null) {
            attributes[count--] = att2;
        }
        if (att1 != null) {
            attributes[count--] = att1;
        }
    }

    /**
     * Constructs an attribute list from a class file stream
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     * @param   noCode      do not read code attribute
     *
     * @exception   java.io.IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to
     *                  write a bad classfile info
     */
    AttributeList(DataInput in, ConstantPool cp, boolean noCode)
        throws IOException, ClassFileFormatException
    {
        int     count = in.readUnsignedShort();

        if (count > 0) {
            Vector  attributes = new Vector();

            for (int i = 0; i < count; i += 1) {
                if (noCode) {  
                    attributes.addElement(Attribute.readInterfaceOnly(in, cp));
                } else {
                    attributes.addElement(Attribute.read(in, cp));
                }
            }
            this.attributes = (Attribute[])Utils.toArray(attributes, Attribute.class);
        } else {
            attributes = Attribute.EMPTY;
        }
    }

    /**
     * Constructs an sub-attribute list of CodeInfo from a class file stream
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     * @param   insns       (sparse) array of instructions
     *
     * @exception   java.io.IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to
     *                  write a bad classfile info
     */
    AttributeList(DataInput in, ConstantPool cp, Instruction[] insns)
        throws IOException, ClassFileFormatException
    {
        int     count = in.readUnsignedShort();

        Vector  attributes = new Vector(count);
        for (int i = 0; i < count; i += 1) {
            attributes.addElement(Attribute.readCodeInfoAttribute(in, cp, insns));
        }
        this.attributes = (Attribute[])Utils.toArray(attributes, Attribute.class);
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Return the first attribute for this tag
     *
     * @param   tag     the tag that identifies the attribute
     */
    /*package*/ final Attribute get(int tag) {
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getTag() == tag) {
                return attributes[i];
            }
        }

        return null;
    }

    /**
     * Adds an attribute to the list of attributes
     *
     * @param   attr        the attribute to add to the list
     */
    /*package*/ final void add(Attribute attr) {
        // if an attribute with same tag exists, replace it
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getTag() == attr.getTag()) {
                attributes[i] = attr;
                return;
            }
        }

        // add the new attribute at the end of the list
        Attribute[] temp = new Attribute[attributes.length + 1];
        System.arraycopy(attributes, 0, temp, 0, attributes.length);
        attributes = temp;
        attributes[attributes.length - 1] = attr;
    }

    /**
     * Removes an attribute from the list of attributes
     *
     * @param   tag     the tag that identifies the attribute
     * @return  true iff an attribute with given tag was in the list
     */
    /*package*/ final boolean remove(int tag) {
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i].getTag() == tag) {
                Attribute[] temp = new Attribute[attributes.length - 1];

                System.arraycopy(attributes, 0, temp, 0, i);
                System.arraycopy(attributes, i + 1, temp, i, attributes.length - i);
                attributes = temp;

                return true;
            }
        }

        return false;
    }

    /**
     * Returns the space in bytes used by this attribute in the classfile
     */
    /*package*/ int getSize() {
        int     size = 0;

        for (int i = 0; i < attributes.length; i++) {
            size += attributes[i].getSize();
        }

        return size;
    }

    // --------------------------------------------------------------------
    // WRITE
    // --------------------------------------------------------------------

    /**
     * Insert or check location of constant values in constant pool
     *
     * @param   cp      the constant pool for this class
     */
    /*package*/ void resolveConstants(ConstantPool cp) throws ClassFileFormatException {
        for (int i = 0; i < attributes.length; i++) {
            attributes[i].resolveConstants(cp);
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
     * @exception   ClassFileFormatException    attempt to
     *                  write a bad classfile info
     */
    /*package*/ void write(ConstantPool cp, DataOutput out)
        throws IOException, ClassFileFormatException
    {
        out.writeShort(attributes.length);
        for (int i = 0; i < attributes.length; i++) {
            attributes[i].write(cp, out);
        }
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    private Attribute[]     attributes;
}

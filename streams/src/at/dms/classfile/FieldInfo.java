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
 * $Id: FieldInfo.java,v 1.3 2006-03-23 18:18:41 dimock Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * VMS 4.5: Fields.
 *
 * Each field is described by this structure.
 *
 * Used to make up new field entries. Fields for a class can have
 * an additional 'ConstantValue' attribute associated them,
 * which the java compiler uses to represent things like
 * static final int blah = foo;
 */
public class FieldInfo extends Member {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs a field entry.
     *
     * @param   modifiers   access permission to and properties of the field
     * @param   name        the name of the field
     * @param   type        the type signature
     * @param   value       the value of a constant field that must be
     *              (explicitly or implicitly) static
     * @param   deprecated  is this field deprecated ?
     * @param   synthetic   is this field synthesized by the compiler ?
     */
    public FieldInfo(short modifiers,
                     String name,
                     String type,
                     Object value,
                     boolean deprecated,
                     boolean synthetic) {
        super(modifiers);
        this.name = new AsciiConstant(name);
        this.type = new AsciiConstant(type);

        this.attributes = new AttributeList(value != null ? new ConstantValueAttribute(value) : null,
                                            deprecated ? new DeprecatedAttribute() : null,
                                            synthetic ? new SyntheticAttribute() : null);
    }

    /**
     * Constructs a field entry from a class file stream.
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to read a bad classfile
     */
    public FieldInfo(DataInput in, ConstantPool cp)
        throws IOException, ClassFileFormatException
    {
        setModifiers((short)in.readUnsignedShort());
        this.name = (AsciiConstant)cp.getEntryAt(in.readUnsignedShort());
        this.type = (AsciiConstant)cp.getEntryAt(in.readUnsignedShort());
        this.attributes = new AttributeList(in, cp, false);
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the name of the this field
     */
    public String getName() {
        return name.getValue();
    }

    /**
     * Sets the name of the this field
     */
    public void setName(String name) {
        this.name = new AsciiConstant(name);
    }

    /**
     * Returns the type of the this field
     */
    public String getSignature() {
        return type.getValue();
    }

    /**
     * Returns the type of the this field
     */
    public void getSignature(String type) {
        this.type = new AsciiConstant(type);
    }

    /**
     * Returns the value of the this field
     */
    public Object getConstantValue() {
        Attribute       attr = attributes.get(Constants.ATT_CONSTANTVALUE);

        return attr == null ? null : ((ConstantValueAttribute)attr).getLiteral();
    }

    /**
     * Sets the value of the this field
     */
    public void setConstantValue(Object value) {
        if (value != null) {
            attributes.add(new ConstantValueAttribute(value));
        } else {
            attributes.remove(Constants.ATT_CONSTANTVALUE);
        }
    }

    /**
     * Returns true if the field is deprecated
     */
    public boolean isDeprecated() {
        return attributes.get(Constants.ATT_DEPRECATED) != null;
    }

    /**
     * Sets the deprecated attribute of this field
     */
    public void setDeprecated(boolean deprecated) {
        if (deprecated) {
            attributes.add(new DeprecatedAttribute());
        } else {
            attributes.remove(Constants.ATT_DEPRECATED);
        }
    }

    /**
     * Returns true if the field is synthetic
     */
    public boolean isSynthetic() {
        return attributes.get(Constants.ATT_SYNTHETIC) != null;
    }

    /**
     * Returns true if the field is synthetic
     */
    public void setSynthetic(boolean synthetic) {
        if (synthetic) {
            attributes.add(new SyntheticAttribute());
        } else {
            attributes.remove(Constants.ATT_SYNTHETIC);
        }
    }

    // --------------------------------------------------------------------
    // WRITE
    // --------------------------------------------------------------------

    /**
     * Insert or check location of constant value on constant pool
     *
     * @param   cp      the constant pool for this class
     */
    /*package*/ void resolveConstants(ConstantPool cp) throws ClassFileFormatException {
        cp.addItem(name);
        cp.addItem(type);
        attributes.resolveConstants(cp);
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
        out.writeShort(getModifiers() & MODIFIER_MASK);
        out.writeShort(name.getIndex());
        out.writeShort(type.getIndex());
        attributes.write(cp, out);
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    /**
     * Valid modifiers for fields.
     *
     * VMS 4.5 : All bits of the access_flags item not assigned in
     * Table 4.4 are reserved for future use. They should be set to zero
     * in generated class files.
     */
    private static final int        MODIFIER_MASK =
        ACC_PUBLIC | ACC_PRIVATE | ACC_PROTECTED | ACC_STATIC
        | ACC_FINAL | ACC_VOLATILE | ACC_TRANSIENT;

    private AsciiConstant           name;
    private AsciiConstant           type;
    private AttributeList           attributes;
}

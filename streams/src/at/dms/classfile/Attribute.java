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
 * $Id: Attribute.java,v 1.2 2006-01-25 17:00:38 thies Exp $
 */

package at.dms.classfile;

import java.io.DataInput;
import java.io.IOException;
import java.io.DataOutput;

/**
 * This is the root class of every attribute
 */
public abstract class Attribute {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs an attribute from a class file stream
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   java.io.IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to
     *                  write a bad classfile info
     */
    public static Attribute read(DataInput in, ConstantPool cp)
        throws IOException, ClassFileFormatException
    {
        AsciiConstant   name = (AsciiConstant)cp.getEntryAt(in.readUnsignedShort());

        String      tag = name.getValue();

        switch (tag.charAt(0)) {
        case 'C':
            if (tag.equals("Code")) {
                return new CodeInfo(in, cp);
            } else if (tag.equals("ConstantValue")) {
                return new ConstantValueAttribute(in, cp);
            }
            break;

        case 'D':
            if (tag.equals("Deprecated")) {
                return new DeprecatedAttribute(in, cp);
            }
            break;

        case 'E':
            if (tag.equals("Exceptions")) {
                return new ExceptionsAttribute(in, cp);
            }
            break;

        case 'I':
            if (tag.equals("InnerClasses")) {
                return new InnerClassTable(in, cp);
            }
            break;

        case 'L':
            if (tag.equals("LineNumberTable")) {
                throw new ClassFileFormatException("Attribute \"LineNumberTable\" illegal outside of Attribute Code");
            } else if (tag.equals("LocalVariableTable")) {
                throw new ClassFileFormatException("Attribute \"LocalVariableTable\" illegal outside of Attribute Code");
            }
            break;

        case 'S':
            if (tag.equals("SourceFile")) {
                return new SourceFileAttribute(in, cp);
            } else if (tag.equals("Synthetic")) {
                return new SyntheticAttribute(in, cp);
            }
            break;

        default:
            break;
        }

        return new GenericAttribute(name, in, cp);
    }

    /**
     * Constructs an attribute from a class file stream
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     *
     * @exception   java.io.IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to
     *                  write a bad classfile info
     */
    public static Attribute readInterfaceOnly(DataInput in, ConstantPool cp)
        throws IOException, ClassFileFormatException
    {
        AsciiConstant   name = (AsciiConstant)cp.getEntryAt(in.readUnsignedShort());
        String      tag = name.getValue();

        switch (tag.charAt(0)) {
        case 'C':
            if (tag.equals("Code")) {
                return new SkippedCodeInfo(in, cp);
            } else if (tag.equals("ConstantValue")) {
                return new ConstantValueAttribute(in, cp);
            }
            break;

        case 'D':
            if (tag.equals("Deprecated")) {
                return new DeprecatedAttribute(in, cp);
            }
            break;

        case 'E':
            if (tag.equals("Exceptions")) {
                return new ExceptionsAttribute(in, cp);
            }
            break;

        case 'I':
            if (tag.equals("InnerClasses")) {
                return new InnerClassTable(in, cp);
            }
            break;

        case 'L':
            if (tag.equals("LineNumberTable")) {
                throw new ClassFileFormatException("Attribute \"LineNumberTable\" illegal outside of Attribute Code");
            } else if (tag.equals("LocalVariableTable")) {
                throw new ClassFileFormatException("Attribute \"LocalVariableTable\" illegal outside of Attribute Code");
            }
            break;

        case 'S':
            if (tag.equals("SourceFile")) {
                return new SourceFileAttribute(in, cp);
            } else if (tag.equals("Synthetic")) {
                return new SyntheticAttribute(in, cp);
            }
            break;

        default:
            break;
        }

        return new GenericAttribute(name, in, cp);
    }

    /**
     * Constructs an sub-attribute of CodeInfo from a class file stream
     *
     * @param   in      the stream to read from
     * @param   cp      the constant pool
     * @param   insns       (sparse) array of instructions
     *
     * @exception   java.io.IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to
     *                  write a bad classfile info
     */
    public static Attribute readCodeInfoAttribute(DataInput in, ConstantPool cp, Instruction[] insns)
        throws IOException, ClassFileFormatException
    {
        AsciiConstant   name = (AsciiConstant)cp.getEntryAt(in.readUnsignedShort());
        String      tag = name.getValue();

        switch (tag.charAt(0)) {
        case 'C':
            if (tag.equals("Code")) {
                throw new ClassFileFormatException("Attribute \"Code\" illegal as sub-attribute of Attribute Code");
            } else if (tag.equals("ConstantValue")) {
                throw new ClassFileFormatException("Attribute \"ConstantValue\" illegal as sub-attribute of Attribute Code");
            }
            break;

        case 'D':
            if (tag.equals("Deprecated")) {
                throw new ClassFileFormatException("Attribute \"Deprecated\" illegal as sub-attribute of Attribute Code");
            }
            break;

        case 'E':
            if (tag.equals("Exceptions")) {
                throw new ClassFileFormatException("Attribute \"Exceptions\" illegal as sub-attribute of Attribute Code");
            }
            break;

        case 'I':
            if (tag.equals("InnerClasses")) {
                throw new ClassFileFormatException("Attribute \"InnerClasses\" illegal as sub-attribute of Attribute Code");
            }
            break;

        case 'L':
            if (tag.equals("LineNumberTable")) {
                return new LineNumberTable(in, cp, insns);
            } else if (tag.equals("LocalVariableTable")) {
                return new LocalVariableTable(in, cp, insns);
            }
            break;

        case 'S':
            if (tag.equals("SourceFile")) {
                throw new ClassFileFormatException("Attribute \"SourceFile\" illegal as sub-attribute of Attribute Code");
            } else if (tag.equals("Synthetic")) {
                throw new ClassFileFormatException("Attribute \"Synthetic\" illegal as sub-attribute of Attribute Code");
            }
            break;

        default:
            break;
        }

        return new GenericAttribute(name, in, cp);
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the attribute's tag
     */
    /*package*/ abstract int getTag();

    /**
     * Returns the space in bytes used by this attribute in the classfile
     */
    /*package*/ abstract int getSize();

    // --------------------------------------------------------------------
    // WRITE
    // --------------------------------------------------------------------

    /**
     * Insert or check location of constant value on constant pool
     *
     * @param   cp      the constant pool for this class
     */
    /*package*/ abstract void resolveConstants(ConstantPool cp) throws ClassFileFormatException;

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
    /*package*/ abstract void write(ConstantPool cp, DataOutput out)
        throws IOException, ClassFileFormatException;

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    public static final Attribute[] EMPTY = new Attribute[0];
}

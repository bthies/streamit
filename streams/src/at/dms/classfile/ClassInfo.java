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
 * $Id: ClassInfo.java,v 1.2 2006-01-25 17:00:38 thies Exp $
 */

package at.dms.classfile;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import at.dms.util.InconsistencyException;
import at.dms.util.Utils;

/**
 * VMS 4.1 : Class File.
 *
 * This is the place where all information about the class defined
 * by this class file resides.
 */
public class ClassInfo extends Member {

    // --------------------------------------------------------------------
    // CONSTRUCTORS
    // --------------------------------------------------------------------

    /**
     * Constructs a class info structure.
     *
     * @param   modifiers   access permission to and properties of this class
     * @param   thisClass   the class or interface defined by this class file
     * @param   superClass  the superclass of this class
     * @param   interfaces  the interfaces implemented by this class
     * @param   fields      the fields which are members of this class
     * @param   methods     the methods which are members of this class
     * @param   innerClasses    the inner classes which are members of this class
     * @param   sourceFile  the name of the source file
     * @param   deprecated  is this class deprecated ?
     */
    public ClassInfo(short modifiers,
                     String thisClass,
                     String superClass,
                     ClassConstant[] interfaces,
                     FieldInfo[] fields,
                     MethodInfo[] methods,
                     InnerClassInfo[] innerClasses,
                     String sourceFile,
                     boolean deprecated)
    {
        super(modifiers);

        minorVersion = JAVA_MINOR;
        majorVersion = JAVA_MAJOR;

        this.thisClass = new ClassConstant(thisClass);
        this.superClass = superClass != null ?
            new ClassConstant(superClass) :
            null;
        this.fields = fields;
        this.methods = methods;
        this.interfaces = interfaces;

        this.attributes = new AttributeList(innerClasses != null ? new InnerClassTable(innerClasses) : null,
                                            sourceFile != null ? new SourceFileAttribute(sourceFile) : null,
                                            deprecated ? new DeprecatedAttribute() : null);
    }

    /**
     * Constructs a class info structure.
     *
     * @param   modifiers   access permission to and properties of this class
     * @param   thisClass   the class or interface defined by this class file
     * @param   superClass  the superclass of this class
     * @param   interfaces  the interfaces implemented by this class
     * @param   fields      the fields which are members of this class
     * @param   methods     the methods which are members of this class
     * @param   innerClasses    the inner classes which are members of this class
     * @param   sourceFile  the name of the source file
     * @param   deprecated  is this class deprecated ?
     */
    public ClassInfo(short modifiers,
                     String thisClass,
                     String superClass,
                     Vector interfaces,
                     Vector fields,
                     Vector methods,
                     InnerClassInfo[] innerClasses,
                     String sourceFile,
                     boolean deprecated)
    {
        this(modifiers,
             thisClass,
             superClass,
             makeInterfacesArray(interfaces),
             (FieldInfo[])Utils.toArray(fields, FieldInfo.class),
             (MethodInfo[])Utils.toArray(methods, MethodInfo.class),
             innerClasses,
             sourceFile,
             deprecated);
    }

    /**
     * Constructs a class info structure from a class file stream.
     *
     * @param   in      the stream to read the class from
     * @param   interfaceOnly   load only the interface, not the source code
     *
     * @exception   IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to read a bad classfile
     */
    public ClassInfo(DataInput in, boolean interfaceOnly)
        throws IOException, ClassFileFormatException
    {
        int     magic = in.readInt();
        if (magic != JAVA_MAGIC) {
            throw new ClassFileFormatException("Bad magic number: " + magic);
        }

        minorVersion = in.readUnsignedShort();
        majorVersion = in.readUnsignedShort();

        ConstantPool constantPool = new ConstantPool(in);

        setModifiers((short)in.readUnsignedShort());
        thisClass = (ClassConstant)constantPool.getEntryAt(in.readUnsignedShort());
        superClass = (ClassConstant)constantPool.getEntryAt(in.readUnsignedShort());

        interfaces   = new ClassConstant[in.readUnsignedShort()];
        for (int i = 0; i < interfaces.length; i += 1) {
            interfaces[i] = (ClassConstant)constantPool.getEntryAt(in.readUnsignedShort());
        }

        fields       = new FieldInfo[in.readUnsignedShort()];
        for (int i = 0; i < fields.length; i += 1) {
            fields[i] = new FieldInfo(in, constantPool);
        }

        methods      = new MethodInfo[in.readUnsignedShort()];
        for (int i = 0; i < methods.length; i += 1) {
            methods[i] = new MethodInfo(in, constantPool, interfaceOnly);
        }

        this.attributes = new AttributeList(in, constantPool, false);

        constantPool.close();
        constantPool = null; // save memory
    }

    // --------------------------------------------------------------------
    // ACCESSORS
    // --------------------------------------------------------------------

    /**
     * Returns the name of the this class (fully qualified)
     */
    public String getName() {
        return thisClass.getName();
    }

    /**
     * Sets the name of the this field (fully qualified)
     */
    public void setName(String name) {
        thisClass = new ClassConstant(name);
    }

    /**
     * Returns the type of the this field
     */
    public String getSignature() {
        return thisClass.getName();
    }

    /**
     * Returns the super class of the class in the file
     */
    public String getSuperClass() {
        return superClass == null ? null : superClass.getName();
    }

    /**
     * Sets the super class of the class in the file
     */
    public void setSuperClass(String superClass) {
        this.superClass = superClass == null ? null : new ClassConstant(superClass);
    }

    /**
     * Returns the version of the class in the file
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Sets the version of the class in the file
     */
    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    /**
     * Returns the version of the class in the file
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Sets the version of the class in the file
     */
    public void setMinor(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    /**
     * Returns the inner classes table of the class in the file
     */
    public InnerClassInfo[] getInnerClasses() {
        Attribute       attr = attributes.get(Constants.ATT_INNERCLASSES);

        return attr == null ? null : ((InnerClassTable)attr).getEntries();
    }

    /**
     * Sets the inner classes table of the class in the file
     */
    public void setInnerClasses(InnerClassInfo[] inners) {
        if (inners != null) {
            attributes.add(new InnerClassTable(inners));
        } else {
            attributes.remove(Constants.ATT_INNERCLASSES);
        }
    }

    /**
     * Returns the source file of the class in the file
     */
    public String getSourceFile() {
        Attribute       attr = attributes.get(Constants.ATT_SOURCEFILE);

        return attr == null ? null : ((SourceFileAttribute)attr).getValue();
    }

    /**
     * Returns the source file of the class in the file
     */
    public void setSourceFile(String name) {
        if (name != null) {
            attributes.add(new SourceFileAttribute(name));
        } else {
            attributes.remove(Constants.ATT_SOURCEFILE);
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
     * Returns the interfaces of the class in the file
     */
    public String[] getInterfaces() {
        String[]    names = new String[interfaces.length];

        for (int i = 0; i < interfaces.length; i++) {
            names[i] = interfaces[i].getName();
        }

        return names;
    }

    /**
     * Sets the interfaces of the class in the file
     */
    public void setInterfaces(String[] interfaces) {
        this.interfaces = new ClassConstant[interfaces.length];
        for (int i = 0; i < this.interfaces.length; i++) {
            this.interfaces[i] = new ClassConstant(interfaces[i]);
        }
    }

    /**
     * Returns the fields info of the class in the file
     */
    public FieldInfo[] getFields() {
        return fields;
    }

    /**
     * Sets the fields info of the class in the file
     */
    public void setFields(FieldInfo[] fields) {
        this.fields = fields;
    }

    /**
     * Returns the methods info of the class in the file
     */
    public MethodInfo[] getMethods() {
        return methods;
    }

    /**
     * Sets the methods info of the class in the file
     */
    public void setMethods(MethodInfo[] methods) {
        this.methods = methods;
    }

    // --------------------------------------------------------------------
    // OUTPUT (WRITE A CLASS FILE)
    // --------------------------------------------------------------------

    /**
     * Writes the content of the class to the specified output stream
     *
     * @param   out     the stream to write to
     *
     * @exception   IOException an io problem has occured
     * @exception   ClassFileFormatException    attempt to write a bad classfile info
     */
    public void write(DataOutput out)
        throws IOException, ClassFileFormatException
    {
        ConstantPool constantPool = resolveConstants();

        // Headers
        out.writeInt(JAVA_MAGIC);
        out.writeShort(minorVersion);
        out.writeShort(majorVersion);

        constantPool.write(out);

        // Class hierarchy/access
        out.writeShort((getModifiers() | ACC_SUPER) & MODIFIER_MASK);
        out.writeShort(thisClass.getIndex());
        out.writeShort(superClass == null ? 0 : superClass.getIndex());

        // interfaces
        out.writeShort(interfaces.length);
        for (int i = 0; i < interfaces.length; i++) {
            out.writeShort(interfaces[i].getIndex());
        }

        // fields
        out.writeShort(fields.length);
        for (int i = 0; i < fields.length; i++) {
            fields[i].write(constantPool, out);
        }

        // methods
        out.writeShort(methods.length);
        for (int i = 0; i < methods.length; i++) {
            methods[i].write(constantPool, out);
        }

        // attributes
        attributes.write(constantPool, out);

        constantPool.close();
    }

    /**
     * Writes the contents of the class to a file.
     *
     * @param   destination the root directory of the class hierarchy
     *
     * @exception   IOException an io problem occured
     * @exception   ClassFileFormatException    attempt to write a bad classfile info
     */
    public void write(String destination)
        throws IOException, ClassFileFormatException
    {
        String[]    classPath = Utils.splitQualifiedName(getName());

        if (destination != null && destination != "") {
            if (classPath[0] != null && classPath[0] != "") {
                // the class is part of a package
                destination += File.separator + classPath[0].replace('/', File.separatorChar);
            }

            // check that destination exists or else create it
            File    destDir = new File(destination);

            if (! destDir.exists()) {
                destDir.mkdirs();
            }

            if (! destDir.isDirectory()) {
                //!!! FIXME graf 000319: USE A CHECKED EXCEPTION
                throw new InconsistencyException("File " + destination + " is not a directory.");
            }
        } else {
            destination = ".";
        }

        File        outputFile = new File(destination, classPath[1] + ".class");
        DataOutputStream    outputStream =
            new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));

        write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    // --------------------------------------------------------------------
    // PRIVATE METHODS
    // --------------------------------------------------------------------

    /**
     * Create a constant pool and insert in it all constants from from
     * all the element of the class (fields, method, attributes, ...)
     */
    private ConstantPool resolveConstants() throws ClassFileFormatException {
        ConstantPool constantPool = new ConstantPool();

        constantPool.addItem(thisClass);
        if (superClass != null) {
            constantPool.addItem(superClass);
        }

        for (int i = 0; i < interfaces.length; i++) {
            constantPool.addItem(interfaces[i]);
        }

        for (int i = 0; i < fields.length; i++) {
            fields[i].resolveConstants(constantPool);
        }

        // methods (generate code, resolve constants, and pregen)
        for (int i = 0; i < methods.length; i++) {
            methods[i].resolveConstants(constantPool);
        }

        attributes.resolveConstants(constantPool);

        return constantPool;
    }

    /**
     *
     */
    private static ClassConstant[] makeInterfacesArray(Vector vector) {
        ClassConstant[] array;

        array = new ClassConstant[vector.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = new ClassConstant((String)vector.elementAt(i));
        }
        return array;
    }

    // --------------------------------------------------------------------
    // DATA MEMBERS
    // --------------------------------------------------------------------

    /**
     * Valid modifiers for classes and interfaces.
     *
     * VMS 4.1 : All bits of the access_flags item not assigned in
     * Table 4.1 are reserved for future use. They should be set to zero
     * in generated class files.
     */
    private static final int        MODIFIER_MASK =
        ACC_PUBLIC | ACC_FINAL | ACC_SUPER | ACC_INTERFACE | ACC_ABSTRACT;

    private int             majorVersion;
    private int             minorVersion;

    private ClassConstant           thisClass;
    private ClassConstant           superClass;
    private ClassConstant[]     interfaces;
    private FieldInfo[]         fields;
    private MethodInfo[]            methods;
    private AttributeList           attributes;
}

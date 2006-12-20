/*
 * Copyright (C) 1990-2001 DMS Decision Management Systems Ges.m.b.H.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * $Id: CField.java,v 1.11 2006-12-20 18:03:32 dimock Exp $
 */

package at.dms.kjc;

import at.dms.classfile.FieldInfo;

/**
 * This class represents an exported member of a class (fields)
 */
public abstract class CField extends CMember {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    protected CField() {} // for cloner only

    /**
     * Constructs a field export
     * @param   owner       the owner of this field
     * @param   modifiers   the modifiers on this field
     * @param   variable    the variable of this field
     * @param   deprecated  is this field deprecated ?
     */
    public CField(CClass owner,
                  int modifiers,
                  JVariableDefinition variable,
                  boolean deprecated)
    {
        super(owner, modifiers, variable.getIdent(), deprecated);
        this.variable = variable;
    }

    /**
     * Constructs a field export
     * @param   owner       the owner of this field
     * @param   modifiers   the modifiers on this field
     * @param     ident           the name of this field
     * @param     type            the type of this field
     * @param   deprecated  is this field deprecated ?
     */
    public CField(CClass owner,
                  int modifiers,
                  String ident,
                  CType type,
                  boolean deprecated)
    {
        super(owner, modifiers, ident, deprecated);
        this.type = type;
        this.variable = null;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * @return  the interface
     */
    public CField getField() {
        return this;
    }

    /**
     * @return the type of this field
     */
    public CType getType() {
        if (variable!=null) {
            return variable.getType();
        } else {
            return type;
        }
    }

    /**
     * Set type.  Delegates to variable if the field has one.
     * @param type  CType to set
     */
    public void setType(CType type) {
        if (variable!=null) {
            variable.setType(type);
        } else {
            this.type = type;
        }
    }
    
    /**
     * @param   value       the value known at third pass
     */
    public void setValue(JExpression value) {
        this.value = value;
    }

    /**
     * @return  the value of initializer or null
     */
    public JExpression getValue() {
        return value;
    }


    /**
     * Returns a string representation of this object.
     */
    public String toString() {
        return getQualifiedName();
    }


    // ----------------------------------------------------------------------
    // GENERATE CLASSFILE INFO
    // ----------------------------------------------------------------------

    /**
     * Generates a sequence of bytecodes to load
     * @param   code        the code list
     */
    public void genLoad(CodeSequence code) {
        code.plantFieldRefInstruction(isStatic() ? opc_getstatic : opc_getfield,
                                      getPrefixName(),
                                      getIdent(),
                                      getType().getSignature());
    }

    /**
     * Generates a sequence of bytecodes to load
     * @param   code        the code list
     */
    public void genStore(CodeSequence code) {
        code.plantFieldRefInstruction(isStatic() ? opc_putstatic : opc_putfield,
                                      getPrefixName(),
                                      getIdent(),
                                      getType().getSignature());
    }

    // ----------------------------------------------------------------------
    // GENERATE CLASSFILE INFO
    // ----------------------------------------------------------------------

    /**
     * Returns the constant value of a constant final field or null.
     */
    public Object getConstantValue() {
        if (! (isFinal()
               && isStatic()
               && value != null
               && value.isConstant())) {
            return null;
        } else {
            value = value.getLiteral();

            switch (value.getType().getTypeID()) {
            case TID_BYTE:
                return new Integer(value.byteValue());
            case TID_SHORT:
                return new Integer(value.shortValue());
            case TID_CHAR:
                return new Integer(value.charValue());
            case TID_INT:
                return new Integer(value.intValue());
            case TID_LONG:
                return new Long(value.longValue());
            case TID_FLOAT:
                return new Float(value.floatValue());
            case TID_DOUBLE:
                return new Double(value.doubleValue());
            case TID_CLASS:
                if (type.equals(CStdType.String)) {
                    return value.stringValue();
                } else {
                    return null;
                }
            case TID_BOOLEAN:
                return new Integer(value.booleanValue() ? 1 : 0);
            default:
                return null;
            }
        }
    }

    /**
     * Generate the code in a class file
     */
    public FieldInfo genFieldInfo() {
        return new FieldInfo((short)getModifiers(),
                             getIdent(),
                             type.getSignature(),
                             getConstantValue(),
                             isDeprecated(),
                             getIdent().indexOf('$') >= 0);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    // one of these two will be null.  For source fields, StreamIt
    // wants to see the actual variable corresponding to the field
    // declaration so that there can be a reference shared between
    // definition and use.
    private JVariableDefinition variable;
    private CType type;

    private JExpression     value;

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.CField other) {
        super.deepCloneInto(other);
        other.variable = (at.dms.kjc.JVariableDefinition)at.dms.kjc.AutoCloner.cloneToplevel(this.variable);
        other.type = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.type);
        other.value = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.value);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

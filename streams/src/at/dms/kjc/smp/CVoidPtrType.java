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
 * $Id: CVoidPtrType.java,v 1.2 2009-07-10 19:50:53 ctan Exp $
 */

package at.dms.kjc.smp;

import at.dms.compiler.UnpositionedError;
import at.dms.kjc.*;
import at.dms.util.SimpleStringBuffer;

final class CVoidPtrType extends CType {

    public static CVoidPtrType VoidPtr = new CVoidPtrType();
    
    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    /**
     * Constructor
     */
    protected CVoidPtrType() {
        super(TID_VOID);
    }

    private Object readResolve() throws Exception {
        return CVoidPtrType.VoidPtr;
    }

    // ----------------------------------------------------------------------
    // BODY CHECKING
    // ----------------------------------------------------------------------

    /**
     * check that type is valid
     * necessary to resolve String into java/lang/String
     * @exception   UnpositionedError   this error will be positioned soon
     */
    public void checkType(CContext context) throws UnpositionedError {
    }

    /**
     * Can this type be converted to the specified type by assignment conversion (JLS 5.2) ?
     * @param   dest        the destination type
     * @return  true iff the conversion is valid
     */
    public boolean isAssignableTo(CType dest) {
        return false;
    }

    /**
     * Can this type be converted to the specified type by casting conversion (JLS 5.5) ?
     * @param   dest        the destination type
     * @return  true iff the conversion is valid
     */
    public boolean isCastableTo(CType dest) {
        return false;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Transforms this type to a string
     */
    public String toString() {
        return "void *";
    }

    /**
     * Returns the VM signature of this type.
     */
    public String getSignature() {
        return "V";
    }

    /**
     * Appends the VM signature of this type to the specified buffer.
     */
    protected void appendSignature(SimpleStringBuffer buffer) {
        buffer.append('V');
    }

    /**
     * Returns the stack size (conservative estimate of maximum number
     * of bytes needed in C on 32-bit machine) used by a value of this
     * type.
     */
    public int getSizeInC() {
        return 4;
    }

    /**
     * Returns the stack size used by a value of this type.
     */
    public int getSize() {
        return 1;
    }
    
    public boolean isReference() {
        return true;
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.smp.CVoidPtrType other = new at.dms.kjc.smp.CVoidPtrType();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.smp.CVoidPtrType other) {
        super.deepCloneInto(other);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

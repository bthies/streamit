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
 * $Id: CInterfaceContext.java,v 1.5 2006-03-24 15:54:47 dimock Exp $
 */

package at.dms.kjc;

import at.dms.compiler.UnpositionedError;

/**
 * This class represents an interface context during check
 */
public class CInterfaceContext extends CClassContext {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    protected CInterfaceContext() {} // for cloner only

    /**
     * @param   parent      the parent context or null at top level
     * @param   clazz       the corresponding clazz
     */
    public CInterfaceContext(CContext parent, CSourceClass clazz) {
        super(parent, clazz, null);
    }

    /**
     * Verify all final fields are initialized
     * @exception   UnpositionedError   this error will be positioned soon
     */
    public void close(JTypeDeclaration decl, CBodyContext virtual) throws UnpositionedError {
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.CInterfaceContext other = new at.dms.kjc.CInterfaceContext();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.CInterfaceContext other) {
        super.deepCloneInto(other);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: AbstractInstructionAccessor.java,v 1.2 2003-05-28 05:58:36 thies Exp $
 */

package at.dms.classfile;

/**
 * This class provides default implementations for the
 * InstructionAccessor interface: the resolution of the accessor is
 * left to the transformer, which must in turn have knowledge of and
 * access to the accessor to be transformed.
 */
public abstract class AbstractInstructionAccessor
  implements InstructionAccessor
{
  /**
   * Transforms the accessor.
   * @param	transformer		the transformer to be used
   * @param	container		the object which contains the accessor
   */
  public InstructionAccessor transform(AccessorTransformer transformer,
				       AccessorContainer container)
    throws BadAccessorException
  {
    return transformer.transform(this, container);
  }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.classfile.AbstractInstructionAccessor other) {
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: CMember.java,v 1.6 2003-08-29 19:25:36 thies Exp $
 */

package at.dms.kjc;

import at.dms.util.InconsistencyException;
import java.io.*;

/**
 * This class represents an exported member of a class
 * @see	CMember
 * @see	CField
 * @see	CMethod
 * @see	CClass
 */
public class CMember extends at.dms.util.Utils implements Constants {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CMember() {} // for cloner only

  /**
   * Constructs a field export
   * @param	owner		the owner of this member
   * @param	modifiers	the modifiers on this member
   * @param	ident		the ident of this member
   * @param	deprecated	is this member deprecated
   */
  public CMember(CClass owner,
		 int modifiers,
		 String ident,
		 boolean deprecated)
  {
    this.owner = owner;
    this.modifiers = modifiers;
    this.ident = ident;
    this.deprecated = deprecated;
  }

  // ----------------------------------------------------------------------
  // CLONING STUFF
  // ----------------------------------------------------------------------

    private Object serializationHandle;
    
    private void writeObject(ObjectOutputStream oos)
	throws IOException {
	this.serializationHandle = ObjectDeepCloner.getHandle(this);
	oos.defaultWriteObject();
    }
    
    private Object readResolve() throws Exception {
	return ObjectDeepCloner.getInstance(serializationHandle, this);
    }
   
  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the owner of this member
   */
  public CClass getOwner() {
    return owner;
  }

  /**
   * @return the ident of this method
   */
  public String getIdent() {
    return ident;
  }

  /**
   * @return the fully qualified name of this member
   */
  public String getQualifiedName() {
    return owner.getQualifiedName() + JAV_NAME_SEPARATOR + ident;
  }

  /**
   * @return the fully qualified name of this member
   */
  public String getPrefixName() {
    return owner.getQualifiedName();
  }

  /**
   * @return the fully qualified name of this member
   */
  public String getJavaName() {
    return getQualifiedName().replace('/' , '.');
  }

  /**
   * @return the modifiers of this member
   */
  public int getModifiers() {
    return modifiers;
  }

  /**
   * Sets the modifiers of this member.
   */
  public void setModifiers(int modifiers) {
    this.modifiers = modifiers;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (QUICK)
  // ----------------------------------------------------------------------

  /**
   * @return	the interface
   */
  public CField getField() {
    throw new InconsistencyException();
  }

  /**
   * @return	the interface
   */
  public CMethod getMethod() {
    throw new InconsistencyException();
  }

  /**
   * @return	the interface
   */
  public CClass getCClass() {
    throw new InconsistencyException();
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (QUICK)
  // ----------------------------------------------------------------------

  /**
   * @return	true if this member is static
   */
  public boolean isStatic() {
    return CModifier.contains(modifiers, ACC_STATIC);
  }

  /**
   * @return	true if this member is public
   */
  public boolean isPublic() {
    return CModifier.contains(modifiers, ACC_PUBLIC);
  }

  /**
   * @return	true if this member is protected
   */
  public boolean isProtected() {
    return CModifier.contains(modifiers, ACC_PROTECTED);
  }

  /**
   * @return	true if this member is private
   */
  public boolean isPrivate() {
    return CModifier.contains(modifiers, ACC_PRIVATE);
  }

  /**
   * @return	true if this member is final
   */
  public boolean isFinal() {
    return CModifier.contains(modifiers, ACC_FINAL);
  }

  /**
   * @return	true if this member is deprecated
   */
  public boolean isDeprecated() {
    return deprecated;
  }

  /**
   * Checks whether this type is accessible from the specified class (JLS 6.6).
   *
   * Note : top level class and interface types are handled in CClass.
   *
   * @return	true iff this member is accessible
   */
  public boolean isAccessible(CClass from) {
    if (isPublic() || from.isDefinedInside(owner) || owner.isDefinedInside(from)) {
      return true;
    } else if (from.isNested() && isAccessible(from.getOwner())) {
      return true;
    } else if (!isPrivate() && owner.getPackage() == from.getPackage()) {
      return true;
    } else if (isProtected() && from.descendsFrom(owner)) {
      return true;
    } else {
      return false;
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected CClass			owner;	//!!! make private
  private int				modifiers;
  private String			ident;
  private boolean			deprecated;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CMember other = new at.dms.kjc.CMember();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CMember other) {
  super.deepCloneInto(other);
  other.serializationHandle = this.serializationHandle;
  other.owner = (at.dms.kjc.CClass)at.dms.kjc.AutoCloner.cloneToplevel(this.owner, other);
  other.modifiers = this.modifiers;
  other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident, other);
  other.deprecated = this.deprecated;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

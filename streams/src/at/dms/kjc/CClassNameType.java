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
 * $Id: CClassNameType.java,v 1.3 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

/**
 * This class represents class type in the type structure
 */
public class CClassNameType extends CClassType {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CClassNameType() {} // for cloner only

  /**
   * Construct a class type
   * @param	qualifiedName	the class qualified name of the class
   */
  public CClassNameType(String qualifiedName) {
    super();

    if (qualifiedName.indexOf('.') >= 0) {
      throw new InconsistencyException(); // $$$
    }

    this.qualifiedName = qualifiedName.intern();
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Transforms this type to a string
   */
  public String toString() {
    return qualifiedName != null ?
      qualifiedName.replace('/', '.') :
      super.toString();
  }

  /**
   *
   */
  public String getQualifiedName() {
    return qualifiedName == null ? super.getQualifiedName() : qualifiedName;
  }

  /**
   * Returns the class object associated with this type
   *
   * If this type was never checked (read from class files)
   * check it!
   *
   * @return the class object associated with this type
   */
  public CClass getCClass() {
    if (qualifiedName != null) {
      try {
	checkType(null);
      } catch (UnpositionedError cue) {
	setClass(new CBadClass(qualifiedName));
	qualifiedName = null;
	return getCClass();
      }
    }

    return super.getCClass();
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * check that type is valid
   * necessary to resolve String into java/lang/String
   * @param	context		the context (may be be null)
   * @exception UnpositionedError	this error will be positioned soon
   */
  public void checkType(CContext context) throws UnpositionedError {
    if (qualifiedName != null) {
      String            clazzName  = null;

      if (context != null && qualifiedName.indexOf('/') < 0) {
        CClassContext     classContext =  context.getClassContext();
        CClass            caller;
        if (classContext == null) {
          caller = CStdType.Object.getCClass();
        } else {
          caller = classContext.getCClass();
        }
	CClassType	type = context.lookupClass(caller, qualifiedName);

	if (type != null) {
	  if ((type instanceof CClassNameType) && 
	      (context != null) &&
	      (((CClassNameType)type).qualifiedName != null)) {
	    qualifiedName = ((CClassNameType)type).qualifiedName;
	    // we have found the good one but we dont want to really load the class here
	    //setClass(type.getCClass());
	    //qualifiedName = null;
	  } else {
	    setClass(type.getCClass());
	    qualifiedName = null;
	  }
	  return;
	}
      } else {
	clazzName = CTopLevel.hasClassFile(qualifiedName) ? qualifiedName : null;
	if (clazzName != null && context != null) {
	  return;
	}
      }
      if (clazzName == null) {
	// May be a innerclass
	int	index = qualifiedName.lastIndexOf("/");

	if (index > 0) {
          CClassType		outer;
	  try {
	    outer = new CClassNameType(qualifiedName.substring(0, index));

	    outer.checkType(context);
	  } catch (UnpositionedError ce) {
	    throw new UnpositionedError(KjcMessages.TYPE_UNKNOWN, qualifiedName);
	  }
          CClass            caller;

          if (context == null) {
            caller = CStdType.Object.getCClass();
          } else {
            CClassContext     classContext =  context.getClassContext();

            if (classContext == null) {
              caller = CStdType.Object.getCClass();
            } else {
              caller = classContext.getCClass();
            }
          }
          CClass        innerClass = outer.getCClass().lookupClass(caller, qualifiedName.substring(index + 1));

          if (innerClass != null) {
            setClass(innerClass);
            qualifiedName = null;
          } else {
            throw new UnpositionedError(KjcMessages.TYPE_UNKNOWN, qualifiedName);
          }
	} else {
	  throw new UnpositionedError(KjcMessages.TYPE_UNKNOWN, qualifiedName);
	}
      } else {
	qualifiedName = clazzName;
      }

      if (clazzName != null) {
	setClass(CTopLevel.loadClass(qualifiedName));
	qualifiedName = null;
      }
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected String		qualifiedName; // null => checked

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CClassNameType other = new at.dms.kjc.CClassNameType();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CClassNameType other) {
  super.deepCloneInto(other);
  other.qualifiedName = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.qualifiedName);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: CClass.java,v 1.3 2003-05-16 21:06:38 thies Exp $
 */

package at.dms.kjc;

import java.io.IOException;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;
import at.dms.util.Utils;

/**
 * This class represents the exported members of a class (inner classes, methods and fields)
 */
public abstract class CClass extends CMember {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

  /**
   * Constructs a class export from file
   */
  public CClass(CClass owner,
		String sourceFile,
		int modifiers,
		String ident,
		String qualifiedName,
		CClassType superClass,
		boolean deprecated)
  {
    super(owner, modifiers, ident, deprecated);

    this.sourceFile = sourceFile;
    this.qualifiedName = qualifiedName;
    this.superClass = superClass;

    int		cursor = qualifiedName.lastIndexOf('/');
    qualifiedAndAnonymous = false;

    this.packageName = cursor > 0 ? qualifiedName.substring(0, cursor).intern() : "";
  }

  /**
   * Ends the definition of this class
   */
  public void close(CClassType[] interfaces,
		    Hashtable fields,
		    CMethod[] methods)
  {
    this.interfaces = interfaces;
    this.fields = fields;
    this.methods = methods;
    assert(interfaces != null);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return	the interface
   */
  public CClass getCClass() {
    return this;
  }

  /**
   * Checks whether this type is nested.
   *
   * JLS 8 (introduction), JLS 9 (introduction) :
   * A nested type (class or interface) is any type whose declaration
   * occurs within the body of another class or interface. A top level
   * type is a type that is not a nested class.
   *
   * @return	true iff this type is nested
   */
  public boolean isNested() {
    return getOwner() != null;
  }

  /**
   * @return	true if this member is abstract
   */
  public boolean isAbstract() {
    return CModifier.contains(getModifiers(), ACC_ABSTRACT);
  }

  /**
   * @return true if this class is anonymous
   */
  public boolean isAnonymous() {
    return getIdent().length() == 0;
  }

  /**
   * @return true if this class is has an outer this
   */
  public boolean hasOuterThis() {
    return hasOuterThis;
  }

  /**
   * Sets hasOuterThis
   */
  public void setHasOuterThis(boolean hasOuterThis) {
    this.hasOuterThis = hasOuterThis;
  }

  /**
   * @return true if this class is an interface
   */
  public boolean isInterface() {
    return CModifier.contains(getModifiers(), ACC_INTERFACE);
  }

  /**
   * @return true if this class is a class
   */
  public boolean isClass() {
    return !CModifier.contains(getModifiers(), ACC_INTERFACE);
  }

  /**
   * @return the full name of this class
   */
  public String getQualifiedName() {
    return qualifiedName;
  }

  /**
   * @return the name of the package of the package this class belongs to
   */
 public String getPackage() {
    return packageName;
  }

  /**
   * Returns the super class of this class
   */
  public CClass getSuperClass() {
    return superClass == null ? null : superClass.getCClass();
  }

  /**
   * Sets the super class of this class
   */
  public void setSuperClass(CClassType superClass) {
    this.superClass = superClass;
  }

  /**
   * Returns the type of this class
   */
  public CClassType getType() {
    if (type == null) {
      type = CClassType.lookup(getQualifiedName());
    }
    return type;
  }

  /**
   * Returns the source file of this class.
   */
  public String getSourceFile() {
    return sourceFile;
  }

  /**
   * Returns the field with the specified name.
   *
   * @param	ident		the name of the field
   */
  public CField getField(String ident) {
    return (CField)fields.get(ident);
  }

  /**
   * Returns the number of fields.
   */
  public int getFieldCount() {
    return fields.size();
  }

  /**
   * Returns an array containing the fields defined by this class.
   */
  public CField[] getFields() {
    CField[]		result;

    result = new CField[fields.size()];
    for (Enumeration enum = fields.elements(); enum.hasMoreElements(); ) {
      CSourceField	field = (CSourceField)enum.nextElement();

      result[field.getPosition()] = field;
    }
    return result;
  }

  /**
   * Returns the interfaces defined by this class.
   */
  public CClassType[] getInterfaces() {
    return interfaces;
  }

  /**
   * Returns the methods defined by this class.
   */
  public CMethod[] getMethods() {
    return methods;
  }

  /**
   * @return the InnerClasses
   */
  public CClassType[] getInnerClasses() {
    return innerClasses;
  }

  /**
   * End of first pass, we need innerclassesinterfaces
   */
  public void setInnerClasses(CClassType[] inners) {
    this.innerClasses = inners;
  }

  /**
   * descendsFrom
   * @param	from	an other CClass
   * @return	true if this class inherit from "from" or equals "from"
   */
  public boolean descendsFrom(CClass from) {
    if (from == this) {
      return true;
    } else if (superClass == null) {
      // java/lang/object
      return false;
    } else if (getSuperClass().descendsFrom(from)) {
      return true;
    } else {
      for (int i = 0; i < interfaces.length; i++) {
	if (interfaces[i].getCClass().descendsFrom(from)) {
	  return true;
	}
      }

      return false;
    }
  }

  /**
   * Returns true iff this class is defined inside the specified class
   * @param	outer		another class
   */
  public boolean isDefinedInside(CClass outer) {
    if (this == outer) {
      return true;
    } else if (getOwner() == null) {
      return false;
    } else {
      return getOwner().isDefinedInside(outer);
    }
  }

  /**
   * Returns a string representation of this class.
   */
  public String toString() {
    return qualifiedName;
  }

  public void setQualifiedAndAnonymous(boolean qualifiedAndAnonymous) {
    this.qualifiedAndAnonymous = qualifiedAndAnonymous;
  }
  public boolean isQualifiedAndAnonymous() {
    return qualifiedAndAnonymous;
  }

  // ----------------------------------------------------------------------
  // LOOKUP
  // ----------------------------------------------------------------------

  /**
   * This can be used to see if a given class name is visible
   * inside of this file.  This includes globally-qualified class names that
   * include their package and simple names that are visible thanks to some
   * previous import statement or visible because they are in this file.
   * If one is found, that entry is returned, otherwise null is returned.
   * @param	caller		the class of the caller
   * @param	name		a TypeName (6.5.2)
   */
  public CClass lookupClass(CClass caller, String name)  
    throws UnpositionedError {
    CClass[]    candidates = new CClass[(interfaces == null) ? 1 : interfaces.length +1]; 
    int         length = 0;

    if (innerClasses != null && (name.indexOf('/') == -1)) {
      for (int i = 0; i < innerClasses.length; i++) {
        CClass  innerClass = innerClasses[i].getCClass();

	if (innerClass.getIdent().equals(name)) {
	  if (innerClass.isAccessible(caller)) {
            candidates[length++] = innerClass;
          }
          break;
	}
      }
    }
    if (length != 0 ) {
      /* JLS 8.5 If the class declares a member type with a certain name, 
         then the declaration of that type is said to hide any and all 
         accessible declarations of member types with the same name in 
         superclasses and superinterfaces of the class. */
      return candidates[0];
    } else {
      if (interfaces != null) {
        for (int i = 0; i < interfaces.length; i++) {
          CClass  superFound = interfaces[i].getCClass().lookupClass(caller, name);
          if (superFound != null) {
            candidates[length++] = superFound;
          }
        }
      }
      if (superClass != null) {
        CClass  superFound = superClass.getCClass().lookupClass(caller, name);

        if (superFound != null) {
          candidates[length++] = superFound;
        }
      }
      if (length == 0 ) {
        return null;
      } else if (length == 1) {
        return candidates[0];
      } else {
        /* JLS 8.5 A class may inherit two or more type declarations with 
           the same name, either from two interfaces or from its superclass 
           and an interface. A compile-time error occurs on any attempt to 
           refer to any ambiguously inherited class or interface by its simple 
           name. */
        throw new UnpositionedError(KjcMessages.CLASS_AMBIGUOUS, name);
      }
    }
  }

  /**
   * Searches the class or interface to locate declarations of fields that are
   * accessible.
   * 
   * @param	caller		the class of the caller
   * @param	ident		the simple name of the field
   * @return	the field definition
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CField lookupField(CClass caller, String ident)
    throws UnpositionedError
  {
    CField	field = getField(ident);

    if (field != null && field.isAccessible(caller)) {
      return field;
    } else {
      return lookupSuperField(caller, ident);
    }
  }

  /**
   * Searches the class or interface to locate declarations of fields that are
   * accessible.
   * 
   * @param	caller		the class of the caller
   * @param	ident		the simple name of the field
   * @return	the field definition
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CField lookupSuperField(CClass caller, String ident)
    throws UnpositionedError
  {
    CField	field;

    if (superClass == null) {
      // java.lang.Object
      field = null;
    } else {
      field = getSuperClass().lookupField(caller, ident);
    }

    for (int i = 0; i < interfaces.length; i++) {
      CField	newField = interfaces[i].getCClass().lookupField(caller, ident);

      if ((field != null) && (newField != null) && (field.getOwner() != newField.getOwner())) {
	throw new UnpositionedError(KjcMessages.FIELD_AMBIGUOUS, ident);
      }

      if (newField != null) {
	field = newField;
      }
    }

    return field;
  }

  /**
   * JLS 15.12.2 :
   * Searches the class or interface to locate method declarations that are
   * both applicable and accessible, that is, declarations that can be correctly
   * invoked on the given arguments. There may be more than one such method
   * declaration, in which case the most specific one is chosen.
   *
   * @param	caller		the class of the caller
   * @param	ident		method invocation name
   * @param	actuals		method invocation arguments
   * @return	the method to apply
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CMethod lookupMethod(CClass caller, String ident, CType[] actuals)
    throws UnpositionedError
  {
    CMethod[]	applicable = getApplicableMethods(ident, actuals);
    CMethod[]	candidates = new CMethod[applicable.length];
    int		length = 0;

    // find the maximally specific methods
  _all_methods_:
    for (int i = 0; i < applicable.length; i++) {
      int	current = 0;

      if (! applicable[i].isAccessible(caller)) {
	continue _all_methods_;
      }

      for (;;) {
	if (current == length) {
	  // no other method is more specific: add it
	  candidates[length++] = applicable[i];
	  continue _all_methods_;
	} else if (candidates[current].isMoreSpecificThan(applicable[i])) {
	  // other method is more specific: skip it
	  continue _all_methods_;
	} else if (applicable[i].isMoreSpecificThan(candidates[current])) {
	  // this method is more specific: remove the other
	  if (current < length - 1) {
	    candidates[current] = candidates[length - 1];
	  }
	  length -= 1;
	} else {
	  // none is more specific than the other: check next candidate
	  current += 1;
	}
      }
    }

    if (length == 0) {
      // no applicable method
      return null;
    } else if (length == 1) {
      // exactly one most specific method
      return candidates[0];
    } else {
      // two or more methods are maximally specific

      // do all the maximally specific methods have the same signature ?
      for (int i = 1; i < length; i++) {
	if (!candidates[i].hasSameSignature(candidates[0])) {
	  // more than one most specific method with different signatures: ambiguous
	  throw new UnpositionedError(KjcMessages.METHOD_INVOCATION_AMBIGUOUS,
				      new Object[] {
					buildCallSignature(ident, actuals),
					candidates[0],
					candidates[i]
				      });
	}
      }

      // now, all maximally specific methods have the same signature
      // is there a non-abstract method (there is at most one) ?
      for (int i = 0; i < length; i++) {
	if (! candidates[i].isAbstract()) {
	  // the non-abstract method is the most specific one
	  return candidates[i];
	}
      }

      // now, all maximally specific methods have the same signature and are abstract
      // !!! FIXME graf 010128
      // "Otherwise, all the maximally specific methods are necessarily declared abstract.
      // The most specific method is chosen arbitrarily among the maximally specific methods.
      // However, the most specific method is considered to throw a checked exception
      // if and only if that exception OR A SUPERCLASS OF THAT EXCEPTION [Java Spec Report]
      // is declared in the throws clause of each of the maximally specific methods."
      // !!! FIXME graf 010128
      return candidates[0];
    }
  }

  /*
   * Returns a string representation of the call.
   */
  private static String buildCallSignature(String ident, CType[] actuals) {
    StringBuffer	buffer = new StringBuffer();

    buffer.append(ident);
    buffer.append("(");
    for (int i = 0; i < actuals.length; i++) {
      if (i != 0) {
	buffer.append(", ");
      }
      buffer.append(actuals[i]);
    }
    buffer.append(")");

    return buffer.toString();
  }

  /**
   * Searches the superclass and superinterfaces to locate method declarations
   * that are applicable. No check for accessibility is done here.
   *
   * @param	ident		method invocation name
   * @param	actuals		method invocation arguments
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CMethod[] lookupSuperMethod(String ident, CType[] actuals)
    throws UnpositionedError
  {
    Vector	container = new Vector();

    if (superClass != null) {
      // java.lang.object
      getSuperClass().collectApplicableMethods(container, ident, actuals);
    }

    for (int i = 0; i < interfaces.length; i++) {
      interfaces[i].getCClass().collectApplicableMethods(container, ident, actuals);
    }

    return (CMethod[])Utils.toArray(container, CMethod.class);
  }

  /**
   * Returns the abstract methods of this class or interface.
   *
   * JLS 8.1.1.1 :
   * A class C has abstract methods if any of the following is true :
   * -	C explicitly contains a declaration of an abstract method.
   * -	Any of C's superclasses declares an abstract method that has
   *	not been implemented in C or any of its superclasses.
   * -	A direct superinterface of C declares or inherits a method
   *	(which is therefore necessarily abstract) and C neither
   *	declares nor inherits a method that implements it.
   */
  public CMethod[] getAbstractMethods() {
    Vector	container = new Vector();

    // C explicitly contains a declaration of an abstract method.
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].isAbstract()) {
	container.addElement(methods[i]);
      }
    }

    // Any of C's superclasses declares an abstract method that has
    // not been implemented in C or any of its superclasses.
    if (superClass != null) {
      CMethod[]		inherited;

      inherited = getSuperClass().getAbstractMethods();
      for (int i = 0; i < inherited.length; i++) {
	if (getImplementingMethod(inherited[i], this, true) == null) {
	  container.addElement(inherited[i]);
	}
      }
    }

    // A direct superinterface of C declares or inherits a method
    // (which is therefore necessarily abstract) and C neither
    // declares nor inherits a method that implements it.
    for (int i = 0; i < interfaces.length; i++) {
      CMethod[]		inherited;

      inherited = interfaces[i].getCClass().getAbstractMethods();
      for (int j = 0; j < inherited.length; j++) {
	if (getImplementingMethod(inherited[j], this, false) == null) {
	  container.addElement(inherited[j]);
	}
      }
    }

    return (CMethod[])Utils.toArray(container, CMethod.class);
  }

  /**
   * Returns the non-abstract method that implements the
   * specified method, or null if the method is not implemented.
   *
   * @param	pattern		the method specification
   * @param	localClassOnly	do not search superclasses ?
   */
  protected CMethod getImplementingMethod(CMethod pattern, 
                                          CClass caller,
					  boolean localClassOnly)
  {
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getIdent() == pattern.getIdent()
	  && methods[i].hasSameSignature(pattern)) {
	return (methods[i].isAbstract() || !methods[i].isAccessible(caller))? null : methods[i];
      }
    }

    if (localClassOnly || superClass == null) {
      return null;
    } else {
      return getSuperClass().getImplementingMethod(pattern, caller, false);
    }
  }

  /**
   * Returns all applicable methods (JLS 15.12.2.1).
   * @param	ident		method invocation name
   * @param	actuals		method invocation arguments
   */
  public CMethod[] getApplicableMethods(String ident, CType[] actuals) {
    Vector	container = new Vector();

    collectApplicableMethods(container, ident, actuals);
    return (CMethod[])Utils.toArray(container, CMethod.class);
  }

  /**
   * Adds all applicable methods (JLS 15.12.2.1) to the specified container.
   * @param	container	the container for the methods
   * @param	ident		method invocation name
   * @param	actuals		method invocation arguments
   */
  public void collectApplicableMethods(Vector container, String ident, CType[] actuals) {
    // look in current class
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].isApplicableTo(ident, actuals)) {
	container.addElement(methods[i]);
      }
    }

    // look in super classes and interfaces
    if (ident != JAV_CONSTRUCTOR
	&& ident != JAV_INIT
	&& ident != JAV_STATIC_INIT) {
      if (superClass != null) {
	// java.lang.object
	getSuperClass().collectApplicableMethods(container, ident, actuals);
      }

      for (int i = 0; i < interfaces.length; i++) {
	interfaces[i].getCClass().collectApplicableMethods(container, ident, actuals);
      }
    }
  }

  /**
   * @return the short name of this class
   */
  protected static String getIdent(String qualifiedName) {
    assert(qualifiedName != null);
    String	syntheticName;

    int		cursor = qualifiedName.lastIndexOf('/');
    if (cursor > 0) {
      syntheticName = qualifiedName.substring(cursor + 1);
    } else {
      syntheticName = qualifiedName;
    }

    cursor = syntheticName.lastIndexOf('$');
    if (cursor > 0) {
      return syntheticName.substring(cursor + 1);
    } else {
      return syntheticName;
    }
  }

  /**
   * Checks whether this type is accessible from the specified class (JLS 6.6).
   *
   * @return	true iff this member is accessible
   */
  public boolean isAccessible(CClass from) {
    if (!isNested()) {
      // JLS 6.6.1
      // If a TOP LEVEL class or interface type is declared public, then it
      // may be accessed by any code, provided that the compilation unit in
      // which it is declared is observable. If a top level class or
      // interface type is not declared public, then it may be accessed only
      // from within the package in which it is declared.
      // Note (graf 010602) :
      // The words TOP LEVEL near the beginning of the first sentence have
      // been added
      return isPublic() || getPackage() == from.getPackage();
    } else {
      // Rules for members apply
      return super.isAccessible(from);
    }
  }

  /**
   * Adds a field.
   */
  public void addField(CSourceField field) {
    field.setPosition(fields.size());
    fields.put(field.getIdent(), field);
  }

  // ----------------------------------------------------------------------
  // INNER CLASS SUPPORT
  // ----------------------------------------------------------------------

  /**
   * add synthetic parameters to method def
   */
  public CType[] genConstructorArray(CType[] params) {
    throw new InconsistencyException();
  }

  /**
   * add synthetic parameters to method call
   */
  public void genOuterSyntheticParams(CodeSequence code) {//, boolean qualified)
//     if (isNested() && hasOuterThis() && !qualified) {
//       code.plantLoadThis();
//     }
  }

  /**
   * add synthetic parameters to super constructor invocation. Adds 
   * reference to enclosing reference.
   *
   * @param qualifiedAndAnonymous true, its it is a qualified
   */
  public void genSyntheticParamsFromExplicitSuper(boolean qualifiedAndAnonymous, 
                                                  CodeSequence code) {
    if (isNested() && !isStatic() && hasOuterThis()) {
      JGeneratedLocalVariable var = new JGeneratedLocalVariable(null, 
                                                                0, 
                                                                getOwner().getType(), 
                                                                JAV_OUTER_THIS, 
                                                                null);
      /* If it is not qualified, the first*/
      var.setPosition(qualifiedAndAnonymous ? 2 : 1);
      new JLocalVariableExpression(TokenReference.NO_REF, var).genCode(code, false);
    }
  }

  // ----------------------------------------------------------------------
  // PRIVATE INITIALIZATIONS
  // ----------------------------------------------------------------------

  protected static CClass	CLS_UNDEFINED = new CSourceClass(null, TokenReference.NO_REF, 0, "<gen>", "<gen>", false);

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /* final*/ String		sourceFile;    // removed final for cloner
    private /* final*/	String		qualifiedName; // removed final for cloner
    private /* final*/	String		packageName;  // removed final for cloner

  private CClassType[]		interfaces;
  private CClassType		type;
  private CClassType[]		innerClasses;
  private CClassType		superClass;

  private boolean		hasOuterThis;

  private Hashtable		fields;
  private CMethod[]		methods;
  private boolean               qualifiedAndAnonymous; 
}

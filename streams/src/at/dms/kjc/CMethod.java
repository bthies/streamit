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
 * $Id: CMethod.java,v 1.6 2003-11-13 10:46:10 thies Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;
import java.util.Enumeration;

import at.dms.classfile.InvokeinterfaceInstruction;
import at.dms.classfile.MethodInfo;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

/**
 * This class represents a class method.
 */
public abstract class CMethod extends CMember {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CMethod() {} // for cloner only

  /**
   * Constructs a method member.
   * @param	owner		the owner of this method
   * @param	modifiers	the modifiers on this method
   * @param	ident		the ident of this method
   * @param	returnType	the return type of this method
   * @param	parameters	the parameters type of this method
   * @param	exceptions	a list of all exceptions of the throws list
   * @param	deprecated	is this method deprecated
   */
  public CMethod(CClass owner,
		 int modifiers,
		 String ident,
		 CType returnType,
		 CType[] parameters,
		 CClassType[] exceptions,
		 boolean deprecated)
  {
    super(owner, modifiers, ident, deprecated);

    this.returnType = returnType;
    this.parameters = parameters;
    this.exceptions = exceptions;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return	the interface
   */
  public CMethod getMethod() {
    return this;
  }

  /**
   * @return the type of this field
   */
  public final CType getReturnType() {
    return returnType;
  }

  /**
   * @return the type of this field
   */
  public final CType[] getParameters() {
    return parameters;
  }

  /**
   * @return the type of this field
   */
  public String getSignature() {
    return CType.genMethodSignature(returnType, parameters);
  }

  /**
   * @return	the exceptions that can be thrown by this method
   */
  public CClassType[] getThrowables() {
    return exceptions;
  }

  /**
   * This method is used by initializers that knows throwables exceptions only
   * after body was checked.
   * @param	throwables	the exceptions that can be thrown by this method
   */
  public void setThrowables(Hashtable throwables) {
    if (throwables != null) {
      Enumeration	enum = throwables.elements();
      int		count = 0;

      exceptions = new CClassType[throwables.size()];
      while (enum.hasMoreElements()) {
	exceptions[count++] = ((CThrowableInfo)enum).getThrowable();
      }
    }
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (QUICK)
  // ----------------------------------------------------------------------

  /**
   * Returns true iff this method is native.
   */
  public boolean isNative() {
    return CModifier.contains(getModifiers(), ACC_NATIVE);
  }

  /**
   * Returns true iff this method is abstract.
   */
  public boolean isAbstract() {
    return CModifier.contains(getModifiers(), ACC_ABSTRACT);
  }

  /**
   * Returns true iff this method is a constructor.
   */
  public boolean isConstructor() {
    return getIdent() == JAV_CONSTRUCTOR;
  }

  // ----------------------------------------------------------------------
  // CHECK MATCHING
  // ----------------------------------------------------------------------

  /**
   * equals
   * search if two methods have same signature
   * @param	other		the other method
   */
  public boolean equals(CMethod other) {
    if (!getOwner().equals(other.getOwner())) {
      return false;
    } else if (getIdent() != other.getIdent()) {
      return false;
    } else if (parameters.length != other.parameters.length) {
      return false;
    } else {
      for (int i = 0; i < parameters.length; i++) {
	if (!parameters[i].equals(other.parameters[i])) {
	  return false;
	}
      }
      return true;
    }
  }

  /**
   * Is this method applicable to the specified invocation (JLS 15.12.2.1) ?
   * @param	ident		method invocation name
   * @param	actuals		method invocation arguments
   */
  public boolean isApplicableTo(String ident, CType[] actuals) {
    if (ident != getIdent()) {
      return false;
    } else if (actuals.length != parameters.length) {
      return false;
    } else {
      for (int i = 0; i < actuals.length; i++) {
	// method invocation conversion = assigment conversion without literal narrowing
	// we just look at the type and do not consider literal special case
	if (!actuals[i].isAssignableTo(parameters[i])) {
	  return false;
	}
      }
      return true;
    }
  }

  /**
   * Is this method more specific than the one given as argument (JLS 15.12.2.2) ?
   * @param	other		the method to compare to
   */
  public boolean isMoreSpecificThan(CMethod other) {
    if (!getOwner().getType().isAssignableTo(other.getOwner().getType())) {
      return false;
    } else if (parameters.length != other.parameters.length) {
      return false;
    } else {
      for (int i = 0; i < other.parameters.length; i++) {
	// method invocation conversion = assigment conversion without literal narrowing
	// we just look at the type and do not consider literal special case
	if (!parameters[i].isAssignableTo(other.parameters[i])) {
	  return false;
	}
      }

      return true;
    }
  }

  /**
   * Has this method the same signature as the one given as argument ?
   * NOTE: return type not considered
   * @param	other		the method to compare to
   */
  public boolean hasSameSignature(CMethod other) {
    if (parameters.length != other.parameters.length) {
      return false;
    } else {
      for (int i = 0; i < parameters.length; i++) {
	if (!parameters[i].equals(other.parameters[i])) {
	  return false;
	}
      }
      return true;
    }
  }

  /**
   * Checks that overriding/hiding is correct.
   *
   * @param	superMethod		method which it overrides
   * @exception	UnositionedError	the analysis detected an error
   */
  public void checkOverriding(CMethod superMethod) throws UnpositionedError {
    // JLS 8.4.3.3 :
    // A method can be declared final to prevent subclasses from overriding
    // or hiding it. It is a compile-time error to attempt to override or
    // hide a final method.
    if (superMethod.isFinal()) {
      throw new UnpositionedError(KjcMessages.METHOD_OVERRIDE_FINAL, this);
    }

    // JLS 8.4.6.1 :
    // A compile-time error occurs if an instance method overrides a
    // static method.
    if (!this.isStatic() && superMethod.isStatic()) {
      throw new UnpositionedError(KjcMessages.METHOD_INSTANCE_OVERRIDES_STATIC,
                                  this, 
                                  superMethod.getOwner());
    }

    // JLS 8.4.6.2 :
    // A compile-time error occurs if a static method hides an instance method.
    if (this.isStatic() && !superMethod.isStatic()) {
      throw new UnpositionedError(KjcMessages.METHOD_STATIC_HIDES_INSTANCE,
                                  this, 
                                  superMethod.getOwner());
    }

    // JLS 8.4.6.3 :
    // If a method declaration overrides or hides the declaration of another
    // method, then a compile-time error occurs if they have different return
    // types or if one has a return type and the other is void.
    if (! returnType.equals(superMethod.getReturnType())) {
      throw new UnpositionedError(KjcMessages.METHOD_RETURN_DIFFERENT, this);
    }

    // JLS 8.4.6.3 :
    // The access modifier of an overriding or hiding method must provide at
    // least as much access as the overridden or hidden method.
    boolean	moreRestrictive;

    if (superMethod.isPublic()) {
      moreRestrictive = !this.isPublic();
    } else if (superMethod.isProtected()) {
      moreRestrictive = !(this.isProtected() || this.isPublic());
    } else if (! superMethod.isPrivate()) {
      // default access
      moreRestrictive = this.isPrivate();
    } else {
      // a private method is not inherited
      throw new InconsistencyException("bad access: " + superMethod.getModifiers());
    }

    if (moreRestrictive) { 
      throw new UnpositionedError(KjcMessages.METHOD_ACCESS_DIFFERENT,
                                  this, 
                                  superMethod.getOwner());
    }

    // JLS 8.4.4 :
    // A method that overrides or hides another method, including methods that
    // implement abstract methods defined in interfaces, may not be declared to
    // throw more CHECKED exceptions than the overridden or hidden method.
    CClassType[]	exc = superMethod.getThrowables();

  _loop_:
    for (int i = 0; i < exceptions.length; i++) {
      if (exceptions[i].isCheckedException()) {
	for (int j = 0; j < exc.length; j++) {
	  if (exceptions[i].isAssignableTo(exc[j])) {
	    continue _loop_;
	  }
	}
	throw new UnpositionedError(KjcMessages.METHOD_THROWS_DIFFERENT, 
                                    this, 
                                    exceptions[i]);
      }
    }
  }


  /**
   * Returns a string representation of this method.
   */
  public String toString() {
    StringBuffer	buffer = new StringBuffer();

    buffer.append(returnType);
    buffer.append(" ");
    buffer.append(getOwner());
    buffer.append(".");
    buffer.append(getIdent());
    buffer.append("(");
    for (int i = 0; i < parameters.length; i++) {
      if (i != 0) {
	buffer.append(", ");
      }
      buffer.append(parameters[i]);
    }
    buffer.append(")");

    return buffer.toString();
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generates a sequence of bytecode
   * @param	code		the code sequence
   * @param	nonVirtual	force non-virtual dispatching
   */
  public void genCode(CodeSequence code, boolean nonVirtual) {
    if (getOwner().isInterface()) {
      int		size = 0;

      for (int i = 0; i < parameters.length; i++) {
	size += parameters[i].getSize();
      }

      code.plantInstruction(new InvokeinterfaceInstruction(getPrefixName(),
							   getIdent(),
							   getSignature(),
							   size + 1)); // this
    } else {
      int	opcode;

      if (isStatic()) {
	opcode = opc_invokestatic;
      } else if (nonVirtual || isPrivate()) {
	// JDK1.2 does not like ...|| isPrivate() || isFinal()
	opcode = opc_invokespecial;
      } else {
	opcode = opc_invokevirtual;
      }

      code.plantMethodRefInstruction(opcode,
				     getPrefixName(),
				     getIdent(),
				     getSignature());
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CType			returnType;
  private CType[]		parameters;
  private CClassType[]		exceptions;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CMethod other) {
  super.deepCloneInto(other);
  other.returnType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.returnType);
  other.parameters = (at.dms.kjc.CType[])at.dms.kjc.AutoCloner.cloneToplevel(this.parameters);
  other.exceptions = (at.dms.kjc.CClassType[])at.dms.kjc.AutoCloner.cloneToplevel(this.exceptions);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

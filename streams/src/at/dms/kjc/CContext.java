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
 * $Id: CContext.java,v 1.5 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.Compiler;
import at.dms.compiler.PositionedError;
import at.dms.compiler.UnpositionedError;
import at.dms.util.MessageDescription;
import java.io.*;

/**
 * This class represents a local context during checkBody
 * It follows the control flow and maintain informations about
 * variable (initialized, used, allocated), exceptions (thrown, catched)
 * It also verify that context is still reachable
 *
 * There is a set of utilities method to access fields, methods and class
 * with the name by clamping the parsing tree
 * @see CContext
 * @see CCompilationUnitContext
 * @see CClassContext
 * @see CMethodContext
 * @see CBodyContext
 * @see CBlockContext
 */
public abstract class CContext extends at.dms.util.Utils implements Constants {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CContext() {} // for cloner only

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   */
  protected CContext(CContext parent) {
    this.parent = parent;
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
  // ACCESSORS (LOOKUP)
  // ----------------------------------------------------------------------

  /**
   * lookupClass
   * search for a class with the provided type parameters
   * @param	caller		the class of the caller
   * @param	name		method name
   * @return	the class if found, null otherwise
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public CClassType lookupClass(CClass caller, String name) throws UnpositionedError {
    return parent.lookupClass(caller, name);
  }

  /**
   * JLS 15.12.2 :
   * Searches the class or interface to locate method declarations that are
   * both applicable and accessible, that is, declarations that can be correctly
   * invoked on the given arguments. There may be more than one such method
   * declaration, in which case the most specific one is chosen.
   *
   * @param	caller		the class of the caller
   * @param	ident		method name
   * @param	actuals		method parameters
   * @return	the method or null if not found
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CMethod lookupMethod(CClass caller, String ident, CType[] actuals)
    throws UnpositionedError
  {
    return getClassContext().lookupMethod(caller, ident, actuals);
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
    return getClassContext().lookupField(caller, ident);
  }

  /**
   * lookupLocalVariable
   * @param	ident		the name of the local variable
   * @return	a variable from an ident in current context
   */
  public JLocalVariable lookupLocalVariable(String ident) {
    return parent.lookupLocalVariable(ident);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (INFOS)
  // ----------------------------------------------------------------------


  /** 
   * JLS 8.1.2: A statement or expression occurs in a static context if and 
   * only if the innermost method, constructor, instance initializer, static 
   * initializer, field initializer, or explicit constructor statement 
   * enclosing the statement or expression is a static method, a static 
   * initializer, the variable initializer of a static variable, or an 
   * explicit constructor invocation statement 
   *
   * @return true iff the context is static
   */
  public boolean isStaticContext() {
    if (parent == null) {
      return false;
    } else {
      return parent.isStaticContext();
    }
  }

  /**
   * Returns the field definition state.
   */
  public CVariableInfo getFieldInfo() {
    return parent.getFieldInfo();
  }

  /**
   * @param	field		the definition of a field
   * @return	a field from a field definition in current context
   */
  public int getFieldInfo(int index) {
    return parent.getFieldInfo(index);
  }

  /**
   * @param	field		the definition of a field
   * @return	a field from a field definition in current context
   */
  public void setFieldInfo(int index, int info) {
    parent.setFieldInfo(index, info);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (TREE HIERARCHY)
  // ----------------------------------------------------------------------

  /**
   * getParentContext
   * @return	the parent
   */
  public CContext getParentContext() {
    return parent;
  }

  /**
   * @return	the compilation unit
   */
  public CCompilationUnitContext getCompilationUnitContext() {
    return parent.getCompilationUnitContext();
  }

  /**
   * getClassContext
   * @return	the near parent of type CClassContext
   */
  public CClassContext getClassContext() {
    return parent.getClassContext();
  }

  /**
   * getMethod
   * @return	the near parent of type CClassContext
   */
  public CMethodContext getMethodContext() {
    return parent.getMethodContext();
  }

  /**
   * Returns the nearest block context (Where yuo can define some local vars)
   */
  public CBlockContext getBlockContext() {
    return parent.getBlockContext();
  }

  // ----------------------------------------------------------------------
  // CLASS HANDLING
  // ----------------------------------------------------------------------

  /**
   * Adds a class to generate.
   */
  public void addSourceClass(CSourceClass clazz) {
    parent.addSourceClass(clazz);
  }

  // ----------------------------------------------------------------------
  // ERROR HANDLING
  // ----------------------------------------------------------------------

  /**
   * Reports a semantic error detected during analysis.
   *
   * @param	trouble		the error to report
   */
  public void reportTrouble(PositionedError trouble) {
    parent.reportTrouble(trouble);
  }

  // ----------------------------------------------------------------------
  // ERROR HANDLING
  // ----------------------------------------------------------------------

  /**
   * Throws a semantic error detected during analysis.
   *
   * @param	description	the message description
   * @param	parameters	the array of parameters
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void fail(MessageDescription description, Object[] parameters)
    throws UnpositionedError
  {
    throw new UnpositionedError(description, parameters);
  }

  /**
   * Signals a semantic error detected during analysis.
   *
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @param	parameter2	the second parameter
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void fail(MessageDescription description, Object parameter1, Object parameter2)
    throws UnpositionedError
  {
    fail(description, new Object[]{ parameter1, parameter2 });
  }

  /**
   * Verifies an assertion.
   *
   * @param	assertion	the assertion to verify
   * @param	description	the message description
   * @param	parameters	the array of parameters
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void check(boolean assertion,
		    MessageDescription description,
		    Object[] parameters)
    throws UnpositionedError
  {
    if (! assertion) {
      fail(description, parameters);
    }
  }

  /**
   * Verifies an assertion.
   *
   * @param	assertion	the assertion to verify
   * @param	description	the message description
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void check(boolean assertion, MessageDescription description)
    throws UnpositionedError
  {
    check(assertion, description, new Object[]{ null, null });
  }

  /**
   * Verifies an assertion.
   *
   * @param	assertion	the assertion to verify
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void check(boolean assertion,
		    MessageDescription description,
		    Object parameter1)
    throws UnpositionedError
  {
    check(assertion, description, new Object[]{ parameter1, null });
  }

  /**
   * Verifies an assertion.
   *
   * @param	assertion	the assertion to verify
   * @param	description	the message description
   * @param	parameter1	the first parameter
   * @param	parameter2		the second parameter
   * @exception	UnpositionedError	this error will be positioned soon
   */
  public void check(boolean assertion,
		    MessageDescription description,
		    Object parameter1,
		    Object parameter2)
    throws UnpositionedError
  {
    check(assertion, description, new Object[]{ parameter1, parameter2 });
  }

  // ----------------------------------------------------------------------
  // DEBUG
  // ----------------------------------------------------------------------

  /**
   * Dumps this context to standard error stream.
   */
  public void dumpContext(String text) {
    System.err.println("*** Dumping " + text);
    dumpContext(1);
    System.err.println("");
  }

  /**
   * Dumps this context to standard error stream.
   */
  public void dumpContext(int level) {
    dumpIndent(level);
    System.err.println(this);
    if (parent != null) {
      parent.dumpContext(level + 1);
    }
  }

  public void dumpIndent(int level) {
    for (int i = 0; i < level; i++) {
      System.err.print("  ");
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected CContext		parent;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CContext other) {
  super.deepCloneInto(other);
  other.serializationHandle = this.serializationHandle;
  other.parent = (at.dms.kjc.CContext)at.dms.kjc.AutoCloner.cloneToplevel(this.parent, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

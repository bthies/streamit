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
 * $Id: CCompilationUnitContext.java,v 1.6 2003-11-13 10:46:10 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;

import at.dms.compiler.Compiler;
import at.dms.compiler.PositionedError;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

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
public class CCompilationUnitContext extends CContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CCompilationUnitContext() {} // for cloner only

  /**
   * Constructs a compilation unit context.
   */
  CCompilationUnitContext(Compiler compiler,
			  CCompilationUnit cunit,
			  Vector classes)
  {
    super(null);
    this.compiler = compiler;
    this.cunit = cunit;
    this.classes = classes;
  }

  /**
   * Constructs a compilation unit context.
   */
  CCompilationUnitContext(Compiler compiler, CCompilationUnit cunit) {
    this(compiler, cunit, null);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (INFOS)
  // ----------------------------------------------------------------------

  /**
   * Returns the field definition state.
   */
  public CVariableInfo getFieldInfo() {
    return null;
  }

  /**
   * @param	field		the definition of a field
   * @return	a field from a field definition in current context
   */
  public int getFieldInfo(CField field) {
    return 0;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (LOOKUP)
  // ----------------------------------------------------------------------

  /**
   * @param	caller		the class of the caller
   * @return	a class according to imports or null if error occur
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CClassType lookupClass(CClass caller, String name) throws UnpositionedError {
    return cunit.lookupClass(caller, name);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (TREE HIERARCHY)
  // ----------------------------------------------------------------------

  /**
   * getParentContext
   * @return	the parent
   */
  public CContext getParentContext() {
    throw new InconsistencyException();
  }

  /**
   * getClass
   * @return	the near parent of type CClassContext
   */
  public CClassContext getClassContext() {
    return null;
  }

  /**
   * getMethod
   * @return	the near parent of type CMethodContext
   */
  public CMethodContext getMethodContext() {
    return null;
  }

  /**
   * @return	the compilation unit
   */
  public CCompilationUnitContext getCompilationUnitContext() {
    return this;
  }

  public CBlockContext getBlockContext() {
    return null;
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
    compiler.reportTrouble(trouble);
  }

  // ----------------------------------------------------------------------
  // CLASS HANDLING
  // ----------------------------------------------------------------------

  /**
   * Adds a class to generate
   */
  public void addSourceClass(CSourceClass clazz) {
    classes.addElement(clazz);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private /*final*/ Compiler		compiler;
  private /*final*/ Vector		classes;
  private /*final*/ CCompilationUnit	cunit;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CCompilationUnitContext other = new at.dms.kjc.CCompilationUnitContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CCompilationUnitContext other) {
  super.deepCloneInto(other);
  other.compiler = (at.dms.compiler.Compiler)at.dms.kjc.AutoCloner.cloneToplevel(this.compiler);
  other.classes = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.classes);
  other.cunit = (at.dms.kjc.CCompilationUnit)at.dms.kjc.AutoCloner.cloneToplevel(this.cunit);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

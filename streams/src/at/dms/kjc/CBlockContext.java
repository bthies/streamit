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
 * $Id: CBlockContext.java,v 1.6 2003-11-13 10:46:10 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;
import java.util.Hashtable;

import at.dms.compiler.CWarning;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.MessageDescription;

/**
 * This class represents a local context during checkBody
 * It follows the control flow and maintain informations about
 * variable (initialized, used, allocated), exceptions (thrown, catched)
 * It also verify that context is still reachable
 *
 * There is a set of utilities method to access fields, methods and class
 * with the name by clamping the parsing tree
 * @see CCompilationUnitContext
 * @see CClassContext
 * @see CMethodContext
 * @see CContext
 */
public class CBlockContext extends CBodyContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CBlockContext() {} // for cloner only

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   */
  CBlockContext(CMethodContext parent, int localVars) {
    super(parent);

    this.localVars = localVars == 0 ? null : new Vector(localVars);
    this.localsPosition = 0;
    this.parentIndex = 0;
  }

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   */
  public CBlockContext(CBodyContext parent) {
    this(parent, 5);
  }

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   */
  public CBlockContext(CBodyContext parent, int predictedVars) {
    super(parent);

    this.localVars = new Vector(predictedVars);

    CBlockContext	parentBlock = parent.getBlockContext();

    this.localsPosition = parentBlock.localsPosition();
    this.parentIndex = parentBlock.localsIndex();
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Verify everything is okay at the end of this context
   */
  public void close(TokenReference ref) {
    // default, no errors
    verifyLocalVarUsed();

    super.close(ref);
  }

  /**
   * check everything correct
   */
  private void verifyLocalVarUsed() {
    for (int i = 0; i < localsIndex; i++) {
      JLocalVariable	var = (JLocalVariable)localVars.elementAt(i);

      if (!var.isUsed() && !var.getIdent().startsWith("_")) {
	MessageDescription		mesg = null;

	switch (var.getDescription()) {
	case JLocalVariable.DES_PARAMETER:
	  mesg = KjcMessages.UNUSED_PARAMETER;
 	  break;
	case JLocalVariable.DES_CATCH_PARAMETER:
	  mesg = KjcMessages.UNUSED_CATCH_PARAMETER;
 	  break;
	case JLocalVariable.DES_LOCAL_VAR:
	  mesg = KjcMessages.UNUSED_LOCALVAR;
 	  break;
	default:
	  continue;
	}
	reportTrouble(new CWarning(var.getTokenReference(),
				   mesg,
				   var.getIdent()));
      } else {
	if (var.getDescription() == JLocalVariable.DES_LOCAL_VAR && !var.isFinal()) {
	  reportTrouble(new CWarning(var.getTokenReference(),
				     KjcMessages.CONSTANT_VARIABLE_NOT_FINAL,
				     var.getIdent()));
	}
      }
    }
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * addLocal variable
   * @param	var		the name of the variable
   * @param	initialized	is the varaible already initialized
   * @exception UnpositionedError	this error will be positioned soon
   */
  public void addVariable(JLocalVariable var) throws UnpositionedError {
    if (localVars == null) {
      localVars = new Vector();
    }

    // verify that the variable is not defined in this or an enclosing block
    check(lookupLocalVariable(var.getIdent()) == null,
	  KjcMessages.VARIABLE_REDECLARED, var.getIdent());

    var.setPosition(localsPosition);
    var.setIndex(localsIndex + parentIndex);

    localVars.addElement(var);
    assert(++localsIndex == localVars.size()); // $$$ 2 infos
    localsPosition += var.getType().getSize();
  }

  /**
   * lookupLocalVariable
   * @param	ident		the name of the variable
   * @return	a variable from an ident in current context
   */
  public JLocalVariable lookupLocalVariable(String ident) {
    if (localVars != null) {
      for (int i = 0; i < localsIndex; i++) {
	JLocalVariable	var = (JLocalVariable)localVars.elementAt(i);

	if (var.getIdent() == ident) {
	  return var;
	}
      }
    }

    return parent.lookupLocalVariable(ident);
  }

  /**
   * addLocal variable
   * @param	var		the name of the variable
   * @param	initialized	is the varaible already initialized
   */
  public void addThisVariable() {
    localsPosition += 1;
  }

  public int localsPosition() {
    return localsPosition;
  }

  public int localsIndex() {
    return parentIndex + localsIndex;
  }

  public CBlockContext getBlockContext() {
    return this;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (TYPE DEFINITION)
  // ----------------------------------------------------------------------

  /**
   * addLocalClass
   * @param	clazz		the clazz to add
   * @exception UnpositionedError	this error will be positioned soon
   */
  public void addClass(CClass clazz) throws UnpositionedError {
    if (localClasses == null) {
      localClasses = new Hashtable();
    }
    Object	old = localClasses.put(clazz.getIdent(), clazz);
    if (old != null) {
      throw new UnpositionedError(KjcMessages.CLAZZ_RENAME, clazz.getIdent());
    }
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (LOOKUP)
  // ----------------------------------------------------------------------

  /**
   * lookupClass
   * search for a class with the provided type parameters
   * @param	caller		the class of the caller
   * @param	ident		the class name
   * @return	the class if found, null otherwise
   * @exception UnpositionedError	this error will be positioned soon
   */
  public CClassType lookupClass(CClass caller, String ident) throws UnpositionedError {
    // search local class first
    if (localClasses != null) {
      CClass	clazz;

      clazz = (CClass)localClasses.get(ident);
      if (clazz != null) {
        if (clazz.isAccessible(caller)) {
          return clazz.getType();
        } else {
          throw new UnpositionedError(KjcMessages.CLASS_NOACCESS, clazz.getIdent());
        }
      }
    }

    return super.lookupClass(caller, ident);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private Hashtable				localClasses;
  private Vector				localVars;

  private /*final*/ int				parentIndex;
  private int					localsIndex;

  private int					localsPosition;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CBlockContext other = new at.dms.kjc.CBlockContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CBlockContext other) {
  super.deepCloneInto(other);
  other.localClasses = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.localClasses);
  other.localVars = (java.util.Vector)at.dms.kjc.AutoCloner.cloneToplevel(this.localVars);
  other.parentIndex = this.parentIndex;
  other.localsIndex = this.localsIndex;
  other.localsPosition = this.localsPosition;
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

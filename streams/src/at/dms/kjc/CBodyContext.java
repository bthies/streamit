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
 * $Id: CBodyContext.java,v 1.3 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

import java.util.Enumeration;
import java.util.Hashtable;

import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class represents a local context during checkBody
 * It follows the control flow and maintain informations about
 * variable (initialized, used, allocated), exceptions (thrown, catched)
 * It also verify that context is still reachable
 *
 * There is a set of utilities method to access fields, methods and class
 * with the name by clamping the parsing tree
 */

public abstract class CBodyContext extends CContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CBodyContext() {} // for cloner only

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * This starts a new context in a method
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   */
  protected CBodyContext(CBodyContext parent) {
    super(parent);

    flowState = parent.flowState;
    variableInfo = null;
    fieldInfo = null;
    throwables = parent.throwables;
  }

  /**
   * Construct a block context, it supports local variable allocation
   * throw statement and return statement
   * This starts a new context in a method
   * @param	parent		the parent context, it must be different
   *				than null except if called by the top level
   */
  protected CBodyContext(CMethodContext parent) {
    super(parent);

    flowState = 0;
    variableInfo = null;
    fieldInfo = null;
    throwables = parent.getThrowables();
  }

  /**
   *
   */
  protected CBodyContext(CBodyContext parent, CBodyContext source)
  {
    super(parent);

    flowState = source.flowState;
    variableInfo = (CVariableInfo)source.getVariableInfo().clone();
    fieldInfo = (CVariableInfo)source.getFieldInfo().clone();
    throwables = parent.throwables;
  }

  /**
   * Returns a copy of this context.
   */
  public final CSimpleBodyContext cloneContext() {
    return new CSimpleBodyContext(this, this);
  }

  /**
   * Verify everything is okay at the end of this context
   */
  public void close(TokenReference ref) {
    informParent();
  }

  /**
   * Verify everything is okay at the end of this context
   */
  private void informParent() {
    // inform parent
    if (parent instanceof CBodyContext) {
      CBodyContext	parent = (CBodyContext)this.parent;

      if (variableInfo != null) {
	int	parentPosition = parent.getBlockContext().localsIndex();

	for (int i = 0; i < parentPosition; i++) {
	  int	info = this.variableInfo.getInfo(i);

	  if (info != 0) {
	    parent.setVariableInfo(i, info);
	  }
	}
      }

      parent.adoptFlowState(this);
    }

    if (fieldInfo != null) {
      int	parentPosition = parent.getClassContext().getCClass().getFieldCount();

      for (int i = 0; i < parentPosition; i++) {
	int	info = fieldInfo.getInfo(i);

	if (info != 0) {
	  parent.setFieldInfo(i, info);
	}
      }
    }
  }

  public void merge(CBodyContext source) {
    mergeFlowState(source);
    mergeVariableInfos(source);
    mergeFieldInfos(source);
  }

  public void adopt(CBodyContext source) {
    adoptFlowState(source);
    adoptVariableInfos(source);
    adoptFieldInfos(source);
  }

  public void adoptFlowState(CBodyContext source) {
    flowState = source.flowState;
  }

  public void mergeFlowState(CBodyContext source) {
    flowState &= source.flowState;
  }

  public void adoptVariableInfos(CBodyContext source) {
    int		parentPosition = getBlockContext().localsIndex();

    for (int i = 0; i < parentPosition; i++) {
      int	info = source.getVariableInfo(i);

      if (info != 0) {
	setVariableInfo(i, info);
      }
    }
  }

  public void mergeVariableInfos(CBodyContext source) {
    if (variableInfo != null || source.variableInfo != null) {
      int	parentPosition = getBlockContext().localsIndex();

      for (int i = 0; i < parentPosition; i++) {
	int	info1 = getVariableInfo(i);
	int	info2 = source.getVariableInfo(i);

	if (info1 + info2 != 0) {
	  if (variableInfo == null) {
	    variableInfo = getVariableInfo();
	  }
	  variableInfo.setInfo(i, CVariableInfo.merge(info1, info2));
	}
      }
    }
  }

  public void adoptFieldInfos(CContext source) {
    int		parentPosition = parent.getClassContext().getCClass().getFieldCount();

    for (int i = 0; i < parentPosition; i++) {
      int	info = source.getFieldInfo(i);

      if (info != 0) {
	setFieldInfo(i, info);
      }
    }
  }

  public void mergeFieldInfos(CBodyContext source) {
    if (fieldInfo != null || source.fieldInfo != null) {
      int	parentPosition = parent.getClassContext().getCClass().getFieldCount();

      for (int i = 0; i < parentPosition; i++) {
	int	info1 = getFieldInfo(i);
	int	info2 = source.getFieldInfo(i);

	if (info1 + info2 != 0) {
	  if (fieldInfo == null) {
	    fieldInfo = getFieldInfo();
	  }
	  fieldInfo.setInfo(i, CVariableInfo.merge(info1, info2));
	}
      }
    }
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * lookupOuterLocalVariable
   * @param	ident		the name of the variable
   * @return	a variable from an ident in upperclass context
   */
  public JExpression lookupOuterLocalVariable(TokenReference ref, String ident) {
    CClassContext	owner = getClassContext();

    return owner.lookupOuterLocalVariable(ref, ident);
  }

  // ----------------------------------------------------------------------
  // FLOW STATE
  // ----------------------------------------------------------------------

  /**
   *
   */
  public void clearFlowState() {
    flowState = 0;
  }

  /**
   * Declares execution to be in the body of a loop.
   *
   * @param	inloop		is execution in the body of a loop ?
   */
  public void setInLoop(boolean inloop) {
    if (inloop) {
      flowState |= FLO_INLOOP;
    } else {
      flowState &= ~FLO_INLOOP;
    }
  }

  /**
   * Returns true iff execution is in the body of a loop.
   */
  public boolean isInLoop() {
    return (flowState & FLO_INLOOP) != 0;
  }

  /**
   * Declares next statement to be reachable.
   *
   * @param	reachable	is the next statement reachable ?
   */
  public void setReachable(boolean reachable) {
    if (reachable) {
      flowState &= ~FLO_UNREACHABLE;
    } else {
      flowState |= FLO_UNREACHABLE;
    }
  }

  /**
   * Returns true iff the statement is reachable.
   */
  public boolean isReachable() {
    return (flowState & FLO_UNREACHABLE) == 0;
  }

  /**
   *
   */
  public void addBreak(JStatement target) {
    addBreak(target, this);
    flowState |= FLO_UNREACHABLE | FLO_BREAKED;
  }

  /**
   *
   */
  protected void addBreak(JStatement target,
			  CBodyContext context)
  {
    ((CBodyContext)getParentContext()).addBreak(target, context);
  }

  /**
   * @return	if this context was set unreachable by a break stmt
   */
  public boolean isBreaked() {
    return (flowState & FLO_BREAKED) != 0;
  }

  /**
   *
   */
  public void addContinue(JStatement target) {
    addContinue(target, this);
    flowState |= FLO_UNREACHABLE | FLO_CONTINUED;
  }

  /**
   *
   */
  protected void addContinue(JStatement target,
			     CBodyContext context)
  {
    ((CBodyContext)getParentContext()).addContinue(target, context);
  }

  /**
   * @return	if this context was set unreachable by a continue stmt
   */
  public boolean isContinued() {
    return (flowState & FLO_CONTINUED) != 0;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (VARIABLE)
  // ----------------------------------------------------------------------

  public int localsPosition() {
    return ((CBodyContext)parent).localsPosition();
  }

  /**
   * Returns the statement with the specified label.
   */
  public JStatement getLabeledStatement(String label) {
    return parent instanceof CBodyContext ? // $$$ remove ths test
      ((CBodyContext)parent).getLabeledStatement(label) :
      null;
  }

  /**
   * Returns the innermost statement which can be target of a break
   * statement without label.
   */
  public JStatement getNearestBreakableStatement() {
    return parent instanceof CBodyContext ? // $$$ remove ths test
      ((CBodyContext)parent).getNearestBreakableStatement() :
      null;
  }

  /**
   * Returns the innermost statement which can be target of a continue
   * statement without label.
   */
  public JStatement getNearestContinuableStatement() {
    return parent instanceof CBodyContext ?
      ((CBodyContext)parent).getNearestContinuableStatement() :
      null;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS (INFOS)
  // ----------------------------------------------------------------------

  /**
   *
   */
  protected CVariableInfo getVariableInfo() {
    if (variableInfo != null) {
      return variableInfo;
    } else if (parent instanceof CBodyContext) {
      return ((CBodyContext)parent).getVariableInfo();
    } else {
      return new CVariableInfo();
    }
  }

  /**
   * @param	index		The variable position in method array of local vars
   * @param	info		The information to add
   *
   * We make it a local copy of this information and at the end of this context
   * we will transfer it to the parent context according to controlFlow
   */
  public void setVariableInfo(int index, int info) {
    if (variableInfo == null) {
      variableInfo = (CVariableInfo)getVariableInfo().clone();
    }
    variableInfo.setInfo(index, info);
  }

  /**
   * @param	var		the definition of a variable
   * @return	all informations we have about this variable
   */
  public int getVariableInfo(int index) {
    if (variableInfo != null) {
      return variableInfo.getInfo(index);
    } else if (parent instanceof CBodyContext) {
      return ((CBodyContext)parent).getVariableInfo(index);
    } else {
      return 0;
    }
  }

  /**
   * @param	index		The field position in method array of local vars
   * @param	info		The information to add
   *
   * We make it a local copy of this information and at the end of this context
   * we will transfert it to the parent context according to controlFlow
   */
  public void setFieldInfo(int index, int info) {
    if (fieldInfo == null) {
      fieldInfo = (CVariableInfo)getFieldInfo().clone();
    }
    fieldInfo.setInfo(index, info);
  }

  /**
   * @param	var		the definition of a field
   * @return	all informations we have about this field
   */
  public int getFieldInfo(int index) {
    if (fieldInfo == null) {
      return parent.getFieldInfo(index);
    } else {
      return fieldInfo.getInfo(index);
    }
  }

  /**
   * Returns the field definition state.
   */
  public CVariableInfo getFieldInfo() {
    if (fieldInfo != null) {
      return fieldInfo;
    } else {
      return parent.getFieldInfo();
    }
  }

  // ----------------------------------------------------------------------
  // THROWABLES
  // ----------------------------------------------------------------------

  /**
   *
   */
  public void clearThrowables() {
    throwables = new Hashtable();
  }

  /**
   * @param	throwable	the type of the new throwable
   */
  public void addThrowable(CThrowableInfo throwable) {
    if (throwables == null) {
      throwables = new Hashtable();
    }
    throwables.put(throwable, throwable);
  }

  /**
   *
   */
  public void mergeThrowables(CBodyContext source) {
    Enumeration		enum = source.getThrowables().elements();

    while (enum.hasMoreElements()) {
      addThrowable((CThrowableInfo)enum.nextElement());
    }
  }

  /**
   *
   */
  public void adoptThrowables(CBodyContext source) {
    throwables = new Hashtable();
    mergeThrowables(source);
  }

  /**
   * @return the list of exception that may be thrown
   */
  public Hashtable getThrowables() {
    return throwables == null ? EMPTY : throwables;
  }

  // ----------------------------------------------------------------------
  // DEBUG
  // ----------------------------------------------------------------------

  /**
   * Dumps this context to standard error stream.
   */
  public void dumpContext(int level) {
    dumpIndent(level);
    System.err.println(this + " " + getBlockContext().localsIndex());
    dumpIndent(level);
    System.err.println("info: "
		       + ((flowState & FLO_UNREACHABLE) != 0 ? "unreach " : "")
		       + ((flowState & FLO_BREAKED) != 0 ? "breaked " : "")
		       + ((flowState & FLO_CONTINUED) != 0 ? "contind " : "")
		       + ((flowState & FLO_INLOOP) != 0 ? "inloop  " : ""));
    dumpIndent(level);
    System.err.print("vars: ");
    if (variableInfo == null) {
      System.err.print("---");
    } else {
      for (int i = 0; i < 8; i++) {
	System.err.print(" " + i + ":" + getVariableInfo(i));
      }
    }
    dumpIndent(level);
    System.err.println("");
    dumpIndent(level);
    System.err.print("flds: ");
    if (fieldInfo == null) {
      System.err.print("---");
    } else {
      for (int i = 0; i < 8; i++) {
	System.err.print(" " + i + ":" + getFieldInfo(i));
      }
    }
    dumpIndent(level);
    System.err.println("");
    dumpIndent(level);
    System.err.print("excp: ");
    if (throwables == null) {
      System.err.print("---");
    } else {
      for (Enumeration e = throwables.elements(); e.hasMoreElements(); ) {
	System.err.print(" " + ((CThrowableInfo)e.nextElement()).getThrowable());
      }
    }
    dumpIndent(level);
    System.err.println("");
    if (parent != null) {
      parent.dumpContext(level + 1);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private static final Hashtable	EMPTY = new Hashtable(1);

  private static final int	FLO_UNREACHABLE = 1 << 0;
  private static final int	FLO_BREAKED = 1 << 1;
  private static final int	FLO_CONTINUED = 1 << 2;
  private static final int	FLO_INLOOP = 1 << 3;

  private int			flowState;
  private CVariableInfo		variableInfo;
  private CVariableInfo		fieldInfo;
  private Hashtable		throwables;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CBodyContext other) {
  super.deepCloneInto(other);
  other.flowState = this.flowState;
  other.variableInfo = (at.dms.kjc.CVariableInfo)at.dms.kjc.AutoCloner.cloneToplevel(this.variableInfo);
  other.fieldInfo = (at.dms.kjc.CVariableInfo)at.dms.kjc.AutoCloner.cloneToplevel(this.fieldInfo);
  other.throwables = (java.util.Hashtable)at.dms.kjc.AutoCloner.cloneToplevel(this.throwables);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: CLoopContext.java,v 1.4 2003-05-28 05:58:42 thies Exp $
 */

package at.dms.kjc;

/**
 * This class provides the contextual information for the semantic
 * analysis loop statements.
 */
public class CLoopContext extends CBodyContext {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected CLoopContext() {} // for cloner only

  /**
   * Constructs the context to analyse a loop statement semantically.
   * @param	parent		the parent context
   * @param	stmt		the loop statement
   */
  CLoopContext(CBodyContext parent, JLoopStatement stmt) {
    super(parent);

    this.stmt = stmt;

    setInLoop(true);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the innermost statement which can be target of a break
   * statement without label.
   */
  public JStatement getNearestBreakableStatement() {
    return stmt;
  }

  /**
   * Returns the innermost statement which can be target of a continue
   * statement without label.
   */
  public JStatement getNearestContinuableStatement() {
    return stmt;
  }

  /**
   *
   */
  protected void addBreak(JStatement target,
			  CBodyContext context)
  {
    if (stmt == target) {
      if (breakContextSummary == null) {
	breakContextSummary = context.cloneContext();
      } else {
	breakContextSummary.merge(context);
      }
      breakContextSummary.setReachable(true);
    } else {
      ((CBodyContext)getParentContext()).addBreak(target, context);
    }
  }

  /**
   *
   */
  protected void addContinue(JStatement target,
			     CBodyContext context)
  {
    if (stmt == target) {
      if (continueContextSummary == null) {
	continueContextSummary = context.cloneContext();
      } else {
	continueContextSummary.merge(context);
      }
    } else {
      ((CBodyContext)getParentContext()).addContinue(target, context);
    }
  }

  /**
   * Checks whether this statement is target of a break statement.
   *
   * @return	true iff this statement is target of a break statement.
   */
  public boolean isBreakTarget() {
    return breakContextSummary != null;
  }

  /**
   * Returns the context state after break statements.
   */
  public CBodyContext getBreakContextSummary() {
    return breakContextSummary;
  }

  /**
   * Checks whether this statement is target of a continue statement.
   *
   * @return	true iff this statement is target of a continue statement.
   */
  public boolean isContinueTarget() {
    return continueContextSummary != null;
  }

  /**
   * Returns the context state after continue statements.
   */
  public CBodyContext getContinueContextSummary() {
    return continueContextSummary;
  }



  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    private /*final*/ JLoopStatement		stmt; // removed final for cloner
  private CBodyContext			breakContextSummary;
  private CBodyContext			continueContextSummary;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.CLoopContext other = new at.dms.kjc.CLoopContext();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.CLoopContext other) {
  super.deepCloneInto(other);
  other.stmt = (at.dms.kjc.JLoopStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.stmt);
  other.breakContextSummary = (at.dms.kjc.CBodyContext)at.dms.kjc.AutoCloner.cloneToplevel(this.breakContextSummary);
  other.continueContextSummary = (at.dms.kjc.CBodyContext)at.dms.kjc.AutoCloner.cloneToplevel(this.continueContextSummary);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

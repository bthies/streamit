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
 * $Id: JStatement.java,v 1.7 2003-08-29 19:25:37 thies Exp $
 */

package at.dms.kjc;

import at.dms.util.*;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;
import at.dms.util.MessageDescription;
import at.dms.util.InconsistencyException;

/**
 * JLS 14.5: Statement
 * This class is the root class for all statement classes.
 */
public abstract class JStatement extends JPhylum {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JStatement() {} // for cloner only
 /**
   * Construct a node in the parsing tree
   * @param where the line of this node in the source code
   */
  public JStatement(TokenReference where, JavaStyleComment[] comments) {
    super(where);
    this.comments = comments;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the statement (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public abstract void analyse(CBodyContext context) throws PositionedError;

  /**
   * Adds a compiler error.
   * @param	context		the context in which the error occurred
   * @param	key		the message ident to be displayed
   * @param	params		the array of parameters
   *
   */
  protected void fail(CContext context, MessageDescription key, Object[] params)
    throws PositionedError
  {
    throw new CLineError(getTokenReference(), key, params);
  }

  // ----------------------------------------------------------------------
  // BREAK/CONTINUE HANDLING
  // ----------------------------------------------------------------------

  /**
   * Returns a label at end of this statement (for break statement)
   */
  public CodeLabel getBreakLabel() {
    throw new InconsistencyException("NO END LABEL");
  }

  /**
   * Returns the beginning of this block (for continue statement)
   */
  public CodeLabel getContinueLabel() {
    throw new InconsistencyException("NO CONTINUE LABEL");
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public abstract void genCode(CodeSequence code);

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    if (comments != null) {
      p.visitComments(comments);
    }
  }
    
    

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public abstract Object accept(AttributeVisitor p);

  /**
   * Returns the comments
   */
  public JavaStyleComment[] getComments() {
    return comments;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  private JavaStyleComment[]	comments;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JStatement other) {
  super.deepCloneInto(other);
  other.comments = (at.dms.compiler.JavaStyleComment[])at.dms.kjc.AutoCloner.cloneToplevel(this.comments, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

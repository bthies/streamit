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
 * $Id: JLabeledStatement.java,v 1.6 2003-05-28 05:58:44 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.7: Labeled Statement
 *
 * Statements may have label prefixes.
 */
public class JLabeledStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JLabeledStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	label		the label of the enclosing labeled statement
   * @param	body		the contained statement
   * @param	comments	comments in the source text
   */
  public JLabeledStatement(TokenReference where,
			   String label,
			   JStatement body,
			   JavaStyleComment[] comments)
  {
    super(where, comments);

    this.label = label;
    this.body = body;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the label of this statement.
   */
  /*package*/ String getLabel() {
    return label;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the statement (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CBodyContext context) throws PositionedError {
    check(context,
	  context.getLabeledStatement(label) == null,
	  KjcMessages.LABEL_ALREADY_EXISTS, label);

    CLabeledContext	labeledContext;

    labeledContext = new CLabeledContext(context, this);
    body.analyse(labeledContext);
    labeledContext.close(getTokenReference());
  }

  // ----------------------------------------------------------------------
  // BREAK/CONTINUE HANDLING
  // ----------------------------------------------------------------------

  /**
   * Returns the actual target statement of a break or continue whose
   * label is the label of this statement.
   *
   * If the statement referencing this labeled statement is either a break
   * or a continue statement :
   * - if it is a continue statement, the target is the contained statement
   *   which must be a loop statement.
   * - if it is a break statement, the target is the labeled statement
   *   itself ; however, if the contained statement is a loop statement,
   *   the target address for a break of the contained statement is the same
   *   as the target address of this labeled statement.
   * Thus, if the contained statement is a loop statement, the target
   * for a break or continue to this labeled statement is the same as the
   * target address of this labeled statement.
   */
  public JStatement getTargetStatement() {
    if (body instanceof JLoopStatement) {
      // JLS 14.15: do, while or for statement
      return body;
    } else {
      return this;
    }
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    super.accept(p);
    p.visitLabeledStatement(this, label, body);
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
    return p.visitLabeledStatement(this, label, body);
  } 
      

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    endLabel = new CodeLabel();
    body.genCode(code);
    code.plantLabel(endLabel);
    endLabel = null;
  }

  /**
   * Returns the end of this block (for break statement).
   */
  public CodeLabel getBreakLabel() {
    return endLabel;
  }

    /**
     * Set the body of this.
     */
    public void setBody(JStatement body) {
	this.body = body;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private String		label;
  private JStatement		body;
  private CodeLabel		endLabel;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JLabeledStatement other = new at.dms.kjc.JLabeledStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JLabeledStatement other) {
  super.deepCloneInto(other);
  other.label = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.label);
  other.body = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.body);
  other.endLabel = (at.dms.kjc.CodeLabel)at.dms.kjc.AutoCloner.cloneToplevel(this.endLabel);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JSynchronizedStatement.java,v 1.8 2003-11-13 10:46:11 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.18: Synchronized Statement
 *
 * A synchronized statement acquires a mutual-exclusion lock on behalf
 * of the executing thread, executes a block, then releases the lock.
 */
public class JSynchronizedStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JSynchronizedStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	cond		the expression to evaluate.
   * @param	body		the loop body.
   */
  public JSynchronizedStatement(TokenReference where,
				JExpression cond,
				JStatement body,
				JavaStyleComment[] comments)
  {
    super(where, comments);
    this.cond = cond;
    this.body = body;
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
    localVar = new JGeneratedLocalVariable(null,
					   0,
					   CStdType.Integer,
					   "sync$" + toString() /* unique ID */,
					   null);
    try {
      context.getBlockContext().addVariable(localVar);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
    cond = cond.analyse(new CExpressionContext(context));
    check(context, cond.getType().isReference(), KjcMessages.SYNCHRONIZED_NOTREFERENCE);
    body.analyse(context);
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
    p.visitSynchronizedStatement(this, cond, body);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
    return p.visitSynchronizedStatement(this, cond, body);
  }

      

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genMonitorExit(CodeSequence code) {
    code.plantLocalVar(opc_aload, localVar);
    code.plantNoArgInstruction(opc_monitorexit);
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    CodeLabel		nextLabel = new CodeLabel();

    cond.genCode(code, false);
    code.plantNoArgInstruction(opc_dup);
    code.plantLocalVar(opc_astore, localVar);
    code.plantNoArgInstruction(opc_monitorenter);

    code.pushContext(this);
    int		startPC = code.getPC();
    body.genCode(code);
    code.popContext(this);
    genMonitorExit(code);			//!!! CHECK : inside ?
    int		endPC = code.getPC();
    code.plantJumpInstruction(opc_goto, nextLabel);

    int	errorPC = code.getPC();
    genMonitorExit(code);
    code.plantNoArgInstruction(opc_athrow);

    code.plantLabel(nextLabel);			//	next:	...

    // protect
    code.addExceptionHandler(startPC, endPC, errorPC, null);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		cond;
  private JStatement		body;
  private JLocalVariable	localVar;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JSynchronizedStatement other = new at.dms.kjc.JSynchronizedStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JSynchronizedStatement other) {
  super.deepCloneInto(other);
  other.cond = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.cond);
  other.body = (at.dms.kjc.JStatement)at.dms.kjc.AutoCloner.cloneToplevel(this.body);
  other.localVar = (at.dms.kjc.JLocalVariable)at.dms.kjc.AutoCloner.cloneToplevel(this.localVar);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

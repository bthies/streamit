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
 * $Id: JTryCatchStatement.java,v 1.5 2003-08-21 09:44:21 thies Exp $
 */

package at.dms.kjc;

import java.util.Enumeration;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.JavaStyleComment;

/**
 * JLS 14.19: Try Statement
 *
 * A try statement executes a block.
 * If a value is thrown and the try statement has one or more catch
 * clauses that can catch it, then control will be transferred to the
 * first such catch clause.
 * If the try statement has a finally clause, then another block of code
 * is executed, no matter whether the try block completes normally or abruptly,
 * and no matter whether a catch clause is first given control.
 *
 * In this implementation, the Try Statement is split into a Try-Catch Statement
 * and a Try-Finally Statement. A Try Statement where both catch and finally
 * clauses are present is rewritten as a Try-Catch Statement enclosed in a
 * Try-Finally Statement.
 */
public class JTryCatchStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JTryCatchStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where			the line of this node in the source code
   * @param	tryClause		the body
   * @param	catchClauses		a vector of catch clause
   */
  public JTryCatchStatement(TokenReference where,
			    JBlock tryClause,
			    JCatchClause[] catchClauses,
			    JavaStyleComment[] comments)
  {
    super(where, comments);

    this.tryClause = tryClause;
    this.catchClauses = catchClauses;
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
    /*
     * Analyse the try clause.
     */
    CTryContext		tryContext;

    tryContext = new CTryContext(context);
    tryClause.analyse(tryContext);
    if (tryContext.isReachable()) {
      context.adopt(tryContext);
    }
    context.setReachable(tryContext.isReachable());

    /*
     * JLS 14.20 :
     * A try-catch statement can complete normally iff :
     * - the try block can complete normally or
     * - any catch block can complete normally.
     */

    /*
     * JLS 14.20 :
     * The try block is reachable iff the try statement is reachable.
     * A catch block C is reachable iff both of the following are true :
     * - Some expression or throw statement in the try block is reachable
     *   and can throw an exception whose type is assignable to the parameter
     *   of the catch clause C.
     * - There is no earlier catch block A in the try statement such that
     *   the type of C's parameter is the same as or a subclass of the
     *   type of A's parameter.
     *
     * Note : as show in http://www.ergnosis.com/java-spec-report,
     * - "is assignable" should be replaced by "is cast convertible"
     * - a catch clause is always reachable if its parameter's type is an
     *   unchecked exception class, Exception, or Throwable
     */

    /*
     * Analyse each catch clause. In a first step, assume that every
     * catch clause is reachable.
     */
    for (int i = 0; i < catchClauses.length; i++) {
      CCatchContext	catchContext;

      catchContext = new CCatchContext(context);
      catchContext.adopt(tryContext);
      catchContext.setReachable(true);

      catchClauses[i].analyse(catchContext);
      if (catchContext.isReachable()) {
	if (! context.isReachable()) {
	  context.adopt(catchContext);
	  context.setReachable(true);
	} else {
	  context.merge(catchContext);
	}
      }
      context.mergeThrowables(catchContext);
    }

    /*
     * Check that every catch clause is reachable.
     */
    boolean[]	catchReachable = new boolean[catchClauses.length];

    Enumeration	enum = tryContext.getThrowables().elements();
    while (enum.hasMoreElements()) {
      CThrowableInfo	info = (CThrowableInfo)enum.nextElement();
      CClassType	type = info.getThrowable();
      boolean		consumed = false;

      for (int i = 0; !consumed && i < catchClauses.length; i++) {
	if (type.isCastableTo(catchClauses[i].getType())) {
	  catchReachable[i] = true;
	  consumed = type.isAssignableTo(catchClauses[i].getType());
	}
      }
      if (!consumed) {
	context.addThrowable(info);
      }
    }

    /*
     * Mark each catch clause reachable reachable if its parameter's
     * type is an unchecked exception class, Exception, or Throwable.
     */
    for (int i = 0; i < catchClauses.length; i++) {
      CClassType	type = catchClauses[i].getType();

      if (!catchReachable[i]
	  && (! type.isCheckedException()
	      || type.equals(CStdType.Throwable)
	      || type.equals(CStdType.Exception))) {
	catchReachable[i] = true;
      }
    }

    /*
     * Check there is no earlier catch clause of the try statement
     * which can handle the same exception.
     */
    for (int i = 0; i < catchClauses.length; i++) {
      if (catchReachable[i]) {
	for (int j = i + 1; j < catchClauses.length; j++) {
	  if (catchReachable[j]
	      && catchClauses[j].getType().isAssignableTo(catchClauses[i].getType())) {
	    catchReachable[j] = false;
	  }
	}
      }
    }

    for (int i = 0; i < catchClauses.length; i++) {
      if (! catchReachable[i]) {
	context.reportTrouble(new PositionedError(catchClauses[i].getTokenReference(),
						  KjcMessages.CATCH_UNREACHABLE));
      }
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
    p.visitTryCatchStatement(this, tryClause, catchClauses);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitTryCatchStatement(this, tryClause, catchClauses);
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    CodeLabel		nextLabel = new CodeLabel();

    int		startPC = code.getPC();
    tryClause.genCode(code);			//		TRY CODE
    int		endPC = code.getPC();
    code.plantJumpInstruction(opc_goto, nextLabel);	//		GOTO next
    for (int i = 0; i < catchClauses.length; i++) {
      catchClauses[i].genCode(code, startPC, endPC);
      code.plantJumpInstruction(opc_goto, nextLabel);	//		GOTO next
    }
    code.plantLabel(nextLabel);			//	next:	...
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JBlock		tryClause;
  private JCatchClause[]	catchClauses;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JTryCatchStatement other = new at.dms.kjc.JTryCatchStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JTryCatchStatement other) {
  super.deepCloneInto(other);
  other.tryClause = (at.dms.kjc.JBlock)at.dms.kjc.AutoCloner.cloneToplevel(this.tryClause, this);
  other.catchClauses = (at.dms.kjc.JCatchClause[])at.dms.kjc.AutoCloner.cloneToplevel(this.catchClauses, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

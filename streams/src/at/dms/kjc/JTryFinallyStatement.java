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
 * $Id: JTryFinallyStatement.java,v 1.6 2003-08-29 19:25:37 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
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
public class JTryFinallyStatement extends JStatement {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JTryFinallyStatement() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where			the line of this node in the source code
   * @param	tryClause		the body
   * @param	finallyClause		the finally clause
   * @param	comments		comments in the source text
   */
  public JTryFinallyStatement(TokenReference where,
			      JBlock tryClause,
			      JBlock finallyClause,
			      JavaStyleComment[] comments)
  {
    super(where, comments);

    this.tryClause = tryClause;
    this.finallyClause = finallyClause;
    this.finallyLabel = new CodeLabel();
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
    CBlockContext	self = new CBlockContext(context);

    /*
     * Create synthetic variables for try-finally administration.
     */
    exceptionVar = addSyntheticVariable(self,
					"try_finally_excp$",
					CStdType.Integer);
    addressVar = addSyntheticVariable(self,
				      "try_finally_addr$",
				      CStdType.Integer);
    returnVar = addSyntheticVariable(self,
				     "try_finally_rtvl$",
				     context.getMethodContext().getCMethod().getReturnType());


    CTryFinallyContext		tryContext;
    CBodyContext	finallyContext;

    tryContext = new CTryFinallyContext(self);
    tryClause.analyse(tryContext);

    finallyContext = new CSimpleBodyContext(context, self);
    finallyContext.adoptVariableInfos(tryContext);
    finallyContext.adoptFieldInfos(tryContext);
    finallyContext.setReachable(true);
    finallyClause.analyse(finallyContext);

    if (!finallyContext.isReachable()) {
      self.setReachable(false);
      self.adoptThrowables(finallyContext);
    } else {
      tryContext.forwardBreaksAndContinues();
      self.setReachable(tryContext.isReachable());
      self.mergeThrowables(tryContext);
      self.mergeThrowables(finallyContext);
      self.adoptVariableInfos(finallyContext);
      self.adoptFieldInfos(finallyContext);
    }

    context.adoptVariableInfos(self);
    context.adoptFieldInfos(self);
    context.setReachable(self.isReachable());
  }

  /**
   * Adds a synthetic local variable.
   */
  private JLocalVariable addSyntheticVariable(CBodyContext context,
					      String prefix,
					      CType type)
    throws PositionedError
  {
    if (type == CStdType.Void) {
      // No need to create a variable, i.e. to hold a return value during
      // execution of the finally clause if the return type of the
      // enclosing method is void.
      return null;
    } else {
      JLocalVariable	var;

      var = new JGeneratedLocalVariable(null,
					0,
					type,
					prefix + toString() /* unique ID */,
					null);
      try {
	context.getBlockContext().addVariable(var);
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }
      return var;
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
    p.visitTryFinallyStatement(this, tryClause, finallyClause);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitTryFinallyStatement(this, tryClause, finallyClause);
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    CodeLabel		nextLabel = new CodeLabel();

    int		startPC = code.getPC();
    code.pushContext(this);
    tryClause.genCode(code);			//		TRY CODE
    code.popContext(this);
    int		endPC = code.getPC();
    code.plantJumpInstruction(opc_jsr, finallyLabel);	//		JSR finally
    code.plantJumpInstruction(opc_goto, nextLabel);	//		GOTO next

    // HANDLER FOR OTHER EXCEPTIONS
    int	finallyPC = code.getPC();
    code.plantLocalVar(opc_astore, exceptionVar);
    code.plantJumpInstruction(opc_jsr, finallyLabel);	//		JSR finally
    code.plantLocalVar(opc_aload, exceptionVar);
    code.plantNoArgInstruction(opc_athrow);

    // FINALLY CODE
    code.plantLabel(finallyLabel);		//	finally:...
    code.plantLocalVar(opc_astore, addressVar);
    finallyClause.genCode(code);			//		FINALLY CLAUSE

    code.plantLocalVar(opc_ret, addressVar);
    code.addExceptionHandler(startPC, endPC, finallyPC, null);
    code.plantLabel(nextLabel);			//	next:	...

    finallyLabel = null;
  }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genFinallyCall(CodeSequence code, JReturnStatement ret) {
    if (ret != null && ret.getType().getSize() > 0) {
      ret.store(code, returnVar);
      code.plantJumpInstruction(Constants.opc_jsr, finallyLabel);
      ret.load(code, returnVar);
    } else {
      code.plantJumpInstruction(Constants.opc_jsr, finallyLabel);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CodeLabel		finallyLabel;
  private JBlock		tryClause;
  private JBlock		finallyClause;

  /**
   * A synthetic local variable to store an exception that might be raised
   * by the execution try clause; after execution of the finally clause
   * the exception must be rethrown.
   */
  private JLocalVariable	exceptionVar;

  /**
   * A synthetic local variable to hold the return address for
   * the jsr to the finally clause.
   */
  private JLocalVariable	addressVar;

  /**
   * A synthetic local variable to hold the return value during the execution
   * of the finally clause if a return statement is enclosed in the try clause.
   */
  private JLocalVariable	returnVar;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JTryFinallyStatement other = new at.dms.kjc.JTryFinallyStatement();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JTryFinallyStatement other) {
  super.deepCloneInto(other);
  other.finallyLabel = (at.dms.kjc.CodeLabel)at.dms.kjc.AutoCloner.cloneToplevel(this.finallyLabel, other);
  other.tryClause = (at.dms.kjc.JBlock)at.dms.kjc.AutoCloner.cloneToplevel(this.tryClause, other);
  other.finallyClause = (at.dms.kjc.JBlock)at.dms.kjc.AutoCloner.cloneToplevel(this.finallyClause, other);
  other.exceptionVar = (at.dms.kjc.JLocalVariable)at.dms.kjc.AutoCloner.cloneToplevel(this.exceptionVar, other);
  other.addressVar = (at.dms.kjc.JLocalVariable)at.dms.kjc.AutoCloner.cloneToplevel(this.addressVar, other);
  other.returnVar = (at.dms.kjc.JLocalVariable)at.dms.kjc.AutoCloner.cloneToplevel(this.returnVar, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

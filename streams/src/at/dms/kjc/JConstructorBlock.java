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
 * $Id: JConstructorBlock.java,v 1.3 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class represents the body of a constructor.
 */
public class JConstructorBlock extends JBlock {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    private JConstructorBlock() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	constructorCall	an explicit constructor invocation
   * @param	body		the statements contained in the block
   */
  public JConstructorBlock(TokenReference where,
			   JConstructorCall constructorCall,
			   JStatement[] body)
  {
    super(where, body, null);
    this.constructorCall = constructorCall;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the constructor called by this constructor.
   */
  public CMethod getCalledConstructor() {
    return constructorCall == null ? null : constructorCall.getMethod();
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the constructor body (semantically).
   *
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CBodyContext context) throws PositionedError {
    sourceClass = (CSourceClass)context.getClassContext().getCClass();

    // JLS 8.8.5 :
    // If a constructor body does not begin with an explicit constructor
    // invocation and the constructor being declared is not part of the
    // primordial class Object, then the constructor body is implicitly
    // assumed by the compiler to begin with a superclass constructor
    // invocation "super();", an invocation of the constructor of its
    // direct superclass that takes no arguments.
    if (constructorCall == null && !sourceClass.getType().equals(CStdType.Object)) {
      constructorCall = new JConstructorCall(getTokenReference(),
					     false,
					     JExpression.EMPTY);
    }

    if (sourceClass.isNested()) {
      paramsLength = context.getMethodContext().getCMethod().getParameters().length;
    }

    // Insert a call to the instance initializer, iff :
    // - there exists an instance initializer
    // - there is no explicit invocation of a constructor of this class
    if (! context.getClassContext().hasInitializer()
	|| constructorCall == null
	|| constructorCall.isThisInvoke()) {
      initializerCall = null;
    } else {
      // "Block$();"
      initializerCall =
	new JExpressionStatement(getTokenReference(),
				 new JMethodCallExpression(getTokenReference(),
							   null,
							   JAV_INIT,
							   JExpression.EMPTY),
				 null);
    }

    if (constructorCall != null) {
      constructorCall.analyse(new CExpressionContext(context));
      if (constructorCall.isThisInvoke()) {
	((CConstructorContext)context.getMethodContext()).markAllFieldToInitialized();
      }
    }

    if (initializerCall != null) {
      initializerCall.analyse(context);
      ((CConstructorContext)context.getMethodContext()).adoptInitializerInfo();
    }

    super.analyse(context);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

   /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    setLineNumber(code);

    if (constructorCall != null) {
      constructorCall.genCode(code, true);
    }

    if (sourceClass.isNested()) {
      sourceClass.genInit(code, paramsLength);
    }

    if (initializerCall != null) {
      initializerCall.genCode(code);
    }

    for (int i = 0; i < body.size(); i++) {
      ((JStatement)body.get(i)).genCode(code);
    }

    //!!! graf 010529 : needed ?
    code.plantNoArgInstruction(opc_return);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JConstructorCall		constructorCall;
  private JStatement			initializerCall;
  private CSourceClass			sourceClass;
  private int				paramsLength;
}

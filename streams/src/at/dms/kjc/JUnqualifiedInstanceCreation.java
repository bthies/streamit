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
 * $Id: JUnqualifiedInstanceCreation.java,v 1.5 2003-05-28 05:58:44 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * JLS 15.9 Class Instance Creation Expressions.
 *
 * This class represents an unqualified class instance creation expression.
 */
public class JUnqualifiedInstanceCreation extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JUnqualifiedInstanceCreation() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	type		the type of the object to be created
   * @param	params		parameters to be passed to constructor
   */
  public JUnqualifiedInstanceCreation(TokenReference where,
				      CClassType type,
				      JExpression[] params)
  {
    super(where);

    this.type = type;
    this.params = params;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
    return type;
  }

  /**
   * Returns true iff this expression can be used as a statement (JLS 14.8)
   */
  public boolean isStatementExpression() {
    return true;
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the expression (semantically).
   * @param	context		the analysis context
   * @return	an equivalent, analysed expression
   * @exception	PositionedError	the analysis detected an error
   */
  public JExpression analyse(CExpressionContext context) throws PositionedError {
    local = context.getClassContext().getCClass();

    // JLS 15.9.1 Determining the Class being Instantiated

    // If the class instance creation expression is an unqualified class
    // instance creation expression, then the ClassOrInterfaceType must name
    // a class that is accessible and not abstract, or a compile-time error
    // occurs. In this case, the class being instantiated is the class
    // denoted by ClassOrInterfaceType.
    try {
      type.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
    check(context, !type.getCClass().isAbstract(), KjcMessages.NEW_ABSTRACT, type);
    check(context, !type.getCClass().isInterface(), KjcMessages.NEW_INTERFACE, type);
    check(context, 
          type.getCClass().isAccessible(local),
	  KjcMessages.CLASS_NOACCESS, 
          type.getCClass());

    /////////////////////////////////////////////////////////////////////////

    CType[] argsType = new CType[params.length];

    for (int i = 0; i < argsType.length; i++) {
      params[i] = params[i].analyse(context);
      argsType[i] = params[i].getType();
      assert(argsType[i] != null);
    }

    //!!! review and create test cases
    context = new CExpressionContext(context);

    try {
      constructor = type.getCClass().lookupMethod(context.getClassContext().getCClass(), JAV_CONSTRUCTOR, argsType);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }

    if (constructor == null || constructor.getOwner() != type.getCClass()) {
      // do not want a super constructor !
      throw new CMethodNotFoundError(getTokenReference(), null, type.toString(), argsType);
    }

    // check access
    check(context, constructor.isAccessible(local), KjcMessages.CONSTRUCTOR_NOACCESS, type);

    if (constructor.getOwner().isNested()) {
      check(context, !constructor.getOwner().hasOuterThis() ||
	    ((!context.getMethodContext().getCMethod().isStatic()) &&
	     (local.descendsFrom(constructor.getOwner().getOwner()) 
              || (local.getOwner() != null && local.getOwner() == constructor.getOwner().getOwner()))),
	    KjcMessages.INNER_INHERITENCE, constructor.getOwner().getType(), local.getType());
    }

    CClassType[]	exceptions = constructor.getThrowables();
    for (int i = 0; i < exceptions.length; i++) {
      context.getBodyContext().addThrowable(new CThrowableInfo(exceptions[i], this));
    }

    argsType = constructor.getParameters();

    for (int i = 0; i < params.length; i++) {
      params[i] = params[i].convertType(argsType[i], context);
    }

    return this;
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitUnqualifiedInstanceCreation(this, type, params);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitUnqualifiedInstanceCreation(this, type, params);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    code.plantClassRefInstruction(opc_new, type.getCClass().getQualifiedName());

    if (!discardValue) {
      code.plantNoArgInstruction(opc_dup);
    }

    if (constructor.getOwner().isNested() 
        && !constructor.getOwner().isStatic()
        && constructor.getOwner().hasOuterThis()) {
      // inner class
      if (!(local.getOwner() != null && local.getOwner() == constructor.getOwner().getOwner())) {
        code.plantLoadThis();
      } else {
        // create inner class in inner class
      code.plantLoadThis();
      code.plantFieldRefInstruction(opc_getfield,
				    local.getType().getSignature().substring(1, local.getType().getSignature().length() - 1),
				    JAV_OUTER_THIS,
				    local.getOwner().getType().getSignature());
      }
       //      constructor.getOwner().genSyntheticParams(code, false);
    }

//     if (constructor.getOwner().isNested()
// 	&& !constructor.getOwner().isStatic()
// 	&& constructor.getOwner().hasOuterThis()
// 	&& local.getOwner() != null
// 	&& local.getOwner() == constructor.getOwner().getOwner()) {
//       // SUPER SYNTHETIC PARAMETER
//     }

    for (int i = 0; i < params.length; i++) {
      params[i].genCode(code, false);
    }

    constructor.getOwner().genOuterSyntheticParams(code);

    constructor.genCode(code, true);
  }

    

    public JExpression[] getParams() {
	return params;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression[]		params;
  private CClassType		type;
  private CClass		local;
  private CMethod		constructor;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JUnqualifiedInstanceCreation other = new at.dms.kjc.JUnqualifiedInstanceCreation();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JUnqualifiedInstanceCreation other) {
  super.deepCloneInto(other);
  other.params = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.params);
  other.type = (at.dms.kjc.CClassType)at.dms.kjc.AutoCloner.cloneToplevel(this.type);
  other.local = (at.dms.kjc.CClass)at.dms.kjc.AutoCloner.cloneToplevel(this.local);
  other.constructor = (at.dms.kjc.CMethod)at.dms.kjc.AutoCloner.cloneToplevel(this.constructor);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JQualifiedInstanceCreation.java,v 1.8 2003-11-13 10:46:11 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * JLS 15.9 Class Instance Creation Expressions.
 *
 * This class represents an qualified class instance creation expression.
 */
public class JQualifiedInstanceCreation extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JQualifiedInstanceCreation() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	prefix		the prefix denoting the object to search
   * @param	ident		the simple name of the class
   * @param	params		parameters to be passed to constructor
   */
  public JQualifiedInstanceCreation(TokenReference where,
				    JExpression prefix,
				    String ident,
				    JExpression[] params)
  {
    super(where);

    this.prefix = prefix;
    this.ident = ident;
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
    context = new CExpressionContext(context);

    prefix = prefix.analyse(context);
    check(context,
	  prefix.getType().isClassType(),
	  KjcMessages.FIELD_BADACCESS, prefix.getType());

    CClass	newClass;
    CType       prefixType = prefix.getType();

    try {
      newClass = prefixType.getCClass().lookupClass(context.getClassContext().getCClass(), ident);
    } catch (UnpositionedError e){
      throw e.addPosition(getTokenReference());
    }

    check(context,
	  newClass != null,
	  KjcMessages.TYPE_UNKNOWN, prefix.getType() + "." + ident);
    type = newClass.getType();

    CType[] argsType;

    try {
      type.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }

    argsType = new CType[params.length];

    for (int i = 0; i < argsType.length; i++) {
      params[i] = params[i].analyse(context);
      argsType[i] = params[i].getType();
      assert(argsType[i] != null);
    }

    //!!! review and create test cases
    check(context, !type.getCClass().isAbstract(), KjcMessages.NEW_ABSTRACT, type);
    check(context, !type.getCClass().isInterface(), KjcMessages.NEW_INTERFACE, type);

    try {
      constructor = type.getCClass().lookupMethod(context.getClassContext().getCClass(), 
                                                  JAV_CONSTRUCTOR, 
                                                  argsType);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }
    if (constructor == null || constructor.getOwner() != type.getCClass()) {
      // do not want a super constructor !
      throw new CMethodNotFoundError(getTokenReference(), null, type.toString(), argsType);
    }

    // check access
    local = context.getClassContext().getCClass();
    /* JLS 15.9.1 It is a compile-time error if Identifier is 
       not the simple name (§6.2) of an accessible (§6.6) non-abstract 
       inner class (§8.1.2) */
    check(context, constructor.isAccessible(local), KjcMessages.CONSTRUCTOR_NOACCESS, type);
    check(context,
          constructor.getOwner().isNested() 
          && !constructor.getOwner().isStatic() 
          && constructor.getOwner().hasOuterThis(), 
          KjcMessages.NOT_INNER_CLASS, 
          type);

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
    p.visitQualifiedInstanceCreation(this, prefix, ident, params);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitQualifiedInstanceCreation(this, prefix, ident, params);
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
 
    prefix.genCode(code, false);  // Qualified !!!!!
    /* JLS 15.9.4 First, if the class instance creation expression is 
       a qualified class instance creation expression, the qualifying 
       primary expression is evaluated. If the qualifying expression 
       evaluates to null, a NullPointerException is raised  */    
    CodeLabel         ok = new CodeLabel();

    code.plantNoArgInstruction(opc_dup);
    code.plantJumpInstruction(opc_ifnonnull, ok);
    code.plantNoArgInstruction(opc_aconst_null);
    code.plantNoArgInstruction(opc_athrow);
    code.plantLabel(ok);

    for (int i = 0; i < params.length; i++) {
      params[i].genCode(code, false);
    }
    constructor.getOwner().genOuterSyntheticParams(code);

    constructor.genCode(code, true);
  }

    /**
     * Sets the prefix of this.
     */
    public void setPrefix(JExpression prefix) {
	this.prefix = prefix;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression		prefix;
  private String		ident;
  private JExpression[]		params;
  private CClassType		type;
  private CClass		local;
  private CMethod		constructor;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JQualifiedInstanceCreation other = new at.dms.kjc.JQualifiedInstanceCreation();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JQualifiedInstanceCreation other) {
  super.deepCloneInto(other);
  other.prefix = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.prefix);
  other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident);
  other.params = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.params);
  other.type = (at.dms.kjc.CClassType)at.dms.kjc.AutoCloner.cloneToplevel(this.type);
  other.local = (at.dms.kjc.CClass)at.dms.kjc.AutoCloner.cloneToplevel(this.local);
  other.constructor = (at.dms.kjc.CMethod)at.dms.kjc.AutoCloner.cloneToplevel(this.constructor);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

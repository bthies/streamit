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
 * $Id: JUnqualifiedAnonymousCreation.java,v 1.10 2004-01-28 16:55:35 dmaze Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * !!! This class represents a new allocation expression 'new Toto(1)'
 */
public class JUnqualifiedAnonymousCreation extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JUnqualifiedAnonymousCreation() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	objectType	the type of this object allocator
   * @param	params		parameters to be passed to constructor
   */
  public JUnqualifiedAnonymousCreation(TokenReference where,
				      CClassType objectType,
				      JExpression[] params,
				      JClassDeclaration decl)
  {
    super(where);

    this.type = objectType;
    this.params = params;
    this.decl = decl;
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
   * Returns the parameter list for the object.
   * @return the parameter list
   */
  public JExpression[] getParams() {
    return params;
  }
    
  /**
   * Returns the class declaration for the object.
   * @return the class declaration
   */
  public JClassDeclaration getDecl() {
    return decl;
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
    CType[]	argsType;
    CClass	owner = context.getClassContext().getCClass();
    CClassType	superClass;

    try {
      type.checkType(context);
    } catch (UnpositionedError e) {
      throw e.addPosition(getTokenReference());
    }

    argsType = new CType[params.length];

    for (int i = 0; i < argsType.length; i++) {
      params[i] = params[i].analyse(context);
      argsType[i] = params[i].getType();
      assert argsType[i] != null;
    }


    /* JLS 15.9.2: If the class instance creation expression occurs 
       in a static context (§8.1.2), then i has no immediately enclosing 
       instance. Otherwise, the immediately enclosing instance of i is this. */
    decl.generateInterface(owner,
			   owner.getQualifiedName() + "$" + context.getClassContext().getNextSyntheticIndex() + "$");

    CClass      superCClass = type.getCClass();

    if (type.getCClass().isInterface()) {
      superClass = CStdType.Object;
      decl.setInterfaces(new CClassType[] { type });
    } else {
      superClass = type;
    }
    decl.setSuperClass(superClass);

    // The class of the super class must be set explicitly
    // before lookup of the constructor of the superclass
    // because it will be set in decl only in checkInterface.
    // On the other hand checkInterface needs the constructor
    // to be created.
    // graf 010422 :
    // But, why not analyse the constructor later ? Perhaps
    // to be able to signal an error ?

    decl.getCClass().setSuperClass(superClass);

    // add implicit constructor
    JConstructorDeclaration	cstr;
    CMethod			superCstr;

    try {
      superCstr = superClass.getCClass().lookupMethod(decl.getCClass(), JAV_CONSTRUCTOR, argsType);
    } catch (UnpositionedError cue) {
      throw cue.addPosition(getTokenReference());
    }
    if (superCstr == null) {
      throw new CMethodNotFoundError(getTokenReference(), null, superClass.toString(), argsType);
    }

    CType[]		parameters = superCstr.getParameters();
    JFormalParameter[]	fparams = new JFormalParameter[parameters.length];
    CClassType[]	throwables = superCstr.getThrowables();

    for (int i = 0; i < parameters.length; i++) {
      fparams[i] = new JFormalParameter(getTokenReference(),
					JLocalVariable.DES_GENERATED,
					parameters[i],
					"dummy" + i,
					true);
    }

    JExpression[]		checkedParams = new JExpression[params.length];

    for (int i = 0; i < params.length; i++) {
      checkedParams[i] = new JLocalVariableExpression(getTokenReference(),
						      fparams[i]);
    }

    cstr = new JConstructorDeclaration(getTokenReference(),
				       ACC_PUBLIC,
				       decl.getCClass().getIdent(),
				       fparams,
				       throwables,
				       new JConstructorCall(getTokenReference(), false, checkedParams),
				       new JStatement[0],
				       null,
				       null);
    decl.setDefaultConstructor(cstr);
    decl.checkInterface(context.getCompilationUnitContext());
    decl.checkInitializers(context);
    decl.checkTypeBody(context);

    type = decl.getCClass().getType();

    //!!! review and create test cases
    context = new CExpressionContext(context);

    check(context, !type.getCClass().isAbstract(), KjcMessages.NEW_ABSTRACT, type);
    check(context, !type.getCClass().isInterface(), KjcMessages.NEW_INTERFACE, type);

    constructor = cstr.getMethod();

    // check access
    local = context.getClassContext().getCClass();
    check(context, constructor.isAccessible(local), KjcMessages.CONSTRUCTOR_NOACCESS, type);

    if (constructor.getOwner().isNested()) {
      check(context, !constructor.getOwner().hasOuterThis() ||
	    ((!context.getMethodContext().getCMethod().isStatic()) &&
	     (local.descendsFrom(constructor.getOwner().getOwner()) || (local.getOwner() != null && local.getOwner() == constructor.getOwner().getOwner()))),
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
    p.visitUnqualifiedAnonymousCreation(this, type, params, decl);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitUnqualifiedAnonymousCreation(this, type, params, decl);
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


    if (!constructor.getOwner().isStatic() 
        && constructor.getOwner().hasOuterThis()) {
      // inner class


      if (!(local.getOwner() != null 
            && constructor.getOwner().getOwner() != null
            && local.getOwner() == constructor.getOwner().getOwner().getOwner())) {
	code.plantLoadThis();
      } else {
        // create inner class in inner class
        //!!! FIXME !!!! To be fixed with get/set to private inner fields 
	code.plantLoadThis();
        /*	code.plantFieldRefInstruction(opc_getfield,
				      local.getType().getSignature().substring(1, local.getType().getSignature().length() - 1),
				      JAV_OUTER_THIS,
				      local.getOwner().getType().getSignature());*/
      }
    }

    for (int i = 0; i < params.length; i++) {
      params[i].genCode(code, false);
    }
    //    if (constructor.getOwner().isNested()) {
      constructor.getOwner().genOuterSyntheticParams(code);
      //    }
    constructor.genCode(code, true);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private JExpression[]		params;
  private CClassType		type;
  private CClass		local;
  private CMethod		constructor;
  private JClassDeclaration	decl;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JUnqualifiedAnonymousCreation other = new at.dms.kjc.JUnqualifiedAnonymousCreation();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JUnqualifiedAnonymousCreation other) {
  super.deepCloneInto(other);
  other.params = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.params);
  other.type = (at.dms.kjc.CClassType)at.dms.kjc.AutoCloner.cloneToplevel(this.type);
  other.local = (at.dms.kjc.CClass)at.dms.kjc.AutoCloner.cloneToplevel(this.local);
  other.constructor = (at.dms.kjc.CMethod)at.dms.kjc.AutoCloner.cloneToplevel(this.constructor);
  other.decl = (at.dms.kjc.JClassDeclaration)at.dms.kjc.AutoCloner.cloneToplevel(this.decl);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

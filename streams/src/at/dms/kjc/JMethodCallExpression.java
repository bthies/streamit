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
 * $Id: JMethodCallExpression.java,v 1.14 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * JLS 15.12 Method Invocation Expressions
 */
public class JMethodCallExpression extends JExpression {

    protected JMethodCallExpression() {} // for cloner only

  /**
   *
   * Construct a node with default prefix of This.
   */
  public JMethodCallExpression(TokenReference where,
			       String ident,
			       JExpression[] args)
  {
      this(where, new JThisExpression(null), ident, args);
  }

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	prefix		an expression that is a field of a class representing a method
   * @param	ident		the method identifier
   * @param	args		the argument of the call
   */
  public JMethodCallExpression(TokenReference where,
			       JExpression prefix,
			       String ident,
			       JExpression[] args)
  {
    super(where);

    this.prefix = prefix;
    this.ident = ident.intern();	// $$$ graf 010530 : why intern ?
    this.args = args;
    this.tapeType = null;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * @return the type of this expression
   */
  public String getIdent() {
    return ident;
  }

  /**
   * @return the type of this expression
   */
  public CType getType() {
      if(method!=null)
	  return method.getReturnType();
      //ADDED BY GORDO, I need a way to 
      //keep the type of a method call where
      //the callee method is not defined
      //this should not affect anything...
      //famous last words
      return tapeType;
      //      return null;
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
    CType[]	argTypes = new CType[args.length];

    for (int i = 0; i < argTypes.length; i++) {
      // evaluate the arguments in rhs mode, result will be used
      args[i] = args[i].analyse(new CExpressionContext(context));
      argTypes[i] = args[i].getType();
    }

    CClass		local = context.getClassContext().getCClass();

    if (prefix != null) {
      // evaluate the prefix in rhs mode, result will be used
      prefix = prefix.analyse(new CExpressionContext(context));
      if (prefix instanceof JNameExpression) {
	// condition as if-statement because of arguments to method check
	check(context,
	      false,
	      KjcMessages.BAD_METHOD_NAME, ((JNameExpression)prefix).getName());
      }
      check(context,
	    prefix.getType().isReference(),
	    KjcMessages.METHOD_BADPREFIX, ident, prefix.getType());
      check(context,
	    prefix.getType().getCClass().isAccessible(local),
	    KjcMessages.CLASS_NOACCESS, prefix.getType().getCClass());
	    
      try {
	method = prefix.getType().getCClass().lookupMethod(local, ident, argTypes);
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }
    } else {
      try {
	method = context.lookupMethod(local, ident, argTypes);
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }
    }

    if (method == null) {
      String	prefixName;

      if (prefix instanceof JNameExpression) {
	prefixName = ((JNameExpression)prefix).getQualifiedName() + ".";
      } else if (prefix instanceof JTypeNameExpression) {
	prefixName = ((JTypeNameExpression)prefix).getQualifiedName() + ".";
      } else {
	prefixName = "";
      }

      throw new CMethodNotFoundError(getTokenReference(), this, prefixName + ident, argTypes);
    }

    CClassType[]	exceptions = method.getThrowables();
    for (int i = 0; i < exceptions.length; i++) {
      if (exceptions[i].isCheckedException()) {
	if (prefix == null ||				// special case of clone
	    !prefix.getType().isArrayType() ||
	    ident != Constants.JAV_CLONE ||
	    !exceptions[i].getCClass().getQualifiedName().equals("java/lang/CloneNotSupportedException")) {
	  context.getBodyContext().addThrowable(new CThrowableInfo(exceptions[i], this));
	}
      }
    }

    CClass		access = method.getOwner();

    if (prefix == null && !method.isStatic()) {
      if (access == local) {
	prefix = new JThisExpression(getTokenReference());
      } else {
	prefix = new JThisExpression(getTokenReference(), access);
      }
      prefix = prefix.analyse(context);
    }

    check(context,
	  method.isStatic() || !(prefix instanceof JTypeNameExpression),
	  KjcMessages.METHOD_STATIC_BAD, method.getIdent());

    if (method.isStatic() && prefix != null && !(prefix instanceof JTypeNameExpression)) {
      context.reportTrouble(new CWarning(getTokenReference(),
					 KjcMessages.INSTANCE_PREFIXES_STATIC_METHOD,
					 method.getIdent(),
					 prefix.getType()));
    }

    argTypes = method.getParameters();
    for (int i = 0; i < argTypes.length; i++) {
      if (args[i] instanceof JTypeNameExpression) {
	check(context, false, KjcMessages.VAR_UNKNOWN, ((JTypeNameExpression)args[i]).getQualifiedName());
      }
      args[i] = args[i].convertType(argTypes[i], context);
    }

    // Mark method as used if it is a source method
    if (method instanceof CSourceMethod) {
      ((CSourceMethod)method).setUsed();
    }

    if (method.getReturnType() != CStdType.Void && context.discardValue()) {
      context.reportTrouble(new CWarning(getTokenReference(),
					 KjcMessages.UNUSED_RETURN_VALUE_FROM_FUNCTION_CALL,
					 method.getIdent()));


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
    p.visitMethodCallExpression(this, prefix, ident, args);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitMethodCallExpression(this, prefix, ident, args);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    setLineNumber(code);

    boolean		forceNonVirtual = false;

    if (! method.isStatic()) {
      prefix.genCode(code, false);
      if (prefix instanceof JSuperExpression) {
	forceNonVirtual = true;
      }
    } else if (prefix != null) {
      prefix.genCode(code, true);
    }

    for (int i = 0; i < args.length; i++) {
      args[i].genCode(code, false);
    }

    method.genCode(code, forceNonVirtual);

    if (discardValue) {
      code.plantPopInstruction(getType());
    }
  }

    public CMethod getMethod() {
	return method;
    }

    /**
     * Adds <arg> as the first arg of this.
     */
    public void addArgFirst(JExpression arg) {
	JExpression newArgs[] = new JExpression[args.length + 1];
	newArgs[0] = arg;
	for (int i=0; i<args.length; i++) {
	    newArgs[i+1] = args[i];
	}
	this.args = newArgs;
    }

    public JExpression[] getArgs() {
	return args;
    }

    public JExpression getPrefix() {
	return prefix;
    }

    public void setArgs(JExpression[] a) {
	this.args = a;
    }

    public void setPrefix(JExpression p) {
	prefix = p;
    }

    public void setIdent(String ident) {
	this.ident = ident;
    }

    public void setTapeType(CType type) {
	this.tapeType = type;
    }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  protected JExpression		prefix;
  protected String	ident;
  protected JExpression[]	args;

  protected CMethod		method;
    //added by gordon
    //I needed a way to store the type of a methodcall expression
    //where the callee is not defined
    protected CType tapeType;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JMethodCallExpression other = new at.dms.kjc.JMethodCallExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JMethodCallExpression other) {
  super.deepCloneInto(other);
  other.prefix = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.prefix, this);
  other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident, this);
  other.args = (at.dms.kjc.JExpression[])at.dms.kjc.AutoCloner.cloneToplevel(this.args, this);
  other.method = (at.dms.kjc.CMethod)at.dms.kjc.AutoCloner.cloneToplevel(this.method, this);
  other.tapeType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.tapeType, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

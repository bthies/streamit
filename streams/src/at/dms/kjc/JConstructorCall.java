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
 * $Id: JConstructorCall.java,v 1.4 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class represents a explicit call to a super or self constructor
 */
public class JConstructorCall extends JExpression {

    protected JConstructorCall() {} // for cloner only

    /**
     * Construct a node in the parsing tree
     * This method is directly called by the parser
     * @param	where		the line of this node in the source code
     * @param	functorIsThis	true if functor is "this" (else "super")
     * @param	arguments	the argument of the call
     */
    public JConstructorCall(TokenReference where,
			    boolean functorIsThis,
			    JExpression[] arguments)
    {
	super(where);

	this.functorIsThis = functorIsThis;
	this.arguments = arguments;
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Returns the called method.
     */
    public CMethod getMethod() {
	return method;
    }

    /**
     * Returns true if it's this() else it's super().
     */
    final boolean isThisInvoke() {
	return functorIsThis;
    }

    /**
     * !!!
     */
    public CType getType() {
	return null;
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
	((CConstructorContext)context.getMethodContext()).setSuperConstructorCalled(false);

	// !!! check in constructor !!!

	CType[]	argsType = new CType[arguments.length];
	for (int i = 0; i < argsType.length; i++) {
	    arguments[i] = arguments[i].analyse(context);

	    argsType[i] = arguments[i].getType();
	    assert(argsType[i] != null);
	}
	inClass = context.getClassContext().getCClass();

	if (functorIsThis) {
	    clazz = context.getClassContext().getCClass();
	} else {
	    clazz = context.getClassContext().getCClass().getSuperClass();
	}

	assert(clazz != null);
	try {
	    method = clazz.lookupMethod(context.getClassContext().getCClass(), JAV_CONSTRUCTOR, argsType);
	} catch (UnpositionedError e) {
	    throw e.addPosition(getTokenReference());
	}

	// changed for streamit: only give warning if can't find
	// superclass constructor, since this probably represents a case
	// where a Pipeline, etc. constructor doesn't exist (but won't be called)
	if (method==null) {
	    System.err.print("WARNING:  Referring to constructor that doesn't exist in Java library:\n" +
			     "    " + clazz.getIdent() + "(");
	    for (int i=0; i<argsType.length; i++) {
		System.err.print(argsType[i]);
		if (i<argsType.length-1) {
		    System.err.print(", ");
		}
	    }
	    System.err.println(");");
	    System.err.println("  This program will compile fine, but it will not run in the Java library\n" +
			       "  without adding support for the above init function signature.  To extend\n" + 
			       "  the library to support this signature, follow the instructions in:\n" +
			       "    streams/docs/implementation-notes/library-init-functions.txt\n");
	} else {
	    //check(context, method != null, KjcMessages.CONSTRUCTOR_NOTFOUND, clazz.getIdent());

	    if (method.getOwner() != clazz) {
		// May be an inner class
		if (clazz.isNested()) {
		    CType[]		argsType2 = new CType[argsType.length + 1];
		    System.arraycopy(argsType, 0, argsType2, 0, argsType.length);
		    argsType2[argsType.length] = clazz.getOwner().getType();
		    try {
			method = clazz.lookupMethod(context.getClassContext().getCClass(), JAV_CONSTRUCTOR, argsType2);
		    } catch (UnpositionedError e) {
			throw e.addPosition(getTokenReference());
		    }
		}
		if (method.getOwner() != clazz) {
		    // do not want a super constructor !
		    throw new CMethodNotFoundError(getTokenReference(), null, clazz.getType().toString(), argsType);
		}
	    }

	    CClassType[]	exceptions = method.getThrowables();
	    for (int i = 0; i < exceptions.length; i++) {
		if (exceptions[i].isCheckedException()) {
		    context.getBodyContext().addThrowable(new CThrowableInfo(exceptions[i], this));
		}
	    }

	    check(context, !context.getMethodContext().getCMethod().isStatic(), KjcMessages.BAD_THIS_STATIC);

	    argsType = method.getParameters();
	    for (int i = 0; i < arguments.length; i++) {
		arguments[i] = arguments[i].convertType(argsType[i], context);
	    }
	    ((CConstructorContext)context.getMethodContext()).setSuperConstructorCalled(true);
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
	p.visitConstructorCall(this, functorIsThis, arguments);
    }

    /**
     * Accepts the specified attribute visitor
     * @param	p		the visitor
     */
    public Object accept(AttributeVisitor p) {
	return    p.visitConstructorCall(this, functorIsThis, arguments);
    }

    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param	code		the bytecode sequence
     * @param	discardValue	discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
	setLineNumber(code);
	code.plantLoadThis();

	CClass      owner = method.getOwner();

	if (owner.isNested() && owner.hasOuterThis()) {
	    clazz.genSyntheticParamsFromExplicitSuper(inClass.isQualifiedAndAnonymous(), code);
	}
	for (int i = 0; i < arguments.length; i++) {
	    arguments[i].genCode(code, false);
	}

	method.genCode(code, true);

	// The return type is void : there is no result value.
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private boolean		functorIsThis;
    private JExpression[]		arguments;

    private CClass		clazz;
    private CClass		inClass;
    private CMethod		method;
}

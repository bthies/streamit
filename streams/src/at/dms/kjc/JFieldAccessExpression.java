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
 * $Id: JFieldAccessExpression.java,v 1.9 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * JLS 15.11 Field Access Expression.
 *
 * A field access expression may access a field of an object or array.
 */
public class JFieldAccessExpression extends JExpression {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    protected JFieldAccessExpression() {} // for cloner only

    /**
     * You usually don't know the cfield--this is mostly for
     * constructing new accesses from old accesses within the ir.
     * note that cfield goes missing otherwise.
     */
    public JFieldAccessExpression(TokenReference where,
				  JExpression prefix,
				  String ident,
				  CField field) {
	super(where);

	this.prefix = prefix;
	this.ident = ident;
	this.constantPrefix = false;
	this.field = field;
    }

    /**
     * Construct a node in the parsing tree
     *
     * @param	where		the line of this node in the source code
     * @param	prefix		the prefix denoting the object to search
     * @param	ident		the simple name of the field
     */
    public JFieldAccessExpression(TokenReference where,
				  JExpression prefix,
				  String ident)
    {
	this(where, prefix, ident, null);
    }


    /**
     * Construct a node in the parsing tree
     * @param	where		the line of this node in the source code
     * @param	ident		the simple name of the field
     */
    public JFieldAccessExpression(TokenReference where, String ident) {
	this(where, null, ident);
    }

    // ----------------------------------------------------------------------
    // ACCESSORS
    // ----------------------------------------------------------------------

    /**
     * Returns the simple name of the field.
     */
    public String getIdent() {
	return ident;
    }

    /**
     * Returns the type of the expression.
     */
    public CType getType() {
	if(field==null)
	    return null;
	return field.getType();
    }

    /**
     * Returns the left-hand-side of the expression.
     */
    public JExpression getPrefix() {
        return prefix;
    }

    /**
     * Tests whether this expression denotes a compile-time constant (JLS 15.28).
     *
     * @return	true iff this expression is constant
     */
    public boolean isConstant() {
	// A compile-time constant expression is an expression [...]
	// that is composed using only the following :
	// - Simple names that refer to final variables whose initializers
	//   are constant expressions
	// - Qualified names of the form TypeName . Identifier that refer to
	//   final variables whose initializers are constant expressions

	return constantPrefix
	    && field.isFinal()
	    && field.getValue() != null
	    && field.getValue().isConstant();
    }

    /**
     * Returns true if this field accepts assignments.
     */
    public boolean isLValue(CExpressionContext context) {
	if (!field.isFinal() || !(field instanceof CSourceField)) {
	    return true;
	} else if (context.getClassContext().getCClass() == field.getOwner()
		   && !((CSourceField)field).isFullyDeclared()) {
	    return !CVariableInfo.mayBeInitialized(context.getBodyContext().getFieldInfo(((CSourceField)field).getPosition()));
	} else {
	    return true; 
	}
    }

    /**
     * Returns true if there must be exactly one initialization of the field. 
     *
     * @return true if the field is final.
     */
    public boolean isFinal() {
	return field.isFinal();
    }

    
    /**
     * (bft: I think...) Returns whether or not this field access is on
     * the <this> object.  Will only work after semantic analysis has
     * been done.
     */
    public boolean isThisAccess() {
	return (prefix instanceof JThisExpression);
    }

    /**
     * Sets the prefix expression of this to <exp>.
     */
    public void setPrefix(JExpression exp) {
	this.prefix = exp;
    }

    /**
     * Returns true iff this field is already initialized.
     */
    public boolean isInitialized(CExpressionContext context) {
	if (!(field instanceof CSourceField) || field.isStatic()) {
	    return true;
	} else if (context.getClassContext().getCClass() == field.getOwner() &&
		   !((CSourceField)field).isFullyDeclared()) {
	    return CVariableInfo.isInitialized(context.getBodyContext().getFieldInfo(((CSourceField)field).getPosition()));
	} else {
	    return true;
	}
    }

    /**
     * Declares this variable to be initialized.
     *
     * @exception	UnpositionedError an error if this object can't actually
     *		be assignated this may happen with final variables.
     */
    public void setInitialized(CExpressionContext context) {
	if ((field instanceof CSourceField) && (context.getClassContext().getCClass() == field.getOwner() && !((CSourceField)field).isFullyDeclared())) {
	    context.setFieldInfo(((CSourceField)field).getPosition(), CVariableInfo.INITIALIZED);
	}
    }

    /**
     * Returns the exported field.
     */
    public CField getField() {
	return field;
    }

    /**
     * Returns the literal value of this field.
     */
    public JLiteral getLiteral() {
	return (JLiteral)field.getValue();
    }

    /**
     * Returns a string representation of this expression.
     */
    public String toString() {
	StringBuffer	buffer = new StringBuffer();

	buffer.append("JFieldAccessExpression[");
	buffer.append(prefix);
	buffer.append(", ");
	buffer.append(ident);
	if (isConstant()) {
	    buffer.append(" = ");
	    buffer.append(field);
	}
	buffer.append("]");
	return buffer.toString();
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
	CClass	local = context.getClassContext().getCClass();

	findPrefix(local, context);
	checkAccess(local, context);

	check(context,
	      field.isStatic() || !(prefix instanceof JTypeNameExpression),
	      KjcMessages.FIELD_NOSTATIC, ident);
	if (field.isStatic() && !(prefix instanceof JTypeNameExpression)) {
	    context.reportTrouble(new CWarning(getTokenReference(),
					       KjcMessages.INSTANCE_PREFIXES_STATIC_FIELD,
					       ident,
					       prefix.getType()));
	}

	if (field instanceof CSourceField && !context.discardValue()) {
	    ((CSourceField)field).setUsed();
	}

	if (isConstant()) {
	    return field.getValue();
	} else {
	    return this;
	}
    }

    /**
     * Finds the type of the prefix.
     *
     * @exception	PositionedError		Error catched as soon as possible
     */
    protected void findPrefix(CClass local, CExpressionContext context) throws PositionedError {
	if (prefix != null) {
	    prefix = prefix.analyse(context);
	    check(context,
		  prefix.getType().isClassType(),
		  KjcMessages.FIELD_BADACCESS, prefix.getType());
	    constantPrefix = prefix instanceof JTypeNameExpression;
	} else {
	    constantPrefix = true;

	    try {
		field = context.lookupField(local, ident);
		if (field == null) {
		    field = context.getClassContext().lookupOuterField(local, ident); // $$$ Why searching again
		}
	    } catch (UnpositionedError e) {
		throw e.addPosition(getTokenReference());
	    }
	    check(context, field != null, KjcMessages.FIELD_UNKNOWN, ident);

	    if (!field.isStatic()) {
		check(context,
		      !local.isStatic() || local.descendsFrom(field.getOwner()),
		      KjcMessages.FIELD_STATICERR, ident);
		prefix = new JThisExpression(getTokenReference(), field.getOwner());
	    } else {
		prefix = new JTypeNameExpression(getTokenReference(), field.getOwner().getType());
	    }
	    prefix = prefix.analyse(context);
	}
    }

    /**
     * Checks is access to prefix is okay
     *
     * @exception	PositionedError	Error catched as soon as possible
     */
    public void checkAccess(CClass local, CExpressionContext context) throws PositionedError {
	CClass	access = prefix.getType().getCClass();

	try {
	    field = access.lookupField(local, ident);
	} catch (UnpositionedError e) {
	    throw e.addPosition(getTokenReference());
	}
	check(context, field != null, KjcMessages.FIELD_UNKNOWN, ident);

	//!!!DEBUG 000213 graf (see CClassType.getCClass())
	try {
	    field.getType().checkType(context);
	} catch (UnpositionedError e) {
	    throw e.addPosition(getTokenReference());
	}
	//!!!DEBUG 000213 graf
 
	if ((context.getMethodContext() instanceof CConstructorContext) &&
	    (prefix instanceof JThisExpression) &&
	    !field.isStatic()) {
	    if (field.getType().isClassType() &&
		field.getType().getCClass() == context.getClassContext().getCClass()) {
		check(context,
		      ((CConstructorContext)context.getMethodContext()).isSuperConstructorCalled(),
		      KjcMessages.CONSTRUCTOR_EXPLICIT_CALL, field.getIdent());
	    }
	}
    }

    public boolean equals(Object o) {
	return (o instanceof JFieldAccessExpression) &&
	    field.equals(((JFieldAccessExpression)o).field) &&
	    prefix.equals(((JFieldAccessExpression)o).prefix);
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor
     * @param	p		the visitor
     */
    public void accept(KjcVisitor p) {
	p.visitFieldExpression(this, prefix, getIdent());
    }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return	p.visitFieldExpression(this, prefix, getIdent());
  }

    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param	code		the bytecode sequence
     * @param	discardValue	discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
	setLineNumber(code);

	if (! field.isStatic()) {
	    prefix.genCode(code, discardValue);
	} else if (prefix != null) {
	    prefix.genCode(code, true);
	}
	if (!discardValue) {
	    field.genLoad(code);
	}
    }

    /**
     * Generates JVM bytecode to store a value into the storage location
     * denoted by this expression.
     *
     * Storing is done in 3 steps :
     * - prefix code for the storage location (may be empty),
     * - code to determine the value to store,
     * - suffix code for the storage location.
     *
     * @param	code		the code list
     */
    public void genStartStoreCode(CodeSequence code) {
	if (! field.isStatic()) {
	    prefix.genCode(code, false);
	} else if (prefix != null) {
	    prefix.genCode(code, true);
	}
    }

    /**
     * Generates JVM bytecode to store a value into the storage location
     * denoted by this expression.
     *
     * Storing is done in 3 steps :
     * - prefix code for the storage location (may be empty),
     * - code to determine the value to store,
     * - suffix code for the storage location.
     *
     * @param	code		the code list
     * @param	discardValue	discard the result of the evaluation ?
     */
    public void genEndStoreCode(CodeSequence code, boolean discardValue) {
	if (!discardValue) {
	    int	opcode;

	    if (field.isStatic()) {
		if (getType().getSize() == 2) {
		    opcode = opc_dup2;
		} else {
		    opcode = opc_dup;
		}
	    } else {
		if (getType().getSize() == 2) {
		    opcode = opc_dup2_x1;
		} else {
		    opcode = opc_dup_x1;
		}
	    }

	    code.plantNoArgInstruction(opcode);
	}

	field.genStore(code);
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    /*
     * Is the prefix null or a type name ? Needed for isConstant().
     */
    private boolean		constantPrefix;

    protected JExpression       prefix;		// !!! graf 991205 make private
    protected String		ident;
    protected CField		field;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JFieldAccessExpression other = new at.dms.kjc.JFieldAccessExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JFieldAccessExpression other) {
  super.deepCloneInto(other);
  other.constantPrefix = this.constantPrefix;
  other.prefix = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.prefix, this);
  other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident, this);
  other.field = (at.dms.kjc.CField)at.dms.kjc.AutoCloner.cloneToplevel(this.field, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JNameExpression.java,v 1.8 2003-08-21 09:44:20 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

/**
 * JLS 6.5.6 Expression Names.
 */
public class JNameExpression extends JExpression {

    // ----------------------------------------------------------------------
    // CONSTRUCTORS
    // ----------------------------------------------------------------------

    protected JNameExpression() {} // for cloner only

    /**
     * Construct a node in the parsing tree
     * @param where the line of this node in the source code
     */
    public JNameExpression(TokenReference where, String ident) {
	super(where);

	this.ident = ident;
	assert(ident.indexOf('.') == -1); // $$$
	assert(ident.indexOf('/') == -1);
    }

    /**
     * Construct a node in the parsing tree
     * @param where the line of this node in the source code
     */
    public JNameExpression(TokenReference where, JExpression prefix, String ident) {
	super(where);

	this.prefix = prefix;
	this.ident = ident;
	assert(ident.indexOf('.') == -1); // $$$
	assert(ident.indexOf('/') == -1);
    }

    /**
     * Construct a node in the parsing tree
     * @param where the line of this node in the source code
     */
    public JNameExpression(TokenReference where, String name, boolean fullyQualified) {
	super(where);

	int			pos = Math.max(name.lastIndexOf("."), name.lastIndexOf("/"));

	if (fullyQualified && pos >= 0) {
	    this.prefix = new JNameExpression(where, name.substring(0, pos), true);
	    this.ident = name.substring(pos + 1, name.length()).intern();
	} else {
	    this.ident = name.intern();
	}
    }

    // ----------------------------------------------------------------------
    // ACCESSORS (NO ACCESS) // it is a temporary node
    // ----------------------------------------------------------------------

    /**
     * Compute the type of this expression (called after parsing)
     * @return the type of this expression
     */
    public CType getType() {
	return null;
    }

    /**
     * @return the name of this name expression
     */
    public String getName() {
	return ident;
    }

    public void setPrefix(JExpression p) {
	this.prefix = p;
    }

    /**
     * @return the prefix of this name expression
     */
    public JExpression getPrefix() {
	return prefix;
    }

    /**
     * Returns the longest name available
     */
    public String getQualifiedName() {
	String str = getName();

	if (prefix == null) {
	    return str;
	} else if (prefix instanceof JNameExpression) {
	    return ((JNameExpression)prefix).getQualifiedName() + "." + str;
	} else if (prefix instanceof JTypeNameExpression) {
	    return ((JNameExpression)prefix) + "." + str;
	} else {
	    return str;
	}
    }

    /**
     * Returns a string representation of this object.
     */
    public String toString() {
	StringBuffer	buffer = new StringBuffer();

	if (prefix != null) {
	    buffer.append(prefix.toString());
	    buffer.append(".");
	}
	buffer.append(ident);
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
	try {
	    // 6.5.2 Reclassification of Contextually Ambiguous Names
	    // If the AmbiguousName is a simple name, consisting of a single Identifier:
	    if (prefix == null) {
		// If the Identifier appears within the scope of a local variable declaration or parameter
		JLocalVariable	var = context.lookupLocalVariable(ident);
		if (var != null) {
		    return new JLocalVariableExpression(getTokenReference(), var).analyse(context);
		}

		// Otherwise, consider the class or interface C within whose declaration the Identifier occurs
		if (context.lookupField(context.getClassContext().getCClass(), ident) != null) {
		    return createClassField(getTokenReference(), ident).analyse(context);
		}

		// If the Identifier appears within (an outer) scope of a local variable declaration or parameter
		JExpression outer = context.getBlockContext().lookupOuterLocalVariable(getTokenReference(), ident);
		if (outer != null) {
		    return outer.analyse(context);
		}

		// Otherwise, outers
		if (context.getClassContext().lookupOuterField(context.getClassContext().getCClass(), ident) != null) {
		    return createClassField(getTokenReference(), ident).analyse(context);
		}

		try {
		    // Otherwise: TypeName
		    CClassType	type = CClassType.lookup(ident);

		    type.checkType(context);
		    return new JTypeNameExpression(getTokenReference(), type);
		} catch (UnpositionedError cue) {
		    // Otherwise: PackageName
		    throw new CLineError(getTokenReference(), KjcMessages.VAR_UNKNOWN, ident);
		}
	    }

	    assert(prefix != null);

	    try {
		prefix = prefix.analyse(context);
	    } catch (CLineError cue) {
		if (cue.hasDescription(KjcMessages.VAR_UNKNOWN)) {
		    return convertToPackageName(cue, context);
		} else {
		    throw cue;
		}
	    }

	    // If the name to the left of the "." is reclassified as a TypeName
	    if (prefix instanceof JTypeNameExpression) {
		if (((JTypeNameExpression)prefix).getClassType().getCClass().lookupField(context.getClassContext().getCClass(), ident) != null) {
		    // there is a field
		    return createClassField(getTokenReference(), prefix, ident).analyse(context);
		} else {
		    // no field => should be a type name
		    return new JTypeNameExpression(getTokenReference(),
						   ((JTypeNameExpression)prefix).getQualifiedName() + "/" + ident);
		}
	    }

	    // If the name to the left of the "." is reclassified as an ExpressionName

	    if (ident == Constants.JAV_LENGTH) {
		// is it an array access ?
		if (prefix.getType().isArrayType()) {
		    return new JArrayLengthExpression(getTokenReference(), prefix);
		}
	    }

	    return createClassField(getTokenReference(), prefix, ident).analyse(context);
	} catch (UnpositionedError e) {
	    throw e.addPosition(getTokenReference());
	}
    }

    /**
     * Try to convert to a package name
     */
    private JExpression convertToPackageName(CLineError cue, CExpressionContext context) throws PositionedError {
	try {
	    if (prefix instanceof JNameExpression) {
		CClassType	type = CClassType.lookup(((JNameExpression)prefix).getName() + '/' + ident);

		type.checkType(context);
		return new JTypeNameExpression(getTokenReference(), type);
	    } else if (!(prefix instanceof JTypeNameExpression)) {
		throw new CLineError(getTokenReference(), KjcMessages.VAR_UNKNOWN, ident);
	    } else {
		return prefix;
	    }
	} catch (UnpositionedError cue2) {
	    // Otherwise: PackageName
	    ident = ((JNameExpression)prefix).getName() + '/' +  ident;
	    throw new CLineError(getTokenReference(), KjcMessages.VAR_UNKNOWN, ident);
	}
    }

    /**
     * Since class field may be overloaded in sub compiler, this method allow
     * you to specifie the type of class field without needed to touch
     * the huge method above !
     */
    protected JFieldAccessExpression createClassField(TokenReference ref,
						      JExpression prefix,
						      String ident)
    {
	return new JFieldAccessExpression(ref, prefix, ident);
    }

    /**
     * Since class field may be overloaded in sub compiler, this method allow
     * you to specifie the type of class field without needed to touch
     * the huge method above !
     */
    protected JFieldAccessExpression createClassField(TokenReference ref,
						      String ident)
    {
	return new JFieldAccessExpression(ref, ident);
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor
     * @param	p		the visitor
     */
    public void accept(KjcVisitor p) {
	p.visitNameExpression(this, prefix, ident);
    }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return	p.visitNameExpression(this, prefix, ident);
  }

    /**
     * Generates JVM bytecode to evaluate this expression.
     *
     * @param	code		the bytecode sequence
     * @param	discardValue	discard the result of the evaluation ?
     */
    public void genCode(CodeSequence code, boolean discardValue) {
	throw new InconsistencyException();
    }

    // ----------------------------------------------------------------------
    // DATA MEMBERS
    // ----------------------------------------------------------------------

    private JExpression		prefix;
    private String		ident;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JNameExpression other = new at.dms.kjc.JNameExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JNameExpression other) {
  super.deepCloneInto(other);
  other.prefix = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.prefix, this);
  other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JThisExpression.java,v 1.7 2003-08-21 09:44:21 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * A 'this' expression
 */
public class JThisExpression extends JExpression {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JThisExpression() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * @param where the line of this node in the source code
   */
  public JThisExpression(TokenReference where) {
    super(where);
  }

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	self		the class onto this suffix is applied
   */
  public JThisExpression(TokenReference where, CClass self) {
    this(where);
    this.self = self;
  }

  /**
   * Construct a node in the parsing tree
   * @param	where		the line of this node in the source code
   * @param	prefix		the left expression like t.this
   */
  public JThisExpression(TokenReference where, JExpression prefix) {
    this(where);
    this.prefix = prefix;
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Compute the type of this expression (called after parsing)
   * @return the type of this expression
   */
  public CType getType() {
      // not sure why <self> is null, but it was causing problems with
      // FMRadio and this seems to fix it
      if (self==null) {
	  return CStdType.Null;
      } else {
	  return self.getType();
      }
  }

  /**
   * @return is this expression a lvalue ?
   */
  public boolean isLValue(CExpressionContext context) {
    return false;
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
  public JExpression analyse(final CExpressionContext context) throws PositionedError {
    if (prefix != null) {
      prefix = prefix.analyse(context);
      check(context, prefix.getType().isClassType(), KjcMessages.THIS_BADACCESS);
      self = prefix.getType().getCClass();
    } else if (self == null) {
      self = context.getClassContext().getCClass();
    }

    if (!context.getClassContext().getCClass().descendsFrom(self)) {
      // access to outer class
      CClass		clazz = context.getClassContext().getCClass();
      JExpression	expr = null;
      CClassContext	classContext = context.getClassContext();

      while (!clazz.descendsFrom(self)) {
	check(context,
	      !classContext.getTypeDeclaration().getCClass().isStatic(),
	      KjcMessages.THIS_INVALID_OUTER);
	classContext.getTypeDeclaration().addOuterThis();
	classContext = classContext.getParentContext().getClassContext();

	if (expr == null) {
	  if (context.getMethodContext() instanceof CConstructorContext) {
	    JGeneratedLocalVariable local = new JGeneratedLocalVariable(null, 0, getType(), "toto", null) {
		/**
		 * @return the local index in context variable table
		 */
		public int getPosition() {
		  return context.getMethodContext().getCMethod().getParameters().length + 1 /*this*/;
		}
	      };

	    return new JLocalVariableExpression(getTokenReference(), local) {
		public JExpression analyse(CExpressionContext ctxt) {
		  // already checked
		  return this;
		}
	      };
	  } else {
	    expr = new JFieldAccessExpression(getTokenReference(), new JThisExpression(getTokenReference()), JAV_OUTER_THIS);
	  }
	} else {
	  expr = new JFieldAccessExpression(getTokenReference(), expr, JAV_OUTER_THIS);
	}

	expr = expr.analyse(context);
	clazz = ((CClassType)expr.getType()).getCClass();
      }

      if (prefix != null) {
	check(context, expr.getType().equals(prefix.getType()) ||
	      /*May be it is an innerclass with the same name, therefore the prefix name has been
	       wrongly assigned to the outer. So we compare the names instead:*/
	      ((CClassType)expr.getType()).getCClass().getIdent().equals(((CClassType)prefix.getType()).getCClass().getIdent()),
	      KjcMessages.THIS_INVALID_OUTER, prefix.getType());
      }
      return expr;
    }

    check(context, !context.getMethodContext().getCMethod().isStatic(), KjcMessages.BAD_THIS_STATIC);

    return this;
  }

  public boolean equals(Object o) {
    return o instanceof JThisExpression &&
      self.equals(((JThisExpression)o).self);
  }

  // ----------------------------------------------------------------------
  // CODE GENERATION
  // ----------------------------------------------------------------------

  /**
   * Accepts the specified visitor
   * @param	p		the visitor
   */
  public void accept(KjcVisitor p) {
    p.visitThisExpression(this, prefix);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitThisExpression(this, prefix);
  }

  /**
   * Generates JVM bytecode to evaluate this expression.
   *
   * @param	code		the bytecode sequence
   * @param	discardValue	discard the result of the evaluation ?
   */
  public void genCode(CodeSequence code, boolean discardValue) {
    if (! discardValue) {
      setLineNumber(code);
      code.plantLoadThis();
    }
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

  private CClass		self;
  private JExpression		prefix;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JThisExpression other = new at.dms.kjc.JThisExpression();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JThisExpression other) {
  super.deepCloneInto(other);
  other.self = (at.dms.kjc.CClass)at.dms.kjc.AutoCloner.cloneToplevel(this.self, this);
  other.prefix = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.prefix, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

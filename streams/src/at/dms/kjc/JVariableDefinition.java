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
 * $Id: JVariableDefinition.java,v 1.10 2004-01-28 16:55:35 dmaze Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import java.io.*;

/**
 * This class represents a local variable definition in the syntax tree
 */
public class JVariableDefinition extends JLocalVariable {
  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JVariableDefinition() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	modifiers	the modifiers of this variable
   * @param	ident		the name of this variable
   * @param	initializer	the initializer
   */
  public JVariableDefinition(TokenReference where,
			     int modifiers,
			     CType type,
			     String ident,
			     JExpression initializer)
  {
    super(where, modifiers, DES_LOCAL_VAR, type, ident, initializer);
    assert type != null;
  }

    public String toString() {
	return "VarDef["+name+"="+expr+"]";
    }
    
  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * hasInitializer
   * @return	true if there is an initializer
   */
  public boolean hasInitializer() {
    return getValue() != null;
  }

  /**
   * @return	the initial value
   */
  public JExpression getValue() {
    return expr;
  }

    public void setValue(JExpression expr) {
	this.expr = expr;
    }

    public void setInitializer (JExpression init) {
	this.expr = init;
    }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   * @param	context		the current context
   * @return	true iff sub tree is correct enought to check code
   */
  public void checkInterface(CClassContext context) {
    try {
      type.checkType(context);
    } catch (UnpositionedError cue) {
      /*checkbody will do it*/
    }
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Check expression and evaluate and alter context
   * @param	context			the actual context of analyse
   * @exception	PositionedError Error catched as soon as possible
   */
  public void analyse(CBodyContext context) throws PositionedError {
    try {
      type.checkType(context);
    } catch (UnpositionedError cue) {
      throw cue.addPosition(getTokenReference());
    }
    if (! type.isPrimitive()) {
      check(context,
	    type.getCClass().isAccessible(context.getClassContext().getCClass()),
	    KjcMessages.CLASS_NOACCESS, type);
    }

    if (expr != null) {
      // special case for array initializers
      if (expr instanceof JArrayInitializer) {
	check(context, type.isArrayType(), KjcMessages.ARRAY_INIT_NOARRAY, type);
	((JArrayInitializer)expr).setType((CArrayType)type);
      }

      CExpressionContext	expressionContext = new CExpressionContext(context);

      expr = expr.analyse(expressionContext);
      if (expr instanceof JTypeNameExpression) {
	check(context,
	      false,
	      KjcMessages.VAR_UNKNOWN,
	      ((JTypeNameExpression)expr).getQualifiedName());
      }

      check(context,
	    expr.isAssignableTo(type),
	    KjcMessages.VAR_INIT_BADTYPE, getIdent(), expr.getType());
      expr = expr.convertType(type, expressionContext);
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
    p.visitVariableDefinition(this, modifiers, type, getIdent(), expr);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      return    p.visitVariableDefinition(this, modifiers, type, getIdent(), expr);
  }


/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JVariableDefinition other = new at.dms.kjc.JVariableDefinition();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JVariableDefinition other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

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
 * $Id: JFieldDeclaration.java,v 1.7 2003-05-28 05:58:43 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.CWarning;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;

/**
 * This class represents a java class in the syntax tree
 */
public class JFieldDeclaration extends JMemberDeclaration {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JFieldDeclaration() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   *
   * @param	where		the line of this node in the source code
   * @param	variable	the variable definition
   * @param	javadoc		is this field deprecated
   * @param	comments	comments in the source text
   */
  public JFieldDeclaration(TokenReference where,
			   JVariableDefinition variable,
			   JavadocComment javadoc,
			   JavaStyleComment[] comments)
  {
    super(where, javadoc, comments);

    this.variable = variable;
  }

  // ----------------------------------------------------------------------
  // ACCESSOR
  // ----------------------------------------------------------------------

  /**
   * Returns true if this field declarator has initializer (should be initialized)
   */
  public boolean hasInitializer() {
    return variable.hasInitializer();
  }

  /**
   * Returns the type of this field
   */
  public CType getType() {
    return variable.getType();
  }

  /**
   * Returns true if this field need to be initialized
   * WARNING: this method return true when initial value corresponds to a default value
   * ====> a second check should be made after calling "analyse" to ensure that
   * an initialization is really needed
   */
  public boolean needInitialization() {
    return hasInitializer();
  }

  public JVariableDefinition getVariable() {
    return variable;
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   *  sub classes must check modifiers and call checkInterface(super)
   * @param v a vector to collect fields
   * @return true iff sub tree is correct enought to check code
   * @exception	PositionedError	an error with reference to the source file
   */
  public CSourceField checkInterface(CClassContext context) throws PositionedError {
    int		modifiers = variable.getModifiers();

    if (! context.getCClass().isInterface()) {
      // JLS 8.3.1 : Class Field Modifiers

      // Syntactically valid field modifiers
      check(context,
	    CModifier.isSubsetOf(modifiers,
				 ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE
				 | ACC_STATIC | ACC_FINAL | ACC_TRANSIENT
				 | ACC_VOLATILE),
	    KjcMessages.NOT_CLASS_FIELD_MODIFIERS,
	    CModifier.toString(CModifier.notElementsOf(modifiers,
						       ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE
						       | ACC_STATIC | ACC_FINAL | ACC_TRANSIENT
						       | ACC_VOLATILE)));
    } else {
      // JLS 9.3 : Interface Field (Constant) Declarations

      // Every field declaration in the body of an interface is
      // implicitly public, static, and final.
      modifiers |= ACC_PUBLIC | ACC_STATIC | ACC_FINAL;

      // Syntactically valid interface field modifiers
      check(context,
	    CModifier.isSubsetOf(modifiers, ACC_PUBLIC | ACC_FINAL | ACC_STATIC),
	    KjcMessages.NOT_INTERFACE_FIELD_MODIFIERS,
	    CModifier.toString(CModifier.notElementsOf(modifiers,
						       ACC_PUBLIC | ACC_FINAL | ACC_STATIC)));
    }

    variable.checkInterface(context);
    setInterface(new CSourceField(context.getCClass(),
				  modifiers,
				  variable.getIdent(),
				  variable.getType(),
				  isDeprecated()));

    return (CSourceField)getField();
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Analyses the statement (semantically).
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  public void analyse(CBodyContext context) throws PositionedError {
    variable.analyse(context);

    if (hasInitializer() && getField().isFinal()) {
      JExpression	value = variable.getValue();

      if (value.isConstant()) {
	getField().setValue(value.getLiteral());

	if (! getField().isStatic()) {
	  context.reportTrouble(new CWarning(getTokenReference(),
					     KjcMessages.FINAL_FIELD_IMPLICITLY_STATIC,
					     getField().getIdent()));
	}
      }
    }

    if (hasInitializer()) {
      context.setFieldInfo(((CSourceField)getField()).getPosition(), CVariableInfo.INITIALIZED);
    }

    if (! (getField().isPublic() || getField().isPrivate())) {
      context.reportTrouble(new CWarning(getTokenReference(),
					 KjcMessages.PACKAGE_PROTECTED_ATTRIBUTE,
					 getField().getIdent()));
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
    super.accept(p);
    p.visitFieldDeclaration(this,
			    variable.getModifiers(),
			    variable.getType(),
			    variable.getIdent(),
			    variable.getValue());
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
    Object Trash = super.accept(p);
    return p.visitFieldDeclaration(this,
			    variable.getModifiers(),
			    variable.getType(),
			    variable.getIdent(),
			    variable.getValue());
  }

    /**
     * Sets the initial value of this.
     */
    public void setValue(JExpression expr) {
	variable.setValue(expr);
    }

  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    if (variable.getValue() != null) {
      setLineNumber(code);

      if (!getField().isStatic()) {
	code.plantLoadThis();
      }
      variable.getValue().genCode(code, false);
      getField().genStore(code);
    }
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    // need a different method array for every method in case people
    // start to add methods; can't just have a constant.
    public static JFieldDeclaration[] EMPTY() {
	return new JFieldDeclaration[0];
    }

  protected JVariableDefinition		variable;

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.JFieldDeclaration other = new at.dms.kjc.JFieldDeclaration();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.JFieldDeclaration other) {
  super.deepCloneInto(other);
  other.variable = (at.dms.kjc.JVariableDefinition)at.dms.kjc.AutoCloner.cloneToplevel(this.variable);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

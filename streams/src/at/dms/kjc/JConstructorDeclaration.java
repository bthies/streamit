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
 * $Id: JConstructorDeclaration.java,v 1.3 2003-05-16 21:58:35 thies Exp $
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
public class JConstructorDeclaration extends JMethodDeclaration {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JConstructorDeclaration() {} // for cloner only

  /**
   * Construct a node in the parsing tree
   * This method is directly called by the parser
   * @param	where		the line of this node in the source code
   * @param	parent		parent in which this methodclass is built
   * @param	modifiers	list of modifiers
   * @param	ident		the name of this method
   * @param	parameters	the parameters of this method
   * @param	exceptions	the exceptions throw by this method
   * @param	constructorCall	an explicit constructor invocation
   * @param	body		the body of the method
   * @param	javadoc		java documentation comments
   * @param	comments	other comments in the source code
   */
  public JConstructorDeclaration(TokenReference where,
				 int modifiers,
				 String ident,
				 JFormalParameter[] parameters,
				 CClassType[] exceptions,
				 JConstructorCall constructorCall,
				 JStatement[] body,
				 JavadocComment javadoc,
				 JavaStyleComment[] comments)
  {
    super(where,
	  modifiers,
	  CStdType.Void,
	  ident,
	  parameters,
	  exceptions,
	  new JConstructorBlock(where, constructorCall, body),
	  javadoc,
	  comments);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the constructor called by this constructor.
   */
  public CMethod getCalledConstructor() {
    return ((JConstructorBlock)body).getCalledConstructor();
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   * @exception	PositionedError	an error with reference to the source file
   */
  public CSourceMethod checkInterface(CClassContext context) throws PositionedError {
    check(context,
	  CModifier.isSubsetOf(modifiers, ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE),
	  KjcMessages.INVALID_CONSTRUCTOR_FLAGS,
	  ident);
    check(context,
	  ident == context.getCClass().getIdent(),
	  KjcMessages.CONSTRUCTOR_BAD_NAME,
	  ident,
	  context.getCClass().getIdent());
    return super.checkInterface(context);
  }

  /**
   * Check expression and evaluate and alter context
   * @param	context			the actual context of analyse
   * @return	a pure java expression including promote node
   * @exception	PositionedError Error catched as soon as possible
   */
  public void checkBody1(CClassContext context) throws PositionedError {
    check(context, body != null, KjcMessages.CONSTRUCTOR_NOBODY, ident);

    CMethodContext	self = new CConstructorContext(context, getMethod());
    CBlockContext	block = new CBlockContext(self, parameters.length);
    CClass              owner = context.getClassContext().getCClass();

    block.addThisVariable();
    if (owner.isNested() && owner.hasOuterThis()) {
      block.addThisVariable(); // add enclosing this$0
    }
    for (int i = 0; i < parameters.length; i++) {
      parameters[i].analyse(block);
    }

    body.analyse(block);

    block.close(getTokenReference());
    self.close(getTokenReference());

    // check that all final instance fields are initialized
    CField[]	classFields = context.getCClass().getFields();

    for (int i = 0; i < classFields.length; i++) {
      if (! classFields[i].isStatic() && !CVariableInfo.isInitialized(self.getFieldInfo(i))) {
	check(context,
	      !classFields[i].isFinal() || classFields[i].getIdent() == JAV_OUTER_THIS,
	      KjcMessages.UNINITIALIZED_FINAL_FIELD,
	      classFields[i].getIdent());

	context.reportTrouble(new CWarning(getTokenReference(),
					   KjcMessages.UNINITIALIZED_FIELD,
					   classFields[i].getIdent()));
      }
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
    genComments(p);
    p.visitConstructorDeclaration(this,
				  modifiers,
				  ident,
				  parameters,
				  exceptions,
				  (JConstructorBlock)body);
  }
    
   /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      Object Trash = genComments1(p);
      return p.visitConstructorDeclaration(this,
				    modifiers,
					   ident,
					   parameters,
					   exceptions,
					   (JConstructorBlock)body);
  }  
    
}

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
 * $Id: JInterfaceDeclaration.java,v 1.3 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;

import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.util.Utils;

/**
 * This class represents a Java interface declaration in the syntax tree.
 */
public class JInterfaceDeclaration extends JTypeDeclaration {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JInterfaceDeclaration() {} // for cloner only

  /**
   * Constructs an interface declaration node in the syntax tree.
   *
   * @param	where		the line of this node in the source code
   * @param	modifiers	the list of modifiers of this class
   * @param	ident		the simple name of this class
   * @param	interfaces	the interfaces implemented by this class
   * @param	fields		the fields defined by this class
   * @param	methods		the methods defined by this class
   * @param	inners		the inner classes defined by this class
   * @param	initializers	the class and instance initializers defined by this class
   * @param	javadoc		java documentation comments
   * @param	comment		other comments in the source code
   */
  public JInterfaceDeclaration(TokenReference where,
			       int modifiers,
			       String ident,
			       CClassType[] interfaces,
			       JFieldDeclaration[] fields,
			       JMethodDeclaration[] methods,
			       JTypeDeclaration[] inners,
			       JPhylum[] initializers,
			       JavadocComment javadoc,
			       JavaStyleComment[] comment)
  {
    super(where,
	  modifiers | ACC_INTERFACE | ACC_ABSTRACT,
	  ident,
	  interfaces,
	  fields,
	  methods,
	  inners,
	  initializers,
	  javadoc,
	  comment);
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   * @return true iff sub tree is correct enought to check code
   * @exception	PositionedError	an error with reference to the source file
   */
  public void checkInterface(CContext context) throws PositionedError {
    checkModifiers(context);

    statInit = constructStaticInitializers();

    super.checkInterface(context, CStdType.Object);
  }

  /**
   * Checks that the modifiers are valid (JLS 9.1.1).
   *
   * @param	context		the analysis context
   * @exception	PositionedError	an error with reference to the source file
   */
  private void checkModifiers(final CContext context) throws PositionedError {
    int		modifiers = getModifiers();

    // Syntactically valid interface modifiers
    check(context,
	  CModifier.isSubsetOf(modifiers,
			       ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE
			       | ACC_ABSTRACT | ACC_STATIC | ACC_STRICT
			       | ACC_INTERFACE),
	  KjcMessages.NOT_INTERFACE_MODIFIERS,
	  CModifier.toString(CModifier.notElementsOf(modifiers,
						     ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE
						     | ACC_ABSTRACT | ACC_STATIC | ACC_STRICT
						     | ACC_INTERFACE)));

    // JLS 9.1.1 : The access modifiers protected and private pertain only
    // to member interfaces within a directly enclosing class declaration.
    check(context,
	  (isNested()
	   && getOwner().getCClass().isClass()
	   && !(context instanceof CBodyContext))
	  || !CModifier.contains(modifiers, ACC_PROTECTED | ACC_PRIVATE),
	  KjcMessages.INVALID_INTERFACE_MODIFIERS,
	  CModifier.toString(CModifier.getSubsetOf(modifiers, ACC_PROTECTED | ACC_PRIVATE)));

    // JLS 9.1.1 : The access modifier static pertains only to member interfaces.
    check(context,
	  isNested() || !CModifier.contains(modifiers, ACC_STATIC),
	  KjcMessages.INVALID_INTERFACE_MODIFIERS,
	  CModifier.toString(CModifier.getSubsetOf(modifiers, ACC_STATIC)));


    // JLS 8.5.2 : Member interfaces are always implicitly static.
    if (isNested()) {
      setModifiers(modifiers | ACC_STATIC);
    }

    // JLS 9.5 : A member type declaration in an interface is implicitly
    // static and public.
    if (isNested() && getOwner().getCClass().isInterface()) {
      setModifiers(modifiers | ACC_STATIC | ACC_PUBLIC);
    }
  }

  /**
   * Check that initializers are correct
   * @exception	PositionedError	an error with reference to the source file
   */
  public void checkInitializers(CContext context) throws PositionedError {
    CInterfaceContext self = new CInterfaceContext(context, sourceClass);

    if (statInit != null) {
      statInit.checkInitializer(self);
    }

    // Check inners
    for (int i = inners.length - 1; i >= 0 ; i--) {
      inners[i].checkInitializers(self);
    }

    super.checkInitializers(context);
  }

  /**
   * checkTypeBody
   * Check expression and evaluate and alter context
   * @param context the actual context of analyse
   * @return  a pure java expression including promote node
   * @exception	PositionedError	an error with reference to the source file
   */
  public void checkTypeBody(CContext context) throws PositionedError {
    CInterfaceContext self = new CInterfaceContext(context, sourceClass);

    for (int i = 0; i < inners.length; i++) {
      try {
	inners[i].checkTypeBody(self);
      } catch (CBlockError e) {
	context.reportTrouble(e);
      }
    }

    for (int i = methods.length - 1; i >= 0 ; i--) {
      try {
	methods[i].checkBody1(self);
      } catch (PositionedError ce) {
	context.reportTrouble(ce);
      }
    }

    super.checkTypeBody(context);
  }

  /**
   * check static initializers
   */
  private JInitializerDeclaration constructStaticInitializers() {
    // collect all static initializers and build a method
    Vector	elems = new Vector();
    boolean	needGen = false;

    for (int i = 0; i < body.length; i++) {
      if (body[i] instanceof JClassBlock) {
	elems.addElement(body[i]);
	needGen = true;
      } else {
	if (body[i] instanceof JFieldDeclaration) {
	  needGen |= ((JFieldDeclaration)body[i]).needInitialization();
	  elems.addElement(new JClassFieldDeclarator(getTokenReference(), (JFieldDeclaration)body[i]));
	}
      }
    }

    if (elems.size() > 0) {
      JStatement[]	stmts = (JStatement[])Utils.toArray(elems, JStatement.class);

      return new JInitializerDeclaration(getTokenReference(),
					 new JClassBlock(getTokenReference(), false, stmts),
					 true,
					 !needGen);
    } else {
      return null;
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
    p.visitInterfaceDeclaration(this,
				getCClass().getModifiers(),
				sourceClass.getIdent(),
				interfaces,
				body,
				methods);
  }

     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
      Object trash = super.accept(p);
      return p.visitInterfaceDeclaration(this,
					 getCClass().getModifiers(),
					 sourceClass.getIdent(),
					 interfaces,
					 body,
					 methods);
  }
    
}

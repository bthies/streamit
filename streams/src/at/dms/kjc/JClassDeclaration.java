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
 * $Id: JClassDeclaration.java,v 1.8 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import java.util.Vector;

import at.dms.compiler.CWarning;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.Utils;

/**
 * This class represents a java class in the syntax tree
 */
public class JClassDeclaration extends JTypeDeclaration {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JClassDeclaration() {} // for cloner only

  /**
   * Constructs a class declaration node in the syntax tree.
   *
   * @param	where		the line of this node in the source code
   * @param	modifiers	the list of modifiers of this class
   * @param	ident		the simple name of this class
   * @param	superClass	the super class of this class
   * @param	interfaces	the interfaces implemented by this class
   * @param	fields		the fields defined by this class
   * @param	methods		the methods defined by this class
   * @param	inners		the inner classes defined by this class
   * @param	initializers	the class and instance initializers defined by this class
   * @param	javadoc		java documentation comments
   * @param	comment		other comments in the source code
   */
  public JClassDeclaration(TokenReference where,
			   int modifiers,
			   String ident,
			   CClassType superClass,
			   CClassType[] interfaces,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods,
			   JTypeDeclaration[] inners,
			   JPhylum[] initializers,
			   JavadocComment javadoc,
			   JavaStyleComment[] comment)
  {
    super(where, modifiers, ident, interfaces, fields, methods, inners, initializers, javadoc, comment);
    setSuperClass(superClass);
  }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------



  /**
   * Gets the CSourceClass
   */
  public CSourceClass getSourceClass() {
      return sourceClass;
  }

  /**
   * Sets the super class
   */
  public void setSuperClass(CClassType superClass) {
    this.superClass = superClass;
    if (superClass!=null) {
	this.superName = superClass.toString();
    }
  }

  /**
   * Sets the super class
   */
  public void setInterfaces(CClassType[] interfaces) {
    this.interfaces = interfaces;
  }

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   * @exception	PositionedError	an error with reference to the source file
   */
  public void checkInterface(final CContext context) throws PositionedError {
    checkModifiers(context);

    if (superClass == null) {
      if (sourceClass.getType().equals(CStdType.Object)) {
	// java/lang/Object
	// superClass = null;
	// superClass1 = null;
      } else {
	  setSuperClass(CStdType.Object);
      }
    } else {
      try {
	superClass.checkType(context);
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }
    }

    // check access
    if (superClass != null) {
      CClass	clazz = superClass.getCClass();

      check(context, 
            clazz.isAccessible(getCClass()),
	    KjcMessages.CLASS_ACCESSPARENT, superClass.getQualifiedName());
      check(context,
	    !clazz.isFinal(),
	    KjcMessages.CLASS_PARENT_FINAL, superClass.getQualifiedName());
      check(context,
	    !clazz.isInterface(),
	    KjcMessages.CLASS_EXTENDS_INTERFACE, superClass.getQualifiedName());
    }

    CClassContext	self = new CClassContext(context, sourceClass, this);

    statInit = constructInitializers(true);
    if (statInit != null) {
      statInit.checkInterface(self);
    }

    int	i = 0;
    for (; i < methods.length; i++) {
      if (methods[i] instanceof JConstructorDeclaration) {
	break;
      }
    }
    if (i == methods.length && getDefaultConstructor() == null) {
      setDefaultConstructor(constructDefaultConstructor());
    }

    instanceInit = constructInitializers(false);
    if (instanceInit != null) {
      instanceInit.checkInterface(self);
    }

    getCClass().setSuperClass(superClass);
    super.checkInterface(context, superClass);
    // Check inners
    for (int k = 0; k  < inners.length; k++) {
      if (!inners[k].getCClass().isStatic()) inners[k].addOuterThis();
    }
  }

  /**
   * Checks that the modifiers are valid (JLS 8.1.1).
   *
   * @param	context		the analysis context
   * @exception	PositionedError	an error with reference to the source file
   */
  private void checkModifiers(final CContext context) throws PositionedError {
    int		modifiers = getModifiers();

    // Syntactically valid class modifiers
    check(context,
	  CModifier.isSubsetOf(modifiers,
			       ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE | ACC_ABSTRACT
			       | ACC_STATIC | ACC_FINAL | ACC_STRICT),
	  KjcMessages.NOT_CLASS_MODIFIERS,
	  CModifier.toString(CModifier.notElementsOf(modifiers,
						     ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE
						     | ACC_ABSTRACT | ACC_STATIC | ACC_FINAL
						     | ACC_STRICT)));

    // JLS 8.1.1 : The access modifier public pertains only to top level
    // classes and to member classes.
    check(context,
	  (!isNested()
	   || !(context instanceof CBodyContext))
	  || !CModifier.contains(modifiers, ACC_PUBLIC),
	  KjcMessages.INVALID_CLASS_MODIFIERS,
	  CModifier.toString(CModifier.getSubsetOf(modifiers, ACC_PUBLIC)));

    // JLS 8.1.1 : The access modifiers protected and private pertain only to
    // member classes within a directly enclosing class declaration.
    check(context,
	  (isNested()
	   && getOwner().getCClass().isClass() 
	   && !(context instanceof CBodyContext))
	  || !CModifier.contains(modifiers, ACC_PROTECTED | ACC_PRIVATE),
	  KjcMessages.INVALID_CLASS_MODIFIERS,
	  CModifier.toString(CModifier.getSubsetOf(modifiers, ACC_PROTECTED | ACC_PRIVATE)));

    // JLS 8.1.1 : The access modifier static pertains only to member classes.
    check(context,
	  isNested() || !CModifier.contains(modifiers, ACC_STATIC),
	  KjcMessages.INVALID_CLASS_MODIFIERS,
	  CModifier.toString(CModifier.getSubsetOf(modifiers, ACC_STATIC)));

    // JLS 8.1.1.2 : A compile-time error occurs if a class is declared both
    // final and abstract.
    check(context,
	  CModifier.getSubsetSize(modifiers, ACC_FINAL | ACC_ABSTRACT) <= 1,
	  KjcMessages.INCOMPATIBLE_MODIFIERS,
	  CModifier.toString(CModifier.getSubsetOf(modifiers, ACC_FINAL | ACC_ABSTRACT)));

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
    self = new CClassContext(context, sourceClass, this);

    compileStaticInitializer(self);

    if (getCClass().getSuperClass() != null) {
      check(context,
	    !(getCClass().descendsFrom(getCClass().getSuperClass()) &&
	      getCClass().getSuperClass().descendsFrom(getCClass())),
	    KjcMessages.CLASS_CIRCULARITY,
	    ident);
    }

    // Check inners
    for (int i = inners.length - 1; i >= 0 ; i--) {
      inners[i].checkInitializers(self);
    }

    super.checkInitializers(context);
  }

  public void compileStaticInitializer(CClassContext context)
    throws PositionedError
  {
    if (statInit != null) {
      statInit.checkInitializer(context);

      // check that all final class fields are initialized
      CField[]	classFields = context.getCClass().getFields();

      for (int i = 0; i < classFields.length; i++) {
	if (classFields[i].isStatic() && !CVariableInfo.isInitialized(context.getFieldInfo(i))) {
	  check(context,
		!classFields[i].isFinal(),
		KjcMessages.UNINITIALIZED_FINAL_FIELD,
		classFields[i].getIdent());

	  context.reportTrouble(new CWarning(getTokenReference(),
					     KjcMessages.UNINITIALIZED_FIELD,
					     classFields[i].getIdent()));
	}
      }

      // mark all static fields initialized
      self.markAllFieldToInitialized(true);
    }
  }

  /**
   * checkTypeBody
   * Check expression and evaluate and alter context
   * @param context the actual context of analyse
   * @return  a pure java expression including promote node
   * @exception	PositionedError	an error with reference to the source file
   */
  public void checkTypeBody(CContext context) throws PositionedError {
    if (getCClass().isNested() && getOwner().getCClass().isClass() 
        && !getCClass().isStatic() && !context.isStaticContext()) {
      addOuterThis();
    }
    try {
      CVariableInfo	instanceInfo = null;
      CVariableInfo[]	constructorsInfo;

      if (instanceInit != null) {
	instanceInit.checkInitializer(self);
      }

      for (int i = fields.length - 1; i >= 0 ; i--) {
	((CSourceField)fields[i].getField()).setFullyDeclared(true);
      }

      for (int i = 0; i < inners.length; i++) {
	try {
	  inners[i].checkTypeBody(self);
	} catch (CBlockError e) {
	  context.reportTrouble(e);
	}
      }

      // First we compile constructors
      constructorsInfo = compileConstructors(context);

      // Now we compile methods
      for (int i = methods.length - 1; i >= 0 ; i--) {
	try {
	  if (!(methods[i] instanceof JConstructorDeclaration)) {
	    methods[i].checkBody1(self);
	  }
	} catch (CBlockError e) {
	  context.reportTrouble(e);
	}
      }

      // Now we check members
      for (int i = methods.length - 1; i >= 0 ; i--) {
	if (!((CSourceMethod)methods[i].getMethod()).isUsed() &&
	    !methods[i].getMethod().getIdent().equals(JAV_CONSTRUCTOR)) {
	  context.reportTrouble(new CWarning( methods[i].getTokenReference(),
					     KjcMessages.UNUSED_PRIVATE_METHOD,
					     methods[i].getMethod().getIdent()));
	}
      }
      for (int i = fields.length - 1; i >= 0 ; i--) {
	if (!((CSourceField)fields[i].getField()).isUsed()) {
	  context.reportTrouble(new CWarning(fields[i].getTokenReference(),
					     KjcMessages.UNUSED_PRIVATE_FIELD,
					     fields[i].getField().getIdent()));
	}
      }

      self.close(this, null, null, null);

      super.checkTypeBody(context);

    } catch (UnpositionedError cue) {
      throw cue.addPosition(getTokenReference());
    }

    self = null;
  }

  /**
   * Compiles the constructors of this class.
   *
   * @param	context		the analysis context
   * @return	the variable state after each constructor
   * @exception	PositionedError	an error with reference to the source file
   */
  private CVariableInfo[] compileConstructors(CContext context)
    throws PositionedError
  {
    JMethodDeclaration[]	constructors;
    CVariableInfo[]		variableInfos;

    // ------------------------------------------------------------------
    // create an array containing the constructors

    if (getDefaultConstructor() != null) {
      // if there is a default constructor, there are no other constructors.
      constructors = new JMethodDeclaration[1];
      constructors[0] = getDefaultConstructor();
    } else {
      int		count;

      // count the number of constructors ...
      count = 0;
      for (int i = 0; i < methods.length; i++) {
	if (methods[i] instanceof JConstructorDeclaration) {
	  count += 1;
	}
      }
      // ... and put them into an array
      constructors = new JMethodDeclaration[count];
      count = 0;
      for (int i = 0; i < methods.length; i++) {
	if (methods[i] instanceof JConstructorDeclaration) {
	  constructors[count] = methods[i];
	  count += 1;
	}
      }
    }

    // ------------------------------------------------------------------
    // compile each constructor

    variableInfos = new CVariableInfo[constructors.length];
    for (int i = 0; i < constructors.length; i++) {
      try {
	constructors[i].checkBody1(self);
      } catch (CBlockError e) {
	context.reportTrouble(e);
      }
    }

    // ------------------------------------------------------------------
    // mark all instance fields initialized

    self.markAllFieldToInitialized(false);

    // ------------------------------------------------------------------
    // check for cycles in constructor calls

    CMethod[]			callers;
    CMethod[]			callees;

    callers = new CMethod[constructors.length];
    callees = new CMethod[constructors.length];

    for (int i = 0; i < constructors.length; i++) {
      callers[i] = constructors[i].getMethod();
      callees[i] = ((JConstructorDeclaration)constructors[i]).getCalledConstructor();
    }

    for (int i = 0; i < constructors.length; i++) {
      if (callees[i] != null) {
	boolean		found = false;

	for (int j = 0; !found && j < constructors.length; j++) {
	  if (callees[i].equals(callers[j])) {
	    found = true;
	  }
	}
	if (! found) {
	  callees[i] = null;
	}
      }
    }

  _scan_:
    for (;;) {
      for (int i = 0; i < constructors.length; i++) {
	// find the first constructor that does not call
	// another constructor : it cannot be part of a
	// cycle
	if (callers[i] != null && callees[i] == null) {
	  // remove it as successor
	  for (int j = 0; j < constructors.length; j++) {
	    if (j != i && callees[j] != null && callees[j].equals(callers[i])) {
	      callees[j] = null;
	    }
	  }

	  // remove it
	  callers[i] = null;

	  // start search again
	  continue _scan_;
	}
      }

      // if we come here, nothing has been done
      break _scan_;
    }

    // if there are remaining constructors, they are part of a cycle
    for (int i = 0; i < constructors.length; i++) {
      if (callers[i] != null) {
	context.reportTrouble(new PositionedError(constructors[i].getTokenReference(),
						  KjcMessages.CYCLE_IN_CONSTRUCTOR_CALL));
	// signal only one
	break;
      }
    }

    return variableInfos;
  }

  /**
   * Constructs the default constructor with no arguments.
   */
  private JConstructorDeclaration constructDefaultConstructor() {
    int         modifier;
    CClass      owner = getCClass();

    if (owner.isPublic()) {
      /* JLS 8.8.7 : If the class is declared public, then the default constructor 
         is implicitly given the access modifier public (§6.6); */
      modifier = ACC_PUBLIC;
    } else if (owner.isProtected()) {
      /* JLS 8.8.7 : If the class is declared protected, then the default 
         constructor is implicitly given the access modifier protected (§6.6); */
      modifier = ACC_PROTECTED;
    } else if (owner.isPrivate()) {
      /* JLS 8.8.7 : If the class is declared private, then the default constructor is 
         implicitly given the access modifier private (§6.6);*/
      modifier = ACC_PRIVATE;
    } else {
      /* JLS 8.8.7 : otherwise, the default constructor has the default 
         access implied by no  access modifier. */
      modifier = 0;
    }    
    return new JConstructorDeclaration(getTokenReference(),
				       modifier,
				       ident,
				       JFormalParameter.EMPTY,
				       CClassType.EMPTY,
				       null,
				       new JStatement[0],
				       null,
				       null);
  }

  /**
   * Collects all initializers and builds a single method.
   * @param	isStatic	class or instance initializers ?
   */
  private JInitializerDeclaration constructInitializers(boolean isStatic) {
    Vector		elems = new Vector();
    boolean		needGen = false;

    for (int i = 0; i < body.length; i++) {
      if ((body[i] instanceof JClassBlock)
	  && (((JClassBlock)body[i]).isStaticInitializer() == isStatic)) {
	elems.addElement(body[i]);
	needGen = true;
      } else {
	if ((body[i] instanceof JFieldDeclaration)
	    && (((JFieldDeclaration)body[i]).getVariable().isStatic() == isStatic)) {
	  needGen |= ((JFieldDeclaration)body[i]).needInitialization();
	  elems.addElement(new JClassFieldDeclarator(getTokenReference(), (JFieldDeclaration)body[i]));
	}
      }
    }

    if (elems.size() > 0) {
      JStatement[]	stmts = (JStatement[])Utils.toArray(elems, JStatement.class);

      return new JInitializerDeclaration(getTokenReference(),
					 new JClassBlock(getTokenReference(), false, stmts),
					 isStatic,
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

    p.visitClassDeclaration(this,
			    modifiers,
			    ident,
			    superName,
			    interfaces,
			    body,
			    fields,
			    methods,
			    inners);
  }

 /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
   Object trash = super.accept(p);
   return p.visitClassDeclaration(this,
				  modifiers,
				  ident,
				  superName,
				  interfaces,
				  body,
				  fields,
				  methods,
				  inners);
  }


  /**
   * Generate the code in pure java form
   * It is useful to debug and tune compilation process
   * @param	p		the printwriter into the code is generated
   */
  public void genInnerJavaCode(KjcPrettyPrinter p) {
    super.accept(p);

    p.visitInnerClassDeclaration(this,
				 modifiers,
				 ident,
				 superName,
				 interfaces,
				 inners,
				 body,
				 fields,
				 methods);
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

  private CClassType		superClass;
  private CClassContext		self;
  protected String		superName;

    // bft:  added for streamit passes
    public static final JClassDeclaration[] EMPTY = new JClassDeclaration[0];
    
}

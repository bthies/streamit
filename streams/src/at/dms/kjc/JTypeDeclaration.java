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
 * $Id: JTypeDeclaration.java,v 1.5 2003-05-16 21:58:36 thies Exp $
 */

package at.dms.kjc;

import java.util.Hashtable;
import at.dms.compiler.Compiler;
import at.dms.compiler.CWarning;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.PositionedError;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;

/**
 * This class represents a Java class or interface declaration
 * in the syntax tree.
 */
public abstract class JTypeDeclaration extends JMemberDeclaration {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JTypeDeclaration() {} // for cloner only

  /**
   * Constructs a type declaration node in the syntax tree.
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
  public JTypeDeclaration(TokenReference where,
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
    super(where, javadoc, comment);

    this.modifiers = modifiers;
    this.ident = ident.intern();
    this.interfaces = interfaces;
    this.fields = fields;
    this.methods = methods;
    this.inners = inners;
    this.body = initializers;
  }

  /**
   * Defines an intermediate external representation of this class to use internally
   *
   */
  public void generateInterface(CClass owner, String prefix) {
    sourceClass = new CSourceClass(owner, getTokenReference(), modifiers, ident, prefix + ident, isDeprecated());

    setInterface(sourceClass);

    CClassType[]	innerClasses = new CClassType[inners.length];
    for (int i = 0; i < inners.length; i++) {
      inners[i].generateInterface(sourceClass, sourceClass.getQualifiedName() + "$");
      innerClasses[i] = inners[i].getCClass().getType();
    }

    sourceClass.setInnerClasses(innerClasses); // prevent interface
    uniqueSourceClass = CTopLevel.addSourceClass(sourceClass);
  }

  // ----------------------------------------------------------------------
  // ACCESSORS
  // ----------------------------------------------------------------------

  /**
   * Returns the declared modifiers for this type.
   */
  public int getModifiers() {
    return modifiers;
  }

  /**
   * Sets the declared modifiers for this type.
   */
  public void setModifiers(int modifiers) {
    this.modifiers = modifiers;
    if (sourceClass != null) {
      sourceClass.setModifiers(modifiers);
    }
  }

  /**
   * Adds <method> to this.
   */
    public void addMethod(JMethodDeclaration method) {
	// make new array
	JMethodDeclaration[] newMethods = 
	    new JMethodDeclaration[methods.length + 1];
	// add extra method
	newMethods[0] = method;
	// copy old into new
	for (int i=0; i<methods.length; i++) {
	    newMethods[i+1] = methods[i];
	}
	// set old to new
	methods = newMethods;
    }

  /**
   * Checks whether this type is nested.
   *
   * JLS 8 (introduction), JLS 9 (introduction) :
   * A nested type (class or interface) is any type whose declaration
   * occurs within the body of another class or interface. A top level
   * type is a type that is not a nested class.
   *
   * @return	true iff this type is nested
   */
  public boolean isNested() {
    return getCClass().isNested();
  }

    public JFieldDeclaration[] getFields() {
	return fields;
    }

    public JMethodDeclaration[] getMethods() {
	return methods;
    }

  public JConstructorDeclaration getDefaultConstructor() {
    return defaultConstructor;
  }

  public void setDefaultConstructor(JConstructorDeclaration defaultConstructor) {
    this.defaultConstructor = defaultConstructor;
  }

  public void setIdent(String ident) {
    this.ident = ident;
  }

    public String getIdent() {
	return ident;
    }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * @exception	PositionedError	an error with reference to the source file
   */
  public abstract void checkInterface(CContext context) throws PositionedError;

  /**
   * Second pass (quick), check interface looks good
   * @exception	PositionedError	an error with reference to the source file
   */
  protected void checkInterface(CContext context, CClassType superClass) throws PositionedError {
    CClassContext	self = new CClassContext(context, sourceClass, this);
    Hashtable		hashField;
    CMethod[]		methodList;
    Hashtable		hashMethod;

    if (!uniqueSourceClass) {
      context.reportTrouble(new PositionedError(getTokenReference(),
						KjcMessages.DUPLICATE_TYPE_NAME,
						sourceClass.getQualifiedName()));
    }

    if (!isNested() && sourceClass.isPublic() && !getTokenReference().getName().startsWith(ident + ".")) {
      context.reportTrouble(new PositionedError(getTokenReference(),
						KjcMessages.CLASS_NAME_FILENAME,
						ident, getTokenReference().getName()));
    }

    // resolve interfaces
    for (int i = 0; i < interfaces.length; i++) {
      try {
	interfaces[i].checkType(self);
      } catch (UnpositionedError e) {
	throw e.addPosition(getTokenReference());
      }

      CClass	clazz = interfaces[i].getCClass();

      check(context,
	    clazz.isInterface(),
	    KjcMessages.SUPERINTERFACE_WRONG_TYPE,
	    interfaces[i].getQualifiedName());

      check(context,
	    clazz.isAccessible(sourceClass),//isPublic() || sourceClass.getPackage() == clazz.getPackage(),
	    KjcMessages.SUPERINTERFACE_NOT_ACCESSIBLE,
	    interfaces[i].getQualifiedName());
    }
    // If the class is an inner class, add field for this-reference.

    // Add fields of this class
    int		generatedFields = getCClass().hasOuterThis() ? 1 : 0;
    hashField = new Hashtable(fields.length + generatedFields + 1);
    for (int i = fields.length - 1; i >= 0 ; i--) {
      CSourceField	field = fields[i].checkInterface(self);

      field.setPosition(i);
      check(context,
	    hashField.put(field.getIdent(), field) == null,
	    KjcMessages.FIELD_RENAME, field.getIdent());
    }
    if (generatedFields > 0) {
      CSourceField	field = outerThis.checkInterface(self);

      field.setPosition(hashField.size());
      check(context,
	    hashField.put(JAV_OUTER_THIS, field) == null,
	    KjcMessages.FIELD_RENAME, JAV_OUTER_THIS);
    }

    // Add methods of this class
    int		generatedMethods = 0;

    if (defaultConstructor != null) {
      generatedMethods += 1;
    }
    if (statInit != null && !statInit.isDummy()) {
      generatedMethods += 1;
    }
    if (instanceInit != null && !instanceInit.isDummy()) {
      generatedMethods += 1;
    }

    methodList = new CMethod[methods.length + generatedMethods];
    for (int i = 0; i < methods.length; i++) {
      methodList[i] = methods[i].checkInterface(self);
      for (int j = 0; j < i; j++) {
	check(context,
	      ! methodList[i].equals(methodList[j]),
	      KjcMessages.METHOD_REDEFINE, methodList[i]);
      }
    }

    int		count = methods.length;
    if (defaultConstructor != null) {
      methodList[count] = defaultConstructor.checkInterface(self);
      count++;
    }
    if (statInit != null) {
      if (!statInit.isDummy()) {
	methodList[count] = statInit.checkInterface(self);
	count++;
      } else {
	statInit.checkInterface(self);
      }
    }
    if (instanceInit != null) {
      if (!instanceInit.isDummy()) {
	methodList[count] = instanceInit.checkInterface(self);
	count++;
      } else {
	instanceInit.checkInterface(self);
      }
    }

    // Check inners
    for (int i = inners.length - 1; i >= 0 ; i--) {
      inners[i].checkInterface(self);
    }

    sourceClass.close(this.interfaces, superClass, hashField, methodList);
  }

  /**
   * Checks that same interface is not specified more than once
   *
   * @exception	PositionedError	Error catched as soon as possible
   */
  public void checkInitializers(CContext context) throws PositionedError {
    for (int i = 0; i < interfaces.length; i++) {
      for (int j = 0; j < i; j++) {
	check(context,
	      !interfaces[i].equals(interfaces[j]),
	      KjcMessages.INTERFACES_DUPLICATE, ident, interfaces[i]);
      }
    }

    // Checks that specified interfaces are not inherited
    for (int i = 0; i < interfaces.length; i++) {
      CClass	parent;

      parent = sourceClass.getSuperClass();
      if (parent != null && parent.descendsFrom(interfaces[i].getCClass())) {
	context.reportTrouble(new CWarning(getTokenReference(),
					   KjcMessages.INTERFACE_IMPLEMENTED_BY_SUPERCLASS,
					   interfaces[i], parent.getIdent()));
      }


      for (int j = 0; j < interfaces.length; j++) {
	if (j != i &&  interfaces[j].getCClass().descendsFrom(interfaces[i].getCClass())) {
	  context.reportTrouble(new CWarning(getTokenReference(),
					     KjcMessages.INTERFACE_IMPLEMENTED_BY_SUPERCLASS,
					     interfaces[i], interfaces[j]));
	}
      }
    }
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   */
  public void addOuterThis() {
    if (outerThis == null) {
      sourceClass.setHasOuterThis(true);
      outerThis = new JFieldDeclaration(getTokenReference(),
					new JVariableDefinition(getTokenReference(),
								ACC_PRIVATE | ACC_FINAL,
								getOwner().getCClass().getType(),
								JAV_OUTER_THIS,
								null),
					null,
					null);
      ((CSourceClass)getCClass()).addField(new CSourceField(getCClass(),
							    ACC_PRIVATE | ACC_FINAL,
							    JAV_OUTER_THIS,
							    getOwner().getCClass().getType(),
							    false));
    }
  }

  /**
   * checkTypeBody
   * Check expression and evaluate and alter context
   * @param context the actual context of analyse
   * @return  a pure java expression including promote node
   * @exception PositionedError Error catched as soon as possible (for subclasses)
   */
  public void checkTypeBody(CContext context) throws PositionedError {
    context.addSourceClass(sourceClass);
  }

  // ----------------------------------------------------------------------
  // PROTECTED UTILITIES
  // ----------------------------------------------------------------------

  public CClass getOwner() {
    return getCClass().getOwner();
  }


  // ----------------------------------------------------------------------
  // PRIVATE DATA MEMBER
  // ----------------------------------------------------------------------

  protected	int			modifiers;
  protected	String			ident;

  protected	JPhylum[]		body;
  protected	JFieldDeclaration[]	fields;
  protected	JMethodDeclaration[]	methods;
  protected	JTypeDeclaration[]	inners;
  protected	CClassType[]		interfaces;

  private	JFieldDeclaration	outerThis;
  private	JConstructorDeclaration defaultConstructor;
  protected	JInitializerDeclaration	statInit;
  protected	JInitializerDeclaration	instanceInit;

  // Definitive data
  protected	CSourceClass		sourceClass;
  private	boolean			uniqueSourceClass = true;
}


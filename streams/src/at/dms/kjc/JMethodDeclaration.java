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
 * $Id: JMethodDeclaration.java,v 1.15 2003-05-16 21:58:35 thies Exp $
 */

package at.dms.kjc;

import at.dms.compiler.PositionedError;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;

import java.util.ListIterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents a Java method declaration in the syntax tree.
 */
public class JMethodDeclaration extends JMemberDeclaration {

  // ----------------------------------------------------------------------
  // CONSTRUCTORS
  // ----------------------------------------------------------------------

    protected JMethodDeclaration() {} // for cloner only

  /**
   * Constructs a method declaration node in the syntax tree.
   *
   * @param	where		the line of this node in the source code
   * @param	modifiers	the list of modifiers of the method
   * @param	returnType	the return type of the method
   * @param	ident		the name of the method
   * @param	parameters	the parameters of the method
   * @param	exceptions	the exceptions declared by the method
   * @param	body		the body of the method
   * @param	javadoc		java documentation comments
   * @param	comment		other comments in the source code
   */
  public JMethodDeclaration(TokenReference where,
			    int modifiers,
			    CType returnType,
			    String ident,
			    JFormalParameter[] parameters,
			    CClassType[] exceptions,
			    JBlock body,
			    JavadocComment javadoc,
			    JavaStyleComment[] comments)
  {
    super(where, javadoc, comments);

    this.modifiers = modifiers;
    this.returnType = returnType;
    this.ident = ident.intern();
    this.body = body;

    this.parameters = parameters;
    this.exceptions = exceptions;
    assert(parameters != null);
    assert(exceptions != null);
  }

    /**
     * Set method name to <name>
     */
    public void setName(String str) {
	this.ident = str;
    }

    /**
     * Return identifier of this method.
     */
    public String getName() {
	return ident;
    }

    /**
     * Inserts <param> as the first parameter of this.
     */
    public void addParameter(JFormalParameter param) {
	// make new parameter list
	JFormalParameter newp[] = new JFormalParameter[parameters.length+1];
	// insert new one
	newp[0] = param;
	// copy over the old ones
	for (int i=0; i<parameters.length; i++) {
	    newp[i+1] = parameters[i];
	}
	// set parameters to be new parameters
	parameters = newp;
    }

    /**
     * Adds <statement> to the end of the statements in this.
     */
    public void addStatement(JStatement statement) {
	body.addStatement(statement);
    }

    /**
     * Adds <statement> to the end of the statements in this.
     */
    public void addStatementFirst(JStatement statement) {
	body.addStatementFirst(statement);
    }
   
    /**
     * Adds all statements in <lst> to this, at the specified position.
     */
    public void addAllStatements(int pos, List lst) {
	body.addAllStatements(pos, lst);
    }

    /**
     * Adds all statements in <lst> to end of this.
     */
    public void addAllStatements(List lst) {
	body.addAllStatements(lst);
    }

  // ----------------------------------------------------------------------
  // INTERFACE CHECKING
  // ----------------------------------------------------------------------

  /**
   * Second pass (quick), check interface looks good
   * Exceptions are not allowed here, this pass is just a tuning
   * pass in order to create informations about exported elements
   * such as Classes, Interfaces, Methods, Constructors and Fields
   * @return true iff sub tree is correct enough to check code
   * @exception	PositionedError	an error with reference to the source file
   */
  public CSourceMethod checkInterface(CClassContext context) throws PositionedError {
    boolean	inInterface = context.getCClass().isInterface();
    boolean	isExported = !(this instanceof JInitializerDeclaration);
    String	ident = (this instanceof JConstructorDeclaration) ? JAV_CONSTRUCTOR : this.ident;

    // Collect all parsed data
    if (inInterface && isExported) {
      modifiers |= ACC_PUBLIC | ACC_ABSTRACT;
    }

    // 8.4.3 Method Modifiers
    check(context,
	  CModifier.isSubsetOf(modifiers,
			       ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE
			       | ACC_ABSTRACT | ACC_FINAL | ACC_STATIC
			       | ACC_NATIVE | ACC_SYNCHRONIZED | ACC_STRICT),
	  KjcMessages.METHOD_FLAGS);

    if (inInterface && isExported) {
      check(context,
	    CModifier.isSubsetOf(modifiers, ACC_PUBLIC | ACC_ABSTRACT),
	    KjcMessages.METHOD_FLAGS_IN_INTERFACE, this.ident);
    }

    try {
      returnType.checkType(context);

      CType[]	parameterTypes = new CType[parameters.length];
      for (int i = 0; i < parameterTypes.length; i++) {
	parameterTypes[i] = parameters[i].checkInterface(context);
      }

      for (int i = 0; i < exceptions.length; i++) {
	exceptions[i].checkType(context);
      }

      setInterface(new CSourceMethod(context.getCClass(),
				     modifiers,
				     ident,
				     returnType,
				     parameterTypes,
				     exceptions,
				     isDeprecated(),
				     body));

      return (CSourceMethod)getMethod();
    } catch (UnpositionedError cue) {
      throw cue.addPosition(getTokenReference());
    }
  }

  // ----------------------------------------------------------------------
  // SEMANTIC ANALYSIS
  // ----------------------------------------------------------------------

  /**
   * Check expression and evaluate and alter context
   * @param context the actual context of analyse
   * @return  a pure java expression including promote node
   * @exception PositionedError Error catched as soon as possible
   */
  public void checkBody1(CClassContext context) throws PositionedError {
    check(context,
	  context.getCClass().isAbstract() || !getMethod().isAbstract(),
	  KjcMessages.METHOD_ABSTRACT_CLASSNOT, ident);

    checkOverriding(context);

    if (body == null) {
      check(context,
	    getMethod().isAbstract()
	    || getMethod().isNative()
	    || context.getClassContext().getCClass().isInterface(),
	    KjcMessages.METHOD_NOBODY_NOABSTRACT, ident);
    } else {
      check(context,
	    !context.getCClass().isInterface(),
	    KjcMessages.METHOD_BODY_IN_INTERFACE, ident);

      check(context,
	    !getMethod().isNative() && !getMethod().isAbstract(),
	    KjcMessages.METHOD_BODY_NATIVE_ABSTRACT, ident);

      CMethodContext	self = new CMethodContext(context, getMethod());
      CBlockContext	block = new CBlockContext(self, parameters.length);

      if (!getMethod().isStatic()) {
	// add this local var
	block.addThisVariable();
      }

      for (int i = 0; i < parameters.length; i++) {
	parameters[i].analyse(block);
      }

      body.analyse(block);

      block.close(getTokenReference());
      self.close(getTokenReference());

      if (block.isReachable() && getMethod().getReturnType() != CStdType.Void) {
	context.reportTrouble(new CLineError(getTokenReference(),
					     KjcMessages.METHOD_NEED_RETURN,
					     getMethod().getIdent()));
      }
    }
  }

  /**
   * Checks that overriding/hiding is correct.
   *
   * @param	context		the analysis context
   * @exception	PositionedError	the analysis detected an error
   */
  private void checkOverriding(CClassContext context) throws PositionedError {
    CMethod[]	superMethods;

    try {
      superMethods = context.getCClass().lookupSuperMethod(ident, getMethod().getParameters());
    } catch (UnpositionedError ce) {
      throw ce.addPosition(getTokenReference());
    }

    for (int i = 0; i < superMethods.length; i++) {
      if (!superMethods[i].isPrivate()
	  && getMethod().hasSameSignature(superMethods[i])) {
	checkOverriding(context, superMethods[i]);
      }
    }
  }

  private void checkOverriding(CClassContext context, CMethod superMethod)
    throws PositionedError
  {
    CMethod	thisMethod = getMethod();

    try {
      thisMethod.checkOverriding(superMethod);
    } catch (UnpositionedError ce) {
      throw ce.addPosition(getTokenReference());
    }

//     // JLS 8.4.3.3 :
//     // A method can be declared final to prevent subclasses from overriding
//     // or hiding it. It is a compile-time error to attempt to override or
//     // hide a final method.
//     check(context,
// 	  !superMethod.isFinal(),
// 	  KjcMessages.METHOD_OVERRIDE_FINAL,
// 	  thisMethod);

//     // JLS 8.4.6.1 :
//     // A compile-time error occurs if an instance method overrides a
//     // static method.
//     check(context,
// 	  ! (!thisMethod.isStatic() && superMethod.isStatic()),
// 	  KjcMessages.METHOD_INSTANCE_OVERRIDES_STATIC,
// 	  thisMethod, superMethod.getOwner());

//     // JLS 8.4.6.2 :
//     // A compile-time error occurs if a static method hides an instance method.
//     check(context,
// 	  ! (thisMethod.isStatic() && !superMethod.isStatic()),
// 	  KjcMessages.METHOD_STATIC_HIDES_INSTANCE,
// 	  thisMethod, superMethod.getOwner());

//     // JLS 8.4.6.3 :
//     // If a method declaration overrides or hides the declaration of another
//     // method, then a compile-time error occurs if they have different return
//     // types or if one has a return type and the other is void.
//     check(context,
// 	  returnType.equals(superMethod.getReturnType()),
// 	  KjcMessages.METHOD_RETURN_DIFFERENT, thisMethod);

//     // JLS 8.4.6.3 :
//     // The access modifier of an overriding or hiding method must provide at
//     // least as much access as the overridden or hidden method.
//     boolean	moreRestrictive;

//     if (superMethod.isPublic()) {
//       moreRestrictive = !thisMethod.isPublic();
//     } else if (superMethod.isProtected()) {
//       moreRestrictive = !(thisMethod.isProtected() || thisMethod.isPublic());
//     } else if (! superMethod.isPrivate()) {
//       // default access
//       moreRestrictive = thisMethod.isPrivate();
//     } else {
//       // a private method is not inherited
//       throw new InconsistencyException("bad access: " + superMethod.getModifiers());
//     }

//     check(context,
// 	  !moreRestrictive,
// 	  KjcMessages.METHOD_ACCESS_DIFFERENT,
// 	  thisMethod, superMethod.getOwner());

//     // JLS 8.4.4 :
//     // A method that overrides or hides another method, including methods that
//     // implement abstract methods defined in interfaces, may not be declared to
//     // throw more CHECKED exceptions than the overridden or hidden method.
//     CClassType[]	exc = superMethod.getThrowables();

//   _loop_:
//     for (int i = 0; i < exceptions.length; i++) {
//       if (exceptions[i].isCheckedException()) {
// 	for (int j = 0; j < exc.length; j++) {
// 	  if (exceptions[i].isAssignableTo(exc[j])) {
// 	    continue _loop_;
// 	  }
// 	}
// 	check(context,
// 	      false,
// 	      KjcMessages.METHOD_THROWS_DIFFERENT, thisMethod, exceptions[i]);
//       }
//     }
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

    p.visitMethodDeclaration(this,
			     modifiers,
			     returnType,
			     ident,
			     parameters,
			     exceptions,
		     body);
  }


     /**
   * Accepts the specified attribute visitor
   * @param	p		the visitor
   */
  public Object accept(AttributeVisitor p) {
     Object trash =  super.accept(p);

    return p.visitMethodDeclaration(this,
			     modifiers,
			     returnType,
			     ident,
			     parameters,
			     exceptions,
			     body);
  }




  /**
   * Generates a sequence of bytescodes
   * @param	code		the code list
   */
  public void genCode(CodeSequence code) {
    throw new InconsistencyException(); // nothing to do here
  }

  // ----------------------------------------------------------------------
  // DATA MEMBERS
  // ----------------------------------------------------------------------

    /**
     * Returns iterator of statements in this.  
     */
    public ListIterator getStatementIterator() {
	return body.getStatementIterator();
    }

    /**
     * Returns the body of this.
     */
    public JBlock getBody() {
	return body;
    }

    /**
     * Sets the body of this
     */
    public void setBody(JBlock body) {
	this.body = body;
    }
    

    /**
     * Returns list of statements in this.  
     */
    public List getStatements() {
	return body.getStatements();
    }

    /**
     * Gets parameters of this.
     */
    public JFormalParameter[] getParameters() {
	return parameters;
    }

    public void setParameters(JFormalParameter[] param) {
	parameters=param;
    }

    // need a different method array for every method in case people
    // start to add methods; can't just have a constant.
    public static JMethodDeclaration[] EMPTY() {
	return new JMethodDeclaration[0];
    }

    public String toString() {
	return "JMethodDeclaration, ident=" + ident;
    }

  // $$$ MOVE TO BE PRIVATE
  protected int				modifiers;
  protected CType			returnType;
  protected String			ident;
  protected JFormalParameter[]		parameters;
  protected CClassType[]		exceptions;
  protected JBlock			body;
}




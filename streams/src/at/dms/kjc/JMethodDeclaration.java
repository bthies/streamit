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
 * $Id: JMethodDeclaration.java,v 1.35 2009-02-05 20:28:51 ctan Exp $
 */

package at.dms.kjc;

import at.dms.util.Utils;
import at.dms.compiler.PositionedError;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.JavadocComment;
import at.dms.compiler.TokenReference;
import at.dms.compiler.UnpositionedError;
import at.dms.util.InconsistencyException;
import at.dms.kjc.sir.SIRDynamicRateManager;

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

    // Only for cloner.  Should not be used as a real method decl.
    // If you need a placeholder Method declaration, use
    // JMethodDeclaration(String) below.
    protected JMethodDeclaration() {
        initIORates();
        // to prevent null points in cloning visitor
        this.parameters = JFormalParameter.EMPTY;
        this.exceptions = CClassType.EMPTY;
        this.body = new JBlock();
        this.returnType = CStdType.Void;
        this.ident = "Method made by cloner";
    }

    /**
     * Dummy JMethodDeclarations should now include comment...
     * (mostly Debugging aide)
     *
     * Replaces new JMethodDeclaration() in code since
     * the constructor with 0 parameters is reserved for the cloner.
     * Sets all io rates to 0, parameters to EMPTY, exceptions to EMPTY
     * body to the passed comment as a block, returnType to Void.
     *
     * @param  comment is for use in debugging Since this comment
     *         is often lost in making copies, it is included
     *         in C / C++ comment form in the ident.
     */

    public JMethodDeclaration (String comment) {
        initIORates();
        this.returnType = CStdType.Void;
        this.parameters = JFormalParameter.EMPTY;
        this.exceptions = CClassType.EMPTY;
        JavaStyleComment[] comments = new JavaStyleComment[1];
        comments[0]= new JavaStyleComment(comment,false,false,false);
        this.body = new JBlock(/*where*/null, new LinkedList(), comments);
        this.ident = "/* '" + comment + "' */ DUMMY UNINITIALIZED METHOD";
    }

    
    /**
     * Constructs a method declaration node in the syntax tree.
     *
     * @param   where       the line of this node in the source code
     * @param   modifiers   the list of modifiers of the method
     * @param   returnType  the return type of the method
     * @param   ident       the name of the method
     * @param   parameters  the parameters of the method
     * @param   exceptions  the exceptions declared by the method
     * @param   body        the body of the method
     * @param   javadoc     java documentation comments
     * @param   comments     other comments in the source code
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
        assert parameters != null;
        assert exceptions != null;

        initIORates();
    }

    public JMethodDeclaration(CType returnType,
                              String ident,
                              JFormalParameter[] parameters,
                              JBlock body) {
        this(null, at.dms.kjc.Constants.ACC_PUBLIC, returnType, ident, parameters, CClassType.EMPTY, body, null, null);
    }

    /**
     * Set method name to <pre>name</pre>
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
     * Return ctype return type of this method.
     */
    public CType getReturnType() {
        return returnType;
    }
    
    public void setReturnType(CType returnType) {
        this.returnType = returnType;
    }

    /**
     * Inserts <pre>param</pre> as the first parameter of this.
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
     * Adds <pre>statement</pre> to the end of the statements in this.
     */
    public void addStatement(JStatement statement) {
        body.addStatement(statement);
    }

    /**
     * Adds <pre>statement</pre> to the end of the statements in this.
     */
    public void addStatementFirst(JStatement statement) {
        body.addStatementFirst(statement);
    }
   
    /**
     * Adds all statements in <pre>lst</pre> to this, at the specified position.
     */
    public void addAllStatements(int pos, List lst) {
        body.addAllStatements(pos, lst);
    }

    /**
     * Adds all statements in <pre>lst</pre> to end of this.
     */
    public void addAllStatements(List lst) {
        body.addAllStatements(lst);
    }

    // ----------------------------------------------------------------------
    // StreamIt part
    // ----------------------------------------------------------------------

    /**
     * The number of items that are peeked in each execution.
     */
    private JExpression peek;
    /**
     * The number of items that are popped in each execution.
     */
    private JExpression pop;
    /**
     * The number of items that are pushed in each execution.
     */
    private JExpression push;

    private void initIORates() {
        this.peek = new JIntLiteral(0);
        this.push = new JIntLiteral(0);
        this.pop = new JIntLiteral(0);
    }

    /**
     * Returns whether this has a non-zero push, pop, or peek rate.
     */
    public boolean doesIO() {
        // always access via accessors
        JExpression myPush = getPush();
        JExpression myPeek = getPeek();
        JExpression myPop = getPop();
        // compute IO
        boolean noPush = (myPush instanceof JIntLiteral) && ((JIntLiteral)myPush).intValue()==0;
        boolean noPeek = (myPeek instanceof JIntLiteral) && ((JIntLiteral)myPeek).intValue()==0;
        boolean noPop = (myPop instanceof JIntLiteral) && ((JIntLiteral)myPop).intValue()==0;
        return !(noPush && noPeek && noPop);        
    }

    public void setPeek(JExpression p)
    {
        this.peek = p;
    }
    
    public void setPop(JExpression p)
    {
        this.pop = p;
    }
    
    public void setPush(JExpression p)
    {
        this.push = p;
    }
    
    public void setPeek(int p)
    {
        setPeek(new JIntLiteral(p));
    }
    
    public void setPop(int p)
    {
        setPop(new JIntLiteral(p));
    }
    
    public void setPush(int p)
    {
        setPush(new JIntLiteral(p));
    }
    
    public JExpression getPeek()
    {
        return SIRDynamicRateManager.interpretRate(this.peek);
    }
    
    public JExpression getPop()
    {
        return SIRDynamicRateManager.interpretRate(this.pop);
    }
    
    public JExpression getPush()
    {
        return SIRDynamicRateManager.interpretRate(this.push);
    }

    /**
     * Returns how many items are popped.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPop.
     */
    public int getPopInt() {
        // always access pop through getPop()
        JExpression myPop = getPop();
        if (pop instanceof JFloatLiteral) { //clleger
            myPop = new JIntLiteral(null, (int) ((JFloatLiteral)myPop).floatValue());
        }
        // need int literal to get number
        if (!(myPop instanceof JIntLiteral)) {
            Utils.fail("Trying to get integer value for myPop value in work function " + getName() + ", but the constant hasn't been resolved yet. " + myPop);
        }
        return ((JIntLiteral)myPop).intValue();
    }

    /**
     * Returns how many items are peeked.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPeek.
     */
    public int getPeekInt() {
        // always access peek through getPeek()
        JExpression myPeek = getPeek();
        if (myPeek instanceof JFloatLiteral) { //clleger
            myPeek = new JIntLiteral(null, (int) ((JFloatLiteral)myPeek).floatValue());
        }
        // need int literal to get number
        if (!(myPeek instanceof JIntLiteral)) {
            Utils.fail("Trying to get integer value for myPeek value in work function " + getName() + ", but the constant hasn't been resolved yet. " + myPeek);
        }
        return ((JIntLiteral)myPeek).intValue();
    }

    /**
     * Returns how many items are pushed.This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPush.
     */
    public int getPushInt() {
        // always access push through getPush()
        JExpression myPush = getPush();
        // need int literal to get number
        if (myPush instanceof JFloatLiteral) { //clleger
            myPush = new JIntLiteral(null, (int) ((JFloatLiteral)myPush).floatValue());
        }

        if (!(myPush instanceof JIntLiteral)) {
            Utils.fail("Trying to get integer value for myPush value in work function " + getName() + ", but the constant hasn't been resolved yet. " + myPush);
        }
        return ((JIntLiteral)myPush).intValue();
    }

    /**
     * Returns string representation of pop rate (either in literal or
     * range, like [1,2,3]).
     */
    public String getPopString() {
        JExpression myPop = getPop();
        if (myPop instanceof JLiteral) {
            return ""+getPopInt();
        } else {
            return myPop.toString();
        }
    }

    /**
     * Returns string representation of peek rate (either in literal or
     * range, like [1,2,3]).
     */
    public String getPeekString() {
        JExpression myPeek = getPeek();
        if (myPeek instanceof JLiteral) {
            return ""+getPeekInt();
        } else {
            return myPeek.toString();
        }
    }

    /**
     * Returns string representation of push rate (either in literal or
     * range, like [1,2,3]).
     */
    public String getPushString() {
        JExpression myPush = getPush();
        if (myPush instanceof JLiteral) {
            return ""+getPushInt();
        } else {
            return myPush.toString();
        }
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
     * @exception   PositionedError an error with reference to the source file
     */
    public CSourceMethod checkInterface(CClassContext context) throws PositionedError {
        boolean inInterface = context.getCClass().isInterface();
        boolean isExported = !(this instanceof JInitializerDeclaration);
        String  ident = (this instanceof JConstructorDeclaration) ? JAV_CONSTRUCTOR : this.ident;

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

            CType[] parameterTypes = new CType[parameters.length];
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

            CMethodContext  self = new CMethodContext(context, getMethod());
            CBlockContext   block = new CBlockContext(self, parameters.length);

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
     * @param   context     the analysis context
     * @exception   PositionedError the analysis detected an error
     */
    private void checkOverriding(CClassContext context) throws PositionedError {
        CMethod[]   superMethods;

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
        CMethod thisMethod = getMethod();

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
        //    !superMethod.isFinal(),
        //    KjcMessages.METHOD_OVERRIDE_FINAL,
        //    thisMethod);

        //     // JLS 8.4.6.1 :
        //     // A compile-time error occurs if an instance method overrides a
        //     // static method.
        //     check(context,
        //    ! (!thisMethod.isStatic() && superMethod.isStatic()),
        //    KjcMessages.METHOD_INSTANCE_OVERRIDES_STATIC,
        //    thisMethod, superMethod.getOwner());

        //     // JLS 8.4.6.2 :
        //     // A compile-time error occurs if a static method hides an instance method.
        //     check(context,
        //    ! (thisMethod.isStatic() && !superMethod.isStatic()),
        //    KjcMessages.METHOD_STATIC_HIDES_INSTANCE,
        //    thisMethod, superMethod.getOwner());

        //     // JLS 8.4.6.3 :
        //     // If a method declaration overrides or hides the declaration of another
        //     // method, then a compile-time error occurs if they have different return
        //     // types or if one has a return type and the other is void.
        //     check(context,
        //    returnType.equals(superMethod.getReturnType()),
        //    KjcMessages.METHOD_RETURN_DIFFERENT, thisMethod);

        //     // JLS 8.4.6.3 :
        //     // The access modifier of an overriding or hiding method must provide at
        //     // least as much access as the overridden or hidden method.
        //     boolean  moreRestrictive;

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
        //    !moreRestrictive,
        //    KjcMessages.METHOD_ACCESS_DIFFERENT,
        //    thisMethod, superMethod.getOwner());

        //     // JLS 8.4.4 :
        //     // A method that overrides or hides another method, including methods that
        //     // implement abstract methods defined in interfaces, may not be declared to
        //     // throw more CHECKED exceptions than the overridden or hidden method.
        //     CClassType[] exc = superMethod.getThrowables();

        //   _loop_:
        //     for (int i = 0; i < exceptions.length; i++) {
        //       if (exceptions[i].isCheckedException()) {
        //  for (int j = 0; j < exc.length; j++) {
        //    if (exceptions[i].isAssignableTo(exc[j])) {
        //      continue _loop_;
        //    }
        //  }
        //  check(context,
        //        false,
        //        KjcMessages.METHOD_THROWS_DIFFERENT, thisMethod, exceptions[i]);
        //       }
        //     }
    }

    // ----------------------------------------------------------------------
    // CODE GENERATION
    // ----------------------------------------------------------------------

    /**
     * Accepts the specified visitor
     * @param   p       the visitor
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
     * @param   p       the visitor
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
     * @param   code        the code list
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
    protected int               modifiers;
    protected CType         returnType;
    protected String            ident;
    protected JFormalParameter[]        parameters;
    protected CClassType[]      exceptions;
    protected JBlock            body;

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.JMethodDeclaration other = new at.dms.kjc.JMethodDeclaration();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.JMethodDeclaration other) {
        super.deepCloneInto(other);
        other.peek = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.peek);
        other.pop = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.pop);
        other.push = (at.dms.kjc.JExpression)at.dms.kjc.AutoCloner.cloneToplevel(this.push);
        other.modifiers = this.modifiers;
        other.returnType = (at.dms.kjc.CType)at.dms.kjc.AutoCloner.cloneToplevel(this.returnType);
        other.ident = (java.lang.String)at.dms.kjc.AutoCloner.cloneToplevel(this.ident);
        other.parameters = (at.dms.kjc.JFormalParameter[])at.dms.kjc.AutoCloner.cloneToplevel(this.parameters);
        other.exceptions = (at.dms.kjc.CClassType[])at.dms.kjc.AutoCloner.cloneToplevel(this.exceptions);
        other.body = (at.dms.kjc.JBlock)at.dms.kjc.AutoCloner.cloneToplevel(this.body);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}




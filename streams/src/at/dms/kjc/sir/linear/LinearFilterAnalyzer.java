package at.dms.kjc.sir.linear;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.linear.*;
import at.dms.kjc.iterator.*;


/**
 * The LinearFilterAnalyzer visits all of the Filter definitions in
 * a StreamIT program. It determines which filters calculate linear
 * functions of their inputs, and for those that do, it keeps a mapping from
 * the filter name to the filter's matrix representation.
 *
 * $Id: LinearFilterAnalyzer.java,v 1.8 2002-09-04 19:05:59 aalamb Exp $
 **/
public class LinearFilterAnalyzer extends EmptyStreamVisitor {
    /** Mapping from filters to linear forms. never would have guessed that, would you? **/
    HashMap filtersToLinearForm;
    
    /** use findLinearFilters to instantiate a LinearFilterAnalyzer **/
    private LinearFilterAnalyzer() {
	this.filtersToLinearForm = new HashMap();
	checkRep();
    }
    
    /**
     * Main entry point -- searches the passed stream for
     * linear filters and calculates their associated matricies.
     *
     * If the debug flag is set, then we print a lot of debugging information
     **/
    public static LinearFilterAnalyzer findLinearFilters(SIRStream str, boolean debug) {
	// set up the printer to either print or not depending on the debug flag
	LinearPrinter.setOutput(debug);
	LinearPrinter.println("aal--In linear filter visitor");
	LinearFilterAnalyzer lfv = new LinearFilterAnalyzer();
	IterFactory.createIter(str).accept(lfv);
	return lfv;
    }
    

    /** More or less get a callback for each stram **/
    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
	LinearPrinter.println("Visiting " + "(" + self + ")");

	// set up the visitor that will actually collect the data
	int peekRate = extractInteger(self.getPeek());
	int pushRate = extractInteger(self.getPush());
	LinearPrinter.println("  Peek rate: " + peekRate);
	LinearPrinter.println("  Push rate: " + pushRate);
	// if we have a peek or push rate of zero, this isn't a linear filter that we care about,
	// so only try and visit the filter if both are non-zero
	if ((peekRate != 0) && (pushRate != 0)) {
	    LinearFilterVisitor theVisitor = new LinearFilterVisitor(peekRate, pushRate);

	    // pump the visitor through the work function
	    // (we might need to send it thought the init function as well so that
	    //  we can determine the initial values of fields. However, I think that
	    //  field prop is supposed to take care of this.)
	    self.getWork().accept(theVisitor);

	    // print out the results of pumping the visitor
	    if (theVisitor.computesLinearFunction()) {
		LinearPrinter.println("Linear filter found: " + self +
				   "\n-->Matrix:\n" + theVisitor.getMatrixRepresentation() +
				   "\n-->Constant Vector:\n" + theVisitor.getConstantVector());
	    }
		
	} else {
	    LinearPrinter.println("  " + self.getIdent() + " is source/sink.");
	}
    }

    /** extract the actual value from a JExpression that is actually a literal... **/
    private static int extractInteger(JExpression expr) {
	if (expr == null) {throw new RuntimeException("null peek rate");}
	if (!(expr instanceof JIntLiteral)) {throw new RuntimeException("non integer peek rate");}
	JIntLiteral literal = (JIntLiteral)expr;
	return literal.intValue();
    }

    private void checkRep() {
	// make sure that all keys in FiltersToMatricies are strings, and that all
	// values are LinearForms.
	Iterator keyIter = this.filtersToLinearForm.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object o = keyIter.next();
	    if (!(o instanceof String)) {throw new RuntimeException("Non string key in LinearFilterAnalyzer");}
	    String key = (String)o;
	    Object val = this.filtersToLinearForm.get(key);
	    if (val == null) {throw new RuntimeException("Null value found in LinearFilterAnalyzer");}
	    if (!(val instanceof LinearForm)) {throw new RuntimeException("Non FilterMatric found in LinearFilterAnalyzer");}
	}
    }
}


// ----------------------------------------
// Code for visitor class.
// ----------------------------------------




/**
 * A visitor class that goes through all of the expressions in the work function
 * of a filter to determine if the filter is linear and if it is what matrix
 * corresponds to the filter.
 **/
class LinearFilterVisitor extends SLIREmptyAttributeVisitor {
    /**
     * Mappings from JLocalVariables and JFieldAccessExpressons
     * to LinearForms. Each LinearForm holds the
     * affine representation that maps the expression to a combination of
     * inputs (eg peek expressions indexes) and possibly a constant.
     **/
    private HashMap variablesToLinearForms;

    /**
     * number of items that are peeked at. therefore this is also the same
     * size of the vector that must be used to represent.
     **/
    private int peekSize;
    /**
     * Number of items that are pused. Therefore it also represents the
     * number of columns that are in the matrix representation.
     **/
    private int pushSize;

    /**
     * The current offset to add to a peeked value. Eg if we execute
     * <pre>peek(5); pop(); peek(5); </pre> the second peek
     * expression actually gets a different element, as you would expect.
     **/
    private int peekOffset;

    /**
     * The current push offset. This keeps track of which colimn in the Linear representation
     * of the current filter should be updated with the linear form.
     **/
    private int pushOffset;

    /**
     * Flag that is set when we detect that something blatently non-linear is
     * computed by the filter (eg <pre>push(peek(1)*peek(2);</pre>)
     * Starts off as false, and is set to true if we hit a statement that
     * makes the filter non-linear.
     **/
    private boolean nonLinearFlag;

    /** The matrix which represents this filter. **/
    private FilterMatrix representationMatrix;
    /**
     * A vector of offsets (eg constants that need to be added
     * to the combo of inputs to produce the output).
     **/
    private FilterVector representationVector;
    
    /**
     * Create a new LinearFilterVisitor which is looking to figure out 
     * how to compute, for all variables, linear forms from the input.
     * Also creates a LinearFilterRepresentation if the
     * filter computes a linear function.
     **/
    public LinearFilterVisitor(int numPeeks, int numPushes) {
	this.peekSize = numPeeks;
	this.pushSize = numPushes;
	this.variablesToLinearForms = new HashMap();
	this.peekOffset = 0;
	this.pushOffset = 0;
	this.representationMatrix = new FilterMatrix(numPeeks, numPushes);
	this.representationVector = new FilterVector(numPushes);
	this.nonLinearFlag = false;
	checkRep();

    }

    /** Returns true of the filter computes a linear function. **/
    public boolean computesLinearFunction() {
	// check the flag (which is set when we hit a non linear function in a push expression)
	// and check that we have seen the correct number of pushes.
	boolean enoughPushesSeen = (this.pushSize == this.pushOffset); // last push was to pushSize-1
	if (!(enoughPushesSeen)) {LinearPrinter.warn("Insufficient pushes detected in filter");}
	// if both the non linear flag is unset and there are enough pushes, return true
	return ((!this.nonLinearFlag) && enoughPushesSeen);
    }
    /** get the matrix representing this filter. **/
    public FilterMatrix getMatrixRepresentation() {
	return this.representationMatrix;
    }
    /** get the vector representing the constants that this filter adds/subtracts to produce output. **/
    public FilterVector getConstantVector() {
	return this.representationVector;
    }
    


    /////// So the deal with this attribute visitor is that all of its methods that visit
    /////// expressions that are some sort of linear (or affine) calculation on the inputs
    /////// the method returns a LinearForm. They return null otherwise.



//     public Object visitArgs(JExpression[] args) {return null;}
    /**
     * Visit an a ArrayAccessExpression. Currently just warn the user if we see one of these.
     * Constant prop should have removed all resolvable array expressions, so if we see one
     * then it is not linear and therefore we should return null;
     **/
    public Object visitArrayAccessExpression(JArrayAccessExpression self,
					     JExpression prefix,
					     JExpression accessor) {
	LinearPrinter.warn("Ignoring access expression: "+ self);
	return null; 
    }
    /**
     * When we visit an array initializer, we  simply need to return null, as we are currently
     * ignoring arrays on the principle that constprop gets rid of all the ones that are known at compile time.
     **/
    public Object visitArrayInitializer(JArrayInitializer self, JExpression[] elems){
	LinearPrinter.warn("Ignoring access expression: " + self);
	return null;
    }

    //     public Object visitArrayLengthExpression(JArrayLengthExpression self, JExpression prefix) {return null;}

    /**
     * Visit's an assignment statement. If the LHS is a variable (local or field)
     * and the RHS becomes a linear form, then we add a mapping from
     * the variable (JLocalVariableExpression or JFieldAccessExpression)
     * to the linear form in the variablesToLinearForm map.
     **/
    public Object visitAssignmentExpression(JAssignmentExpression self, JExpression left, JExpression right) {
	LinearPrinter.println("  visiting assignment expression: " + self);
	LinearPrinter.println("   left side: " + left);
	LinearPrinter.println("   right side: " + right);


	//// NOTE !!!!
	//// This doesn't handle th case of aliasing yet. Oh dearie.
	
	// make sure that we start with legal state
	checkRep();

	// check the RHS to see if it is a linear form -- pump us through it
	LinearForm rightLinearForm = (LinearForm)right.accept(this);

	// if the RHS is not a linear form, we are done (because this expression computes
	// something nonlinear we need to remove any mappings to the left hand
	// side of the assignment expression (because it now contains something non linear).
	if (rightLinearForm == null) {
	    removeMapping(left);
	    return null;
	}

	
	// now, if the left hand side is JLocalVariableExpression add a mapping
	// from the variable to the linear form
	if (left instanceof JLocalVariableExpression) {
	    JLocalVariableExpression lve = (JLocalVariableExpression)left; // casting happiness
	    JLocalVariable lv = lve.getVariable(); // get the variable
	    LinearPrinter.println("   adding a mapping from " + lv +
				  " to " + rightLinearForm);
	    // add the mapping from the local variable to the linear form
	    this.variablesToLinearForms.put(lv, rightLinearForm);
				  
	}
	if (left instanceof JFieldAccessExpression) {
	    // wrap the access expression so that the equals methods work out
	    AccessWrapper wrapper = AccessWrapper.wrapFieldAccess((JFieldAccessExpression)left);
	    LinearPrinter.println("   adding a field mapping from " + wrapper +
				  " to " + rightLinearForm);

	    this.variablesToLinearForms.put(wrapper, rightLinearForm);
	}

	// make sure that we didn't screw up our state
	checkRep();
	
	return null;
    }


    
    /**
     * visits a binary expression: eg add, sub, etc.
     * If the operator is a plus or minus,
     * we can deal with all LinearForms for left and right.
     * If the operator is multiply or divide, we can only deal if
     * both the left and right sides are LinearForms with
     * only offsets. It is not clear to me that we will be
     * ever run into this situation because constant prop should get
     * rid of them all.
     **/
    public Object visitBinaryExpression(JBinaryExpression self,
						  String oper,
						  JExpression left,
						  JExpression right) {

	// and I can't seem to figure out where the constants to recognize the
	// operators are. Therefore, I am going to hard code in the strings. Sorry about that.
	// if we are computing an additon or subtraction, we are all set, otherwise
	// we are done
	if (!(oper.equals("+") || oper.equals("-") || oper.equals("*") || oper.equals("/"))) {
	    LinearPrinter.println("  can't process " + oper + " linearly");
	    return null;
	}

	LinearPrinter.println("  visiting JBinaryExpression(" + oper + ")");
	LinearPrinter.println("   left: " + left);
	LinearPrinter.println("   right: " + right);
			      
	// first of all, try and figure out if left and right sub expression can
	// be represented in linear form.
	LinearForm leftLinearForm  = (LinearForm)left.accept(this);
	LinearForm rightLinearForm = (LinearForm)right.accept(this);

	// if both the left and right are non null, we are golden and can combine these two,
	// otherwise give up.
	if (leftLinearForm == null) {
	    LinearPrinter.println("  left arg (" + left + ") was not linear"); 
	    return null;
	}
	if (rightLinearForm == null) {
	LinearPrinter.println("  right arg (" + right + ") was not linear"); 
	    return null;
	}

	// if both expressions were linear, we can try to merge them together
	// dispatch on type -- sorry to all you language purists
	if ((self instanceof JAddExpression) || (self instanceof JMinusExpression)) {
	    return combineAddExpression(leftLinearForm, rightLinearForm, oper);
	} else if (self instanceof JMultExpression) {
	    return combineMultExpression(leftLinearForm, rightLinearForm, oper);
	} else if (self instanceof JDivideExpression) {
	    return combineDivideExpression(leftLinearForm, rightLinearForm, oper);
	} else {
 	    throw new RuntimeException("Non JAdd/JMinus/JMult/JDiv implementing +, -, *, /");
	}
    }

    /**
     * Combines an add expression whose arguments are both linear forms -- implements
     * a straightup element wise vector add.
     **/
    private Object combineAddExpression(LinearForm leftLinearForm, LinearForm rightLinearForm, String oper) {
	// if the operator is subtraction, negate the right expression
	if (oper.equals("-")) {
	    rightLinearForm = rightLinearForm.negate();
	}

	// now, add the two forms together and return the resulut
	LinearForm combinedLinearForm = leftLinearForm.plus(rightLinearForm);
	
	return combinedLinearForm;
    }

    /**
     * Combines a multiplication expression which has at most one non constant
     * sub expression. If both of the linear forms are non constant (eg 
     * weights that are non zero) then we return null
     **/
    private Object combineMultExpression(LinearForm leftLinearForm, LinearForm rightLinearForm, String oper) {
	LinearForm constantForm;
	LinearForm otherForm;

	// figure out which form represents a constant
	// is the left a constant?
	if (leftLinearForm.isOnlyOffset()) {
	    constantForm = leftLinearForm;
	    otherForm = rightLinearForm;
	    // how about the right?
	} else if (rightLinearForm.isOnlyOffset()) {
	    constantForm = rightLinearForm;
	    otherForm = leftLinearForm;
	// both are non constant, so give up
	} else {
	    return null;
	}

	// now, scale the other by the constant offset in the constant form
	// and return the scaled version
	LinearForm scaledForm = otherForm.multiplyByConstant(constantForm.getOffset());
	return scaledForm;
    }

    /**
     * Combines a division expresson. The right argument has to be a constant (eg only offset)
     **/
    private Object combineDivideExpression(LinearForm leftLinearForm, LinearForm rightLinearForm, String oper) {       
	// ensure that the right form is a constant. If not, we are done.
	if (!rightLinearForm.isOnlyOffset()) {
	    LinearPrinter.println("  right side of linear form is not constant"); 
	    return null;
	}
	LinearPrinter.println("  dividing left " + leftLinearForm + "\n   by right " + rightLinearForm);
	// just divide the left form by the constant
	return leftLinearForm.divideByConstant(rightLinearForm.getOffset());
    }
	    

    
    

//     public Object visitBitwiseComplementExpression(JUnaryExpression self, JExpression expr){return null;}
//     public Object visitBitwiseExpression(JBitwiseExpression self,
// 					 int oper,
// 					 JExpression left,
// 					 JExpression right){return null;}
//     public Object visitBlockStatement(JBlock self, JavaStyleComment[] comments){return null;}
//     public Object visitBreakStatement(JBreakStatement self, String label){return null;}
//     public Object visitCastExpression(JCastExpression self, JExpression expr, CType type){return null;}
//     public Object visitCatchClause(JCatchClause self, JFormalParameter exception, JBlock body){return null;}
//     public Object visitClassBody(JTypeDeclaration[] decls,
// 				 JFieldDeclaration[] fields,
// 				 JMethodDeclaration[] methods,
// 				 JPhylum[] body){return null;}
//     public Object visitClassDeclaration(JClassDeclaration self, int modifiers,
// 					String ident, String superName,
// 					CClassType[] interfaces, JPhylum[] body,
// 					JFieldDeclaration[] fields, JMethodDeclaration[] methods,
// 					JTypeDeclaration[] decls){return null;}
//     public Object visitClassExpression(JClassExpression self, CType type){return null;}
//     public Object visitClassImport(String name){return null;}
//     public Object visitComment(JavaStyleComment self){return null;}
//     public Object visitComments(JavaStyleComment[] self){return null;}
//     public Object visitCompilationUnit(JCompilationUnit self, JPackageName packageName,
// 						 JPackageImport[] importedPackages,
// 				       JClassImport[] importedClasses,
// 				       JTypeDeclaration[] typeDeclarations){return null;}

    /**
     * visits a compound assignment expression -- eg +=, -=, etc.
     * If the left hand side is a JLocalVariableExpression or JFieldAccessExpression
     * we add the appropriate mapping in variablesToLinearForms.
     **/
    public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
						    int oper, JExpression left,
 						    JExpression right){


	// act based on the operaton being performed. -- code mostly copied from Propagator.java
	// The basic premise is to expand out the compound operation into a normal JAssignmentExpression
	// and stuff ourselves (the visitor) through the new construct. This allows us to use our existing code.
	switch(oper) {

	case OPE_SR: // shift right operator:  >> -- non linear
	    return new JAssignmentExpression(null,left,new JShiftExpression(null,OPE_SR,left,right)).accept(this);

	case OPE_SL: // shift left operator:  << -- non linear
	    return new JAssignmentExpression(null,left,new JShiftExpression(null,OPE_SL,left,right)).accept(this);

	case OPE_PLUS: // plus operator:  +  -- linear!
	    return new JAssignmentExpression(null,left,new JAddExpression(null,left,right)).accept(this);

	case OPE_MINUS: // minus operator:  -  -- linear!
	    return new JAssignmentExpression(null,left,new JMinusExpression(null,left,right)).accept(this);

	case OPE_STAR: // multiplication operator:  +  -- linear!
	    return new JAssignmentExpression(null,left,new JMultExpression(null,left,right)).accept(this);

	case OPE_SLASH: // division operator:  / -- linear!
	    return new JAssignmentExpression(null,left,new JDivideExpression(null,left,right)).accept(this);

	case OPE_PERCENT: // modulus operator:  % -- non linear
	    return new JAssignmentExpression(null,left,new JModuloExpression(null,left,right)).accept(this);

	default:
	    throw new RuntimeException("Unknown operation while analyzing compound assignment expression:" + oper);
	}
    }


//     public Object visitCompoundStatement(JCompoundStatement self, JStatement[] body){return null;}
//     public Object visitConditionalExpression(JConditionalExpression self, JExpression cond,
// 					     JExpression left, JExpression right){return null;}
//     public Object visitConstructorCall(JConstructorCall self, boolean functorIsThis, JExpression[] params){return null;}
//     public Object visitConstructorDeclaration(JConstructorDeclaration self, int modifiers,
// 					      String ident, JFormalParameter[] parameters,
// 					      CClassType[] exceptions, JConstructorBlock body){return null;}
//     public Object visitContinueStatement(JContinueStatement self, String label){return null;}
//     public Object visitDoStatement(JDoStatement self, JExpression cond, JStatement body){return null;}
//     public Object visitEmptyStatement(JEmptyStatement self){return null;}
//     public Object visitEqualityExpression(JEqualityExpression self, boolean equal,
// 					  JExpression left, JExpression right){return null;}
//     public Object visitExpressionListStatement(JExpressionListStatement self, JExpression[] expr){return null;}
//     public Object visitExpressionStatement(JExpressionStatement self, JExpression expr){return null;}
//     public Object visitFieldDeclaration(JFieldDeclaration self, int modifiers,
// 					CType type, String ident, JExpression expr){return null;}
    /**
     * visits a field access expression. If there is a linear form mapped to this
     * expression in variablesToLinearForms, we return that. If there is no linear
     * form mapped to this expression, we simply return null;
     **/
    public Object visitFieldExpression(JFieldAccessExpression self, JExpression left, String ident){
	checkRep();
	LinearPrinter.println("  visiting field access expression: " + self);
	LinearPrinter.println("   left: " + left);
	LinearPrinter.println("   ident: " + ident);

	checkRep();

	// wrap the field access and then use that wrapper as the
	// key into the variables->linearform mapping
	AccessWrapper wrapper = AccessWrapper.wrapFieldAccess(self);

	// if we have a mapping, return it. Otherwise return null.
	if (this.variablesToLinearForms.containsKey(wrapper)) {
	    LinearPrinter.println("   (found mapping for " + wrapper + ")");
	    return this.variablesToLinearForms.get(wrapper);
	} else {
	    LinearPrinter.println("   (no mapping found for " + wrapper + ")");
	    Iterator keyIter = this.variablesToLinearForms.keySet().iterator();
	    while(keyIter.hasNext()) {
		LinearPrinter.println("key: " + keyIter.next());
	    }
	    return null;
	}
    }
//     public Object visitFormalParameters(JFormalParameter self, boolean isFinal,
// 					CType type, String ident){return null;}
//     public Object visitForStatement(JForStatement self, JStatement init,
// 				    JExpression cond, JStatement incr, JStatement body){return null;}
//     public Object visitIfStatement(JIfStatement self, JExpression cond,
// 				   JStatement thenClause, JStatement elseClause){return null;}
//     public Object visitInnerClassDeclaration(JClassDeclaration self, int modifiers,
// 					     String ident, String superName,
// 					     CClassType[] interfaces, JTypeDeclaration[] decls,
// 					     JPhylum[] body, JFieldDeclaration[] fields,
// 					     JMethodDeclaration[] methods){return null;}
//     public Object visitInstanceofExpression(JInstanceofExpression self, JExpression expr, CType dest){return null;}
//     public Object visitInterfaceDeclaration(JInterfaceDeclaration self, int modifiers,
// 					    String ident, CClassType[] interfaces,
// 					    JPhylum[] body, JMethodDeclaration[] methods){return null;}
//     public Object visitJavadoc(JavadocComment self){return null;}
//     public Object visitLabeledStatement(JLabeledStatement self, String label,
// 					JStatement stmt){return null;}

    /**
     * Visit a local variable expression. If we have a mapping of this
     * variable in our mappings to linear forms, return the linear form
     * otherwise return null.
     **/
    public Object visitLocalVariableExpression(JLocalVariableExpression self, String ident){
 	LinearPrinter.println("  visiting local var expression: " + self);
	LinearPrinter.println("   variable: " + self.getVariable());
	
	checkRep();

	// get the variable that this expression represents
	JLocalVariable theVariable = self.getVariable();
	
	// if we have a mapping, return it. Otherwise return null
	if (this.variablesToLinearForms.containsKey(theVariable)) {
	    LinearPrinter.println("   (found mapping!)");
	    return this.variablesToLinearForms.get(theVariable);
	} else {
	    LinearPrinter.println("   (mapping not found!)");
	    return null;
	}
    }
//     public Object visitLogicalComplementExpression(JUnaryExpression self, JExpression expr){return null;}
//     public Object visitMethodCallExpression(JMethodCallExpression self, JExpression prefix,
// 					    String ident, JExpression[] args){return null;}
//     public Object visitMethodDeclaration(JMethodDeclaration self, int modifiers,
// 					 CType returnType, String ident,
// 					 JFormalParameter[] parameters, CClassType[] exceptions,
// 					 JBlock body){return null;}
//     public Object visitNameExpression(JNameExpression self, JExpression prefix, String ident){return null;}

    /**
     * Visit a NewArrayExpression, creating mappings for all entries of the array to
     * a zero linear form (corresponding to initializing all array entries to zero
     * in StreamIt semantics.
     **/
    public Object visitNewArrayExpression(JNewArrayExpression self, CType type,
 					  JExpression[] dims, JArrayInitializer init){
	LinearPrinter.warn("Ignoring new array expression " + self);
	return null;
    }
    

    //     public Object visitPackageImport(String name){return null;}
    //     public Object visitPackageName(String name){return null;}
    public Object visitParenthesedExpression(JParenthesedExpression self, JExpression expr){
	LinearPrinter.println("  visiting parenthesized expression");
	// pass ourselves through the parenthesized expression to generate the approprate constant forms
	return expr.accept(this);
    }
//     public Object visitPostfixExpression(JPostfixExpression self, int oper, JExpression expr){return null;}
//     public Object visitPrefixExpression(JPrefixExpression self, int oper, JExpression expr){return null;}
//     public Object visitQualifiedAnonymousCreation(JQualifiedAnonymousCreation self,
// 						  JExpression prefix, String ident,
// 						  JExpression[] params, JClassDeclaration decl){return null;}
//     public Object visitQualifiedInstanceCreation(JQualifiedInstanceCreation self,
// 						 JExpression prefix, String ident,
// 						 JExpression[] params){return null;}
//     public Object visitRelationalExpression(JRelationalExpression self, int oper,
// 					    JExpression left, JExpression right){return null;}
//     public Object visitReturnStatement(JReturnStatement self, JExpression expr){return null;}
//     public Object visitShiftExpression(JShiftExpression self, int oper, JExpression left, JExpression right){return null;}
//     public Object visitShortLiteral(JShortLiteral self, short value){return null;}
//     public Object visitSuperExpression(JSuperExpression self){return null;}
//     public Object visitSwitchGroup(JSwitchGroup self, JSwitchLabel[] labels, JStatement[] stmts){return null;}
//     public Object visitSwitchLabel(JSwitchLabel self, JExpression expr){return null;}
//     public Object visitSwitchStatement(JSwitchStatement self, JExpression expr, JSwitchGroup[] body){return null;}
//     public Object visitSynchronizedStatement(JSynchronizedStatement self, JExpression cond, JStatement body){return null;}
//     public Object visitThisExpression(JThisExpression self, JExpression prefix){return null;}
//     public Object visitThrowStatement(JThrowStatement self, JExpression expr){return null;}
//     public Object visitTryCatchStatement(JTryCatchStatement self, JBlock tryClause, JCatchClause[] catchClauses){return null;}
//     public Object visitTryFinallyStatement(JTryFinallyStatement self, JBlock tryClause, JBlock finallyClause){return null;}
//     public Object visitTypeDeclarationStatement(JTypeDeclarationStatement self, JTypeDeclaration decl){return null;}
//     public Object visitTypeNameExpression(JTypeNameExpression self, CType type){return null;}
//     public Object visitUnaryMinusExpression(JUnaryExpression self, JExpression expr){return null;}
//     public Object visitUnaryPlusExpression(JUnaryExpression self, JExpression expr){return null;}
//     public Object visitUnaryPromoteExpression(JUnaryPromote self, JExpression expr, CType type){return null;}
//     public Object visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
// 						    CClassType type, JExpression[] params,
// 						    JClassDeclaration decl){return null;}
//     public Object visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self, CClassType type,
// 						   JExpression[] params){return null;}
//     public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
// 						    JVariableDefinition[] vars){return null;}
//     public Object visitVariableDefinition(JVariableDefinition self, int modifiers,
// 					  CType type, String ident, JExpression expr){return null;}
//     public Object visitWhileStatement(JWhileStatement self, JExpression cond, JStatement body){return null;}



    ///// SIR Constructs that are interesting for linear analysis (eg push, pop, and peek expressions)
    
    /**
     * when we visit a push expression, we are basically going to try and
     * resolve the argument expression into linear form. If the argument
     * resolves into linear form, then we are golden -- we make a note of the 
     **/
    public Object visitPushExpression(SIRPushExpression self, CType tapeType, JExpression arg) {
	LinearPrinter.println("  visiting push expression: " +
			   "argument: " + arg);
	// try and resolve the argument to a LinearForm by munging the argument
	LinearForm argLinearForm = (LinearForm)arg.accept(this);

	// if we get null, it means we don't know that this push expression will
	// yield a linear form, so therefore we can't characterize the filter as
	// a whole as having linear form.
	if (argLinearForm == null) {
	    // set the flag that says this filter isn't linear
	    this.nonLinearFlag = true;
	} else {
	    // (note that the first push ends up in the rightmost column)
	    // so we calculate which column that this push statement corresponds to
	    int pushColumn = this.pushSize - this.pushOffset -1;
	    // we have a linear form, so we update the matrix representation
	    argLinearForm.copyToColumn(this.representationMatrix, pushColumn);
	    // update the constant vector with the offset from the linear form
	    this.representationVector.setElement(pushColumn, argLinearForm.getOffset());
	}

	// increment the push offset (for the next push statement)
	this.pushOffset++;

	// make sure we didn't screw up anything.
	checkRep();
	// push expressions don't return values, so they also certainly don't return linear forms
	return null;
    }
    /**
     * A pop expression generates a linear form based on the current offset, and then
     * updates the current offset. The basic idea is that a pop expression represents using one
     * one of the input values from the tapes, and therefore should correspond to a linear form
     * with a "1" at the appropriate place in the weights vector.
     **/
    public Object visitPopExpression(SIRPopExpression self, CType tapeType) {
	LinearPrinter.println("  visiting pop expression: " + self);
	// A pop expression is one of the base cases for creating LinearForms
	// the pop expression will creates a linear form that corresponds to using
	// a peek at the current offset, which in turn corresponds to a
	// use of the element at size-peekoffset-1 in the input vector
	int inputIndex = this.peekSize - this.peekOffset - 1;
    
	LinearForm currentForm = this.getBlankLinearForm();
	currentForm.setWeight(inputIndex, ComplexNumber.ONE);
	
	// when we hit a pop expression, all further peek expressions have their
	// indicies incremented by one compared to the previous expressions
	this.peekOffset++;

	// return the linear form of the pop expression
	LinearPrinter.println("  returning " + currentForm + " from pop expression");
	return currentForm;
    }
    /**
     * Peek expressions are also base expressions that generate linear forms.
     * The peek index is transformed into a "1" in the appropriate place in
     * the weights vector of the returned linear form. We also have to keep track of
     * the case when there haev been previous pops which change the relative position of the
     * index we are processing.
     **/
    public Object visitPeekExpression(SIRPeekExpression self, CType tapeType, JExpression arg) {
	LinearPrinter.println("  visiting peek expression" +
			   " peek index: " + arg);

	// now, we have to visit the expression of the peek( ) to see if it is a constant
	// (in this context, that will be a linear form)
	LinearForm exprLinearForm = (LinearForm)arg.accept(this);

	// if we didn't find a linear form in the expression, we are cooked
	// (possibly we couldn't resolve the value of some variable
	if (exprLinearForm == null) {
	    return null;
	}

	// otherwise, make sure that the linear form is only a constant offset
	// (if it isn't, we are done beacuse we can't resolve what data item is being looked at)
	if (!exprLinearForm.isOnlyOffset()) {
	    return null;
	}

	// if the offset is not an integer, something is very wrong. Well, not wrong
	// but unesolable because the index is computed with input data.
	if (!exprLinearForm.isIntegerOffset()) {
	    return null;
	}

	// otherwise, create a new linear form that represents which input value that this
	// peek expression produces.
	// basically, it will be a linear form that is all zeros in its weights
	// except for a 1 in the index corresponding to the data item that this peek expression
	// accesses	
	LinearForm peekExprLinearForm = this.getBlankLinearForm();
	peekExprLinearForm.setWeight(this.peekSize - 1 - exprLinearForm.getIntegerOffset() - this.peekOffset,
				     ComplexNumber.ONE);
	LinearPrinter.println("  returning linear form from peek expression: " + peekExprLinearForm);
	return peekExprLinearForm;
    }


    



    public Object visitMethodCallExpression(JMethodCallExpression self,
					    JExpression prefix,
					    String ident,
					    JExpression[] args) {
	LinearPrinter.println("  visiting method call expression: " + self);
	return super.visitMethodCallExpression(self, prefix, ident, args);
    }


    ////// Generators for literal expressions
    
    /** boolean logic falls outside the realm of linear filter analysis -- return null**/
    public Object visitBooleanLiteral(JBooleanLiteral self,boolean value) {return null;}
    /** create the appropriate valued offset **/
    public Object visitByteLiteral(JByteLiteral self, byte value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** create the appropriate valued offset **/
    public Object visitCharLiteral(JCharLiteral self,char value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** create the appropriate valued offset **/
    public Object visitDoubleLiteral(JDoubleLiteral self,double value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** create the appropriate valued offset **/
    public Object visitFloatLiteral(JFloatLiteral self,float value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** create the appropriate valued offset **/
    public Object visitIntLiteral(JIntLiteral self, int value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** create the appropriate valued offset **/
    public Object visitLongLiteral(JLongLiteral self,long value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** create the appropriate valued offset **/
    public Object visitShortLiteral(JShortLiteral self,short value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** We can't deal with strings, not linear, return null **/
    public Object visitStringLiteral(JStringLiteral self,String value) {
	return null;
    }
    /** if we have null nonsense, not going to be linear. Return null (how appropriate)**/
    public Object visitNullLiteral(JNullLiteral self) {
	return null;
    }






    /**
     * Removes the mapping of a local variable expression or a field access expression
     * from the variablesToLinearForm mapping. Throws an exception if we try to remove
     * a mapping to a type that we don't know about
     **/
    public void removeMapping(JExpression expr) {
	// dispatch on type
	if (expr instanceof JLocalVariableExpression) {
	    removeMapping((JLocalVariableExpression)expr);
	} else if (expr instanceof JFieldAccessExpression) {
	    removeMapping((JFieldAccessExpression)expr);
	} else {
	    throw new RuntimeException("Can have linear mappings to " + expr +
				       " (of type " + expr.getClass().getName());
	}
    }
    
    /**
     * Removes the mapping for a particular JLocalVariable in a JLocalVariableExpression.
     * Does not report errors when a variable that is not in the mapping is "removed."
     **/
    public void removeMapping(JLocalVariableExpression lve) {
	// extract the variable
	JLocalVariable theVariable = lve.getVariable();
	// if the variable is a key in the mappings, then remove the mapping
	if (this.variablesToLinearForms.containsKey(theVariable)) {
	    LinearPrinter.println("  Removing mapping for " + theVariable);
	    this.variablesToLinearForms.remove(theVariable);
	}
    }
    /**
     * Removes the mapping for a particular JFieldAccessExpression
     * Does not report errors when a variable that is not in the mapping is "removed."
     **/
    public void removeMapping(JFieldAccessExpression fae) {
	throw new RuntimeException("Not yet implemented");
    }
    

    

    /**
     * Creates a blank linear form that is appropriate for this filter (eg
     * it has size of peekSize.
     **/
    private LinearForm getBlankLinearForm() {
	checkRep();
	return new LinearForm(this.peekSize);
    }

    /** Creates a blank linear form that has the specified offset **/
    private LinearForm getOffsetLinearForm(double offset) {
	checkRep();
	LinearForm lf = this.getBlankLinearForm();
	lf.setOffset(offset);
	LinearPrinter.println("  created constant linear form for " + offset);
	return lf;
    }

    /**
     * Check the representation invariants of the LinearFilterVisitor.
     **/
    private void checkRep() {
	// check that the only values in the HashMap are LinearForm objects
	Iterator keyIter = this.variablesToLinearForms.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object key = keyIter.next();
	    if (key == null) {throw new RuntimeException("Null key in linear form map");}
	    // make sure that they key is a JLocalVariable or a JFieldAccessExpression
	    if (!((key instanceof JLocalVariable) ||
		  (key instanceof AccessWrapper))) {
		throw new RuntimeException("Non local or field access wrapper in linear form map.");
	    }
	    Object val = this.variablesToLinearForms.get(key);
	    if (!(val instanceof LinearForm)) {throw new RuntimeException("Non LinearForm in value map");}
	}
	
	
	// make sure that the peekoffset is not less than one, and that it
	// is not greater than the peeksize
	if (this.peekOffset < 0) {throw new RuntimeException("Peekoffset < 0");}
	// if the filter doesn't peek at any data, the following is incorrect.
	if (peekSize != 0) {
	    if (this.peekOffset > this.peekSize) {
		throw new RuntimeException("Filter pops more than peeks:" +
					   "peekSize: " + this.peekSize + " " +
					   "peekOffset: " + this.peekOffset);
	    }
	}
	// make sure that the number of pushes that we have seen doesn't go past the end of
	// the matrix/vector that represents this filter.
	if (this.pushOffset > this.representationMatrix.getRows()) {
	    throw new RuntimeException("Filter pushes more items than is decalred");
	}
	    
    }

}
























/** Control point for printing messages **/
class LinearPrinter {
    /** flag to control output generation. **/
    private static boolean outputEnabled = false;
    public static void setOutput(boolean outFlag) {
	outputEnabled = outFlag;
    }
    public static void println(String message) {
	if (outputEnabled) {
	    System.out.println(message);
	}
    }
    public static void print(String message) {
	if (outputEnabled) {
	    System.out.print(message);
	}
    }
    public static void warn(String message) {
	System.err.println("WARNING: " + message);
    }
}
    

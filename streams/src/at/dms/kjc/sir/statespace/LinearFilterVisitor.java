package at.dms.kjc.sir.statespace;

import java.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.*;


/**
 * The visitor that goes through all of the expressions of a work function
 * of a filter to determine if the filter is in linear state space form and if it is what matrices
 * corresponds to the filter. 
 * This main workings of this class are as follows: it is an AttributeVisitor,
 * which means (in plain English), that its methods return objects. For the
 * LinearFilterVisitor, each method analyzes a IR node. It returns one of two
 * things: null or a LinearForm. <br>
 *
 * Returning null indicates that that particular IR node does not compute a
 * linear function. Otherwise, a LinearForm is returned, which corresponds to
 * the linear function that is computed by that IR node. LinearForms are
 * used to represent linear combinations of the inputs and states.<br>
 *
 * $Id: LinearFilterVisitor.java,v 1.2 2004-02-12 22:32:57 sitij Exp $
 * Modified to state space form by Sitij Agrawal  2/9/04
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
     * Number of items that are peeked at. This information is needed for
     * later stages of analysis and is passed on to the LinearFilterRepresentation.
     **/
    private int peekSize;

    /**
     * Number of items that are pushed. Therefore it also represents the
     * number of rows that are in the matrices C, D.
     **/
    private int pushSize;

    /**
     * Number of items that are popped. Therefore this is also the same
     * size of the vector that must be used to represent each affine combination of inputs.
     **/
    private int popSize;

    /**
     * Number of (global) fields
     **/
    private int fieldSize;

    /**
     * Fields enumerated in an array
     **/
    private JFieldDeclaration[] fieldArray;

    /**
     * Number of states. There are peekSize-popSize buffer states, one state for each global      * field declaration, and one state used as a constant.
     **/
    private int stateSize;

    /**
     * The current offset to add to a peeked value. Eg if we execute
     * <pre>peek(5); pop(); peek(5); </pre> the second peek
     * expression actually gets a different element, as you would expect.
     **/
    private int peekOffset;

    /**
     * The current push offset. This keeps track of which column in the Linear representation
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

    /** The matrices which represents this filter. **/
    private FilterMatrix A;
    private FilterMatrix B;
    private FilterMatrix C;
    private FilterMatrix D;

    /**
     * String which represents the name of the filter being visited -- this is used
     * for reporting useful error messages.
     **/
    private String filterName;

    
    private SIRFilter filt;



    /**
     * Create a new LinearFilterVisitor which is looking to figure out 
     * how to compute, for all variables, linear forms from the input.
     * Also creates a LinearFilterRepresentation if the
     * filter computes a linear function.
     **/
    public LinearFilterVisitor(SIRFilter Self) {
        this.filt = Self;
	this.peekSize = Self.getPeekInt();
	this.pushSize = Self.getPushInt();
	this.popSize  = Self.getPopInt();
	this.fieldArray = Self.getFields();
        this.fieldSize = this.fieldArray.length;

	// one state for each buffered item, one state for each field, one constant state
        this.stateSize = (this.peekSize - this.popSize) + this.fieldSize + 1;

	this.variablesToLinearForms = new HashMap();
	this.peekOffset = 0;
	this.pushOffset = 0;

	// create mappings for each global variable

        AccessWrapper fieldWrapper;
        LinearForm fieldForm;
        int offset = this.peekSize - this.popSize;

	for(int i = 0; i < this.fieldSize; i++) {
	    fieldWrapper = AccessWrapperFactory.wrapAccess(this.fieldArray[i]);
	    fieldForm = new LinearForm(this.popSize,this.stateSize);
	    fieldForm.setStateWeight(offset+i, ComplexNumber.ONE);
	    this.variablesToLinearForms.put(fieldWrapper,fieldForm);
	}

	// create matrices

        this.A = new FilterMatrix(this.stateSize, this.stateSize);
	this.B = new FilterMatrix(this.stateSize, this.popSize);
	this.C = new FilterMatrix(this.pushSize, this.stateSize);        
	this.D = new FilterMatrix(this.pushSize, this.popSize);

	this.nonLinearFlag = false;
	this.filterName = Self.getIdent();
	checkRep();

    }


    /**
     * This is the final clean-up code
     * It finds the final assignments to state-variables (which go to matrices A and B)
     */

    public void complete() {

	AccessWrapper fieldWrapper;
        LinearForm fieldForm;
        int offset = this.peekSize - this.popSize;
        int rowVal;

	for(int i = 0; i < this.fieldSize; i++) {

	  fieldWrapper =  AccessWrapperFactory.wrapAccess( this.fieldArray[i]);

	  if (this.variablesToLinearForms.containsKey(fieldWrapper)) {
	    LinearPrinter.println("   (found mapping for " + fieldWrapper + ")");
	    fieldForm = (LinearForm)this.variablesToLinearForms.get(fieldWrapper);
	    rowVal = offset+i;
	    // we have a linear form, so we update the matrix representation
	    fieldForm.copyInputsToRow(this.B, rowVal);
	    fieldForm.copyStatesToRow(this.A, rowVal);
	  } 
	  else {
	    LinearPrinter.println("   (no mapping found for " + fieldWrapper + ")");
	    this.nonLinearFlag = true;
	  }
	}

	// this loop updates the peek states
	LinearForm peekLinear;
	for(int i=0; i < offset; i++) {
	    peekLinear = new LinearForm(this.popSize, this.stateSize);
	    if(i+this.popSize < offset) {   // the ith peek state is updated by a state
		peekLinear.setStateWeight(i+this.popSize,ComplexNumber.ONE);
	    }
	    else {   // the ith peek state is updated by an input
		peekLinear.setInputWeight(i+this.popSize - offset,ComplexNumber.ONE);
	    }
	    peekLinear.copyInputsToRow(this.B, i);
	    peekLinear.copyStatesToRow(this.A, i);
	}

    }

    /**
     * Returns a shallow clone of this filter visitor (eg all of the
     * data structures are copied, but the things that they point to are not.
     * Used at program split points (eg if statements).
     **/
    
    private LinearFilterVisitor copy() {
	// first, make the copy using the default constructors
	LinearFilterVisitor otherVisitor = new LinearFilterVisitor(this.filt);

	// now, copy the other data structures.
	otherVisitor.variablesToLinearForms = new HashMap(this.variablesToLinearForms);
	otherVisitor.peekOffset = this.peekOffset;
	otherVisitor.pushOffset = this.pushOffset;
	otherVisitor.A = this.A.copy();
	otherVisitor.B = this.B.copy();
	otherVisitor.C = this.C.copy();
	otherVisitor.D = this.D.copy();	
	otherVisitor.nonLinearFlag = this.nonLinearFlag;

	// and I think that that is all the state that we need.
	return otherVisitor;
    }
    

    /**
     * Recconcile the differences between two LinearFilterVisitors after
     * we rejoin from an if statement.
     * Basically, this implements the confluence operation that we
     * want to to after analyzing both the then and the else parts
     * of an if statement. We assume that otherVisitor was passed through
     * the other branch, and we are at the point where the control flow
     * comes back together and we want to figure out what is going on.<br>
     *
     * The basic rules are pretty simple. If the representation matrices are
     * different, or either this or otherVisitor has the nonlinear flag
     * set, we set this.nonlinear flag to true, and continue (because the rest
     * is irrelevant). If the linear representations are different, we also
     * set the non linear flag and continue. Also, if the push counts are different
     * we complain loudly, and bomb with an exception. If the reps are not different,
     * then we do a set union on the variables to linear forms (like const prop).
     **/
    
    public void applyConfluence(LinearFilterVisitor other) {
	// first thing that we need to do is to check both non linear flags.
	if (this.nonLinearFlag || other.nonLinearFlag) {
	    this.nonLinearFlag = true;
	    throw new NonLinearException("One side of an if branch is non-linear");
	}
	// now, check the linear representations
	if ((!this.A.equals(other.A)) || (!this.B.equals(other.B)) ||
            (!this.C.equals(other.C)) || (!this.D.equals(other.D))) {
	    this.nonLinearFlag = true;
	    throw new NonLinearException("Different branches compute different functions. Nonlinear!");
	}
	// now, check the peek offset
	if (!(this.peekOffset == other.peekOffset)) {	    
	    this.nonLinearFlag = true;
	    throw new NonLinearException("Different branches have diff num of pops. " +
					 "this = " + this.peekOffset +
					 " other= " + other.peekOffset +
					 ")  Nonlinear!");
	}	    
	// now, check the push offset
	if (!(this.pushOffset == other.pushOffset)) {	   
	    this.nonLinearFlag = true;
	    throw new NonLinearException("Different branches have diff num of pushes. Nonlinear!");
	}
	// now, do a set union on the two variable mappings and set
	// that union as the variable mapping for this
	this.variablesToLinearForms = setUnion(this.variablesToLinearForms,
					       other.variablesToLinearForms);

	// make sure we are still good from a representation invariant point of view
	checkRep();
    }
    

    /**
     * Implements set union for the confluence operation.
     * Returns the union of the two sets. That is it returns
     * a HashMap that contains only mappings from the same
     * key to the same value in both map1 and map2.
     **/
    
    private HashMap setUnion(HashMap map1,
			     HashMap map2) {
	HashMap unionMap = new HashMap();
	// iterate over the values in map1Iter.
	Iterator map1Iter = map1.keySet().iterator();
	while(map1Iter.hasNext()) {
	    Object currentKey = map1Iter.next();
	    // if both maps contain the same key
	    if (map1.containsKey(currentKey) && map2.containsKey(currentKey)) {
		// if the item that the key maps to in both sets is the same
		if (map2.get(currentKey).equals(map1.get(currentKey))) {
		    // add the key to the union map
		    unionMap.put(currentKey, map1.get(currentKey));
		}
	    }
	}
	return unionMap;
    }      
    

    /** Returns true if the filter computes a linear state space function. **/
    public boolean computesLinearFunction() {
	// check the flag (which is set when we hit a non linear function in a push expression)
	// and check that we have seen the correct number of pushes.
	boolean enoughPushesSeen = (this.pushSize == this.pushOffset); // last push was to pushSize-1
	if (!(enoughPushesSeen)) {LinearPrinter.warn("Insufficient pushes detected in filter");}
	// if both the non linear flag is unset and there are enough pushes, return true
	return ((!this.nonLinearFlag) && enoughPushesSeen);
    }

    /** Sets the non linear flag to true. Used when a non linear exception is thrown. **/
    public void setNonLinear() {
	this.nonLinearFlag = true;
    }
	   
    /** Get the matrices representing this filter. **/
    public FilterMatrix getA() {
	return this.A;
    }

    public FilterMatrix getB() {
	return this.B;
    }

    public FilterMatrix getC() {
	return this.C;
    }

    public FilterMatrix getD() {
	return this.D;
    }

    /** Get the linear representation of this filter. **/
    public LinearFilterRepresentation getLinearRepresentation() {
	// throw exception if this filter is not linear, becase therefore we
	// shouldn't be trying to get its linear form.
	if (!this.computesLinearFunction()) {
	    throw new RuntimeException("Can't get the linear form of a non linear filter!");
	}
	return new LinearFilterRepresentation(this.getA(), this.getB(),
					      this.getC(), this.getD(),
					      this.peekSize);
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
	return getMapping(self);
    }
    /**
     * When we visit an array initializer, we simply need to return null, as we are currently
     * ignoring arrays on the principle that constprop gets rid of all the ones that are
     * known at compile time.
     **/
    public Object visitArrayInitializer(JArrayInitializer self, JExpression[] elems){
	LinearPrinter.warn("Ignoring array initialization expression: " + self);
	throw new RuntimeException("Array initializations are not supported yet.");
    }

    //     public Object visitArrayLengthExpression(JArrayLengthExpression self, JExpression prefix) {return null;}

    /**
     * Visits an assignment statement. If the LHS is a variable (local or field)
     * and the RHS becomes a linear form, then we add a mapping from
     * the variable (JLocalVariableExpression or JFieldAccessExpression)
     * to the linear form in the variablesToLinearForm map.
     **/
    public Object visitAssignmentExpression(JAssignmentExpression self, JExpression left, JExpression right) {
	LinearPrinter.println("  visiting assignment expression: " + self);
	LinearPrinter.println("   left side: " + left);
	LinearPrinter.println("   right side: " + right);


	//// NOTE !!!!
	//// This doesn't handle the case of array aliasing.
	// (which is fine because array aliases can not arise in the
	//  new streamit syntax.)

	//Oh dearie.
	// Also note that we basically ignore structures because the
	// necessary actions should be implemented in  
	// field prop and not reimplemented here
	
	// make sure that we start with legal state
	checkRep();

	// eventually, ConstProp will handle setting the inialized array values to
	// zero. For now, we only provide mappings to arr[i]=0 for in arr=new int[5]
	// for single dimension arrays
	// check the RHS to see if it is a new array expression
	if (right instanceof JNewArrayExpression) {
	    // make sure that this is only a one dimensional array
	    JNewArrayExpression nae = (JNewArrayExpression)right;
	    if (nae.getDims().length > 1) {
		LinearPrinter.warn("Multidimensional array initializations are not handled by the linear filter analyzer");
		return null;
	    }
	    // see if we have a linear form (eg constant) for the size of the array
	    LinearForm sizeForm = (LinearForm)nae.getDims()[0].accept(this);
	    if ((sizeForm != null) && (sizeForm.isIntegerOffset())) {
		int arraySize = sizeForm.getIntegerOffset();
		// add new mappings for each element in the array to zero.	    
		AccessWrapperFactory.addInitialArrayMappings(left,
							     arraySize,
							     this.variablesToLinearForms,
							     this.popSize,
							     this.stateSize);
	    } else {
		LinearPrinter.warn("Ignoring JNewArrayExpression: " + right);
	    }
	    // we are all done, so return null
	    return null;
	}

	// check the RHS to see if it is a linear form -- pump us through it
	LinearForm rightLinearForm = (LinearForm)right.accept(this);

	// if the RHS is not a linear form, we are done (because this expression computes
	// something nonlinear we need to remove any mappings to the left hand
	// side of the assignment expression (because it now contains something non linear).
	if (rightLinearForm == null) {
	    removeMapping(left);
	    return null;
	}
	
	// try and wrap the left hand side of this expression
	AccessWrapper leftWrapper = AccessWrapperFactory.wrapAccess(left);

	// if we successfully wrapped the expression, say so, and add it to our variable
	// map
	if (leftWrapper != null) {
	    LinearPrinter.println("   adding a mapping from " + left +
				  //" to " + rightLinearForm);
				  " to a linear form");
	    // add the mapping from the local variable to the linear form
	    this.variablesToLinearForms.put(leftWrapper, rightLinearForm);  
	}

	// make sure that we didn't screw up our state
	checkRep();
	
	return null;
    }


    
    /**
     * Visits a binary expression: eg add, sub, etc.
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
	    

    
    

     public Object visitBitwiseComplementExpression(JUnaryExpression self, JExpression expr){return null;}
     public Object visitBitwiseExpression(JBitwiseExpression self,
 					 int oper,
 					 JExpression left,
 					 JExpression right){return null;}
//     public Object visitBlockStatement(JBlock self, JavaStyleComment[] comments){return null;}
//     public Object visitBreakStatement(JBreakStatement self, String label){return null;}
    /**
     * Visit a cast expression, which basically means that we need to
     * do chopping off if we are casting to
     * an integer, byte, etc. If we cast something to an int, that is a non linear operation, so we
     * just return null.
     **/
    public Object visitCastExpression(JCastExpression self, JExpression expr, CType type){
	// if we have a non ordinal type for the expression, and an ordinal type for
	// the cast, then this is a non linear operation, and we should return null.
	if (type.isOrdinal() && (!expr.getType().isOrdinal())) {
	    // this chops off digits (possibly), so non linear. We are all done.
	    return null;
	} else {
	    // return whatever the expression evaluates to
	    return expr.accept(this);
	}
    }
    

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
    /**
     * Visits an expression of the form (expr) ? (left) : (right). Since we expect const prop to
     * handle reducing this case if the conditional expression expr is constant, we assume that a
     * JConditionalExpression is non linear, and thus return null.
     **/
    public Object visitConditionalExpression(JConditionalExpression self, JExpression cond,
					     JExpression left, JExpression right){
	return null; 
    }
//     public Object visitConstructorCall(JConstructorCall self, boolean functorIsThis, JExpression[] params){return null;}
//     public Object visitConstructorDeclaration(JConstructorDeclaration self, int modifiers,
// 					      String ident, JFormalParameter[] parameters,
// 					      CClassType[] exceptions, JConstructorBlock body){return null;}
//     public Object visitContinueStatement(JContinueStatement self, String label){return null;}
//     public Object visitDoStatement(JDoStatement self, JExpression cond, JStatement body){return null;}
//     public Object visitEmptyStatement(JEmptyStatement self){return null;}
    /** Equality is a non linear operation, so we return null. **/
    public Object visitEqualityExpression(JEqualityExpression self, boolean equal,
 					  JExpression left, JExpression right){
	return null;
    }
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
	AccessWrapper wrapper = AccessWrapperFactory.wrapAccess(self);

	// if we have a mapping, return it. Otherwise return null.
	if (this.variablesToLinearForms.containsKey(wrapper)) {
	    LinearPrinter.println("   (found mapping for " + wrapper + ")");
	    return this.variablesToLinearForms.get(wrapper);
	} else {
	    LinearPrinter.println("   (no mapping found for " + wrapper + ")");
	    return null;
	}
    }
//     public Object visitFormalParameters(JFormalParameter self, boolean isFinal,
// 					CType type, String ident){return null;}
     public Object visitForStatement(JForStatement self, JStatement init,
				     JExpression cond, JStatement incr, JStatement body){
	 this.nonLinearFlag = true;
	 LinearPrinter.warn("Not yet implemented -- for loops are not handled yet (use --unroll 100000).");
	 throw new NonLinearException("For loops are not implemented yet. Use --unroll 1000000 option.");
     }
    /**
     * Visit an if statement -- push this down the
     * the then clause, push a copy of this down the else clause,
     * and then merge the differences. Also of note is that
     * the conditional is a constant, constprop should have taken
     * care of it, so flag an error.
     **/
    public Object visitIfStatement(JIfStatement self, JExpression cond,
				   JStatement thenClause, JStatement elseClause){
	LinearForm condForm = (LinearForm)cond.accept(this);
	// if the cond form is a constant (eg only an offset), we should bomb an error as
	// const prop should have taken care of it.
	if (condForm != null) {
	    if (condForm.isOnlyOffset()) {
		throw new RuntimeException("Constant condition to if statement, " +
					   "const prop should have handled " +
					   self);
	    }
	}
	// now, make a copy of this
	LinearFilterVisitor otherVisitor = this.copy();
	// send this through the then clause, and the copy through the else clause
	thenClause.accept(this);
	// if we have an else clause, then send the visitor through it. Else,
	// the program could possibly skip the then clause, so we need to
	// merge the differences afterwards.
	if (elseClause != null) {
	    elseClause.accept(otherVisitor);
	}

	// recconcile the differences between the else clause (or skip if no
	// else clause).
	this.applyConfluence(otherVisitor);
	
	return null;
    }
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

	return getMapping(self);
    }
//     public Object visitLogicalComplementExpression(JUnaryExpression self, JExpression expr){return null;}

    /**
     * Eventually, we should do interprodcedural analysis. Right now instead, we will
     * simply ignore them (eg return null signifying that they do not generate linear things).
     **/
    public Object visitMethodCallExpression(JMethodCallExpression self, JExpression prefix,
 					    String ident, JExpression[] args){	
	LinearPrinter.warn("Assuming method call expression non linear(" +
			   ident + "). Also removing all field mappings.");
	this.removeAllFieldMappings();
	return null;
    }
    
    public Object visitPrintStatement(SIRPrintStatement self,
				      JExpression arg) {
	/* if this method call is to a method with side effects,
	   mark the whole expression as non-linear. */
	String message = " printing is a non-linear operation.";
	LinearPrinter.println(message);
	this.nonLinearFlag = true;
	throw new NonLinearException(message);
    }


    /**
     * Removes all of the mappings from fields to linear forms. We do this on a method call
     * to make conservative, safe assumptions.
     **/
    private void removeAllFieldMappings() {
	// basic idea is really simple -- iterate over all keys in our hashmap
	// and remove the ones that are AccessWrappers.
	Vector toRemove = new Vector(); // list of items to remove.
	Iterator keyIter = this.variablesToLinearForms.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object key = keyIter.next();
	    if (AccessWrapperFactory.isFieldWrapper(key)) {
		toRemove.add(key);
	    }
	}
	// now, remove all items in the toRemove list from the mapping
	Iterator removeIter = toRemove.iterator();
	while(removeIter.hasNext()) {
	    this.variablesToLinearForms.remove(removeIter.next());
	}
    }

    /**
     * Returns the mapping that we have from expr to linear form.
     * returns null if no such mapping exists. 
     **/
    public Object getMapping(JExpression expr) {
	checkRep();

	// wrap the variable that the expression represents
	AccessWrapper wrapper = AccessWrapperFactory.wrapAccess(expr);

	// if we have a mapping, return it. Otherwise return null
	if (this.variablesToLinearForms.containsKey(wrapper)) {
	    LinearPrinter.println("   (found mapping for " + expr + "!)");
	    return this.variablesToLinearForms.get(wrapper);
	} else {
	    LinearPrinter.println("   (mapping not found for " + expr + "!)");
	    return null;
	}
    }

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
	// right now, only support one dimensional arrays. If more than one dimension, complain loudly
	if (dims.length > 1) { throw new RuntimeException("Multidimensional arrays are not supported yet"); }
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
    /**
     * Visit a relational expression (eg using a <, >, ==, !=, etc. type of parameter.
     * Since these are all non linear operatons, we need to return
     * null to flag the rest of the visitor that they are non linear operations.
     **/
    public Object visitRelationalExpression(JRelationalExpression self, int oper,
					    JExpression left, JExpression right){
	LinearPrinter.println("  visiting non linear" + self);
	return null;
    }
//     public Object visitReturnStatement(JReturnStatement self, JExpression expr){return null;}

    /**
     * Shift expressions are linear for integer operators.
     * They correspond to multiplications or divisions by a power of two.
     * if the RHS of the expression is a constant (eg a linear form
     * with only an offset) then we can convert it to a power of two, and
     * then make the shift a multiplication or division. Otherwise, this is
     * not a linear operation.
     **/
    public Object visitShiftExpression(JShiftExpression self, int oper, JExpression left, JExpression right){
	// since the left and the right expressions are somre type of integer
	// or byte or something, we are all set. You can't shift floats, as it
	// is not in java syntax and KOPI disallows it in the semantic checking
	// phase.

	LinearPrinter.println("  visiting shift expression: " + self);
	LinearPrinter.println("   left: " + left);
	LinearPrinter.println("   right: " + right);
	

	// try and figure out what the right hand side of the expression is.
	LinearForm rightForm = (LinearForm)right.accept(this);

	// if the right side is non linear, give up. Accept the left side
	// (to ensure any side effects are taken into account) and return null
	if (right == null) {
	    left.accept(this);
	    return null;
	}
	
	// if the right side is a constant, figure it out at compile time,
	// and accept the equivalant multiplication or division.
	if (rightForm.getOffset().isReal() && rightForm.getOffset().isIntegral()) {
	    int rightInt = (int)rightForm.getOffset().getReal(); // get the real part
	    // calculate the power of two.
	    // this probably won't handle _large_ powers of two.
	    int rightPowerOfTwo = 1 << rightInt;
	    // if this is a left shift, accept a multiplication
	    if (oper == OPE_SL) {
		return (new JMultExpression(null, left, new JIntLiteral(rightPowerOfTwo))).accept(this);
		// if this is a right shift, accept a division
	    } else if (oper == OPE_SR) {
		return (new JDivideExpression(null, left, new JIntLiteral(rightPowerOfTwo))).accept(this);
	    } else {
		throw new RuntimeException("Unknown operator in ShiftExpression:" + oper);
	    }
	} else {
	    // merely accept the left and return
	    left.accept(this);
	    return null;
	}
    }
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

    /* visit a unary minus expression to negate a value. */
    public Object visitUnaryMinusExpression(JUnaryExpression self, JExpression expr){
	/* visit the internal expression to see if it is linear. */
	LinearForm exprForm = (LinearForm)expr.accept(this);
	if (exprForm != null) {
	    // if it was linear, multiply it my -1 and return
	    return exprForm.negate();
	} else {
	    return exprForm;
	}
    }
	
	    
//     public Object visitUnaryPlusExpression(JUnaryExpression self, JExpression expr){return null;}
    /** Visits a unary promote expression, which is basically a type cast.
     * Since we aren't really worried about
     * type casts in the linear analysis, a promote
     * expression basically just visits the expression.
     **/
    public Object visitUnaryPromoteExpression(JUnaryPromote self, JExpression expr, CType type){
	return expr.accept(this);
    }
    
    
    //     public Object visitUnqualifiedAnonymousCreation(JUnqualifiedAnonymousCreation self,
// 						    CClassType type, JExpression[] params,
// 						    JClassDeclaration decl){return null;}
//     public Object visitUnqualifiedInstanceCreation(JUnqualifiedInstanceCreation self, CClassType type,
// 						   JExpression[] params){return null;}
//     public Object visitVariableDeclarationStatement(JVariableDeclarationStatement self,
// 						    JVariableDefinition[] vars){return null;}
//     public Object visitVariableDefinition(JVariableDefinition self, int modifiers,
// 					  CType type, String ident, JExpression expr){return null;}
    /**
     * Visits a while statement. While statements are not currently
     * analyzed because if the constprop/unroller couldn't handle them
     * then they are unlikely to do linear things and hence this filter is
     * most probably non-linear.
     **/
    public Object visitWhileStatement(JWhileStatement self, JExpression cond, JStatement body){
	this.nonLinearFlag = true;	
	LinearPrinter.warn("Not yet implemented -- while statements are not yet implemented");
	throw new NonLinearException("While statements are not implemented.");
    }



    ///// SIR Constructs that are interesting for linear analysis (eg push, pop, and peek expressions)
    
    /**
     * Visit a push expression. When we visit a push expression, we are basically going to try and
     * resolve the argument expression into linear form. If the argument
     * resolves into linear form, then we are golden -- we make a note of the fact
     * by copying the linear form into the appropriate row of C and the
     * appropriate row of D.
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
	    LinearPrinter.println("  push argument wasn't linear: " + arg);
	    throw new NonLinearException("push argument wasn't linear");
	} else {
	    // (note that the first push ends up in the topmost row)
	    // so we calculate which row that this push statement corresponds to
	    int pushRow = this.pushOffset;
	    // we have a linear form, so we update the matrix representation
	    argLinearForm.copyInputsToRow(this.D, pushRow);
	    argLinearForm.copyStatesToRow(this.C, pushRow);
	}

	// increment the push offset (for the next push statement)
	this.pushOffset++;

	LinearPrinter.println("   (current push offset: " + this.pushOffset);

	// make sure we didn't screw up anything.
	checkRep();
	// push expressions don't return values, so they also certainly don't return linear forms
	return null;
    }

    /**
     * Visit a pop expression.
     * A pop expression generates a linear form based on the current offset, and then
     * updates the current offset. The basic idea is that a pop expression represents using one
     * one of the input values from the tapes, and therefore should correspond to a linear form
     * with a "1" at the appropriate place in either the inputs or states vector.
     * if peekOffset >= peekSize - popSize, then it is an input
     * otherwise, it is a state
     **/
    public Object visitPopExpression(SIRPopExpression self, CType tapeType) {
	LinearPrinter.println("  visiting pop expression: " + self);
	// A pop expression is one of the base cases for creating LinearForms
	// the pop expression will creates a linear form that corresponds to using
	// a peek at the current offset, which in turn corresponds to a
	// use of the element at size-peekoffset-1 in the input vector

	if (this.peekSize - this.peekOffset < 0) {
	    LinearPrinter.warn("Too many pops detected!");
	    this.nonLinearFlag = true;
	    throw new NonLinearException("too many pops detected");
	}
	
	LinearForm currentForm = this.getBlankLinearForm();

        if(this.peekOffset >= this.peekSize - this.popSize) {
	  int inputIndex = this.peekOffset - (this.peekSize - this.popSize);
  	  currentForm.setInputWeight(inputIndex, ComplexNumber.ONE);
        } 
        else	
	    currentForm.setStateWeight(this.peekOffset, ComplexNumber.ONE);

	// when we hit a pop expression, all further peek expressions have their
	// indicies incremented by one compared to the previous expressions
	this.peekOffset++;

	// return the linear form of the pop expression
	//LinearPrinter.println("  returning " + currentForm + " from pop expression");
	LinearPrinter.println("  returning linear form from pop expression");
	return currentForm;
    }

    /**
     * Visit a peek expression.
     * Peek expressions are also base expressions that generate linear forms.
     * The peek index is transformed into a "1" in the appropriate place in
     * the appropriate vector of the returned linear form. We also have to keep track of
     * the case when there have been previous pops which change the relative position of the
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
	// but unresolvable because the index is computed with input data.
	if (!exprLinearForm.isIntegerOffset()) {
	    return null;
	}

	// otherwise, create a new linear form that represents which input value that this
	// peek expression produces.
	// basically, it will be a linear form that is all zeros in its weights
	// except for a 1 in the index corresponding to the data item that this peek expression
	// accesses	
	LinearForm peekExprLinearForm = this.getBlankLinearForm();

        int currPeek = exprLinearForm.getIntegerOffset();
        
        if(currPeek + this.peekOffset >= this.peekSize - this.popSize) {
	    int inputIndex = currPeek + this.peekOffset - (this.peekSize - this.popSize);
	    peekExprLinearForm.setInputWeight(inputIndex, ComplexNumber.ONE);
        }
        else
	    peekExprLinearForm.setStateWeight(currPeek + this.peekOffset, ComplexNumber.ONE);

	LinearPrinter.println("  returning linear form from peek expression.");
	//LinearPrinter.println("  returning linear form from peek expression: " + peekExprLinearForm);
	return peekExprLinearForm;
    }


    ////// Generators for literal expressions
    
    /**
     * Visit boolean literal. Boolean logic falls outside the
     * realm of linear filter analysis -- return null.
     **/
    public Object visitBooleanLiteral(JBooleanLiteral self,boolean value) {return null;}
    /** Visit a byte literal. Create the appropriate valued offset. **/
    public Object visitByteLiteral(JByteLiteral self, byte value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** Visit a character literal. Create the appropriate valued offset **/
    public Object visitCharLiteral(JCharLiteral self,char value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** Visit a double. Create the appropriate valued offset. **/
    public Object visitDoubleLiteral(JDoubleLiteral self,double value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** Visit a float. Create the appropriate valued offset. **/
    public Object visitFloatLiteral(JFloatLiteral self,float value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** Visit an int. Create the appropriate valued offset. **/
    public Object visitIntLiteral(JIntLiteral self, int value) {
	return this.getOffsetLinearForm((double)value);
    }
    /**  Visit a long. Create the appropriate valued offset. **/
    public Object visitLongLiteral(JLongLiteral self,long value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** Visit a short. Create the appropriate valued offset. **/
    public Object visitShortLiteral(JShortLiteral self,short value) {
	return this.getOffsetLinearForm((double)value);
    }
    /** Visit a string (don't handle). We can't deal with strings, not linear, return null **/
    public Object visitStringLiteral(JStringLiteral self,String value) {
	return null;
    }
    /**
     * Visit a null literal (don't handle). If we have null nonsense, the expression
     * is not going to be linear. Return null (how appropriate).
     **/
    public Object visitNullLiteral(JNullLiteral self) {
	return null;
    }

    /**
     * Removes the mapping of a local variable expression or a field access expression
     * from the variablesToLinearForm mapping. Throws an exception if we try to remove
     * a mapping to a type that we don't know about
     **/
    public void removeMapping(JExpression expr) {
	// wrap the field access expression
	AccessWrapper theWrapper = AccessWrapperFactory.wrapAccess(expr);
	if (theWrapper != null) {
	    LinearPrinter.println("   removing mapping for : " + expr);
	    // actually remove the data from the hash map
	    this.variablesToLinearForms.remove(theWrapper);
	} else {
	    LinearPrinter.println("   no previous mapping for: " + expr);
	}
    }
    
    /**
     * Creates a blank linear form that is appropriate for this filter (i.e.
     * it has size of peekSize.
     **/
    private LinearForm getBlankLinearForm() {
	checkRep();
	return new LinearForm(this.popSize, this.stateSize);
    }

    /** Creates a blank linear form that has the specified offset. **/
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
	// make sure that the matrices is the correct size
	if (this.stateSize != this.A.getRows()) {
	    throw new RuntimeException("Inconsistent matrix A representation, rows");
	}
	if (this.stateSize != this.A.getCols()) {
	    throw new RuntimeException("Inconsistent matrix A representation, cols");
	}
	if (this.stateSize != this.B.getRows()) {
	    throw new RuntimeException("Inconsistent matrix B representation, rows");
	}
	if (this.popSize != this.B.getCols()) {
	    throw new RuntimeException("Inconsistent matrix B representation, cols");
	}
	if (this.pushSize != this.C.getRows()) {
	    throw new RuntimeException("Inconsistent matrix C representation, rows");
	}
        if (this.stateSize != this.C.getCols()) {
	    throw new RuntimeException("Inconsistent matrix C representation, cols");
	}
	if (this.popSize != this.D.getCols()) {
	    throw new RuntimeException("Inconsistent matrix D representation, cols");
	}
	if (this.pushSize != this.D.getRows()) {
	    throw new RuntimeException("Inconsistent matrix D representation, rows");
	}

	// check that the only values in the HashMap are LinearForm objects
	Iterator keyIter = this.variablesToLinearForms.keySet().iterator();
	while(keyIter.hasNext()) {
	    Object key = keyIter.next();
	    if (key == null) {throw new RuntimeException("Null key in linear form map");}
	    // make sure that they key is a JLocalVariable or a JFieldAccessExpression
	    if (!(key instanceof AccessWrapper)) {
		throw new RuntimeException("Non access wrapper in linear form map.");
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
		throw new RuntimeException("Filter (" + this.filterName +
					   ") pops more than peeks:" +
					   "peekSize: " + this.peekSize + " " +
					   "peekOffset: " + this.peekOffset);
	    }
	}
	// make sure that the number of pushes that we have seen doesn't go past the end of
	// the matrix/vector that represents this filter.
	if (this.pushOffset > this.D.getRows()) {
	    throw new RuntimeException("Filter (" + this.filterName +
				       ") pushes more items " + 
				       "than is declared (" + this.D.getRows());
	}	    
    }    
}





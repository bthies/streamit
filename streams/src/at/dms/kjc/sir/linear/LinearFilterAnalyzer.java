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
 * $Id: LinearFilterAnalyzer.java,v 1.4 2002-08-20 19:12:47 aalamb Exp $
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




/**
 * A visitor class that goes through all of the expressions in the work function
 * of a filter to determine if the filter is linear and if it is what matrix
 * corresponds to the filter.
 **/
class LinearFilterVisitor extends SLIREmptyAttributeVisitor {
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
     * Mappings from expressions to LinearForms. Each LinearForm holds the
     * affine representation that maps the expression to a combination of
     * inputs (eg peek expressions indexes);
     **/
    private HashMap expressionsToLinearForms;

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
	this.expressionsToLinearForms = new HashMap();
	this.peekOffset = 0;
	this.pushOffset = 0;
	this.representationMatrix = new FilterMatrix(numPeeks, numPushes);
	this.representationVector = new FilterVector(numPeeks);
	this.nonLinearFlag = false;
	checkRep();

    }

    /** Returns true of the filter computes a linear function. **/
    public boolean computesLinearFunction() {
	return !this.nonLinearFlag;
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
//     public Object visitArrayAccessExpression(JArrayAccessExpression self,
// 					     JExpression prefix,
// 					     JExpression accessor) {return null;}
//     public Object visitArrayInitializer(JArrayInitializer self, JExpression[] elems){return null;}
//     public Object visitArrayLengthExpression(JArrayLengthExpression self, JExpression prefix) {return null;}

    public Object visitAssignmentExpression(JAssignmentExpression self, JExpression left, JExpression right) {
	LinearPrinter.println("  visiting assignment expression: " + self);
	return super.visitAssignmentExpression(self, left, right);
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
	// for some reason, JAddExpressions implement all of + - * / statements
	// and I can't seem to figure out where the constants to recognize the
	// operators are. Therefore, I am going to hard code in the strings. Sorry about that.
	if (!(self instanceof JAddExpression)) {
	    return null;
	}
	// if we are computing an additon or subtraction, we are all set, otherwise
	// we are done
	if (!(oper.equals("+") || oper.equals("-"))) {
	    LinearPrinter.println("  can't process " + oper + " linearly");
	    return null;
	}

	LinearPrinter.println("  visiting JAddExpression(" + oper + ")");
	
	// first of all, try and figure out if left and right sub expression can
	// be represented in linear form.
	LinearForm leftLinearForm  = (LinearForm)left.accept(this);
	LinearForm rightLinearForm = (LinearForm)right.accept(this);

	// if both the left and right are non null, we are golden and can combine these two,
	// otherwise give up.
	if ((leftLinearForm == null) || (rightLinearForm == null)) {
	    return null;
	}
	// if the operator is subtraction, negate the right expression
	if (oper.equals("-")) {
	    leftLinearForm = leftLinearForm.negate();
	}

	// now, add the two forms together and return the resulut
	LinearForm combinedLinearForm = leftLinearForm.plus(rightLinearForm);
	    
	return combinedLinearForm;
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
//     public Object visitCompoundAssignmentExpression(JCompoundAssignmentExpression self,
// 						    int oper, JExpression left,
// 						    JExpression right){return null;}
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
//     public Object visitFieldExpression(JFieldAccessExpression self, JExpression left, String ident){return null;}
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
//     public Object visitLocalVariableExpression(JLocalVariableExpression self, String ident){return null;}
//     public Object visitLogicalComplementExpression(JUnaryExpression self, JExpression expr){return null;}
//     public Object visitMethodCallExpression(JMethodCallExpression self, JExpression prefix,
// 					    String ident, JExpression[] args){return null;}
//     public Object visitMethodDeclaration(JMethodDeclaration self, int modifiers,
// 					 CType returnType, String ident,
// 					 JFormalParameter[] parameters, CClassType[] exceptions,
// 					 JBlock body){return null;}
//     public Object visitNameExpression(JNameExpression self, JExpression prefix, String ident){return null;}
//     public Object visitNewArrayExpression(JNewArrayExpression self, CType type,
// 					  JExpression[] dims, JArrayInitializer init){return null;}
//     public Object visitPackageImport(String name){return null;}
//     public Object visitPackageName(String name){return null;}
//     public Object visitParenthesedExpression(JParenthesedExpression self, JExpression expr){return null;}
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
	    // increment the push offset (for the next push statement)
	    this.pushOffset++;
	}
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

	// if the offset is not an integer, something is very wrong...
	if (!exprLinearForm.isIntegerOffset()) {
	    throw new RuntimeException("Can't have a non integer offset in a peek expression...");
	}

	// otherwise, create a new linear form that represents which input value that this
	// peek expression produces.
	// basically, it will be a linear form that is all zeros in its weights
	// except for a 1 in the index corresponding to the data item that this peek expression
	// accesses
	LinearForm peekExprLinearForm = this.getBlankLinearForm();
	peekExprLinearForm.setWeight(exprLinearForm.getIntegerOffset(),
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
    ////// Literal processing handlers

    
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
    return lf;
}

    /**
     * Check the representation invariants of the LinearFilterVisitor.
     **/
    private void checkRep() {
	// check that the only values in the HashMap are LinearForm objects
	Iterator valIter = this.expressionsToLinearForms.values().iterator();
	while(valIter.hasNext()) {
	    Object o = valIter.next();
	    if (o == null) {throw new RuntimeException("Null object in value map");}
	    if (!(o instanceof LinearForm)) {throw new RuntimeException("Non LinearForms in value map");}
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
}
    

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
 * $Id: LinearFilterAnalyzer.java,v 1.2 2002-08-15 20:38:04 aalamb Exp $
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
     **/
    public static LinearFilterAnalyzer findLinearFilters(SIRStream str) {
	System.out.println("aal--In linear filter visitor");
	LinearFilterAnalyzer lfv = new LinearFilterAnalyzer();
	IterFactory.createIter(str).accept(lfv);
	return lfv;
    }
    

    /** More or less get a callback for each stram **/
    public void visitFilter(SIRFilter self, SIRFilterIter iter) {
	System.out.println("Visiting " + "(" + self + ")");

	// set up the visitor that will actually collect the data
	int peekRate = extractInteger(self.getPeek());
	System.out.println("  Peek rate: " + peekRate);
	LinearFilterVisitor theVisitor = new LinearFilterVisitor(peekRate);

	// pump the visitor through the work function
	// (we might need to send it thought the init function as well so that
	//  we can determine the initial values of fields. However, I think that
	//  field prop is supposed to take care of this.)
	self.getWork().accept(theVisitor);
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
     * Flag that is set when we detect that something blatently non-linear is
     * computed by the filter (eg <pre>push(peek(1)*peek(2);</pre>)
     * Starts off as false, and is set to true if we hit a statement that
     * makes the filter non-linear.
     **/
    private boolean nonLinearFlag;
    
    /**
     * Create a new LinearFilterVisitor which is looking to figure out 
     * how to compute, for all variables, linear forms from the input.
     * Also creates a LinearFilterRepresentation if the
     * filter computes a linear function.
     **/
    public LinearFilterVisitor(int numPeeks) {
	this.peekSize = numPeeks;
	this.expressionsToLinearForms = new HashMap();
	this.peekOffset = 0;
	this.nonLinearFlag = false;
	checkRep();
    }



    /////// So the deal with this attribute visitor is that all of its methods that visit
    /////// expressions that are some sort of linear (or affine) calculation on the inputs
    /////// the method returns a LinearForm. They return null otherwise.
    

    public Object visitAssignmentExpression(JAssignmentExpression self, JExpression left, JExpression right) {
	System.out.println("  visiting assignment expression: " + self);
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
	    System.out.println("  can't process " + oper + " linearly");
	    return null;
	}

	System.out.println("  visiting JAddExpression(" + oper + ")");
	
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

    
    public Object visitMethodCallExpression(JMethodCallExpression self,
					    JExpression prefix,
					    String ident,
					    JExpression[] args) {
	System.out.println("  visiting method call expression: " + self);
	return super.visitMethodCallExpression(self, prefix, ident, args);
    }

    public Object visitPushExpression(SIRPushExpression self, CType tapeType, JExpression arg) {
	System.out.println("  visiting push expression: " +
			   "argument: " + arg);
	return super.visitPushExpression(self, tapeType, arg);
    }

    public Object visitPopExpression(SIRPopExpression self, CType tapeType) {
	System.out.println("  visitng pop expression: " + self);

	// A pop expression is one of the base cases for creating LinearForms
	// the pop expression will creates a linear form that corresponds to using
	// a peek at the current offset.
	LinearForm currentForm = this.getBlankLinearForm();
	currentForm.setWeight(this.peekOffset, ComplexNumber.ONE);
	
	// when we hit a pop expression, all further peek expressions have their
	// indicies incremented by one compared to the previous expressions
	this.peekOffset++;

	// return the linear form of the pop expression
	System.out.println("  returning " + currentForm + " from pop expression");
	return currentForm;
    }

    public Object visitPeekExpression(SIRPeekExpression self, CType tapeType, JExpression arg) {
	System.out.println("  visiting peek expression" +
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
	System.out.println("  returning linear form from peek expression: " + peekExprLinearForm);
	return peekExprLinearForm;
    }


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
	    if (this.peekOffset >= this.peekSize) {
		throw new RuntimeException("Filter pops more than peeks:" +
					   "peekSize: " + this.peekSize + " " +
					   "peekOffset: " + this.peekOffset);
	    }
	}
	    
    }

}

package at.dms.kjc.sir;

import at.dms.kjc.lir.LIRStreamType;
import at.dms.kjc.*;
import at.dms.util.*;

/**
 * This represents a StreaMIT filter.
 */
public class SIRFilter extends SIRStream implements Cloneable {
    /**
     * The number of items that are peeked per invocation.  This
     * number includes the items that are popped; i.e. if a filter
     * pops two and peeks 3, then it looks at a total of 3 elements,
     * not 5 elements.
     */
    protected JExpression peek;
    /**
     * The number of items that are popped per invocation.
     */
    protected JExpression pop;
    /**
     * The number of items that are pushed per invocation.
     */
    protected JExpression push;
    /**
     * The input and output types.  That is, the type of the items on
     * the input and output channels, respectively.  Each type is void
     * if and only if this is a source or sink, respectively.
     */
    private CType inputType, outputType;

    public SIRFilter() {
	this(null);
    }

    public SIRFilter(String ident) {
	super(null, ident, JFieldDeclaration.EMPTY(), JMethodDeclaration.EMPTY());
	this.pop = new JIntLiteral(0);
	this.push = new JIntLiteral(0);
	this.peek = new JIntLiteral(0);
    }
    
    public SIRFilter(SIRContainer parent,
		     String ident,
		     JFieldDeclaration[] fields, 
		     JMethodDeclaration[] methods, 
		     JExpression peek, JExpression pop, JExpression push, 
		     JMethodDeclaration work, 
		     CType inputType, 
		     CType outputType) {
	super(parent, ident, fields, methods);
	this.peek = peek;
	this.pop = pop;
	this.push = push;
	this.inputType = inputType;
	this.outputType = outputType;
	// make this call to ensure that <work> is in <methods> array
	if (work!=null) {
	    setWork(work);
	}
    }

    /**
     * Returns the type of this stream.
     */
    public LIRStreamType getStreamType() {
	return LIRStreamType.LIR_FILTER;
    }

    /**
     * Copies the state of filter <other> into this.  Fields that are
     * objects will be shared instead of cloned.
     */
    public void copyState(SIRFilter other) {
	this.pop = other.pop;
	this.push = other.push;
	this.peek = other.peek;
	this.work = other.work;
	this.init = other.init;
	this.inputType = other.inputType;
	this.outputType = other.outputType;
	this.parent = other.parent;
	this.fields = other.fields;
	this.methods = other.methods;
	this.ident = other.ident;
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitFilter(this,
			     fields,
			     methods,
			     init,
			     work,
			     inputType, outputType);
    }


    public void setPeek(JExpression p) {
	this.peek = p;
    }

    public void setPop(JExpression p) {
	this.pop = p;
    }
    public void setPush(JExpression p) {
	this.push = p;
    }

    public void setPeek(int p) {
	this.peek = new JIntLiteral(p);
    }

    public void setPush(int p) {
	this.push = new JIntLiteral(p);
    }

    public void setPop(int p) {
	this.pop = new JIntLiteral(p);
    }

    public JExpression getPush() {
	return this.push;
    }

    public JExpression getPeek() {
	return this.peek;
    }

    public JExpression getPop() {
	return this.pop;
    }

    /**
     * Returns how many items are popped.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPop.
     */
    public int getPopInt() {
      if (pop instanceof JFloatLiteral) { //clleger
	pop = new JIntLiteral(null, (int) ((JFloatLiteral)pop).floatValue());
      }
	// need int literal to get number
	if (!(pop instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for pop value in filter " + this.getName() + ", but the constant hasn't been resolved yet. " + pop);
	}
	return ((JIntLiteral)pop).intValue();
    }

    /**
     * Returns how many items are peeked.  This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPeek.
     */
    public int getPeekInt() {
      if (peek instanceof JFloatLiteral) { //clleger
	peek = new JIntLiteral(null, (int) ((JFloatLiteral)peek).floatValue());
      }
	// need int literal to get number
	if (!(peek instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for peek value in filter " + this.getIdent() + ", but the constant hasn't been resolved yet. " + peek);
	}
	return ((JIntLiteral)peek).intValue();
    }

    /**
     * Returns how many items are pushed.This will throw an
     * exception if the integral numbers haven't been calculated
     * yet--in this case one can only get the JExpression, but calling
     * getPush.
     */
    public int getPushInt() {
	// need int literal to get number
      if (push instanceof JFloatLiteral) { //clleger
	push = new JIntLiteral(null, (int) ((JFloatLiteral)push).floatValue());
      }

	if (!(push instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for push value in filter " + this.getIdent() + ", but the constant hasn't been resolved yet. " + push);
	}
	return ((JIntLiteral)push).intValue();
    }

    public void setInputType(CType t){
	this.inputType = t;
    }
    public CType getInputType(){
	return inputType;
    }

    public void setOutputType(CType t) {
	this.outputType = t;
    }
    public CType getOutputType() {
	return this.outputType;
    }

    public String toString() {
	return "SIRFilter name=" + getName() + " ident=" + getIdent();
    }
}



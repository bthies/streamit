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
    private JExpression peek;
    /**
     * The number of items that are popped per invocation.
     */
    private JExpression pop;
    /**
     * The number of items that are pushed per invocation.
     */
    private JExpression push;
    /**
     * The input and output types.  That is, the type of the items on
     * the input and output channels, respectively.
     */
    private CType inputType, outputType;

    public SIRFilter() {
	super();
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
      * Return a shallow clone of the SIRFilter
     */
    public Object clone() 
    {
	SIRFilter f = new SIRFilter(this.parent,
				    this.ident,
				    this.fields,
				    this.methods,
				    this.peek,
				    this.pop,
				    this.push,
				    this.work,
				    this.inputType,
				    this.outputType);
	f.setInit(this.init);
	return f;
    }

    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(StreamVisitor v) {
	v.visitFilter(this,
		      parent,
		      fields,
		      methods,
		      init,
		      work,
		      inputType, outputType);
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitFilter(this,
			     parent,
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
	// need int literal to get number
	if (!(pop instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for pop value, but the constant hasn't been resolved yet.  It is of class " + pop.getClass());
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
	// need int literal to get number
	if (!(peek instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for peek value, but the constant hasn't been resolved yet.  It is of class " + peek.getClass());
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
	if (!(push instanceof JIntLiteral)) {
	    Utils.fail("Trying to get integer value for push value, but the constant hasn't been resolved yet.  It is of class " + push.getClass());
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
}



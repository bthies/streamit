package at.dms.kjc.sir;

import at.dms.kjc.*;

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
    private int peek;
    /**
     * The number of items that are popped per invocation.
     */
    private int pop;
    /**
     * The number of items that are pushed per invocation.
     */
    private int push;
    /**
     * The work function.
     */
    private JMethodDeclaration work;
    /**
     * The input and output types.  That is, the type of the items on
     * the input and output channels, respectively.
     */
    private CType inputType, outputType;

    public SIRFilter(SIRStream parent,
		     JFieldDeclaration[] fields, 
		     JMethodDeclaration[] methods, 
		     int peek, int pop, int push, 
		     JMethodDeclaration work, 
		     CType inputType, 
		     CType outputType) {
	super(parent, fields, methods);
	this.peek = peek;
	this.pop = pop;
	this.push = push;
	this.work = work;
	this.inputType = inputType;
	this.outputType = outputType;
    }

    /**
      * Return a shallow clone of the SIRFilter
     */
    public Object clone() 
    {
	SIRFilter f = new SIRFilter(this.parent,
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
     * Constructs a SIRFilter with null variables (use set*)
     */
    public SIRFilter() {
	super();
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
		      peek, pop, push,
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
			     peek, pop, push,
			     work,
			     inputType, outputType);
    }


    public void setPeek(int p) {
	this.peek = p;
    }
    public void setPop(int p) {
	this.pop = p;
    }
    public void setPush(int p) {
	this.push = p;
    }
    public void setWork (JMethodDeclaration w) {
	this.work = w;
	addMethod(w);
    }
    public JMethodDeclaration getWork () {
	return this.work;
    }
    public void setInputType(CType t){
	this.inputType = t;
    }
    public void setOutputType(CType t) {
	this.outputType = t;
    }
    public CType getOutputType() {
	return this.outputType;
    }
    

}



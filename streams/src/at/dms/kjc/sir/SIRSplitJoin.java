package at.dms.kjc.sir;

import at.dms.kjc.*;

/**
 * This represents a SplitJoin construct.
 */
public class SIRSplitJoin extends SIRStream implements Cloneable{
    /**
     * The splitter at the top of this.
     */
    private SIRSplitter splitter;
    /**
     * The joiner at the bottom of this.
     */
    private SIRJoiner joiner;
    /**
     * The stream components in this.  The i'th element of this array
     * corresponds to the i'th tape in the splitter and joiner.  
     */
    private SIRStream elements[];

    

    /**
     * Return a shallow clone of the SIRSplitJoin
     */
    public Object clone() {
	SIRSplitJoin s = new SIRSplitJoin(this.parent,
				       this.fields,
				       this.methods);
	s.setInit(this.init);
	s.setSplitter(this.splitter);
	s.setJoiner(this.joiner);
	return s;
    }
    
    /**
     * sets the splitter for this SplitJoin
     */
    public void setSplitter(SIRSplitter s) 
    {
	this.splitter = s;
    }
    
    /**
     * sets the joiner for this SplitJoin
     */
    public void setJoiner(SIRJoiner j) 
    {
	this.joiner = j;
    }
   

    /**
     * Returns the output type of this.
     */
    public CType getOutputType() {
	// output type should be output type of any of the elements.
	// Assume that this is checked by semantic checker, and here
	// just return the output type of the first element.
	return elements[0].getOutputType();
    }
    
    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(StreamVisitor v) {
	v.preVisitSplitJoin(this,
			    parent,
			    fields,
			    methods,
			    init);
	/* visit components */
	splitter.accept(v);
	for (int i=0; i<elements.length; i++) {
	    elements[i].accept(v);
	}
	joiner.accept(v);
	v.postVisitSplitJoin(this,
			     parent,
			     fields,
			     methods,
			     init);
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitSplitJoin(this,
				parent,
				fields,
				methods,
				init);
    }

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRSplitJoin(SIRStream parent,
			JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
	super(parent, fields, methods);

    }
     /**
     * Construct a new SIRPipeline with null fields, parent, and methods.
     */
    public SIRSplitJoin() {
	super();
    }
}

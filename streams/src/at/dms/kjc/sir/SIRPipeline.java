package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.lir.LIRStreamType;
import java.util.List;
import java.util.LinkedList;

/**
 * This represents a pipeline of stream structures, as would be
 * declared with a Stream construct in StreaMIT.
 */
public class SIRPipeline extends SIRContainer implements Cloneable {
    /**
     * The elements of the pipeline.  Each element should be an SIRStream.
     */
    private LinkedList elements;

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRPipeline(SIRContainer parent,
		       JFieldDeclaration[] fields,
		       JMethodDeclaration[] methods) {
	super(parent, fields, methods);
	// initialize elements array
	this.elements = new LinkedList();
    }

/*
     * Return a shallow clone of the SIRPipeline
     */
    public Object clone() {
	SIRPipeline p = new SIRPipeline(this.parent, this.fields,
					this.methods);
	p.setInit(this.init);
	for(int i = 0; i < elements.size(); i++) 
	    p.add((SIRStream)this.elements.get(i));
	return p;
    }
    

    /**
     * Returns the output type of this.
     */
    public CType getOutputType() {
	// output type is output type of last element in list
	return ((SIRStream)elements.getLast()).getOutputType();
    }
    
    /**
     * Returns the type of this stream.
     */
    public LIRStreamType getStreamType() {
	return LIRStreamType.LIR_PIPELINE;
    }

    /**
     * Returns the input type of this.
     */
    public CType getInputType() {
	// input type is input type of first element of the list
	return ((SIRStream)elements.getFirst()).getInputType();
    }
    
    /**
     * Returns the relative name by which this object refers to child
     * <child>, or null if <child> is not a child of this.
     */
    public String getChildName(SIROperator str) {
	// return "stream" + (x+1), where x is the index of <str> in this pipe
	int index = elements.indexOf(str);
	if (index==-1) {
	    return null;
	} else {
	    return "stream" + (index+1);
	}
    }
    
    /**
     * Returns a list of the children of this.  The children are
     * stream objects that are contained within this.
     */
    public List getChildren() {
	// the children are just the components of the pipeline
	return (List)elements.clone();
    }

    /**
     * Returns a list of tuples (two-element arrays) of SIROperators,
     * representing a tape from the first element of each tuple to the
     * second.
     */
    public List getTapePairs() {
	// construct result
	LinkedList result = new LinkedList();
	// go through list of elements
	for (int i=0; i<elements.size()-1; i++) {
	    // make an entry from one stream to next
	    SIROperator[] entry = { (SIROperator)elements.get(i),
				    (SIROperator)elements.get(i+1) };
	    // add entry 
	    result.add(entry);
	}
	// return result
	return result;
    }

    /**
     * Add a stream to the pipeline.
     */
    public void add(SIRStream str) {
	elements.add(str);
    }

    /**
     * Return i'th stream in this pipeline.
     */
    public SIRStream get(int i) {
	return (SIRStream)elements.get(i);
    }

    /**
     * Returns the index of <str> in this pipeline, or -1 if <str>
     * does not appear in this.
     */
    public int indexOf(SIRStream str) {
	return elements.indexOf(str);
    }

    /**
     * Returns the number of substreams in this.
     */
    public int size() {
	return elements.size();
    }

    /**
     * Accepts visitor <v> at this node.
     */
    public void accept(StreamVisitor v) {
	v.preVisitPipeline(this,
			   parent,
			   fields,
			   methods,
			   init,
			   elements);
	/* visit components */
	for (int i=0; i<elements.size(); i++) {
	    ((SIRStream)elements.get(i)).accept(v);
	}
	v.postVisitPipeline(this,
			    parent,
			    fields,
			    methods,
			    init,
			    elements);
    }


    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitPipeline(this,
			       parent,
			       fields,
			       methods,
			       init,
			       elements);
    }
}


package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.lir.LIRStreamType;
import java.util.List;
import java.util.LinkedList;

/**
 * This represents a SplitJoin construct.
 */
public class SIRSplitJoin extends SIRContainer implements Cloneable {
    /**
     * The splitter at the top of this.
     */
    private SIRSplitter splitter;
    /**
     * The joiner at the bottom of this.
     */
    private SIRJoiner joiner;
    /**
     * The stream components in this.  The i'th element of this list
     * corresponds to the i'th tape in the splitter and joiner.  
     */
    private LinkedList elements;

    

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
	return ((SIRStream)elements.getFirst()).getOutputType();
    }
    
    /**
     * Returns the input type of this.
     */
    public CType getInputType() {
	// input type should be input type of any of the elements.
	// Assume that this is checked by semantic checker, and here
	// just return the input type of the first element.
	return ((SIRStream)elements.getFirst()).getInputType();
    }
    
    /**
     * Returns the type of this stream.
     */
    public LIRStreamType getStreamType() {
	return LIRStreamType.LIR_SPLIT_JOIN;
    }

    /**
     * Returns the relative name by which this object refers to child
     * <child>, or null if <child> is not a child of this.
     */
    public String getChildName(SIROperator str) {
	int index = elements.indexOf(str);
	if (index>=0) {
	    // return stream index if it's a stream
	    return "stream" + (index+1);
	} else if (str==joiner) {
	    // return joiner if joiner
	    return "joiner";
	} else if (str==splitter) {
	    // return splitter if splitter
	    return "splitter";
	} else {
	    // otherwise, <str> is not a child--return null
	    return null;
	}
    }

    /**
     * Returns a list of the children of this.  The children are
     * stream objects that are contained within this.
     */
    public List getChildren() {
	// build result
	LinkedList result = new LinkedList();
	// add the children: the component streams, plus the
	// splitter and joiner
	result.addAll(elements);
	result.add(splitter);
	result.add(joiner);
	// return result
	return result;
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
	    // make an entry from splitter to each stream
	    SIROperator[] entry1 = { splitter, (SIROperator)elements.get(i) };
	    // make an entry from each stream to splitter
	    SIROperator[] entry2 = { (SIROperator)elements.get(i), joiner };
	    // add entries
	    result.add(entry1);
	    result.add(entry2);
	}
	// return result
	return result;
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
	for (int i=0; i<elements.size(); i++) {
	    ((SIRStream)elements.get(i)).accept(v);
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
				init,
				elements,
				splitter,
				joiner);
    }

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRSplitJoin(SIRContainer parent,
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

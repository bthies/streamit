package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
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
     * Return a shallow clone of the SIRSplitJoin
     */
    public Object clone() {
	SIRSplitJoin s = new SIRSplitJoin(this.parent,
					  this.ident,
					  this.fields,
					  this.methods);
	s.setInit(this.init);
	s.setSplitter(this.splitter);
	s.setJoiner(this.joiner);
	return s;
    }

    /**
     * Whether or not <str> is an immediate child of this.
     */
    public boolean contains(SIROperator str) {
	return children.contains(str);
    }

    /**
     * returns i'th child of this.
     */
    public SIRStream get(int i) {
	return (SIRStream)children.get(i);
    }
    
    /**
     * returns the number of parallel streams in this.
     */
    public int size() {
	return children.size();
    }
    
    /**
     * sets the splitter for this SplitJoin
     */
    public void setSplitter(SIRSplitter s) 
    {
	this.splitter = s;
    }
    
    /**
     * gets the splitter.
     */
    public SIRSplitter getSplitter() 
    {
	Utils.assert(this.splitter!=null);
	return this.splitter;
    }
    
    /**
     * sets the joiner for this SplitJoin
     */
    public void setJoiner(SIRJoiner j) 
    {
	this.joiner = j;
    }
   
    /**
     * gets the joinger.
     */
    public SIRJoiner getJoiner() 
    {
	Utils.assert(this.splitter!=null);
	return this.joiner;
    }
    

    /**
     * Returns the output type of this.
     */
    public CType getOutputType() {
	// first look for a non-null type (since some of them might
	// not be feeding into the joiner)
	for (int i=0; i<children.size(); i++) {
	    CType type = ((SIRStream)children.get(i)).getOutputType();
	    if (type!=CStdType.Null) {
		return type;
	    }
	}
	// otherwise, they're all null, so return null
	return CStdType.Null;
    }
    
    /**
     * Returns the input type of this.
     */
    public CType getInputType() {
	// first look for a non-null type (since some of them might
	// not be reading in from the splitter)
	for (int i=0; i<children.size(); i++) {
	    CType type = ((SIRStream)children.get(i)).getInputType();
	    if (type!=CStdType.Null) {
		return type;
	    }
	}
	// otherwise, they're all null, so return null
	return CStdType.Null;
    }

    
    /**
     * Add a stream to the SplitJoin, and set <str>'s parent field to this.
     */
    public void add(SIRStream str) {
	children.add(str);
	str.setParent(this);
    }
    
    /**
     * See documentation in SIRContainer.
     */
    public void replace(SIRStream oldStr, SIRStream newStr) {
	int index = children.indexOf(oldStr);
	Utils.assert(index!=-1,
		     "Trying to replace with bad parameters, since " + this
		     + " doesn't contain " + oldStr);
	children.set(index, newStr);
	// set parent of <newStr> to be this
	newStr.setParent(this);
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
	int index = children.indexOf(str);
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
     * stream objects that are contained within this (including the
     * splitter and joiner.
     */
    public List getChildren() {
	// build result
	LinkedList result = new LinkedList();
	// add the children: the component streams, plus the
	// splitter and joiner
	result.add(splitter);
	result.addAll(children);
	result.add(joiner);
	// return result
	return result;
    }

    /**
     * Returns a list of the parallel streams in this.
     */
    public List getParallelStreams() {
	return (List)children.clone();
    }

    /**
     * Sets the parallel streams in this, and resets the count on the
     * splitters and joiners, if they depended on the number of
     * <children> before.
     */
    public void setParallelStreams(LinkedList children) {
	// reset children
	this.children.clear();
	for (int i=0; i<children.size(); i++) {
	    add((SIRStream)children.get(i));
	}
	this.rescale();
    }

    // reset splits and joins to have right number of children.
    public void rescale() {
	this.splitter.rescale(children.size());
	this.joiner.rescale(children.size());
    }

    /**
     * Returns a list of tuples (two-element arrays) of SIROperators,
     * representing a tape from the first element of each tuple to the
     * second.
     */
    public List getTapePairs() {
	// construct result
	LinkedList result = new LinkedList();
	// go through list of children
	for (int i=0; i<children.size()-1; i++) {
	    // make an entry from splitter to each stream
	    SIROperator[] entry1 = { splitter, (SIROperator)children.get(i) };
	    // make an entry from each stream to splitter
	    SIROperator[] entry2 = { (SIROperator)children.get(i), joiner };
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
	for (int i=0; i<children.size(); i++) {
	    ((SIRStream)children.get(i)).accept(v);
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
				children,
				splitter,
				joiner);
    }

    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRSplitJoin(SIRContainer parent,
			String ident,
			JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
	super(parent, ident, fields, methods);
    }
     /**
     * Construct a new SIRPipeline with null fields, parent, and methods.
     */
    public SIRSplitJoin() {
	super();
    }
}

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
	for (int i=0; i<size(); i++) {
	    CType type = get(i).getOutputType();
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
	boolean isVoid=false;
	for (int i=0; i<size(); i++) {
	    CType type = get(i).getInputType();
	    if (type!=CStdType.Null) {
		if(type==CStdType.Void)
		    isVoid=true;
		else
		    return type;
	    }
	}
	// otherwise, they're all null (or void)
	if(isVoid)
	    return CStdType.Void;
	else
	    return CStdType.Null;
    }

    
    /**
     * Sets the parallel streams in this, and resets the count on the
     * splitters and joiners, if they depended on the number of
     * <children> before.  Only clears the argument list if there are
     * a different number of streams than before.
     */
    public void setParallelStreams(LinkedList children) {
	if (size()==children.size()) {
	    // same size
	    for (int i=0; i<children.size(); i++) {
		set(i, (SIRStream)children.get(i));
	    }
	} else {
	    // not same size
	    clear();
	    for (int i=0; i<children.size(); i++) {
		add((SIRStream)children.get(i));
	    }
	    rescale();
	}
    }

    /**
     * See documentation in SIRContainer.
     */
    public void replace(SIRStream oldStr, SIRStream newStr) {
	int index = myChildren().indexOf(oldStr);
	Utils.assert(index!=-1,
		     "Trying to replace with bad parameters, since " + this
		     + " doesn't contain " + oldStr);
	myChildren().set(index, newStr);
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
     * Returns a list of the children of this.  The children are
     * stream objects that are contained within this (including the
     * splitter and joiner.
     */
    public List getChildren() {
	// build result from child streams
	List result = super.getChildren();
	// add splitter and joiner
	result.add(0, splitter);
	result.add(joiner);
	// return result
	return result;
    }

    /**
     * Returns a list of the parallel streams in this.
     */
    public List getParallelStreams() {
	return super.getChildren();
    }

    // reset splits and joins to have right number of children.
    public void rescale() {
	this.splitter.rescale(size());
	this.joiner.rescale(size());
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
	for (int i=0; i<size()-1; i++) {
	    // make an entry from splitter to each stream
	    SIROperator[] entry1 = { splitter, get(i) };
	    // make an entry from each stream to splitter
	    SIROperator[] entry2 = { get(i), joiner };
	    // add entries
	    result.add(entry1);
	    result.add(entry2);
	}
	// return result
	return result;
    }

    /**
     * Accepts attribute visitor <v> at this node.
     */
    public Object accept(AttributeStreamVisitor v) {
	return v.visitSplitJoin(this,
				fields,
				methods,
				init,
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

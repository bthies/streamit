package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.lir.LIRStreamType;
import java.util.HashMap;
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
     * Construct a new SIRSplitJoin with the given fields and methods.
     */
    public SIRSplitJoin(SIRContainer parent,
			String ident,
			JFieldDeclaration[] fields,
			JMethodDeclaration[] methods) {
	super(parent, ident, fields, methods);
    }

    /**
     * Construct a new SIRSplitJoin with empty fields and methods.
     */
    public SIRSplitJoin(SIRContainer parent,
			String ident) {
	super(parent, ident, JFieldDeclaration.EMPTY(), JMethodDeclaration.EMPTY() );
    }

    /**
     * Construct a new SIRPipeline with null fields, parent, and methods.
     */
    public SIRSplitJoin() {
	super();
    }

    /**
     * sets the splitter for this SplitJoin, and sets the parent of
     * <b>s</b> to be this.
     */
    public void setSplitter(SIRSplitter s) 
    {
	this.splitter = s;
	s.setParent(this);
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
     * sets the joiner for this SplitJoin, and sets the parent of <j>
     * to be this.
     */
    public void setJoiner(SIRJoiner j) 
    {
	this.joiner = j;
	j.setParent(this);
    }
   
    /**
     * gets the joinger.
     */
    public SIRJoiner getJoiner() 
    {
	Utils.assert(this.joiner!=null);
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

    public int getPushForSchedule(HashMap[] counts) {
	// the splitjoin pushes everything that children push in a
	// steady-state
	int sum = 0;
	for (int i=0; i<size(); i++) {
	    sum += get(i).getPushForSchedule(counts);
	}
	return sum;
    }

    public int getPopForSchedule(HashMap[] counts) {
	// the splitjoin pops everything that children pop in a
	// steady-state.  Unless it's a duplicate splitter, in which
	// case it pops only what one child pops.
	if (splitter.getType()==SIRSplitType.DUPLICATE) {
	    return get(0).getPopForSchedule(counts);
	} else {
	    int sum = 0;
	    for (int i=0; i<size(); i++) {
		sum += get(i).getPopForSchedule(counts);
	    }
	    return sum;
	}
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
     * splitter and joiner. Specifically, the first element in the
     * list is the splitter(SIRSplitter),  the next elements are
     * the child streams (SIRStream)
     * ordered from left to right, and the final element is the
     * joiner(SIRJoiner).
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

    public void reclaimChildren() {
	super.reclaimChildren();
	splitter.setParent(this);
	joiner.setParent(this);
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
     * Returns whether or not all streams in this have the same
     * "height" (see getComponentHeight).
     */
    public boolean isRectangular() {
	return getRectangularHeight()!=-1;
    }

    /**
     * If this is not rectangular, then pad the shorter streams in
     * this with Identity filters until all component streams have the
     * same length.  Put the identity filters at the end of the
     * shorter streams (unless shorter stream is a sink, in which case
     * put them at the beginning.)
     */
    public void makeRectangular() {
	if (isRectangular()) {
	    return;
	}
	// find max height of component of this
	int max = getComponentHeight(get(0));
	for (int i=1; i<size(); i++) {
	    max = Math.max(max, getComponentHeight(get(i)));
	}
	// pad all short streams with identities
	for (int i=0; i<size(); i++) {
	    SIRStream child = get(i);
	    int height = getComponentHeight(child);
	    // if we have to pad...
	    if (height<max) {
		// if we have anything but a pipeline, wrap it in a
		// pipeline so we can add to it.
		SIRPipeline wrapper;
		if (child instanceof SIRPipeline) {
		    wrapper = (SIRPipeline)child;
		} else {
		    wrapper = SIRContainer.makeWrapper(child);
		}
		// add identities to wrapper
		for (int j=height; j<max; j++) {
		    if (wrapper.getOutputType()!=CStdType.Null) {
			wrapper.add(new SIRIdentity(child.getOutputType()));
		    } else if (wrapper.getInputType()!=CStdType.Null) {
			wrapper.add(0, new SIRIdentity(child.getInputType()));
		    } else {
			Utils.fail("Trying to extend a void->void child stream into a rectangular splitjoin.");
		    }
		}
	    }
	}
    }

    /**
     * If this is a rectangular splitjoin, returns the "height" of all
     * streams in the rectangule (see getComponentHeight).  If this is
     * non-rectangular, returns -1.
     */
    public int getRectangularHeight() {
	int height = getComponentHeight(get(0));
	for (int i=1; i<size(); i++) {
	    if (height!=getComponentHeight(get(i))) {
		return -1;
	    }
	}
	return height;
    }

    /**
     * Helper function for getHeight - returns the height of <str>.
     * Everything but pipelines have a height of 1 since they are
     * treated as a hierarchical unit.
     */
    private static int getComponentHeight(SIRStream str) {
	if (str instanceof SIRPipeline) {
	    return ((SIRPipeline)str).size();
	} else {
	    return 1;
	}
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
     * Overrides SIRStream.getSuccessor.  All parallel streams should
     * have the joiner as their successor.  The splitter has the first
     * parallel stream as its successor.
     */
    public SIROperator getSuccessor(SIRStream child) {
	// all parallel streams should have the joiner as their successor
	if (getParallelStreams().contains(child)) {
	    return joiner;
	} else {
	    return super.getSuccessor(child);
	}
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

    public String toString() {
	return "SIRSplitJoin name=" + getName();
    }


/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.kjc.sir.SIRSplitJoin other = new at.dms.kjc.sir.SIRSplitJoin();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRSplitJoin other) {
  super.deepCloneInto(other);
  other.splitter = (at.dms.kjc.sir.SIRSplitter)at.dms.kjc.AutoCloner.cloneToplevel(this.splitter, other);
  other.joiner = (at.dms.kjc.sir.SIRJoiner)at.dms.kjc.AutoCloner.cloneToplevel(this.joiner, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

package at.dms.kjc.sir;

import at.dms.util.Utils;
import at.dms.kjc.*;
import at.dms.kjc.lir.LIRStreamType;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.io.*;

/**
 * This represents a pipeline of stream structures, as would be
 * declared with a Stream construct in StreaMIT.
 */
public class SIRPipeline extends SIRContainer implements Cloneable {
    /**
     * Construct a new SIRPipeline with the given fields and methods.
     */
    public SIRPipeline(SIRContainer parent,
		       String ident,
		       JFieldDeclaration[] fields,
		       JMethodDeclaration[] methods) {
	super(parent, ident, fields, methods);
    }

    /**
     * Return a shallow clone of the SIRPipeline
     */
    public Object clone() {
	SIRPipeline p = new SIRPipeline(this.parent, this.ident,
					this.fields, this.methods);
	p.setInit(this.init);
	for(int i = 0; i < children.size(); i++) {
	    // get child
	    SIRStream child = (SIRStream)this.children.get(i);
	    // clone child
	    SIRStream childClone = (SIRStream)child; //.clone();
	    // set child's parent to <p>
	    //childClone.setParent(p);
	    // add it to <p>'s children
	    p.add(childClone);
	}
	return p;
    }

    /**
     * Returns the output type of this.
     */
    public CType getOutputType() {
	// output type is output type of last element in list
	return ((SIRStream)children.getLast()).getOutputType();
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
	return ((SIRStream)children.getFirst()).getInputType();
    }
    
    /**
     * Returns the relative name by which this object refers to child
     * <child>, or null if <child> is not a child of this.
     */
    public String getChildName(SIROperator str) {
	// return "stream" + (x+1), where x is the index of <str> in this pipe
	int index = children.indexOf(str);
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
	return (List)children.clone();
    }

    /**
     * Whether or not <str> is an immediate child of this.
     */
    public boolean contains(SIROperator str) {
	return children.contains(str);
    }

    /**
     * Returns a list of the children between <first> and <last>,
     * inclusive.  Assumes that <first> and <last> are both contained
     * in this, and that <first> comes before <last>.
     */
    public List getChildrenBetween(SIROperator first, SIROperator last) {
	// make result
	LinkedList result = new LinkedList();
	// start iterating through children at <first>
	ListIterator iter = children.listIterator(children.indexOf(first));
	Object o;
	do {
	    // get next child and add to result list
	    o = iter.next();
	    result.add(o);
	    // quit when we've added the last one
	} while (o!=last);
	// return result
	return result;
    }

    /**
     * Sets children of this to be all the children of <children>, and
     * set all the parent fields in <children> to be this.
     */
    public void setChildren(LinkedList children) {
	this.children.clear();
	for (int i=0; i<children.size(); i++) {
	    add((SIRStream)children.get(i));
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
	for (int i=0; i<children.size()-1; i++) {
	    // make an entry from one stream to next
	    SIROperator[] entry = { (SIROperator)children.get(i),
				    (SIROperator)children.get(i+1) };
	    // add entry 
	    result.add(entry);
	}
	// return result
	return result;
    }

    /**
     * Add a stream to the end of the pipeline, and set that stream's
     * parent to this.
     */
    public void add(SIRStream str) {
	add(children.size(), str);
    }

    /**
     * Adds stream <str> to this pipeline, at index <index>, and sets
     * the parent of <str> to be this.
     */
    public void add(int index, SIRStream str) {
	children.add(index, str);
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
	// set parent of new stream
	newStr.setParent(this);
    }

    /**
     * Replaces the sequence of <start> ... <end> within this pipeline with
     * the single stream <newStream>.  Requires that <start> and <end> are
     * both in this, with <start> coming before <end>.
     */
    public void replace(SIRStream start, SIRStream end, SIRStream newStream) {
	// get positions of start and ending streams
	int index1 = children.indexOf(start);
	int index2 = children.indexOf(end);
	Utils.assert(index1!=-1 && index1!=-1 && index1 <= index2,
		     "Trying to replace with bad parameters, from start at "
		     + "position " + index1 + " to end at position " + index2);
	// remove the old streams
	for (int i=index1; i<=index2; i++) {
	    children.remove(index1);
	}
	// add the new stream
	add(index1, newStream);
    }

    /**
     * Remove a stream from the pipeline.
     */
    public void remove(SIRStream str) {
	children.remove(str);
    }

    /**
     * Return i'th stream in this pipeline.
     */
    public SIRStream get(int i) {
	return (SIRStream)children.get(i);
    }

    /**
     * Returns the index of <str> in this pipeline, or -1 if <str>
     * does not appear in this.
     */
    public int indexOf(SIRStream str) {
	return children.indexOf(str);
    }

    /**
     * Returns the number of substreams in this.
     */
    public int size() {
	return children.size();
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
			   children);
	/* visit components */
	for (int i=0; i<children.size(); i++) {
	    ((SIRStream)children.get(i)).accept(v);
	}
	v.postVisitPipeline(this,
			    parent,
			    fields,
			    methods,
			    init,
			    children);
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
			       children);
    }
}


package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import streamit.scheduler.iriter.*;

public class SIRPipelineIter extends SIRIterator implements PipelineIter {

    /**
     * Object pointed to by this iterator.
     */
    private SIRPipeline obj;

    /**
     * Returns new iterator for <obj> with no parent.
     */
    SIRPipelineIter(SIRPipeline obj) {
	this.obj = obj;
    }

    /**
     * Returns new iterator for <obj> in position <pos> of parent <parent>.
     */
    SIRPipelineIter(SIRPipeline obj, SIRIterator parent, int pos) {
	super(parent, pos);
	this.obj = obj;
    }

    public PipelineIter isPipeline() {
	return this;
    }

    /**
     * Return the stream pointed to by this.
     */
    public SIRStream getStream() {
	checkValidity();
	return obj;
    }

    public int getNumChildren () {
	return obj.size();
    }

    public Iterator getChild (int n) {
	return IterFactory.createIter(obj.get(n), this, n);
    }

    /**
     * The same as <getChild> with a different signature.
     */
    public SIRIterator get (int n) {
	return (SIRIterator)getChild(n);
    }

    public void accept(StreamVisitor v) {
	v.preVisitPipeline(obj, this);
	for (int i=0; i<getNumChildren(); i++) {
	    ((SIRIterator)getChild(i)).accept(v);
	}
	v.postVisitPipeline(obj, this);
    }

    /**
     * This function is needed by the scheduler, but isn't useful from
     * the compiler.
     */
    public Iterator getUnspecializedIter() {
	return this;
    }
}

package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import streamit.scheduler.iriter.*;

class SIRPipelineIter extends SIRIterator implements PipelineIter {

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
     * Returns the object the iterator points to.
     */
    public Object getObject() {
	checkValidity();
	return obj;
    }

    public int getNumChildren () {
	return obj.size();
    }

    public Iterator getChild (int n) {
	return IterFactory.createIter(obj.get(n), this, n);
    }
}

package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import streamit.scheduler.iriter.*;

abstract class SIRIterator implements Iterator {
    /**
     * The root of this iterator.
     */
    private Root root;

    /**
     * The parent of this iterator.
     */
    private SIRIterator parent;

    /**
     * The position of this iterator in the parent.
     */
    private int pos;

    /**
     * Make an iterator with no parent
     */
    protected SIRIterator() {
	this.root = new Root();
	this.parent = null;
	this.pos = -1;
    }

    /**
     * Make an iterator for parent <parent> that contains this at
     * position <pos>
     */
    protected SIRIterator(SIRIterator parent, int pos) {
	this.root = parent.root;
	this.parent = parent;
	this.pos = pos;
    }

    public FilterIter isFilter() {
	return null;
    }

    public PipelineIter isPipeline() {
	return null;
    }

    public SplitJoinIter isSplitJoin() {
	return null;
    }

    public FeedbackLoopIter isFeedbackLoop() {
	return null;
    }

    /**
     * Get the parent of this.
     */
    public SIRIterator getParent() {
	checkValidity();
	return this.parent;
    }

    /**
     * Check the validity of this iterator, and print an error message
     * with a stack trace if not valid.
     */
    protected void checkValidity() {
	if (!root.isValid()) {
	    new InvalidIteratorException().printStackTrace();
	}
    }
}

/**
 * A root object in the iterator tree, which is just a stub that can
 * be invalidated once an iterator tree is obsolete.
 */
class Root {
    private boolean valid;

    public Root() {
	this.valid = true;
    }

    /**
     * Invalidates this root, since it's been replaced by something.
     */
    public void invalidate() {
	this.valid = false;
    }

    /**
     * Return whether or not this is valid.
     */
    public boolean isValid() {
	return this.valid;
    }
}

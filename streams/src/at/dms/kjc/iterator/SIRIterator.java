package at.dms.kjc.iterator; 

import at.dms.kjc.sir.*;
import streamit.scheduler.iriter.*;

public abstract class SIRIterator implements Iterator {
    /**
     * The root of this iterator.
     */
    private Root root;

    /**
     * Whether or not this individual node is valid.  An iterator is
     * safe to use if (and only if) both it and its root are valid.
     */
    private boolean validNode = true;

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
     * Return the stream pointed to by this.  (Redundant with
     * getStream(), but required for Iterator interface.)
     */
    public Object getObject() {
	return getStream();
    }

    /**
     * Return the stream pointed to by this.
     */
    public abstract SIRStream getStream();

    /**
     * Get the parent of this.
     */
    public SIRIterator getParent() {
	checkValidity();
	return this.parent;
    }

    /**
     * Returns position of this in parent.
     */
    public int getPos() {
	checkValidity();
	return pos;
    }

    /**
     * Invalidates this individual node, but does not invalidate the
     * tree above it.
     */
    public void invalidateNode() {
	this.validNode = false;
    }

    /**
     * Invalidates the entire tree of which this iterator is a part --
     * that is, everyone that shares the same root as this.
     */
    public void invalidateTree() {
	this.root.invalidate();
    }

    /**
     * Check the validity of this iterator, and print an error message
     * with a stack trace if not valid.
     */
    protected void checkValidity() {
	if (!(root.isValid() && validNode)) {
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

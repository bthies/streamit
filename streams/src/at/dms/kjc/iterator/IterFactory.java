package at.dms.kjc.iterator;

/**
 * This class is the outside interface for managing iterators.
 */

import at.dms.kjc.*;
import at.dms.util.*;
import at.dms.kjc.sir.*;

public class IterFactory {
    /**
     * For now, we just need one memoizer, so let's just keep it here.
     */
    private static final Memoizer memoizer = Memoizer.create();

    /**
     * Constructor should stay protected so that it's only returned
     * from static methods in this package.
     */
    IterFactory() {}

    /***********************************************************************/

    /**
     * Returns default factory.
     */
    public static IterFactory createFactory() {
	return new IterFactory();
    }

    /**
     * Returns fine-grained iter factory.
     */
    public static IterFactory createFineGrainedFactory() {
	return new FineGrainedIterFactory();
    }

    /***********************************************************************/

    public SIRIterator createIter(SIRStream obj) { 
	if (obj instanceof SIRFilter) {
	    return createIter((SIRFilter)obj);
        } else if (obj instanceof SIRPhasedFilter) {
            return createIter((SIRPhasedFilter)obj);
	} else if (obj instanceof SIRPipeline) {
	    return createIter((SIRPipeline)obj);
	} else if (obj instanceof SIRSplitJoin) {
	    return createIter((SIRSplitJoin)obj);
	} else if (obj instanceof SIRFeedbackLoop) {
	    return createIter((SIRFeedbackLoop)obj);
	} else if (obj instanceof SIRRecursiveStub) {
	    return createIter((SIRRecursiveStub)obj);
	} else {
	    new RuntimeException().printStackTrace();
	    Utils.fail("Unexpected iterator " + obj + " of type " 
		       + (obj==null ? "" : obj.getClass().toString()));
	    return null;
	}
    }
    // The following methods are provided over the general method
    // aboveto provide better type checking on the return value, in
    // the event that the object type is known statically.  Also
    // allows for easier overriding.
    public SIRPipelineIter createIter(SIRPipeline obj) { 
	return new SIRPipelineIter(this, obj);
    }
    public SIRFilterIter createIter(SIRFilter obj) { 
	return new SIRFilterIter(this, obj);
    }
    public SIRPhasedFilterIter createIter(SIRPhasedFilter obj) {
        return new SIRPhasedFilterIter(this, obj);
    }
    public SIRSplitJoinIter createIter(SIRSplitJoin obj) {
	return new SIRSplitJoinIter(this, obj);
    }
    public SIRFeedbackLoopIter createIter(SIRFeedbackLoop obj) {
	return new SIRFeedbackLoopIter(this, obj);
    }
    public SIRRecursiveStubIter createIter(SIRRecursiveStub obj) {
	return new SIRRecursiveStubIter(this, obj);
    }

    /**
     * For building a child iterator, internal only to the iterator
     * package.  Requires that <parent> is non-null (to create a fresh
     * top-level iterator, use methods above.)
     */
    SIRIterator createIter(SIRStream obj, SIRIterator parent, int pos) { 
	assert parent!=null;
	if (obj instanceof SIRFilter) {
	    return createIter((SIRFilter)obj, parent, pos);
        } else if (obj instanceof SIRPhasedFilter) {
            return createIter((SIRPhasedFilter)obj, parent, pos);
	} else if (obj instanceof SIRPipeline) {
	    return createIter((SIRPipeline)obj, parent, pos);
	} else if (obj instanceof SIRSplitJoin) {
	    return createIter((SIRSplitJoin)obj, parent, pos);
	} else if (obj instanceof SIRFeedbackLoop) {
	    return createIter((SIRFeedbackLoop)obj, parent, pos);
	} else if (obj instanceof SIRRecursiveStub) {
	    return createIter((SIRRecursiveStub)obj, parent, pos);
	} else {
	    Utils.fail("Unexpected iterator " + obj + " of type " 
		       + (obj==null ? "" : obj.getClass().toString()));
	    return null;
	}
    }
    // The following methods are provided over the general method
    // aboveto provide better type checking on the return value, in
    // the event that the object type is known statically.  Also
    // allows for easier overriding.
    SIRPipelineIter createIter(SIRPipeline obj, SIRIterator parent, int pos) { 
	return new SIRPipelineIter(this, obj, parent, pos);
    }
    SIRFilterIter createIter(SIRFilter obj, SIRIterator parent, int pos) { 
	return new SIRFilterIter(this, obj, parent, pos);
    }
    SIRPhasedFilterIter createIter(SIRPhasedFilter obj, SIRIterator parent, int pos) {
        return new SIRPhasedFilterIter(this, obj, parent, pos);
    }
    SIRSplitJoinIter createIter(SIRSplitJoin obj, SIRIterator parent, int pos) {
	return new SIRSplitJoinIter(this, obj, parent, pos);
    }
    SIRFeedbackLoopIter createIter(SIRFeedbackLoop obj, SIRIterator parent, int pos) {
	return new SIRFeedbackLoopIter(this, obj, parent, pos);
    }
    SIRRecursiveStubIter createIter(SIRRecursiveStub obj, SIRIterator parent, int pos) {
	return new SIRRecursiveStubIter(this, obj, parent, pos);
    }

    /**
     * Requires that <i> points at an immutable node; invalidates <i>
     * and returns an iterator that points at a mutable node at the
     * same location, and with the same contents as the node pointed
     * to at <i>.  Only performs shallow immutability -- that is, if
     * the node is a container, it does not do a deep clone of the
     * child SIRStreams.
     *
     * - does cloning
     * - replaces in parents by shallowclone and replacement
    public static SIRIterator getMutableCopy(SIRIterator iter) { 
	SIRStream result = (SIRStream)ObjectDeepCloner.shallowCopy(iter.getStream());
	return replace(iter, result); 
    }
     */

    /**
     * - does finalization of contained stream structures
     * - does structural equality test
     * - replaces in parents by shallowclone and replacement
    public static SIRIterator finalize(SIRIterator iter) {
	SIRStream str = (SIRStream)iter.getObject();
	// replace all methods with finalized version
	JMethodDeclaration[] methods = str.getMethods();
	for (int i=0; i<methods.length; i++) {
	    methods[i] = (JMethodDeclaration)memoizer.finalize(methods[i]);
	}
	// if we have a container, replace all children with finalized
	// versions
	if (str instanceof SIRContainer) {
	    SIRContainer parent = (SIRContainer)str;
	    for (int i=0; i<parent.size(); i++) {
		Object child = parent.get(i);
		parent.set(i, (SIRStream)memoizer.finalize(child));
	    }
	}
	// get a finalized version of <str> 
	SIRStream result = (SIRStream)memoizer.finalize(str);
	// do the replacement in parents
	return replace(iter, result);
    }
     */

    /**
     * Returns whether or not object <o> has been finalized.
     */
    public static boolean isFinalized(Object o) {
	return memoizer.isFinalized(o);
    }

    /***********************************************************************/

    /**
     * Replace the stream pointed to by <iter> with <str>, returning
     * an iterator that points to <str> in the position pointed to by
     * <iter>.  This traces up the parent streams, doing a shallow
     * clone of any parent that is immutable, until it reaches a
     * mutable parent.  At this parent, it just adjusts the child to
     * point to the new tree.
    private static SIRIterator replace(SIRIterator iter, SIRStream str) {
	// get parent of <iter>
	SIRIterator parentIter = iter.getParent();
	if (parentIter==null) {
	    // if we're at the top, return a fresh root node, and invalide
	    // the whole tree of <iter>
	    iter.invalidateTree();
	    return createIter(str);
	} else {
	    // otherwise, examine parent stream
	    SIRContainer parentStr = (SIRContainer)parentIter.getStream();
	    if (memoizer.isFinalized(parentStr)) {
		// if the parent is immutable, create a shallow copy and
		// recurse
		SIRContainer newParentStr = (SIRContainer)ObjectDeepCloner.shallowCopy(parentStr);
		// set <str> in the new parent
		int pos = iter.getPos();
		newParentStr.set(pos, str);
		// finalize the new parent to make it immutable
		memoizer.finalize(newParentStr);
		// recurse to get parent iterator
		SIRIterator newParentIter = replace(parentIter, newParentStr);
		// invalidate old iterator
		iter.invalidateNode();
		// return new iterator
		return createIter(str, newParentIter, pos);
	    } else {
		// otherwise, we found a mutable parent, in which case we
		// can just adjust one of its children
		int pos = iter.getPos();
		parentStr.set(pos, str);
		// invalidate <iter>
		iter.invalidateNode();
		// make new iterator
		return createIter(str, parentIter, pos);
	    } 
	}
    }
     */

}

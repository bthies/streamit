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

    /***********************************************************************/

    public static SIRPipelineIter createIter(SIRPipeline obj) { 
	return new SIRPipelineIter(obj);
    }

    public static SIRFilterIter createIter(SIRFilter obj) { 
	return new SIRFilterIter(obj);
    }

    public static SIRPhasedFilterIter createIter(SIRPhasedFilter obj) {
        return new SIRPhasedFilterIter(obj);
    }

    public static SIRSplitJoinIter createIter(SIRSplitJoin obj) {
	return new SIRSplitJoinIter(obj);
    }

    public static SIRFeedbackLoopIter createIter(SIRFeedbackLoop obj) {
	return new SIRFeedbackLoopIter(obj);
    }

    public static SIRRecursiveStubIter createIter(SIRRecursiveStub obj) {
	return new SIRRecursiveStubIter(obj);
    }

    public static SIRIterator createIter(SIRStream obj) { 
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
	    Utils.fail("Unexpected iterator " + obj + " of type " 
		       + (obj==null ? "" : obj.getClass().toString()));
	    return null;
	}
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

    /**
     * For building a child iterator, internal only to the iterator
     * package.  Requires that <parent> is non-null (to create a fresh
     * top-level iterator, use methods above.)
     */
    static SIRIterator createIter(SIRStream obj, SIRIterator parent, int pos) { 
	Utils.assert(parent!=null);
	if (obj instanceof SIRFilter) {
	    return new SIRFilterIter((SIRFilter)obj, parent, pos);
        } else if (obj instanceof SIRPhasedFilter) {
            return new SIRPhasedFilterIter((SIRPhasedFilter)obj, parent, pos);
	} else if (obj instanceof SIRPipeline) {
	    return new SIRPipelineIter((SIRPipeline)obj, parent, pos);
	} else if (obj instanceof SIRSplitJoin) {
	    return new SIRSplitJoinIter((SIRSplitJoin)obj, parent, pos);
	} else if (obj instanceof SIRFeedbackLoop) {
	    return new SIRFeedbackLoopIter((SIRFeedbackLoop)obj, parent, pos);
	} else if (obj instanceof SIRRecursiveStub) {
	    return new SIRRecursiveStubIter((SIRRecursiveStub)obj, parent, pos);
	} else {
	    Utils.fail("Unexpected iterator " + obj + " of type " 
		       + (obj==null ? "" : obj.getClass().toString()));
	    return null;
	}
    }

}

package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import java.util.*;
import java.io.*;

/**
 * This represents a 1-to-1 stream that can contain other streams as a
 * hierarchical unit.
 */
public abstract class SIRContainer extends SIRStream {
    /**
     * These are two consistent lists of children and parameters.  The
     * i'th element of children corresponds to the i'th element of
     * parameters.  The two lists are guaranteed to be consistent
     * because only the methods in this class can mutate them; the
     * subclasses should access these by myChildren() and myParams()
     */
    protected MutableList children;
    protected MutableList params;

    protected SIRContainer() {
	super();
	this.children = new MutableList();
	this.params = new MutableList();
    }

    protected SIRContainer(SIRContainer parent,
			   String ident,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods) {
	super(parent, ident, fields, methods);
	this.children = new MutableList();
	this.params = new MutableList();
    }

    /**
     * Returns the relative name by which this object refers to child
     * <child>, or null if <child> is not a child of this.
     */
    public abstract String getChildName(SIROperator str);

    /**
     * Returns a list of the children of this.  The children are
     * stream objects that are contained within this.  Should be
     * over-ridden by subclasses to include splitters/joiners etc.
     */
    public List getChildren() {
	List result = new LinkedList();
	for (int i=0; i<size(); i++) {
	    result.add(get(i));
	}
	return result;
    }

    /**
     * So subclasses can view parameter list.
     */
    protected ConstList myParams() {
	return params;
    }

    /**
     * Clears the child/param lists.
     */
    protected void clear() {
	children.clear();
	params.clear();
    }

    /**
     * So subclasses can view children.
     */
    protected ConstList myChildren() {
	return children;
    }

    /**
     * Add a <child> with null parameters.
     */
    public void add(SIRStream str) {
	children.add(str);
	params.add(null);
    }

    public void add(int index, SIRStream str) {
	children.add(index, str);
	params.add(index, null);
    }

    /**
     * Returns the i'th child of this.  For FeedbackLoops, can use the
     * constant indices defined in SIRFeedbackLoop, or getBody/getLoop.
     */
    public SIRStream get(int i) {
	return (SIRStream)children.get(i);
    }

    /**
     * Removes the i'th child of this.
     */
    public void remove(int i) {
	children.remove(i);
	params.remove(i);
    }

    /**
     * Set child at index <index> to <str>
     */
    public void set(int index, SIRStream str) {
	children.set(index, str);
    }

    /**
     * Returns a list of the parameters (JExpressions) that are being
     * passed to the i'th child of this, or null if the parameters
     * have not been resolved yet.
     */
    public LinkedList getParams(int i) {
	return ((SIRStreamInstance)children.get(i)).args;
    }

    /**
     * Returns number of child streams of this.  (Will always be two
     * for feedbackloop's).
     */
    public int size() {
	return children.size();
    }

    /**
     * Returns a list of tuples (two-element arrays) of SIROperators,
     * representing a tape from the first element of each tuple to the
     * second.
     */
    public abstract List getTapePairs();
    
    /**
     * Replaces <oldStr> with <newStr> in this.  Requires that
     * <oldStr> is an immediate child of this.  (It does not do a
     * deep-replacement.)  Also, it sets the parent of <newStr> to be
     * this, but does NOT mend the calls in the init function to call
     * the init function of newStr.
     */
    public abstract void replace(SIRStream oldStr, SIRStream newStr);

    /**
     * Whether or not <str> is an immediate child of this.
     */
    public boolean contains(SIROperator str) {
	for (int i=0; i<children.size(); i++) {
	    if (((SIRStreamInstance)children.get(i)).str==str) {
		return true;
	    }
	}
	return false;
    }

    private class MutableList extends ConstList implements Serializable {

	/** Inserts the specified element at the specified position in
	 * this list (optional operation). */
	void add(int index, Object element) {
	    list.add(index, element);
	}
	    
	/** Appends the specified element to the end of this list
	 * (optional operation). */
	boolean add(Object o) {
	    return list.add(o);
	}

	/** Appends all of the elements in the specified collection to
	 * the end of this list, in the order that they are returned
	 * by the specified collection's iterator (optional
	 * operation). */
	boolean addAll(Collection c) {
	    return list.addAll(c);
	}

	/** Inserts all of the elements in the specified collection
	 * into this list at the specified position (optional
	 * operation). */
	boolean addAll(int index, Collection c) {
	    return list.addAll(index, c);
	}

	/** Removes all of the elements from this list (optional
	 * operation). */
	void clear() {
	    list.clear();
	}

	/** Removes the element at the specified position in this list
	 * (optional operation). */
	Object remove(int index)  {
	    return list.remove(index);
	}

	/** Removes the first occurrence in this list of the specified
	 * element (optional operation). */
	boolean remove(Object o) {
	    return list.remove(o);
	}

	/** Removes from this list all the elements that are contained
	 * in the specified collection (optional operation). */
	boolean removeAll(Collection c) {
	    return list.removeAll(c);
	}

	/** Retains only the elements in this list that are contained
	 * in the specified collection (optional operation). */
	boolean retainAll(Collection c) {
	    return list.retainAll(c);
	}

	/** Returns a view of the portion of this list between the
	 * specified fromIndex, inclusive, and toIndex, exclusive. */
	List subList(int fromIndex, int toIndex) {
	    return list.subList(fromIndex, toIndex);
	}
    }
}

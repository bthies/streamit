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
     * Returns a list of the children of this (does not return the
     * internal representation list of this.)  The children are stream
     * objects that are contained within this.  Should be over-ridden
     * by subclasses to include splitters/joiners etc.
     */
    public List getChildren() {
	List result = new LinkedList();
	for (int i=0; i<size(); i++) {
	    result.add(get(i));
	}
	return result;
    }

    /**
     * Returns copy of list of parameters passed to children of this.
     */
    public List getParams() {
	List result = new LinkedList();
	for (int i=0; i<size(); i++) {
	    result.add(getParams(i));
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
     * Returns the index of <str> in this, or -1 if <str> does not
     * appear in this.
     */
    public int indexOf(SIRStream str) {
	return children.indexOf(str);
    }

    /**
     * Clears the child/param lists.
     */
    public void clear() {
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
     * Add a <child> with empty parameters.
     */
    public void add(SIRStream str) {
	this.add(str, new LinkedList());
    }

    /**
     * Adds <str> at index <index> with empty parameters.
     */
    public void add(int index, SIRStream str) {
	this.add(index, str, new LinkedList());
    }

    /**
     * Adds <str> at the end of this with parameters <param>.
     */
    public void add(SIRStream str, List param) {
	this.add(size(), str, param);
    }

    /**
     * Adds <str> at index <index> with parameters <param>, and sets
     * parent of <str> to this.
     */
    public void add(int index, SIRStream str, List param) {
	children.add(index, str);
	params.add(index, param);
	if (str!=null) {
	    str.setParent(this);
	}
    }

    /**
     * Returns the i'th child of this.  For FeedbackLoops, can use the
     * constant indices defined in SIRFeedbackLoop, or getBody/getLoop.
     */
    public SIRStream get(int i) {
	SIRStream child = (SIRStream)children.get(i);
	//Utils.assert(child.getParent()==this);
	return child;
    }

    /**
     * Returns the successor of <child>.  If <child> is the last child
     * of this, then returns the next succesor of <this> in the parent
     * of <this>.  Note that this is as fine-grained as possible -
     * e.g., the successor of a parallel stream in a SplitJoin should
     * be a joiner.
     */
    public SIROperator getSuccessor(SIRStream child) {
	List myChildren = getChildren();
	int i = myChildren.indexOf(child);
	if (i==myChildren.size()-1) {
	    // if it's at the end of the whole program, return null
	    if (getParent()==null) {
		return null;
	    } else {
		// otherwise, go for first element in parent's successor
		SIROperator succ = getParent().getSuccessor(this);
		// recurse down through the successor to find it's first
		// child
		while (succ instanceof SIRContainer) {
		    succ = (SIROperator)((SIRContainer)succ).getChildren().get(0);
		}
		return succ;
	    }
	} else {
	    return (SIROperator)myChildren.get(i+1);
	}
    }

    /**
     * Removes the i'th child of this.
     */
    public void remove(int i) {
	children.remove(i);
	params.remove(i);
    }

    /**
     * Removes <str> from this.
     */
    public void remove(SIRStream str) {
	Utils.assert(this.contains(str));
	remove(indexOf(str));
    }

    /**
     * Set child at index <index> to <str>
     */
    public void set(int index, SIRStream str) {
	children.set(index, str);
    }

    /**
     * Set parameters for <i>'th child to <params>
     */
    public void setParams(int index, List params) {
	this.params.set(index, params);
    }

    /**
     * Returns a list of the parameters (JExpressions) that are being
     * passed to the i'th child of this, or null if the parameters
     * have not been resolved yet.
     */
    public List getParams(int i) {
	return (List)params.get(i);
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
     * the init function of newStr (if there are any SIRInitStatments
     * remaining.)
     */
    public abstract void replace(SIRStream oldStr, SIRStream newStr);

    /**
     * Whether or not <str> is an immediate child of this.
     */
    public boolean contains(SIROperator str) {
	for (int i=0; i<children.size(); i++) {
	    if (children.get(i)==str) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns a wrapper for <str>.  Also mutates parent of <str> to
     * contain the wrapper in between itself and <str>.
     */
    public static SIRContainer makeWrapper(SIRStream str) {
	SIRPipeline wrapper = new SIRPipeline(str.getParent(),
					      "wrapper_" + str.getName(), 
					      JFieldDeclaration.EMPTY(), 
					      JMethodDeclaration.EMPTY());
	wrapper.setInit(SIRStream.makeEmptyInit());
	wrapper.add(str);
	if (str.getParent()!=null) {
	    str.getParent().replace(str, wrapper);
	}
	return wrapper;
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

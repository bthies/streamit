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
    private MutableList children;
    private MutableList params;
    private static LinkedList toClear=new LinkedList();

    protected SIRContainer() {
	super();
	this.children = new MutableList();
	this.params = new MutableList();
	toClear.add(this);
    }

    protected SIRContainer(SIRContainer parent,
			   String ident,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods) {
	super(parent, ident, fields, methods);
	this.children = new MutableList();
	this.params = new MutableList();
	toClear.add(this);
    }

    //Do not use unless not using structure anymore
    public static void destroy() {
	while(toClear.size()>0) {
	    SIRContainer cont=(SIRContainer)toClear.removeFirst();
	    cont.children.clear();
	    cont.children=null;
	    cont.params.clear();
	    cont.params=null;
	}
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
	return child;
    }

    /**
     * Resets all children of this to have this as their parent, and
     * calls reclaimChildren on all children that are containers.
     * This is needed because a stream can have exactly one parent,
     * but sometimes a stream is shuffled around between different
     * parents (e.g. in partitioning).
     */
    public void reclaimChildren() {
	for (int i=0; i<size(); i++) {
	    get(i).setParent(this);
	    if (get(i) instanceof SIRContainer) {
		((SIRContainer)get(i)).reclaimChildren();
	    }
	}
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
    public static SIRPipeline makeWrapper(SIRStream str) {
	// wrapper.add below changes the parent of str, so grab the
	// old parent now.
	SIRContainer oldParent = str.getParent();
	SIRPipeline wrapper = new SIRPipeline(oldParent, str.getIdent());
	wrapper.setInit(SIRStream.makeEmptyInit());
	wrapper.add(str);
	if (oldParent!=null) {
	    oldParent.replace(str, wrapper);
	}
	return wrapper;
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() { at.dms.util.Utils.fail("Error in auto-generated cloning methods - deepClone was called on an abstract class."); return null; }

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.kjc.sir.SIRContainer other) {
  super.deepCloneInto(other);
  other.children = (at.dms.util.MutableList)at.dms.kjc.AutoCloner.cloneToplevel(this.children, other);
  other.params = (at.dms.util.MutableList)at.dms.kjc.AutoCloner.cloneToplevel(this.params, other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

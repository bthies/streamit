package at.dms.util;

import java.io.*;
import java.util.*;

/**
 * Intended to be used in conjunction with ConstList, if one user
 * wants a constant view of a list while another wants to be able to
 * mutate it.
 */
public class MutableList extends ConstList implements Serializable {

    /** Inserts the specified element at the specified position in
     * this list (optional operation). */
    public void add(int index, Object element) {
	list.add(index, element);
    }
	    
    /** Appends the specified element to the end of this list
     * (optional operation). */
    public boolean add(Object o) {
	return list.add(o);
    }

    /** Appends all of the elements in the specified collection to
     * the end of this list, in the order that they are returned
     * by the specified collection's iterator (optional
     * operation). */
    public boolean addAll(Collection c) {
	return list.addAll(c);
    }

    /** Inserts all of the elements in the specified collection
     * into this list at the specified position (optional
     * operation). */
    public boolean addAll(int index, Collection c) {
	return list.addAll(index, c);
    }

    /** Removes all of the elements from this list (optional
     * operation). */
    public void clear() {
	list.clear();
    }

    /** Removes the element at the specified position in this list
     * (optional operation). */
    public Object remove(int index)  {
	return list.remove(index);
    }

    /** Removes the first occurrence in this list of the specified
     * element (optional operation). */
    public boolean remove(Object o) {
	return list.remove(o);
    }

    /** Removes from this list all the elements that are contained
     * in the specified collection (optional operation). */
    public boolean removeAll(Collection c) {
	return list.removeAll(c);
    }

    /** Retains only the elements in this list that are contained
     * in the specified collection (optional operation). */
    public boolean retainAll(Collection c) {
	return list.retainAll(c);
    }

    /** Returns a view of the portion of this list between the
     * specified fromIndex, inclusive, and toIndex, exclusive. */
    public List subList(int fromIndex, int toIndex) {
	return list.subList(fromIndex, toIndex);
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.util.MutableList other = new at.dms.util.MutableList();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.util.MutableList other) {
  super.deepCloneInto(other);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

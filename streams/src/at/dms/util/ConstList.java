package at.dms.util;

import java.io.*;
import java.util.*;

/**
 * This provides a constant interface to a list, where "constant"
 * means that elements can't be added or removed from the list.  They
 * can, however, be set to different elements.
 */
public class ConstList implements Serializable, at.dms.kjc.DeepCloneable {

    protected List list;

    public ConstList() {
	list = new LinkedList();
    }

    public ConstList(List list) {
	this.list = list;
    }

    /** Returns true if this list contains the specified element.  */
    public boolean contains(Object o) {
	return list.contains(o);
    }

    /** Returns true if this list contains all of the elements of the specified collection. */
    public boolean containsAll(Collection c) {
	return list.containsAll(c);
    }
	
    /** Compares the specified object with this list for equality. */
    public boolean equals(Object o) {
	return list.equals(o);
    }

    /** Returns the element at the specified position in this list. */
    public Object get(int index) {
	return list.get(index);
    }

    /** Returns the hash code value for this list. */
    public int hashCode() {
	return list.hashCode();
    }

    /** Returns the index in this list of the first occurrence of
     * the specified element, or -1 if this list does not contain
     * this element. */
    public int indexOf(Object o) {
	return list.indexOf(o);
    }

    /** Returns true if this list contains no elements. */
    public boolean isEmpty() {
	return list.isEmpty();
    }

    /** Returns an iterator over the elements in this list in proper sequence. */
    public Iterator iterator() {
	return list.iterator();
    }

    /** Returns the index in this list of the last occurrence of the
     * specified element, or -1 if this list does not contain this
     * element. */
    public int lastIndexOf(Object o) {
	return list.lastIndexOf(o);
    }


    /** Returns a list iterator of the elements in this list (in proper sequence). */
    public ListIterator listIterator() {
	return list.listIterator();
    }


    /** Returns a list iterator of the elements in this list (in
     * proper sequence), starting at the specified position in this
     * list. */
    public ListIterator listIterator(int index) {
	return list.listIterator(index);
    }


    /** Replaces the element at the specified position in this list
     * with the specified element (optional operation). */
    public Object set(int index, Object element) {
	return list.set(index, element);
    }


    /** Returns the number of elements in this list. */
    public int size() {
	return list.size();
    }


    /** Returns an array containing all of the elements in this list in proper sequence. */
    public Object[] toArray() {
	return list.toArray();
    }


    /** Returns an array containing all of the elements in this list in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array. */
    public Object[] toArray(Object[] a) {
	return list.toArray(a);
    }

/** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

/** Returns a deep clone of this object. */
public Object deepClone() {
  at.dms.util.ConstList other = new at.dms.util.ConstList();
  at.dms.kjc.AutoCloner.register(this, other);
  deepCloneInto(other);
  return other;
}

/** Clones all fields of this into <other> */
protected void deepCloneInto(at.dms.util.ConstList other) {
  other.list = (java.util.List)at.dms.kjc.AutoCloner.cloneToplevel(this.list, this);
}

/** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

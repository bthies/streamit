package at.dms.kjc.iterator;

import at.dms.util.*;
import at.dms.kjc.*;

import java.security.Permission;
import java.util.*;
import java.lang.reflect.*;

/**
 * In this implementation, an object is memoized iff it is immutable.
 */

public class Memoizer {
    
    /**
     * Mapping from signature to cached object.
     */
    private Hashtable cache;

    /**
     * Set of all objects that have been finalized.
     */
    private HashSet finalized;
    
    private Memoizer() {
	this.cache = new Hashtable();
	this.finalized = new HashSet();
    }

    /*******************************************************************/

    // when the create() method is called for the first time, disable
    // access control on all fields
    static {
	System.setSecurityManager(new SecurityManager() {
		public void checkPermission(Permission perm) {}
	    });
    }

    /**
     * Returns a memoizer for use in the IR.
     */
    static Memoizer create() {
	// return a memoizer
	return new Memoizer();
    }

    /**
     * Given an Object <o> which may or may not be mutable, returns an
     * immutable object that is "equivalent" to <o>.  Requires that
     * <o> is non-null.  Should maintain only package-level access
     * since the interface to this should be through iterators only.
     */
    Object finalize(Object o) {
	// assert <o> is non-null
	assert o!=null: "Memoizer.finalize requires non-null argument";
	// mark <o> and all of its components to be finalized
	deepFinalize(o);
	// get a hash for the structure of <o>
	Signature sig = hashStructure(o);
	// see if we have something that matches <o>
	Object equiv = cache.get(sig);
	// if so, return it
	if (equiv!=null) {
	    return equiv;
	} 
	// otherwise, cache <o> with <sig> and return it
	cache.put(sig, o);
	return o;
    }

    /**
     * Returns whether or not we've finalized <o>.
     */
    boolean isFinalized(Object o) {
	return finalized.contains(o);
    }

    /**
     * Returns whether or not the cached contents of this are
     * consistent.  The cache is inconsistent when one of the
     * finalized members have been mutated so as to have a different
     * signature.  The cache should always be consistent.
     */
    boolean isConsistent() {
	for (Enumeration e = cache.keys(); e.hasMoreElements(); ) {
	    Signature oldSig = (Signature)e.nextElement();
	    Object o = cache.get(oldSig);
	    Signature newSig = new Signature(o);
	    hashFields(o, newSig);
	    if (!newSig.equals(oldSig)) {
		return false;
	    }
	}
	return true;
    }

    /**
     * Returns a "shallow clone" of <o> -- that is, an instance of the
     * same type of <o> that is instantiated with a no-argument
     * constructor, and which has all the same fields as <o>.
     static Object shallowClone(Object o) {
     // create an instance of the same type as <o>
     Class c = o.getClass();
     Object result = c.newInstance();

     // for all the superclasses <c> of <o>...
     for ( ; c!=null; c = c.getSuperclass()) {
     // for all the fields of <c>...
     Field[] field = c.getDeclaredFields();
     for (int i=0; i<field.length; i++) {
     // get the value for the field
     field[i].setAccessible(true);
     try {
     // copy the value over to <result>
     Object value = field[i].get(o);
     field[i].set(result, value);
     } catch (IllegalAccessException e) {
     e.printStackTrace();
     }
     }
     }
	
     }
    */

    /*******************************************************************/

    /**
     * Returns a signature for the object <o> based on the deep
     * structural properties of its fields.  That is, hashFields(o1)
     * and hashFields(o2) should return the same value if all the
     * fields of o1 and o2 have the same deep structure.
     */
    private Signature hashStructure(Object o) {
	Signature sig = new Signature(o);
	hashFields(o, sig);
	return sig;
    }

    /**
     * Descends through all fields of <o> until it hits a primitive
     * type, invoking the finalize() method on any Finalizable object
     * it finds.
     */
    private void deepFinalize(Object o) {
	// if <o> is finalizable, then record it as finalized
	if (o instanceof Finalizable) {
	    finalized.add(o);
	}
	// for all the superclasses <c> of <o>...
	for (Class c = o.getClass(); c!=null; c = c.getSuperclass()) {
	    // for all the fields of <c>...
	    Field[] field = c.getDeclaredFields();
	    for (int i=0; i<field.length; i++) {
		// get the value for the field
		field[i].setAccessible(true);
		try {
		    Object value = field[i].get(o);
		    if (value!=null && !isPrimitiveWrapper(value)) {
			// if <o> is neither null nor primitive
			// wrapper, recurse
			Object newValue = finalize(value);
			// set the value of the field to the finalized
			// value
			field[i].set(o, newValue);
		    }
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    private void hashFields(Object o, Signature sig) {
	// for all the superclasses <c> of <o>...
	for (Class c = o.getClass(); c!=null; c = c.getSuperclass()) {
	    // for all the fields of <c>...
	    Field[] field = c.getDeclaredFields();
	    for (int i=0; i<field.length; i++) {
		// get the value for the field
		field[i].setAccessible(true);
		try {
		    Object value = field[i].get(o);
		    if (value==null || isPrimitiveWrapper(value)) {
			// if <o> is null or a primitive, hash the value
			sig.add(value);
		    } else {
			// otherwise, recurse
			hashFields(value, sig);
		    }
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    /**
     * Returns whether or not <o1> and <o2> are structurally equal, in
     * the sense of having the same field structure with all the leaf
     * nodes (primitives) being identical.
     */
    protected static boolean compareStructure(Object o1, Object o2) {
	// get the class of interest
	Class c = o1.getClass();
	// if <o2> is of a different class, return false
	if (c!=o2.getClass()) { return false; }
	// otherwise, iterate superclasses...
	for (; c!=null; c = c.getSuperclass()) {
	    // for all the fields of <c>...
	    Field[] field = c.getDeclaredFields();
	    for (int i=0; i<field.length; i++) {
		// get the value for the field
		field[i].setAccessible(true);
		try {
		    // get values from each
		    Object val1 = field[i].get(o1);
		    Object val2 = field[i].get(o1);
		    if (val1==val2) { 
			// if references are the same, continue
			continue;
		    } else if (isPrimitiveWrapper(val1) && 
			       isPrimitiveWrapper(val2)) {
			// if primitive values are the same, continue
			if (val1.equals(val2)) {
			    continue; 
			}
		    } else if (compareStructure(val1, val2)) {
			// if structures are the same, continue
			continue;
		    }
		    // otherwise, they're not equal, so fail
		    return false;
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		}
	    }
	}
	// if we survived all the tests, return true
	return true;
    }

    /**
     * Returns whether or not <o> is a wrapper for a primitive type
     */
    public static boolean isPrimitiveWrapper(Object o) {
	// these should be arranged in order of their frequency to get
	// short-circuiting of the OR expression in the common case
	return (
		o instanceof Integer ||
		o instanceof Character ||
		o instanceof Double ||
		o instanceof Float ||
		o instanceof Byte ||
		o instanceof Short ||
		o instanceof Long ||
		o instanceof Boolean
		);
	// the following doesn't work -- not comparing the right thing, somehow
	//	return o.getClass().isPrimitive();
    }

    /**
     * Represents the signature of an object -- that is, the object
     * identity of all its fields.
     */
    static class Signature {
	/**
	 * Hash code for this.
	 */
	private int hashCode;
	/**
	 * Object for which this is a signature for.
	 */
	private Object obj;
    
	/**
	 * Create a signature for Object <o>
	 */
	public Signature(Object obj) {
	    this.hashCode = 0;
	    this.obj = obj;
	}

	public void add(Object o) {
	    hashCode ^= o.hashCode();
	}

	/**
	 * Return true iff s.obj and this.obj have identical structures
	 * with identical primitive values at the leaves.  (This returns
	 * structural equality between s.obj and this.obj).
	 */
	public boolean equals(Signature s) {
	    return Memoizer.compareStructure(s.obj, this.obj);
	}

	public int hashCode() {
	    return hashCode;
	}
    }
}

package at.dms.kjc.sir;

import java.security.Permission;
import java.util.*;
import java.lang.reflect.*;

/**
 * In this implementation, an object is memoized iff it is immutable.
 */

public class Memoizer implements Finalizer {
    
    /**
     * The set of all objects that we have cached.
     */
    private HashSet cached;
    /**
     * Mapping from signature to cached object.
     */
    private Hashtable sigMap;
    
    private Memoizer() {
	this.cached = new HashSet();
	this.sigMap = new Hashtable();
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
    public static Finalizer create() {
	// return a memoizer
	return new Memoizer();
    }

    /*******************************************************************
     * Implementation of the finalizer interface.
     */

    /**
     * Given an Object <o> which may or may not be mutable, returns an
     * immutable object that is "equivalent" to <o>.
     */
    public Object finalize(Object o) {
	// if <o> is null or <o> is just a wrapper for a primitive
	// type, then just return <o>
	if (o==null || isPrimitiveWrapper(o)) { return o; }
	// if <o> is a String, then return the interned version
	if (o instanceof String) { return ((String)o).intern(); }
	// if object is cached already, return it
	if (cached.contains(o)) {
	    return o;
	}
	// otherwise, finalize all the fields of <o>
	Signature sig = finalizeFields(o);
	// if we have cached an object that's equivalent to the
	// field-finalized <o>, return that
	Object equiv = getEquivalent(sig);
	if (equiv!=null) {
	    return equiv;
	}
	// otherwise, cache <o> with <sig> and return it
	cache(sig, o);
	return o;
    }

    /**
     * Returns whether or not <o> can be mutated.  This should be
     * checked by mutator methods in <o> before they try to modify the
     * fields of <o>.
     */
    public boolean isMutable(Object o) {
	// optimized a little bit to try to improve performance of
	// common case
	return (!cached.contains(o) &&
		!isPrimitiveWrapper(o) &&
		! (o instanceof String));
    }

    /*******************************************************************/

    /**
     * Mutate all the fields of <o> to finalized versions, and returns
     * a signature for the object <o> based on its fields.
     */
    private Signature finalizeFields(Object o) {
	// make result
	Signature sig = new Signature(o);
	// for all the superclasses of <o>...
	for (Class c = o.getClass(); c!=null; c = c.getSuperclass()) {
	    // get the fields of <c>
	    Field[] field = c.getDeclaredFields();
	    // replace all the fields with their finalized values
	    for (int i=0; i<field.length; i++) {
		field[i].setAccessible(true);
		try {
		    Object value = field[i].get(o);
		    field[i].set(o, finalize(value));
		    sig.add(value);
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		}
	    }
	}
	// return signature
	return sig;
    }

    /**
     * If we have cached an object that has all the same fields as
     * <o>, then return it; otherwise, return null.
     */
    private Object getEquivalent(Signature sig) {
	return sigMap.get(sig);
    }

    /**
     * Adds an object <o> that has just been finalized to the caches
     * of this.
     */
    private void cache(Signature sig, Object o) {
	// add <o> to the cached list
	cached.add(o);
	// add <sig, o> pair to signature map
	sigMap.put(sig, o);
    }

    /**
     * Returns whether or not <o> is a wrapper for a primitive type
     */
    public static boolean isPrimitiveWrapper(Object o) {
	//	return o.getClass().isPrimitive();
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
    }
}

/**
 * Represents the signature of an object -- that is, the object
 * identity of all its fields.
 */
class Signature {
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
	this.hashCode = ~0;
	this.obj = obj;
    }

    public void add(Object o) {
	hashCode ^= o.hashCode();
    }

    /**
     * Return true iff all the fields of s.obj are equal (for objects,
     * in the sense of references, and for primitives, in the sense of
     * values) with that of this.obj.
     */
    public boolean equals(Signature s) {
	// first, make sure s.obj and <obj> are of the same type
	Class c = this.obj.getClass();
	Class c2 = s.obj.getClass();
	if (c!=c2) { return false; }
	// then iterate through all fields
	for ( ; c!=null; c = c.getSuperclass()) {
	    // get the fields of <c>
	    Field[] field = c.getDeclaredFields();
	    // make sure they're the same in all the classes.
	    for (int i=0; i<field.length; i++) {
		field[0].setAccessible(true);
		Object value1=null, value2=null;
		try {
		    value1 = field[0].get(this.obj);
		    value2 = field[0].get(s.obj);
		} catch (IllegalAccessException e) {
		    e.printStackTrace();
		}
		if (Memoizer.isPrimitiveWrapper(value1)) {
		    // do value equality for primitives
		    if (!value1.equals(value2)) {
			return false;
		    }
		} else {
		    // otherwise, do reference equality
		    if (value1!=value2) {
			return false;
		    }
		}
	    }
	}
	return true;
    }

    public int hashCode() {
	return hashCode;
    }
}

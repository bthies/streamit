package at.dms.kjc;

import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import at.dms.compiler.JavaStyleComment;

import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

public class AutoCloner {

    /**
     * List of things that should be cloned on the current pass.
     */
    private static LinkedList toBeCloned;
    /**
     * Mapping from old objects to their clones, so that object
     * equality is preserved across a cloning call.
     *
     * To avoid hashcode problems, this maps from RegistryWrapper to
     * Object.
     */
    private static HashMap registry;
    
    /**
     * Deep copy a KJC structure -- this is the toplevel function to
     * call.
     */
    static public Object deepCopy(JPhylum oldObj) {
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	oldObj.accept(visitor);
	toBeCloned = visitor.getToBeCloned();
	registry = new HashMap();
	return cloneToplevel(oldObj);
    }

    /*************************************************************
     * The following methods are all callbacks that should only be
     * called from the cloning process, not by the user.
     *************************************************************/

    /**
     * Indicate that <oldObj> is being cloned into <newObj>.  All
     * future references to <oldObj> should take <newObj> as the clone
     * instead of creating yet another clone.
     */
    static public void register(Object oldObj, Object newObj) {
	registry.put(new RegistryWrapper(oldObj), newObj);
    }

    /**
     * This is the toplevel helper function for the automatic cloner.
     * It should never be called directly by the user -- use deepCopy
     * for that.  This dictates the toplevel cloning policy, and is
     * called on every field that is cloned.
     */
    static public Object cloneToplevel(Object o) {
	// if we've already cloned <o>, then return the clone
	Object alreadyCloned = registry.get(new RegistryWrapper(o));
	if (alreadyCloned!=null) {
	    return alreadyCloned;
	}
	// otherwise, dispatch on type of <o>...
	String typeName = o.getClass().getName();
	// local variables require special treatment since their
	// references might be shared
	if (o instanceof JLocalVariable) {
	    return cloneJLocalVariable((JLocalVariable)o);
	} 	
	// immutable types -- don't clone them, might rely on
	// reference equality
	else if (o instanceof CType ||
		 o instanceof String ||
		 o instanceof PrintWriter ||
		 o instanceof at.dms.compiler.WarningFilter) {
	    // don't clone these since they're immutable or shouldn't be copied
	    return o;
	} 
	// other kjc classes, do deep cloning
	else if (CloneGenerator.inTargetClasses(typeName)) {
	    // first pass:  output deep cloning for everything in at.dms
	    Utils.assert(o instanceof DeepCloneable, "Should declare " + o.getClass() + " to implement DeepCloneable.");
	    return ((DeepCloneable)o).deepClone();
	}
	// hashtables -- clone along with contents
	else if (o instanceof Hashtable) {
	    return cloneHashtable((Hashtable)o);
	} 
	// arrays -- need to clone children as well
	else if (o.getClass().isArray()) {
	    Object[] result = (Object[])((Object[])o).clone();
	    cloneWithinArray(result);
	    return result;
	} 
	// enumerate the list types to make the java compiler happy
	// with calling the .clone() method
	else if (o instanceof LinkedList) {
	    List result = (List)((LinkedList)o).clone();
	    cloneWithinList(result);
	    return result;
	} else if (o instanceof Stack) {
	    List result = (List)((Stack)o).clone();
	    cloneWithinList(result);
	    return result;
	} else if (o instanceof Vector) {
	    List result = (List)((Vector)o).clone();
	    cloneWithinList(result);
	    return result;
	} 
	// unknown types
	else {
	    Utils.fail("Don't know how to clone field of type " + o.getClass());
	    return o;
	}
    }

    /**
     * Helper function.  Should only be called as part of automatic
     * cloning process.
     */
    static private Object cloneJLocalVariable(JLocalVariable var) {
	if (toBeCloned.contains(var)) {
	    return var.deepClone();
	} else {
	    return var;
	}
    }

    /**
     * Helper function.  Should only be called as part of automatic
     * cloning process.
     */
    static private Object cloneHashtable(Hashtable ht) {
	Hashtable result = new Hashtable();
	Enumeration e = ht.keys();
	while (e.hasMoreElements()) {
	    Object key = e.nextElement();
	    Object value = ht.get(key);
	    result.put(cloneToplevel(key), cloneToplevel(value));
	}
	return result;
    }

    /**
     * Helper function.  Should only be called as part of automatic
     * cloning process.
     */
    static private void cloneWithinList(List clone) {
	for (int i=0; i<clone.size(); i++) {
	    Object old = clone.get(i);
	    clone.set(i, cloneToplevel(old));
	}
    }

    /**
     * Helper function.  Should only be called as part of automatic
     * cloning process.
     */
    static private void cloneWithinArray(Object[] clone) {
	// clone elements
	for (int i=0; i<clone.length; i++) {
	    clone[i] = cloneToplevel(clone[i]);
	}
    }

    /**
     * This provides a hash-safe wrapper that only returns .equals if
     * the objects have object-equality.
     */
    static class RegistryWrapper {
	final Object obj;
	
	public RegistryWrapper(Object obj) {
	    this.obj = obj;
	}
	
	/**
	 * Hashcode of <obj>.
	 */
	public int hashCode() {
	    return obj.hashCode();
	}
	
	/**
	 * Only .equal if the objects have reference equality.
	 */
	public boolean equals(Object other) {
	    return other==obj;
	}
    }
}

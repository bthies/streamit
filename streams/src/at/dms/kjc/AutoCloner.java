package at.dms.kjc;

import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import at.dms.compiler.JavaStyleComment;
import at.dms.compiler.TokenReference;

import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

public class AutoCloner {
    /**
     * List of things that should be cloned on the current pass.
     */
    private static HashSet toBeCloned;
    /**
     * Mapping from old objects to their clones, so that object
     * equality is preserved across a cloning call.
     *
     * To avoid hashcode problems, this maps from RegistryWrapper to
     * Object.
     */
    private static HashMap registry;
    
    /**
     * Deep copy a stream structure.
     */
    static public Object deepCopy(SIRStream oldObj) {
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	IterFactory.createFactory().createIter(oldObj).accept(visitor);

	toBeCloned = visitor.getToBeCloned();
	registry = new HashMap();
	Object result = cloneToplevel(oldObj, null);
	// clear registry to promote GC
	registry = null;
	return result;
    }

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
	Object result = cloneToplevel(oldObj, null);
	// clear registry to promote GC
	registry = null;
	return result;
    }

    /**
     * Clone everything starting from this offset of the block
     * Useful in BranchAnalyzer
     */
    static public Object deepCopy(int offset,JBlock oldObj) {
	// set the list of what we should clone
	CloningVisitor visitor = new CloningVisitor();
	visitor.visitBlockStatement(offset,oldObj,oldObj.getComments());

	toBeCloned = visitor.getToBeCloned();
	registry = new HashMap();
	Object result = cloneToplevel(oldObj, null);
	// clear registry to promote GC
	registry = null;
	return result;
    }

    /**
     * Clone everything in a kopi2sir, useful for recursive stream
     * definitions.
     */
    static public Object deepCopy(Kopi2SIR kopi2sir) {
	// not sure what toBeCloned should be in this case... for now
	// make it empty.
	toBeCloned = new HashSet();

	registry = new HashMap();
	Object result = cloneToplevel(kopi2sir, null);
	registry = null;
	return result;
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
     *
     * The parent is the object (if any) that is <o>'s parent. In the
     * event that <o> is an SIROperator, its parent will be set to
     * <parent> after cloning.  Parent can be null if this case is
     * N/A.
     */
    static public Object cloneToplevel(Object o, Object parent) {
	// if it is null, keep it that way
	if (o==null) {
	    return null;
	}
	// if we've already cloned <o>, then return the clone
	Object alreadyCloned = registry.get(new RegistryWrapper(o));
	if (alreadyCloned!=null) {
	    return alreadyCloned;
	}
	// otherwise, we'll get a new cloned result for <o>.  
	Object result;
	// dispatch on type of <o>...
	String typeName = o.getClass().getName();
	// local variables require special treatment since their
	// references might be shared
	if (o instanceof JLocalVariable) {
	    result = cloneJLocalVariable((JLocalVariable)o);
	}
	// immutable types -- don't clone them, either because we
	// don't have to or because they might rely on reference
	// equality
	else if (typeName.startsWith("at.dms.kjc.C") ||
		 o instanceof JLiteral ||
		 o instanceof JavaStyleComment ||
		 o instanceof TokenReference ||
		 o instanceof SIRSplitType ||
		 o instanceof SIRJoinType ||
		 o instanceof String ||
		 o instanceof PrintWriter ||
		 o instanceof at.dms.compiler.WarningFilter) {
	    // don't clone these since they're immutable or shouldn't be copied
	    result = o;
	} 
	// other kjc classes, do deep cloning
	else if (CloneGenerator.inTargetClasses(typeName)) {
	    // first pass:  output deep cloning for everything in at.dms
	    Utils.assert(o instanceof DeepCloneable, "Should declare " + o.getClass() + " to implement DeepCloneable.");
	    result = ((DeepCloneable)o).deepClone();
	}
	// hashtables -- clone along with contents
	else if (o instanceof Hashtable) {
	    result = cloneHashtable((Hashtable)o, parent);
	} 
	// arrays -- need to clone children as well
	else if (o.getClass().isArray()) {
	    // don't clone arrays of certain element types...
	    boolean shouldClone;
	    Object[] arr = (Object[])o;
	    if (arr.length > 0) {
		Object elem = arr[0];
		// don't clone these array types
		if (elem instanceof JavaStyleComment) {
		    shouldClone = false;
		} else {
		    shouldClone = true;
		}
	    } else {
		shouldClone = true;
	    }
	    // clone array if we decided to
	    if (shouldClone) {
		result = ((Object[])o).clone();
	    } else {
		result = o;
	    }
	    register(o, result);
	    if (shouldClone) {
		cloneWithinArray((Object[])result, parent);
	    }
	}
	// enumerate the list types to make the java compiler happy
	// with calling the .clone() method
	else if (o instanceof LinkedList) {
	    result = ((LinkedList)o).clone();
	    register(o, result);
	    cloneWithinList((List)result, parent);
	} else if (o instanceof Stack) {
	    result = ((Stack)o).clone();
	    register(o, result);
	    cloneWithinList((List)result, parent);
	} else if (o instanceof Vector) {
	    result = ((Vector)o).clone();
	    register(o, result);
	    cloneWithinList((List)result, parent);
	}
	// unknown types
	else {
	    Utils.fail("Don't know how to clone field of type " + o.getClass());
	    result = o;
	}
	// try fixing parent
	fixParent(result, parent);
	// remember result
	register(o, result);
	return result;
    }

    /**
     * If o is an SIROperator, sets it's parent to <parent>.  Either
     * way, returns o (for convenience).
     */
    static private Object fixParent(Object o, Object parent) {
	if (o instanceof SIROperator && parent!=null && parent instanceof SIRContainer) {
	    ((SIROperator)o).setParent((SIRContainer)parent);
	}
	return o;
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
    static private Object cloneHashtable(Hashtable orig, Object parent) {
	Hashtable result = new Hashtable();
	register(orig, result);
	Enumeration e = orig.keys();
	while (e.hasMoreElements()) {
	    Object key = e.nextElement();
	    Object value = orig.get(key);
	    result.put(cloneToplevel(key, parent),
		       cloneToplevel(value, parent));
	}
	return result;
    }

    /**
     * Helper function.  Should only be called as part of automatic
     * cloning process.
     */
    static private void cloneWithinList(List clone, Object parent) {
	for (int i=0; i<clone.size(); i++) {
	    Object old = clone.get(i);
	    clone.set(i, cloneToplevel(old, parent));
	}
    }

    /**
     * Helper function.  Should only be called as part of automatic
     * cloning process.
     */
    static private void cloneWithinArray(Object[] clone, Object parent) {
	// clone elements
	for (int i=0; i<clone.length; i++) {
	    clone[i] = cloneToplevel(clone[i], parent);
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
	    return (other instanceof RegistryWrapper && ((RegistryWrapper)other).obj==obj);
	}
    }
}

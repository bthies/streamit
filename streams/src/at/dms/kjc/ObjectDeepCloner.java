//===========================================================================
//
//   FILE: ObjectDeepCloner.java:
//   
//   Author: Michael Gordon
//   Date: Wed Oct 17 14:03:33 2001
//
//   Function:  Deep clone using Serializable
//
//===========================================================================

package at.dms.kjc;

import at.dms.kjc.sir.*;
import at.dms.kjc.iterator.*;
import at.dms.util.*;
import at.dms.compiler.JavaStyleComment;

import java.io.*;
import java.util.*;
import java.lang.reflect.Array;

/**
 * This class implements general deep cloning using the serializable interface
 */
public class ObjectDeepCloner
{
    /**
     * List of things that should be cloned on the current pass.
     */
    private static HashSet toBeCloned;

    /**
     * List of objects we're preserving across a cloning operation.
     */
    private static LinkedList preserved;

    // so that nobody can accidentally create an ObjectCloner object
    private ObjectDeepCloner(){}

    /**
     * Deep copy a stream structure.
     */
    static public Object deepCopy(SIRStream oldObj) {
	if (!KjcOptions.clone_with_serialization) {
	    return AutoCloner.deepCopy(oldObj);
	} else {
	    // set the list of what we should clone
	    CloningVisitor visitor = new CloningVisitor();
	    IterFactory.createFactory().createIter(oldObj).accept(visitor);
	    toBeCloned = visitor.getToBeCloned();
	    return doCopy(oldObj);
	}
    }

    /**
     * Deep copy a KJC structure.
     */
    static public Object deepCopy(JPhylum oldObj) {
	if (!KjcOptions.clone_with_serialization) {
	    return AutoCloner.deepCopy(oldObj);
	} else {
	    // set the list of what we should clone
	    CloningVisitor visitor = new CloningVisitor();
	    oldObj.accept(visitor);
	    toBeCloned = visitor.getToBeCloned();
	    return doCopy(oldObj);
	}
    }

    /**
     * Clone everything starting from this offset of the block
     * Useful in BranchAnalyzer
     */
    static public Object deepCopy(int offset,JBlock oldObj) {
	if (!KjcOptions.clone_with_serialization) {
	    return AutoCloner.deepCopy(offset, oldObj);
	} else {
	    // set the list of what we should clone
	    CloningVisitor visitor = new CloningVisitor();
	    visitor.visitBlockStatement(offset,oldObj,oldObj.getComments());
	    toBeCloned = visitor.getToBeCloned();
	    return doCopy(oldObj);
	}
    }

    /**
     * Deep copy an array of KJC structures.  Assumes that all the
     * elements of the array are of the same type.
     */
    static public JPhylum[] deepCopy(JPhylum[] oldObj) {
	Class componentType = oldObj.getClass().getComponentType();
	JPhylum[] result = (JPhylum[])Array.newInstance(componentType, oldObj.length);

	for (int i=0; i<oldObj.length; i++) {
	    result[i] = (JPhylum)deepCopy(oldObj[i]);
	}
	return result;
    }

    /**
     * Deep copy a stream container, except do not clone any of its
     * child streams.  This means that the LIST of children is copied,
     * but the children themselves are not duplicated.  Splitters and
     * joiners are not considered as children - only SIRStreams.  If
     * <oldObj> is not an SIRContainer, then this has the same effect
     * as deepCopy.  
     *
     * This is only intended for use from the iterator package, and
     * should not be called from within the IR.
     static public Object shallowCopy(SIRStream oldObj) {
     // only do something different for containers
     if (!(oldObj instanceof SIRContainer)) {
     return deepCopy(oldObj);
     } 
     SIRContainer parent = (SIRContainer)oldObj;
     // set the list of what we should clone
     CloningVisitor visitor = new CloningVisitor();
     IterFactory.createFactory().createIter(parent).accept(visitor);
     toBeCloned = visitor.getToBeCloned();
     // subtract the list of <parent>'s children from the
     // toBeCloned list.
     for (ListIterator it=parent.getChildren().listIterator(); it.hasNext(); ) {
     Object o = it.next();
     if (toBeCloned.contains(o)) {
     toBeCloned.remove(o);
     }
     }
     return doCopy(parent);
     }
    */

    /**
     * Return a handle for <oldInstance> that it can store to protect
     * its identity across a serialization operation.
     */
    static public Object getHandle(Object oldInstance) {
	if (toBeCloned.contains(oldInstance)) {
	    return new Integer(-1);
	} else {
	    //System.err.println("Preserving across a cloning call: " + oldInstance.getClass());
	    preserved.add(oldInstance);
	    return new Integer(preserved.size() - 1);
	}
    }

    /**
     * Given that <newInstance> finds itself being unserialized, this
     * method returns what its new representation should be given that
     * it was handed <handle> prior to the serialization.
     */
    static public Object getInstance(Object handle, Object newInstance) {
	assert handle instanceof Integer:
            "DeepObjectCloner being called with a handle it didn't "
            + " give out:  handle is " + handle + " of type " +
            handle.getClass();

	int index = ((Integer)handle).intValue();
	// if the instance was not preserved, then return current instance
	if (index==-1) {
	    /*
	      System.err.println("Cloning container " + newInstance);
	    */
	    return newInstance;
	} else {
	    /*
	      System.err.println("Preserving container " + preserved.get(index));
	    */
	    // otherwise, return our old preserved version
	    return preserved.get(index);
	}
    }

    /**
     * Returns the deep clone of an object, if <cloneVars> is true
     * then clone vars also...
     */ 
    static private Object doCopy(Object oldObj)
    {
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;
	try
	    {
		// clear the list of objects we're preserving
		preserved = new LinkedList();
		// get an output stream ready
		ByteArrayOutputStream bos = 
		    new ByteArrayOutputStream();
		oos = new ObjectOutputStream(bos);
		// serialize and pass the object
		oos.writeObject(oldObj);  
		oos.flush();              
		ByteArrayInputStream bin = 
		    new ByteArrayInputStream(bos.toByteArray()); 
		ois = new ObjectInputStream(bin);                  
		// return the new object
		oos.close();
		ois.close();
		return ois.readObject(); 
	    }
	catch(Exception e)
	    {
		System.err.println("Exception in ObjectCloner = " + e);
		e.printStackTrace();
		System.exit(-1);
	 
	    }
	return null;
    }
}


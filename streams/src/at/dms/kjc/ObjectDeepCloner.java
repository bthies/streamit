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
import java.io.*;
import java.util.*;
import java.awt.*;


/**
 * This class implements general deep cloning using the serializable interface
 */
public class ObjectDeepCloner
{
    public static boolean deepCloneVars;

    /**
     * This is the first enclosing stream operator that is *outside*
     * the current region of cloning.  References to outside parent
     * should *not* be cloned, while references to other parents
     * should be.
     */
    public static SIROperator outsideParent;

    // so that nobody can accidentally create an ObjectCloner object
    private ObjectDeepCloner(){}

    /**
     * Deep copy a KJC structure.
     */
    static public Object deepCopy(JPhylum oldObj, 
				  boolean cloneVars) {
	setOutsideParent(oldObj);
	return doCopy(oldObj, cloneVars);
    }

    /**
     * Deep copy a stream structure.
     */
    static public Object deepCopy(SIROperator oldObj, 
				  boolean cloneVars) {
	setOutsideParent(oldObj);
	return doCopy(oldObj, cloneVars);
    }

    /**
     * Returns the deep clone of an object, if <cloneVars> is true
     * then clone vars also...
     */ 
    static private Object doCopy(Object oldObj, boolean cloneVars)
    {
	deepCloneVars = cloneVars;
       
	ObjectOutputStream oos = null;
	ObjectInputStream ois = null;
	try
	    {
		// clear the serialization vector
		SerializationVector.clear();
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

    /**
     * Finds the outside parent of <str>.
     */
    static private void setOutsideParent(SIROperator str) {
	outsideParent = str.getParent();
    }

    /**
     * Finds the outside parent of KjcObject <obj>.
     */
    static private void setOutsideParent(JPhylum obj) {
	// to find the parent of <obj>, we create an SLIRVisitor that
	// looks for stream objects and finds their parents.
	obj.accept(new SLIREmptyVisitor() {
		// visit init statements, where sub-streams are defined
		public void visitInitStatement(SIRInitStatement self,
					       JExpression[] args,
					       SIRStream target) {
		    ObjectDeepCloner.outsideParent = target.getParent();
		}});
    }
}

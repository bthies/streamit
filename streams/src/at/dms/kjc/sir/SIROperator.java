package at.dms.kjc.sir;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.iterator.*;
import at.dms.kjc.sir.lowering.LoweringConstants;

import java.util.List;
import java.util.LinkedList;
import java.io.*;

/**
 * This represents an operator in the stream graph.
 */
public abstract class SIROperator extends Utils implements Finalizable {
    /**
     * Keep a unique number for all SIROperators (for hashing,
     * debugging, etc.)
     */
    private static int MAX_NUMBER = 0;
    /**
     * A unique number for each sir operator.
     */
    private final int myNumber;

    /**
     * The stream structure containing this, or NULL if this is the
     * toplevel stream.
     */
    protected SIRContainer parent;

    /**
     * Constructs and operator with parent <parent>.
     */
    protected SIROperator(SIRContainer parent) {
	this.parent = parent;
	this.myNumber = MAX_NUMBER++;
    }

    protected SIROperator() {
	this.parent = null;
	this.myNumber = MAX_NUMBER++;
    }

    /**
     * Sets parent container
     */
    public void setParent(SIRContainer p){
	this.parent = p;
    }

     /**
      * TO BE REMOVED once immutable stuff is in place.
      *
      * Returns list of all parents.  The first element of the list is
      * the immediate parent of this, and the last element is the final
      * non-null ancestor of this.
      */
    public SIRContainer[] getParents() {
	LinkedList result = new LinkedList();
	SIRContainer parent = getParent();
	// make list of parents
	while (parent!=null) {
	    result.add(parent);
	    parent = parent.getParent();
	}
	return (SIRContainer[])result.toArray(new SIRContainer[0]);
    }

     /**
      * TO BE REMOVED once immutable IR is in place.
      *
      * Returns an expression that accesses the structure of the parent
      * of this, assuming that the toplevel structure of the last
      * parent is named as in LoweringConstants.getDataField()
      */
    public JExpression getParentStructureAccess() {
	// get parents of <str>
	SIRStream parents[] = getParents();
	
	// construct result expression
	JExpression result = LoweringConstants.getDataField();
	
	// go through parents from top to bottom, building up the
	// field access expression.
	for (int i=parents.length-2; i>=0; i--) {
	    // get field name for child context
	    String childName = parents[i].getRelativeName();
	    // build up cascaded field reference
	    result = new JFieldAccessExpression(/* tokref */
						null,
						/* prefix is previous ref*/
						result,
						/* ident */
						childName);
	}
	
	// return result
	return result;
    }

    /**
     * TO BE REMOVED once immutable stuff is in place.
     */
    public String getRelativeName() {
	if (parent==null) {
	    return null;
	} else {
	    if (parent instanceof SIRFeedbackLoop) {
		if (this==((SIRFeedbackLoop)parent).getLoop()) {
		    return "loop";
		} else {
		    return "body";
		}
	    } else {
		return "child_" + parent.indexOf((SIRStream)this);
	    }
	}
    }
    
    public abstract Object accept(AttributeStreamVisitor v);

  // ----------------------------------------------------------------------
  // CLONING STUFF
  // ----------------------------------------------------------------------

    private Object serializationHandle;
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
	this.serializationHandle = ObjectDeepCloner.getHandle(parent);
	
	if (((Integer)serializationHandle).intValue()>=0) {
	    // if we got a handle, erase our parent for the write object call
	    SIRContainer temp = this.parent;
	    this.parent = null;
	    oos.defaultWriteObject();
	    this.parent = temp;
	} else {
	    // otherwise just write the parent
	    oos.defaultWriteObject();
	}
    }
    
    protected Object readResolve() throws Exception {
	Object o = ObjectDeepCloner.getInstance(serializationHandle, this);
	if (o!=this) {
 	    // if we had a handle, reset parent before returning
	    this.parent = (SIRContainer)o;
	}
	return this;
    }

  // ----------------------------------------------------------------------

    /**
     * Returns an identifier for this which is NOT unique.  This will
     * return the same string for a given type of filter that was
     * added to the stream graph.
     */
    public abstract String getIdent();

    /**
     * Returns a UNIQUE name for this.  That is, if a given stream
     * operator was added in multiple positions of the stream graph,
     * then this will return a different name for each instantiation.
     */
    public String getName() {
	return getIdent() + "_" + myNumber;
    }

    /**
     * Returns the parent of this.
     */
    public SIRContainer getParent() {
	return parent;
    }

    public int hashCode() {
	return myNumber;
    }
    
    /**
     * This should be called in every mutator.
     */
    public void assertMutable() {
	Utils.assert(!IterFactory.isFinalized(this), 
		     "A mutability check failed.");
    }
}

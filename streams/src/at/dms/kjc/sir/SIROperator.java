package at.dms.kjc.sir;

import at.dms.util.*;
import at.dms.kjc.*;
import at.dms.kjc.sir.lowering.Namer;
import at.dms.kjc.sir.lowering.LoweringConstants;

import java.util.List;
import java.util.LinkedList;
import java.io.*;

/**
 * This represents an operator in the stream graph.
 */
public abstract class SIROperator extends Utils implements Finalizable {
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
    }

    protected SIROperator() {
	parent = null;
    }

    /**
     * Sets parent container
     */
    public void setParent(SIRContainer p){
	this.parent = p;
    }

    /**
     * Accepts <v> at this node.
     */
    public abstract void accept(StreamVisitor v);

    /**
     * Accepts <v> at this node.
     */
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
     * Returns the name of this, or null if a name has not yet been
     * assigned.  This name should be unique for all objects in the
     * stream graph.
     */
    public String getName() {
	return Namer.getName(this);
    }

    /**
     * Returns the relative name of this.  The relative name is the
     * name by which the parent would refer to this child in a field
     * of a structure, for instance.  Many different objects in the
     * stream graph can have the same relative name.  If the parent is
     * null, then this returns null, as well.
     */
    public String getRelativeName() {
	if (parent==null) {
	    return null;
	} else {
	    return parent.getChildName(this);
	}
    }

    /**
     * Returns the parent of this.
     */
    public SIRContainer getParent() {
	return parent;
    }
    
    /**
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
     * This should be called in every mutator.
     */
    public void assertMutable() {
	Utils.assert(!StreaMITMain.memoizer.isFinalized(this), 
		     "A mutability check failed.");
    }
}

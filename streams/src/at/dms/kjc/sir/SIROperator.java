package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.kjc.sir.lowering.Namer;

import java.util.List;
import java.util.LinkedList;

/**
 * This represents an operator in the stream graph.
 */
public abstract class SIROperator extends at.dms.util.Utils {
    /**
     * The stream structure containing this, or NULL if this is the
     * toplevel stream.
     */
    protected SIRStream parent;

    /**
     * Constructs and operator with parent <parent>.
     */
    protected SIROperator(SIRStream parent) {
	this.parent = parent;
    }

    protected SIROperator() {
	parent = null;
    }

    /**
     * Sets parent SIRStream
     */
    public void setParent(SIRStream p){
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

    /**
     * Returns the name of this, or null if a name has not yet been assigned.
     */
    public String getName() {
	return Namer.getName(this);
    }

    /**
     * Returns the parent of this.
     */
    public SIRStream getParent() {
	return parent;
    }
    
    /**
     * Returns list of all parents.  The first element of the list is
     * the immediate parent of this, and the last element is the final
     * non-null ancestor of this.
     */
    public SIRStream[] getParents() {
	LinkedList result = new LinkedList();
	SIRStream parent = getParent();
	// make list of parents
	while (parent!=null) {
	    result.add(parent);
	    parent = parent.getParent();
	}
	return (SIRStream[])result.toArray(new SIRStream[0]);
    }

}

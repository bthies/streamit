package at.dms.kjc.sir;

import at.dms.kjc.*;
import java.util.List;

/**
 * This represents a 1-to-1 stream that can contain other streams as a
 * hierarchical unit.
 */
public abstract class SIRContainer extends SIRStream {

    protected SIRContainer() {
	super();
    }

    protected SIRContainer(SIRContainer parent,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods) {
      super(parent, fields, methods);
    }

    /**
     * Returns the relative name by which this object refers to child
     * <child>, or null if <child> is not a child of this.
     */
    public abstract String getChildName(SIROperator str);

    /**
     * Returns a list of the children of this.  The children are
     * stream objects that are contained within this.
     */
    public abstract List getChildren();
    
    /**
     * Returns a list of tuples (two-element arrays) of SIROperators,
     * representing a tape from the first element of each tuple to the
     * second.
     */
     public abstract List getTapePairs();
    
}



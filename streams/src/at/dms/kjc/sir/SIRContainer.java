package at.dms.kjc.sir;

import at.dms.kjc.*;
import at.dms.util.*;
import java.util.List;
import java.io.*;

/**
 * This represents a 1-to-1 stream that can contain other streams as a
 * hierarchical unit.
 */
public abstract class SIRContainer extends SIRStream {

    protected SIRContainer() {
	super();
    }

    protected SIRContainer(SIRContainer parent,
			   String ident,
			   JFieldDeclaration[] fields,
			   JMethodDeclaration[] methods) {
      super(parent, ident, fields, methods);
    }

  // ----------------------------------------------------------------------
  // CLONING STUFF
  // ----------------------------------------------------------------------

    private Object serializationHandle;
    
    private void writeObject(ObjectOutputStream oos) throws IOException {
	this.serializationHandle = ObjectDeepCloner.getHandle(this);
	oos.defaultWriteObject();
    }
    
    protected Object readResolve() throws Exception {
	return ObjectDeepCloner.getInstance(serializationHandle, this);
    }

  // ----------------------------------------------------------------------

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
    
    /**
     * Replaces <oldStr> with <newStr> in this.  Requires that
     * <oldStr> is an immediate child of this.  (It does not do a
     * deep-replacement.)  Also, it does not mend the calls in the
     * init function to the newStr.
     */
    public abstract void replace(SIRStream oldStr, SIRStream newStr);
    
}



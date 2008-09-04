package at.dms.kjc.slicegraph;

import java.util.*;

/**
 * This class represents the edge that connects two slice nodes in the same 
 * slice.  
 * 
 * @author mgordon
 *
 */
public class IntraSliceEdge extends Edge implements at.dms.kjc.DeepCloneable {
    /**
     * No argument constructor, FOR AUTOMATIC CLONING ONLY.
     */
    private IntraSliceEdge() {
        super();
    }
    
    public IntraSliceEdge(SliceNode src, SliceNode dst) {
        super(src, dst);
        //assert src.getParent() == dst.getParent(); //can't assume that parent pointer is set up
        //could use a lot more checking here, but at this point, not really crucial
    }

    /** THE FOLLOWING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */

    /** Returns a deep clone of this object. */
    public Object deepClone() {
        at.dms.kjc.slicegraph.IntraSliceEdge other = new at.dms.kjc.slicegraph.IntraSliceEdge();
        at.dms.kjc.AutoCloner.register(this, other);
        deepCloneInto(other);
        return other;
    }

    /** Clones all fields of this into <pre>other</pre> */
    protected void deepCloneInto(at.dms.kjc.slicegraph.IntraSliceEdge other) {
        super.deepCloneInto(other);
    }

    /** THE PRECEDING SECTION IS AUTO-GENERATED CLONING CODE - DO NOT MODIFY! */
}

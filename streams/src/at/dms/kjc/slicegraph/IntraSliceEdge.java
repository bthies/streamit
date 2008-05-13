package at.dms.kjc.slicegraph;

import java.util.*;

/**
 * This class represents the edge that connects two slice nodes in the same 
 * slice.  
 * 
 * @author mgordon
 *
 */
public class IntraSliceEdge extends Edge {
    
    public IntraSliceEdge(SliceNode src, SliceNode dst) {
        super(src, dst);
        //assert src.getParent() == dst.getParent(); //can't assume that parent pointer is set up
        //could use a lot more checking here, but at this point, not really crucial
    }
}

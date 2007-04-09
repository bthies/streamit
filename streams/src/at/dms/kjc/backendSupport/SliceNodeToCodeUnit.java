package at.dms.kjc.backendSupport;
import at.dms.kjc.sir.SIRCodeUnit;
import java.util.*;
import at.dms.kjc.slicegraph.SliceNode;

/**
 * This small utility module meant to allow tracking of code that has already been generated.
 * @author dimock
 */
public class SliceNodeToCodeUnit {
    static private Map<SliceNode,SIRCodeUnit> sliceNodeToCodeUnit = new HashMap<SliceNode,SIRCodeUnit>() ;
    /**
     * Use findCodeForSlice, addCodeForSlice to keep track of whether a SIRCodeUnit of code has been
     * generated already for a SliceNode.
     * @param s  A SliceNode
     * @return  The SIRCodeUnit added for the SliceNode by addCodeForSlice.
     */
    public static SIRCodeUnit findCodeForSliceNode(SliceNode s) {
        return sliceNodeToCodeUnit.get(s);
    }
   
    /**
     * Record a mapping from a SliceNode to a SIRCodeUnit.
     * Used to track out-of-sequence code generation to eliminate duplicates. 
     * @param s a SliceNode
     * @param u a SIRCodeUnit
     */
    public static void addCodeForSliceNode(SliceNode s, SIRCodeUnit u) {
        sliceNodeToCodeUnit.put(s, u);
    }
   
    /**
     * Clean up static data.
     */
    public void reset() {
        sliceNodeToCodeUnit = new HashMap<SliceNode,SIRCodeUnit>() ;
    }
}

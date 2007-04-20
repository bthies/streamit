package at.dms.kjc.backendSupport;

import at.dms.kjc.slicegraph.Edge;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.FilterSliceNode;

/**
 * Centralize calculation of buffer sizes
 * @author dimock  refactored from mgordon
 *
 */

public class BufferSize {
    
    /**
     * Return the number of items passing over an edge.
     * This is the max of the number that can pass in the init phase
     * and the number that can pass in the steady state phase.
     * @param theEdge
     * @return
     */
    public static int calculateSize(Edge theEdge) {
        if (theEdge.getSrc().isFilterSlice()) {  // filter->filter, filter->output
            // the init size is the max of the multiplicities for init and prime-pump
            // times the push rate
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getSrc());
            int maxItems = Math.max(initPush(fi), steadyPush(fi));
            // steady is just pop * mult
            return maxItems;
        } else if (theEdge.getDest().isFilterSlice()) { // joiner->filter
            // calculate the maximum number of elements that we may need 
            // upstream of a filter.
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getDest());
            // on init: have calculation:
            int initSize = fi.initItemsReceived();
            // in steady state: max(peek, pop+remaining) if multiplicity == 1
            // if multiplicity > 1, then account for more popped.
            int steadySize = (fi.steadyMult - 1) * fi.pop +
            Math.max(fi.peek, fi.pop+fi.remaining);
            // return larger of two.
            int maxSize = Math.max(initSize, steadySize);
            return maxSize;
        } else {
            assert theEdge instanceof InterSliceEdge;
            int maxItems = Math.max(((InterSliceEdge)theEdge).initItems(), 
                    ((InterSliceEdge)theEdge).steadyItems());
            return maxItems;
        }
    }
    
    /** number of items pushed during init / pre-work, prime-pump?? */
    private static int initPush(FilterInfo fi) {
        int items= fi.initMult * fi.push;
        // account for the initpush.
        // currently overestimates if there is a preWork and it pushes
        // less than work does.
        if (fi.push < fi.prePush) {
            items += (fi.prePush - fi.push);
        }
        return items;
    }
    
    /** number of items pushed in a steady state (takes multiplicity into account). */
    private static int steadyPush(FilterInfo fi) {
        return fi.push*fi.steadyMult;
    }
    
}

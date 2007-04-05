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
        if (theEdge.getSrc().isFilterSlice()) {  // filter->splitter
            // the init size is the max of the multiplicities for init and pp
            // times the push rate
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getSrc());
            int maxItems = fi.initMult;
            maxItems *= fi.push;
            // account for the initpush
            if (fi.push < fi.prePush)
                maxItems += (fi.prePush - fi.push);
            maxItems = Math.max(maxItems, fi.push*fi.steadyMult);
            // steady is just pop * mult
            return maxItems;
        } else if (theEdge.getDest().isFilterSlice()) { // joiner->filter or filter->filter
            // this is not a perfect estimation but who cares
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getDest());
            int maxItems = fi.initMult;
            maxItems *= fi.pop;
            // now account for initpop, initpeek, peek
            maxItems += (fi.prePeek + fi.prePop + fi.prePeek);
            maxItems = Math.max(maxItems, fi.pop* fi.steadyMult);
            // steady is just pop * mult
            return maxItems;
        } else {
            assert theEdge instanceof InterSliceEdge;
            int maxItems = Math.max(((InterSliceEdge)theEdge).initItems(), 
                    ((InterSliceEdge)theEdge).steadyItems());
            return maxItems;
        }
    }
}

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
            // the init size is the max of the multiplicities for init and prime-pump
            // times the push rate
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getSrc());
            int maxItems = Math.max(initPush(fi), steadyPush(fi));
            // steady is just pop * mult
            return maxItems;
        } else if (theEdge.getDest().isFilterSlice()) { // joiner->filter or filter->filter
            // this is not a perfect estimation but who cares
            FilterInfo fi = FilterInfo.getFilterInfo((FilterSliceNode) theEdge.getDest());
            int maxItems = Math.max(initPop(fi), steadyPop(fi));
            // steady is just pop * mult
            return maxItems;
        } else {
            assert theEdge instanceof InterSliceEdge;
            int maxItems = Math.max(((InterSliceEdge)theEdge).initItems(), 
                    ((InterSliceEdge)theEdge).steadyItems());
            return maxItems;
        }
    }
    
    /** number of items pushed during init / pre-work, prime-pump?? */
    public static int initPush(FilterInfo fi) {
        int items= fi.initMult;
        items = fi.push;
        // account for the initpush
        if (fi.push < fi.prePush) {
            items += (fi.prePush - fi.push);
        }
        return items;
    }
    
    /** number of items pushed in a steady state (takes multiplicity into account). */
    public static int steadyPush(FilterInfo fi) {
        return fi.push*fi.steadyMult;
    }
    
    /** (Somewhat innaccurate according to Gordo) number of items popped in init state. */
    public static int initPop(FilterInfo fi) {
        int items = fi.initMult;
        items *= fi.pop;
        // now account for initpop, initpeek, peek
        items += (fi.prePeek + fi.prePop + fi.prePeek);
        return items;
    }
    
    /** number of items popped in a steady state (takes multiplicity into account). */
    public static int steadyPop(FilterInfo fi) {
        return fi.pop* fi.steadyMult;
    }
}

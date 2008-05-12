/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Comparator;

import at.dms.kjc.slicegraph.Slicer;
import at.dms.kjc.slicegraph.Slice;

/**
 * A Comparator for the work estimation of slices that compares slices
 * based on the amount of work in the bottleneck (the filter of the slice
 * that performs the most work).
 * 
 * @author mgordon
 *
 */
public class CompareSliceBNWork implements Comparator<Slice> {
    /** The partition we used */
    private Slicer slicer;
    
    /**
     * Create a new object that uses the work estimates of partitioner.
     * 
     * @param slicer
     */
    public CompareSliceBNWork(Slicer slicer) {
        this.slicer = slicer;
    }
    
    /**
     * Compare the bottleneck work of Slice <pre>o1</pre> with Slice <pre>o2</pre>.
     * 
     * @return The comparison 
     */
    public int compare(Slice o1, Slice o2) {
//        assert o1 instanceof Slice && o2 instanceof Slice;
        
        if (slicer.getSliceBNWork((Slice) o1) < slicer
                .getSliceBNWork((Slice) o2))
            return -1;
        else if (slicer.getSliceBNWork((Slice) o1) == slicer
                .getSliceBNWork((Slice) o2))
            return 0;
        else
            return 1;
    }
}

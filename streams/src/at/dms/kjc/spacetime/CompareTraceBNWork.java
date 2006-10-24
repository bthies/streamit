/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.Comparator;

import at.dms.kjc.slicegraph.Partitioner;

/**
 * A Comparator for the work estimation of slices that compares slices
 * based on the amount of work in the bottleneck (the filter of the slice
 * that performs the most work).
 * 
 * @author mgordon
 *
 */
public class CompareTraceBNWork implements Comparator {
    /** The partition we used */
    private Partitioner partitioner;
    
    /**
     * Create a new object that uses the work estimates of partitioner.
     * 
     * @param partitioner
     */
    public CompareTraceBNWork(Partitioner partitioner) {
        this.partitioner = partitioner;
    }
    
    /**
     * Compare the bottleneck work of Trace <pre>o1</pre> with Trace <pre>o2</pre>.
     * 
     * @return The comparison 
     */
    public int compare(Object o1, Object o2) {
        assert o1 instanceof Trace && o2 instanceof Trace;
        
        if (partitioner.getTraceBNWork((Trace) o1) < partitioner
                .getTraceBNWork((Trace) o2))
            return -1;
        else if (partitioner.getTraceBNWork((Trace) o1) == partitioner
                .getTraceBNWork((Trace) o2))
            return 0;
        else
            return 1;
    }
}
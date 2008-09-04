
package at.dms.kjc.spacetime;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SIRSlicer;
import at.dms.kjc.KjcOptions;

/**
 * @author mgordon / dimock
 * The class creates the steady-state space time schedule for the partitioned graph.
 * 
 * BasicGenerateSteadyStateSchedule assumes that it has N nodes that are all identical
 * and have access to I/O (as opposed to the version for Raw Spacetime that needs to
 * track File Readers and File Writers).
 * 
 * The current scheduling algorithm:
 * If KjcOptions.spacetime is not set or if KjcOptions.noswpipe is set, then schedule in DataFlowOrder.
 * Else schedule in decreasing order by amount of work (CompareSliceBNWork / Partitioner)
 */
public class BasicGenerateSteadyStateSchedule {
    private BasicSpaceTimeSchedule spaceTime;
    private SIRSlicer slicer;
    private LinkedList<Slice> schedule;
    
    /**
     * 
     * @param sts
     * @param layout The layout of filterTraceNode->RawTile, this could
     * be null if we are --noanneal. 
     */
    public BasicGenerateSteadyStateSchedule(BasicSpaceTimeSchedule sts, SIRSlicer slicer) {
      
        spaceTime = sts;
        this.slicer = slicer;
        schedule = new LinkedList<Slice>();
    }
    
    
    public void schedule() {
        if (! KjcOptions.spacetime || KjcOptions.noswpipe) {
            spaceTime.setSchedule(DataFlowOrder.getTraversal
                    (slicer.getSliceGraph()));
        }
        else {
            //for now just call schedule work, may want other schemes later
            scheduleWork();
            spaceTime.setSchedule(schedule);
        }
        printSchedule();
    }
    
    /**
     * Create a space / time schedule for the traces of the graph 
     * trying to schedule the traces with the most work as early as possible.
     */
    private void scheduleWork() {
        // sort traces into decreasing order by bottleneck work.
        Slice[] tempArray = (Slice[]) slicer.getSliceGraph().clone();
        Arrays.sort(tempArray, new CompareSliceBNWork(slicer));
        LinkedList<Slice> sortedTraces = new LinkedList<Slice>(Arrays.asList(tempArray));
        Collections.reverse(sortedTraces);

//        CommonUtils.println_debugging("Sorted Traces: ");
//        for (Slice slice : sortedTraces) {
//            CommonUtils.println_debugging(" * " + slice + " (work: "
//                               + partitioner.getSliceBNWork(slice) + ")");
//        }
        
        while (!sortedTraces.isEmpty()) {
            //remove the first trace, the trace with the most work
            Slice slice = sortedTraces.removeFirst();
            schedule.add(slice);
        }
    }
    
   
    private void printSchedule() {
        Iterator<Slice> sch = schedule.iterator();
        CommonUtils.println_debugging("Schedule: ");
        while (sch.hasNext()) {
            Slice slice = sch.next();
            CommonUtils.println_debugging(" ** " + slice);
        }

    }
    
}

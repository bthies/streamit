package at.dms.kjc.tilera;
/**
 * 
 */

import java.util.*;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndSlicer;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.KjcOptions;

/**
 * This class operates on the SpaceTimeSchedule and generates the preloop
 * schedule for the partitioned stream graph.  It will create a pre loop schedule
 * so that each slice can be double buffered meaning it will receive its input and 
 * send its output while computing.
 * 
 * @author mgordon
 *
 */
public class GeneratePrimePumpScheduleSMD {
    private SpaceTimeScheduleAndSlicer spaceTimeSchedule;
    //the execution count for each trace during the calculation of the schedule
    private HashMap<Object, Integer> exeCounts;
    
    
   
    public GeneratePrimePumpScheduleSMD(SpaceTimeScheduleAndSlicer sts) {
        spaceTimeSchedule = sts;
        exeCounts = new HashMap<Object, Integer>();
    }
    
    /**
     * Create the preloop schedule and place it in the SpaceTimeSchedule.
     */
    public void schedule(Slice[] sliceGraph) {
        LinkedList<LinkedList<Slice>> preLoopSchedule = new LinkedList<LinkedList<Slice>>();
    
        LinkedList<Slice> dataFlowTraversal = DataFlowOrder.getTraversal(sliceGraph);
              
        //keep adding slices and edges to the schedule until all traces can fire 
        while (!canEverythingFire(dataFlowTraversal)) {
            CommonUtils.println_debugging("Pre-loop Scheduling Step...");
            //the traces that are firing in the current step...
            LinkedList<Slice> currentStep = new LinkedList<Slice>();
            HashSet fired = new HashSet();
            
            /* XXX testing */   int maxsiz = dataFlowTraversal.size();
            /* XXX testing */   int usesiz = 0;
            for (Slice slice : dataFlowTraversal) {
                /* XXX testing */ assert usesiz < maxsiz;
                /* XXX testing */ usesiz++;
                //if the trace can fire, then fire it in this priming step
                if (canFire(slice)) {
                    currentStep.add(slice);
                    fired.add(slice);
                    CommonUtils.println_debugging("  Adding " + slice);
                }
                //check the outgoing edges of the slice to see if any of them can "fire"
                for (InterSliceEdge edge : slice.getTail().getDestSet()) {
                    if (canFire(edge)) {
                        fired.add(edge);
                        CommonUtils.println_debugging("  Adding " + edge);
                    }
                }
            }
            recordFired(fired);
            preLoopSchedule.add(currentStep);
        } 
       
        spaceTimeSchedule.setPrimePumpSchedule(preLoopSchedule);
    }

    /**
     * Record that the edge/slice has fired at this time in the calculation of the 
     * preloop schedule.
     * 
     * @param obj the edge or slice that has fired
     */
    private void recordFired(Set set) {
        for (Object obj: set) {
            assert obj instanceof InterSliceEdge || obj instanceof Slice;
            if (exeCounts.containsKey(obj)) {
                exeCounts.put(obj, exeCounts.get(obj) + 1);
            }
            else {
                exeCounts.put(obj, 1);
            }
        }
    }
    
    /**
     * Get the number of times (the iteration number) that the edge or slice has
     * executed at this point in the schedule.
     * 
     * @param obj 
     * @return The execution count (iteration number)
     */
    private int getExeCount(Object obj) {
        assert obj instanceof InterSliceEdge || obj instanceof Slice;
        if (exeCounts.containsKey(obj))
            return exeCounts.get(obj).intValue();
        else
            return 0;
    }
    
    /**
     * Return true if the trace can fire currently in the preloop schedule meaning
     * all of its dependencies are satisfied
     * @param slice
     * @return True if the trace can fire.
     */
    private boolean canFire(Object obj) {
        assert obj instanceof InterSliceEdge || obj instanceof Slice;
        int myExeCount = getExeCount(obj);
        
        if (obj instanceof Slice) {
            Slice slice = (Slice)obj;

            //check each of the depends to make sure that they have fired at least
            //one more time than me.
            for (InterSliceEdge edge : slice.getHead().getSources()) {
                
                int dependsExeCount = getExeCount(edge);
                assert !(myExeCount > dependsExeCount);
                if (myExeCount == dependsExeCount)
                    return false;
            }
            return true;
        } else {
            InterSliceEdge edge = (InterSliceEdge)obj;
            //check the edge to see if we can transfer data, meaning the upstream
            //slice has executed more than the edge has
            int upstreamExeCount = getExeCount(edge.getSrc().getParent());
            assert !(myExeCount > upstreamExeCount);
            return  (myExeCount != upstreamExeCount);
        }
    }
    
    /**
     * @return True if all the slices and edges in the stream graph can fire, 
     * end of pre-loop schedule.
     */
    private boolean canEverythingFire(LinkedList<Slice> dataFlowTraversal) {
        for (Slice slice : dataFlowTraversal) {
            for (InterSliceEdge edge : slice.getTail().getDestSet()) {
                if (!(canFire(edge))) {
                    System.out.println(slice + " ||| " + edge);
                    return false;
                }
            }
            if (!canFire(slice)) {
                System.out.println(slice);
                return false;
            }
        }
        return true;
    }
    
    
    /**
     * We only need to schedule io slices that split or join.  Otherwise
     * their function is folded in to the neighboring slice.
     * 
     * @param slice the slice to check
     * @return should this be counted as a trace that needs to fire.
     */
    private boolean shouldFireSlice(Slice slice) { 
        return !(slice.getHead().getNextFilter().isPredefined());
    }
}

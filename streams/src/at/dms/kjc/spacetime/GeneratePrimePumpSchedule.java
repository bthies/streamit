/**
 * 
 */
package at.dms.kjc.spacetime;

import java.util.*;

import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.DataFlowOrder;
/**
 * This class operates on the SpaceTimeSchedule and generates the preloop
 * schedule for the partitioned stream graph.  It will create a pre loop schedule
 * so that each trace can execute without respect to data-flow constraints in the 
 * steady-state (software pipelining).
 * 
 * @author mgordon
 *
 */
public class GeneratePrimePumpSchedule {
    private SpaceTimeSchedule spaceTimeSchedule;
    //the execution count for each trace during the calculation of the schedule
    private HashMap<Trace, Integer> exeCounts;
    
    
   
    public GeneratePrimePumpSchedule(SpaceTimeSchedule sts) {
        spaceTimeSchedule = sts;
        exeCounts = new HashMap<Trace, Integer>();
    }
    
    /**
     * Create the preloop schedule and place it in the SpaceTimeSchedule.
     */
    public void schedule() {
        LinkedList<LinkedList<Trace>> preLoopSchedule = new LinkedList<LinkedList<Trace>>();
        if (SpaceTimeBackend.NO_SWPIPELINE) {
            spaceTimeSchedule.setPrimePumpSchedule(preLoopSchedule);
            return;
        }
        
        LinkedList dataFlowTraversal = DataFlowOrder.getTraversal(spaceTimeSchedule.partitioner.getTraceGraph());
              
        //keep adding traces to the schedule until all traces can fire 
        while (!canEverythingFire(dataFlowTraversal)) {
            CommonUtils.println_debugging("Pre-loop Scheduling Step...");
            //the traces that are firing in the current step...
            LinkedList<Trace> currentStep = new LinkedList<Trace>();
           
            Iterator it = dataFlowTraversal.iterator();
            while (it.hasNext()) {
                Trace trace = (Trace)it.next();
                //if the trace can fire, then fire it in this init step
                if (canFire(trace)) {
                    currentStep.add(trace);
                    CommonUtils.println_debugging("  Adding " + trace);
                }
            }
            recordFired(currentStep);
            preLoopSchedule.add(currentStep);
        } 
       
        spaceTimeSchedule.setPrimePumpSchedule(preLoopSchedule);
    }

    /**
     * Record that the trace has fired at this time in the calculation of the 
     * preloop schedule.
     * @param trace
     */
    private void recordFired(LinkedList<Trace> fired) {
        Iterator<Trace> it = fired.iterator();
        while (it.hasNext()) {
            Trace trace = it.next();
            if (exeCounts.containsKey(trace)) {
                exeCounts.put(trace, new Integer(exeCounts.get(trace).intValue() + 1));
            }
            else {
                exeCounts.put(trace, new Integer(1));
            }
        }
    }
    
    /**
     * Get the number of times (the iteration number) that the trace has
     * executed at this point in the schedule.
     * @param trace
     * @return The execution count (iteration number)
     */
    private int getExeCount(Trace trace) {
        if (exeCounts.containsKey(trace))
            return exeCounts.get(trace).intValue();
        else
            return 0;
    }
    
    /**
     * Return true if the trace can fire currently in the preloop schedule meaning
     * all of its dependencies are satisfied
     * @param trace
     * @return True if the trace can fire.
     */
    private boolean canFire(Trace trace) {
        if (!shouldFire(trace))
            return false;
                   
        Trace[] depends = trace.getDependencies();
        
        int myExeCount = getExeCount(trace);
        
        //check each of the depends to make sure that they have fired at least
        //one more time than me.
        for (int i = 0; i < depends.length; i++) {
            //file input nodes can always fire
            if (spaceTimeSchedule.partitioner.isIO(depends[i]))
                continue;
            
            int dependsExeCount = getExeCount(depends[i]);
            assert !(myExeCount > dependsExeCount);
            if (myExeCount == dependsExeCount)
                return false;
        }
        return true;
    }
    
    /**
     * @return True if all the traces in the stream graph can fire, end of pre-loop schedule.
     */
    private boolean canEverythingFire(LinkedList dataFlowTraversal) {
        Iterator it = dataFlowTraversal.iterator();
        while (it.hasNext()) {
            Trace trace = (Trace)it.next();
            if (!shouldFire(trace))
                continue;
            if (!canFire(trace))
                return false;
        }
        return true;
    }
    
    
    /**
     * We only need to schedule io traces that split or join.  Otherwise
     * their function is folded in to the neighboring trace.
     * 
     * @param trace The trace
     * @return should this be counted as a trace that needs to fire.
     */
    private boolean shouldFire(Trace trace) {
        if (!spaceTimeSchedule.partitioner.isIO(trace))
            return true;
        if (trace.getHead().getNextFilter().isFileOutput()) {
            if (trace.getHead().oneInput())
                return false;
            else 
                return true;
        }
        else {
            if (trace.getTail().oneOutput())
                return false;
            else
                return true;
        }
    }
}
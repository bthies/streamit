/**
 * 
 */
package at.dms.kjc.smp;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import at.dms.kjc.KjcOptions;
import at.dms.kjc.backendSupport.BackEndFactory;
import at.dms.kjc.backendSupport.BackEndScaffold;
import at.dms.kjc.backendSupport.ComputeNodesI;
import at.dms.kjc.slicegraph.InputSliceNode;
import at.dms.kjc.slicegraph.InterSliceEdge;
import at.dms.kjc.slicegraph.OutputSliceNode;
import at.dms.kjc.slicegraph.SchedulingPhase;
import at.dms.kjc.slicegraph.SimpleSlice;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;

/**
 * @author mgordon
 *
 */
public class SMPBackEndScaffold extends BackEndScaffold {
    
    protected void beforeScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        // nothing to do in default case.
    }
    
    protected void betweenScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        // nothing to do in default case.
    }
    
   
    protected void afterScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        // nothing to do.
    }
    
    /**
     * Called in case of no software pipelining to determine if no
     * code should be created for this joiner, but it is allowable
     * to create code for the following filter(s) in the slice.
     * Not called if software pipelining.
     * <br/>
     * Historic leftover from RAW specetime schedule, which ignored
     * file inputs (which came from off-chip).
     * @param input InputSliceNode to consider for to a joiner.
     * @return
     */
    protected boolean doNotCreateJoiner(InputSliceNode input) {
        return false;
    }
    
    /**
     * Pass in a {@link BasicSpaceTimeSchedule schedule}, and get a set of {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode}s
     * and a set of (underspecified) {@link at.dms.kjc.backendSupport.Channel Buffer}s filled in.
     * @param schedule
     * @param computeNodes
     * @param resources The instance of BackEndFactory to be used for callbacks, data.
     */
    public void run(BasicSpaceTimeSchedule schedule, BackEndFactory resources) {
   
        ComputeNodesI computeNodes = resources.getComputeNodes();
        this.resources = resources;
        
        Slice slices[];

        beforeScheduling(schedule,resources);
        
        // schedule the initialization phase.
        slices = schedule.getInitSchedule();
        iterateInorder(slices, SchedulingPhase.INIT, computeNodes);
        // schedule the prime pump phase.
        // (schedule should be empty if not spacetime)
        slices = schedule.getPrimePumpScheduleFlat();
        iterateInorder(slices, SchedulingPhase.PRIMEPUMP, computeNodes);
        // schedule the steady-state phase.
        slices = schedule.getSchedule();

        betweenScheduling(schedule, resources);
      
        iterateInorder(slices, SchedulingPhase.STEADY, computeNodes);
        
        afterScheduling(schedule, resources);
    }
 
    /**
     * Iterate over the schedule of slices and over each node of each slice and 
     * generate the code necessary to fire the schedule.  Generate splitters and 
     * joiners intermixed with the trace execution...
     * 
     * @param slices The schedule to execute.
     * @param whichPhase True if the init stage.
     * @param computeNodes The collection of compute nodes.
     */
    protected void iterateInorder(Slice slices[], SchedulingPhase whichPhase,
                                       SMPMachine computeNodes) {
        Slice slice;

        for (int i = 0; i < slices.length; i++) {
            slice = (Slice) slices[i];
            //create code for joining input to the trace
            resources.processInputSliceNode((InputSliceNode)slice.getHead(),
                    whichPhase, computeNodes);
            //create the compute code and the communication code for the
            //filters of the trace
            resources.processFilterSliceNode(slice.getFilterNodes().get(0), whichPhase, computeNodes);
            //create communication code for splitting the output
            resources.processOutputSliceNode((OutputSliceNode)slice.getTail(),
                    whichPhase, computeNodes);
            
        }
    }  
}

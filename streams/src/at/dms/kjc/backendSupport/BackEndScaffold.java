// $Id: BackEndScaffold.java,v 1.5 2007-03-28 21:39:12 dimock Exp $
package at.dms.kjc.backendSupport;

import java.util.*;

import at.dms.kjc.slicegraph.*;
import at.dms.kjc.spacetime.BasicSpaceTimeSchedule;
import at.dms.kjc.KjcOptions;


/**
 * Create code for a partitioning of {@link at.dms.kjc.slicegraph.Slice Slice}s 
 * on a collection of {@link at.dms.kjc.backendSupport.ComputeNode ComputeNode}s.
 * Connections between the ComputeNode s are returned as 
 * under-specified {@link at.dms.kjc.backendSupport.Channel Buffer}s.
 * @author dimock
 * @param <ComputeNodesType> type needs to match the one in your instance of BackEndFactory
 * @param <ComputeNodeType> type needs to match the one in your instance of BackEndFactory
 * @param <CodeStoreType> type needs to match the one in your instance of BackEndFactory
 * @param <ComputeNodeSelectorArgType> type needs to match the one in your instance of BackEndFactory
 * 
 */
public class BackEndScaffold<
ComputeNodesType extends ComputeNodesI<?>, 
ComputeNodeType extends ComputeNode<?>, 
CodeStoreType extends ComputeCodeStore<?>, 
ComputeNodeSelectorArgType extends Object> {
    
    /** used to pass back-end factory around */
    protected BackEndFactory<ComputeNodesType,ComputeNodeType,CodeStoreType, ComputeNodeSelectorArgType> resources;

    /**
     * Use in subclasses to perform work before code is created.
     * Only needed if subclassing and need to share data generated in beforeScheduling code.
     * for any schedule.
     * @param schedule
     * @param resources the BackEndFactory used to redirect to correct code generation routines.
     */
    protected void beforeScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        // nothing to do in default case.
    }
    
    /**
     * Use in subclasses to perform work after code is created for all schedules.
       Only needed if subclassing and need to share data generated beforeScheduling or schedule code.
    * @param schedule
     * @param resources
     */
    protected void afterScheduling(BasicSpaceTimeSchedule schedule,
            BackEndFactory resources) {
        // nothing to do.
    }
    
    /**
     * Use in subclass to indicate that no code needs to be created for
     * this joiner.
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
    public void run(BasicSpaceTimeSchedule schedule,
            BackEndFactory<ComputeNodesType,ComputeNodeType,CodeStoreType, ComputeNodeSelectorArgType> resources) {
   
        ComputeNodesType computeNodes = resources.getComputeNodes();
        this.resources = resources;
        
        Slice traces[];

        beforeScheduling(schedule,resources);
        
        //the initialization stage!!
        traces = schedule.getInitSchedule();
        iterateInorder(traces, SchedulingPhase.INIT, computeNodes);
        //the prime pump stage!!
        traces = schedule.getPrimePumpScheduleFlat();
        iterateInorder(traces, SchedulingPhase.PRIMEPUMP, computeNodes);
        //the steady-state!!
        traces = schedule.getSchedule();
        
        if (KjcOptions.noswpipe) {
            iterateNoSWPipe(schedule.getScheduleList(), SchedulingPhase.STEADY, computeNodes);
        } else {
            //iterate over the joiners then the filters then 
            //the splitter, this will create a data-redistribution 
            //stage between the iterations that will improve performance 
            iterateJoinFiltersSplit(traces, SchedulingPhase.STEADY, computeNodes);
        }
        afterScheduling(schedule, resources);
    }
 
    /**
     * Iterate over the schedule of traces and over each node of each trace and 
     * generate the code necessary to fire the schedule.  Generate splitters and 
     * joiners intermixed with the trace execution...
     * 
     * @param traces The schedule to execute.
     * @param whichPhase True if the init stage.
     * @param computeNodes The collection of compute nodes.
     */
    private void iterateInorder(Slice traces[], SchedulingPhase whichPhase,
                                       ComputeNodesType computeNodes) {
        Slice slice;

        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
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
    
    /**
     * Iterate over the schedule of traces and over each node of each trace and 
     * generate the code necessary to fire the schedule.  Generate splitters and 
     * joiners first so that they will data will be redistributed before the filters
     * execute.
     * 
     * @param traces The schedule to execute.
     * @param whichPhase True if the init stage.
     * @param rawChip The raw chip
     */
    private void iterateJoinFiltersSplit(Slice traces[], SchedulingPhase whichPhase,
                                                ComputeNodesType computeNodes) {
        Slice slice;

        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
            //create code for joining input to the trace
            resources.processInputSliceNode((InputSliceNode)slice.getHead(),
                    whichPhase, computeNodes);
        }
        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
            //create the compute code and the communication code for the
            //filters of the trace
            if (slice instanceof SimpleSlice) {
                resources.processFilterSliceNode(((SimpleSlice)slice).getBody(), whichPhase, computeNodes);                
            } else {
                resources.processFilterSlices(slice, whichPhase, computeNodes);
            }
        }
        for (int i = 0; i < traces.length; i++) {
            slice = (Slice) traces[i];
            //create communication code for splitting the output
            resources.processOutputSliceNode((OutputSliceNode)slice.getTail(),
                    whichPhase, computeNodes);
        }
    }
    
    private void iterateNoSWPipe(List<Slice> scheduleList, SchedulingPhase whichPhase,
            ComputeNodesType computeNodes) {
        HashSet<OutputSliceNode> hasBeenSplit = new HashSet<OutputSliceNode>();
        HashSet<InputSliceNode> hasBeenJoined = new HashSet<InputSliceNode>();
        LinkedList<Slice> scheduled = new LinkedList<Slice>();
        LinkedList<Slice> needToSchedule = new LinkedList<Slice>();
        needToSchedule.addAll(scheduleList);
        
        
        while (needToSchedule.size() != 0) {
            //join everyone that can be joined
            for (int n = 0; n < needToSchedule.size(); n++) {
                Slice notSched = needToSchedule.get(n);

                // a joiner with 0 inputs does not create code.
                // presumably followed by a filter with 0 inputs that
                // may create code.
                if (notSched.getHead().noInputs()) {
                    hasBeenJoined.add(notSched.getHead());
                    continue;
                }

                // If a subclass of this says that there is no joiner code
                // then do not create this joiner.
                if (doNotCreateJoiner(notSched.getHead())) {
                    hasBeenJoined.add(notSched.getHead());
                    continue;
                }


                // joiner can not be created until upstream slpitters
                // feeding the joiner have all been created.
                // XXX WTF: Precludes feedback loops.
                boolean canJoin = true;
                for (InterSliceEdge inEdge : notSched.getHead().getSourceSet()) {
                    if (!hasBeenSplit.contains(inEdge.getSrc())) {
                        canJoin = false;
                        break;
                    }
                }
                if (! canJoin) { continue; }
                
                // create code for joining input to the trace
                hasBeenJoined.add(notSched.getHead());
                // System.out.println("Scheduling join of " + notSched.getHead().getNextFilter());
                resources.processInputSliceNode(notSched.getHead(), whichPhase,
                        computeNodes);

            } // end of for loop //join everyone that can be joined

            //create the compute code and the communication code for the
            //filters of the trace after joiner has been processed.
            while (needToSchedule.size() != 0) {
                Slice slice = needToSchedule.get(0);
                if (hasBeenJoined.contains(slice.getHead())) {
                    scheduled.add(slice);
                    if (slice instanceof SimpleSlice) {
                        resources.processFilterSliceNode(((SimpleSlice)slice).getBody(), whichPhase, computeNodes);
                    }
                    //System.out.println("Scheduling " + trace.getHead().getNextFilter());
                    needToSchedule.removeFirst();
                }
                else {
                    break;
                }
            }

        }
        
        //schedule any splits that have not occured
        // but whose preceeding filters in a slice have been scheduled.
        if (hasBeenSplit.size() != scheduled.size()) {
            for (int t = 0; t < scheduled.size(); t++) {
                if (!hasBeenSplit.contains(scheduled.get(t).getTail())) {
                    OutputSliceNode output = 
                        scheduled.get(t).getTail();
                    //System.out.println("Scheduling split of " + output.getPrevFilter()); 
                    resources.processOutputSliceNode(output,
                            whichPhase, computeNodes);
                    hasBeenSplit.add(output);
                }
            }
        }
        
        // schedule any joins that have not occured 
        // but whose following filters in the slice have been scheduled
        if (hasBeenJoined.size() != scheduled.size()) {
            for (int t = 0; t < scheduled.size(); t++) {
                if (!hasBeenJoined.contains(scheduled.get(t).getHead())) {
                    InputSliceNode input  = 
                        scheduled.get(t).getHead();
                    //System.out.println("Scheduling join of " + input.getNextFilter()); 
                    resources.processInputSliceNode(input,
                            whichPhase, computeNodes);
                    hasBeenJoined.add(input);
                }
            }
        }

    }
    
//    private void processOutputSliceNode(OutputSliceNode node, SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
//        // TODO
//    }
//    
//    private void processInputSliceNode(InputSliceNode node, SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
//        // TODO
//    }
//    
//    private void processFilterSliceNode(FilterSliceNode node, SchedulingPhase whichPhase, ComputeNodesI computeNodes) {
//        ComputeNode tile = layout.getComputeNode(node);
//        FilterInfo filterInfo = FilterInfo.getFilterInfo(node);
//        addComputeCode(whichPhase, tile, filterInfo);
//    }
    
    /** 
     * Based on what phase we are currently in, generate the compute code 
     * (filter) code to execute the phase at this currently.  This is done 
     * in ComputeCodeStore.java.
     * 
     * @param init
     * @param primepump
     * @param tile
     * @param filterInfo
     */
    private void addComputeCode(SchedulingPhase whichPhase,
            ComputeNodeType computeNode, FilterInfo filterInfo) {
        switch (whichPhase) {
        case INIT:
            computeNode.getComputeCode().addSliceInit(filterInfo, resources.getLayout());
            break;

        case PRIMEPUMP:
            computeNode.getComputeCode().addSlicePrimePump(filterInfo, resources.getLayout());
            break;
            
        case STEADY:
            computeNode.getComputeCode().addSliceSteady(filterInfo, resources.getLayout());
            break;

        default:
            throw new AssertionError("" + whichPhase);
        }
    }

    
}

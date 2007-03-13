
package at.dms.kjc.spacetime;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.DataFlowOrder;
import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;

/**
 * @author mgordon
 * The class creates the steady-state space time schedule for the partitioned graph.
 * It uses a discrete time simulation of the steady-state as it schedules to keep track 
 * of the resources used. 
 */
public class GenerateSteadyStateSchedule {
    //the current time of the discrete time simulation of the steady-state
    private int currentTime;
    //the time at which a tile will be idle
    private int[] tileAvail;    
    //the spacetime schedule object where we record the schedule
    private SpaceTimeSchedule spaceTime;
    private RawChip rawChip;
    //the schedule we are building
    private LinkedList<Slice> schedule;
    private Layout<RawTile> layout;
    
    /**
     * 
     * @param sts
     * @param layout The layout of filterTraceNode->RawTile, this could
     * be null if we are --noanneal. 
     */
    public GenerateSteadyStateSchedule(SpaceTimeSchedule sts, Layout layout) {
      
        this.layout = layout;
        spaceTime = sts;
        rawChip = spaceTime.getRawChip();
        schedule = new LinkedList<Slice>();
        tileAvail = new int[rawChip.getTotalTiles()];
        for (int i = 0; i < rawChip.getTotalTiles(); i++) {
            tileAvail[i] = 0;
        }
        currentTime = 0;
    }
    
    
    public void schedule() {
        if (SpaceTimeBackend.NO_SWPIPELINE) {
            spaceTime.setSchedule(DataFlowOrder.getTraversal
                    (spaceTime.getPartitioner().getSliceGraph()));
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
        // sort traces...
        Slice[] tempArray = (Slice[]) spaceTime.getPartitioner().getSliceGraph().clone();
        Arrays.sort(tempArray, new CompareSliceBNWork(spaceTime.getPartitioner()));
        LinkedList<Slice> sortedTraces = new LinkedList<Slice>(Arrays.asList(tempArray));

        // schedule predefined filters first, but don't put them in the
        // schedule just assign them tiles...
        //removePredefined(sortedTraces);

        // reverse the list
        Collections.reverse(sortedTraces);

        CommonUtils.println_debugging("Sorted Traces: ");
        Iterator<Slice> it = sortedTraces.iterator();
        while (it.hasNext()) {
            Slice slice = it.next();
            CommonUtils.println_debugging(" * " + slice + " (work: "
                               + spaceTime.getPartitioner().getSliceBNWork(slice) + ")");
        }

        
        // start to schedule the traces
        while (!sortedTraces.isEmpty()) {
            //remove the first trace, the trace with the most work
            Slice slice = sortedTraces.removeFirst();
          
            scheduleSlice(slice, sortedTraces);
        }
    }
    
   
    /**
     * Advance the current simulation time to the minimum avail time of all the tiles. 
     */
    private void advanceCurrentTime() {
        int newMin = 0;
        // set newMin to max of tileavail times
        for (int i = 0; i < tileAvail.length; i++)
            if (newMin < tileAvail[i])
                newMin = tileAvail[i];
        //only reset the current time if we are advancing time...
        if (newMin > currentTime)
            currentTime = newMin;
    }
    
    /**
     *  Take a slice that is ready to be scheduled (along with its layout) and
     *  make the necessary state changes to schedule the trace.
     *  
     * @param layout The layout hashmap
     * @param slice The Slice that is going to be scheduled for execution
     * @param sortedList The list of Traces that need to be scheduled
     */
    private void scheduleSlice(Slice slice,
                               LinkedList<Slice> sortedList) {
        assert slice != null;
        CommonUtils.println_debugging("Scheduling Slice: " + slice + " at time "
                           + currentTime);
        // remove this trace from the list of traces to schedule
        sortedList.remove(slice);
        // add the trace to the schedule
        schedule.add(slice);

        // now set the layout for the filterTraceNodes
        // and set the available time for each tile
        SliceNode node = slice.getHead().getNext();

        while (node instanceof FilterSliceNode) {
            
            RawTile tile = layout.getComputeNode(node.getAsFilter());

            // add to the avail time for the tile, use either the current time
            // or the tile's avail
            // whichever is greater
            // add the bottleneck work
            tileAvail[tile.getTileNumber()] = ((currentTime > tileAvail[tile
                                                                        .getTileNumber()]) ? currentTime : tileAvail[tile
                                                                                                                     .getTileNumber()])
                + spaceTime.getPartitioner().getSliceBNWork(slice);
            CommonUtils.println_debugging("   * new avail for " + tile + " = "
                               + tileAvail[tile.getTileNumber()]);
            // SpaceTimeBackend.println(" *(" + currentTime + ") Assigning " + node +
            // " to " + tile +
            // "(new avail: " + tileAvail[tile.getTileNumber()] + ")");

            //assert !(((FilterTraceNode) node).isFileInput() || ((FilterTraceNode) node)
            //         .isFileInput());

            // if this trace has file input take care of it,
            // assign the file reader and record that the tile reads a file...
            /*
            if (node.getPrevious().isInputTrace()) {
                InputTraceNode in = (InputTraceNode) node.getPrevious();
                // if this trace reads a file, make sure that we record that
                // the tile is being used to read a file
                if (in.hasFileInput()) {
                    spaceTime.setReadsFile(tile.getTileNumber());
                    assert in.getSingleEdge().getSrc().getPrevFilter()
                        .isPredefined();
                    assert layout.getTile(in.getSingleEdge().getSrc().getPrevFilter()) == tile;
                }
            }
            */
            // writes to a file, set tile for writer and record that the tile
            // writes a file
            /*
            if (node.getNext().isOutputTrace()) {
                OutputTraceNode out = (OutputTraceNode) node.getNext();
                if (out.hasFileOutput()) {
                    spaceTime.setWritesFile(tile.getTileNumber());
                    assert out.getSingleEdge().getDest().getNextFilter()
                        .isPredefined();
                    // set the tile
                    assert layout.getTile(out.getSingleEdge().getDest().getNextFilter()) == 
                        tile; 
                    
                }
            }
            */
            // set file reading and writing
            // set the space time schedule?? Jasper's stuff
            // don't do it for the first node
            /*
             * if (node != trace.getHead().getNext()) spSched.add(trace,
             * ((FilterTraceNode)node).getY(), ((FilterTraceNode)node).getX());
             * else //do this instead spSched.addHead(trace,
             * ((FilterTraceNode)node).getY(), ((FilterTraceNode)node).getX());
             */

            node = node.getNext();
        }
    }

    
    /**
     * Remove the predefined filters from the list of traces to 
     * schedule.  These should not be scheduled as they are not assigned
     * to tiles.
     * 
     * @param sortedTraces The traces of the graph
     */
    private void removePredefined(LinkedList sortedTraces) {
        for (int i = 0; i < spaceTime.getPartitioner().io.length; i++) {
            sortedTraces.remove(spaceTime.getPartitioner().io[i]);
        }
    } 
   

    private void printSchedule() {
        Iterator<Slice> sch = schedule.iterator();
        Slice prev = null;
        CommonUtils.println_debugging("Schedule: ");
        while (sch.hasNext()) {
            Slice slice = sch.next();
            CommonUtils.println_debugging(" ** " + slice);
            //System.out.println(" ** " + trace);
            prev = slice;
        }

    }
    
}

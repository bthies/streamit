
package at.dms.kjc.spacetime;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import at.dms.kjc.KjcOptions;

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
    private LinkedList schedule;
    /** the layout we are using from annealedlayout, it could be null
     * if we are --noanneal, if so the we can manual layout.
     */
    private HashMap layout;
    
    /**
     * 
     * @param sts
     * @param layout The layout of filterTraceNode->RawTile, this could
     * be null if we are --noanneal. 
     */
    public GenerateSteadyStateSchedule(SpaceTimeSchedule sts,
            HashMap layout) {
        if (layout == null)
            assert KjcOptions.noanneal;
        this.layout = layout;
        
        spaceTime = sts;
        rawChip = spaceTime.getRawChip();
        schedule = new LinkedList();
        tileAvail = new int[rawChip.getTotalTiles()];
        for (int i = 0; i < rawChip.getTotalTiles(); i++) {
            tileAvail[i] = 0;
        }
        currentTime = 0;
    }
    
    
    public void schedule() {
        //for now just call schedule work, may want other schemes later
        scheduleWork();
        spaceTime.setSchedule(schedule);
        //      set up dependencies
        printSchedule();
    }
    
    /**
     * Create a space / time schedule for the traces of the graph 
     * trying to schedule the traces with the most work as early as possible.
     */
    private void scheduleWork() {
        // sort traces...
        Trace[] tempArray = (Trace[]) spaceTime.partitioner.getTraceGraph().clone();
        Arrays.sort(tempArray, new CompareTraceBNWork(spaceTime.partitioner));
        LinkedList sortedTraces = new LinkedList(Arrays.asList(tempArray));

        // schedule predefined filters first, but don't put them in the
        // schedule just assign them tiles...
        removePredefined(sortedTraces);

        // reverse the list
        Collections.reverse(sortedTraces);

        System.out.println("Sorted Traces: ");
        Iterator it = sortedTraces.iterator();
        while (it.hasNext()) {
            Trace trace = (Trace) it.next();
            System.out.println(" * " + trace + " (work: "
                               + spaceTime.partitioner.getTraceBNWork(trace) + ")");
        }

        // start to schedule the traces
        while (!sortedTraces.isEmpty()) {
            //remove the first trace, the trace with the most work
            Trace trace = (Trace)sortedTraces.removeFirst();
            
            if (KjcOptions.noanneal) {
                //get the layout 
                layout = layoutTrace(trace);
            }
            //schedule the trace...
            scheduleTrace(layout, trace, sortedTraces);
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
        //only reset teh current time if we are advancing time...
        if (newMin > currentTime)
            currentTime = newMin;
    }
    
    /**
     *  Take a trace that is ready to be scheduled (along with its layout) and
     *  make the necessary state changes to schedule the trace.
     *  
     * @param layout The layout hashmap
     * @param trace The Trace that is going to be scheduled for execution
     * @param sortedList The list of Traces that need to be scheduled
     */
    private void scheduleTrace(HashMap layout, Trace trace,
                               LinkedList sortedList) {
        assert layout != null && trace != null;
        System.out.println("Scheduling Trace: " + trace + " at time "
                           + currentTime);
        // remove this trace from the list of traces to schedule
        sortedList.remove(trace);
        // add the trace to the schedule
        schedule.add(trace);

        // now set the layout for the filterTraceNodes
        // and set the available time for each tile
        TraceNode node = trace.getHead().getNext();

        while (node instanceof FilterTraceNode) {
            assert layout.containsKey(node) && layout.get(node) != null;
            RawTile tile = (RawTile) layout.get(node);
            ((FilterTraceNode) node).setXY(tile.getX(), tile.getY());

            // add to the avail time for the tile, use either the current time
            // or the tile's avail
            // whichever is greater
            // add the bottleneck work
            tileAvail[tile.getTileNumber()] = ((currentTime > tileAvail[tile
                                                                        .getTileNumber()]) ? currentTime : tileAvail[tile
                                                                                                                     .getTileNumber()])
                + spaceTime.partitioner.getTraceBNWork(trace);
            System.out.println("   * new avail for " + tile + " = "
                               + tileAvail[tile.getTileNumber()]);
            // System.out.println(" *(" + currentTime + ") Assigning " + node +
            // " to " + tile +
            // "(new avail: " + tileAvail[tile.getTileNumber()] + ")");

            assert !(((FilterTraceNode) node).isFileInput() || ((FilterTraceNode) node)
                     .isFileInput());

            // if this trace has file input take care of it,
            // assign the file reader and record that the tile reads a file...
            if (node.getPrevious().isInputTrace()) {
                InputTraceNode in = (InputTraceNode) node.getPrevious();
                // if this trace reads a file, make sure that we record that
                // the tile is being used to read a file
                if (in.hasFileInput()) {
                    spaceTime.setReadsFile(tile.getTileNumber());
                    assert in.getSingleEdge().getSrc().getPrevFilter()
                        .isPredefined();
                    // set the tile for the file reader to tbe this tile
                    in.getSingleEdge().getSrc().getPrevFilter().setXY(
                                                                      tile.getX(), tile.getY());
                }
            }

            // writes to a file, set tile for writer and record that the tile
            // writes a file
            if (node.getNext().isOutputTrace()) {
                OutputTraceNode out = (OutputTraceNode) node.getNext();
                if (out.hasFileOutput()) {
                    spaceTime.setWritesFile(tile.getTileNumber());
                    assert out.getSingleEdge().getDest().getNextFilter()
                        .isPredefined();
                    // set the tile
                    out.getSingleEdge().getDest().getNextFilter().setXY(
                                                                        tile.getX(), tile.getY());
                }
            }

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
        for (int i = 0; i < spaceTime.partitioner.io.length; i++) {
            sortedTraces.remove(spaceTime.partitioner.io[i]);
        }
    } 
   
    /**
     * Layout the trace on the RawChip.
     * @param trace The trace to be assigned tiles
     * @return HashMap of FilterTraceNode->RawTile
     */
    private HashMap layoutTrace(Trace trace) {
        /*if (getAvailTiles() < trace.getNumFilters())
          return null;*/
        HashMap layout = null;
        
        layout = ManualTraceLayout.layout(rawChip, trace);
        
        return layout;
    }

    private void printSchedule() {
        Iterator sch = schedule.iterator();
        Trace prev = null;
        System.out.println("Schedule: ");
        while (sch.hasNext()) {
            Trace trace = (Trace) sch.next();
            System.out.println(" ** " + trace);
         
            prev = trace;
        }

    }
    
}

/**
 * A Comparator for the work estimation of slices that compares slices
 * based on the amount of work in the bottleneck (the filter of the slice
 * that performs the most work).
 * 
 * @author mgordon
 *
 */
class CompareTraceBNWork implements Comparator {
    /** The partition we used */
    private Partitioner partitioner;

    public CompareTraceBNWork(Partitioner partitioner) {
        this.partitioner = partitioner;
    }

    /**
     * Compare the bottleneck work of Trace <o1> with Trace <o2>.
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

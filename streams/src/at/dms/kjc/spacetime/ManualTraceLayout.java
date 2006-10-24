/**
 * 
 */
package at.dms.kjc.spacetime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import at.dms.kjc.slicegraph.FilterTraceNode;
import at.dms.kjc.slicegraph.TraceNode;


/**
 * Given a trace, ask the user where he/she wants it placed on
 *              the raw chip.
 */
public class ManualTraceLayout implements Layout {
    private HashMap assignment;
    private SpaceTimeSchedule spaceTime;
    
    public ManualTraceLayout(SpaceTimeSchedule spaceTime) {
        this.spaceTime = spaceTime; 
    }
    
    public RawTile getTile(FilterTraceNode node) {
        assert assignment.containsKey(node);
        return (RawTile)assignment.get(node);
    }
    
    public void setTile(FilterTraceNode node, RawTile tile) {
        assignment.put(node, tile);
    }
    
    public void run() {
        assignment = new HashMap();
        //call layout on traces!
        Trace[] traces = spaceTime.partitioner.getTraceGraph();
        
        for (int i = 0; i < traces.length; i++) {
            if (!spaceTime.partitioner.isIO(traces[i]))
                layout(spaceTime.getRawChip(), traces[i]);
        }
        
        //now set the tiles of the i/o
        for (int i = 0; i < spaceTime.partitioner.io.length; i++) {
            Trace trace = spaceTime.partitioner.io[i];
            if (trace.getHead().getNextFilter().isFileOutput()) {
                //file writer
                assert trace.getHead().oneInput();
                //set this file writer tile to the tile of its upstream input
                assignment.put(trace.getHead().getNextFilter(),
                        getTile(trace.getHead().getSingleEdge().getSrc().getPrevFilter()));
            }
            else if (trace.getTail().getPrevFilter().isFileInput()) {
                //file reader
                assert trace.getTail().oneOutput();
                //set this file reader to the tile of its downstream reader
                assignment.put(trace.getTail().getPrevFilter(),
                        getTile(trace.getTail().getSingleEdge().getDest().getNextFilter()));
            }
            else 
                assert false : "Some unknown i/o trace...";
        }
        
        //now call buffer dram assignment
        new BufferDRAMAssignment().run(spaceTime, this);
        
        LayoutDot.printLayoutCost(spaceTime, this);
    }
    
    /**
     * Ask the user to lay out the trace on the raw chip.
     * @param rawChip The Raw Chip 
     * @param trace The Trace we would want to layout out on <pre>rawChip</pre>
     */
    private void layout(RawChip rawChip, Trace trace) {
        BufferedReader inputBuffer = 
            new BufferedReader(new InputStreamReader(
                    System.in));
        // the current node we are getting the tile assignment for
        TraceNode node = trace.getHead().getNext();
        // the tile number we are assigning
        int tileNumber;
        String str = "";
        RawTile tile;
        
        System.out.println("Enter layout for trace: " + trace);
        
        while (node instanceof FilterTraceNode) {
            while (true) {
                System.out.print("Enter tile number for " + node + ": ");

                try {
                    str = inputBuffer.readLine();
                    tileNumber = Integer.valueOf(str).intValue();
                } catch (Exception e) {
                    System.out.println("Bad number " + str);
                    continue;
                }

                if (tileNumber < 0 || tileNumber >= rawChip.getTotalTiles()) {
                    System.out.println("Bad tile number!");
                    continue;
                }
                tile = rawChip.getTile(tileNumber);
                // other wise the assignment is valid, assign and break!!
                System.out.println("Assigning " + node.toString() + " to tile "
                                   + tileNumber);
                assignment.put(node, tile);
                break;
            }
            node = node.getNext();
        }
    }

}
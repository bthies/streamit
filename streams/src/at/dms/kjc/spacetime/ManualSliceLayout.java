/**
 * 
 */
package at.dms.kjc.spacetime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import at.dms.kjc.slicegraph.FilterSliceNode;
import at.dms.kjc.slicegraph.Slice;
import at.dms.kjc.slicegraph.SliceNode;


/**
 * Given a trace, ask the user where he/she wants it placed on
 *              the raw chip.
 */
public class ManualSliceLayout implements Layout {
    private HashMap assignment;
    private SpaceTimeSchedule spaceTime;
    
    public ManualSliceLayout(SpaceTimeSchedule spaceTime) {
        this.spaceTime = spaceTime; 
    }
    
    public RawTile getTile(FilterSliceNode node) {
        assert assignment.containsKey(node);
        return (RawTile)assignment.get(node);
    }
    
    public void setTile(FilterSliceNode node, RawTile tile) {
        assignment.put(node, tile);
    }
    
    public void run() {
        assignment = new HashMap();
        //call layout on traces!
        Slice[] traces = spaceTime.partitioner.getSliceGraph();
        
        for (int i = 0; i < traces.length; i++) {
            if (!spaceTime.partitioner.isIO(traces[i]))
                layout(spaceTime.getRawChip(), traces[i]);
        }
        
        //now set the tiles of the i/o
        for (int i = 0; i < spaceTime.partitioner.io.length; i++) {
            Slice slice = spaceTime.partitioner.io[i];
            if (slice.getHead().getNextFilter().isFileOutput()) {
                //file writer
                assert slice.getHead().oneInput();
                //set this file writer tile to the tile of its upstream input
                assignment.put(slice.getHead().getNextFilter(),
                        getTile(slice.getHead().getSingleEdge().getSrc().getPrevFilter()));
            }
            else if (slice.getTail().getPrevFilter().isFileInput()) {
                //file reader
                assert slice.getTail().oneOutput();
                //set this file reader to the tile of its downstream reader
                assignment.put(slice.getTail().getPrevFilter(),
                        getTile(slice.getTail().getSingleEdge().getDest().getNextFilter()));
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
     * @param slice The Slice we would want to layout out on <pre>rawChip</pre>
     */
    private void layout(RawChip rawChip, Slice slice) {
        BufferedReader inputBuffer = 
            new BufferedReader(new InputStreamReader(
                    System.in));
        // the current node we are getting the tile assignment for
        SliceNode node = slice.getHead().getNext();
        // the tile number we are assigning
        int tileNumber;
        String str = "";
        RawTile tile;
        
        System.out.println("Enter layout for trace: " + slice);
        
        while (node instanceof FilterSliceNode) {
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
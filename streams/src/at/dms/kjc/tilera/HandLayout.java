package at.dms.kjc.tilera;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.Layout;
import at.dms.kjc.backendSupport.SpaceTimeScheduleAndSlicer;
import at.dms.kjc.flatgraph.FlatNode;
import at.dms.kjc.slicegraph.*;
import at.dms.kjc.spacedynamic.RawTile;
import at.dms.kjc.spacedynamic.SpdStaticStreamGraph;


public class HandLayout implements Layout {
    
    protected Slicer slicer;
    protected TileraChip chip;
    protected LinkedList<Slice> scheduleOrder;
    protected HashMap<SliceNode, Tile> assignment;    
        
    public HandLayout(SpaceTimeScheduleAndSlicer spaceTime, TileraChip chip) {
        this.chip = chip;
        this.slicer = spaceTime.getSlicer();
        scheduleOrder = 
            DataFlowOrder.getTraversal(spaceTime.getSlicer().getSliceGraph());
        assignment = new HashMap<SliceNode, Tile>();
    }

    public ComputeNode getComputeNode(SliceNode node) {
        return assignment.get(node);
    }

    /**
     * Given a Buffered reader, get the tile number assignment from the reader
     * for <pre>slice</pre>
     */
    private void assignFromReader(BufferedReader inputBuffer,
                                  Slice slice) {
        // Assign a filter, joiner to a tile
        // perform some error checking.
        while (true) {
            int tileNumber;
            String str = null;

            System.out.print(slice.getFirstFilter().toString() + ": ");
            try {
                str = inputBuffer.readLine();
                tileNumber = Integer.valueOf(str).intValue();
            } catch (Exception e) {
                System.out.println("Bad number " + str);
                continue;
            }
            if (tileNumber < 0 || tileNumber >= chip.size()) {
                System.out.println("Bad tile number!");
                continue;
            }
            Tile tile = chip.getNthComputeNode(tileNumber);
            if (assignment.values().contains(tile)) {
                System.out.println("Tile Already Assigned!");
                continue;
            }
            // other wise the assignment is valid, assign and break!!
            System.out.println("Assigning " + slice.getFirstFilter().toString() + " to tile "
                               + tileNumber);
            setComputeNode(slice.getFirstFilter(), tile);
            break;
        }
    }
    
    public void runLayout() {
        Iterator<Slice> slices = scheduleOrder.iterator();
                
        System.out.println("Enter desired tile for each filter: ");
        BufferedReader inputBuffer = 
                new BufferedReader(new InputStreamReader(System.in));
        
        while (slices.hasNext()) {
          Slice slice = slices.next();

          assert slice.getNumFilters() == 1 : "HandLayout only works for Slices with one filter! "  + 
               slice;

          assignFromReader(inputBuffer, slice);
        }
    }

    public void setComputeNode(SliceNode node, ComputeNode tile) {
        assignment.put(node, (Tile)tile);
    }

}

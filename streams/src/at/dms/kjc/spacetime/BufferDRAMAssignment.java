package at.dms.kjc.spacetime;

import java.util.ListIterator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Random;
import at.dms.kjc.flatgraph2.*;

/**
 * This class will assign the offchip-rotating buffers to DRAM banks of the
 * raw chip.  It uses a heuristic approach that is executed in stages.  See the
 * code for details.
 * 
 * @author mgordon
 *
 */
public class BufferDRAMAssignment {
    /** if this is true, then just assign intra-slice buffers 
     * to the home base of the endpoint tiles.
     */
    private final boolean ALWAYS_ASSIGN_INTRA_HOME_BASE = true;
    private RawChip rawChip;

    /**
     * Assign the buffers to ports
     */
    public void run(SpaceTimeSchedule spaceTime) {
        rawChip = spaceTime.getRawChip();
        TraceNode[] traceNodes = Util.traceNodeArray(spaceTime.partitioner.getTraceGraph());
        
        // take care of the file readers and writes
        // assign the reader->output buffer and the input->writer buffer
        fileStuff(spaceTime.partitioner.io);

        // assign the buffer between inputtracenode and the filter
        // to the filter's home device
        for (int i = 0; i < traceNodes.length; i++) {
            if (traceNodes[i].isInputTrace())
                inputFilterAssignment((InputTraceNode) traceNodes[i]);
        }
            
        if (!ALWAYS_ASSIGN_INTRA_HOME_BASE) {
            //Assign filter->output and output->input buffers where there is
            //a single output to an input trace
            for (int i = 0; i < traceNodes.length; i++) {
                if (traceNodes[i].isOutputTrace() && 
                        (traceNodes[i].getAsOutput().oneOutput() ||
                                traceNodes[i].getAsOutput().noOutputs()))
                    singleOutputAssignment(traceNodes[i].getAsOutput());
            }
            
            //assign filter->output intratracebuffers for split output traces
            for (int i = 0; i < traceNodes.length; i++) {
                if (traceNodes[i].isOutputTrace() &&
                        !traceNodes[i].getAsOutput().oneOutput())
                    splitOutputAssignment(traceNodes[i].getAsOutput());
            }
        }
        else {
            for (int i = 0; i < traceNodes.length; i++) {
                //we want to always use the home base for filter output
                //so just call splitoutputassignment, it will do the 
                //right thing
                if (traceNodes[i].isOutputTrace())
                    splitOutputAssignment(traceNodes[i].getAsOutput());
            }
        }
      
        //assign intertracebuffer that end at a trace with one input.
        for (int i = 0; i < traceNodes.length; i++) {
            if (traceNodes[i].isInputTrace() && 
                    traceNodes[i].getAsInput().oneInput())
                singleInputAssignment(traceNodes[i].getAsInput());
        }
        
        //now go through the remaining inter trace buffer's and 
        //assign them according to distance from their source, dest
        for (int i = 0; i < traceNodes.length; i++) {
            if (traceNodes[i].isOutputTrace())
                assignRemaining(traceNodes[i].getAsOutput());
        }
        
        //this is overly strong right now, but lets see if it gets tripped!
        //we see if some dependency chain exists between the stores...
        assert !gdnStoreSamePortDifferentTile(spaceTime.getSchedule()) :
            "We cannot have two different tiles attempt to store to the same dram using the gdn (race condition)";
        
        //make sure that everything is assigned!!!
        assert OffChipBuffer.areAllAssigned() :
            "Some buffers remain unassigned after BufferDRAMAssignment.";
    }

    /**
     * @param traceNode
     * @return True if two or more different tiles issue a store command to the same 
     * dram using the gdn, this is bad, it could lead to a race condition.  But right
     * now we are being overly conservative, there could exist other dependencies to 
     * prevent the race.
     */
    private boolean gdnStoreSamePortDifferentTile(Trace[] traces) {
        //this hashset stores a mapping from drams to a tile that
        //has already issued a store on the gdn to the dram
        HashMap dramToTile = new HashMap();
        for (int i = 0; i < traces.length; i++) {
            OutputTraceNode output = traces[i].getTail();
            IntraTraceBuffer buffer = IntraTraceBuffer.getBuffer(output.getPrevFilter(),
                    output);
            if (!buffer.isStaticNet()) {
                if (dramToTile.containsKey(buffer.getDRAM())) {
                    //we have already seen this dram

                    //if this tile is different from the tile we have already 
                    //issued a gdn store command from, then we might have a race condition.
                    if (dramToTile.get(buffer.getDRAM()) != getFilterTile(output.getPrevFilter())) {
                        System.out.println(dramToTile.get(buffer.getDRAM()) + " and " +
                                getFilterTile(output.getPrevFilter()));
                        return true;
                    }
                }
                else //otherwise put the tile in the hashmap to remember that we issued a store from it 
                    dramToTile.put(buffer.getDRAM(), getFilterTile(output.getPrevFilter()));
            }
            
        }
        return false;
    }
   
    private StreamingDram getHomeDevice(RawTile tile) {
        return LogicalDramTileMapping.getHomeDram(tile);
    }
    
   
    private RawTile getFilterTile(FilterTraceNode node) {
        return rawChip.getTile(node.getX(), node.getY());
    }
            
    /**
     * first go thru the file, reader and writers and assign their 
     * input->file and file->output buffers to reside in the dram 
     * attached to the output port.
     * 
     * @param files
     * @param chip
     */
    private  void fileStuff(Trace[] files) {
        // 
        for (int i = 0; i < files.length; i++) {
            // these traces should have only one filter, make sure
            assert files[i].getHead().getNext().getNext() == files[i].getTail() : 
                "File Trace incorrectly generated";
            FilterTraceNode filter = (FilterTraceNode) files[i].getHead()
                .getNext();

            if (files[i].getHead().isFileOutput()) {
                assert files[i].getHead().oneInput() : 
                    "buffer assignment of a joined file writer not implemented ";
                //get the tile assigned to the file writer by the layout stage?
                
                RawTile tile = getFilterTile(files[i].getHead().getNextFilter());
                
                //check that the tile assignment for this file writer is correct
                assert tile == 
                    getFilterTile(files[i].getHead().getSingleEdge().getSrc().getPrevFilter());
                
                IntraTraceBuffer buf = IntraTraceBuffer.getBuffer(files[i]
                                                                  .getHead(), filter);
                // the dram of the tile where we want to add the file writer
                StreamingDram dram = getHomeDevice(tile);
                // set the port for the buffer
                buf.setDRAM(dram);
                // use the static net if we can
                buf.setStaticNet(!LogicalDramTileMapping.mustUseGdn(tile));
                // assign the other buffer to the same port
                // this should not affect anything
                IntraTraceBuffer.getBuffer(filter, files[i].getTail()).setDRAM(dram);
                System.out.println("Assigning " + filter.getFilter() + " to " + 
                        dram + " written by " + 
                        files[i].getHead().getSingleEdge().getSrc().getPrevFilter());
                
                // attach the file writer to the port
                dram.setFileWriter((FileOutputContent)filter.getFilter());
            } else if (files[i].getTail().isFileInput()) {
                assert files[i].getTail().oneOutput() : "buffer assignment of a split file reader not implemented ";
                FileInputContent fileIC = (FileInputContent) filter.getFilter();
                //get the tile assigned to the next
                RawTile tile = getFilterTile(files[i].getHead().getNextFilter());
                //make sure the tile assignment for this writer is the same as the upstream
                //filter that creates the data
                assert tile == 
                    getFilterTile(files[i].getTail().getSingleEdge().getDest().getNextFilter());
                
                IntraTraceBuffer buf = IntraTraceBuffer.getBuffer(filter,
                                                                  files[i].getTail());
                StreamingDram dram = getHomeDevice(tile);

                buf.setDRAM(dram);
                buf.setStaticNet(!LogicalDramTileMapping.mustUseGdn(tile));
                IntraTraceBuffer.getBuffer(files[i].getHead(), filter).setDRAM(
                                                                               dram);
                dram.setFileReader(fileIC);
            } else
                assert false : "File trace is neither reader or writer";
        }
    }
    
    /**
     * Assign the filter->output intratracebuffer of a split trace
     * to the upstream filter's homebase.
     * 
     * @param output
     */
    private void splitOutputAssignment(OutputTraceNode output) {
        if ((output.oneOutput() || output.noOutputs()) && 
                !ALWAYS_ASSIGN_INTRA_HOME_BASE) 
            return;
        
        //if we are splitting this output then assign the intratracebuffer
        //to the home base of the dest filter
        RawTile tile = getFilterTile(output.getPrevFilter());
        IntraTraceBuffer buf = IntraTraceBuffer.getBuffer(output.getPrevFilter(), output);
        buf.setDRAM(getHomeDevice(tile));
        buf.setStaticNet(tile == getHomeDevice(tile).getNeighboringTile());
    }

    /**
     * Assign output-filter buffer of an output trace node that has a single
     * output (not split). Also, assign the intertracebuffer to the single output
     * to make it redundant if possible.  
     * 
     * @param output
     */
    private void singleOutputAssignment(OutputTraceNode output) {

        //get the upstream tile
        RawTile upTile = getFilterTile(output.getPrevFilter());
        //the downstream trace is a single input trace
        IntraTraceBuffer buf = IntraTraceBuffer.getBuffer(output.getPrevFilter(), 
                output);
        
        //if we have no outputs, then just assign to the home device
        if (output.noOutputs()) {
            buf.setDRAM(getHomeDevice(upTile));
            return;
        }
        
        assert output.oneOutput();
        
        //if the dest has one input (not joined) then set the output to write to
        //the dest's home device...
        if (output.getSingleEdge().getDest().oneInput()) {
          
            //get the tile that the downstream filter is assigned to
            RawTile dsTile = getFilterTile(output.getSingleEdge().getDest().getNextFilter());   
            buf.setDRAM(getHomeDevice(dsTile));
            
            //should we use the dynamic network
            //if we are a border tile then yes, or if we are not the same tile as
            //the downstream tile
            buf.setStaticNet(getHomeDevice(dsTile).getNeighboringTile() == 
                upTile);
            
            //now set the intertracebuffer between the two
            InterTraceBuffer interBuf = InterTraceBuffer.getBuffer(output.getSingleEdge());
            interBuf.setDRAM(getHomeDevice(dsTile));
        }
        else {
            //joined downstream trace
            InputTraceNode input = output.getSingleEdge().getDest();
            StreamingDram assignment = null;
            //we would like to assign this output to the home base of 
            //the upstream tile, but it might have been assigned already
            //to another input of the join, check to see if it is
            StreamingDram wanted = getHomeDevice(upTile);
            
            if (!assignedInputDRAMs(input).contains(wanted)) 
                assignment = wanted;
            else {
                // we cannot use our home dram, so we have to use another one...
                assert false;
                return;
            }
            //set the assignment and the network to use
            buf.setDRAM(assignment);
            buf.setStaticNet(assignment.getNeighboringTile() == upTile);
        }
    }
         
    /**
     * Try to assign InterTraceBuffers that originate from a trace with multiple
     * outputs and end at a trace a single input.  Try to assign the 
     * inter trace buffer to the downstream trace's input->filter buffer dram.
     * 
     * @param input
     */
    private void singleInputAssignment(InputTraceNode input) {
        assert input.oneInput();
        
        OutputTraceNode output = input.getSingleEdge().getSrc();
        
        //get the single input we are interested in
        InterTraceBuffer buffer = 
            InterTraceBuffer.getBuffer(input.getSingleEdge());
        
        //we have already assigned this dram
        if (buffer.isAssigned())
            return;
        //this is the dram we would like, the home dram from the first filter
        //of the downstream trace
        StreamingDram wanted = getHomeDevice(getFilterTile(input.getNextFilter()));
        //if it is not assigned yet to an intertracebuffer of the output,
        //then assign it, otherwise, do nothing...
        if (!assignedOutputDRAMs(output).contains(wanted)) {
            buffer.setDRAM(wanted);
        }
    }
    
    /**
     * Now, take the remaining InterTraceBuffers that were not assigned in 
     * previous passes and assign them.  To do this we look at all the edges for 
     * the OutputtraceNode and if any are unassigned, we build a list of drams
     * in ascending distance from the src port of the filter->outputtrace and the
     * dest port of the inputtrace->filter and try to assign it to the buffer one at a time.
     * We will not be able to assign a buffer to a port if the port has already been used
     * for the outputtracenode or the inputtracenode.
     *
     * 
     * @param traceNode
     */
    private void assignRemaining(OutputTraceNode traceNode) {
        //get all the edges of this output trace node
        Iterator edges = traceNode.getDestSet().iterator();
        while (edges.hasNext()) {
            Edge edge = (Edge)edges.next();
            InputTraceNode input = edge.getDest();
            //get the buffer that represents this edge
            InterTraceBuffer buffer = InterTraceBuffer.getBuffer(edge);
            //if it is already assigned, do nothing...
            if (buffer.isAssigned())
                continue;
            
            //get the order of ports in ascending order of distance from
            //the src port + the dest port
            Iterator order = assignmentOrder(edge);
            StreamingDram dramToAssign = null;
            while (order.hasNext()) {
                StreamingDram current = ((PortDistance) order.next()).dram;
                if (assignedInputDRAMs(input).contains(current) || 
                        assignedOutputDRAMs(traceNode).contains(current)) 
                    continue;
                dramToAssign = current;
                break;
            }
            assert dramToAssign != null : "Could not find a dram to assign to " +
                  buffer + " during dram assignment.";
            buffer.setDRAM(dramToAssign);
        }
    }
    

    /**
     * Assign the intra trace buffer between an inputtracenode and a filter
     * based on where the filter is placed.
     * 
     * @param input
     * @param chip
     */
    private void inputFilterAssignment(InputTraceNode input) {
        FilterTraceNode filter = input.getNextFilter();

        RawTile tile = getFilterTile(filter);
        // the neighboring dram of the tile we are assigning this buffer to
        StreamingDram dram = getHomeDevice(tile);
        // assign the buffer to the dram
        SpaceTimeBackend.println("Assigning (" + input + "->" + input.getNext()
                                 + " to " + dram + ")");
        IntraTraceBuffer.getBuffer(input, filter).setDRAM(dram);
        IntraTraceBuffer.getBuffer(input, filter).
           setStaticNet(!LogicalDramTileMapping.mustUseGdn(tile));
    }

    /**
     * @param output
     * @return A hashet of StreamingDrams that are already assigned to the
     * outgoing edges of <output> at the current time.  
     */
    private Set assignedOutputDRAMs(OutputTraceNode output) {
        HashSet set = new HashSet();
        Iterator dests = output.getDestSet().iterator();
        while (dests.hasNext()) {
            Edge edge = (Edge)dests.next();
            if (InterTraceBuffer.getBuffer(edge).isAssigned())
                set.add(InterTraceBuffer.getBuffer(edge).getDRAM());
        }
        return set;
    }
    
    /**
     * @param input
     * @return A hashset of StreamingDrams that are already assigned to the incoming
     * buffers of <input> at the current time.
     */
    private Set assignedInputDRAMs(InputTraceNode input) {
        HashSet set = new HashSet();
        for (int i = 0; i < input.getSources().length; i++) {
            if (InterTraceBuffer.getBuffer(input.getSources()[i]).isAssigned())
                set.add(InterTraceBuffer.getBuffer(input.getSources()[i])
                        .getDRAM());
        }
        return set;
    }

    /**
     * given an output trace node and an assignment of inputtracenodes to
     * streaming drams return the tiles that are needed to route this assignment
     * on the chip
     */
    public Set tilesOccupiedSplit(OutputTraceNode output,
                                         HashMap assignment) {
        HashSet tiles = new HashSet();
        Iterator edges = assignment.keySet().iterator();
        StreamingDram src = IntraTraceBuffer.getBuffer(output.getPrevFilter(),
                                                       output).getDRAM();

        while (edges.hasNext()) {
            // add the tiles for splitting
            Util.addAll(tiles, Router.getRoute(src, (StreamingDram) assignment
                                               .get(edges.next())));
        }
        return tiles;
    }

    public Set tilesOccupiedJoin(InputTraceNode input) {
        HashSet tiles = new HashSet();
        StreamingDram dest = IntraTraceBuffer.getBuffer(input,
                                                        input.getNextFilter()).getDRAM();
        for (int i = 0; i < input.getSources().length; i++) {
            Util.addAll(tiles, Router.getRoute(InterTraceBuffer.getBuffer(
                                                                          input.getSources()[i]).getDRAM(), dest));
        }
        return tiles;
    }

    /**
     * @param edge
     * @param chip
     * @return An iterator over a list of PortDistance ordered in ascending 
     * order of the distance from both the dram assigned to the source of 
     * <edge> and the dram assigned to the dest of <edge>. 
     */
    private Iterator assignmentOrder(Edge edge) {
        // the streaming DRAM implementation can do both a
        // read and a write on the same cycle, so it does not
        // matter if the port is assigned to reading the outputtracenode
        // or writing to the inputtracenode
        // so just assign to ports based on the distance from the output
        // tracenode's port and to the input of the inputracenode
        TreeSet sorted = new TreeSet();
        StreamingDram src = 
            IntraTraceBuffer.getBuffer(edge.getSrc().getPrevFilter(), edge.getSrc()).getDRAM();

        StreamingDram dst = IntraTraceBuffer.getBuffer(edge.getDest(),
                                                       edge.getDest().getNextFilter()).getDRAM();
        
        for (int i = 0; i < rawChip.getDevices().length; i++) {
            //add to the sorted tree set, the current dram device and sort on its
            //distance from the both the source and destination,
            sorted.add(new PortDistance(edge, (StreamingDram)rawChip.getDevices()[i], src, dst));
        }

        return sorted.iterator();
    }
}

/**
 * This class encapsulates the port plus the distance of this port to the
 * source and dest of the edge in question (see assignmentOrder).  It 
 * implements comparable so that the TreeSet can sort it based on the
 * distance field.
 * 
 * @author mgordon
 *
 */
class PortDistance implements Comparable {
    public StreamingDram dram;
    private Edge edge;
    
    private StreamingDram src;
    private StreamingDram dst;
    
    private int distance;
    
    // put this crap in so it sorts correctly with duplicate distances...
    private static int index;

    public int id;
    
    public PortDistance(Edge edge, StreamingDram dram, 
            StreamingDram src, StreamingDram dst) {
        this.dram = dram;
        this.edge = edge;
        this.src = src;
        this.dst = dst;
        distance = computeDistance();
        id = index++;
    }

    /**
     * @return a distance metric for this dram from its src and dest.
     */
    private int computeDistance() {
        //if the src of this edge has only one output,
        //then weight the port already assigned to its output very low (good)
        if (edge.getSrc().oneOutput() && 
                src == dram)
            return 0;
        //if the des has only one input, then weight the port already assigned 
        //to its input very low (good)...
        if (edge.getDest().oneInput() && 
                dst == dram)
            return 0;
        
        //now compute the manhattan distance from the source and from the
        //dest
        int srcDist = Math.abs(src.getNeighboringTile().getX() - 
                dram.getNeighboringTile().getX()) + 
                Math.abs(src.getNeighboringTile().getY() - 
                        dram.getNeighboringTile().getY());
        int dstDist = Math.abs(dst.getNeighboringTile().getX() -
                dram.getNeighboringTile().getX()) +
                Math.abs(dst.getNeighboringTile().getY() - 
                        dram.getNeighboringTile().getY());
        //add one because we for the final hop of the route.
        return srcDist + dstDist + 1;
    }
    
    public boolean equals(PortDistance pd) {
        return (this.distance == pd.distance && dram == pd.dram);
    }

    public int compareTo(Object pd) {
        assert pd instanceof PortDistance;
        PortDistance portd = (PortDistance) pd;
        if (portd.distance == this.distance) {
            if (dram == portd.dram)
                return 0;
            if (id < portd.id)
                return -1;
            return 1;
        }
        if (this.distance < portd.distance)
            return -1;
        else
            return 1;
    }

}

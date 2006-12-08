package at.dms.kjc.spacetime;

import java.util.ListIterator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Vector;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Random;

import at.dms.kjc.common.CommonUtils;
import at.dms.kjc.slicegraph.*;

/**
 * This class will assign the offchip-rotating buffers to DRAM banks of the
 * raw chip.
 * <p>
 * The assignment is stored in the {@link at.dms.kjc.spacetime.OffChipBuffer} 
 * class for the {@link at.dms.kjc.spacetime.InterSliceBuffer} and 
 * {@link at.dms.kjc.spacetime.IntraSliceBuffer}.
 *
 *@author mgordon
 *
 */
public class BufferDRAMAssignment {
    /** if this is true, then just assign intra-slice buffers 
     * to the home base of the endpoint tiles.
     */
    private final boolean ALWAYS_ASSIGN_INTRA_HOME_BASE = true;
    /** The raw chip we are compiling to */
    private RawChip rawChip;
    /** the layout we are using */
    private Layout layout;
    private SpaceTimeSchedule spaceTime;
    
    
    /**
     * Assign the buffers to DRAM ports.
     * 
     * @param spaceTime The space time schedule.
     * @param layout The layout object that assigns FilterSliceNode to Tiles.
     */
    public void run(SpaceTimeSchedule spaceTime, Layout layout) {
        OffChipBuffer.resetDRAMAssignment();
        this.spaceTime = spaceTime;
        this.layout = layout;
        
        rawChip = spaceTime.getRawChip();
        SliceNode[] sliceNodes = Util.sliceNodeArray(spaceTime.partitioner.getSliceGraph());
        
        // take care of the file readers and writes
        // assign the reader->output buffer and the input->writer buffer
        fileStuff(spaceTime.partitioner.io);

        // assign the buffer between inputslicenode and the filter
        // to the filter's home device
        for (int i = 0; i < sliceNodes.length; i++) {
            if (sliceNodes[i].isInputSlice())
                inputFilterAssignment((InputSliceNode) sliceNodes[i]);
        }
            
        if (!ALWAYS_ASSIGN_INTRA_HOME_BASE) {
            //Assign filter->output and output->input buffers where there is
            //a single output to an input slice
            for (int i = 0; i < sliceNodes.length; i++) {
                if (sliceNodes[i].isOutputSlice() && 
                        (sliceNodes[i].getAsOutput().oneOutput() ||
                                sliceNodes[i].getAsOutput().noOutputs()))
                    singleOutputAssignment(sliceNodes[i].getAsOutput());
            }
            
            //assign filter->output intraslicebuffers for split output slices
            for (int i = 0; i < sliceNodes.length; i++) {
                if (sliceNodes[i].isOutputSlice() &&
                        !sliceNodes[i].getAsOutput().oneOutput())
                    splitOutputAssignment(sliceNodes[i].getAsOutput());
            }
        }
        else {
            for (int i = 0; i < sliceNodes.length; i++) {
                //we want to always use the home base for filter output
                //so just call splitoutputassignment, it will do the 
                //right thing
                if (sliceNodes[i].isOutputSlice())
                    splitOutputAssignment(sliceNodes[i].getAsOutput());
            }
        }
      
        //assign interslicebuffer that end at a slice with one input.
        for (int i = 0; i < sliceNodes.length; i++) {
            if (sliceNodes[i].isInputSlice() && 
                    sliceNodes[i].getAsInput().oneInput())
                singleInputAssignment(sliceNodes[i].getAsInput());
        }
        
        //now go through the remaining inter slice buffer's and 
        //assign them according to distance from their source, dest
        boolean forgetOpt = false;
        for (int i = 0; i < sliceNodes.length; i++) {
            if (sliceNodes[i].isOutputSlice())
                if (!assignRemaining(sliceNodes[i].getAsOutput())) {
                    forgetOpt = true;
                    break;
                }
        }
        
        if (forgetOpt) {
            System.out.println("Running force placement...");
            forceAssignment(sliceNodes);
        }
        
        //this is overly strong right now, but lets see if it gets tripped!
        //we see if some dependency chain exists between the stores...
        assert !gdnStoreSamePortDifferentTile(spaceTime.partitioner.getSliceGraph()) :
            "We cannot have two different tiles attempt to store to the same dram using the gdn (race condition)";
        
        //make sure that everything is assigned!!!
        assert OffChipBuffer.areAllAssigned() :
            "Some buffers remain unassigned after BufferDRAMAssignment.";
    }

    private void forceAssignment(SliceNode[] sliceNodes) {
        //sort the input and output slice nodes of the graph according 
        //to their width
        LinkedList<SliceNode> sortedSliceNodes = new LinkedList<SliceNode>();
        for (int i = 0; i < sliceNodes.length; i++) {
            int width = 0;
            if (sliceNodes[i].isOutputSlice() || sliceNodes[i].isInputSlice())
                width = getWidth(sliceNodes[i]);
            else //do nothing for filters
                continue;
            
            //unset all the assignments for the buffers!
            unsetDramAssignment(sliceNodes[i]);
            
            if (sortedSliceNodes.size() == 0 || 
                    width >= getWidth(sortedSliceNodes.get(0)))
                sortedSliceNodes.addFirst(sliceNodes[i]);
            else {
                for (int j = 0; j < sortedSliceNodes.size(); j++) {
                    if (width >= getWidth(sortedSliceNodes.get(j))) {
                        sortedSliceNodes.add(j, sliceNodes[i]); 
                        break;
                    }
                }
            }
        }
        Iterator<SliceNode> tns= sortedSliceNodes.iterator();
        while (tns.hasNext()) {
            SliceNode tn = tns.next();
            List<Edge> edges;
            if (tn.isOutputSlice())
                edges = tn.getAsOutput().getSortedOutputs();
            else 
                edges = tn.getAsInput().getSourceSequence();
            //System.out.println("Assigning edges for " + tn + " (" + edges.size() + ")");
            if (!assignRemaining(edges,0))
                assert false;
        }
    }
    
    private void unsetDramAssignment(SliceNode tn) {
        List<Edge> edges;
        if (tn.isOutputSlice())
            edges = tn.getAsOutput().getSortedOutputs();
        else 
            edges = tn.getAsInput().getSourceSequence();
        for (int i = 0; i < edges.size(); i++) {
            InterSliceBuffer.getBuffer(edges.get(i)).unsetDRAM();
        }
    }
    
    private int getWidth(SliceNode node) {
        assert node.isOutputSlice() || node.isInputSlice();
        int width;
        if (node.isOutputSlice()) 
            width = node.getAsOutput().getWidth();
        else //(node.isInputSlice())
            width = node.getAsInput().getWidth();
        
        return width;
    }
    
    /**
     * Return True if two or more different tiles issue a store command to the same 
     * dram using the gdn, this is bad, it could lead to a race condition.  But right
     * now we are being overly conservative, there could exist other dependencies to 
     * prevent the race.
     * 
     * @param slices The slices of the application.
     * 
     * @return True if two or more different tiles issue a store command to the same 
     * dram using the gdn, this is bad, it could lead to a race condition.  But right
     * now we are being overly conservative, there could exist other dependencies to 
     * prevent the race.
     */
    private boolean gdnStoreSamePortDifferentTile(Slice[] slices) {
        //this hashset stores a mapping from drams to a tile that
        //has already issued a store on the gdn to the dram
        HashMap<StreamingDram, RawTile> dramToTile = new HashMap<StreamingDram, RawTile>();
        for (int i = 0; i < slices.length; i++) {
            OutputSliceNode output = slices[i].getTail();
            IntraSliceBuffer buffer = IntraSliceBuffer.getBuffer(output.getPrevFilter(),
                    output);
            if (!buffer.isStaticNet()) {
                if (dramToTile.containsKey(buffer.getDRAM())) {
                    //we have already seen this dram

                    //if this tile is different from the tile we have already 
                    //issued a gdn store command from, then we might have a race condition.
                    if (dramToTile.get(buffer.getDRAM()) != layout.getTile(output.getPrevFilter())) {
                        System.out.println(dramToTile.get(buffer.getDRAM()) + " and " +
                                layout.getTile(output.getPrevFilter()));
                        return true;
                    }
                }
                else //otherwise put the tile in the hashmap to remember that we issued a store from it 
                    dramToTile.put(buffer.getDRAM(), layout.getTile(output.getPrevFilter()));
            }
            
        }
        return false;
    }
   
    /**
     * Return the home DRAM that is controlled by tile.
     * 
     * @param tile The raw tile.
     * @return the home DRAM that is controlled by tile.
     */
    private StreamingDram getHomeDevice(RawTile tile) {
        return LogicalDramTileMapping.getHomeDram(tile);
    }
    
    /**
     * First go thru the file, reader and writers and assign their 
     * input-&gt;file and file-&gt;output buffers to reside in the dram 
     * attached to the output port.
     * 
     * @param files The slices that read or write files.
     */
    private  void fileStuff(Slice[] files) {
        // go through the drams and reset the file readers and writers 
        //associated with them...
        for (int i = 0; i < rawChip.getDevices().length; i++) {
            StreamingDram dram = (StreamingDram)rawChip.getDevices()[i];
            dram.resetFileAssignments();
        }
        
        for (int i = 0; i < files.length; i++) {
            // these slices should have only one filter, make sure
            assert files[i].getHead().getNext().getNext() == files[i].getTail() : 
                "File Slice incorrectly generated";
            FilterSliceNode filter = (FilterSliceNode) files[i].getHead()
                .getNext();

            if (files[i].getHead().isFileOutput()) {
               
                //    "buffer assignment of a joined file writer not implemented ";
                //get the tile assigned to the file writer by the layout stage?
                RawTile tile;
                if (files[i].getHead().oneInput()) {
//                 set the filter tile to be the tile of the upstream tile
                    tile = layout.getTile(files[i].getHead().getSingleEdge().getSrc().getPrevFilter());
                    layout.setTile(files[i].getHead().getNextFilter(), tile);
                } else {
                    tile = layout.getTile(filter); 
                }
                    
                
                IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(files[i]
                                                                  .getHead(), filter);
                // the dram of the tile where we want to add the file writer
                StreamingDram dram = getHomeDevice(tile);
                // set the port for the buffer
                buf.setDRAM(dram);
                // use the static net if we can
                buf.setStaticNet(!LogicalDramTileMapping.mustUseGdn(tile));
                // assign the other buffer to the same port
                // this should not affect anything
                IntraSliceBuffer.getBuffer(filter, files[i].getTail()).setDRAM(dram);
                /*System.out.println("Assigning " + filter.getFilter() + " to " + 
                        dram + " written by " + 
                        files[i].getHead().getSingleEdge().getSrc().getPrevFilter());
                */
                // attach the file writer to the port
                /*System.out.println("FileWriter Upstream " + 
                        files[i].getHead().getSingleEdge().getSrc().getPrevFilter() + " " + tile
                        + " " + dram);*/
                dram.setFileWriter((FileOutputContent)filter.getFilter());
            } else if (files[i].getTail().isFileInput()) {
                
                FileInputContent fileIC = (FileInputContent) filter.getFilter();
                //get the tile assigned to the next
                RawTile tile;
                //if there is only one output, then force its assignment to the 
                //filter that the tile is assigned to so that there is no copying
                if (files[i].getTail().oneOutput()) {
                    tile = layout.getTile(files[i].getTail().getSingleEdge().getDest().getNextFilter());
                    layout.setTile(files[i].getHead().getNextFilter(), tile);                
                } else {
                    tile = layout.getTile(filter);
                }
                

                
                IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(filter,
                                                                  files[i].getTail());
                StreamingDram dram = getHomeDevice(tile);

                buf.setDRAM(dram);
                buf.setStaticNet(!LogicalDramTileMapping.mustUseGdn(tile));
                IntraSliceBuffer.getBuffer(files[i].getHead(), filter).setDRAM(
                                                                               dram);
                dram.setFileReader(fileIC);
            } else
                assert false : "File slice is neither reader or writer";
        }
    }
    
    /**
     * Assign the filter-&gt;output intraslicebuffer of a split slice
     * to the upstream filter's homebase.
     * 
     * @param output The output slice node.
     */
    private void splitOutputAssignment(OutputSliceNode output) {
        if ((output.oneOutput() || output.noOutputs()) && 
                !ALWAYS_ASSIGN_INTRA_HOME_BASE) 
            return;
        
        //if we are splitting this output then assign the intraslicebuffer
        //to the home base of the dest filter
        RawTile tile = layout.getTile(output.getPrevFilter());
        IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(output.getPrevFilter(), output);
        buf.setDRAM(getHomeDevice(tile));
        buf.setStaticNet(tile == getHomeDevice(tile).getNeighboringTile());
    }

    /**
     * Assign output-filter buffer of an output slice node that has a single
     * output (not split). Also, assign the interslicebuffer to the single output
     * to make it redundant if possible.  
     * 
     * @param output The output slice node
     */
    private void singleOutputAssignment(OutputSliceNode output) {

        //get the upstream tile
        RawTile upTile = layout.getTile(output.getPrevFilter());
        //the downstream slice is a single input slice
        IntraSliceBuffer buf = IntraSliceBuffer.getBuffer(output.getPrevFilter(), 
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
            RawTile dsTile = layout.getTile(output.getSingleEdge().getDest().getNextFilter());   
            buf.setDRAM(getHomeDevice(dsTile));
            
            //should we use the dynamic network
            //if we are a border tile then yes, or if we are not the same tile as
            //the downstream tile
            buf.setStaticNet(getHomeDevice(dsTile).getNeighboringTile() == 
                upTile);
            
            //now set the interslicebuffer between the two
            InterSliceBuffer interBuf = InterSliceBuffer.getBuffer(output.getSingleEdge());
            interBuf.setDRAM(getHomeDevice(dsTile));
        }
        else {
            //joined downstream slice
            InputSliceNode input = output.getSingleEdge().getDest();
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
     * Try to assign InterSliceBuffers that originate from a slice with multiple
     * outputs and end at a slice a single input.  Try to assign the 
     * inter slice buffer to the downstream slice's input-&gt;filter buffer dram.
     * 
     * @param input The input slice node.
     */
    private void singleInputAssignment(InputSliceNode input) {
        assert input.oneInput();
        
        OutputSliceNode output = input.getSingleEdge().getSrc();
        
        //get the single input we are interested in
        InterSliceBuffer buffer = 
            InterSliceBuffer.getBuffer(input.getSingleEdge());
        
        //we have already assigned this dram
        if (buffer.isAssigned())
            return;
        //this is the dram we would like, the home dram from the first filter
        //of the downstream slice
        StreamingDram wanted = getHomeDevice(layout.getTile(input.getNextFilter()));
        //if it is not assigned yet to an interslicebuffer of the output,
        //then assign it, otherwise, do nothing...
        if (!assignedOutputDRAMs(output).contains(wanted)) {
            buffer.setDRAM(wanted);
        }
    }
    
    /**
     * 
     * @param SliceNode
     * @return
     */
    private boolean valid(OutputSliceNode sliceNode) {
       Iterator<Edge> edges = sliceNode.getDestSet().iterator();
       HashSet<Integer> freePorts = new HashSet<Integer>();
       //System.out.println(" * For " + sliceNode);
       while (edges.hasNext()) {
           Edge edge = edges.next();
           InputSliceNode input = edge.getDest();
           
           HashSet<Integer>ports = new HashSet<Integer>();
           for (int i = 0; i < rawChip.getNumDev(); i++) 
               ports.add(new Integer(i));
           System.out.println("      " + input + "is using:");
           Iterator<Edge> inEdges = input.getSourceSet().iterator();
           while (inEdges.hasNext()) {
               Edge inEdge = inEdges.next();
               InterSliceBuffer buffer = InterSliceBuffer.getBuffer(inEdge);
               
               if (buffer.isAssigned()) {
                   ports.remove(new Integer(buffer.getDRAM().port));
                   System.out.println("        " + buffer.getDRAM().port);
               }
           }
           System.out.print("    has free: ");
           Iterator<Integer> ints = ports.iterator();
           while (ints.hasNext())
               System.out.print(ints.next() + " ");
           System.out.println();
           
           if (ports.size() == 0)
               return false;
           freePorts.addAll(ports);
       }
       if (freePorts.size() < sliceNode.getWidth())
           return false;
       return true;
    }
    
    private boolean assignRemaining(OutputSliceNode sliceNode) {
        //System.out.println("Calling assignRemaining for: " + sliceNode);
        //get all the edges of this output slice node
        //sorted by their weight
        List<Edge>edges = sliceNode.getSortedOutputs();
        
        //assert valid(sliceNode);
        
        return assignRemaining(edges, 0);
    }
    
    /**
     * Now, take the remaining InterSliceBuffers that were not assigned in 
     * previous passes and assign them.  To do this we look at all the edges for 
     * the OutputsliceNode and if any are unassigned, we build a list of drams
     * in ascending distance from the src port of the filter-&gt;outputslice and the
     * dest port of the inputslice-&gt;filter and try to assign it to the buffer one at a time.
     * We will not be able to assign a buffer to a port if the port has already been used
     * for the outputslicenode or the inputslicenode.
     *
     * 
     * @param sliceNode
     */
    private boolean assignRemaining(List<Edge> edgesToAssign, int index) {
        //the end condition
        if (index >= edgesToAssign.size())
            return true;
        
        /*System.out.println("Assigning edges for " + edgesToAssign.get(0).getSrc().getPrevFilter() + 
                " " + index);*/
        
        //get the edge
        Edge edge = edgesToAssign.get(index);
        OutputSliceNode sliceNode = edge.getSrc();
        InputSliceNode input = edge.getDest();
        
        //get the buffer that represents this edge
        InterSliceBuffer buffer = InterSliceBuffer.getBuffer(edge);
        
        //if it is already assigned, do skip over this edge and move on
        //with assigning...
        
        if (buffer.isAssigned())
            return assignRemaining(edgesToAssign, index + 1);
            
        //get the order of ports in ascending order of distance from
        //the src port + the dest port
        Iterator<PortDistance> order = assignmentOrder(edge);
        while (order.hasNext()) {
            StreamingDram current = order.next().dram;
                //System.out.println("     Trying " + current + " for " + edge);
                if (assignedInputDRAMs(input).contains(current) || 
                        assignedOutputDRAMs(sliceNode).contains(current)) 
                    continue;
                
                //System.out.println(" * Assigning " + edge + " to " + current);
                
                buffer.setDRAM(current);
                if (assignRemaining(edgesToAssign, index + 1))
                    return true;
        }
        //we got here because we could not find an assignment for this 
        //edge, so unset the buffer and return false and try another 
        //assignment recursively
        buffer.unsetDRAM();
        //System.out.println(" * Unsetting " + edge);
        return false;
    }
    

    /**
     * Assign the intra slice buffer between an inputslicenode and a filter
     * based on where the filter is placed.
     * 
     * @param input
     * @param chip
     */
    private void inputFilterAssignment(InputSliceNode input) {
        FilterSliceNode filter = input.getNextFilter();
        
        RawTile tile = layout.getTile(filter);
        // the neighboring dram of the tile we are assigning this buffer to
        StreamingDram dram = getHomeDevice(tile);
        // assign the buffer to the dram
        
        CommonUtils.println_debugging("Assigning (" + input + "->" + input.getNext()
                                 + " to " + dram + ")");
        IntraSliceBuffer.getBuffer(input, filter).setDRAM(dram);
        IntraSliceBuffer.getBuffer(input, filter).
           setStaticNet(!LogicalDramTileMapping.mustUseGdn(tile));
    }

    /**
     * Return a hashet of StreamingDrams that are already assigned to the
     * outgoing edges of <pre>output</pre> at the current time.  
     * 
     * @param output
     * @return A hashet of StreamingDrams that are already assigned to the
     * outgoing edges of <pre>output</pre> at the current time.  
     */
    private Set<StreamingDram> assignedOutputDRAMs(OutputSliceNode output) {
        HashSet<StreamingDram> set = new HashSet<StreamingDram>();
        Iterator dests = output.getDestSet().iterator();
        while (dests.hasNext()) {
            Edge edge = (Edge)dests.next();
            if (InterSliceBuffer.getBuffer(edge).isAssigned()) {
                //System.out.println("     "  +
                //        InterSliceBuffer.getBuffer(edge).getDRAM() + "(this output)");
                set.add(InterSliceBuffer.getBuffer(edge).getDRAM());
            }
        }
        return set;
    }
    
    /**
     * Return a hashset of StreamingDrams that are already assigned to the incoming
     * buffers of <pre>input</pre> at the current time.
     * 
     * @param input
     * @return A hashset of StreamingDrams that are already assigned to the incoming
     * buffers of <pre>input</pre> at the current time.
     */
    private Set<StreamingDram> assignedInputDRAMs(InputSliceNode input) {
        HashSet<StreamingDram> set = new HashSet<StreamingDram>();
        for (int i = 0; i < input.getSources().length; i++) {
            if (InterSliceBuffer.getBuffer(input.getSources()[i]).isAssigned()) {
                //System.out.println("      " +         
                //        InterSliceBuffer.getBuffer(input.getSources()[i])
                //        .getDRAM() + " (downstream input)");
                set.add(InterSliceBuffer.getBuffer(input.getSources()[i])
                        .getDRAM());
            }
        }
        return set;
    }

    /**
     * Given an output slice node and an assignment of inputslicenodes to
     * streaming drams return the tiles that are needed to route this assignment
     * on the chip.
     * 
     * @param output The output slice node that splits
     * @param assignment The assignment of filterSliceNodes to Tiles. 
     * 
     * @return A set of tiles that this splitter, output, occupies.
     */
    /*
    public Set tilesOccupiedSplit(OutputSliceNode output, HashMap assignment) {
        HashSet tiles = new HashSet();
        Iterator edges = assignment.keySet().iterator();
        StreamingDram src = IntraSliceBuffer.getBuffer(output.getPrevFilter(),
                                                       output).getDRAM();

        while (edges.hasNext()) {
            // add the tiles for splitting
            Util.addAll(tiles, Router.getRoute(src, (StreamingDram) assignment
                                               .get(edges.next())));
        }
        return tiles;
    }
    */
    /**
     * Not used currently. 
     * 
     * Returns the tiles used to implement the joining of the input
     * slice node. 
     * 
     * @param input The input slice node in question.
     * 
     * @return A set of RawTiles.
     */
    /*
    public Set tilesOccupiedJoin(InputSliceNode input) {
        HashSet tiles = new HashSet();
        StreamingDram dest = IntraSliceBuffer.getBuffer(input,
                                                        input.getNextFilter()).getDRAM();
        for (int i = 0; i < input.getSources().length; i++) {
            Util.addAll(tiles, Router.getRoute(InterSliceBuffer.getBuffer(
                                                                          input.getSources()[i]).getDRAM(), dest));
        }
        return tiles;
    }
    */
    
    /**
     * Given a Edge, edge, return an order iterator of PortDistances
     * that is ordered in increase cost of communication for assigning 
     * edge to the DRAM in the PortDistance. 
     * 
     * @param edge The Edge in question.
     * @param chip The raw chip.
     * 
     * @return An iterator over a list of PortDistance ordered in ascending 
     * order of the distance from both the dram assigned to the source of 
     * <pre>edge</pre> and the dram assigned to the dest of <pre>edge</pre>. 
     */
    private Iterator<PortDistance> assignmentOrder(Edge edge) {
        // the streaming DRAM implementation can do both a
        // read and a write on the same cycle, so it does not
        // matter if the port is assigned to reading the outputslicenode
        // or writing to the inputslicenode
        // so just assign to ports based on the distance from the output
        // slicenode's port and to the input of the inpuslicenode
        TreeSet<PortDistance> sorted = new TreeSet<PortDistance>();
        StreamingDram src = 
            IntraSliceBuffer.getBuffer(edge.getSrc().getPrevFilter(), edge.getSrc()).getDRAM();

        StreamingDram dst = IntraSliceBuffer.getBuffer(edge.getDest(),
                                                       edge.getDest().getNextFilter()).getDRAM();
        
        for (int i = 0; i < rawChip.getDevices().length; i++) {
            //add to the sorted tree set, the current dram device and sort on its
            //distance from the both the source and destination,
            sorted.add(new PortDistance(edge, (StreamingDram)rawChip.getDevices()[i], src, dst));
        }

        return sorted.iterator();
    }
}

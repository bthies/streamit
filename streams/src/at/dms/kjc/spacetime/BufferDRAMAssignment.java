package at.dms.kjc.spacetime;

import java.util.ListIterator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Random;


public class BufferDRAMAssignment
{
    private static Random rand;
    
    static 
    {
	rand = new Random(17);
    }
    

    /** 
     * Assign the buffers to ports
     **/
    public static void run(LinkedList steadyList, RawChip chip) 
    {
	ListIterator steadyTrav = steadyList.listIterator();
	//first go thru the traversal and assign
	//input->filter and filter->output buffers to drams 
	//based on jasper's placement
	while(steadyTrav.hasNext()) {
	    Trace trace = (Trace)steadyTrav.next();
	    TraceNode traceNode = trace.getHead();
	    while (traceNode != null) {
		//assign the buffer between inputtracenode and the filter
		//to a dram
		if (traceNode.isInputTrace()) 
		    inputFilterAssignment((InputTraceNode)traceNode, chip);
		//assign the buffer between the output trace node and the filter
		if (traceNode.isOutputTrace())
		    filterOutputAssignment((OutputTraceNode)traceNode, chip);
		traceNode = traceNode.getNext();
	    }
	    
	}
	
	//cycle thru the steady state trav...
	//when we hit an output trace node
	//assign its output buffers to
	steadyTrav = steadyList.listIterator();
	while(steadyTrav.hasNext()) {
	    Trace trace = (Trace)steadyTrav.next();
	    TraceNode traceNode = trace.getHead();
	    while (traceNode != null) {
		//for each output trace node get assign
		//its output buffers to ports
		//based on the ordering given by assignment order
		//assign buffers in descending order of items sent to 
		//the buffer	
		if (traceNode.isOutputTrace()) {
		    //get the assignment for each input trace node
		    HashMap ass = assignment((OutputTraceNode)traceNode, chip);
		    System.out.println(traceNode + " outputs = " + 
				       ((OutputTraceNode)traceNode).getWeights().length);
		    Iterator inputTs = ass.keySet().iterator();

		    //commit the assignment
		    while (inputTs.hasNext()) {
			InputTraceNode inputT = (InputTraceNode)inputTs.next();
			System.out.println("Assigning (" + (OutputTraceNode)traceNode + "->" + 
					   inputT + ") to " + ass.get(inputT));
			
			OffChipBuffer.getBuffer((OutputTraceNode)traceNode,
						inputT).setDRAM((StreamingDram)ass.get(inputT));
		    }
		}
		traceNode = traceNode.getNext();
	    }
	}
    }
    
    private static void inputFilterAssignment(InputTraceNode input, RawChip chip) 
    {
	FilterTraceNode filter = (FilterTraceNode)input.getNext();
	
	RawTile tile = chip.getTile(filter.getX(), filter.getY());
	//the neighboring dram of the tile we are assigning this buffer to
	int index = 0;
	//if there is more than one neighboring dram, randomly pick one
	if (tile.getIODevices().length > 1) {
	    index = rand.nextInt(tile.getIODevices().length);
	}
	//assign the buffer to the dram
	OffChipBuffer.getBuffer(input, input.getNext()).setDRAM((StreamingDram)tile.getIODevices()[index]);
    }

    private static void filterOutputAssignment(OutputTraceNode output, RawChip chip) 
    {
	FilterTraceNode filter = (FilterTraceNode)output.getPrevious();
	
	RawTile tile = chip.getTile(filter.getX(), filter.getY());
	//the neighboring dram of the tile we are assigning this buffer to
	int index = 0;
	//if there is more than one neighboring dram, randomly pick one
	if (tile.getIODevices().length > 1) {
	    index = rand.nextInt(tile.getIODevices().length);
	}
	//assign the buffer to the dram
	OffChipBuffer.getBuffer(output.getPrevious(), output).setDRAM((StreamingDram)tile.getIODevices()[index]);	
    }
    

    /**
     * given an <output> tracenode, this method returns a hashmap that assigns
     * the uptream inputtracenodes to streaming drams, so one can assign
     * the IO buffers to drams based on the hashmap, make sure that
     * the buffer for the filter->outputtracenode and the buffers for the
     * inputtracenode->filter are assigned (it can always be reset) 
     * to ports before calling this...
     **/
    public static HashMap assignment(OutputTraceNode output, RawChip chip) 
    {
	HashMap assign = new HashMap();
	Iterator inputTs = output.getSortedOutputs().iterator();
	HashSet unassignedPorts = new HashSet();
	//populate the unassigned ports set
	for (int i = 0; i < chip.getDevices().length; i++) 
	    unassignedPorts.add(chip.getDevices()[i]);
	
	while (inputTs.hasNext()) {
	    System.out.println("Input");
	    
	    //make sure we have enough ports for the outputs

	    InputTraceNode inputT = (InputTraceNode)inputTs.next();
	    //now assign the buffer to the first available port that show up 
	    //in the iterator
	    Iterator portOrder = assignmentOrder(output, inputT, chip);
	    boolean assigned = false;
	    while (portOrder.hasNext()) {
		StreamingDram current = ((PortDistance)portOrder.next()).dest;
		//assign the current dram to this input trace node
		//and exit the inner loop if the port has not 
		//been used by this output trace and the corresponding input trace
		if (unassignedPorts.contains(current) && 
		    !assignedInputDRAMs(inputT).contains(current)) {
		    unassignedPorts.remove(current);
		    assign.put(inputT, current);
		    assigned = true;
		    break;
		}
	    }
	    assert assigned : "Split/join width exceeds number of ports on the chip";
	}
	return assign;
    }
    
    //return the set of drams already assigned to incoming buffers of this input
    //trace node.
    private static Set assignedInputDRAMs(InputTraceNode input) 
    {
	HashSet set = new HashSet();
	for (int i = 0; i < input.getSources().length; i++) {
	    if (OffChipBuffer.getBuffer(input.getSources()[i], input).isAssigned())
		set.add(OffChipBuffer.getBuffer(input.getSources()[i], input).getDRAM());
	}
	return set;
    }
    
    
    
    /**
     * given an output trace node and an assignment of inputtracenodes to streaming drams
     * return the tiles that are needed to route this assignment on the chip
     **/
    public static Set tilesOccupiedSplit(OutputTraceNode output, HashMap assignment) 
    {
	HashSet tiles = new HashSet();
	Iterator inputTs = assignment.keySet().iterator();
	StreamingDram src = OffChipBuffer.getBuffer(output.getPrevious(),
						    output).getDRAM();
	
	while (inputTs.hasNext()) {
	    //add the tiles for splitting
	    Util.addAll(tiles, Router.getRoute(src, 
					       (StreamingDram)assignment.get(inputTs.next())));
	}
	return tiles;
    }

    public static Set tilesOccupiedJoin(InputTraceNode input) 
    {
	HashSet tiles = new HashSet();
	StreamingDram dest = OffChipBuffer.getBuffer(input, 
						     input.getNext()).getDRAM();
	for (int i = 0; i < input.getSources().length; i++) {
	    Util.addAll(tiles, 
			Router.getRoute(OffChipBuffer.getBuffer(input.getSources()[i], input).getDRAM(),
					dest));
	}
	return tiles;
    }
    

    private static Iterator assignmentOrder(OutputTraceNode output, 
					    InputTraceNode input, RawChip chip) 
    {
	//the streaming DRAM implementation can do both a 
	//read and a write on the same cycle, so it does not 
	//matter if the port is assigned to reading the outputtracenode
	//or writing to the inputtracenode
	//so just assign to ports based on the distance from the output
	//tracenode's port and to the input of the inputracenode
	TreeSet sorted = new TreeSet();
	StreamingDram src = OffChipBuffer.getBuffer(output.getPrevious(),
						    output).getDRAM();
	StreamingDram dst = OffChipBuffer.getBuffer(input, 
						    input.getNext()).getDRAM();
	for (int i = 0; i < chip.getDevices().length; i++) {
	    sorted.add(new PortDistance((StreamingDram)chip.getDevices()[i],
					Router.distance(src, chip.getDevices()[i]) + 
					Router.distance(chip.getDevices()[i], dst)));
	}
	
	return sorted.iterator();
    }
}

class PortDistance implements Comparable 
{
    public StreamingDram dest;
    public int distance;

    public PortDistance(StreamingDram dst, int dist) 
    {
	this.dest = dst;
	this.distance = dist;
    }
    

    public boolean equals(PortDistance pd)
    {
	return (this.distance == pd.distance);
    }
    
    public int compareTo(Object pd) 
    {
	assert pd instanceof PortDistance;
	PortDistance portd = (PortDistance)pd;
	if (portd.distance == this.distance)
	    return 0;
	if (portd.distance > this.distance)
	    return -1;
	else
	    return 1;
    }
    
}

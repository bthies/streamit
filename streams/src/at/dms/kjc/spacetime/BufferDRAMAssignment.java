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
    public static void run(List steadyList, RawChip chip) 
    {
	//first go thru the traversal and assign
	//input->filter and filter->output buffers to drams 
	//based on jasper's placement
	Iterator traceNodeTrav = Util.traceNodeTraversal(steadyList);
	while(traceNodeTrav.hasNext()) {
	    TraceNode traceNode = (TraceNode)traceNodeTrav.next();
	    //assign the buffer between inputtracenode and the filter
	    //to a dram
	    if (traceNode.isInputTrace()) 
		inputFilterAssignment((InputTraceNode)traceNode, chip);
	    //assign the buffer between the output trace node and the filter
	    if (traceNode.isOutputTrace())
		    filterOutputAssignment((OutputTraceNode)traceNode, chip);
	    traceNode = traceNode.getNext();
	}

	//assign all the outputnodes with one output first
	traceNodeTrav = Util.traceNodeTraversal(steadyList);
	while(traceNodeTrav.hasNext()) {
	    TraceNode traceNode = (TraceNode)traceNodeTrav.next();
	    
	    if (traceNode.isOutputTrace() &&
		((OutputTraceNode)traceNode).oneOutput()) {
		performAssignment((OutputTraceNode)traceNode, chip);
	    }
	}
	
	//cycle thru the steady state trav...
	//when we hit an output trace node
	//assign its output buffers to
	traceNodeTrav = Util.traceNodeTraversal(steadyList);
	while(traceNodeTrav.hasNext()) {
	    TraceNode traceNode = (TraceNode)traceNodeTrav.next();
	    //for each output trace node get assign
	    //its output buffers to ports
	    //based on the ordering given by assignment order
	    //assign buffers in descending order of items sent to 
	    //the buffer	
	    
	    //do not assign one output outputTracenodes
	    //perform assign will not assign anything that has 
	    //an assignment already
	    if (traceNode.isOutputTrace()) {
		performAssignment((OutputTraceNode)traceNode, chip);
	    }
	}
	
    }
    
    //get the assignment and set the assignment in OffChipBuffer
    //perform assign will not assign anything that has 
    //an assignment already
    private static void performAssignment(OutputTraceNode traceNode, RawChip chip) 
    {
	//get the assignment for each input trace node
	HashMap ass = assignment((OutputTraceNode)traceNode, chip);
	Iterator edges = ass.keySet().iterator();
	
	SpaceTimeBackend.println("Assigning Output Buffers for: " + traceNode + " " + traceNode.getDestSet().size());

	//commit the assignment
	while (edges.hasNext()) {
	    Edge edge = (Edge)edges.next();
	    SpaceTimeBackend.println("  " + edge + " ...");
	    //if already assigned do nothing
	    if (InterTraceBuffer.getBuffer(edge).isAssigned())
		continue;
	    SpaceTimeBackend.println("  Assigning (" + edge + ") to " + ass.get(edge));
	    
	    InterTraceBuffer.getBuffer(edge).setDRAM((StreamingDram)ass.get(edge));
	}
    }
    
   
    private static void inputFilterAssignment(InputTraceNode input, RawChip chip) 
    {
	FilterTraceNode filter = input.getNextFilter();
	
	RawTile tile = chip.getTile(filter.getX(), filter.getY());
	//the neighboring dram of the tile we are assigning this buffer to
	int index = 0;
	//if there is more than one neighboring dram, randomly pick one
	if (tile.getIODevices().length > 1) {
	    index = rand.nextInt(tile.getIODevices().length);
	}
	//assign the buffer to the dram
	SpaceTimeBackend.println("Assigning (" + input + "->" + 
				 input.getNext() + " to " + tile.getIODevices()[index] + ")");
	IntraTraceBuffer.getBuffer(input, filter).
	    setDRAM((StreamingDram)tile.getIODevices()[index]);
    }

    private static void filterOutputAssignment(OutputTraceNode output, RawChip chip) 
    {
	FilterTraceNode filter = output.getPrevFilter();
	
	RawTile tile = chip.getTile(filter.getX(), filter.getY());
	//the neighboring dram of the tile we are assigning this buffer to
	int index = 0;
	//if there is more than one neighboring dram, randomly pick one
	if (tile.getIODevices().length > 1) {
	    index = rand.nextInt(tile.getIODevices().length);
	}
	//assign the buffer to the dram
	SpaceTimeBackend.println("Assigning (" + output.getPrevious() + "->" + 
			   output + " to " + tile.getIODevices()[index] + ")");
	IntraTraceBuffer.getBuffer(filter, output).
	    setDRAM((StreamingDram)tile.getIODevices()[index]);	
    }
    

    /**
     * given an <output> tracenode, this method returns a hashmap that assigns
     * the downstream edges to streaming drams, so one can assign
     * the IO buffers to drams based on the hashmap, make sure that
     * the buffer for the filter->outputtracenode and the buffers for the
     * inputtracenode->filter are assigned (it can always be reset) 
     * to ports before calling this...
     **/
    public static HashMap assignment(OutputTraceNode output, RawChip chip) 
    {
	HashMap assign = new HashMap();
	Iterator edges = output.getSortedOutputs().iterator();
	//the edges that have more than one input 
	//and were not initially assigned
	HashSet needToAssign = new HashSet();
	HashSet unassignedPorts = new HashSet();

	//if this outputtracenode has one output try to assign it 
	//to the same dram as its previous buffer
	if (output.oneOutput()) {
	    StreamingDram wanted = 
		IntraTraceBuffer.getBuffer(output.getPrevFilter(),
					   output).getDRAM();
	    //if the dram is not being used by another buffer connected to 
	    //the input trace, then assign it, otherwise let the below crap
	    //handle it.
	    if (!assignedInputDRAMs(output.getSingleEdge().getDest()).contains(wanted)) {
		assign.put(output.getSingleEdge(), wanted); 
		//exit because we have assigned the only edge
		return assign;
	    }
	    else {  //it might be added below also, but this is fine...
		needToAssign.add(output.getSingleEdge());
	    }
	    
	}

	//populate the unassigned ports set
	for (int i = 0; i < chip.getDevices().length; i++) 
	    unassignedPorts.add(chip.getDevices()[i]);

	//try to assign input trace nodes with one input 
	//first to make them redundant
	while (edges.hasNext()) {
	    Edge edge = (Edge)edges.next();
	    if (edge.getDest().oneInput()) {
		StreamingDram wanted =
		    IntraTraceBuffer.getBuffer(edge.getDest(), 
					       edge.getDest().getNextFilter()).getDRAM();
		if (unassignedPorts.contains(wanted)) {
		    unassignedPorts.remove(wanted);
		    assign.put(edge, wanted);
		}
		else //we could not assign it, the port was already assigned to a one input
		    needToAssign.add(edge);
	    }
	    else {
		//otherwise we need to assign it below
		needToAssign.add(edge);
	    }
	    
	}
	//assign the rest
	SpaceTimeBackend.println("  Need to assign (normally): " + needToAssign.size());
	edges = needToAssign.iterator();
	while (edges.hasNext()) {
	    Edge edge = (Edge)edges.next();
	    SpaceTimeBackend.println("    Getting assignment for " + edge);
	    //now assign the buffer to the first available port that show up 
	    //in the iterator
	    Iterator portOrder = assignmentOrder(edge, chip);
	    boolean assigned = false;
	    //SpaceTimeBackend.println("Assigning " + output + "->" + inputT + ": ");
	    while (portOrder.hasNext()) {
		StreamingDram current = ((PortDistance)portOrder.next()).dest;
		//SpaceTimeBackend.println("  Trying " + current);
		//assign the current dram to this input trace node
		//and exit the inner loop if the port has not 
		//been used by this output trace and the corresponding input trace
		if (unassignedPorts.contains(current) && 
		    !assignedInputDRAMs(edge.getDest()).contains(current)) {
		    unassignedPorts.remove(current);
		    assign.put(edge, current);
		    assigned = true;
		    //SpaceTimeBackend.println("  Assigned to " + current);
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
	    if (InterTraceBuffer.getBuffer(input.getSources()[i]).isAssigned())
		set.add(InterTraceBuffer.getBuffer(input.getSources()[i]).getDRAM());
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
	Iterator edges = assignment.keySet().iterator();
	StreamingDram src = IntraTraceBuffer.getBuffer(output.getPrevFilter(),
						       output).getDRAM();
	
	while (edges.hasNext()) {
	    //add the tiles for splitting
	    Util.addAll(tiles, Router.getRoute(src, 
					       (StreamingDram)assignment.get(edges.next())));
	}
	return tiles;
    }
    

    public static Set tilesOccupiedJoin(InputTraceNode input) 
    {
	HashSet tiles = new HashSet();
	StreamingDram dest = IntraTraceBuffer.getBuffer(input, 
							input.getNextFilter()).getDRAM();
	for (int i = 0; i < input.getSources().length; i++) {
	    Util.addAll(tiles, 
			Router.getRoute(InterTraceBuffer.getBuffer(input.getSources()[i]).getDRAM(),
					dest));
	}
	return tiles;
    }
    

    private static Iterator assignmentOrder(Edge edge, RawChip chip) 
    {
	//the streaming DRAM implementation can do both a 
	//read and a write on the same cycle, so it does not 
	//matter if the port is assigned to reading the outputtracenode
	//or writing to the inputtracenode
	//so just assign to ports based on the distance from the output
	//tracenode's port and to the input of the inputracenode
	TreeSet sorted = new TreeSet();
	StreamingDram src = IntraTraceBuffer.getBuffer(edge.getSrc().getPrevFilter(),
						       edge.getSrc()).getDRAM();
	System.out.println(IntraTraceBuffer.getBuffer(edge.getDest(), 
						      edge.getDest().getNextFilter()));
	StreamingDram dst = IntraTraceBuffer.getBuffer(edge.getDest(), 
						       edge.getDest().getNextFilter()).getDRAM();
	//	System.out.println("Order for: " + OffChipBuffer.getBuffer(output,input) + ", " +
	//		   src + " to " + dst);
	for (int i = 0; i < chip.getDevices().length; i++) {
	    //  System.out.println("  " + (StreamingDram)chip.getDevices()[i] + " = " + 
	    //		       (Router.distance(src, chip.getDevices()[i]) + 
	    //			Router.distance(chip.getDevices()[i], dst)));
	    
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
    //put this crap in so it sorts correctly with duplicate distances...
    private static int index;
    public int id;

    public PortDistance(StreamingDram dst, int dist) 
    {
	this.dest = dst;
	this.distance = dist;
	id = index++;
    }
    

    public boolean equals(PortDistance pd)
    {
	return (this.distance == pd.distance &&
		dest == pd.dest);
    }
    
    public int compareTo(Object pd) 
    {
	assert pd instanceof PortDistance;
	PortDistance portd = (PortDistance)pd;
	if (portd.distance == this.distance) {
	    if (dest == portd.dest)
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

package at.dms.kjc.raw;

import at.dms.kjc.*;
import at.dms.kjc.sir.*;
import at.dms.kjc.sir.lowering.*;
import at.dms.util.Utils;
import java.util.HashSet;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Iterator;
import streamit.scheduler.*;
import streamit.scheduler.simple.*;

/**
 * This class generates a schedule for the switch code by simulating the 
 * init schedule and one
 * steady state execution of the schedule
 */
public class RateMatchSim extends Simulator implements FlatVisitor
{
    private HashMap switchSchedules;
    
    //the current joiner code we are working on (steady or init)
    private static HashMap joinerCode;
        
    //the curent node in the joiner schedule we are working on
    private HashMap currentJoinerCode;
    
    private FlatNode toplevel;
    private FlatNode bottom;

    //true if we are simulating the init schedule
    private boolean initSimulation;

    public void simulate(FlatNode top) 
    {
	System.out.println("RateMatchSim Running...");
	
	initJoinerCode = new HashMap();
        steadyJoinerCode = new HashMap();

	//generate the joiner schedule
	JoinerSimulator.createJoinerSchedules(top);
	
	SimulationCounter counters = 
	    new SimulationCounter(JoinerSimulator.schedules);
	
	//generate the block execution counts
	BlockExecutionCounts.calcBlockCounts(top);
	
	//create copies of the executionCounts
	HashMap initExecutionCounts = (HashMap)RawBackend.initExecutionCounts.clone();
	HashMap steadyExecutionCounts = (HashMap)RawBackend.steadyExecutionCounts.clone();

	joinerCode = initJoinerCode;
	//	RawBackend.printCounts(RawBackend.initExecutionCounts, 
	//	       RawBackend.steadyExecutionCounts);
	initSchedules = (new RateMatchSim(top, true)).goInit(initExecutionCounts, counters, null);
	testExecutionCounts(initExecutionCounts);
	System.out.println("End of init simulation");

	joinerCode = steadyJoinerCode;
	steadySchedules = (new RateMatchSim(top, false)).go(steadyExecutionCounts, counters, null);
	testExecutionCounts(steadyExecutionCounts);
	System.out.println("End of steady-state simulation");
    }
    
    //used only to create a sim to call simulate on
    public RateMatchSim() {
    }
    
    private RateMatchSim(FlatNode top, boolean init) 
    {
	switchSchedules = new HashMap();
	currentJoinerCode = new HashMap();
	toplevel = top;
	//find the bottom (last) filter, used later to decide
	//execution order
	bottom = null;
	initSimulation = init;
    }
    
    /*
      This function tests to see if a simulation of a schedule has executed
      to its completion.  It checks if all the execution counts for 
      mapped streams are 0
    */
    private static void testExecutionCounts(HashMap exeCounts) 
    {
	boolean bad = false;
	
	Iterator it = exeCounts.keySet().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode)it.next();
	    if (Layout.getTile(node) != null) {
		if (((Integer)exeCounts.get(node)).intValue() != 0) {
		    System.out.println(node.contents.getName() + " has " + 
				       exeCounts.get(node) + " executions remaining!!!!");
		    bad = true;
		}
	    }
	}
	if (bad)
	    Utils.fail("Error in simulator.  Some nodes did not execute.  See above...");
	
    }
    
    /*
      This function is called before the init simulation is run.  It creates the code
      in the joiner code to call the initpath function and place the results in the
      correct buffer of the joiner
    */
    private void callInitPaths(SimulationCounter counters) 
    {
	//find all the joiners that are immediately contained in a FeedbackLoop
	Iterator joiners = Layout.joiners.iterator();
	//clone the joiner schedules
	
	FlatNode joiner;
	//iterate over all of the joiners of a feedbackloop
	while (joiners.hasNext()) {
	    joiner = (FlatNode)joiners.next();
	    if ((((SIRJoiner)joiner.contents).getParent() instanceof SIRFeedbackLoop)) {
		//create the initPath calls 
		SIRFeedbackLoop loop = (SIRFeedbackLoop)((SIRJoiner)joiner.contents).getParent();
		int delay = loop.getDelayInt();
		JoinerScheduleNode current = ((JoinerScheduleNode)JoinerSimulator.schedules.get(joiner));
		//		System.out.println("initPath " + joiner.contents.getName() + " " + delay);
		for (int i = 0; i < delay; i++) {
		    //for each init path call find the correct buffer to place it in.
		    while(true) {
			if (current.buffer.endsWith("1")) {
			    //create the joinerCode Node and put it in the init schedule
			    JoinerScheduleNode prev = 
				(JoinerScheduleNode)currentJoinerCode.get(joiner);
			    JoinerScheduleNode code = new JoinerScheduleNode(i, current.buffer);
			    if (prev == null) {
				//first node in joiner code
				//this will add it to the joiner code hashmap
				//this hashmap store the first instruction of each code sequence
				joinerCode.put(joiner, code);
			    }
			    else {
				//connect to the prev
				prev.next = code;
			    }
			    //set current
			    currentJoinerCode.put(joiner, code);
			    //record that a data item was placed in this buffer
			    counters.incrementJoinerBufferCount(joiner, current.buffer);
			    //we found a buffer so break and place the next initPath call
			    current = current.next;
			    break;
			}
			current = current.next;
		    }
		}
	    }
	}
    }
    
    //The start of the simulation for the initialization schedule
    private HashMap goInit(HashMap counts, SimulationCounter counters, FlatNode lastToFire) 
    {
	//create the initpath calls
	callInitPaths(counters);
	//simulate
	return go(counts, counters, lastToFire);
    }
	    
 
    /* the main simulation method */
    private HashMap go(HashMap counts, SimulationCounter counters, FlatNode lastToFire) 
    {
	
	FlatNode fire, dest;
		
	while(true) {
	    //find out who should fire
	    fire = whoShouldFire(lastToFire, counts, counters);
	    //if no one left to fire, stop
	    if (fire == null)
		break;
	    //keep track of everything needed when a node fires
	    int items = fireMe(fire, counters, counts);
	    //simulate the firings
	    //1 item for a joiner, push items for a filter
	    //System.out.println("firing " +fire.contents.getName() + " " + items);
	    for (int i = 0; i < items; i++) {
		//get the destinations of this item
		//could be multiple dests with duplicate splitters
		//a filter always has one outgoing arc, so sent to way 0
		generateSwitchCode(fire, getDestination(fire, 
							counters));
		//see if anyone downstream can fire
		//if (fire != lastToFire)
		//go(counts, counters, fire);
	    }
	}
	return switchSchedules;
    }


    //generate the switch code for 1 data item given the list of destinations
    //we do not want to duplicate items until necesary, so we have to keep track 
    //of all the routes and then generate the switch code
    //this way we can route multiple dests per route instruction
    private void generateSwitchCode(FlatNode fire, List dests) 
    {
	//should only have one previous
	HashMap prev = new HashMap();
	HashMap next = new HashMap();

	ListIterator destsIt = dests.listIterator();
	while (destsIt.hasNext()) {
 	    FlatNode dest = (FlatNode)destsIt.next();
	    if (dest == null) 
		System.out.println("Yup dest is null");
 	    Coordinate[] hops = 
 		(Coordinate[])Router.getRoute(fire, dest).toArray(new Coordinate[0]);
	    //add to fire's next
	    if (!next.containsKey(Layout.getTile(fire))) 
		next.put(Layout.getTile(fire), new HashSet());
	    ((HashSet)next.get(Layout.getTile(fire))).add(hops[1]);
	    //add to all other previous, next
	    for (int i = 1; i < hops.length -1; i++) {
		if (prev.containsKey(hops[i]))
		    if (prev.get(hops[i]) != hops[i-1])
			Utils.fail("More than one previous tile for a single data item");
		prev.put(hops[i], hops[i-1]);
		if (!next.containsKey(hops[i]))
		    next.put(hops[i], new HashSet());
		((HashSet)next.get(hops[i])).add(hops[i+1]);
	    }
	    //add the last step, plus the dest to the dest map
	    if (prev.containsKey(hops[hops.length - 1]))
		if (prev.get(hops[hops.length - 1]) != hops[hops.length - 2])
		    Utils.fail("More than one previous tile for a single data item (2)");
	    prev.put(hops[hops.length-1], hops[hops.length - 2]);
	    if (!next.containsKey(hops[hops.length-1]))
		next.put(hops[hops.length - 1], new HashSet());
	    ((HashSet)next.get(hops[hops.length - 1])).add(hops[hops.length -1]);
	}
	
	//create the appropriate amount of rou
	int elements = Util.getTypeSize(Util.getOutputType(fire));
	for (int i = 0; i < elements; i++)
	    asm(Layout.getTile(fire), prev, next);
    }
    
    private void asm(Coordinate fire, HashMap previous, HashMap next) 
    {
	//generate the sends
	if (!switchSchedules.containsKey(fire))
	    switchSchedules.put(fire, new StringBuffer());
	StringBuffer buf = (StringBuffer)switchSchedules.get(fire);
	Iterator it = ((HashSet)next.get(fire)).iterator();
	buf.append("route ");
	while (it.hasNext()) {
	    Coordinate dest = (Coordinate)it.next();
	    buf.append("$csto->" + "$c" + 
		       Layout.getDirection(fire, dest) + 
		       "o,");
	}
	//erase the trailing ,
	buf.setCharAt(buf.length() - 1, '\n');
	
	//generate all the other 
	Iterator tiles = next.keySet().iterator();
	while (tiles.hasNext()) {
	    Coordinate tile = (Coordinate)tiles.next();
	    if (tile == fire) 
		continue;
	    if (!switchSchedules.containsKey(tile))
		switchSchedules.put(tile, new StringBuffer());
	    buf = (StringBuffer)switchSchedules.get(tile);
	    Coordinate prevTile = (Coordinate)previous.get(tile);
	    buf.append("route ");	    Iterator nexts = ((HashSet)next.get(tile)).iterator();
	    while(nexts.hasNext()) {
		Coordinate nextTile = (Coordinate)nexts.next();
		if (!nextTile.equals(tile))
		    buf.append("$c" + Layout.getDirection(tile, prevTile) + "i->$c" +
			       Layout.getDirection(tile, nextTile) + "o,");
		else 
		    buf.append("$c" + Layout.getDirection(tile, prevTile) + "i->$c" +
			       Layout.getDirection(tile, nextTile) + "i,");
	    }
	    buf.setCharAt(buf.length() - 1, '\n');
	}
    }
    
    private int itemsNeededToFire(FlatNode fire, SimulationCounter counters,
				  HashMap executionCounts) 
    {
	if (initSimulation) {
	    if (!counters.hasFired(fire)) {
		if (fire.contents instanceof SIRTwoStageFilter){
		    //if the twostage does nothing in its initWork()
		    //then the initWork should not count as an execution
		    //return peek items as the needed items
		    SIRTwoStageFilter two = (SIRTwoStageFilter)fire.contents;
		    if (two.getInitPeek() == 0 &&
			two.getInitPush() == 0 &&
			two.getInitPop() == 0) {
			return two.getPeekInt();
		    }
		    return ((SIRTwoStageFilter)fire.contents).getInitPeek();
		}
		else 
		    return ((SIRFilter)fire.contents).getPeekInt();
	    }
	    return ((SIRFilter)fire.contents).getPopInt();
	}

	return ((SIRFilter)fire.contents).getPopInt() * 
	    BlockExecutionCounts.getBlockCount(fire);
	//if inside a feedback loop then just execution one 
	//work function call
	//	if (((SIRStream)fire.contents).insideFeedbackLoop())
	//    return ((SIRFilter)fire.contents).getPopInt();
	//else
	//    return ((SIRFilter)fire.contents).getPopInt() * 
	//	((Integer)executionCounts.get(fire)).intValue();
    }
   
    private void decrementExecutionCounts(FlatNode fire, HashMap executionCounts) 
    {
	//if we are inside of a feedback loop we have to execute the old way
	//one work() at a time
	int oldVal = ((Integer)executionCounts.get(fire)).intValue();
		
	if (initSimulation) {
	    if (oldVal - 1 < 0)
		Utils.fail("Executed too much");
	    executionCounts.put(fire, new Integer(oldVal - 1));
	}
	else {
	    if (oldVal - BlockExecutionCounts.getBlockCount(fire) < 0)
		Utils.fail("Executed too much");
	    
	    executionCounts.put(fire, new Integer(oldVal - 
						  BlockExecutionCounts.getBlockCount(fire)));
	}
    }
    
    //consume the data and return the number of items produced
    private int fireMe(FlatNode fire, SimulationCounter counters, HashMap executionCounts) 
    {
	if (fire.contents instanceof SIRFilter) {
	    
	    //consume the date from the buffer
	    counters.decrementBufferCount(fire, 
					  itemsNeededToFire(fire, counters, executionCounts));
	    
	    
	    //for a steady state execution return the normal push
	    int ret = ((SIRFilter)fire.contents).getPushInt();
	    if (initSimulation) {
		//if the filter is a two stage, and it has not fired
		//return the initPush() unless the initWork does nothing
		if (fire.contents instanceof SIRTwoStageFilter) {
		    if (!counters.hasFired(fire)) {
			SIRTwoStageFilter two = (SIRTwoStageFilter)fire.contents;
			if (!(two.getInitPeek() == 0 &&
			      two.getInitPush() == 0 &&
			      two.getInitPop() == 0)){
			    ret = ((SIRTwoStageFilter)fire.contents).getInitPush();
			}
		    }
		}
		//decrement the schedule execution counter
		//now this node has fired
		counters.setFired(fire);
		decrementExecutionCounts(fire, executionCounts);
		return ret;
	    }
	    //now this node has fired
	    counters.setFired(fire);
	    //if this is not inside of a feedback loop fire everything
	    //at once
	    ret *= BlockExecutionCounts.getBlockCount(fire);
	    //decrement the schedule execution counter
	    decrementExecutionCounts(fire, executionCounts);
	    return ret; 
	}
	else if (fire.contents instanceof SIRJoiner) {
	    return fireJoiner(fire, counters, executionCounts);
	}
    
	Utils.fail("Trying to fire a non-filter or joiner");
	return -1;
    }
    
    /*
      add the joiner code to the code schedule for the given joiner
    */
    private void addJoinerCode(FlatNode fire, JoinerScheduleNode code) 
    {
	//add to the joiner code for this fire
	    JoinerScheduleNode prev = 
		(JoinerScheduleNode)currentJoinerCode.get(fire);
	    if (prev == null) {
		//first node in joiner code
		joinerCode.put(fire, code);
	    }
	    else {
		//connect 
		prev.next = code;
	    }
	    //set code
	    currentJoinerCode.put(fire, code);
    }
    
    
    
    private int fireJoiner(FlatNode fire, SimulationCounter counters, HashMap executionCounts)
    {
	//System.out.println("Firing " + fire.contents.getName());
	//The joiner is passing a data item, record this as an execution
	decrementExecutionCounts(fire, executionCounts);
	
	//else, this joiner fired because it has data that can be sent downstream
	JoinerScheduleNode current = new JoinerScheduleNode();
	current.buffer = counters.getJoinerBuffer(fire);
	current.type = JoinerScheduleNode.FIRE;
	addJoinerCode(fire, current);
	
	//decrement the buffer
	counters.decrementJoinerBufferCount(fire, counters.getJoinerBuffer(fire));
	//step the schedule
	counters.incrementJoinerSchedule(fire);
	return 1;
    }


    //return the destinations of the data item and generate the code
    //to receive data into the joiner
    private List getDestination(FlatNode node, SimulationCounter counters)
    {
	List list = getDestinationHelper(node.edges[0], counters, "", node);
	HashSet visited = new HashSet();
	//generate joiner receive code	
	//iterate over the list 
	Iterator it = list.iterator();
	while(it.hasNext()) {
	    FlatNode dest = (FlatNode)it.next();
	    if (!(dest.contents instanceof SIRJoiner))
		continue;
	    //decrement the buffer from the joiner
	    counters.decrementBufferCount(dest, 1);
	    //get the joiner buffer as determined by getDestination and stored in a list 
	    String joinerBuffer = counters.getJoinerReceiveBuffer(dest);
	    //as determined by the simulation
	    counters.incrementJoinerBufferCount(dest, joinerBuffer);
	    //add to the joiner code for this dest
	    JoinerScheduleNode current = new JoinerScheduleNode();
	    current.buffer = joinerBuffer;
	    if (visited.contains(dest))
		current.type = JoinerScheduleNode.DUPLICATE;
	    else
		current.type = JoinerScheduleNode.RECEIVE;
	    addJoinerCode(dest, current);
	    
	    visited.add(dest);
	}
	return list;
    }
    

    //get the destination of the data item
    private List getDestinationHelper(FlatNode node, SimulationCounter counters, String
				 joinerBuffer, FlatNode previous) 
    {
	//if we reached a node then this is a destination
	//add to its buffer and
	//create a list and add it
	if (node.contents instanceof SIRFilter) {
	    if (Layout.identities.contains(node)) {
		return getDestinationHelper(node.edges[0], counters,
					    joinerBuffer, node);
	    }
	    counters.incrementBufferCount(node);
	    LinkedList list = new LinkedList();
	    list.add(node);
	    return list;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //just pass thru joiners except the joiners that are the 
	    //last joiner in a joiner group
	    //this list is kept in the layout class
	    if (Layout.joiners.contains(node)) {
		joinerBuffer = joinerBuffer + getJoinerBuffer(node, previous);
		counters.addJoinerReceiveBuffer(node, joinerBuffer);
		counters.incrementBufferCount(node);
		LinkedList list = new LinkedList();
		list.add(node);
		return list;
	    }
	    else {
		return getDestinationHelper(node.edges[0], counters, 
				      joinerBuffer + getJoinerBuffer(node, 
								     previous),
				      node);
	    }
	}
	else if (node.contents instanceof SIRSplitter) {
	    //here is the meat
	    SIRSplitter splitter = (SIRSplitter)node.contents;
	    //if splitter send the item out to all arcs
	    //build a list of all the dests
	    if (splitter.getType() == SIRSplitType.DUPLICATE) {
		LinkedList list = new LinkedList();
                for (int i = 0; i < node.ways;i++) {
		    //decrement counter on arc
		    if (counters.getArcCountOutgoing(node, i) == 0)
			counters.resetArcCountOutgoing(node, i);
		    counters.decrementArcCountOutgoing(node, i);
		    list.addAll(getDestinationHelper(node.edges[i], 
					    counters, joinerBuffer,
					    previous));
		}
		return list;
	    }
	    else {
		//weighted round robin
		for (int i = 0; i < node.ways; i++) {
		    if (counters.getArcCountOutgoing(node, i) > 0) {
			counters.decrementArcCountOutgoing(node, i);
			return getDestinationHelper(node.edges[i], 
					      counters, joinerBuffer,
					      previous);
		    }
		}
		//none were greater than zero, reset all counters
		//and send to the first non-zero weight
		for (int i = 0; i < node.ways; i++) {
		    counters.resetArcCountOutgoing(node, i);
		}
		for (int i = 0; i < node.ways; i++) {
		    if (counters.getArcCountOutgoing(node, i) > 0) {
			counters.decrementArcCountOutgoing(node, i);
			return getDestinationHelper(node.edges[i], 
					      counters, joinerBuffer,
					      previous);
		    }
		}
	    }
	    
	}
	else {
	    throw new RuntimeException("SimulateDataItem");
	}
	return null;
    }

    //find the most upstream node that can fire
    private FlatNode whoShouldFire(FlatNode current, HashMap executionCounts, 
				    SimulationCounter counters) 
    {
	FlatNode start = current;
	//breadth first search from bottom
	if (start == null)
	    start = toplevel;
	HashSet visited = new HashSet();
	Vector queue = new Vector();
	FlatNode node;
	FlatNode mostDownStream = null;
	
	queue.add(start);
	while (!queue.isEmpty()) {
  
	    node = (FlatNode)queue.get(0);
	    queue.remove(0);
	    
	    if (node == null)
		continue;
	    
	    visited.add(node);
	    if (canFire(node, executionCounts, counters)) {
		return node;
	    }

	    //to keep the order of the nodes of a splitjoin in the correct order
	    //(the order defined by the joiner) add to the queue in the reverse order
	    for (int i = node.ways - 1; i >= 0; i--) {
		if (!visited.contains(node.edges[i]))
			queue.add(node.edges[i]);
	    }
	}
	//no node can fire
	return null;
    }
   

    private boolean canFire(FlatNode node, HashMap executionCounts, 
			    SimulationCounter counters) 
    {
	if (node == null)
	    return false;
	if (Layout.identities.contains(node))
	    return false;

	if (node.contents instanceof SIRFilter) {
	    //check if this node has fired the number of times given by
	    //the schedule
	    Integer count = (Integer)executionCounts.get(node);
	    //if a node is not executed at all in a schedule it will not have an
	    //entry
	    if (count == null)
		return false;
	    if (count.intValue() == 0) {
		return false;
	    }
	    if (counters.getBufferCount(node) >= itemsNeededToFire(node, counters, 
								   executionCounts)) {
		return true;
	    }
	    else
		return false;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //first of all, a joiner can only fire it is the most downstream
	    //joiner in a joiner group
	    if (!Layout.joiners.contains(node))
		return false;
	    
	    //determine if the joiner can receive and buffer data
	    //this does not count as an execution of the joiner
	    //only sending data counts as an execution, that is why we check that 
	    //next
	    //if (counters.getBufferCount(node) > 0) 
	    //	return true;
	    
	    //check if this node has fired the number of times given by
	    //the schedule
	    Integer count = (Integer)executionCounts.get(node);
	    //if a node is not executed at all in a schedule it will not have an
	    //entry
	    if (count == null)
		return false;
	    if (count.intValue() == 0) {
		return false;
	    }

	    //	    System.out.println("Trying to fire Joiner " + node.contents.getName());
	    //System.out.println(counters.getJoinerBufferCount(node, counters.
	    //							     getJoinerBuffer(node)));
	    
	    //determine if the joiner can send data downstream from a buffer
	    if (counters.getJoinerBufferCount(node, counters.
	    				      getJoinerBuffer(node)) > 0)
		return true;
	    else
		return false;
	}
	else 
	    return false;
    }
   	
    //Just a debugging function, not used
    public void visitNode(FlatNode node) 
    {
	System.out.println(node.contents.getName());
	if (node.contents instanceof SIRTwoStageFilter) {
	    SIRTwoStageFilter two = (SIRTwoStageFilter)node.contents;
	    System.out.println("init peek: " + two.getInitPeek() + " init pop:" + two.getInitPop() +
			       " init push: " + two.getInitPush());
	}
	if (node.contents instanceof SIRFilter) {
	    SIRFilter two = (SIRFilter)node.contents;
	    System.out.println("init peek: " + two.getPeekInt() + " init pop:" + two.getPopInt() +
			       " init push: " + two.getPushInt());
	}
	
	if (RawBackend.initExecutionCounts.containsKey(node))
	    System.out.println("   executes in init " + 
			       ((Integer)RawBackend.initExecutionCounts.get(node)).intValue());
	if (RawBackend.steadyExecutionCounts.containsKey(node))
	    System.out.println("   executes in steady " + 
			       ((Integer)RawBackend.steadyExecutionCounts.get(node)).intValue());
    }
    
    private String getJoinerBuffer(FlatNode node, FlatNode previous) 
    {
	//	System.out.println(node.contents.getName());
	//System.out.println(previous.contents.getName());
	for (int i = 0; i < node.inputs; i++) {
	    if (node.incoming[i] == null)
		continue;
	    
	    if (node.incoming[i] == previous)
		return String.valueOf(i);
	    if (node.incoming[i].contents instanceof SIRSplitter) {
		FlatNode temp = node.incoming[i];
		while (true) {
		    if (temp == previous)
			return String.valueOf(i);
		    if (!(temp.contents instanceof SIRSplitter))
			break;
		    temp = temp.incoming[0];
		}
	    }
	}
	
	Utils.fail("cannot find previous node in joiner list");
	return null;
    }
 
    private static void printExecutionCounts(HashMap map) 
    {
	System.out.println();

	Iterator it = map.keySet().iterator();
	while(it.hasNext()) {
	    FlatNode node = (FlatNode)it.next();
	    System.out.println(node.contents.getName() + " " + 
			       ((Integer)map.get(node)).toString());
	}
	System.out.println();
	
    }

}

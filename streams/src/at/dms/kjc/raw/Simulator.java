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
public class Simulator extends at.dms.util.Utils implements FlatVisitor
{
    public static HashMap initSchedules;
    public static HashMap steadySchedules;
    
    public static HashMap initJoinerCode;
    public static HashMap steadyJoinerCode;
    
    private HashMap switchSchedules;
    
    //the current joiner code we are working on (steady or init)
    private static HashMap joinerCode;
        
    //the curent node in the joiner schedule we are working on
    private HashMap currentJoinerCode;
    
    private FlatNode toplevel;
    private FlatNode bottom;

    //true if we are simulating the init schedule
    private boolean initSimulation;

    public static void simulate(FlatNode top) 
    {
	System.out.println("Simulator Running...");
	
	initJoinerCode = new HashMap();
        steadyJoinerCode = new HashMap();

	//generate the joiner schedule
	JoinerSimulator.createJoinerSchedules(top);
	
	SimulationCounter counters = 
	    new SimulationCounter(JoinerSimulator.schedules);

	joinerCode = initJoinerCode;
	initSchedules = (new Simulator(top, true)).go(RawBackend.initExecutionCounts, counters, null);
	System.out.println("End of init simulation");
       
	joinerCode = steadyJoinerCode;
	steadySchedules = (new Simulator(top, false)).go(RawBackend.steadyExecutionCounts, counters, null);
    }
    
   
    

    private Simulator(FlatNode top, boolean init) 
    {
	switchSchedules = new HashMap();
	currentJoinerCode = new HashMap();
	toplevel = top;
	//find the bottom (last) filter, used later to decide
	//execution order
	bottom = null;
	initSimulation = init;
	//toplevel.accept(this, new HashSet(), false);

	//System.out.println("Bottom node " + Namer.getName(bottom.contents));
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
	    	    

	    for (int i = 0; i < items; i++) {
		//System.out.println(Namer.getName(fire.contents) + " pushing " + items);
		//get the destinations of this item
		//could be multiple dests with duplicate splitters
		//a filter always has one outgoing arc, so sent to way 0
		generateSwitchCode(fire, getDestination(fire.edges[0], 
							counters, "", 
							fire));
		//see if anyone downstream can fire
		//if (fire != lastToFire)
		go(counts, counters, fire);
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
	    buf.append("route ");
	    Iterator nexts = ((HashSet)next.get(tile)).iterator();
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
    
    private int itemsNeededToFire(FlatNode fire, SimulationCounter counters) 
    {
	if (!counters.hasFired(fire)) {
	    if (fire.contents instanceof SIRTwoStageFilter && initSimulation)
		return ((SIRTwoStageFilter)fire.contents).getInitPeek();
	    else 
		return ((SIRFilter)fire.contents).getPeekInt();
	}
	else
	    return ((SIRFilter)fire.contents).getPopInt();
    }
   
    //consume the data and return the number of items produced
    private int fireMe(FlatNode fire, SimulationCounter counters, HashMap executionCounts) 
    {
	//System.out.println("Firing " + Namer.getName(fire.contents));
	

	if (fire.contents instanceof SIRFilter) {
	    //decrement the schedule execution counter
	    int oldVal = ((Integer)executionCounts.get(fire)).intValue();
	    if (oldVal - 1 < 0)
		Utils.fail("Executed too much");
	    executionCounts.put(fire, new Integer(oldVal - 1));
	    
	    counters.decrementBufferCount(fire, 
					  itemsNeededToFire(fire, counters));
	    
	    //for a steady state execution return the normal push
	    int ret = ((SIRFilter)fire.contents).getPushInt();
	    //if the filter is a two stage, and it has not fired
	    //return the initPush()
	    if (fire.contents instanceof SIRTwoStageFilter)
		if (!counters.hasFired(fire) && initSimulation)
		    ret = ((SIRTwoStageFilter)fire.contents).getInitPush();
	    
	    //now this node has fired
	    counters.setFired(fire);
	    return ret;
	}
	else if (fire.contents instanceof SIRJoiner) {
	    //System.out.println("Firing a joiner");
	    
	    //determine if this joiner fired because of an initpath call
	    //if so just increment the number of calls to the initpath
	    if (counters.canFeedbackJoinerFire(fire)) {
		counters.incrementInitPathCall(fire);
		return 1;
	    }
	    else { //otherwise the joiner fired normally ...
		return fireJoiner(fire, counters, executionCounts);

	    }
	}
	Utils.fail("Trying to fire a non-filter or joiner");
	return -1;
    }
    
    private int fireJoiner(FlatNode fire, SimulationCounter counters, HashMap executionCounts)
    {
	//A joiner can fire if needs to receive data from its upstream filter or if it can
	//send data downstream
	if (counters.getBufferCount(fire) > 0) {
	    //decrement the buffer from the joiner
	    counters.decrementBufferCount(fire, 1);
	    //get the joiner buffer as determined by getDestination and stored in a list 
	    String joinerBuffer = counters.getJoinerReceiveBuffer(fire);
	    //System.out.println(Namer.getName(fire.contents) + " adding to joiner buffer " + joinerBuffer);
	    
	    //as determined by the simulation
	    counters.incrementJoinerBufferCount(fire, joinerBuffer);
	    //add to the joiner code for this fire
	    JoinerScheduleNode prev = 
		(JoinerScheduleNode)currentJoinerCode.get(fire);
	    JoinerScheduleNode current = new JoinerScheduleNode();
	    current.buffer = joinerBuffer;
	    current.type = JoinerScheduleNode.RECEIVE;
	    if (prev == null) {
		//first node in joiner code
		joinerCode.put(fire, current);
	    }
	    else {
		//connect 
		prev.next = current;
	    }
	    //set current
	    currentJoinerCode.put(fire, current);
	    return 0;
	}
	else {
	    //else, this joiner fired because it received data
	    //that can be sent on
	    JoinerScheduleNode previous = 
		(JoinerScheduleNode)currentJoinerCode.get(fire);
	    JoinerScheduleNode current = new JoinerScheduleNode();
	    current.buffer = counters.getJoinerBuffer(fire);
	    current.type = JoinerScheduleNode.FIRE;
	    previous.next = current;
	    //System.out.println(Namer.getName(fire.contents) + " sending from joiner buffer " + current.buffer);
	    
	    
	    //set current
	    currentJoinerCode.put(fire, current);
	    //decrement the buffer
	    counters.decrementJoinerBufferCount(fire, counters.getJoinerBuffer(fire));
	    //step the schedule
	    counters.incrementJoinerSchedule(fire);
	    return 1;
	}
    }
    //get the destination of the data item
    private  List getDestination(FlatNode node, SimulationCounter counters, String
				 joinerBuffer, FlatNode previous) 
    {
	//if we reached a node then this is a destination
	//add to its buffer and
	//create a list and add it
	if (node.contents instanceof SIRFilter) {
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
		return getDestination(node.edges[0], counters, 
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
		    list.addAll(getDestination(node.edges[i], 
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
			return getDestination(node.edges[i], 
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
			return getDestination(node.edges[i], 
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

    //for now, find the most-downstream filter to fire
    //from the starting node
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
			
	//System.out.println("Starting from " + Namer.getName(start.contents));
	
	queue.add(start);
	while (!queue.isEmpty()) {
	    node = (FlatNode)queue.get(0);
	    visited.add(node);
	    if (canFire(node, executionCounts, counters)) {
		mostDownStream = node;
	    }
	    queue.remove(0);
	    for (int i = 0; i < node.ways; i++) {
		if (!visited.contains(node.edges[i]))
			queue.add(node.edges[i]);
	    }
	}
	//no node can fire
	if (mostDownStream == current)
	    return null;
	return mostDownStream;
    }
   

    private boolean canFire(FlatNode node, HashMap executionCounts, 
			    SimulationCounter counters) 
    {
	if (node.contents instanceof SIRFilter) {
	    //check if this node has fired the number of times given by
	    //the schedule
	    
	    //System.out.println("Checking execution count: " + 
	    //Namer.getName(node.contents));
	 
	    //System.out.println("Can I fire " + Namer.getName(node.contents) + " " + counters.getBufferCount(node) + " " + itemsNeededToFire(node, counters));
   
	    Integer count = (Integer)executionCounts.get(node);
	    if (count == null)
		return false;
	    if (count.intValue() == 0) {
		//System.out.println("executed enough already");
		return false;
	    }
	    
	    if (counters.getBufferCount(node) >= itemsNeededToFire(node, counters)) {
		return true;
	    }
	    else
		return false;
	}
	else if (node.contents instanceof SIRJoiner) {
	    //System.out.println("Can I fire " + Namer.getName(node.contents) + " " + counters.getBufferCount(node));
	    //first of all, a joiner can only fire it is the most downstream
	    //joiner in a joiner group
	    if (!Layout.joiners.contains(node))
		return false;
	    //if this joiner is inside of a feedbackloop then it can fire
	    //as many times as the delay
	    if (counters.canFeedbackJoinerFire(node))
		return true;
	    //determine if a joiner can fire
	    //if the buffer associated with its current 
	    //input has an item in it or if it has input waiting to be buffered
	    if (counters.getBufferCount(node) > 0) 
		return true;
	    if (counters.getJoinerBufferCount(node, counters.
	    				      getJoinerBuffer(node)) > 0)
		return true;
	    else
		return false;
	}
	else 
	    return false;
    }
   	
    //look for bottom node
    public void visitNode(FlatNode node) 
    {
	System.out.println(Namer.getName(node.contents));
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
	for (int i = 0; i < node.inputs; i++) {
	    if (node.incoming[i] == previous)
		return String.valueOf(i);
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
	    System.out.println(Namer.getName(node.contents) + " " + 
			       ((Integer)map.get(node)).toString());
	}
	System.out.println();
	
    }
       
}
 //  private void generateSwitchCode(FlatNode fire, List dests) 
//     {
// 	//keeps the next hop for the sends
// 	HashSet sends = new HashSet();
// 	//keeps routes for all of the intermediate hops
// 	//this hashset points to a hashset 
// 	//the secode hash map is indexed by the sources of 
// 	//the route instructions for the tile
// 	//the second hashmap points to a hashset
// 	//of all the dest for a given source

// 	HashMap routes = new HashMap();
       
// 	//maps receivers to their previous hop
// 	HashMap receives = new HashMap();
		
// 	//fill the maps
// 	ListIterator destsIt = dests.listIterator();
// 	while (destsIt.hasNext()) {
// 	    FlatNode dest = (FlatNode)destsIt.next();
// 	    Coordinate[] hops = 
// 		(Coordinate[])Router.getRoute(fire, dest).toArray(new Coordinate[0]);
// 	    //add each route to the maps
// 	    sends.add(hops[1]);
// 	    //add the intermediate routes
// 	    for (int i = 1; i < hops.length -1; i++) {
// 		if (!routes.containsKey(hops[i]))
// 		    routes.put(hops[i], new HashMap());
// 		HashMap prevs = (HashMap)routes.get(hops[i]);
// 		if (!prevs.containsKey(hops[i-1]))
// 		    prevs.put(hops[i-1], new HashSet());
// 		HashSet nexts = (HashSet)prevs.get(hops[i-1]);
// 		nexts.add(hops[i+1]);
// 	    }
// 	    //add the receive
// 	    receives.put(hops[hops.length-1], hops[hops.length - 2]);
// 	}
// 	//now generate the send, routes, and receives
// 	addSends(fire, sends);
// 	addRoutes(routes);
// 	addReceives(receives);
//     }
//  private void addRoutes(HashMap routes) 
//     {
// 	//get each router node
// 	Iterator routerIt = routes.keySet().iterator();
// 	while (routerIt.hasNext()) {
// 	    //for each router get all the sources
// 	    Coordinate router = (Coordinate)routerIt.next();
// 	    //get the router's switch code buffer
// 	    if (!switchSchedules.containsKey(router))
// 		switchSchedules.put(router, new StringBuffer());
	
// 	    StringBuffer buf = (StringBuffer)switchSchedules.get(router);
// 	    //for each source get the dests
// 	    Iterator sourcesIt = ((HashMap)routes.get(router)).keySet().iterator();
// 	    while(sourcesIt.hasNext()) {
// 		//generate the switch code, for each source send to all the dests
// 		//in one route instruction
// 		Coordinate source = (Coordinate)sourcesIt.next();
// 		Iterator destsIt = 
// 		    ((HashSet)((HashMap)routes.get(router)).get(source)).iterator();
// 		buf.append("\tnop\troute ");
// 		while(destsIt.hasNext()) {
// 		    Coordinate dest = (Coordinate)destsIt.next();
// 		    buf.append("$c" + Layout.getDirection(router, source) + "i->$c" +
// 			       Layout.getDirection(router, dest) + "o,");
// 		}
// 		buf.setCharAt(buf.length() - 1, '\n');
// 	    }
// 	}
//     }
        
//     private void addSends(FlatNode fire, HashSet sends) 
//     {
// 	if (!switchSchedules.containsKey(Layout.getTile(fire)))
// 	    switchSchedules.put(Layout.getTile(fire), new StringBuffer());
	
// 	StringBuffer buf = (StringBuffer)switchSchedules.get(Layout.getTile(fire));
	
// 	Iterator it = sends.iterator();
	
// 	buf.append("\tnop\troute ");
// 	while (it.hasNext()) {
// 	    Coordinate dest = (Coordinate)it.next();
// 	    buf.append("$csto->" + "$c" + 
// 		       Layout.getDirection(Layout.getTile(fire), dest) + 
// 		       "o,");
// 	}
// 	//erase the trailing ,
// 	buf.setCharAt(buf.length() - 1, '\n');
//     }
    
//     private void addReceives(HashMap receives) 
// 	{
// 	    Iterator it = receives.keySet().iterator();
	    
// 	    while (it.hasNext()) {
// 		Coordinate rec = (Coordinate)it.next();
// 		Coordinate send = (Coordinate)receives.get(rec);
// 		if (!switchSchedules.containsKey(rec))
// 		    switchSchedules.put(rec, new StringBuffer());
// 		StringBuffer buf = (StringBuffer)switchSchedules.get(rec);
// 		buf.append("\tnop\troute ");
// 		buf.append("$c" + Layout.getDirection(rec, send) + "i");
// 		buf.append("->$csti\n");
// 	    }
// 	}
    
